/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (www.cellcloud.net)

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

/** 内核标签。
 * 
 * @author Jiangwei Xu
 */
public final class NucleusTag {

	private UUID soul;
	private String strFormat;

	/** 构造函数。生成随机标签。
	 */
	public NucleusTag() {
		this.soul = UUID.randomUUID();
		this.strFormat = this.soul.toString();
	}

	/** 构造函数。根据标签字符串生成。
	 */
	public NucleusTag(String value) {
		this.soul = UUID.fromString(value);
		this.strFormat = value;
	}

	/** 返回字符串格式。
	 */
	public String asString() {
		return this.strFormat;
	}

	/** 返回字符串格式。
	 */
	@Override
	public String toString() {
		return this.strFormat;
	}
}
