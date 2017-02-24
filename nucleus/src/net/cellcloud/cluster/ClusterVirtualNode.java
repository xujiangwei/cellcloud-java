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
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集群虚拟节点。
 * 
 * @author Ambrose Xu
 * 
 */
public class ClusterVirtualNode extends ClusterNode {

	/**
	 * 此虚拟节点关联的物理节点。
	 */
	protected ClusterNode master = null;

	/**
	 * 节点上存储的数据块。
	 */
	private ConcurrentHashMap<String, Chunk> memoryChunks;

	/**
	 * 构造函数。
	 * 
	 * @param master 指定物理节点。
	 * @param hashCode 指定该节点散列码。
	 * @param address 指定访问地址。
	 */
	public ClusterVirtualNode(ClusterNode master, long hashCode, InetSocketAddress address) {
		super(hashCode, address, -1);
		this.master = master;
		this.memoryChunks = new ConcurrentHashMap<String, Chunk>();
	}

	/**
	 * 返回数据块数量。
	 * 
	 * @return 返回数据块数量。
	 */
	public int numOfChunk() {
		return this.memoryChunks.size();
	}

	/**
	 * 获得指定标签的数据块。
	 * 
	 * @param label 指定标签。
	 * @return 返回数据块。
	 */
	public Chunk getChunk(String label) {
		return this.memoryChunks.get(label);
	}

	/**
	 * 插入数据块到节点中。
	 * 
	 * @param chunk 指定插入的数据块。
	 */
	public void insertChunk(Chunk chunk) {
		this.memoryChunks.put(chunk.getLabel(), chunk);
	}

	/**
	 * 删除指定的数据块。
	 * 
	 * @param chunk 指定删除的数据块。
	 */
	public void deleteChunk(Chunk chunk) {
		this.memoryChunks.remove(chunk);
	}

	/**
	 * 更新指定的数据块。
	 * 
	 * @param chunk 指定待更新的数据块。
	 */
	public void updateChunk(Chunk chunk) {
		if (this.memoryChunks.containsKey(chunk.getLabel())) {
			this.memoryChunks.remove(chunk);
		}
		this.memoryChunks.put(chunk.getLabel(), chunk);
	}

}
