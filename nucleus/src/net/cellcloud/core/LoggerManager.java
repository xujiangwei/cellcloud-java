/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (cellcloudproject@gmail.com)

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

package net.cellcloud.core;

import java.text.SimpleDateFormat;
import java.util.Date;

/** 日志管理器。
 * 
 * @author Jiangwei Xu
 */
public final class LoggerManager {

	private final static LoggerManager instance = new LoggerManager();

	public final static SimpleDateFormat dataFormat = new SimpleDateFormat("HH:mm:ss");

	public final static byte DEBUG = 1;
	public final static byte INFO = 2;
	public final static byte WARNING = 3;
	public final static byte ERROR = 4;

	private LogHandle handle;

	private LoggerManager() {
		this.handle = createSystemOutHandle();
	}

	public synchronized static LoggerManager getInstance() {
		return instance;
	}

	public void log(byte level, String tag, String log) {
		if (null == this.handle)
			return;

		switch (level) {
		case DEBUG:
			this.handle.logDebug(tag, log);
			break;
		case INFO:
			this.handle.logInfo(tag, log);
			break;
		case WARNING:
			this.handle.logWarning(tag, log);
			break;
		case ERROR:
			this.handle.logError(tag, log);
			break;
		default:
			break;
		}
	}

	public void setHandle(LogHandle handle) {
		this.handle = handle;
	}

	/** 创建 System.out 日志。
	 */
	public LogHandle createSystemOutHandle() {
		return new LogHandle() {

			private byte[] mutex = new byte[0];
			private StringBuilder buf = new StringBuilder();

			@Override
			public void logDebug(String tag, String log) {
				synchronized (mutex) {
					buf.append(dataFormat.format(new Date()));
					buf.append(" [DEBUG] ");
					buf.append(tag);
					buf.append(" ");
					buf.append(log);

					System.out.println(buf.toString());

					buf.delete(0, buf.length());
				}
			}

			@Override
			public void logInfo(String tag, String log) {
				synchronized (mutex) {
					buf.append(dataFormat.format(new Date()));
					buf.append(" [INFO]  ");
					buf.append(tag);
					buf.append(" ");
					buf.append(log);

					System.out.println(buf.toString());

					buf.delete(0, buf.length());
				}
			}

			@Override
			public void logWarning(String tag, String log) {
				synchronized (mutex) {
					buf.append(dataFormat.format(new Date()));
					buf.append(" [WARN]  ");
					buf.append(tag);
					buf.append(" ");
					buf.append(log);

					System.out.println(buf.toString());

					buf.delete(0, buf.length());
				}
			}

			@Override
			public void logError(String tag, String log) {
				synchronized (mutex) {
					buf.append(dataFormat.format(new Date()));
					buf.append(" [ERROR] ");
					buf.append(tag);
					buf.append(" ");
					buf.append(log);

					System.out.println(buf.toString());

					buf.delete(0, buf.length());
				}
			}
		};
	}
}
