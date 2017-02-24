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

package net.cellcloud.core;

/**
 * 终端节点。
 * 
 * @author Ambrose Xu
 * 
 */
public class Endpoint {

	/** 访问地址。 */
	private String host;
	/** 访问端口。 */
	private int port;

	/** 终端的内核标签。 */
	private NucleusTag tag;
	/** 终端的角色。 */
	private Role role;

	private int hashCode = 0;

	/**
	 * 构造函数。
	 */
	public Endpoint(String host, int port) {
		this.host = host;
		this.port = port;
	}

	/**
	 * 构造函数。
	 */
	public Endpoint(String tag, Role role, String host, int port) {
		this.tag = new NucleusTag(tag);
		this.role = role;
		this.host = host;
		this.port = port;
	}

	/**
	 * 构造函数。
	 */
	public Endpoint(NucleusTag tag, Role role, String host, int port) {
		this.tag = tag;
		this.role = role;
		this.host = host;
		this.port = port;
	}

	/**
	 * 获得访问地址。
	 * 
	 * @return 返回访问地址。
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * 获得访问端口。
	 * 
	 * @return 返回访问端口。
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * 获得终端标签。
	 * 
	 * @return 返回终端的内核标签。
	 */
	public NucleusTag getTag() {
		return this.tag;
	}

	/**
	 * 获得终端角色。
	 * 
	 * @return 返回终端角色。
	 */
	public Role getRole() {
		return this.role;
	}

	/**
	 * 设置终端的内核标签。
	 * 
	 * @param tag 指定内核标签。
	 */
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
				if (other.host.equals(this.host)
					&& other.port == this.port) {
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
			buf.append(this.host);
			buf.append(":");
			buf.append(this.port);

			this.hashCode = buf.toString().hashCode();

			buf = null;
		}

		return this.hashCode;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(this.host);
		buf.append(":");
		buf.append(this.port);

		return buf.toString();
	}

}
