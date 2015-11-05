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
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;

/**
 * HTTP 跨域支持。
 * 
 * @author Jiangwei Xu
 */
public final class HttpCrossDomainHandler extends HttpHandler implements CapsuleHolder {

	protected static final String URI = "u";
	protected static final String METHOD = "m";
	protected static final String PARAMETERS = "p";
	protected static final String BODY = "b";
	protected static final String CALLBACK = "c";
	protected static final String TIME = "t";
	protected static final String COOKIE = "_cookie";

	private HttpService service;

	private AtomicInteger concurrentCounts;

	public HttpCrossDomainHandler(HttpService service) {
		super();
		this.service = service;
		this.concurrentCounts = new AtomicInteger(0);
	}

	public int getConcurrentCounts() {
		return this.concurrentCounts.get();
	}

	@Override
	public String getPathSpec() {
		return "/cccd.js";
	}

	@Override
	public HttpHandler getHttpHandler() {
		return this;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		// 计数
		this.concurrentCounts.incrementAndGet();

		// 实际 URI
		String uri = request.getParameter(URI);
		CapsuleHolder holder = this.service.holders.get(uri);
		if (null == holder) {
			baseRequest.setHandled(true);
			this.respond(response, HttpResponse.SC_BAD_REQUEST);

			this.concurrentCounts.decrementAndGet();
			return;
		}

		// 会话管理器
		SessionManager sessionMgr = holder.getHttpHandler().getSessionManager();

		// 方法
		String method = request.getParameter(METHOD);

		// 创建跨域对象
		CrossDomainRequest httpRequest = new CrossDomainRequest(new CrossOriginHttpServletRequest(request, method, uri), sessionMgr);
		CrossDomainResponse httpResponse = new CrossDomainResponse(new CrossOriginHttpServletResponse(response, 1024));

		// 进行会话管理
		if (null != sessionMgr) {
			sessionMgr.manage(httpRequest, httpResponse);
		}

		// 服务器类型
		response.setHeader("Server", "Cell Cloud");
		// 允许跨域访问
		httpResponse.setHeader("Access-Control-Allow-Origin", "*");

		// 回调
		String callback = request.getParameter(CALLBACK);
		// 时间戳
		long timestamp = 0;
		try {
			timestamp = Long.parseLong(request.getParameter(TIME));
		} catch (Exception e) {
			// Nothing
		}

		if (method.equalsIgnoreCase(HttpMethod.GET.asString())) {
			try {
				holder.getHttpHandler().doGet(httpRequest, httpResponse);
			} catch (IOException e) {
				Logger.log(getClass(), e, LogLevel.WARNING);
			}
		}
		else if (method.equalsIgnoreCase(HttpMethod.POST.asString())) {
			try {
				holder.getHttpHandler().doPost(httpRequest, httpResponse);
			} catch (IOException e) {
				Logger.log(getClass(), e, LogLevel.WARNING);
			}
		}
		else {
			this.respond(httpResponse, HttpResponse.SC_NOT_IMPLEMENTED);

			this.concurrentCounts.decrementAndGet();
			return;
		}

		// 响应请求
		// httpResponse.crossCookie 的值是由会话管理器赋值，如果有值表示使用新的 Cookie
		httpResponse.respond(timestamp, callback, httpResponse.crossCookie);

		// 已处理
		baseRequest.setHandled(true);

		if (null != httpRequest) {
			httpRequest.destroy();
			httpRequest = null;
		}

		this.concurrentCounts.decrementAndGet();
	}

	@Override
	protected void doGet(HttpRequest request, HttpResponse response)
			throws IOException {
		this.respond(response, HttpResponse.SC_NOT_IMPLEMENTED);
	}

	@Override
	protected void doPost(HttpRequest request, HttpResponse response)
			throws IOException {
		this.respond(response, HttpResponse.SC_NOT_IMPLEMENTED);
	}

	@Override
	protected void doPut(HttpRequest request, HttpResponse response)
			throws IOException {
		this.respond(response, HttpResponse.SC_NOT_IMPLEMENTED);
	}

	@Override
	protected void doDelete(HttpRequest request, HttpResponse response)
			throws IOException {
		this.respond(response, HttpResponse.SC_NOT_IMPLEMENTED);
	}

	private void respond(HttpResponse response, int status) {
		response.setContentType("text/javascript");
		response.setCharacterEncoding("UTF-8");
		response.setStatus(status);

		PrintWriter out = null;
		try {
			out = response.getWriter();
			out.print("console.log(\"Talk service http cross-domain error.\");");
		} catch (IOException e) {
			Logger.log(HttpCrossDomainHandler.class, e, LogLevel.ERROR);
		} finally {
			try {
				out.close();
			} catch (Exception e) {
				// Nothing
			}
		}
	}

	private void respond(HttpServletResponse response, int status) {
		response.setContentType("text/javascript");
		response.setCharacterEncoding("UTF-8");
		response.setStatus(status);

		PrintWriter out = null;
		try {
			out = response.getWriter();
			out.print("console.log(\"Talk service http cross-domain error.\");");
		} catch (IOException e) {
			Logger.log(HttpCrossDomainHandler.class, e, LogLevel.ERROR);
		} finally {
			try {
				out.close();
			} catch (Exception e) {
				// Nothing
			}
		}
	}
}
