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

	private ConcurrentHashMap<Long, HttpSession> sessions;

	public CookieSessionManager() {
		this.sessions = new ConcurrentHashMap<Long, HttpSession>();
	}

	@Override
	public void manage(HttpRequest request, HttpResponse response) {
		// 获取 Cookie
		String cookie = request.getHeader(COOKIE);
		if (null != cookie) {
			Long sessionId = this.readSessionId(cookie);
			if (sessionId.longValue() > 0) {
				// 判断是否是已被管理 Session
				if (this.sessions.containsKey(sessionId)) {
					// 已被管理
					return;
				}
			}
		}

		// 创建会话
		HttpSession session = new HttpSession(request.getRemoteAddr());
		this.sessions.put(session.getId(), session);

		// 设置 Cookie
		response.setCookie("SID:" + session.getId().toString());
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
					// 已被管理
					this.sessions.remove(sessionId);
				}
			}
		}
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

		return null;
	}

	@Override
	public boolean hasSession(Long id) {
		return this.sessions.containsKey(id);
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
}
