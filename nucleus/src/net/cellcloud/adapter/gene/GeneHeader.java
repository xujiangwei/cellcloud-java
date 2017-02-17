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

/**
 * Gene 数据单元，内置支持的数据头名称。
 * 
 * @author Ambrose Xu
 *
 */
public class GeneHeader {

	/**
	 * 数据源的 Tag 信息。
	 */
	public final static String SourceTag = "SourceTag";

	/**
	 * Gene 所属的主机连接地址。
	 */
	public final static String Host = "Host";

	/**
	 * Gene 所属的主机连接端口。
	 */
	public final static String Port = "Port";

	/**
	 * Gene 的序号。在整个 Nucleus 生命周期里，该序号唯一。
	 */
	public final static String Seq = "Seq";

	/**
	 * Gene 的负载类型。
	 */
	public final static String PayloadType = "PayloadType";

	private GeneHeader() {
	}

}
