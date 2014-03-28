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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

import javax.servlet.http.HttpServletRequest;

/**
 * HTTP 请求描述。
 * 
 * @author Jiangwei Xu
 *
 */
public class HttpRequest {

	protected HttpServletRequest request;
	protected SessionManager sessionManager;

	private ByteArrayOutputStream dataStream;

	protected boolean crossDomain;

	protected HttpRequest(HttpServletRequest request, SessionManager sessionManager) {
		this.request = request;
		this.sessionManager = sessionManager;
		this.crossDomain = false;
	}

	protected void destroy() {
		this.request = null;
		this.sessionManager = null;
		this.dataStream = null;
		this.crossDomain = false;
	}

	/**
	 * 返回指定的 HTTP 包头数据。
	 * @param header
	 * @return
	 */
	public String getHeader(String header) {
		return this.request.getHeader(header);
	}

	/**
	 * 返回指定名称的参数值。
	 * @param name
	 * @return
	 */
	public String getParameter(String name) {
		return this.request.getParameter(name);
	}

	/**
	 * 设置属性值。
	 * @param name
	 * @param value
	 */
	public void setAttribute(String name, Object value) {
		this.request.setAttribute(name, value);
	}

	/**
	 * 返回指定名称的属性值。
	 * @param name
	 * @return
	 */
	public Object getAttribute(String name) {
		return this.request.getAttribute(name);
	}

	/**
	 * 返回 Session 。
	 * @return
	 */
	public HttpSession getSession() {
		return this.sessionManager.getSession(this);
	}

	/**
	 * 返回访问端地址。
	 * @return
	 */
	public InetSocketAddress getRemoteAddr() {
		return new InetSocketAddress(this.request.getRemoteAddr(), this.request.getRemotePort());
	}

	/**
	 * 是否执行跨域处理。
	 * @return
	 */
	public boolean isCrossDomain() {
		return this.crossDomain;
	}

	/**
	 * 读取请求数据。
	 * @return
	 * @throws IOException
	 */
	public byte[] readRequestData() throws IOException {
		if (null != this.dataStream) {
			return this.dataStream.toByteArray();
		}

		try {
			this.dataStream = new ByteArrayOutputStream();
			InputStream is = this.request.getInputStream();
			if (null != is) {
				byte[] buffer = new byte[1024];
				int len = -1;
				while ((len = is.read(buffer)) != -1) {
					this.dataStream.write(buffer, 0, len);
				}
			}
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				this.dataStream.close();
			} catch (Exception e) {
				// Nothing
			}
		}

		return this.dataStream.toByteArray();
	}
}
