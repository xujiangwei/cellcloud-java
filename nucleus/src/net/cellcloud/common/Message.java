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

package net.cellcloud.common;

import java.nio.charset.Charset;

/** 消息描述类。
 * 
 * @author Jiangwei Xu
 */
public class Message {

	private byte[] data;

	/** 构造函数。
	 */
	public Message(byte[] data) {
		this.data = data;
	}

	/** 构造函数。
	 */
	public Message(String data) {
		this.data = data.getBytes(Charset.forName("UTF-8"));
	}

	protected void set(byte[] newData) {
		this.data = newData;
	}

	/** 返回消息数据。
	 */
	public byte[] get() {
		return this.data;
	}

	/** 消息数据长度。
	 */
	public int length() {
		return this.data.length;
	}

	/** 返回 UTF-8 字符集编码的字符串形式的消息数据。
	 */
	public String getAsString() {
		return new String(this.data, Charset.forName("UTF-8"));
	}

	/** 返回指定字符集的消息数据的字符串形式。
	 */
	public String getAsString(String charsetName) {
		return new String(this.data, Charset.forName(charsetName));
	}
}
