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
import java.util.Vector;

/** 终端节点。
 * 
 * @author Jiangwei Xu
 */
public final class EndpointNode extends Endpoint implements Comparable<EndpointNode> {

	private long hashCode;
	private Vector<EndpointNode> children;
	private Vector<ClusterConnector> connectors;

	/** 构造函数。
	 */
	public EndpointNode(long hash, InetSocketAddress address) {
		super(Nucleus.getInstance().getTag(), NucleusConfig.Role.NODE, address);
		this.hashCode = hash;
		this.children = new Vector<EndpointNode>();
		this.connectors = new Vector<ClusterConnector>();
	}

	/** 节点 Hash 值。
	 */
	public long getHashCode() {
		return this.hashCode;
	}

	/** 添加子节点。
	 */
	public void addChild(EndpointNode node, ClusterConnector connector) {
		if (!this.children.contains(node)) {
			this.children.add(node);
			this.connectors.add(connector);
		}
	}

	/** 关闭所有连接器。
	 */
	public void closeAllConnectors() {
		for (ClusterConnector c : this.connectors) {
			c.closeConnector();
		}
	}

	@Override
	public int compareTo(EndpointNode other) {
		return (int)(this.hashCode - other.hashCode);
	}

	@Override
	public int hashCode() {
		return (int)this.hashCode;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof EndpointNode) {
			EndpointNode node = (EndpointNode)other;
			if (node.hashCode == this.hashCode) {
				return true;
			}
		}

		return false;
	}
}
