/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2013 Cell Cloud Team (www.cellcloud.net)

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

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.json.JSONObject;
import org.w3c.dom.Document;

/** 状语语素。
 * 
 * @author Jiangwei Xu
 */
public final class AdverbialStuff extends Stuff {

	/** 构造函数。 */
	protected AdverbialStuff() {
		super(StuffType.ADVERBIAL);
	}

	/** 构造函数。 */
	public AdverbialStuff(String value) {
		super(StuffType.ADVERBIAL, value);
	}

	/** 构造函数。 */
	public AdverbialStuff(int value) {
		super(StuffType.ADVERBIAL, value);
	}

	/** 构造函数。 */
	public AdverbialStuff(long value) {
		super(StuffType.ADVERBIAL, value);
	}

	/** 构造函数。 */
	public AdverbialStuff(float value) {
		super(StuffType.ADVERBIAL, value);
	}

	/** 构造函数。 */
	public AdverbialStuff(boolean value) {
		super(StuffType.ADVERBIAL, value);
	}

	/** 构造函数。 */
	public AdverbialStuff(JSONObject value) {
		super(StuffType.ADVERBIAL, value);
	}

	/** 构造函数。 
	 * @throws TransformerException 
	 * @throws TransformerConfigurationException
	 */
	public AdverbialStuff(Document value) throws TransformerException {
		super(StuffType.ADVERBIAL, value);
	}

	@Override
	public void clone(Stuff target) {
		if (target.getType() == StuffType.ADVERBIAL) {
			target.setValue(this.value);
			target.setLiteralBase(this.literalBase);
		}
	}
}
