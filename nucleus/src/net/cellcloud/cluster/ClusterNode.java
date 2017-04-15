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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeMap;

import net.cellcloud.core.Endpoint;
import net.cellcloud.core.Nucleus;
import net.cellcloud.core.Role;

/**
 * 集群节点。
 * 
 * @author Ambrose Xu
 * 
 */
public class ClusterNode extends Endpoint implements Comparable<ClusterNode> {

	/** 节点散列码。 */
	private long hashCode;

	/** 物理兄弟节点。 */
	private TreeMap<Long, ClusterNode> brotherNodes;

	/** 物理节点包含的虚拟节点。 */
	private VirtualNode[] virtualNodes;

	/** Hash 环。 */
	private Long[] hashRing;

	/**
	 * 构造函数。
	 * 
	 * @param hashCode 指定节点散列码。
	 * @param address 指定本机地址。
	 * @param numVNode 指定包含虚拟节点数量。
	 */
	public ClusterNode(long hashCode, InetSocketAddress address) {
		this(hashCode, address, 0);
	}

	/**
	 * 构造函数。
	 * 
	 * @param hashCode 指定节点散列码。
	 * @param address 指定本机地址。
	 * @param numVNode 指定包含虚拟节点数量。
	 */
	public ClusterNode(long hashCode, InetSocketAddress address, int numVNode) {
		super(Nucleus.getInstance().getTag(), Role.NODE, address.getHostString(), address.getPort());
		this.hashCode = hashCode;

		this.brotherNodes = new TreeMap<Long, ClusterNode>();

		if (numVNode > 0) {
			// 创建虚节点
			this.virtualNodes = new VirtualNode[numVNode];
			for (int i = 0; i < numVNode; ++i) {
				this.virtualNodes[i] = new VirtualNode(this);
			}
		}
	}

	/**
	 * 获得节点散列码。
	 * 
	 * @return 返回节点散列码。
	 */
	public final long getHashCode() {
		return this.hashCode;
	}

	/**
	 * 判断是否是兄弟节点。
	 * 
	 * @param hashCode 指定待判断的散列码。
	 * @return 如果是兄弟节点返回 <code>true</code> ，否则返回 <code>false</code> 。
	 */
	public boolean isBrotherNode(long hashCode) {
		synchronized (this.brotherNodes) {
			return this.brotherNodes.containsKey(hashCode);
		}
	}

	/**
	 * 添加兄弟节点。
	 * 
	 * @param brother 指定需添加的兄弟节点。
	 */
	public void addBrother(ClusterNode brother) {
		synchronized (this.brotherNodes) {
			this.brotherNodes.put(brother.getHashCode(), brother);
		}

		this.updateHashRing();
	}

	/**
	 * 获得自己的虚拟节点。
	 * 
	 * @return 返回自己的虚拟节点数组。
	 */
	public VirtualNode[] getVirtualNodes() {
		synchronized (this) {
			return this.virtualNodes;
		}
	}

	/**
	 * 清空所有兄弟节点。
	 */
	public void clearup() {
		synchronized (this.brotherNodes) {
			this.brotherNodes.clear();
		}

		synchronized (this) {
			this.hashRing = null;
		}
	}

	/**
	 * 获得整个集群里已知的节点散列码有序列表。
	 * 
	 * @return 返回整个集群里的哈希环。
	 */
	public Long[] getHashRing() {
		synchronized (this) {
			return this.hashRing;
		}
	}

	/**
	 * 根据指定散列码返回最优节点。
	 * 
	 * @param hash 指定需计算的散列码。
	 * @return 返回被选中的节点。
	 */
	public ClusterNode findNode(long hash) {
		synchronized (this) {
			if (null == this.hashRing) {
				return null;
			}

			// 二分搜索 Hash 值
			int index = this.doBinarySearchHash(0, this.hashRing.length - 1, hash, this.hashRing);
			// 获得哈希环上的哈希值
			Long nodeHash = this.hashRing[index];

			if (nodeHash.longValue() == this.hashCode) {
				return this;
			}
			else {
				return this.brotherNodes.get(nodeHash);
			}
		}
	}

	/**
	 * 根据指定散列码返回最优节点。
	 * 
	 * @param hash 指定需计算的散列码。
	 * @return 返回被选中的节点。
	 */
	protected VirtualNode selectVirtualNode(long dataHash) {
		long mod = dataHash % (long) this.virtualNodes.length;
		return this.virtualNodes[(int)mod];
	}

	/**
	 * 用二分搜索查找指定 Key 值最佳插入点。
	 * 按照数据环方式递归遍历列表，按照自然数增序找到指定 Key 的插入位置。
	 */
	private int doBinarySearchHash(int low, int high, Long key, Long[] source) {
		if (low < high) {
			int mid = (int) ((low + high) * 0.5);
			int result = key.compareTo(source[mid]);

			if (result < 0) {
				return doBinarySearchHash(low, mid - 1, key, source);
			}
			else if (result > 0) {
				return doBinarySearchHash(mid + 1, high, key, source);
			}
			else {
				return mid;
			}
		}
		else if (low > high) {
			return low;
		}
		else {
			Long v = source[high];
			if (key.longValue() < v.longValue()) {
				return high;
			}
			else {
				// 判断索引是否超出最大范围，如果超出则折返至 0 索引位置
				if (high + 1 < source.length - 1)
					return high + 1;
				else
					return 0;
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
			ClusterNode node = (ClusterNode) other;
			if (node.hashCode == this.hashCode) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 将当前的虚拟节点按照散列算法插入到表中。
	 */
	private void updateHashRing() {
		ArrayList<Long> list = new ArrayList<Long>();

		synchronized (this.brotherNodes) {
			if (null != this.brotherNodes && !this.brotherNodes.isEmpty()) {
				Iterator<ClusterNode> iter = this.brotherNodes.values().iterator();
				while (iter.hasNext()) {
					list.add(iter.next().hashCode);
				}
			}
		}

		// 排序
		Collections.sort(list);

		synchronized (this) {
			this.hashRing = null;
			this.hashRing = new Long[list.size()];
			list.toArray(this.hashRing);
		}

		list.clear();
		list = null;
	}

}
