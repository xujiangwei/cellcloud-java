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

package net.cellcloud.adapter.gene;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import net.cellcloud.adapter.RelationNucleusAdapter;
import net.cellcloud.common.Logger;
import net.cellcloud.core.Endpoint;

/**
 * 
 * @author Jiangwei Xu
 */
public class Gene {

	private String name;
	private HashMap<String, String> header;
	private String payload;

	private Endpoint destination;

	public Gene(String name) {
		this(name, null);
	}

	public Gene(String name, String payload) {
		this.name = name;
		this.payload = payload;
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

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public String getPayload() {
		return this.payload;
	}

	public void forceDestination(Endpoint destination) {
		this.destination = destination;
	}

	public Endpoint getDestination() {
		return this.destination;
	}

	public static byte[] pack(Gene gene) {
		StringBuilder buf = new StringBuilder();

		buf.append(gene.name).append("\r\n");

		for (Map.Entry<String, String> e : gene.header.entrySet()) {
			buf.append(e.getKey()).append(":").append(e.getValue()).append("\r\n");
		}

		if (null != gene.payload) {
			buf.append("\r\n");
			buf.append(gene.payload).append("\r\n");
		}

		String str = buf.toString();
		buf = null;

		return str.getBytes(Charset.forName("UTF-8"));
	}

	public static Gene unpack(byte[] bytes) {
		String data = new String(bytes, Charset.forName("UTF-8"));

		String[] ret = data.split("\\\r\\\n");
		if (ret.length < 2) {
			Logger.w(RelationNucleusAdapter.class, "Data format error");
			return null;
		}

		String name = ret[0];
		Gene gene = new Gene(name);

		for (int i = 1; i < ret.length; ++i) {
			String r = ret[i];

			if (r.length() == 0) {
				gene.setPayload(ret[i+1]);
				break;
			}

			int index = r.indexOf(":");
			if (index > 0) {
				String key = r.substring(0, index);
				String value = r.substring(index + 1);
				gene.setHeader(key.trim(), value.trim());
			}
		}

		return gene;
	}

}
