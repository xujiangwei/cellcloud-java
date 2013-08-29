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
 * @author Jiangwei Xu
 */
public class CrossOriginHttpServletResponse implements HttpServletResponse {

	private HttpServletResponse soul;

	private PrintWriter writer;
	private StringWriter stringWriter;

	private String callback;

	public CrossOriginHttpServletResponse(HttpServletResponse soul, String callback) {
		this.soul = soul;
		this.callback = callback;
		this.stringWriter = new StringWriter(512);
		this.writer = new PrintWriter(this.stringWriter);
	}

	public String getCallback() {
		return this.callback;
	}

	protected void output() {
		StringBuffer buffer = this.stringWriter.getBuffer();

		try {
			PrintWriter writer = this.soul.getWriter();
			if (null != this.callback) {
				StringBuilder buf = new StringBuilder();
				buf.append("var _rd = eval('(");
				buf.append(buffer.toString());
				buf.append(")';");
				buf.append(this.callback);
				buf.append(".call(null,_rd);");
				writer.print(buf.toString());
				buf = null;
				writer.close();
			}
			else {
				
			}
		} catch (IOException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		}
	}

	@Override
	public void flushBuffer() throws IOException {
		this.writer.flush();
	}

	@Override
	public int getBufferSize() {
		return this.stringWriter.getBuffer().length();
	}

	@Override
	public String getCharacterEncoding() {
		return this.soul.getCharacterEncoding();
	}

	@Override
	public String getContentType() {
		return this.soul.getContentType();
	}

	@Override
	public Locale getLocale() {
		return this.soul.getLocale();
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return this.soul.getOutputStream();
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return this.writer;
	}

	@Override
	public boolean isCommitted() {
		return this.soul.isCommitted();
	}

	@Override
	public void reset() {
		this.soul.reset();
	}

	@Override
	public void resetBuffer() {
		this.stringWriter.getBuffer().setLength(0);
	}

	@Override
	public void setBufferSize(int size) {
		this.stringWriter.getBuffer().setLength(size);
	}

	@Override
	public void setCharacterEncoding(String value) {
		this.soul.setCharacterEncoding(value);
	}

	@Override
	public void setContentLength(int length) {
		this.soul.setContentLength(length);
	}

	@Override
	public void setContentType(String value) {
		this.soul.setContentType(value);
	}

	@Override
	public void setLocale(Locale locale) {
		this.soul.setLocale(locale);
	}

	@Override
	public void addCookie(Cookie cookie) {
		this.soul.addCookie(cookie);
	}

	@Override
	public void addDateHeader(String name, long value) {
		this.soul.addDateHeader(name, value);
	}

	@Override
	public void addHeader(String header, String value) {
		this.soul.addHeader(header, value);
	}

	@Override
	public void addIntHeader(String header, int value) {
		this.soul.addIntHeader(header, value);
	}

	@Override
	public boolean containsHeader(String header) {
		return this.soul.containsHeader(header);
	}

	@Override
	public String encodeRedirectURL(String arg0) {
		return this.soul.encodeRedirectURL(arg0);
	}

	@Override
	public String encodeRedirectUrl(String arg0) {
		return this.soul.encodeRedirectURL(arg0);
	}

	@Override
	public String encodeURL(String arg0) {
		return this.soul.encodeURL(arg0);
	}

	@Override
	public String encodeUrl(String arg0) {
		return this.soul.encodeURL(arg0);
	}

	@Override
	public String getHeader(String header) {
		return this.soul.getHeader(header);
	}

	@Override
	public Collection<String> getHeaderNames() {
		return this.soul.getHeaderNames();
	}

	@Override
	public Collection<String> getHeaders(String header) {
		return this.soul.getHeaders(header);
	}

	@Override
	public int getStatus() {
		return this.soul.getStatus();
	}

	@Override
	public void sendError(int error) throws IOException {
		this.soul.sendError(error);
	}

	@Override
	public void sendError(int arg0, String arg1) throws IOException {
		this.soul.sendError(arg0, arg1);
	}

	@Override
	public void sendRedirect(String arg0) throws IOException {
		this.soul.sendRedirect(arg0);
	}

	@Override
	public void setDateHeader(String arg0, long arg1) {
		this.soul.setDateHeader(arg0, arg1);
	}

	@Override
	public void setHeader(String header, String value) {
		this.soul.setHeader(header, value);
	}

	@Override
	public void setIntHeader(String header, int value) {
		this.soul.setIntHeader(header, value);
	}

	@Override
	public void setStatus(int status) {
		this.soul.setStatus(status);
	}

	@Override
	public void setStatus(int arg0, String arg1) {
		// Nothing
	}
}
