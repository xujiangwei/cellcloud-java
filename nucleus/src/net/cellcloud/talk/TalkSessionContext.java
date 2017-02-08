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

package net.cellcloud.talk;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import net.cellcloud.common.Session;
import net.cellcloud.core.Endpoint;
import net.cellcloud.core.Role;
import net.cellcloud.util.Clock;

/** Talk 会话上下文。
 * 
 * @author Jiangwei Xu
 */
public final class TalkSessionContext {

	private LinkedList<Session> sessions;
	private ConcurrentHashMap<Long, Long> sessionHeartbeats;
	private ConcurrentHashMap<Long, TalkTracker> sessionTrackers;
	// 用于直接返回列表
	private ArrayList<TalkTracker> trackerList;

	private String tag;

	private Endpoint endpoint;

	protected long dialogueTickTime = 0;

	/** 构造函数。
	 */
	public TalkSessionContext(String tag, Session session) {
		this.tag = tag;

		this.sessions = new LinkedList<Session>();
		this.sessions.add(session);

		this.sessionHeartbeats = new ConcurrentHashMap<Long, Long>();
		this.sessionHeartbeats.put(session.getId(), Clock.currentTimeMillis());

		this.sessionTrackers = new ConcurrentHashMap<Long, TalkTracker>();
		this.trackerList = new ArrayList<TalkTracker>();

		TalkTracker tracker = new TalkTracker();
		this.sessionTrackers.put(session.getId(), tracker);
		this.trackerList.add(tracker);

		this.endpoint = new Endpoint(tag, Role.CONSUMER,
				session.getAddress().getHostName(), session.getAddress().getPort());
	}

	/** 返回 Session 会话列表。
	 * @return
	 */
	public List<Session> getSessions() {
		synchronized (this.sessions) {
			return this.sessions;
		}
	}

	public long getSessionHeartbeat(Session session) {
		synchronized (this.sessions) {
			Long v = this.sessionHeartbeats.get(session.getId());
			if (null == v) {
				return 0;
			}
			return v.longValue();
		}
	}

	public TalkTracker getTracker(Session session) {
		synchronized (this.sessions) {
			return this.sessionTrackers.get(session.getId());
		}
	}

	public List<TalkTracker> getTrackers() {
		return this.trackerList;
	}

	public void addSession(Session session) {
		synchronized (this.sessions) {
			if (this.sessions.contains(session)) {
				return;
			}

			this.sessions.add(session);
			this.sessionHeartbeats.put(session.getId(), Clock.currentTimeMillis());

			TalkTracker tracker = new TalkTracker();
			this.trackerList.add(tracker);
			this.sessionTrackers.put(session.getId(), tracker);
		}
	}

	public void removeSession(Session session) {
		synchronized (this.sessions) {
			this.sessions.remove(session);
			this.sessionHeartbeats.remove(session.getId());
			TalkTracker tracker = this.sessionTrackers.remove(session.getId());
			if (null != tracker) {
				this.trackerList.remove(tracker);
			}
		}
	}

	public int numSessions() {
		synchronized (this.sessions) {
			return this.sessions.size();
		}
	}

	public void updateSessionHeartbeat(Session session, long time) {
		synchronized (this.sessions) {
			if (this.sessions.isEmpty()) {
				return;
			}

			// 先删除
			this.sessionHeartbeats.remove(session.getId());

			// 再更新
			this.sessionHeartbeats.put(session.getId(), time);
		}
	}

	/**
	 * 返回标签。
	 * @return
	 */
	public String getTag() {
		return this.tag;
	}

	/** 返回终端。
	 */
	public Endpoint getEndpoint() {
		return this.endpoint;
	}

}
