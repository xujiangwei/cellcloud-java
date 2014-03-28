/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2013 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.http;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.http.HttpHeader;

/**
 * 基于 Cookie 的会话管理器。
 * 
 * @author Jiangwei Xu
 *
 */
public class CookieSessionManager implements SessionManager {

	private static final String COOKIE = HttpHeader.COOKIE.asString();

	private long sessionExpires;
	private int maxSessionNum;
	private ConcurrentHashMap<Long, HttpSession> sessions;

	// 监听器列表
	private ArrayList<SessionListener> listeners;

	private volatile long maintainTime;

	public CookieSessionManager() {
		// 默认会话有效期：12 小时
		this.sessionExpires = 12 * 60 * 60 * 1000;
		this.maxSessionNum = 5000;
		this.sessions = new ConcurrentHashMap<Long, HttpSession>();
		this.maintainTime = System.currentTimeMillis();
		this.listeners = new ArrayList<SessionListener>(1);
	}

	@Override
	public void manage(HttpRequest request, HttpResponse response) {
		// 获取 Cookie
		String cookie = null;

		if (!request.isCrossDomain()) {
			// 非跨域
			cookie = request.getHeader(COOKIE);
		}
		else {
			// 跨域
			cookie = request.getParameter(HttpCrossDomainHandler.COOKIE);
		}

		if (null != cookie) {
			// 注入 Cookie 到 Request，管理 Session 时用于索引 Session 实例
			request.setAttribute(COOKIE, cookie);

			Long sessionId = this.readSessionId(cookie);
			if (sessionId.longValue() > 0) {
				// 判断是否是已被管理 Session
				if (!this.sessions.containsKey(sessionId)) {
					// 添加管理
					HttpSession session = new HttpSession(sessionId.longValue(), request.getRemoteAddr(), this.sessionExpires);
					this.sessions.put(sessionId, session);
				}

				return;
			}
		}

		// 创建会话
		HttpSession session = new HttpSession(request.getRemoteAddr(), this.sessionExpires);
		this.sessions.put(session.getId(), session);

		cookie = "SID=" + session.getId().toString();
		// 设置 Cookie
		if (!request.isCrossDomain()) {
			response.setCookie(cookie);
		}
		else {
			response.setCrossCookie(cookie);
		}

		// 注入 Cookie 到 Request，管理 Session 时用于索引 Session 实例
		request.setAttribute(COOKIE, cookie);

		// 分发事件
		this.dispatchCreate(session);

		// 是否需要进行 Session 维护
		if (this.sessions.size() > this.maxSessionNum) {
			long time = System.currentTimeMillis();
			// 维护最小间隔时间：30 分钟
			if (time - this.maintainTime > 1800000) {
				this.maintainTime = time;
				SessionMaintainTask task = new SessionMaintainTask();
				// 启动维护线程
				task.start();
			}
		}
	}

	@Override
	public void unmanage(HttpRequest request) {
		// 获取 Cookie
		String cookie = request.getHeader(COOKIE);
		if (null != cookie) {
			Long sessionId = this.readSessionId(cookie);
			if (sessionId.longValue() > 0) {
				// 判断是否是已被管理 Session
				if (this.sessions.containsKey(sessionId)) {
					// 解除管理
					HttpSession session = this.sessions.remove(sessionId);

					// 分发事件
					this.dispatchDestroy(session);
				}
			}
		}
	}

	@Override
	public void unmanage(HttpSession session) {
		Long sessionId = session.getId();
		if (this.sessions.containsKey(sessionId)) {
			this.sessions.remove(sessionId);

			// 分发事件
			this.dispatchDestroy(session);
		}
	}

	@Override
	public List<HttpSession> getSessions() {
		ArrayList<HttpSession> ret = new ArrayList<HttpSession>(this.sessions.size());
		for (HttpSession session : this.sessions.values()) {
			ret.add(session);
		}
		return ret;
	}

	@Override
	public HttpSession getSession(Long id) {
		return this.sessions.get(id);
	}

	@Override
	public HttpSession getSession(HttpRequest request) {
		// 获取 Cookie
		String cookie = request.getHeader(COOKIE);
		if (null != cookie) {
			Long sessionId = this.readSessionId(cookie);
			if (sessionId.longValue() > 0) {
				// 返回 Session
				return this.sessions.get(sessionId);
			}
		}
		else {
			// 检查属性
			String attrCookie = (String) request.getAttribute(COOKIE);
			if (null != attrCookie) {
				Long sessionId = this.readSessionId(attrCookie);
				if (sessionId.longValue() > 0) {
					// 返回 Session
					return this.sessions.get(sessionId);
				}
			}
		}

		return null;
	}

	@Override
	public boolean hasSession(Long id) {
		return this.sessions.containsKey(id);
	}

	@Override
	public void addSessionListener(SessionListener listener) {
		synchronized (this.listeners) {
			if (!this.listeners.contains(listener)) {
				this.listeners.add(listener);
			}
		}
	}

	@Override
	public void removeSessionListener(SessionListener listener) {
		synchronized (this.listeners) {
			this.listeners.remove(listener);
		}
	}

	private void dispatchCreate(HttpSession session) {
		synchronized (this.listeners) {
			for (SessionListener listener : this.listeners) {
				listener.onCreate(session);
			}
		}
	}

	private void dispatchDestroy(HttpSession session) {
		synchronized (this.listeners) {
			for (SessionListener listener : this.listeners) {
				listener.onDestroy(session);
			}
		}
	}

	/**
	 * 读取 Cookie 里的 Session ID
	 * @param cookie
	 * @return
	 */
	private long readSessionId(String cookie) {
		String[] array = cookie.split("=");
		if (null == array || array.length != 2) {
			return -1;
		}

		String sessionId = array[1].trim();
		long ret = -1;
		try {
			ret = Long.parseLong(sessionId);
		} catch (NumberFormatException e) {
			// Nothing
		}

		return ret;
	}


	/**
	 * Session 维护任务。
	 */
	protected final class SessionMaintainTask extends Thread {
		protected SessionMaintainTask() {
			super(SessionMaintainTask.class.getName());
		}

		@Override
		public void run() {
			long time = System.currentTimeMillis();

			// 依次删除所有超出有效期的会话
			Iterator<Map.Entry<Long, HttpSession>> iter = sessions.entrySet().iterator();
			while (iter.hasNext()) {
				HttpSession session = iter.next().getValue();
				if (time - session.getTimestamp() > session.getExpires()) {
					iter.remove();

					// 分发事件
					dispatchDestroy(session);
				}
			}
		}
	}
}
