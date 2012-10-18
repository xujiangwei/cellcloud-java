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

package net.cellcloud.talk.dialect;

import net.cellcloud.talk.stuff.Primitive;

/** 原语方言
 * 
 * @author Jiangwei Xu
 */
public abstract class Dialect {

	private String name;
	private String tracker;
	private String tag;

	public Dialect(String name, String tracker) {
		this.name = name;
		this.tracker = tracker;
	}

	/** 返回方言名。
	 */
	public String getName() {
		return this.name;
	}

	/** 返回方言追踪名。
	 */
	public String getTracker() {
		return this.tracker;
	}

	/** 设置源标签。
	 */
	public void setTag(final String tag) {
		this.tag = tag;
	}

	/** 返回源标签。
	 */
	public String getTag() {
		return this.tag;
	}

	/** 翻译原语为方言。
	 */
	abstract public Primitive translate(final String tag);

	/** 从原语构建方言。
	 */
	abstract public void build(Primitive primitive);
}
