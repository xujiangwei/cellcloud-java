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

package net.cellcloud.cluster.protocol;

import java.util.Map;

import net.cellcloud.cluster.ClusterNode;
import net.cellcloud.common.Session;
import net.cellcloud.core.Nucleus;
import net.cellcloud.util.Utils;

/**
 * 集群发现协议。
 * 
 * @author Ambrose Xu
 * 
 */
public class ClusterDiscoveringProtocol extends ClusterProtocol {

	/**
	 * 协议名。
	 */
	public final static String NAME = "Discovering";

	/**
	 * 数据键：网络层IP。
	 */
	public final static String KEY_SOURCE_IP = "Source-IP";
	/**
	 * 数据键：网络层端口。
	 */
	public final static String KEY_SOURCE_PORT = "Source-Port";
	/**
	 * 数据键：源散列码。
	 */
	public final static String KEY_SOURCE_HASH = "Source-Hash";

	public final static String KEY_HASH = "Hash";

	/** 源IP地址。 */
	private String sourceIP = null;
	/** 源端口号。 */
	private int sourcePort = 0;
	/** 源散列码。 */
	private long sourceHash = 0;
	/** 源节点。 */
	private ClusterNode sourceNode = null;

	/**
	 * 构造函数。
	 * 
	 * @param sourceIP 指定源IP地址。
	 * @param sourcePort 指定源端口号。
	 * @param node 指定源节点。
	 */
	public ClusterDiscoveringProtocol(String sourceIP, int sourcePort, ClusterNode sourceNode) {
		super(ClusterDiscoveringProtocol.NAME);
		super.setProp(KEY_SN, Utils.randomNumberString(16));
		this.sourceIP = sourceIP;
		this.sourcePort = sourcePort;
		this.sourceNode = sourceNode;
	}

	/**
	 * 构造函数。
	 * 
	 * @param prop 指定数据键值对创建协议。
	 */
	public ClusterDiscoveringProtocol(Map<String, Object> prop) {
		super(ClusterDiscoveringProtocol.NAME, prop);
	}

	/**
	 * 返回源 IP 。
	 */
	public String getSourceIP() {
		if (null != this.sourceIP) {
			return this.sourceIP;
		}

		Object value = this.getProp(KEY_SOURCE_IP);
		if (null != value) {
			this.sourceIP = value.toString();
		}

		return this.sourceIP;
	}

	/**
	 * 返回源端口。
	 */
	public int getSourcePort() {
		if (0 != this.sourcePort) {
			return this.sourcePort;
		}

		Object value = this.getProp(KEY_SOURCE_PORT);
		if (null != value) {
			this.sourcePort = Integer.parseInt(value.toString());
		}

		return this.sourcePort;
	}

	/**
	 * 返回虚拟节点的 Hash 码列表。
	 */
	public long getSourceHash() {
		if (0 != this.sourceHash) {
			return this.sourceHash;
		}

		Object value = this.getProp(KEY_SOURCE_HASH);
		if (null != value) {
			this.sourceHash = Long.parseLong(value.toString());
		}

		return this.sourceHash;
	}

	/**
	 * 获得应答终端的 Hash 值。
	 * 
	 * @return 返回应答终端的 Hash 值。
	 */
	public long getResponseHash() {
		Object value = this.getProp(KEY_HASH);
		return Long.parseLong(value.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void launch(Session session) {
		StringBuilder buf = new StringBuilder();
		buf.append(KEY_PROTOCOL).append(":").append(ClusterDiscoveringProtocol.NAME).append("\n");
		buf.append(KEY_TAG).append(":").append(Nucleus.getInstance().getTagAsString()).append("\n");
		buf.append(KEY_SN).append(":").append(super.getSN()).append("\n");
		buf.append(KEY_DATE).append(":").append(super.getStandardDate()).append("\n");
		buf.append(KEY_SOURCE_IP).append(":").append(this.sourceIP).append("\n");
		buf.append(KEY_SOURCE_PORT).append(":").append(this.sourcePort).append("\n");
		buf.append(KEY_SOURCE_HASH).append(":").append(this.sourceNode.getHashCode()).append("\n");

		// 发送
		this.touch(session, buf, null);
		buf = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void respond(ClusterNode node, StateCode state, Object custom) {
		StringBuilder buf = new StringBuilder();
		buf.append(KEY_PROTOCOL).append(":").append(ClusterDiscoveringProtocol.NAME).append("\n");
		buf.append(KEY_TAG).append(":").append(Nucleus.getInstance().getTagAsString()).append("\n");
		buf.append(KEY_SN).append(":").append(super.getSN()).append("\n");
		buf.append(KEY_DATE).append(":").append(super.getStandardDate()).append("\n");
		buf.append(KEY_STATE).append(":").append(state.getCode()).append("\n");
		buf.append(KEY_HASH).append(":").append(node.getHashCode()).append("\n");

		// 发送
		this.touch(this.contextSession, buf, null);
		buf = null;
	}

	/**
	 * 拒绝被发现。
	 */
	public void reject() {
		StringBuilder buf = new StringBuilder();
		buf.append(KEY_PROTOCOL).append(":").append(ClusterDiscoveringProtocol.NAME).append("\n");
		buf.append(KEY_TAG).append(":").append(Nucleus.getInstance().getTagAsString()).append("\n");
		buf.append(KEY_SN).append(":").append(super.getSN()).append("\n");
		buf.append(KEY_DATE).append(":").append(super.getStandardDate()).append("\n");
		buf.append(KEY_STATE).append(":").append(ClusterProtocol.StateCode.REJECT.getCode()).append("\n");

		// 发送
		this.touch(this.contextSession, buf, null);
		buf = null;
	}

}
