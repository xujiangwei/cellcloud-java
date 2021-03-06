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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedOperation;

/**
 * HTTP 协议处理句柄。
 * 
 * @author Ambrose Xu
 *
 */
public abstract class HttpHandler implements Handler {

	private Server server;
	private SessionManager sessionManager;

	private HttpFilter filter;

	/**
	 * 构造函数。
	 */
	public HttpHandler() {
		super();
	}

	/**
	 * 获得该句柄的会话管理器。
	 * 
	 * @return 返回该句柄的会话管理器。
	 */
	public SessionManager getSessionManager() {
		return this.sessionManager;
	}

	/**
	 * 设置会话管理器。
	 * 
	 * @param sessionManager 指定会话管理器。
	 */
	protected void setSessionManager(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	/**
	 * 设置过滤器。
	 * 
	 * @param filter 指定过滤器。
	 */
	protected void setFilter(HttpFilter filter) {
		this.filter = filter;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		// 尝试调用过滤器
		if (null != this.filter && this.filter.doFilter(target, baseRequest, request, response)) {
			// 已处理
			baseRequest.setHandled(true);
			return;
		}

		HttpRequest httpRequest = new HttpRequest(request, this.sessionManager);
		HttpResponse httpResponse = new HttpResponse(response);

		// 进行会话管理
		if (null != this.sessionManager) {
			this.sessionManager.manage(httpRequest, httpResponse);
		}

		// FIXME 访问方法判断，这是 Jetty 9.0.x 版本的 BUG
		String method = request.getMethod();
		if (request.getContentLength() > 0) {
			method = HttpMethod.POST.asString();
		}

		// 服务器类型
		response.setHeader("Server", "Cell Cloud");
		response.setHeader("Access-Control-Allow-Origin", "*");

		if (method.equalsIgnoreCase(HttpMethod.GET.asString())) {
			doGet(httpRequest, httpResponse);
		}
		else if (method.equalsIgnoreCase(HttpMethod.POST.asString())) {
			doPost(httpRequest, httpResponse);
		}
		else if (method.equalsIgnoreCase(HttpMethod.OPTIONS.asString())) {
			doOptions(httpRequest, httpResponse);
		}
		else if (method.equalsIgnoreCase(HttpMethod.PUT.asString())) {
			doPut(httpRequest, httpResponse);
		}
		else if (method.equalsIgnoreCase(HttpMethod.DELETE.asString())) {
			doDelete(httpRequest, httpResponse);
		}

		// 已处理
		baseRequest.setHandled(true);

		if (null != httpRequest) {
			httpRequest.destroy();
			httpRequest = null;
		}
	}

	/**
	 * 执行 GET 方法。
	 * 
	 * @param request HTTP 请求。
	 * @param response HTTP 应答。
	 * 
	 * @throws IOException
	 */
	protected abstract void doGet(HttpRequest request, HttpResponse response) throws IOException;

	/**
	 * 执行 POST 方法。
	 * 
	 * @param request HTTP 请求。
	 * @param response HTTP 应答。
	 * 
	 * @throws IOException
	 */
	protected abstract void doPost(HttpRequest request, HttpResponse response) throws IOException;

	/**
	 * 执行 OPTIONS 方法。
	 * 
	 * @param request HTTP 请求。
	 * @param response HTTP 应答。
	 * 
	 * @throws IOException
	 */
	protected abstract void doOptions(HttpRequest request, HttpResponse response) throws IOException;

	/**
	 * 执行 PUT 方法。
	 * 
	 * @param request HTTP 请求。
	 * @param response HTTP 应答。
	 * 
	 * @throws IOException
	 */
	protected abstract void doPut(HttpRequest request, HttpResponse response) throws IOException;

	/**
	 * 执行 DELETE 方法。
	 * 
	 * @param request HTTP 请求。
	 * @param response HTTP 应答。
	 * 
	 * @throws IOException
	 */
	protected abstract void doDelete(HttpRequest request, HttpResponse response) throws IOException;

	@Override
	public void addLifeCycleListener(Listener listener) {
		// Nothing
	}

	@Override
	public void removeLifeCycleListener(Listener listener) {
		// Nothing
	}

	@Override
	public boolean isFailed() {
		return false;
	}

	@Override
	public boolean isRunning() {
		return true;
	}

	@Override
	public boolean isStarted() {
		return true;
	}

	@Override
	public boolean isStarting() {
		return true;
	}

	@Override
	public boolean isStopped() {
		return false;
	}

	@Override
	public boolean isStopping() {
		return false;
	}

	@Override
	@ManagedOperation(value = "Starts the instance", impact = "ACTION")
	public void start() throws Exception {
		// Nothing
	}

	@Override
	@ManagedOperation(value = "Stops the instance", impact = "ACTION")
	public void stop() throws Exception {
		// Nothing
	}

	@Override
	@ManagedOperation(value = "destroy associated resources", impact = "ACTION")
	public void destroy() {
		// Nothing
	}

	@Override
	@ManagedAttribute(value = "the jetty server for this handler", readonly = true)
	public Server getServer() {
		return this.server;
	}

	@Override
	public void setServer(Server server) {
		this.server = server;
	}

}
