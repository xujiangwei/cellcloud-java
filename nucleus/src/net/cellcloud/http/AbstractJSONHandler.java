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
import java.io.PrintWriter;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;

import org.json.JSONObject;

/**
 * 抽象层 JSON 格式 HTTP Handler 。
 * 
 * @author Ambrose Xu
 * 
 */
public abstract class AbstractJSONHandler extends HttpHandler {

	/**
	 * 构造函数。
	 */
	public AbstractJSONHandler() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doGet(HttpRequest request, HttpResponse response)
		throws IOException {
		this.respond(response, HttpResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doPost(HttpRequest request, HttpResponse response)
		throws IOException {
		this.respond(response, HttpResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doOptions(HttpRequest request, HttpResponse response)
			throws IOException {
		this.respond(response, HttpResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doPut(HttpRequest request, HttpResponse response)
		throws IOException {
		this.respond(response, HttpResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doDelete(HttpRequest request, HttpResponse response)
		throws IOException {
		this.respond(response, HttpResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * 按照指定状态码处理访问响应。
	 * 
	 * @param response HTTP 应答。
	 * @param status HTTP 状态码。
	 */
	protected void respond(HttpResponse response, int status) {
		response.setContentType("application/json");
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
	 * 
	 * @param response HTTP 响应。
	 * @param status HTTP 状态码。
	 * @param json JSON 数据。
	 * 
	 * @throws IOException
	 */
	protected void respond(HttpResponse response, int status, JSONObject json) throws IOException {
		response.setContentType("application/json");
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
	 * 按照 200 状态处理访问响应。
	 * 
	 * @param response HTTP 响应。
	 */
	protected void respondWithOk(HttpResponse response) {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setStatus(HttpResponse.SC_OK);

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
	 * 
	 * @param response HTTP 响应。
	 * @param json JSON 数据。
	 * 
	 * @throws IOException
	 */
	protected void respondWithOk(HttpResponse response, JSONObject json) throws IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setStatus(HttpResponse.SC_OK);

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
