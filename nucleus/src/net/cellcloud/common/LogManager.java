/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2015 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.common;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/** 日志管理器。
 * 
 * @author Jiangwei Xu
 */
public final class LogManager {

	private final static LogManager instance = new LogManager();

	public final static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

	private ArrayList<LogHandle> handles;
	private byte level;

	private LogManager() {
		this.handles = new ArrayList<LogHandle>();
		this.level = LogLevel.DEBUG;
	}

	public static LogManager getInstance() {
		return instance;
	}

	/** 设置日志等级。
	 */
	public void setLevel(byte level) {
		this.level = level;
	}
	/** 返回日志等级。
	 */
	public byte getLevel() {
		return this.level;
	}

	/** 记录日志。
	 */
	public void log(byte level, String tag, String log) {
		synchronized (this) {
			if (this.handles.isEmpty()) {
				this.addHandle(createSystemOutHandle());
			}

			if (this.level > level) {
				// 过滤日志等级
				return;
			}

			for (LogHandle handle : this.handles) {
				switch (level) {
				case LogLevel.DEBUG:
					handle.logDebug(tag, log);
					break;
				case LogLevel.INFO:
					handle.logInfo(tag, log);
					break;
				case LogLevel.WARNING:
					handle.logWarning(tag, log);
					break;
				case LogLevel.ERROR:
					handle.logError(tag, log);
					break;
				default:
					break;
				}
			}
		}
	}

	/**
	 * 返回指定名称的处理器。
	 * 
	 * @param name
	 * @return
	 */
	public LogHandle getHandle(String name) {
		synchronized (this) {
			for (LogHandle handle : this.handles) {
				if (handle.getName().equals(name)) {
					return handle;
				}
			}
		}

		return null;
	}

	/** 添加日志内容处理器。
	 */
	public void addHandle(LogHandle handle) {
		synchronized (this) {
			if (this.handles.contains(handle)) {
				return;
			}

			for (LogHandle h : this.handles) {
				if (h.getName().equals(handle.getName())) {
					return;
				}
			}

			this.handles.add(handle);
		}
	}

	/** 移除日志内容处理器。
	 */
	public void removeHandle(LogHandle handle) {
		synchronized (this) {
			this.handles.remove(handle);
		}
	}

	/** 移除所有日志内容处理器。
	 */
	public void removeAllHandles() {
		synchronized (this) {
			this.handles.clear();
		}
	}

	/** 创建 System.out 日志。
	 */
	public static LogHandle createSystemOutHandle() {
		return new LogHandle() {

			private String name = "DefaultSystemOutHandle";
			private StringBuilder buf = new StringBuilder();

			@Override
			public String getName() {
				return this.name;
			}

			@Override
			public void logDebug(String tag, String log) {
				synchronized (buf) {
					buf.append(TIME_FORMAT.format(new Date()));
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
				synchronized (buf) {
					buf.append(TIME_FORMAT.format(new Date()));
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
				synchronized (buf) {
					buf.append(TIME_FORMAT.format(new Date()));
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
				synchronized (buf) {
					buf.append(TIME_FORMAT.format(new Date()));
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
