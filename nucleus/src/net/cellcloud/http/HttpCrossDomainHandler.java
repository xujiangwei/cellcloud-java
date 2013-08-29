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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

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

	private HttpService service;

	public HttpCrossDomainHandler(HttpService service) {
		this.service = service;
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
	protected void doGet(HttpRequest request, HttpResponse response)
			throws IOException {
		String uri = request.getParameter(URI);
		CapsuleHolder holder = this.service.holders.get(uri);
		if (null == holder) {
			this.respond(response, HttpResponse.SC_BAD_REQUEST);
			return;
		}

		// 回调
		String callback = request.getParameter(CALLBACK);

		// 创建响应
		CrossOriginHttpServletResponse cor = createHttpServletResponse(response, callback);

		String method = request.getParameter(METHOD);
		if (method.equalsIgnoreCase(HttpMethod.GET.asString())) {
			try {
				holder.getHttpHandler().handle(uri, createBaseRequest()
						, createHttpServletRequest(request, HttpMethod.GET.asString(), uri)
						, cor);
			} catch (ServletException e) {
				Logger.log(getClass(), e, LogLevel.WARNING);
			}
		}
		else if (method.equalsIgnoreCase(HttpMethod.POST.asString())) {
			try {
				holder.getHttpHandler().handle(uri, createBaseRequest()
						, createHttpServletRequest(request, HttpMethod.POST.asString(), uri)
						, cor);
			} catch (ServletException e) {
				Logger.log(getClass(), e, LogLevel.WARNING);
			}
		}
		else {
			this.respond(response, HttpResponse.SC_NOT_IMPLEMENTED);
			return;
		}

		// 处理响应数据内容
		cor.output();
	}

	@Override
	protected void doPost(HttpRequest request, HttpResponse response)
			throws IOException {
		this.doGet(request, response);
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
			Logger.log(AbstractJSONHandler.class, e, LogLevel.ERROR);
		} finally {
			try {
				out.close();
			} catch (Exception e) {
				// Nothing
			}
		}
	}

	private Request createBaseRequest() {
		BaseRequest request = new BaseRequest();
		return request;
	}

	private HttpServletRequest createHttpServletRequest(HttpRequest request, String method, String uri) {
		CrossOriginHttpServletRequest ret = new CrossOriginHttpServletRequest(request.request, method, uri);
		return ret;
	}

	private CrossOriginHttpServletResponse createHttpServletResponse(HttpResponse response, String callback) {
		CrossOriginHttpServletResponse ret = new CrossOriginHttpServletResponse(response.response, callback);
		return ret;
	}


	/**
	 * 基础 Request 。
	 * @author Jiangwei Xu
	 */
	protected final class BaseRequest extends Request {
		public BaseRequest() {
			super(null, null);
		}
	}
}
