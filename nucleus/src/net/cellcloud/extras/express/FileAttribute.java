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

package net.cellcloud.extras.express;

import java.util.Date;

import net.cellcloud.util.Util;

/** 文件属性描述类。
 * 
 * @author Jiangwei Xu
 */
public final class FileAttribute {

	private final static char ATTR_SEPARATOR = '|';

	protected boolean exist = false;
	protected long size = -1;
	protected String hashCode = "7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f";
	protected Date lastModifyTime;

	public FileAttribute() {
	}

	public boolean exist() {
		return this.exist;
	}

	public long size() {
		return this.size;
	}

	public Date lastModifyTime() {
		return this.lastModifyTime;
	}

	public String fileHashCode() {
		return this.hashCode;
	}

	public byte[] serialize() {
		// 序列化格式：是否存在|文件长度|最后修改日期|MD5码

		if (this.exist) {
			StringBuilder buf = new StringBuilder();
			buf.append('1');
			buf.append(ATTR_SEPARATOR);
			buf.append(this.size);
			buf.append(ATTR_SEPARATOR);
			buf.append(Util.convertDateToSimpleString(this.lastModifyTime));
			buf.append(ATTR_SEPARATOR);
			buf.append(this.hashCode);

			return buf.toString().getBytes();
		}
		else {
			byte[] ret = {'0'};
			return ret;
		}
	}

	public void deserialize(byte[] bytes) {
		// 序列化格式：是否存在|文件长度|最后修改日期|MD5码
		String str = new String(bytes);
		String[] array = str.split("\\|");
		if (array.length == 1) {
			this.exist = false;
		}
		else {
			this.exist = true;
			this.size = Long.parseLong(array[1]);
			this.lastModifyTime = Util.convertSimpleStringToDate(array[2]);
			this.hashCode = array[3];
		}
	}
}
