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
import net.cellcloud.util.Properties;

/** 单文件存储器。
 * 
 * @author Jiangwei Xu
 */
public final class LocalFileStorage implements FileStorage {

	public final static String TYPE_NAME = "LocalFileStorage";

	public final static String FIELD_FILENAME_STRING = "filename";
	public final static String FIELD_EXIST_BOOL = "exist";
	public final static String FIELD_SIZE_LONG = "size";
	public final static String FIELD_DATA_RAW = "data";

	private String instanceName;
	private byte[] monitor = new byte[0];

	// 默认文件块大小：256 KB
	protected int chunkSize = 262144;

	// 写缓存
	private ArrayList<FileWrapper> fileList;

	protected LocalFileStorage(String name) {
		this.instanceName = name;
		this.fileList = new ArrayList<FileWrapper>();
	}

	@Override
	public String getName() {
		return this.instanceName;
	}

	@Override
	public String getTypeName() {
		return LocalFileStorage.TYPE_NAME;
	}

	@Override
	public boolean open(Properties properties)
			 throws StorageException {
		// TODO
		return true;
	}

	@Override
	public void close() throws StorageException {
		synchronized (this.monitor) {
			for (FileWrapper fw : this.fileList) {
				fw.close();
			}

			this.fileList.clear();
		}
	}

	@Override
	public ResultSet store(String statement)
			throws StorageException {
		// 分析存储语句
		String[] ary = statement.split("\\|");
		if (ary.length != 2) {
			return null;
		}

		final String operate = ary[0];
		final String filename = ary[1];
		synchronized (this.monitor) {
			if (operate.equals("read") || operate.equals("write")) {
				FileWrapper file = this.findFileWrapper(filename);
				if (null == file) {
					file = this.loadFile(filename);
				}

				if (null == file) {
					throw new StorageException("LocalFileStorage ("+ this.instanceName +") load file '" + filename + "' failed.");
				}

				LocalFileResultSet rs = new LocalFileResultSet(this, operate, file);
				return rs;
			}
			else {
				return null;
			}
		}
	}

	@Override
	public ResultSet store(Schema schema)
			throws StorageException {
		// TODO
		return null;
	}

	/** 查找已缓存的文件。
	 * 如果文件不存在返回空指针。
	 */
	private FileWrapper findFileWrapper(String fullpath) {
		for (FileWrapper f : this.fileList) {
			if (f.filename.equals(fullpath)) {
				return f;
			}
		}

		return null;
	}

	/** 加载文件数据。
	 */
	private FileWrapper loadFile(String fullpath) {
		FileWrapper f = new FileWrapper(fullpath);
		this.fileList.add(f);
		f.open();
		return f;
	}

	/** 返回读文件操作声明。
	 */
	public String createReadStatement(final String file) {
		return "read|" + file;
	}

	/** 返回写文件操作声明。
	*/
	public String createWriteStatement(final String file) {
		return "write|" + file;
	}

	/** 设置分段大小。
	 */
	public void setChunkSize(int size) {
		synchronized (this.monitor) {
			if (size <= 0) {
				return;
			}

			// 块大小必须是 2 的次幂
			if ((size & (size - 1)) == 0) {
				this.chunkSize = size;
			}
		}
	}
	/** 返回分段大小。
	 */
	public int getChunkSize() {
		synchronized (this.monitor) {
			return this.chunkSize;
		}
	}


	/** Chunk 缓存。
	 */
	protected class ChunkBuffer {
		protected long offset = 0;
		protected int length = 0;
		protected byte[] data = null;

		protected ChunkBuffer(long offset, int length, byte[] data) {
			this.offset = offset;
			this.length = length;
			this.data = data;
		}
	}

	/** 存储器内部文件结构。
	 */
	protected class FileWrapper {
		protected String filename = null;
		protected long filesize = -1;
		private ChunkBuffer readBuffer = null;
		private ArrayList<ChunkBuffer> writeBuffers = null;
		private long writedLength = 0;

		protected FileWrapper(String filename) {
			this.filename = filename;
		}

		protected boolean open() {
			if (0 > this.filesize) {
				File file = new File(this.filename);
				if (file.exists()) {
					this.filesize = file.length();
				}
				else {
					// 文件不存在则返回 false
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
				fis = new FileInputStream(this.filename);
				fc = fis.getChannel();
				ByteBuffer buf = ByteBuffer.allocate((int)chunkSize);
				fc.read(buf);
				buf.flip();

				// 复制数据
				int len = buf.limit();
				byte[] dst = new byte[len];
				buf.get(dst);

				// 创建读缓存
				this.readBuffer = new ChunkBuffer(0, len, dst);

				buf.clear();
				buf = null;
			} catch (Exception e) {
				e.printStackTrace();
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

		protected void close() {
			if (null != this.writeBuffers && !this.writeBuffers.isEmpty()) {
				try {
					this.flushBuffer();
				} catch (Exception e) {
					// Nothing
				}
			}

			this.readBuffer = null;
			this.writedLength = 0;
		}

		/** 读数据
		 */
		protected int read(byte[] dest, long offset, int length) {
			// 缓存区边界
			int end = (int)this.readBuffer.offset + this.readBuffer.length;
			// 读取偏移已经超过缓冲区大小
			if (offset < 0 || offset >= end) {
				return -1;
			}

			int len = length;
			if (end < offset + length) {
				len = end - (int)offset;
			}

			System.arraycopy(this.readBuffer.data, (int)offset, dest, 0, len);
			return len;
		}

		protected ChunkBuffer readWithoutBuffer(long offset, int length) {
			ByteBuffer buffer = ByteBuffer.allocate(length);
			ChunkBuffer cb = null;
			// 使用 NIO 读取文件。
			FileInputStream fis = null;
			FileChannel fc = null;
			try {
				fis = new FileInputStream(this.filename);
				fc = fis.getChannel();
				fc.read(buffer, offset);
				buffer.flip();

				byte[] data = new byte[buffer.limit()];
				buffer.get(data);
				cb = new ChunkBuffer(offset, data.length, data);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					fis.close();
					fc.close();
				} catch (Exception e) {
					// Nothing
				}
			}

			return cb;
		}

		/** 写入文件数据。
		 */
		protected synchronized void write(byte[] src, long offset, long length) {
			if (null == this.writeBuffers) {
				this.writeBuffers = new ArrayList<ChunkBuffer>();
			}

			ChunkBuffer buf = new ChunkBuffer(offset, (int)length, src);
			this.writeBuffers.add(buf);
			this.writedLength += length;

			if (this.writedLength >= chunkSize) {
				flushBuffer();
				this.writedLength = 0;
			}
		}

		private void flushBuffer() {
			boolean useStream = true;
			ChunkBuffer buf = null;

			// 判断写入的数据是否是连续数据
			for (int i = 0, size = this.writeBuffers.size(); i < size; ++i) {
				buf = this.writeBuffers.get(i);
				if (i + 1 < size) {
					ChunkBuffer next = this.writeBuffers.get(i + 1);
					if (buf.offset + buf.length != next.offset) {
						// 非连续数据，不使用流
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
					fos = new FileOutputStream(this.filename, this.writeBuffers.get(0).offset == 0 ? false : true);

					Iterator<ChunkBuffer> iter = this.writeBuffers.iterator();
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
					File file = new File(this.filename);
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
	} // #class FileWrapper
}
