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

package net.cellcloud.http;

import java.util.List;

/**
 * HTTP Sessoin 管理器接口。
 * 
 * @author Ambrose Xu
 *
 */
public interface SessionManager {

	/**
	 * 管理指定的请求。
	 * 
	 * @param request HTTP 请求对象。
	 * @param response HTTP 应答对象。
	 */
	public void manage(HttpRequest request, HttpResponse response);

	/**
	 * 解除管理指定的请求。
	 * 
	 * @param request HTTP 请求对象。
	 */
	public void unmanage(HttpRequest request);

	/**
	 * 解除管理指定的会话。
	 * 
	 * @param session HTTP 会话。
	 */
	public void unmanage(HttpSession session);

	/**
	 * 获得当前被管理的连接会话总数量。
	 * 
	 * @return 返回当前被管理的连接会话总数量。
	 */
	public int getSessionNum();

	/**
	 * 获得所有连接会话列表。
	 * 
	 * @return 返回所有连接会话列表。
	 */
	public List<HttpSession> getSessions();

	/**
	 * 获得指定 ID 的连接会话。
	 * 
	 * @param id 指定会话的 ID 。
	 * @return 返回指定 ID 的连接会话。
	 */
	public HttpSession getSession(Long id);

	/**
	 * 获得指定请求的会话。
	 * 
	 * @param request 指定 HTTP 请求对象。
	 * @return 返回指定请求的会话。
	 */
	public HttpSession getSession(HttpRequest request);

	/**
	 * 是否存在指定 ID 的会话。
	 * 
	 * @param id 指定会话的 ID 。
	 * @return 如果存在指定 ID 的会话返回 <code>true</code> 。
	 */
	public boolean hasSession(Long id);

	/**
	 * 添加会话监听器。
	 * 
	 * @param listener 指定监听器。
	 */
	public void addSessionListener(SessionListener listener);

	/**
	 * 移除会话监听器。
	 * 
	 * @param listener 指定监听器。
	 */
	public void removeSessionListener(SessionListener listener);

}
