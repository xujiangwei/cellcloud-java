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

package net.cellcloud.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;

import net.cellcloud.exception.StorageException;

/** 单文件存储器。
 * 
 * @author Jiangwei Xu
 */
public final class SingleFileStorage implements Storage {

	public final static String FACTORY_TYPE_NAME = "SingleFileStorage";

	public final static String FIELD_EXIST = "exist";
	public final static String FIELD_DATA = "data";
	public final static String FIELD_SIZE = "size";

	private byte[] monitor = new byte[0];

	private String name;
	private String fileName;
	private long fileSize;

	protected long fetchSize;
	protected int maxFetchNum;

	// 读缓存
	private ByteBuffer readBuffer;
	// 写缓存
	private ArrayList<SFSBuffer> writeBuffers;

	public SingleFileStorage(String name) {
		this.name = name;
		this.fileName = null;
		this.fileSize = 0;
		this.fetchSize = 8192;
		this.maxFetchNum = (int)((Integer.MAX_VALUE / 32 / this.fetchSize) + 1);
		this.readBuffer = null;
		this.writeBuffers = new ArrayList<SFSBuffer>();
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getTypeName() {
		return SingleFileStorage.FACTORY_TYPE_NAME;
	}

	@Override
	public boolean open(String fileName) {
		synchronized (this.monitor) {
			if (null != this.fileName) {
				// 同一文件
				if (fileName.equals(this.fileName)) {
					return true;
				}

				// 已经打开
				if (this.fileName.length() > 0) {
					return false;
				}
			}

			this.fileName = fileName;
			return true;
		}
	}

	@Override
	public void close() {
		synchronized (this.monitor) {
			if (!this.writeBuffers.isEmpty()) {
				try {
					flushBuffer();
				} catch (Exception e) {
					// Nothing
				}
			}

			this.fileName = null;
			if (null != this.readBuffer) {
				this.readBuffer.clear();
				this.readBuffer = null;
			}
		}
	}

	@Override
	public ResultSet store(String statement) {
		synchronized (this.monitor) {
			if (null == this.fileName || this.fileName.isEmpty()) {
				return null;
			}

			if (statement.equals("read")) {
				if (read()) {
					SingleFileResultSet rs = new SingleFileResultSet(this,
						statement, this.fileName, this.fileSize);
					rs.copyFileData(this.readBuffer);
					return rs;
				}
				else {
					SingleFileResultSet rs = new SingleFileResultSet(this,
							statement, this.fileName, this.fileSize);
					return rs;
				}
			}
			else if (statement.equals("write")) {
				SingleFileResultSet rs = new SingleFileResultSet(this,
						statement, this.fileName, this.fileSize);
				return rs;
			}

			return null;
		}
	}

	/** 生成读文件操作声明。
	 */
	public String generateReadStatement() {
		return "read";
	}

	/** 生成写文件操作声明。
	*/
	public String generateWriteStatement() {
		return "write";
	}

	/** 设置分段大小。
	 */
	public void setFetchSize(int size) {
		synchronized (this.monitor) {
			if (size <= 0) {
				return;
			}

			if ((size & (size - 1)) == 0) {
				this.fetchSize = size;
			}
		}
	}
	/** 返回分段大小。
	 */
	public int getFetchSize() {
		synchronized (this.monitor) {
			return (int)this.fetchSize;
		}
	}

	/** 设置最大分段数量。
	 */
	public void setMaxFetchNum(int num) {
		synchronized (this.monitor) {
			if (num <= 0) {
				return;
			}

			this.maxFetchNum = num;
		}
	}
	/** 返回最大分段数量。
	 */
	public int getMaxFetchNum() {
		synchronized (this.monitor) {
			return this.maxFetchNum;
		}
	}

	protected void changeSize(long size) {
		synchronized (this.monitor) {
			this.fileSize = size;
		}
	}

	/** 写入文件数据。
	 */
	protected void write(byte[] src, long offset, long length)
		throws StorageException {
		synchronized (this.monitor) {
			if (this.writeBuffers.size() >= this.maxFetchNum) {
				flushBuffer();
			}

			SFSBuffer buf = new SFSBuffer((int)offset, (int)length, src);
			this.writeBuffers.add(buf);
		}
	}

	private void flushBuffer() throws StorageException {

		boolean useStream = true;
		SFSBuffer buf = null;

		// 判断写入的数据是否是连续数据
		for (int i = 0, size = this.writeBuffers.size(); i < size; ++i) {
			buf = this.writeBuffers.get(i);
			if (i + 1 < size) {
				SFSBuffer next = this.writeBuffers.get(i + 1);
				if (buf.offset + buf.length != next.offset) {
					useStream = false;
					break;
				}
			}
		}

		if (useStream) {
			// 使用流写文件
			FileOutputStream fos = null;
			try {
				// 如果第一个缓存从 0 开始则覆盖原文件，否则追加文件数据
				fos = new FileOutputStream(this.fileName, this.writeBuffers.get(0).offset == 0 ? false : true);

				Iterator<SFSBuffer> iter = this.writeBuffers.iterator();
				while (iter.hasNext()) {
					buf = iter.next();
					fos.write(buf.data);
				}

				fos.flush();
				this.writeBuffers.clear();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (null != fos) {
						fos.close();
					}
				} catch (Exception e) {
					// Nothing
				}
			}
		}
		else {
			// 随机写文件
			RandomAccessFile raf = null;
			try {
				File file = new File(this.fileName);
				if (!file.exists()) {
					file.createNewFile();
				}

				raf = new RandomAccessFile(file, "rw");

				for (int i = 0, size = this.writeBuffers.size(); i < size; ++i) {
					buf = this.writeBuffers.get(i);
					raf.seek(buf.offset);
					raf.write(buf.data);
				}

				this.writeBuffers.clear();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					raf.close();
				} catch (Exception e) {
					// Nothing
				}
			}
		}
	}

	private boolean read() {
		if (0 == this.fileSize) {
			File file = new File(this.fileName);
			if (file.exists()) {
				this.fileSize = file.length();
			}
			else {
				return false;
			}
		}

		if (null != this.readBuffer) {
			return true;
		}

		// 使用 NIO 读取文件。
		FileInputStream fis = null;
		FileChannel fc = null;
		try {
			fis = new FileInputStream(this.fileName);
			fc = fis.getChannel();

			if (null == this.readBuffer) {
				this.readBuffer = ByteBuffer.allocate((int) Math.min(this.fetchSize, this.fileSize));
			}

			fc.read(this.readBuffer);
			this.readBuffer.flip();
		} catch (Exception e) {
			e.printStackTrace();
			this.readBuffer = null;
			return false;
		} finally {
			try {
				fis.close();
				fc.close();
			} catch (Exception e) {
				// Nothing
			}
		}

		return true;
	}

	private class SFSBuffer {
		protected int offset = 0;
		protected int length = 0;
		protected byte[] data = null;

		protected SFSBuffer(int offset, int length, byte[] data) {
			this.offset = offset;
			this.length = length;
			this.data = data;
		}
	}
}
