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
import java.io.StringWriter;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;

/**
 * 用于支持跨域访问的 Response 。
 * 
 * @author Ambrose Xu
 * 
 */
public class CrossOriginHttpServletResponse implements HttpServletResponse {

	/** 原始的 Servlet 响应。 */
	private HttpServletResponse soul;

	/** 响应内容 Writer 。 */
	private PrintWriter writer;
	private StringWriter stringWriter;

	/** 是否写入 Eval 。 */
	private boolean eval;

	/**
	 * 构造函数。
	 * 
	 * @param response HTTP 响应。
	 * @param block 初始缓存块大小。
	 */
	public CrossOriginHttpServletResponse(HttpServletResponse response, int block) {
		this.soul = response;
		this.stringWriter = new StringWriter(block);
		this.writer = new PrintWriter(this.stringWriter);
		this.eval = false;
	}

	/**
	 * 响应数据。
	 * 
	 * @param timestamp 时间戳。
	 * @param callback 回调执行的函数。
	 * @param newCookie 新的 Cookie 。
	 */
	public void respond(long timestamp, String callback, String newCookie) {
		StringBuffer buffer = this.stringWriter.getBuffer();

		try {
			PrintWriter writer = this.soul.getWriter();
			if (null != callback) {
				StringBuilder buf = new StringBuilder();

				// 返回的参数
				if (this.eval) {
					buf.append("var _rd = eval('(");
					buf.append(buffer.toString());
					buf.append(")');");
				}
				else {
					buf.append("var _rd = ");
					buf.append(buffer.toString());
					buf.append(";");
				}
				// 被执行的回调函数
				buf.append(callback);
				buf.append(".call(null,");
				buf.append(timestamp);
				buf.append(",_rd");
				if (null != newCookie) {
					buf.append(",'");
					buf.append(newCookie);
					buf.append("'");
				}
				buf.append(");");
				writer.print(buf.toString());
				buf = null;
				writer.close();
			}
			else {
				writer.print(buffer.toString());
				writer.close();
			}
		} catch (IOException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		}
	}

	/**
	 * 响应数据。
	 * 
	 * @param timestamp 时间戳。
	 * @param callback 回调执行的函数。
	 */
	public void respond(long timestamp, String callback) {
		this.respond(timestamp, callback, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flushBuffer() throws IOException {
		this.writer.flush();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getBufferSize() {
		return this.stringWriter.getBuffer().length();
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
	public String getContentType() {
		return this.soul.getContentType();
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
	public ServletOutputStream getOutputStream() throws IOException {
		return this.soul.getOutputStream();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PrintWriter getWriter() throws IOException {
		return this.writer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCommitted() {
		return this.soul.isCommitted();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		this.soul.reset();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void resetBuffer() {
		this.stringWriter.getBuffer().setLength(0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBufferSize(int size) {
		this.stringWriter.getBuffer().setLength(size);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setCharacterEncoding(String value) {
		this.soul.setCharacterEncoding(value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setContentLength(int length) {
		this.soul.setContentLength(length);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setContentType(String value) {
		this.soul.setContentType(value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setLocale(Locale locale) {
		this.soul.setLocale(locale);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addCookie(Cookie cookie) {
		this.soul.addCookie(cookie);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addDateHeader(String name, long value) {
		this.soul.addDateHeader(name, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addHeader(String header, String value) {
		this.soul.addHeader(header, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addIntHeader(String header, int value) {
		this.soul.addIntHeader(header, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsHeader(String header) {
		return this.soul.containsHeader(header);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String encodeRedirectURL(String url) {
		return this.soul.encodeRedirectURL(url);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String encodeRedirectUrl(String url) {
		return this.soul.encodeRedirectURL(url);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String encodeURL(String url) {
		return this.soul.encodeURL(url);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String encodeUrl(String url) {
		return this.soul.encodeURL(url);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getHeader(String header) {
		return this.soul.getHeader(header);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<String> getHeaderNames() {
		return this.soul.getHeaderNames();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<String> getHeaders(String header) {
		return this.soul.getHeaders(header);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getStatus() {
		return this.soul.getStatus();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendError(int error) throws IOException {
		this.soul.sendError(error);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendError(int error, String content) throws IOException {
		this.soul.sendError(error, content);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendRedirect(String url) throws IOException {
		this.soul.sendRedirect(url);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDateHeader(String data, long length) {
		this.soul.setDateHeader(data, length);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setHeader(String header, String value) {
		this.soul.setHeader(header, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setIntHeader(String header, int value) {
		this.soul.setIntHeader(header, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setStatus(int status) {
		this.soul.setStatus(status);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void setStatus(int status, String arg) {
		this.soul.setStatus(status, arg);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setContentLengthLong(long length) {
		this.soul.setContentLengthLong(length);
	}

}
