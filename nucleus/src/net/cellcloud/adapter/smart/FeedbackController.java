/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2017 Cell Cloud Team (www.cellcloud.net)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
-----------------------------------------------------------------------------
*/

package net.cellcloud.adapter.smart;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import net.cellcloud.common.Logger;
import net.cellcloud.core.Endpoint;
import net.cellcloud.util.Clock;

/**
 * 回馈控制器。
 * 
 * @author Ambrose Xu
 * 
 */
public class FeedbackController extends TimerTask {

	/**
	 * 关键字与回馈信息的映射关系。
	 */
	private ConcurrentHashMap<String, Feedback> feedbackMap;

	/**
	 * 回馈信息主动发送记录。
	 */
	private ConcurrentHashMap<String, InitiativeRecord> initiativeRecordMap;

	/**
	 * 关键字超期时间。
	 */
	private long keywordExpired = 12L * 60L * 60L * 1000L;

	/**
	 * 两次回馈数据压制间隔。
	 */
	private long inhibitionInterval = 3L * 60L * 1000L;
	/**
	 * 进行压制需要达到的主动发送次数。
	 */
	private long inhibitionCounts = 2L;

	/**
	 * 关键字压制生效之后的过期时长。当压制后达到此时长，则压制信息被清空。 
	 */
	private long inhibitionExpired = 30L * 60L * 1000L;

	/**
	 * 任务定时器。
	 */
	private Timer timer;

	/**
	 * 构造器。
	 */
	public FeedbackController() {
		this.feedbackMap = new ConcurrentHashMap<String, Feedback>();
		this.initiativeRecordMap = new ConcurrentHashMap<String, InitiativeRecord>();
	}

	/**
	 * 启动控制器。
	 */
	public void start() {
		if (null == this.timer) {
			this.timer = new Timer(this.getClass().getSimpleName() + "-Timer");
			this.timer.schedule(this, 10000L, 60000L);
		}
	}

	/**
	 * 停止控制器。
	 */
	public void stop() {
		if (null != this.timer) {
			this.timer.cancel();
			this.timer = null;
		}
	}

	/**
	 * 判断指定的关键字是否有回馈信息。
	 * 
	 * @param keyword 指定关键字。
	 * @return 如果关键字有回馈信息返回 true ，否则返回 false 。
	 */
	public boolean hasFeedback(String keyword) {
		return this.feedbackMap.containsKey(keyword);
	}

	/**
	 * 获取关键字在指定终端的回馈的正回馈计数。
	 * 
	 * @param keyword 指定待查询关键字。
	 * @param endpoint 指定终端。
	 * @return 返回此关键字由该终端回馈的正回馈次数。
	 */
	public int getPositiveCounts(String keyword, Endpoint endpoint) {
		Feedback feedback = this.feedbackMap.get(keyword);
		if (null == feedback) {
			return 0;
		}

		return feedback.countPositive(endpoint);
	}

	/**
	 * 获取关键字在指定终端的回馈的负回馈计数。
	 * 
	 * @param keyword 指定待查询关键字。
	 * @param endpoint 指定终端。
	 * @return 返回此关键字由该终端回馈的负回馈次数。
	 */
	public int getNegativeCounts(String keyword, Endpoint endpoint) {
		Feedback feedback = this.feedbackMap.get(keyword);
		if (null == feedback) {
			return 0;
		}

		return feedback.countNegative(endpoint);
	}

	/**
	 * 以正回馈方式更新关键字对应的终端。
	 * 
	 * @param keyword 指定关键字。
	 * @param endpoint 指定终端。
	 */
	public void updateEncourage(String keyword, Endpoint endpoint) {
		Feedback feedback = this.feedbackMap.get(keyword);
		if (null != feedback) {
			feedback.updatePositive(endpoint);
		}
		else {
			feedback = new Feedback(keyword);
			feedback.updatePositive(endpoint);
			this.feedbackMap.put(keyword, feedback);
		}

		// 从 Negative 中移除
		feedback.removeNegative(endpoint);
	}

	/**
	 * 以负回馈方式更新关键字对应的终端。
	 * 
	 * @param keyword 指定关键字。
	 * @param endpoint 指定终端。
	 */
	public void updateDiscourage(String keyword, Endpoint endpoint) {
		Feedback feedback = this.feedbackMap.get(keyword);
		if (null != feedback) {
			feedback.updateNegative(endpoint);
		}
		else {
			feedback = new Feedback(keyword);
			feedback.updateNegative(endpoint);
			this.feedbackMap.put(keyword, feedback);
		}

		// 从 Positive 中移除
		feedback.removePositive(endpoint);
	}

	/**
	 * 记录正回馈发送记录。
	 * 
	 * @param keyword 指定关键字。
	 * @param endpoint 指定终端。
	 */
	public void recordEncourage(String keyword, Endpoint endpoint) {
		InitiativeRecord record = this.initiativeRecordMap.get(keyword);
		if (null != record) {
			record.timestamp = Clock.currentTimeMillis();

			// 记录时间
			synchronized (record.lastEncourageTime) {
				AtomicLong time = record.lastEncourageTime.get(endpoint);
				if (null != time) {
					time.set(Clock.currentTimeMillis());
				}
				else {
					record.lastEncourageTime.put(endpoint, new AtomicLong(Clock.currentTimeMillis()));
				}
			}

			// 记录次数
			synchronized (record.encourageCounts) {
				AtomicLong counts = record.encourageCounts.get(endpoint);
				if (null != counts) {
					counts.incrementAndGet();
				}
				else {
					record.encourageCounts.put(endpoint, new AtomicLong(1));
				}
			}
		}
		else {
			record = new InitiativeRecord();
			record.lastEncourageTime.put(endpoint, new AtomicLong(Clock.currentTimeMillis()));
			record.encourageCounts.put(endpoint, new AtomicLong(1));
			this.initiativeRecordMap.put(keyword, record);
		}
	}

	/**
	 * 记录负回馈发送记录。
	 * 
	 * @param keyword 指定关键字。
	 * @param endpoint 指定终端。
	 */
	public void recordDiscourage(String keyword, Endpoint endpoint) {
		InitiativeRecord record = this.initiativeRecordMap.get(keyword);
		if (null != record) {
			record.timestamp = Clock.currentTimeMillis();

			// 记录时间
			synchronized (record.lastDiscourageTime) {
				AtomicLong time = record.lastDiscourageTime.get(endpoint);
				if (null != time) {
					time.set(Clock.currentTimeMillis());
				}
				else {
					record.lastDiscourageTime.put(endpoint, new AtomicLong(Clock.currentTimeMillis()));
				}
			}

			// 记录次数
			synchronized (record.discourageCounts) {
				AtomicLong counts = record.discourageCounts.get(endpoint);
				if (null != counts) {
					counts.incrementAndGet();
				}
				else {
					record.discourageCounts.put(endpoint, new AtomicLong(1));
				}
			}
		}
		else {
			record = new InitiativeRecord();
			record.lastDiscourageTime.put(endpoint, new AtomicLong(Clock.currentTimeMillis()));
			record.discourageCounts.put(endpoint, new AtomicLong(1));
			this.initiativeRecordMap.put(keyword, record);
		}
	}

	/**
	 * 判断指定关键字在指定终端上是否需要压制正回馈。以便减少不必要的数据发送。
	 * 
	 * @param keyword 指定关键字。
	 * @param endpoint 指定终端。
	 * @return 如果需要压制则返回 true ，否则返回 false 。
	 */
	public boolean isInhibitiveEncourage(String keyword, Endpoint endpoint) {
		InitiativeRecord record = this.initiativeRecordMap.get(keyword);
		if (null == record) {
			return false;
		}

		AtomicLong time = null;
		synchronized (record.lastEncourageTime) {
			time = record.lastEncourageTime.get(endpoint);
			if (null == time) {
				return false;
			}
		}

		// 计算指定的时间间隔内的执行次数
		if (Clock.currentTimeMillis() - time.get() <= this.inhibitionInterval) {
			// 间隔小于设置值
			AtomicLong counts = null;
			synchronized (record.encourageCounts) {
				counts = record.encourageCounts.get(endpoint);
			}
			if (null != counts) {
				if (counts.get() >= this.inhibitionCounts) {
					// 发送次数大于等于设置门限
					return true;
				}
			}

			return false;
		}

		return false;
	}

	/**
	 * 判断指定关键字在指定终端上是否需要压制负回馈。以便减少不必要的数据发送。
	 * 
	 * @param keyword 指定关键字。
	 * @param endpoint 指定终端。
	 * @return 如果需要压制则返回 true ，否则返回 false 。
	 */
	public boolean isInhibitiveDiscourage(String keyword, Endpoint endpoint) {
		InitiativeRecord record = this.initiativeRecordMap.get(keyword);
		if (null == record) {
			return false;
		}

		AtomicLong time = null;
		synchronized (record.lastDiscourageTime) {
			time = record.lastDiscourageTime.get(endpoint);
			if (null == time) {
				return false;
			}
		}

		// 计算指定的时间间隔内的执行次数
		if (Clock.currentTimeMillis() - time.get() <= this.inhibitionInterval) {
			// 间隔小于设置值
			AtomicLong counts = null;
			synchronized (record.discourageCounts) {
				counts = record.discourageCounts.get(endpoint);
			}
			if (null != counts) {
				if (counts.get() >= this.inhibitionCounts) {
					// 发送次数大于等于设置门限
					return true;
				}
			}

			return false;
		}

		return false;
	}

	@Override
	public void run() {
		long time = Clock.currentTimeMillis();

		// 关键字超期检测

		Iterator<Entry<String, Feedback>> fiter = this.feedbackMap.entrySet().iterator();
		while (fiter.hasNext()) {
			Entry<String, Feedback> e = fiter.next();
			Feedback fb = e.getValue();
			if (time - fb.getTimestamp() >= this.keywordExpired) {
				fiter.remove();
				Logger.i(this.getClass(), "Remove expired feedback keyword: " + fb.getKeyword());
			}
		}

		// 记录超期检测

		Iterator<Entry<String, InitiativeRecord>> iter = this.initiativeRecordMap.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, InitiativeRecord> e = iter.next();
			InitiativeRecord record = e.getValue();

			if (time - record.timestamp >= this.keywordExpired) {
				String keyword = e.getKey();
				iter.remove();
				Logger.i(this.getClass(), "Remove expired initiative record keyword: " + keyword);
				continue;
			}

			synchronized (record.lastEncourageTime) {
				Iterator<Entry<Endpoint, AtomicLong>> reiter = record.lastEncourageTime.entrySet().iterator();
				while (reiter.hasNext()) {
					Entry<Endpoint, AtomicLong> re = reiter.next();
					AtomicLong lastTime = re.getValue();
					if (time - lastTime.get() > this.inhibitionExpired) {
						Endpoint endpoint = re.getKey();
						reiter.remove();
						synchronized (record.encourageCounts) {
							record.encourageCounts.remove(endpoint);
						}

						Logger.i(this.getClass(), "Remove expired initiative encourage record: " + endpoint.toString());
					}
				}
			}

			synchronized (record.lastDiscourageTime) {
				Iterator<Entry<Endpoint, AtomicLong>> reiter = record.lastDiscourageTime.entrySet().iterator();
				while (reiter.hasNext()) {
					Entry<Endpoint, AtomicLong> re = reiter.next();
					AtomicLong lastTime = re.getValue();
					if (time - lastTime.get() > this.inhibitionExpired) {
						Endpoint endpoint = re.getKey();
						reiter.remove();
						synchronized (record.discourageCounts) {
							record.discourageCounts.remove(endpoint);
						}

						Logger.i(this.getClass(), "Remove expired initiative discourage record: " + endpoint.toString());
					}
				}
			}
		}
	}


	/**
	 * 主动发送回馈记录。
	 * 
	 * @author Ambrose Xu
	 *
	 */
	protected class InitiativeRecord {

		/** 时间戳。 */
		protected long timestamp = System.currentTimeMillis();

		/** 最近一次发送给指定终端正回馈的时间戳。 */
		protected HashMap<Endpoint, AtomicLong> lastEncourageTime = new HashMap<Endpoint, AtomicLong>();
		/** 正回馈计数记录。 */
		protected HashMap<Endpoint, AtomicLong> encourageCounts = new HashMap<Endpoint, AtomicLong>();

		/** 最近一次发送给指定终端负回馈的时间戳。 */
		protected HashMap<Endpoint, AtomicLong> lastDiscourageTime = new HashMap<Endpoint, AtomicLong>();
		/** 负回馈计数记录。 */
		protected HashMap<Endpoint, AtomicLong> discourageCounts = new HashMap<Endpoint, AtomicLong>();

	}

}
