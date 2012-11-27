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

package net.cellcloud.cell.log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import net.cellcloud.core.LogHandle;
import net.cellcloud.core.LoggerManager;
import net.cellcloud.util.Util;

/** 文件日志。
 * 
 * @author Jiangwei Xu
 */
public final class FileLogger implements LogHandle {

	private static final FileLogger instance = new FileLogger();

	private byte[] mutex = new byte[0];
	private StringBuilder stringBuf = new StringBuilder();

	private FileOutputStream outputStream = null;
	private BufferedOutputStream buffer = null;

	private String lineBreak = Util.isWindowsOS() ? "\r\n" : "\n";

	private int flushThreshold = 1024;
	private int countSize = 0;

	private FileLogger() {
		this.outputStream = null;
	}

	/** 返回单例。
	 */
	synchronized public static FileLogger getInstance() {
		return instance;
	}

	/** 设置日志 Flush 门限。
	 */
	public void setFlushThreshold(int value) {
		if (value < 0) {
			return;
		}

		this.flushThreshold = value;
	}

	/** 打开日志文件。
	 */
	public void open(String filename) {
		if (null != this.outputStream) {
			return;
		}

		File file = new File(filename);

		try {
			this.outputStream = new FileOutputStream(file);
			this.buffer = new BufferedOutputStream(this.outputStream);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// 设置日志操作器
		LoggerManager.getInstance().removeAllHandles();
		LoggerManager.getInstance().addHandle(this);
	}

	/** 关闭日志文件。
	 */
	public void close() {
		if (null == this.outputStream) {
			return;
		}

		synchronized (this.mutex) {
			this.countSize = 0;

			try {
				this.buffer.flush();
				this.outputStream.flush();

				this.buffer.close();
				this.outputStream.close();

				this.outputStream = null;
				this.buffer = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void logDebug(String tag, String log) {
		synchronized (this.mutex) {
			if (null == this.buffer)
				return;

			this.stringBuf.append(LoggerManager.dateFormat.format(new Date()));
			this.stringBuf.append(" [DEBUG] ");
			this.stringBuf.append(tag);
			this.stringBuf.append(" ");
			this.stringBuf.append(log);
			this.stringBuf.append(lineBreak);

			try {
				this.buffer.write(Util.string2Bytes(this.stringBuf.toString()));

				this.countSize += this.stringBuf.length();
				if (this.countSize >= this.flushThreshold) {
					this.buffer.flush();
					this.countSize = 0;
				}
			} catch (IOException e) {
				// Nothing
			}

			this.stringBuf.delete(0, this.stringBuf.length());
		}
	}

	@Override
	public void logInfo(String tag, String log) {
		synchronized (this.mutex) {
			if (null == this.buffer)
				return;

			this.stringBuf.append(LoggerManager.dateFormat.format(new Date()));
			this.stringBuf.append(" [INFO]  ");
			this.stringBuf.append(tag);
			this.stringBuf.append(" ");
			this.stringBuf.append(log);
			this.stringBuf.append(lineBreak);

			try {
				this.buffer.write(Util.string2Bytes(this.stringBuf.toString()));

				this.countSize += this.stringBuf.length();
				if (this.countSize >= this.flushThreshold) {
					this.buffer.flush();
					this.countSize = 0;
				}
			} catch (IOException e) {
				// Nothing
			}

			this.stringBuf.delete(0, this.stringBuf.length());
		}
	}

	@Override
	public void logWarning(String tag, String log) {
		synchronized (this.mutex) {
			if (null == this.buffer)
				return;

			this.stringBuf.append(LoggerManager.dateFormat.format(new Date()));
			this.stringBuf.append(" [WARN]  ");
			this.stringBuf.append(tag);
			this.stringBuf.append(" ");
			this.stringBuf.append(log);
			this.stringBuf.append(lineBreak);

			try {
				this.buffer.write(Util.string2Bytes(this.stringBuf.toString()));

				this.countSize += this.stringBuf.length();
				if (this.countSize >= this.flushThreshold) {
					this.buffer.flush();
					this.countSize = 0;
				}
			} catch (IOException e) {
				// Nothing
			}

			this.stringBuf.delete(0, this.stringBuf.length());
		}
	}

	@Override
	public void logError(String tag, String log) {
		synchronized (this.mutex) {
			if (null == this.buffer)
				return;

			this.stringBuf.append(LoggerManager.dateFormat.format(new Date()));
			this.stringBuf.append(" [ERROR] ");
			this.stringBuf.append(tag);
			this.stringBuf.append(" ");
			this.stringBuf.append(log);
			this.stringBuf.append(lineBreak);

			try {
				this.buffer.write(Util.string2Bytes(this.stringBuf.toString()));

				this.countSize += this.stringBuf.length();
				if (this.countSize >= this.flushThreshold) {
					this.buffer.flush();
					this.countSize = 0;
				}
			} catch (IOException e) {
				// Nothing
			}

			this.stringBuf.delete(0, this.stringBuf.length());
		}
	}
}
