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
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

/** 集群节点。
 * 
 * @author Jiangwei Xu
 */
public class ClusterNode extends Endpoint implements Comparable<ClusterNode> {

	// 节点散列码
	private long hashCode;

	// 物理节点下的虚拟节点
	private TreeMap<Long, ClusterVirtualNode> virtualNodes;
	// 物理兄弟节点
	private TreeMap<Long, ClusterNode> brotherNodes;

	/** 构造函数。
	 */
	public ClusterNode(long hashCode, InetSocketAddress address, int numVNode) {
		super(Nucleus.getInstance().getTag(), NucleusConfig.Role.NODE, address);
		this.hashCode = hashCode;

		if (numVNode > 0) {
			// 创建虚节点
			this.virtualNodes = new TreeMap<Long, ClusterVirtualNode>();
			for (int i = 0; i < numVNode; ++i) {
				long hash = ClusterController.hashVNode(address, i + 1);
				ClusterVirtualNode vn = new ClusterVirtualNode(hash, address);
				this.virtualNodes.put(vn.getHashCode(), vn);
			}
		}
	}

	/** 构造函数。
	 */
	public ClusterNode(long hashCode, InetSocketAddress address, List<Long> vnodeHashList) {
		super(Nucleus.getInstance().getTag(), NucleusConfig.Role.NODE, address);
		this.hashCode = hashCode;

		if (!vnodeHashList.isEmpty()) {
			// 创建虚节点
			this.virtualNodes = new TreeMap<Long, ClusterVirtualNode>();
			for (Long hash : vnodeHashList) {
				ClusterVirtualNode vn = new ClusterVirtualNode(hash.longValue(), address);
				this.virtualNodes.put(vn.getHashCode(), vn);
			}
		}
	}

	/** 节点 Hash 值。
	 */
	public long getHashCode() {
		return this.hashCode;
	}

	/** 判断是否是兄弟节点。
	 */
	public boolean isBrotherNode(long hashCode) {
		synchronized (this) {
			return (null != this.brotherNodes) ? this.brotherNodes.containsKey(hashCode) : false;
		}
	}

	/** 添加兄弟节点。
	 */
	public void addBrother(ClusterNode brother) {
		if (null == this.brotherNodes) {
			this.brotherNodes = new TreeMap<Long, ClusterNode>();
		}
		this.brotherNodes.put(brother.getHashCode(), brother);
	}

	/** 返回所有虚拟节点。
	 */
	public Collection<ClusterVirtualNode> getVirtualNodes() {
		synchronized (this) {
			return (null != this.virtualNodes) ? this.virtualNodes.values() : null;
		}
	}

	/** 是否包含指定散列码的虚拟节点。
	 */
	public boolean containsVirtualNode(long hashCode) {
		synchronized (this) {
			return (null != this.virtualNodes) ? this.virtualNodes.containsKey(hashCode) : false;
		}
	}

	/** 清空所有兄弟节点和虚拟节点。
	 */
	public void clearup() {
		synchronized (this) {
			if (null != this.virtualNodes) {
				this.virtualNodes.clear();
			}
		}
	}

	@Override
	public int compareTo(ClusterNode other) {
		return (int)(this.hashCode - other.hashCode);
	}

	@Override
	public int hashCode() {
		return (int) (this.hashCode % Integer.MAX_VALUE);
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
