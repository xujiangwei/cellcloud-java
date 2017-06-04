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
import java.util.concurrent.atomic.AtomicLong;

import net.cellcloud.common.Session;
import net.cellcloud.core.Endpoint;
import net.cellcloud.core.Role;
import net.cellcloud.util.Clock;

/**
 * Talk 会话上下文。
 * 
 * @author Ambrose Xu
 * 
 */
public final class TalkSessionContext {

	/**
	 * 存储当前上下文包含的所有 {@link Session} 列表。
	 */
	private LinkedList<Session> sessions;
	
	/**
	 * 上下文关联的终端 {@link Endpoint} 描述。
	 */
	private LinkedList<Endpoint> endpoints;

	/**
	 * {@link Session} 对应的心跳时间戳。
	 */
	private ConcurrentHashMap<Long, AtomicLong> sessionHeartbeats;

	/**
	 * {@link Session} 对应的对话追踪器。
	 */
	private ConcurrentHashMap<Long, TalkTracker> sessionTrackers;

	/**
	 * 存储 {@link TalkTracker} 的列表。
	 */
	private ArrayList<TalkTracker> trackerList;

	/**
	 * 上下文对应的内核标签。
	 */
	private String tag;

	/**
	 * 对话事件发生的时间戳。
	 */
	protected long dialogueTickTime = 0;

	/**
	 * 构造函数。
	 * 
	 * @param tag 内核标签。
	 * @param session 网络 Session 。
	 */
	public TalkSessionContext(String tag, Session session) {
		this.tag = tag;

		this.sessions = new LinkedList<Session>();
		this.sessions.add(session);

		this.endpoints = new LinkedList<Endpoint>();
		this.endpoints.add(new Endpoint(tag, Role.CONSUMER,
				session.getAddress().getHostString(), session.getAddress().getPort()));

		this.sessionHeartbeats = new ConcurrentHashMap<Long, AtomicLong>();
		this.sessionHeartbeats.put(session.getId(), new AtomicLong(Clock.currentTimeMillis()));

		this.sessionTrackers = new ConcurrentHashMap<Long, TalkTracker>();
		this.trackerList = new ArrayList<TalkTracker>();

		TalkTracker tracker = new TalkTracker(session);
		this.sessionTrackers.put(session.getId(), tracker);
		this.trackerList.add(tracker);
	}

	/**
	 * 获得所有 Session 列表。
	 * 
	 * @return 返回 Session 列表。
	 */
	public List<Session> getSessions() {
		return this.sessions;
	}

	/**
	 * 获得指定 Session 的心跳时间戳。
	 * 
	 * @param session 指定网络 Session 。
	 * @return 返回 Session 的心跳时间戳。
	 */
	public long getSessionHeartbeat(Session session) {
		synchronized (this.sessions) {
			AtomicLong v = this.sessionHeartbeats.get(session.getId());
			if (null == v) {
				return 0;
			}
			return v.get();
		}
	}

	/**
	 * 获得指定 Session 的追踪器。
	 * 
	 * @param session 指定网络 Session 。
	 * @return 返回 Session 的追踪器。
	 */
	public TalkTracker getTracker(Session session) {
		synchronized (this.sessions) {
			return this.sessionTrackers.get(session.getId());
		}
	}

	/**
	 * 获得所有追踪器列表。
	 * 
	 * @return 返回所有追踪器列表。
	 */
	public List<TalkTracker> getTrackers() {
		return this.trackerList;
	}

	/**
	 * 添加 Session 到上下文里。
	 * 
	 * @param session 待添加的新的网络 Session 。
	 */
	public void addSession(Session session) {
		synchronized (this.sessions) {
			if (this.sessions.contains(session)) {
				return;
			}

			this.sessions.add(session);

			this.endpoints.add(new Endpoint(this.tag, Role.CONSUMER,
					session.getAddress().getHostString(), session.getAddress().getPort()));

			this.sessionHeartbeats.put(session.getId(), new AtomicLong(Clock.currentTimeMillis()));

			TalkTracker tracker = new TalkTracker(session);
			this.trackerList.add(tracker);
			this.sessionTrackers.put(session.getId(), tracker);
		}
	}

	/**
	 * 从上下文里移除 Session 。
	 * 
	 * @param session 待移除的网络 Session 。
	 */
	public void removeSession(Session session) {
		synchronized (this.sessions) {
			int index = this.sessions.indexOf(session);
			if (index >= 0) {
				this.endpoints.remove(index);
			}
			this.sessions.remove(session);
			this.sessionHeartbeats.remove(session.getId());
			TalkTracker tracker = this.sessionTrackers.remove(session.getId());
			if (null != tracker) {
				this.trackerList.remove(tracker);
			}
		}
	}

	/**
	 * 获得当前上下文里的 Session 数量。
	 * 
	 * @return 返回当前上下文里的 Session 数量。
	 */
	public int numSessions() {
		synchronized (this.sessions) {
			return this.sessions.size();
		}
	}

	/**
	 * 更新 {@link Session} 的心跳时间戳。
	 * 
	 * @param session 指定需更新的 Session 。
	 * @param time 指定新的时间戳。
	 */
	public void updateSessionHeartbeat(Session session, long time) {
		synchronized (this.sessions) {
			if (this.sessions.isEmpty()) {
				return;
			}

			// 更新值
			AtomicLong value = this.sessionHeartbeats.get(session.getId());
			if (null == value) {
				this.sessionHeartbeats.put(session.getId(), new AtomicLong(time));
			}
			else {
				value.set(time);
			}
		}
	}

	/**
	 * 获得对应的内核标签。
	 * 
	 * @return 返回该上下文的内核标签。
	 */
	public String getTag() {
		return this.tag;
	}

	/**
	 * 获得指定会话的终端信息描述。
	 * 
	 * @param session 指定待查找的会话。
	 * @return 返回指定会话的终端信息描述。
	 */
	public Endpoint getEndpoint(Session session) {
		synchronized (this.sessions) {
			int index = this.sessions.indexOf(session);
			if (index >= 0) {
				return this.endpoints.get(index);
			}
		}

		return null;
	}

	/**
	 * 获得对应的终端信息描述列表。
	 * 
	 * @return 返回终端信息列表。
	 */
	public List<Endpoint> getEndpointList() {
		return this.endpoints;
	}

}
