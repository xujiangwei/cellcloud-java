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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * HTTP 协议处理句柄。
 * 
 * @author Jiangwei Xu
 *
 */
public abstract class HttpHandler extends AbstractHandler {

	private SessionManager sessionManager;

	public HttpHandler() {
		super();
	}

	/**
	 * 返回该句柄的会话管理器。
	 * @return
	 */
	public SessionManager getSessionManager() {
		return this.sessionManager;
	}

	/**
	 * 设置会话管理器。
	 * @param sessionManager
	 */
	protected void setSessionManager(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		HttpRequest httpRequest = new HttpRequest(request, this.sessionManager);
		HttpResponse httpResponse = new HttpResponse(response);

		// 进行会话管理
		if (null != this.sessionManager) {
			this.sessionManager.manage(httpRequest, httpResponse);
		}

		String method = baseRequest.getMethod();
		if (method.equalsIgnoreCase(HttpMethod.GET.asString())) {
			doGet(httpRequest, httpResponse);
			baseRequest.setHandled(true);
		}
		else if (method.equalsIgnoreCase(HttpMethod.POST.asString())) {
			doPost(httpRequest, httpResponse);
			baseRequest.setHandled(true);
		}
		else if (method.equalsIgnoreCase(HttpMethod.PUT.asString())) {
			doPut(httpRequest, httpResponse);
			baseRequest.setHandled(true);
		}
		else if (method.equalsIgnoreCase(HttpMethod.DELETE.asString())) {
			doDelete(httpRequest, httpResponse);
			baseRequest.setHandled(true);
		}

		if (null != httpRequest) {
			httpRequest.destroy();
			httpRequest = null;
		}
	}

	/**
	 * GET Method
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	protected abstract void doGet(HttpRequest request, HttpResponse response) throws IOException;

	/**
	 * POST Method
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	protected abstract void doPost(HttpRequest request, HttpResponse response) throws IOException;

	/**
	 * PUT Method
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	protected abstract void doPut(HttpRequest request, HttpResponse response) throws IOException;

	/**
	 * DELETE Method
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	protected abstract void doDelete(HttpRequest request, HttpResponse response) throws IOException;
}
