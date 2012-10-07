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

package net.cellcloud.talk.stuff;

/** 原语语素。
 * 
 * @author Jiangwei Xu
 */
public abstract class Stuff {

	private StuffType type;
	protected String value;
	protected LiteralBase literalBase;

	/** 构造函数。 */
	public Stuff(StuffType type, String value) {
		this.type = type;
		this.value = value;
		this.literalBase = LiteralBase.STRING;
	}

	/** 构造函数。 */
	public Stuff(StuffType type, int value) {
		this.type = type;
		this.value = Integer.toString(value);
		this.literalBase = LiteralBase.INT;
	}

	/** 构造函数。 */
	public Stuff(StuffType type, long value) {
		this.type = type;
		this.value = Long.toString(value);
		this.literalBase = LiteralBase.LONG;
	}

	/** 构造函数。 */
	public Stuff(StuffType type, boolean value) {
		this.type = type;
		this.value = Boolean.toString(value);
		this.literalBase = LiteralBase.BOOL;
	}

	/** 将自身语素数据复制给目标语素。 */
	abstract public void clone(Stuff target);

	/** 返回语素类型。
	 */
	public StuffType getType() {
		return this.type;
	}

	/** 按照字符串形式返回值。
	*/
	public String getValueAsString() {
		return this.value;
	}

	/** 按照整数形式返回值。
	*/
	public int getValueAsInt() {
		return Integer.parseInt(this.value);
	}

	/** 按照长整数形式返回值。
	*/
	public long getValueAsLong() {
		return Long.parseLong(this.value);
	}

	/** 按照布尔值形式返回值。
	*/
	public boolean getValueAsBool() {
		if (this.value.equalsIgnoreCase("true")
			|| this.value.equalsIgnoreCase("yes")
			|| this.value.equalsIgnoreCase("1"))
			return true;
		else
			return false;
	}

	/** 返回数值字面义。
	*/
	public LiteralBase getLiteralBase() {
		return this.literalBase;
	}

	/** @private
	 */
	protected void setValue(String value) {
		this.value = value;
	}
	/** @private
	 */
	protected void setLiteralBase(LiteralBase literalBase) {
		this.literalBase = literalBase;
	}
}
