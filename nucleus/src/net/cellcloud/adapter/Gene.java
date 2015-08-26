/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2015 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.adapter;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Jiangwei Xu
 */
public class Gene {

	private String name;
	private HashMap<String, String> header;
	private String body;

	public Gene(String name) {
		this.name = name;
		this.header = new HashMap<String, String>();
	}

	public String getName() {
		return this.name;
	}

	public void setHeader(String name, String value) {
		this.header.put(name, value);
	}

	public String getHeader(String name) {
		return this.header.get(name);
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getBody() {
		return this.body;
	}

	public byte[] packet() {
		StringBuilder buf = new StringBuilder();

		buf.append(this.name).append("\r\n");

		for (Map.Entry<String, String> e : this.header.entrySet()) {
			buf.append(e.getKey()).append(":").append(e.getValue()).append("\r\n");
		}

		if (null != this.body) {
			buf.append("\r\n");
			buf.append(this.body).append("\r\n");
		}

		buf.append("\0");

		String str = buf.toString();
		buf = null;

		return str.getBytes(Charset.forName("UTF-8"));
	}
}
