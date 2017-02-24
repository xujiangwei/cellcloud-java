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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

	/** 物理节点包含的虚拟节点。 */
	private TreeMap<Long, ClusterVirtualNode> ownVirtualNodes;
	/** 物理兄弟节点。 */
	private TreeMap<Long, ClusterNode> brotherNodes;

	/** 虚节点散列码表。 */
	private Long[] virtualNodeHashCodes;
	/** 虚拟节点的散列码对应的节点实例。 */
	private HashMap<Long, ClusterVirtualNode> virtualNodes;

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

		if (numVNode > 0) {
			// 创建虚节点
			this.ownVirtualNodes = new TreeMap<Long, ClusterVirtualNode>();
			for (int i = 0; i < numVNode; ++i) {
				long hash = ClusterController.hashVNode(address, i + 1);
				ClusterVirtualNode vn = new ClusterVirtualNode(this, hash, address);
				this.ownVirtualNodes.put(vn.getHashCode(), vn);
			}

			this.updateVNodeHash();
		}
	}

	/**
	 * 构造函数。
	 * 
	 * @param hashCode 指定节点散列码。
	 * @param address 指定本机地址。
	 * @param vnodeHashList 指定虚拟节点的散列码。
	 */
	public ClusterNode(long hashCode, InetSocketAddress address, List<Long> vnodeHashList) {
		super(Nucleus.getInstance().getTag(), Role.NODE, address.getHostString(), address.getPort());
		this.hashCode = hashCode;

		if (!vnodeHashList.isEmpty()) {
			// 创建虚节点
			this.ownVirtualNodes = new TreeMap<Long, ClusterVirtualNode>();
			for (Long hash : vnodeHashList) {
				ClusterVirtualNode vn = new ClusterVirtualNode(this, hash.longValue(), address);
				this.ownVirtualNodes.put(vn.getHashCode(), vn);
			}

			this.updateVNodeHash();
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
		synchronized (this) {
			return (null != this.brotherNodes) ? this.brotherNodes.containsKey(hashCode) : false;
		}
	}

	/**
	 * 添加兄弟节点。
	 * 
	 * @param brother 指定需添加的兄弟节点。
	 */
	public void addBrother(ClusterNode brother) {
		synchronized (this) {
			if (null == this.brotherNodes) {
				this.brotherNodes = new TreeMap<Long, ClusterNode>();
			}
			this.brotherNodes.put(brother.getHashCode(), brother);

			this.updateVNodeHash();
		}
	}

	/**
	 * 获得自己包含的虚拟节点。
	 * 
	 * @return 返回自己包含的虚拟节点的原始容器。
	 */
	public Collection<ClusterVirtualNode> getOwnVirtualNodes() {
		synchronized (this) {
			return (null != this.ownVirtualNodes) ? this.ownVirtualNodes.values() : null;
		}
	}

	/**
	 * 是否包含指定散列码的虚拟节点。
	 * 
	 * @param hashCode 指定散列码。
	 * @return 如果包含返回 <code>true</code> 。
	 */
	public boolean containsOwnVirtualNode(long hashCode) {
		synchronized (this) {
			return (null != this.ownVirtualNodes) ? this.ownVirtualNodes.containsKey(hashCode) : false;
		}
	}
	/**
	 * 是否包含指定散列码的虚拟节点。
	 * 
	 * @param hashCode 指定散列码。
	 * @return 如果包含返回 <code>true</code> 。
	 */
	public boolean containsOwnVirtualNode(Long hashCode) {
		synchronized (this) {
			return (null != this.ownVirtualNodes) ? this.ownVirtualNodes.containsKey(hashCode) : false;
		}
	}

	/**
	 * 获得包含的指定散列码的虚拟节点
	 * 
	 * @param hashCode 指定散列码。
	 * @return 如果找到节点返回节点实例，否则返回 <code>null</code> 值。
	 */
	public ClusterVirtualNode getOwnVirtualNode(long hashCode) {
		synchronized (this) {
			return this.ownVirtualNodes.get(hashCode);
		}
	}

	/**
	 * 清空所有兄弟节点和虚拟节点。
	 */
	public void clearup() {
		synchronized (this) {
			if (null != this.ownVirtualNodes) {
				this.ownVirtualNodes.clear();
			}

			if (null != this.brotherNodes) {
				this.brotherNodes.clear();
			}

			this.virtualNodeHashCodes = null;

			if (null != this.virtualNodes) {
				this.virtualNodes.clear();
				this.virtualNodes = null;
			}
		}
	}

	/**
	 * 获得整个集群里已知的虚节点散列码列表。
	 * 
	 * @return 返回整个集群里已知的虚节点散列码列表。
	 */
	public Long[] getVirtualNodeHashList() {
		synchronized (this) {
			return this.virtualNodeHashCodes;
		}
	}

	/**
	 * 获得整个集群里指定散列码的虚拟节点。
	 * 
	 * @param nodeHash 指定节点散列码。
	 * @return 如果找到返回节点实例，否则返回 <code>null</code> 值。
	 */
	public ClusterVirtualNode getVirtualNode(Long nodeHash) {
		synchronized (this) {
			return this.virtualNodes.get(nodeHash);
		}
	}

	/**
	 * 根据指定散列码返回最优虚拟节点的散列码。
	 * 
	 * @param hash 指定需计算的散列码。
	 * @return 返回被选中的虚拟节点的散列码。
	 */
	public Long findVNodeHash(long hash) {
		synchronized (this) {
			if (null == this.virtualNodeHashCodes) {
				return null;
			}

			int index = this.doBinarySearchHash(0, this.virtualNodeHashCodes.length - 1, hash, this.virtualNodeHashCodes);
			return this.virtualNodeHashCodes[index];
		}
	}

	/**
	 * 根据指定散列码返回最优虚拟节点。
	 * 
	 * @param hash 指定需计算的散列码。
	 * @return 返回被选中的虚拟节点。
	 */
	protected ClusterVirtualNode selectVNode(long hash) {
		Long vhash = this.findVNodeHash(hash);
		if (null == vhash) {
			// 未找到节点
			return null;
		}

		synchronized (this) {
			if (null != this.virtualNodes && this.virtualNodes.containsKey(vhash)) {
				return this.virtualNodes.get(vhash);
			}
			else {
				return null;
			}
		}
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
			ClusterNode node = (ClusterNode)other;
			if (node.hashCode == this.hashCode) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 将当前的虚拟节点按照散列算法插入到表中。
	 */
	private void updateVNodeHash() {
		ArrayList<Long> list = new ArrayList<Long>();

		if (null == this.virtualNodes) {
			this.virtualNodes = new HashMap<Long, ClusterVirtualNode>();
		}
		else {
			this.virtualNodes.clear();
		}

		Iterator<Map.Entry<Long, ClusterVirtualNode>> iter = this.ownVirtualNodes.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<Long, ClusterVirtualNode> e = iter.next();
			// 添加散列码
			list.add(e.getKey());
			// 添加节点
			this.virtualNodes.put(e.getKey(), e.getValue());
		}

		if (null != this.brotherNodes && !this.brotherNodes.isEmpty()) {
			Iterator<ClusterNode> niter = this.brotherNodes.values().iterator();
			while (niter.hasNext()) {
				ClusterNode n = niter.next();
				if (null == n.ownVirtualNodes) {
					continue;
				}

				iter = n.ownVirtualNodes.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry<Long, ClusterVirtualNode> e = iter.next();
					// 添加散列码
					list.add(e.getKey());
					// 添加节点
					this.virtualNodes.put(e.getKey(), e.getValue());
				}
			}
		}

		// 排序
		Collections.sort(list);

		this.virtualNodeHashCodes = null;
		this.virtualNodeHashCodes = new Long[list.size()];
		list.toArray(this.virtualNodeHashCodes);

		list = null;
	}

}
