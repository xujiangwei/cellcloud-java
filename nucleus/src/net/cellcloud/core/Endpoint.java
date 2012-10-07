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

package net.cellcloud.core;

import java.net.InetSocketAddress;

/** 云图中的节点。
 * 
 * @author Jiangwei Xu
 */
public final class Endpoint {

	private NucleusTag tag;
	private byte role;
	private Coordinate coordinate;

	/** 构造函数。
	 */
	public Endpoint(String tag, byte role, InetSocketAddress address) {
		this.tag = new NucleusTag(tag);
		this.role = role;
		this.coordinate = new Coordinate(address);
	}

	/** 返回终端标签。
	 */
	public NucleusTag getTag() {
		return this.tag;
	}

	/** 返回终端角色。
	 */
	public byte getRole() {
		return this.role;
	}

	/** 返回终端坐标。
	 */
	public Coordinate getCoordinate() {
		return this.coordinate;
	}

	/** 终端坐标。
	 */
	public class Coordinate {
		private InetSocketAddress address;

		public Coordinate(InetSocketAddress address) {
			this.address = address;
		}

		public InetSocketAddress getAddress() {
			return this.address;
		}
	}
}
