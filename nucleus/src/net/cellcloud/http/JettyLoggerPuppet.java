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

import org.eclipse.jetty.util.log.Logger;

/**
 * Jetty 日志傀儡。
 * 
 * @author Ambrose Xu
 * 
 */
public class JettyLoggerPuppet implements org.eclipse.jetty.util.log.Logger {

	private boolean debugEnabled = false;
	private StringBuilder buffer = null;

	/**
	 * 构造函数。
	 */
	public JettyLoggerPuppet() {
		this.buffer = new StringBuilder();
	}

	@Override
	public Logger getLogger(String name) {
		return this;
	}

	@Override
	public String getName() {
		return "PuppetLogForJetty";
	}

	@Override
	public void debug(Throwable throwable) {
		if (!this.debugEnabled) {
			return;
		}

		net.cellcloud.common.Logger.d(this.getClass(), throwable.getMessage());
	}

	@Override
	public void debug(String format, Object... params) {
		if (!this.debugEnabled) {
			return;
		}

		String out = format;
		synchronized (this.buffer) {
			for (Object param : params) {
				out = out.replace("{}", param.toString());
				this.buffer.append(out);
			}

			net.cellcloud.common.Logger.d(this.getClass(), this.buffer.toString());
			this.buffer.delete(0, this.buffer.length());
		}
	}

	@Override
	public void debug(String format, Throwable throwable) {
		if (!this.debugEnabled) {
			return;
		}

		net.cellcloud.common.Logger.d(this.getClass(), format);
	}

	@Override
	public void debug(String msg, long value) {
		if (!this.debugEnabled) {
			return;
		}

		net.cellcloud.common.Logger.d(this.getClass(), msg);
	}

	@Override
	public void info(Throwable throwable) {
		net.cellcloud.common.Logger.i(this.getClass(), throwable.getMessage());
	}

	@Override
	public void info(String format, Object... params) {
		String out = format;
		synchronized (this.buffer) {
			for (Object param : params) {
				out = out.replace("{}", param.toString());
				this.buffer.append(out);
			}

			net.cellcloud.common.Logger.i(this.getClass(), this.buffer.toString());
			this.buffer.delete(0, this.buffer.length());
		}
	}

	@Override
	public void info(String format, Throwable throwable) {
		net.cellcloud.common.Logger.i(this.getClass(), format);
	}

	@Override
	public void warn(Throwable throwable) {
		net.cellcloud.common.Logger.w(this.getClass(), throwable.getMessage());
	}

	@Override
	public void warn(String format, Object... params) {
		String out = format;
		synchronized (this.buffer) {
			for (Object param : params) {
				out = out.replace("{}", param.toString());
				this.buffer.append(out);
			}

			net.cellcloud.common.Logger.w(this.getClass(), this.buffer.toString());
			this.buffer.delete(0, this.buffer.length());
		}
	}

	@Override
	public void warn(String format, Throwable throwable) {
		net.cellcloud.common.Logger.w(this.getClass(), format);
	}

	@Override
	public void ignore(Throwable throwable) {
		// Nothing
	}

	@Override
	public boolean isDebugEnabled() {
		return this.debugEnabled;
	}

	@Override
	public void setDebugEnabled(boolean value) {
		this.debugEnabled = value;
	}

}
