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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ReadListener;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 用于支持跨域访问的 Request 。
 * 
 * @author Ambrose Xu
 * 
 */
public class CrossOriginHttpServletRequest implements HttpServletRequest {

	/** 原始的 Servlet 请求。 */
	private HttpServletRequest soul;
	/** 跨域的方法。 */
	private String method;
	/** 跨域访问的 URI 。 */
	private String uri;
	/** 跨域访问的 Cookie 。 */
	private String cookie;
	/** 请求的参数。 */
	private JSONObject parameters;
	/** 用于兼容处理的哑元输入流。 */
	private DummyServletInputStream inputStream;
	/** 哑元数据长度。 */
	private int length;

	/**
	 * 构造函数。
	 * 
	 * @param request HTTP Servlet 请求。
	 * @param method HTTP 方法名。
	 * @param uri HTTP URI 。
	 */
	public CrossOriginHttpServletRequest(HttpServletRequest request, String method, String uri) {
		this.soul = request;
		this.method = method;
		this.uri = uri;

		this.cookie = request.getParameter(HttpCrossDomainHandler.COOKIE);

		// 分析参数
		this.analyseParameters();

		// 分析模拟的 POST Body
		this.analysePostBody();
	}

	/**
	 * 分析参数。
	 */
	private void analyseParameters() {
		String parameters = this.soul.getParameter(HttpCrossDomainHandler.PARAMETERS);
		if (null != parameters) {
			try {
				this.parameters = new JSONObject(parameters);
			} catch (JSONException e) {
				Logger.log(CrossOriginHttpServletRequest.class, e, LogLevel.WARNING);
			}
		}
	}

	/**
	 * 分析模拟的 POST 请求消息体。
	 */
	private void analysePostBody() {
		String content = this.soul.getParameter(HttpCrossDomainHandler.BODY);
		if (null != content) {
			this.length = content.length();
			this.inputStream = new DummyServletInputStream(content);
		}
		else {
			this.length = 0;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AsyncContext getAsyncContext() {
		return this.soul.getAsyncContext();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getAttribute(String name) {
		return this.soul.getAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Enumeration<String> getAttributeNames() {
		return this.soul.getAttributeNames();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getCharacterEncoding() {
		return this.soul.getCharacterEncoding();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getContentLength() {
		return this.length;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getContentType() {
		return this.soul.getContentType();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DispatcherType getDispatcherType() {
		return this.soul.getDispatcherType();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ServletInputStream getInputStream() throws IOException {
		return this.inputStream;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getLocalAddr() {
		return this.soul.getLocalAddr();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getLocalName() {
		return this.soul.getLocalName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getLocalPort() {
		return this.soul.getLocalPort();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Locale getLocale() {
		return this.soul.getLocale();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Enumeration<Locale> getLocales() {
		return this.soul.getLocales();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getParameter(String name) {
		if (name.equals(HttpCrossDomainHandler.COOKIE)) {
			return this.cookie;
		}

		if (null == this.parameters) {
			return null;
		}

		String ret = null;
		try {
			ret = this.parameters.get(name).toString();
		} catch (JSONException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		}
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, String[]> getParameterMap() {
		if (null == this.parameters) {
			return null;
		}

		HashMap<String, String[]> result = new HashMap<String, String[]>();
		Iterator<Object> iter = this.parameters.keys();
		while (iter.hasNext()) {
			String key = iter.next().toString();
			try {
				Object value = this.parameters.get(key);
				result.put(key, new String[]{ value.toString() });
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Enumeration<String> getParameterNames() {
		if (null == this.parameters) {
			return null;
		}

		String[] names = JSONObject.getNames(this.parameters);
		Vector<String> list = new Vector<String>();
		for (String n : names) {
			list.add(n);
		}
		return list.elements();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getParameterValues(String name) {
		if (null == this.parameters) {
			return null;
		}

		String value = null;
		try {
			value = this.parameters.get(name).toString();
		} catch (JSONException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		}

		if (null != value) {
			return (new String[]{value});
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getProtocol() {
		return this.soul.getProtocol();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BufferedReader getReader() throws IOException {
		return this.soul.getReader();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRealPath(String arg0) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRemoteAddr() {
		return this.soul.getRemoteAddr();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRemoteHost() {
		return this.soul.getRemoteHost();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getRemotePort() {
		return this.soul.getRemotePort();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RequestDispatcher getRequestDispatcher(String name) {
		return this.soul.getRequestDispatcher(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getScheme() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getServerName() {
		return this.soul.getServerName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getServerPort() {
		return this.soul.getServerPort();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ServletContext getServletContext() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAsyncStarted() {
		return this.soul.isAsyncStarted();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAsyncSupported() {
		return this.soul.isAsyncSupported();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSecure() {
		return this.soul.isSecure();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeAttribute(String attrName) {
		this.soul.removeAttribute(attrName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAttribute(String name, Object value) {
		this.soul.setAttribute(name, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setCharacterEncoding(String value)
			throws UnsupportedEncodingException {
		this.soul.setCharacterEncoding(value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		return this.soul.startAsync();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AsyncContext startAsync(ServletRequest request, ServletResponse response)
			throws IllegalStateException {
		return this.soul.startAsync(request, response);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean authenticate(HttpServletResponse response)
			throws IOException, ServletException {
		return this.soul.authenticate(response);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAuthType() {
		return this.soul.getAuthType();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getContextPath() {
		return this.soul.getContextPath();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cookie[] getCookies() {
		return this.soul.getCookies();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getDateHeader(String name) {
		return this.soul.getDateHeader(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getHeader(String name) {
		return this.soul.getHeader(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Enumeration<String> getHeaderNames() {
		return this.soul.getHeaderNames();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Enumeration<String> getHeaders(String name) {
		return this.soul.getHeaders(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getIntHeader(String name) {
		return this.soul.getIntHeader(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMethod() {
		return this.method;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Part getPart(String arg0) throws IOException, ServletException {
		return this.soul.getPart(arg0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		return this.soul.getParts();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPathInfo() {
		return this.getPathInfo();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPathTranslated() {
		return this.getPathTranslated();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getQueryString() {
		return this.getQueryString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRemoteUser() {
		return this.getRemoteUser();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRequestURI() {
		return this.uri;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public StringBuffer getRequestURL() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRequestedSessionId() {
		return this.soul.getRequestedSessionId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getServletPath() {
		return this.soul.getServletPath();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HttpSession getSession() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HttpSession getSession(boolean arg0) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Principal getUserPrincipal() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return this.soul.isRequestedSessionIdFromCookie();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isRequestedSessionIdFromURL() {
		return this.soul.isRequestedSessionIdFromURL();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isRequestedSessionIdFromUrl() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isRequestedSessionIdValid() {
		return this.soul.isRequestedSessionIdValid();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isUserInRole(String arg0) {
		return this.soul.isUserInRole(arg0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void login(String arg0, String arg1) throws ServletException {			
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void logout() throws ServletException {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getContentLengthLong() {
		return this.soul.getContentLengthLong();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String changeSessionId() {
		return this.soul.changeSessionId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0)
			throws IOException, ServletException {
		return this.soul.upgrade(arg0);
	}

	/**
	 * 封装的 Servlet 流。
	 * 
	 * @author Ambrose Xu
	 *
	 */
	protected class DummyServletInputStream extends ServletInputStream {

		private ByteArrayInputStream inputStream;

		protected DummyServletInputStream(String content) {
			this.inputStream = new ByteArrayInputStream(content.getBytes(Charset.forName("UTF-8")));
		}

		@Override
		public int read() throws IOException {
			return this.inputStream.read();
		}

		@Override
		public int read(byte[] b) throws IOException {
			return this.inputStream.read(b);
		}

		@Override
		public int read(byte[] b, int off, int len) {
			return this.inputStream.read(b, off, len);
		}

		@Override
		public boolean isFinished() {
			return true;
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setReadListener(ReadListener listener) {
		}

	}

}
