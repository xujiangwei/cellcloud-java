/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2016 Cell Cloud Team (www.cellcloud.net)

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
import java.text.SimpleDateFormat;
import java.util.Date;

import net.cellcloud.common.LogHandle;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.LogManager;
import net.cellcloud.util.Clock;
import net.cellcloud.util.Utils;

/** 文件日志。
 * 
 * @author Jiangwei Xu
 */
public class FileLogger implements LogHandle {

	private String name;
	protected StringBuilder stringBuf = new StringBuilder();

	protected String filename = null;
	protected FileOutputStream outputStream = null;
	protected BufferedOutputStream buffer = null;

	private String lineBreak;

	private int bufSize = 256;

	// 累积记录日志大小
	protected long byteSum = 0;
	// 日志记录的起始时间
	protected long created = -1;

	public FileLogger(String name) {
		this.name = name;
		this.outputStream = null;
		this.lineBreak = System.lineSeparator();
	}

	/** 设置日志 Flush 门限。
	 */
	public void setBufferSize(int value) {
		if (value < 0 || this.bufSize == value) {
			return;
		}

		this.bufSize = value;
	}

	/** 打开日志文件。
	 */
	public void open(final String filename) {
		if (null != this.outputStream) {
			return;
		}

		String[] strings = filename.split("\\\\");
		if (strings.length > 1) {
			StringBuilder path = new StringBuilder();
			for (int i = 0; i < strings.length - 1; ++i) {
				path.append(strings[i]);
				path.append(File.separator);
			}

			File fp = new File(path.toString());
			if (!fp.exists()) {
				fp.mkdirs();
			}
			path = null;
		}
		else {
			strings = filename.split("/");
			if (strings.length > 1) {
				StringBuilder path = new StringBuilder();
				for (int i = 0; i < strings.length - 1; ++i) {
					path.append(strings[i]);
					path.append(File.separator);
				}

				File fp = new File(path.toString());
				if (!fp.exists()) {
					fp.mkdirs();
				}
				path = null;
			}
		}

		File file = new File(filename);
		if (file.exists()) {
			// 复制当前日志文件
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			Date date = new Date(file.lastModified());
			try {
				Utils.copyFile(file, filename + "." + sdf.format(date));
			} catch (IOException e) {
				e.printStackTrace();
			}

			// 删除旧文件
			file.delete();
		}

		try {
			this.filename = filename.toString();

			this.outputStream = new FileOutputStream(file);
			this.buffer = new BufferedOutputStream(this.outputStream, this.bufSize);

			this.created = Clock.currentTimeMillis();
			this.byteSum = 0;
		} catch (FileNotFoundException e) {
			e.printStackTrace(System.out);
		}
	}

	/** 关闭日志文件。
	 */
	public void close() {
		if (null == this.outputStream) {
			return;
		}

		synchronized (this.stringBuf) {
			try {
				this.buffer.flush();
				this.outputStream.flush();

				this.buffer.close();
				this.outputStream.close();

				this.outputStream = null;
				this.buffer = null;
			} catch (IOException e) {
				// Nothing
			}
		}
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void logDebug(String tag, String log) {
		this.writeLog(tag, log, LogLevel.DEBUG);
	}

	@Override
	public void logInfo(String tag, String log) {
		this.writeLog(tag, log, LogLevel.INFO);
	}

	@Override
	public void logWarning(String tag, String log) {
		this.writeLog(tag, log, LogLevel.WARNING);
	}

	@Override
	public void logError(String tag, String log) {
		this.writeLog(tag, log, LogLevel.ERROR);
	}

	private void writeLog(String tag, String log, int level) {
		if (null == this.buffer)
			return;

		synchronized (this.stringBuf) {
			this.stringBuf.append(LogManager.TIME_FORMAT.format(new Date()));
			switch (level) {
			case LogLevel.DEBUG:
				this.stringBuf.append(" [DEBUG] ");
				break;
			case LogLevel.INFO:
				this.stringBuf.append(" [INFO]  ");
				break;
			case LogLevel.WARNING:
				this.stringBuf.append(" [WARN]  ");
				break;
			case LogLevel.ERROR:
				this.stringBuf.append(" [ERROR] ");
				break;
			default:
				this.stringBuf.append(" [VERBOSE] ");
				break;
			}
			this.stringBuf.append(tag);
			this.stringBuf.append(" ");
			this.stringBuf.append(log);
			this.stringBuf.append(this.lineBreak);

			try {
				this.buffer.write(Utils.string2Bytes(this.stringBuf.toString()));
			} catch (IOException e) {
				// Nothing
			}

			// 累积大小
			this.byteSum += this.stringBuf.length();

			this.stringBuf.delete(0, this.stringBuf.length());
		}
	}
}
