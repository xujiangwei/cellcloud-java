/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2014 Cell Cloud Team (www.cellcloud.net)

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
import net.cellcloud.common.LogManager;
import net.cellcloud.util.Utils;

/** 文件日志。
 * 
 * @author Jiangwei Xu
 */
public final class FileLogger implements LogHandle {

	private static final FileLogger instance = new FileLogger();

	private String name;
	private StringBuilder stringBuf = new StringBuilder();

	private FileOutputStream outputStream = null;
	private BufferedOutputStream buffer = null;

	private String lineBreak = Utils.isWindowsOS() ? "\r\n" : "\n";

	private int bufSize = 256;

	private FileLogger() {
		this.name = "CellFileLogger";
		this.outputStream = null;
	}

	/** 返回单例。
	 */
	public synchronized static FileLogger getInstance() {
		return instance;
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
	public void open(String filename) {
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
			this.outputStream = new FileOutputStream(file);
			this.buffer = new BufferedOutputStream(this.outputStream, this.bufSize);
		} catch (FileNotFoundException e) {
			e.printStackTrace(System.out);
			return;
		}

		// 设置日志操作器
		LogManager.getInstance().addHandle(this);
	}

	/** 关闭日志文件。
	 */
	public void close() {
		if (null == this.outputStream) {
			return;
		}

		LogManager.getInstance().removeHandle(this);

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
		if (null == this.buffer)
			return;

		synchronized (this.stringBuf) {
			this.stringBuf.append(LogManager.timeFormat.format(new Date()));
			this.stringBuf.append(" [DEBUG] ");
			this.stringBuf.append(tag);
			this.stringBuf.append(" ");
			this.stringBuf.append(log);
			this.stringBuf.append(lineBreak);

			try {
				this.buffer.write(Utils.string2Bytes(this.stringBuf.toString()));
			} catch (IOException e) {
				// Nothing
			}

			this.stringBuf.delete(0, this.stringBuf.length());
		}
	}

	@Override
	public void logInfo(String tag, String log) {
		if (null == this.buffer)
			return;

		synchronized (this.stringBuf) {
			this.stringBuf.append(LogManager.timeFormat.format(new Date()));
			this.stringBuf.append(" [INFO]  ");
			this.stringBuf.append(tag);
			this.stringBuf.append(" ");
			this.stringBuf.append(log);
			this.stringBuf.append(lineBreak);

			try {
				this.buffer.write(Utils.string2Bytes(this.stringBuf.toString()));
			} catch (IOException e) {
				// Nothing
			}

			this.stringBuf.delete(0, this.stringBuf.length());
		}
	}

	@Override
	public void logWarning(String tag, String log) {
		if (null == this.buffer)
			return;

		synchronized (this.stringBuf) {
			this.stringBuf.append(LogManager.timeFormat.format(new Date()));
			this.stringBuf.append(" [WARN]  ");
			this.stringBuf.append(tag);
			this.stringBuf.append(" ");
			this.stringBuf.append(log);
			this.stringBuf.append(lineBreak);

			try {
				this.buffer.write(Utils.string2Bytes(this.stringBuf.toString()));
			} catch (IOException e) {
				// Nothing
			}

			this.stringBuf.delete(0, this.stringBuf.length());
		}
	}

	@Override
	public void logError(String tag, String log) {
		if (null == this.buffer)
			return;

		synchronized (this.stringBuf) {
			this.stringBuf.append(LogManager.timeFormat.format(new Date()));
			this.stringBuf.append(" [ERROR] ");
			this.stringBuf.append(tag);
			this.stringBuf.append(" ");
			this.stringBuf.append(log);
			this.stringBuf.append(lineBreak);

			try {
				this.buffer.write(Utils.string2Bytes(this.stringBuf.toString()));
			} catch (IOException e) {
				// Nothing
			}

			this.stringBuf.delete(0, this.stringBuf.length());
		}
	}
}
