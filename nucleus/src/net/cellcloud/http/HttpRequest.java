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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

import javax.servlet.http.HttpServletRequest;

/**
 * HTTP 请求描述。
 * 
 * @author Ambrose Xu
 *
 */
public class HttpRequest {

	/** 原始 HTTP Servlet 请求。 */
	protected HttpServletRequest request;

	/** 会话管理器。 */
	protected SessionManager sessionManager;

	/** 用于存储数据的输出流。 */
	private ByteArrayOutputStream dataStream;

	/** 是否是跨域请求。 */
	protected boolean crossDomain;

	/**
	 * 构造函数。
	 * 
	 * @param request 原始 Servlet 请求。
	 * @param sessionManager 会话管理器。
	 */
	protected HttpRequest(HttpServletRequest request, SessionManager sessionManager) {
		this.request = request;
		this.sessionManager = sessionManager;
		this.crossDomain = false;
	}

	/**
	 * 销毁此请求。
	 */
	protected void destroy() {
		this.request = null;
		this.sessionManager = null;
		this.dataStream = null;
		this.crossDomain = false;
	}

	/**
	 * 获得指定的 HTTP 包头对应的数据。
	 * 
	 * @param header 指定报头名。
	 * @return 返回指定的 HTTP 包头对应的数据。
	 */
	public String getHeader(String header) {
		return this.request.getHeader(header);
	}

	/**
	 * 获得指定名称的参数值。
	 * 
	 * @param name 指定参数名。
	 * @return 返回指定名称的参数值。
	 */
	public String getParameter(String name) {
		return this.request.getParameter(name);
	}

	/**
	 * 设置属性值。
	 * 
	 * @param name 指定属性名。
	 * @param value 指定属性值。
	 */
	public void setAttribute(String name, Object value) {
		this.request.setAttribute(name, value);
	}

	/**
	 * 获得指定名称的属性值。
	 * 
	 * @param name 指定属性名。
	 * @return 返回指定名称的属性值。
	 */
	public Object getAttribute(String name) {
		return this.request.getAttribute(name);
	}

	/**
	 * 获得该请求对应的会话。
	 * 
	 * @return 返回该请求对应的会话。
	 */
	public HttpSession getSession() {
		return this.sessionManager.getSession(this);
	}

	/**
	 * 获得访问端地址。
	 * 
	 * @return 返回访问端地址。
	 */
	public InetSocketAddress getRemoteAddr() {
		return new InetSocketAddress(this.request.getRemoteAddr(), this.request.getRemotePort());
	}

	/**
	 * 是否执行跨域处理。
	 * 
	 * @return 如果是跨域请求返回 <code>true</code> 。
	 */
	public boolean isCrossDomain() {
		return this.crossDomain;
	}

	/**
	 * 获得原始的 Servlet Request 对象。
	 * 
	 * @return 返回原始 Servlet Request 对象。
	 */
	public HttpServletRequest getServletRequest() {
		return this.request;
	}

	/**
	 * 读取请求数据。
	 * 
	 * @return 返回读取的数据。
	 * 
	 * @throws IOException
	 */
	public byte[] readRequestData() throws IOException {
		if (null != this.dataStream) {
			return this.dataStream.toByteArray();
		}

		try {
			this.dataStream = new ByteArrayOutputStream(1024);
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
