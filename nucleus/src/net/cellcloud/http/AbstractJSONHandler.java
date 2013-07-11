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
import javax.servlet.http.HttpServletResponse;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.json.JSONObject;

/**
 * 抽象层 JSON 格式 HTTP Handler 。
 * 
 * @author Jiangwei Xu
 * 
 */
public abstract class AbstractJSONHandler extends HttpHandler {

	public AbstractJSONHandler() {
		super();
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		String method = baseRequest.getMethod();
		if (method.equalsIgnoreCase(HttpMethod.GET.asString())) {
			doGet(request, response);
			baseRequest.setHandled(true);
		}
		else if (method.equalsIgnoreCase(HttpMethod.POST.asString())) {
			doPost(request, response);
			baseRequest.setHandled(true);
		}
		else if (method.equalsIgnoreCase(HttpMethod.PUT.asString())) {
			doPut(request, response);
			baseRequest.setHandled(true);
		}
		else if (method.equalsIgnoreCase(HttpMethod.DELETE.asString())) {
			doDelete(request, response);
			baseRequest.setHandled(true);
		}
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		this.respond(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		this.respond(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	protected void doPut(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		this.respond(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	protected void doDelete(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		this.respond(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * 按照指定状态码处理访问响应。
	 * @param response
	 * @param status
	 */
	protected void respond(HttpServletResponse response, int status) {
		response.setContentType("text/json");
		response.setCharacterEncoding("UTF-8");
		response.setStatus(status);

		PrintWriter out = null;
		try {
			out = response.getWriter();
			out.print("{}");
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

	/**
	 * 按照指定状态处理访问响应，并写入 JSON 数据。
	 * @param response
	 * @param status
	 * @param json
	 * @throws IOException
	 */
	protected void respond(HttpServletResponse response, int status, JSONObject json) throws IOException {
		response.setContentType("text/json");
		response.setCharacterEncoding("UTF-8");
		response.setStatus(status);

		PrintWriter out = null;
		try {
			out = response.getWriter();
			out.print(json.toString());
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				out.close();
			} catch (Exception e) {
				// Nothing
			}
		}
	}

	/**
	 * 按照 200 状态处理访问响应
	 * @param response
	 */
	protected void respondWithOk(HttpServletResponse response) {
		response.setContentType("text/json");
		response.setCharacterEncoding("UTF-8");
		response.setStatus(HttpServletResponse.SC_OK);

		PrintWriter out = null;
		try {
			out = response.getWriter();
			out.print("{}");
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

	/**
	 * 按照 200 状态处理访问响应，并写入 JSON 数据。
	 * @param response
	 * @param json
	 * @throws IOException
	 */
	protected void respondWithOk(HttpServletResponse response, JSONObject json) throws IOException {
		response.setContentType("text/json");
		response.setCharacterEncoding("UTF-8");
		response.setStatus(HttpServletResponse.SC_OK);

		PrintWriter out = null;
		try {
			out = response.getWriter();
			out.print(json.toString());
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				out.close();
			} catch (Exception e) {
				// Nothing
			}
		}
	}
}
