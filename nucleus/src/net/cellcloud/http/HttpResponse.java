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

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpHeader;

/**
 * HTTP 响应描述。
 * 
 * @author Jiangwei Xu
 *
 */
public class HttpResponse {

	// 100
	public static final int SC_CONTINUE = 100;
	// 101
	public static final int SC_SWITCHING_PROTOCOLS = 101;

	// 200
	public static final int SC_OK = 200;
	// 201
	public static final int SC_CREATED = 201;
	// 202
	public static final int SC_ACCEPTED = 202;
	// 203
	public static final int SC_NON_AUTHORITATIVE_INFORMATION = 203;
	// 204
	public static final int SC_NO_CONTENT = 204;
	// 205
	public static final int SC_RESET_CONTENT = 205;
	// 206
	public static final int SC_PARTIAL_CONTENT = 206;

	// 300
	public static final int SC_MULTIPLE_CHOICES = 300;
	// 301
	public static final int SC_MOVED_PERMANENTLY = 301;
	// 302
	public static final int SC_MOVED_TEMPORARILY = 302;
	// 302
	public static final int SC_FOUND = 302;
	// 303
	public static final int SC_SEE_OTHER = 303;
	// 304
	public static final int SC_NOT_MODIFIED = 304;
	// 305
	public static final int SC_USE_PROXY = 305;
	// 307
	public static final int SC_TEMPORARY_REDIRECT = 307;

	// 400
	public static final int SC_BAD_REQUEST = 400;
	// 401
	public static final int SC_UNAUTHORIZED = 401;
	// 402
	public static final int SC_PAYMENT_REQUIRED = 402;
	// 403
	public static final int SC_FORBIDDEN = 403;
	// 404
	public static final int SC_NOT_FOUND = 404;
	// 405
	public static final int SC_METHOD_NOT_ALLOWED = 405;
	// 406
	public static final int SC_NOT_ACCEPTABLE = 406;
	// 407
	public static final int SC_PROXY_AUTHENTICATION_REQUIRED = 407;
	// 408
	public static final int SC_REQUEST_TIMEOUT = 408;
	// 409
	public static final int SC_CONFLICT = 409;
	// 410
	public static final int SC_GONE = 410;
	// 411
	public static final int SC_LENGTH_REQUIRED = 411;
	// 412
	public static final int SC_PRECONDITION_FAILED = 412;
	// 413
	public static final int SC_REQUEST_ENTITY_TOO_LARGE = 413;
	// 414
	public static final int SC_REQUEST_URI_TOO_LONG = 414;
	// 415
	public static final int SC_UNSUPPORTED_MEDIA_TYPE = 415;
	// 416
	public static final int SC_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
	// 417
	public static final int SC_EXPECTATION_FAILED = 417;

	// 500
	public static final int SC_INTERNAL_SERVER_ERROR = 500;
	// 501
	public static final int SC_NOT_IMPLEMENTED = 501;
	// 502
	public static final int SC_BAD_GATEWAY = 502;
	// 503
	public static final int SC_SERVICE_UNAVAILABLE = 503;
	// 504
	public static final int SC_GATEWAY_TIMEOUT = 504;
	// 505
	public static final int SC_HTTP_VERSION_NOT_SUPPORTED = 505;


	private HttpServletResponse response;

	public HttpResponse(HttpServletResponse response) {
		this.response = response;
	}

	public void setContentType(String type) {
		this.response.setContentType(type);
	}

	public void setCharacterEncoding(String encoding) {
		this.response.setCharacterEncoding(encoding);
	}

	public void setStatus(int status) {
		this.response.setStatus(status);
	}

	public void setCookie(String cookie) {
		this.response.setHeader(HttpHeader.SET_COOKIE.asString(), cookie);
	}

	public void setHeader(String key, String name) {
		this.response.setHeader(key, name);
	}

	public PrintWriter getWriter() throws IOException {
		return this.response.getWriter();
	}
}
