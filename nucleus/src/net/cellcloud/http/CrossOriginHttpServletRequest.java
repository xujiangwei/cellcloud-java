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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
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
import javax.servlet.http.Part;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 用于支持跨域访问的 Request 。
 * 
 * @author Jiangwei Xu
 */
public class CrossOriginHttpServletRequest implements HttpServletRequest {

	private HttpServletRequest soul;
	private String method;
	private String uri;
	private String cookie;
	private JSONObject parameters;
	private DummyServletInputStream inputStream;
	private int length;

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

	private void analysePostBody() {
		String content = this.soul.getParameter(HttpCrossDomainHandler.BODY);
		if (null != content) {
			this.length = content.length();
			System.out.println("content:" + content);
			this.inputStream = new DummyServletInputStream(content);
		}
		else {
			this.length = 0;
		}
	}

	@Override
	public AsyncContext getAsyncContext() {
		return this.soul.getAsyncContext();
	}

	@Override
	public Object getAttribute(String name) {
		return this.soul.getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return this.soul.getAttributeNames();
	}

	@Override
	public String getCharacterEncoding() {
		return this.soul.getCharacterEncoding();
	}

	@Override
	public int getContentLength() {
		return this.length;
	}

	@Override
	public String getContentType() {
		return this.soul.getContentType();
	}

	@Override
	public DispatcherType getDispatcherType() {
		return this.soul.getDispatcherType();
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return this.inputStream;
	}

	@Override
	public String getLocalAddr() {
		return this.soul.getLocalAddr();
	}

	@Override
	public String getLocalName() {
		return this.soul.getLocalName();
	}

	@Override
	public int getLocalPort() {
		return this.soul.getLocalPort();
	}

	@Override
	public Locale getLocale() {
		return this.soul.getLocale();
	}

	@Override
	public Enumeration<Locale> getLocales() {
		return this.soul.getLocales();
	}

	/**
	 * @note 逻辑被重写。
	 * @param name
	 * @return
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

	@Override
	public Map<String, String[]> getParameterMap() {
		return null;
	}

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

	@Override
	public String getProtocol() {
		return this.soul.getProtocol();
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return this.soul.getReader();
	}

	@Override
	public String getRealPath(String arg0) {
		return null;
	}

	@Override
	public String getRemoteAddr() {
		return this.soul.getRemoteAddr();
	}

	@Override
	public String getRemoteHost() {
		return this.soul.getRemoteHost();
	}

	@Override
	public int getRemotePort() {
		return this.soul.getRemotePort();
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String name) {
		return this.soul.getRequestDispatcher(name);
	}

	@Override
	public String getScheme() {
		return null;
	}

	@Override
	public String getServerName() {
		return this.soul.getServerName();
	}

	@Override
	public int getServerPort() {
		return this.soul.getServerPort();
	}

	@Override
	public ServletContext getServletContext() {
		return null;
	}

	@Override
	public boolean isAsyncStarted() {
		return this.soul.isAsyncStarted();
	}

	@Override
	public boolean isAsyncSupported() {
		return this.soul.isAsyncSupported();
	}

	@Override
	public boolean isSecure() {
		return this.soul.isSecure();
	}

	@Override
	public void removeAttribute(String attrName) {
		this.soul.removeAttribute(attrName);
	}

	@Override
	public void setAttribute(String name, Object value) {
		this.soul.setAttribute(name, value);
	}

	@Override
	public void setCharacterEncoding(String value)
			throws UnsupportedEncodingException {
		this.soul.setCharacterEncoding(value);
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		return this.soul.startAsync();
	}

	@Override
	public AsyncContext startAsync(ServletRequest request, ServletResponse response)
			throws IllegalStateException {
		return this.soul.startAsync(request, response);
	}

	@Override
	public boolean authenticate(HttpServletResponse response)
			throws IOException, ServletException {
		return this.soul.authenticate(response);
	}

	@Override
	public String getAuthType() {
		return this.soul.getAuthType();
	}

	@Override
	public String getContextPath() {
		return this.soul.getContextPath();
	}

	@Override
	public Cookie[] getCookies() {
		return this.soul.getCookies();
	}

	@Override
	public long getDateHeader(String name) {
		return this.soul.getDateHeader(name);
	}

	@Override
	public String getHeader(String name) {
		return this.soul.getHeader(name);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return this.soul.getHeaderNames();
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		return this.soul.getHeaders(name);
	}

	@Override
	public int getIntHeader(String name) {
		return this.soul.getIntHeader(name);
	}

	@Override
	public String getMethod() {
		return this.method;
	}

	@Override
	public Part getPart(String arg0) throws IOException, ServletException {
		return this.soul.getPart(arg0);
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		return this.soul.getParts();
	}

	@Override
	public String getPathInfo() {
		return this.getPathInfo();
	}

	@Override
	public String getPathTranslated() {
		return this.getPathTranslated();
	}

	@Override
	public String getQueryString() {
		return this.getQueryString();
	}

	@Override
	public String getRemoteUser() {
		return this.getRemoteUser();
	}

	@Override
	public String getRequestURI() {
		return this.uri;
	}

	@Override
	public StringBuffer getRequestURL() {
		return null;
	}

	@Override
	public String getRequestedSessionId() {
		return this.soul.getRequestedSessionId();
	}

	@Override
	public String getServletPath() {
		return this.soul.getServletPath();
	}

	@Override
	public HttpSession getSession() {
		return null;
	}

	@Override
	public HttpSession getSession(boolean arg0) {
		return null;
	}

	@Override
	public Principal getUserPrincipal() {
		return null;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return this.soul.isRequestedSessionIdFromCookie();
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return this.soul.isRequestedSessionIdFromURL();
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return this.soul.isRequestedSessionIdValid();
	}

	@Override
	public boolean isUserInRole(String arg0) {
		return this.soul.isUserInRole(arg0);
	}

	@Override
	public void login(String arg0, String arg1) throws ServletException {			
	}

	@Override
	public void logout() throws ServletException {
	}

	/**
	 * 封装的 Servlet 流。
	 * 
	 * @author Jiangwei Xu
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
	}
}
