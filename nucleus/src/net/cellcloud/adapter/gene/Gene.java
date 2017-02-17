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

/**
 * 用于描述内核适配器数据传输的最小数据单元。
 * 
 * Gene 数据单元由名称、数据头描述、数据负载3部分组成。其中数据负载是非必要部分。
 * 
 * @author Ambrose Xu
 *
 */
public class Gene {

	/**
	 * 数据名。
	 */
	private String name;

	/**
	 * 数据头内容。
	 */
	private HashMap<String, String> header;

	/**
	 * 数据负载内容。
	 */
	private String payload;

	public final static byte EOP_1 = 'E';
	public final static byte EOP_2 = 'O';
	public final static byte EOP_3 = 'P';
	public final static byte EOP_4 = '\r';
	public final static byte EOP_5 = '\n';

	/** 序列化数据的结束符。 */
	public final static byte[] EOP_BYTES = new byte[]{ EOP_1, EOP_2, EOP_3, EOP_4, EOP_5 };
	public final static String EOP = "EOP";

	/**
	 * 构造器。
	 * 
	 * @param name 指定数据名。
	 */
	public Gene(String name) {
		this(name, null);
	}

	/**
	 * 构造器。
	 * 
	 * @param name 指定数据名
	 * @param payload 指定数据负载内容。
	 */
	public Gene(String name, String payload) {
		this.name = name;
		this.payload = payload;
		this.header = new HashMap<String, String>();
	}

	/**
	 * 获得数据名。
	 * 
	 * @return 返回字符串形式的数据名。
	 */
	public String getName() {
		return this.name;
	}

	/** 设置数据头内容。
	 * 
	 * @param name 指定数据头名称。
	 * @param value 指定数据头名称对应的内容。
	 */
	public void setHeader(String name, String value) {
		this.header.put(name, value);
	}

	/**
	 * 从数据头里获得指定名称的数据内容。
	 * 
	 * @param name 指定数据头名称。
	 * @return 返回字符串形式的数据内容。
	 */
	public String getHeader(String name) {
		return this.header.get(name);
	}

	/**
	 * 设置数据负载内容。
	 * 
	 * @param payload 指定数据负责内容。
	 */
	public void setPayload(String payload) {
		this.payload = payload;
	}

	/**
	 * 获得数据负载内容。
	 * 
	 * @return 返回字符串形式的数据负载内容。
	 */
	public String getPayload() {
		return this.payload;
	}

	/**
	 * 将 Gene 数据打包为字节序列。
	 * 
	 * @param gene 指定需要打包的 Gene 对象实例。
	 * @return 返回字节数组形式的序列。
	 */
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

		// EOP 结束符
		buf.append(EOP).append("\r\n");

		String str = buf.toString();
		buf = null;

		return str.getBytes(Charset.forName("UTF-8"));
	}

	/**
	 * 将字节序列解包为 Gene 实例。
	 * 
	 * @param bytes 指定需要解包的字节序列。
	 * @return 返回 Gene 实例。如果序列的格式错误，返回 null 值。
	 */
	public static Gene unpack(byte[] bytes) {
		String data = new String(bytes, Charset.forName("UTF-8"));

		String[] ret = data.split("\\\r\\\n");
		if (ret.length < 2) {
			Logger.d(RelationNucleusAdapter.class, "Data format error");
			return null;
		}

		String name = ret[0];
		Gene gene = new Gene(name);

		for (int i = 1; i < ret.length; ++i) {
			String r = ret[i];

			if (r.equals(EOP)) {
				break;
			}

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
