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

package net.cellcloud.cluster;


/**
 * 节点数据块。
 * 
 * @author Ambrose Xu
 * 
 */
public class Chunk {

	/** 块标签。 */
	private String label;
	/** 块数据。 */
	private byte[] data;
	/** 块数据长度。 */
	private int length;

	/**
	 * 构造器。
	 * 指定块标签和数据构建数据块。
	 * 
	 * @param label 指定标签。
	 * @param data 指定数据。
	 */
	public Chunk(String label, byte[] data) {
		this.label = label;
		this.data = data;
		this.length = data.length;
	}

	/**
	 * 构造器。
	 * 指定块标签和数据构建数据块。
	 * 
	 * @param label 指定标签。
	 * @param data 指定数据。
	 * @param length 指定数据长度。
	 */
	public Chunk(String label, byte[] data, int length) {
		this.label = label;
		this.data = new byte[length];
		System.arraycopy(data, 0, this.data, 0, length);
		this.length = length;
	}

	/**
	 * 获得块标签。
	 * 
	 * @return 返回块标签。
	 */
	public String getLabel() {
		return this.label;
	}

	/**
	 * 获得块数据。
	 * 
	 * @return 返回字节数组形式的块数据。
	 */
	public byte[] getData() {
		return this.data;
	}

	/**
	 * 获得块数据长度。
	 * 
	 * @return 返回块数据长度。
	 */
	public int getLength() {
		return this.length;
	}

}
