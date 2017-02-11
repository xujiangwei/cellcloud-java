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

package net.cellcloud.core;


/** 终端节点。
 * 
 * @author Jiangwei Xu
 */
public class Endpoint {

	private NucleusTag tag;
	private Role role;
	private Coordinate coordinate;

	private int hashCode = 0;

	/** 构造函数。
	 */
	public Endpoint(String host, int port) {
		this.coordinate = new Coordinate(host, port);
	}

	/** 构造函数。
	 */
	public Endpoint(String tag, Role role, String host, int port) {
		this.tag = new NucleusTag(tag);
		this.role = role;
		this.coordinate = new Coordinate(host, port);
	}

	/** 构造函数。
	 */
	public Endpoint(NucleusTag tag, Role role, String host, int port) {
		this.tag = tag;
		this.role = role;
		this.coordinate = new Coordinate(host, port);
	}

	/** 返回终端标签。
	 */
	public NucleusTag getTag() {
		return this.tag;
	}

	/** 返回终端角色。
	 */
	public Role getRole() {
		return this.role;
	}

	/** 返回终端坐标。
	 */
	public Coordinate getCoordinate() {
		return this.coordinate;
	}

	public void setTag(NucleusTag tag) {
		this.tag = tag;
	}

	@Override
	public boolean equals(Object obj) {
		if (null != obj && obj instanceof Endpoint) {
			Endpoint other = (Endpoint) obj;
			if (null != other.tag && null != this.tag) {
				return other.tag.equals(this.tag);
			}
			else {
				if (other.coordinate.address.equals(this.coordinate.address)
					&& other.coordinate.port == this.coordinate.port) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public int hashCode() {
		if (this.hashCode == 0) {
			StringBuilder buf = new StringBuilder();
			buf.append(this.coordinate.address);
			buf.append(":");
			buf.append(this.coordinate.port);
			if (null != this.tag) {
				buf.append("#").append(this.tag.asString());
			}

			this.hashCode = buf.toString().hashCode();
		}

		return this.hashCode;
	}

	/** 终端坐标。
	 */
	public final class Coordinate {
		private String address;
		private int port;

		public Coordinate(String address, int port) {
			this.address = address;
			this.port = port;
		}

		/** 返回访问地址。
		 */
		public String getAddress() {
			return this.address;
		}

		/** 返回访问端口。
		 */
		public int getPort() {
			return this.port;
		}
	}

}
