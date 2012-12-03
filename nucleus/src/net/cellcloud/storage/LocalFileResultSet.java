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

import net.cellcloud.core.LogLevel;
import net.cellcloud.core.Logger;
import net.cellcloud.exception.StorageException;
import net.cellcloud.storage.LocalFileStorage.ChunkBuffer;
import net.cellcloud.storage.LocalFileStorage.FileWrapper;

/** 单文件存储器结果集。
 * 
 * @author Jiangwei Xu
 */
public final class LocalFileResultSet implements ResultSet {

	private LocalFileStorage storage;
	private String operate;

	private int cursor;
	private ChunkBuffer buffer;

	private FileWrapper fileWrapper;

	public LocalFileResultSet(LocalFileStorage storage,
			final String operate, final FileWrapper fileWrapper) {
		this.storage = storage;
		this.operate = operate;
		this.fileWrapper = fileWrapper;
		this.cursor = -1;
		this.buffer = null;
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

		if (label.equals(LocalFileStorage.LABEL_LONG_SIZE)) {
			return this.fileWrapper.filesize;
		}
		else if (label.equals(LocalFileStorage.LABEL_LONG_LASTMODIFIED)) {
			return this.fileWrapper.lastModified;
		}

		return 0;
	}

	@Override
	public String getString(int index) {
		return null;
	}

	@Override
	public String getString(String label) {
		if (this.cursor != 0) {
			return null;
		}

		if (label.equals(LocalFileStorage.LABEL_STRING_FILENAME)) {
			return this.fileWrapper.filename;
		}

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

		if (label.equals(LocalFileStorage.LABEL_BOOL_EXIST)) {
			return this.fileWrapper.filesize > 0;
		}

		return false;
	}

	@Override
	public byte[] getRaw(String label, long offset, long length) {
		if (this.cursor != 0
			|| !label.equals(LocalFileStorage.LABEL_RAW_DATA)) {
			return null;
		}
		else if (offset < 0 || length <= 0 || offset >= this.fileWrapper.filesize) {
			return null;
		}

		byte[] fileData = new byte[(int)length];
		int datalen = this.fileWrapper.read(fileData, offset, (int)length);
		// 检查读取的数据长度是否与请求的一致
		if (datalen == length) {
			return fileData;
		}
		else {
			// 判断是否是读到数据尾
			if (offset + datalen == this.fileWrapper.filesize) {
				byte[] tbuf = new byte[datalen];
				System.arraycopy(fileData, 0, tbuf, 0, datalen);
				return tbuf;
			}

			if (null == this.buffer) {
				this.buffer = this.fileWrapper.readWithoutBuffer(offset, this.storage.chunkSize);
			}
			else {
				// 判断是否需要重新建立缓存
				if (offset >= (long)(this.buffer.offset + this.buffer.length)
					|| offset + length > (long)(this.buffer.offset + this.buffer.length)) {
					this.buffer = this.fileWrapper.readWithoutBuffer(offset, this.storage.chunkSize);
				}
			}

			int len = (int)length;
			if ((this.buffer.offset + this.buffer.length) < (offset + length)) {
				len = (int) ((this.buffer.offset + this.buffer.length) - offset);
			}

			fileData = new byte[len];
			try {
				System.arraycopy(this.buffer.data, (int)(offset - this.buffer.offset), fileData, 0, len);
			} catch (Exception e) {
				Logger.logException(e, LogLevel.ERROR);
			}
			return fileData;
		}
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
			|| !this.operate.equals("write")) {
			return;
		}

		if (label.equals(LocalFileStorage.LABEL_LONG_SIZE)) {
			this.fileWrapper.filesize = value;
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
			|| !this.operate.equals("write")
			|| !label.equals(LocalFileStorage.LABEL_RAW_DATA)) {
			return;
		}

		if (src.length < length) {
			throw new StorageException("Data source length less than input length param");
		}

		this.fileWrapper.write(src, offset, length);
	}

	@Override
	public void close() {
		this.fileWrapper.close();
	}
}
