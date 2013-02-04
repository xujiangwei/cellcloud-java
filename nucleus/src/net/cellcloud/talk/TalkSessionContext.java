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

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.cellcloud.common.Session;

/** Talk 会话上下文。
 * 
 * @author Jiangwei Xu
 */
public final class TalkSessionContext {

	private Session session;

	/// Key：内核标签，Value：追踪器。
	private ConcurrentHashMap<String, TalkTracker> trackers;

	public long tickTime = 0;

	/** 构造函数。
	 */
	public TalkSessionContext(Session session) {
		this.session = session;
		this.trackers = new ConcurrentHashMap<String, TalkTracker>();
	}

	/** 返回上下文对应的 Session 。
	 */
	public Session getSession() {
		return this.session;
	}

	/** 返回所有 Tracker 。
	 */
	public Map<String, TalkTracker> getTrackers() {
		return this.trackers;
	}

	/** 返回指定 Tag 的 Tracker 。
	 */
	public TalkTracker getTracker(final String tag) {
		return this.trackers.get(tag);
	}

	/** 添加 Tracker 。
	 */
	public TalkTracker addTracker(final String tag, final InetSocketAddress address) {
		if (this.trackers.containsKey(tag)) {
			this.trackers.remove(tag);
		}

		TalkTracker tracker = new TalkTracker(tag, address);
		this.trackers.put(tag, tracker);
		return tracker;
	}

	/** 删除 Tracker 。
	 */
	public void removeTracker(final String tag) {
		this.trackers.remove(tag);
	}
}
