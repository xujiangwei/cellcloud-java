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

package net.cellcloud.adapter;

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
 */
public class FeedbackController extends TimerTask {

	private ConcurrentHashMap<String, Feedback> feedbackMap;

	private ConcurrentHashMap<String, InitiativeRecord> initiativeRecordMap;

	/**
	 * 关键字超期时间。
	 */
	private long keywordExpired = 12L * 60L * 60L * 1000L;

	private long inhibitionInterval = 3L * 60L * 1000L;
	private long inhibitionCounts = 3L;

	private long inhibitionExpired = 30L * 60L * 1000L;

	private Timer timer;

	public FeedbackController() {
		this.feedbackMap = new ConcurrentHashMap<String, Feedback>();
		this.initiativeRecordMap = new ConcurrentHashMap<String, InitiativeRecord>();
	}

	public void start() {
		if (null == this.timer) {
			this.timer = new Timer(this.getClass().getSimpleName() + "-Timer");
			this.timer.schedule(this, 10000L, 60000L);
		}
	}

	public void stop() {
		if (null != this.timer) {
			this.timer.cancel();
			this.timer = null;
		}
	}

	public int getPositiveCounts(String keyword, Endpoint endpoint) {
		Feedback feedback = this.feedbackMap.get(keyword);
		if (null == feedback) {
			return 0;
		}

		return feedback.countPositive(endpoint);
	}

	public int getNegativeCounts(String keyword, Endpoint endpoint) {
		Feedback feedback = this.feedbackMap.get(keyword);
		if (null == feedback) {
			return 0;
		}

		return feedback.countNegative(endpoint);
	}

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
	 * 
	 * @author Ambrose Xu
	 *
	 */
	protected class InitiativeRecord {

		protected long timestamp = System.currentTimeMillis();

		protected HashMap<Endpoint, AtomicLong> lastEncourageTime = new HashMap<Endpoint, AtomicLong>();
		protected HashMap<Endpoint, AtomicLong> encourageCounts = new HashMap<Endpoint, AtomicLong>();

		protected HashMap<Endpoint, AtomicLong> lastDiscourageTime = new HashMap<Endpoint, AtomicLong>();
		protected HashMap<Endpoint, AtomicLong> discourageCounts = new HashMap<Endpoint, AtomicLong>();

	}

}
