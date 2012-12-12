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

import java.util.Collection;
import java.util.TreeMap;

/** 集群节点。
 * 
 * @author Jiangwei Xu
 */
public final class ClusterNode extends Endpoint implements Comparable<ClusterNode> {

	private long hashCode;
	private TreeMap<Long, ClusterNode> children;
	private ClusterConnector connector;

	/** 构造函数。
	 */
	public ClusterNode(long hash, ClusterConnector connector) {
		super(Nucleus.getInstance().getTag(), NucleusConfig.Role.NODE
				, null != connector ? connector.getAddress() : null);
		this.hashCode = hash;
		this.children = new TreeMap<Long, ClusterNode>();
		this.connector = connector;
	}

	/** 节点 Hash 值。
	 */
	public long getHashCode() {
		return this.hashCode;
	}

	/** 添加子节点。
	 */
	public void addChild(ClusterNode node) {
		synchronized (this) {
			if (!this.children.containsKey(node.getHashCode())) {
				this.children.put(node.getHashCode(), node);
			}
		}
	}

	/** 返回所有子节点。
	 */
	public Collection<ClusterNode> getChildren() {
		synchronized (this) {
			return this.children.values();
		}
	}

	/** 是否包含指定散列码的节点。
	 */
	public boolean contains(long hashCode) {
		synchronized (this) {
			return this.children.containsKey(hashCode);
		}
	}

	/** 清空所有子节点。
	 */
	public void clear() {
		synchronized (this) {
			this.children.clear();
		}
	}

	/** 关闭连接器。
	 */
	public void closeConnector() {
		this.connector.close();
	}

	@Override
	public int compareTo(ClusterNode other) {
		return (int)(this.hashCode - other.hashCode);
	}

	@Override
	public int hashCode() {
		return (int)this.hashCode;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ClusterNode) {
			ClusterNode node = (ClusterNode)other;
			if (node.hashCode == this.hashCode) {
				return true;
			}
		}

		return false;
	}
}
