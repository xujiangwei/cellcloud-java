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

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import net.cellcloud.exception.StorageException;

/** 单文件存储器结果集。
 * 
 * @author Jiangwei Xu
 */
public final class SingleFileResultSet implements ResultSet {

	private SingleFileStorage storage;
	private String statement;
	private String fileName;
	private long fileSize;

	private int cursor;
	private long bufOffset;
	private ByteBuffer buffer;

	public SingleFileResultSet(SingleFileStorage storage,
			final String statement, final String fileName, final long fileSize) {
		this.storage = storage;
		this.statement = statement;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.cursor = -1;
		this.bufOffset = 0;
		this.buffer = null;
	}

	protected void copyFileData(ByteBuffer buffer) {
		this.bufOffset = 0;
		this.buffer = buffer;
	}

	@Override
	public boolean absolute(int cursor) {
		if (cursor != 0)
			return false;

		this.cursor = cursor;
		return true;
	}

	@Override
	public boolean relative(int cursor) {
		return false;
	}

	@Override
	public boolean first() {
		this.cursor = 0;
		return true;
	}

	@Override
	public boolean last() {
		this.cursor = 0;
		return true;
	}

	@Override
	public boolean next() {
		if (this.cursor + 1 == 0)
		{
			this.cursor += 1;
			return true;
		}

		return false;
	}

	@Override
	public boolean previous() {
		return false;
	}

	@Override
	public boolean isFirst() {
		return (this.cursor == 0);
	}

	@Override
	public boolean isLast() {
		return (this.cursor == 0);
	}

	@Override
	public char getChar(int index) {
		return 0;
	}

	@Override
	public char getChar(String label) {
		return 0;
	}

	@Override
	public int getInt(int index) {
		return 0;
	}

	@Override
	public int getInt(String label) {
		return 0;
	}

	@Override
	public long getLong(int index) {
		return 0;
	}

	@Override
	public long getLong(String label) {
		if (this.cursor != 0) {
			return -1;
		}

		if (label.equals(SingleFileStorage.FIELD_SIZE)) {
			return this.fileSize;
		}

		return 0;
	}

	@Override
	public String getString(int index) {
		return null;
	}

	@Override
	public String getString(String label) {
		return null;
	}

	@Override
	public boolean getBool(int index) {
		return false;
	}

	@Override
	public boolean getBool(String label) {
		if (this.cursor != 0) {
			return false;
		}

		if (label.equals(SingleFileStorage.FIELD_EXIST)) {
			return this.fileSize > 0;
		}

		return false;
	}

	@Override
	public byte[] getRaw(String label, long offset, long length) {
		if (this.cursor != 0
			|| !label.equals(SingleFileStorage.FIELD_DATA)
			|| null == this.buffer) {
			return null;
		}
		else if (offset < 0 || length <= 0 || offset >= this.fileSize) {
			return null;
		}

		if (this.fileSize > this.storage.fetchSize) {
			boolean invalid = false;

			long bufEnd = this.bufOffset + this.buffer.capacity();
			if (offset > bufEnd || offset < this.bufOffset) {
				invalid = true;
			}
			else if (offset + length > bufEnd || length > this.buffer.capacity()) {
				invalid = true;
			}

			if (invalid) {
				long remaining = this.fileSize - offset;
				int bufLength = (int)((this.storage.fetchSize < remaining) ? this.storage.fetchSize : remaining);
				this.bufOffset = offset;
				this.buffer = null;

				this.buffer = ByteBuffer.allocate(bufLength);

				// 使用 NIO 读取文件。
				FileInputStream fis = null;
				FileChannel fc = null;
				try {
					fis = new FileInputStream(this.fileName);
					fc = fis.getChannel();
					fc.read(this.buffer, this.bufOffset);
					this.buffer.flip();
				} catch (Exception e) {
					e.printStackTrace();
					this.buffer = null;
				} finally {
					try {
						fis.close();
						fc.close();
					} catch (Exception e) {
						// Nothing
					}
				}
			}

			// 调整 Offset
			offset = offset - this.bufOffset;
		}

		if (offset + length > this.buffer.capacity()) {
			length = this.buffer.capacity() - offset;
		}

		if (length <= 0) {
			return null;
		}

		byte[] dst = new byte[(int)length];
		this.buffer.position((int)offset);
		this.buffer.get(dst, 0, (int)length);
		return dst;
	}

	@Override
	public void updateChar(int index, char value) {
		// Nothing
	}

	@Override
	public void updateChar(String label, char value) {
		// Nothing
	}

	@Override
	public void updateInt(int index, int value) {
		// Nothing
	}

	@Override
	public void updateInt(String label, int value) {
		updateLong(label, (long)value);
	}

	@Override
	public void updateLong(int index, long value) {
		// Nothing
	}

	@Override
	public void updateLong(String label, long value) {
		if (this.cursor != 0
			|| !this.statement.equals("write")) {
			return;
		}

		if (label.equals(SingleFileStorage.FIELD_SIZE)) {
			if (this.fileSize != value) {
				this.fileSize = value;
				this.storage.changeSize(value);
			}
		}
	}

	@Override
	public void updateString(int index, String value) {
		// Nothing
	}

	@Override
	public void updateString(String label, String value) {
		// Nothing
	}

	@Override
	public void updateBool(int index, boolean value) {
		// Nothing
	}

	@Override
	public void updateBool(String label, boolean value) {
		// Nothing
	}

	@Override
	public void updateRaw(String label, byte[] src, int offset, int length)
			throws StorageException {
		updateRaw(label, src, (long)offset, (long)length);
	}

	@Override
	public void updateRaw(String label, byte[] src, long offset, long length)
			throws StorageException {
		if (this.cursor != 0
			|| !this.statement.equals("write")
			|| !label.equals(SingleFileStorage.FIELD_DATA)) {
			return;
		}

		if (src.length < length) {
			throw new StorageException("Data source length less than input length param");
		}

		this.storage.write(src, offset, length);
	}
}
