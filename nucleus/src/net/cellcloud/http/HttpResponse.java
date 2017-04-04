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

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpHeader;

/**
 * HTTP 响应描述。
 * 
 * @author Ambrose Xu
 *
 */
public class HttpResponse {

	/** HTTP 状态码 100 */
	public static final int SC_CONTINUE = 100;
	/** HTTP 状态码 101 */
	public static final int SC_SWITCHING_PROTOCOLS = 101;

	/** HTTP 状态码 200 */
	public static final int SC_OK = 200;
	/** HTTP 状态码 201 */
	public static final int SC_CREATED = 201;
	/** HTTP 状态码 202 */
	public static final int SC_ACCEPTED = 202;
	/** HTTP 状态码 203 */
	public static final int SC_NON_AUTHORITATIVE_INFORMATION = 203;
	/** HTTP 状态码 204 */
	public static final int SC_NO_CONTENT = 204;
	/** HTTP 状态码 205 */
	public static final int SC_RESET_CONTENT = 205;
	/** HTTP 状态码 206 */
	public static final int SC_PARTIAL_CONTENT = 206;

	/** HTTP 状态码 300 */
	public static final int SC_MULTIPLE_CHOICES = 300;
	/** HTTP 状态码 301 */
	public static final int SC_MOVED_PERMANENTLY = 301;
	/** HTTP 状态码 302 */
	public static final int SC_MOVED_TEMPORARILY = 302;
	/** HTTP 状态码 302 */
	public static final int SC_FOUND = 302;
	/** HTTP 状态码 303 */
	public static final int SC_SEE_OTHER = 303;
	/** HTTP 状态码 304 */
	public static final int SC_NOT_MODIFIED = 304;
	/** HTTP 状态码 305 */
	public static final int SC_USE_PROXY = 305;
	/** HTTP 状态码 307 */
	public static final int SC_TEMPORARY_REDIRECT = 307;

	/** HTTP 状态码 400 */
	public static final int SC_BAD_REQUEST = 400;
	/** HTTP 状态码 401 */
	public static final int SC_UNAUTHORIZED = 401;
	/** HTTP 状态码 402 */
	public static final int SC_PAYMENT_REQUIRED = 402;
	/** HTTP 状态码 403 */
	public static final int SC_FORBIDDEN = 403;
	/** HTTP 状态码 404 */
	public static final int SC_NOT_FOUND = 404;
	/** HTTP 状态码 405 */
	public static final int SC_METHOD_NOT_ALLOWED = 405;
	/** HTTP 状态码 406 */
	public static final int SC_NOT_ACCEPTABLE = 406;
	/** HTTP 状态码 407 */
	public static final int SC_PROXY_AUTHENTICATION_REQUIRED = 407;
	/** HTTP 状态码 408 */
	public static final int SC_REQUEST_TIMEOUT = 408;
	/** HTTP 状态码 409 */
	public static final int SC_CONFLICT = 409;
	/** HTTP 状态码 410 */
	public static final int SC_GONE = 410;
	/** HTTP 状态码 411 */
	public static final int SC_LENGTH_REQUIRED = 411;
	/** HTTP 状态码 412 */
	public static final int SC_PRECONDITION_FAILED = 412;
	/** HTTP 状态码 413 */
	public static final int SC_REQUEST_ENTITY_TOO_LARGE = 413;
	/** HTTP 状态码 414 */
	public static final int SC_REQUEST_URI_TOO_LONG = 414;
	/** HTTP 状态码 415 */
	public static final int SC_UNSUPPORTED_MEDIA_TYPE = 415;
	/** HTTP 状态码 416 */
	public static final int SC_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
	/** HTTP 状态码 417 */
	public static final int SC_EXPECTATION_FAILED = 417;

	/** HTTP 状态码 500 */
	public static final int SC_INTERNAL_SERVER_ERROR = 500;
	/** HTTP 状态码 501 */
	public static final int SC_NOT_IMPLEMENTED = 501;
	/** HTTP 状态码 502 */
	public static final int SC_BAD_GATEWAY = 502;
	/** HTTP 状态码 503 */
	public static final int SC_SERVICE_UNAVAILABLE = 503;
	/** HTTP 状态码 504 */
	public static final int SC_GATEWAY_TIMEOUT = 504;
	/** HTTP 状态码 505 */
	public static final int SC_HTTP_VERSION_NOT_SUPPORTED = 505;

	/** 原始的 HTTP Servlet 应答。  */
	protected HttpServletResponse response;

	/** 跨域的 Cookie。 */
	protected String crossCookie;

	/**
	 * 构造函数。
	 * 
	 * @param response 原始 Servlet 应答。
	 */
	public HttpResponse(HttpServletResponse response) {
		this.response = response;
		this.crossCookie = null;
	}

	/**
	 * 设置应答的内容类型。
	 * 
	 * @param type 指定字符串形式的内容类型。
	 */
	public void setContentType(String type) {
		this.response.setContentType(type);
	}

	/**
	 * 设置应答数据使用的字符编码集。
	 * 
	 * @param encoding 指定编码集。
	 */
	public void setCharacterEncoding(String encoding) {
		this.response.setCharacterEncoding(encoding);
	}

	/**
	 * 设置应答状态吗。
	 * 
	 * @param status 指定 HTTP 状态码。
	 */
	public void setStatus(int status) {
		this.response.setStatus(status);
	}

	/**
	 * 设置跨域的 Cookie 。
	 * 
	 * @param cookie 指定 Cookie 字符串。
	 */
	public void setCrossCookie(String cookie) {
		this.crossCookie = cookie;
	}

	/**
	 * 设置 Cookie 内容。
	 * 
	 * @param cookie 指定 Cookie 字符串。
	 */
	public void setCookie(String cookie) {
		this.response.setHeader(HttpHeader.SET_COOKIE.asString(), cookie);
	}

	/**
	 * 设置应答头数据。
	 * 
	 * @param key 指定应答头的键值。
	 * @param name 指定应答头的数据。
	 */
	public void setHeader(String key, String name) {
		this.response.setHeader(key, name);
	}

	/**
	 * 获得应答数据写入器。
	 * 
	 * @return 返回应答数据写入器。
	 * 
	 * @throws IOException
	 */
	public PrintWriter getWriter() throws IOException {
		return this.response.getWriter();
	}

	/**
	 * 获得原始的 Servlet Response 对象。
	 * 
	 * @return 返回原始 Servlet Response 对象。
	 */
	public HttpServletResponse getServletResponse() {
		return this.response;
	}

}
