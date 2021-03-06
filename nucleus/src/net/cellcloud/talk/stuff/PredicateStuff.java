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

package net.cellcloud.talk.stuff;

import javax.xml.transform.TransformerException;

import org.json.JSONObject;
import org.w3c.dom.Document;

/**
 * 谓语语素。
 * 
 * @author Ambrose Xu
 * 
 */
public final class PredicateStuff extends Stuff {

	/**
	 * 构造函数。
	 */
	protected PredicateStuff() {
		super(StuffType.PREDICATE);
	}

	/**
	 * 构造函数。
	 * 
	 * @param value 指定语义为二进制的数据。
	 */
	public PredicateStuff(byte[] value) {
		super(StuffType.PREDICATE, value);
	}

	/**
	 * 构造函数。
	 * 
	 * @param value 指定语义为字符串的数据。
	 */
	public PredicateStuff(String value) {
		super(StuffType.PREDICATE, value);
	}

	/**
	 * 构造函数。
	 * 
	 * @param value 指定语义为整数的数据。
	 */
	public PredicateStuff(int value) {
		super(StuffType.PREDICATE, value);
	}

	/**
	 * 构造函数。
	 * 
	 * @param value 指定语义为长整数的数据。
	 */
	public PredicateStuff(long value) {
		super(StuffType.PREDICATE, value);
	}

	/**
	 * 构造函数。
	 * 
	 * @param value 指定语义为浮点数的数据。
	 */
	public PredicateStuff(float value) {
		super(StuffType.PREDICATE, value);
	}

	/**
	 * 构造函数。
	 * 
	 * @param value 指定语义为双精浮点数的数据。
	 */
	public PredicateStuff(double value) {
		super(StuffType.PREDICATE, value);
	}

	/**
	 * 构造函数。
	 * 
	 * @param value 指定语义为布尔值的数据。
	 */
	public PredicateStuff(boolean value) {
		super(StuffType.PREDICATE, value);
	}

	/**
	 * 构造函数。
	 * 
	 * @param value 指定语义为 JSON 类型的数据。
	 */
	public PredicateStuff(JSONObject value) {
		super(StuffType.PREDICATE, value);
	}

	/**
	 * 构造函数。 
	 * 
	 * @param value 指定语义为 XML 类型的数据。
	 * @throws TransformerException
	 */
	public PredicateStuff(Document value) throws TransformerException {
		super(StuffType.PREDICATE, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clone(Stuff target) {
		if (target.getType() == StuffType.PREDICATE) {
			target.setValue(this.value);
			target.setLiteralBase(this.literalBase);
		}
	}

}
