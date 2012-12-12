/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2013 Cell Cloud Team (cellcloudproject@gmail.com)

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

import java.util.Map;

import net.cellcloud.common.Session;

/** 发现协议。
 * 
 * @author Jiangwei Xu
 */
public class ClusterDiscoveringProtocol extends ClusterProtocol {

	public final static String NAME = "discovering";

	// 开启的服务端口。
	public final static String KEY_PORT = "Port";

	private int port;
	private Map<String, String> prop;

	private ClusterNode root;

	/** 指定源端的端口创建协议。
	 */
	public ClusterDiscoveringProtocol(int port) {
		super(ClusterDiscoveringProtocol.NAME);
		this.port = port;
	}

	/** 指定数据键值对创建协议。
	 */
	public ClusterDiscoveringProtocol(Map<String, String> prop) {
		super(ClusterDiscoveringProtocol.NAME);
		this.prop = prop;
	}

	/** 返回协议内传输的标签。
	 */
	public String getTag() {
		return this.prop.get(KEY_TAG);
	}

	/** 返回协议状态。
	 */
	public int getState() {
		String szState = this.prop.get(KEY_STATE);
		if (null != szState) {
			return Integer.parseInt(szState);
		}

		return -1;
	}

	/** 指定根节点。
	 */
	public void setRootNode(ClusterNode node) {
		this.root = node;
	}

	@Override
	public void launch(Session session) {
		StringBuilder buf = new StringBuilder();
		buf.append(KEY_PROTOCOL).append(": ").append(ClusterDiscoveringProtocol.NAME).append("\n");
		buf.append(KEY_TAG).append(": ").append(Nucleus.getInstance().getTagAsString()).append("\n");
		buf.append(KEY_DATE).append(": ").append(super.getStandardDate()).append("\n");
		buf.append(KEY_PORT).append(": ").append(this.port).append("\n");

		this.touch(session, buf);
		buf = null;
	}

	@Override
	public void stack(Session session) {
		StringBuilder buf = new StringBuilder();
		buf.append(KEY_PROTOCOL).append(": ").append(ClusterDiscoveringProtocol.NAME).append("\n");
		buf.append(KEY_TAG).append(": ").append(Nucleus.getInstance().getTagAsString()).append("\n");
		buf.append(KEY_DATE).append(": ").append(super.getStandardDate()).append("\n");
		buf.append(KEY_STATE).append(": ").append(ClusterProtocol.StateCode.SUCCESS).append("\n");
		buf.append(KEY_HASHCODE).append(": ").append(this.root.getHashCode()).append("\n");

		this.touch(session, buf);
		buf = null;
	}

	@Override
	public void stackReject(Session session) {
		StringBuilder buf = new StringBuilder();
		buf.append(KEY_PROTOCOL).append(": ").append(ClusterDiscoveringProtocol.NAME).append("\n");
		buf.append(KEY_TAG).append(": ").append(Nucleus.getInstance().getTagAsString()).append("\n");
		buf.append(KEY_DATE).append(": ").append(super.getStandardDate()).append("\n");
		buf.append(KEY_STATE).append(": ").append(ClusterProtocol.StateCode.REJECT).append("\n");

		this.touch(session, buf);
		buf = null;
	}
}
