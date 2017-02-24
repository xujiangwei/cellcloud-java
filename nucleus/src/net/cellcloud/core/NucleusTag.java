/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2017 Cell Cloud Team (www.cellcloud.net)

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

import java.util.UUID;

/**
 * 内核标签。
 * 
 * @author Ambrose Xu
 * 
 */
public final class NucleusTag {

	/** UUID 描述。 */
	private UUID uuid;
	/** 标签的字符串格式。 */
	private String strFormat;

	/**
	 * 构造函数。
	 * 生成随机标签。
	 */
	public NucleusTag() {
		this.uuid = UUID.randomUUID();
		this.strFormat = this.uuid.toString();
	}

	/**
	 * 构造函数。
	 * 根据标签字符串生成。
	 * 
	 * @param value 指定字符串形式的标签。
	 */
	public NucleusTag(String value) {
		this.uuid = UUID.fromString(value);
		this.strFormat = value;
	}

	/**
	 * 获得标签的字符串形式。
	 * 
	 * @return 返回字符串形式的标签。
	 */
	public String asString() {
		return this.strFormat;
	}

	/**
	 * 返回标签的字符串形式。
	 */
	@Override
	public String toString() {
		return this.strFormat;
	}

	@Override
	public boolean equals(Object obj) {
		if (null != obj && obj instanceof NucleusTag) {
			NucleusTag other = (NucleusTag) obj;
			if (other.strFormat.equals(this.strFormat)) {
				return true;
			}
		}

		return false;
	}

}
