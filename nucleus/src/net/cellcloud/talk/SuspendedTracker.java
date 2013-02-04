/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.talk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import net.cellcloud.core.Cellet;

/** 被挂起的对端。
 * 
 * 系统在以下两种情况下将对端挂起：
 * 1、对端通知系统将其挂起时
 * 2、系统设置对端支持闪断重连后，当连接被断开时
 * 
 * @author Jiangwei Xu
 */
public final class SuspendedTracker {

	// 挂起的有效时长，单位：毫秒，默认：5分钟
	protected long liveDuration = 5 * 60 * 1000;

	private String tag;
	private long startTime;

	private ConcurrentHashMap<String, Record> records;

	protected SuspendedTracker(String tag) {
		this.tag = tag;
		this.records = new ConcurrentHashMap<String, Record>();
		this.startTime = System.currentTimeMillis();
	}

	/** 返回标签。
	 */
	public String getTag() {
		return this.tag;
	}

	/** 跟踪指定的 Cellet 。
	 */
	protected void track(Cellet cellet, int suspendMode) {
		if (this.records.containsKey(cellet.getFeature().getIdentifier())) {
			Record r = this.records.get(cellet.getFeature().getIdentifier());
			// 如果是主动模式，则不能重置为被动模式
			if (r.suspendMode == SuspendMode.PASSIVE) {
				r.suspendMode = suspendMode;
			}

			// 更新时间
			this.startTime = System.currentTimeMillis();
		}
		else {
			Record r = new Record(cellet, suspendMode);
			this.records.put(cellet.getFeature().getIdentifier(), r);
		}
	}

	/** 移除指定 Cellet 的记录。
	 */
	protected void retreat(Cellet cellet) {
		this.records.remove(cellet.getFeature().getIdentifier());
	}

	/** 是否存在指定的 Cellet 的记录。
	 */
	protected boolean exist(Cellet cellet) {
		return this.records.containsKey(cellet.getFeature().getIdentifier());
	}

	/** 缓存原语。
	 */
	protected void offerPrimitive(Cellet cellet, Long timestamp, Primitive primitive) {
		final Record r = this.records.get(cellet.getFeature().getIdentifier());
		if (null != r) {
			r.timestamps.offer(timestamp);
			r.primitives.offer(primitive);
		}
	}

	/** 按照匹配被动挂起方式推送原语给对端。
	 */
	protected boolean pollPrimitiveMatchMode(final TalkServiceDaemon daemon, final Cellet cellet,
			final int suspendMode, final long startTime) {
		final Record r = this.records.get(cellet.getFeature().getIdentifier());
		if (null != r && r.suspendMode == suspendMode) {
			daemon.joinTask(new Thread() {
				@Override
				public void run() {
					TalkService.getInstance().noticeResume(r.cellet, tag, r.timestamps, r.primitives, startTime);
				}
			});

			return true;
		}

		return false;
	}

	/** 是否超时。
	 */
	protected boolean isTimeout() {
		return (System.currentTimeMillis() - this.startTime) >= this.liveDuration;
	}

	protected List<Cellet> getCelletList () {
		List<Cellet> list = new ArrayList<Cellet>();
		Iterator<Record> iter = this.records.values().iterator();
		while (iter.hasNext()) {
			list.add(iter.next().cellet);
		}
		return list;
	}

	/** 记录封装类。
	 */
	protected class Record {
		protected int suspendMode = SuspendMode.PASSIVE;
		protected Cellet cellet = null;
		protected Queue<Long> timestamps = null;
		protected Queue<Primitive> primitives = null;

		protected Record(Cellet cellet, int suspendMode) {
			this.cellet = cellet;
			this.suspendMode = suspendMode;
			this.timestamps = new LinkedList<Long>();
			this.primitives = new LinkedList<Primitive>();
		}
	}
}
