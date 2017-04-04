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
	 * @param request
	 * @param response
	 */
	public void manage(HttpRequest request, HttpResponse response);

	/**
	 * 解除管理指定的请求。
	 * 
	 * @param request
	 */
	public void unmanage(HttpRequest request);

	/**
	 * 解除管理指定的会话。
	 * 
	 * @param session
	 */
	public void unmanage(HttpSession session);

	/**
	 * 返回当前被管理的 Session 总数量。
	 * 
	 * @return
	 */
	public int getSessionNum();

	/**
	 * 返回所有 Session 列表。
	 * 
	 * @return
	 */
	public List<HttpSession> getSessions();

	/**
	 * 返回指定 ID 的 Session 。
	 * 
	 * @param id
	 * @return
	 */
	public HttpSession getSession(Long id);

	/**
	 * 返回指定请求的 Session 。
	 * 
	 * @param request
	 * @return
	 */
	public HttpSession getSession(HttpRequest request);

	/**
	 * 是否包含该 Session 。
	 * 
	 * @param id
	 * @return
	 */
	public boolean hasSession(Long id);

	/**
	 * 添加心跳监听器。
	 * 
	 * @param listener
	 */
	public void addSessionListener(SessionListener listener);

	/**
	 * 移除心跳监听器。
	 * 
	 * @param listener
	 */
	public void removeSessionListener(SessionListener listener);

}
