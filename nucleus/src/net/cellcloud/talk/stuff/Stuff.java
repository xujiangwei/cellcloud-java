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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.text.DecimalFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** 原语语素。
 * 
 * @author Jiangwei Xu
 */
public abstract class Stuff {

	private static final DecimalFormat DF = new DecimalFormat("#0.0000");

	private StuffType type;
	protected String value;
	protected LiteralBase literalBase;

	/** 构造函数。 */
	public Stuff(StuffType type) {
		this.type = type;
	}

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
	public Stuff(StuffType type, float value) {
		this.type = type;
		this.value = DF.format(value);
		this.literalBase = LiteralBase.FLOAT;
	}

	/** 构造函数。 */
	public Stuff(StuffType type, double value) {
		this.type = type;
		this.value = DF.format(value);
		this.literalBase = LiteralBase.DOUBLE;
	}

	/** 构造函数。 */
	public Stuff(StuffType type, boolean value) {
		this.type = type;
		this.value = Boolean.toString(value);
		this.literalBase = LiteralBase.BOOL;
	}

	/** 构造函数。 */
	public Stuff(StuffType type, JSONObject json) {
		this.type = type;
		this.value = json.toString();
		this.literalBase = LiteralBase.JSON;
	}

	/** 构造函数。 */
	public Stuff(StuffType type, Document doc)
			throws TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		t.setOutputProperty("encoding", "UTF-8");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		t.transform(new DOMSource(doc), new StreamResult(bos));
		this.value = bos.toString();
		this.type = type;
		this.literalBase = LiteralBase.XML;
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

	/** 按照浮点数形式返回值。
	 */
	public float getValueAsFloat() {
		return Float.parseFloat(this.value);
	}

	/** 按照双精浮点数形式返回值。
	 */
	public double getValueAsDouble() {
		return Double.parseDouble(this.value);
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

	/** 按照 JSON 格式返回值。
	 * @throws JSONException 
	 */
	public JSONObject getValueAsJSON()
			throws JSONException {
		return new JSONObject(this.value);
	}

	/** 按照 XML 格式返回值。
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public Document getValueAsXML()
			throws ParserConfigurationException, SAXException, IOException {
		String xmlStr = new String(this.value.getBytes(), Charset.forName("UTF-8"));
		StringReader sr = new StringReader(xmlStr);
		InputSource is = new InputSource(sr);
		DocumentBuilderFactory factory =  DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(is);
	    return doc;
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
	protected void setValue(int value) {
		this.value = Integer.toString(value);
	}
	/** @private
	 */
	protected void setValue(long value) {
		this.value = Long.toString(value);
	}
	/** @private
	 */
	protected void setValue(boolean value) {
		this.value = Boolean.toString(value);
	}
	/** @private
	 */
	protected void setValue(float value) {
		this.value = DF.format(value);
	}
	/** @private
	 */
	protected void setValue(double value) {
		this.value = DF.format(value);
	}
	/** @private
	 */
	protected void setValue(JSONObject json) {
		this.value = json.toString();
	}

	/** @private
	 */
	protected void setLiteralBase(LiteralBase literalBase) {
		this.literalBase = literalBase;
	}
}
