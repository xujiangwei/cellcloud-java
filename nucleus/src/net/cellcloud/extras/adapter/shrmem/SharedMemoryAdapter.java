/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2013 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.extras.adapter.shrmem;

import java.util.LinkedList;

import net.cellcloud.adapter.RelationNucleusAdapter;
import net.cellcloud.util.BTree;

/** 共享内存适配器。
 * 
 * @author Jiangwei Xu
 */
public final class SharedMemoryAdapter extends RelationNucleusAdapter {

	private static final SharedMemoryAdapter instance = new SharedMemoryAdapter();

	private BTree<Long, MemoryVirtualNode> nodes;
	private LinkedList<MemoryVirtualNode> nodeRing;

	private SharedMemoryAdapter() {
		super("SharedMemoryAdapter");
		this.nodes = new BTree<Long, MemoryVirtualNode>();
		this.nodeRing = new LinkedList<MemoryVirtualNode>();
	}

	public synchronized static SharedMemoryAdapter getInstance() {
		return SharedMemoryAdapter.instance;
	}

	@Override
	public void setup() {
	}

	@Override
	public void teardown() {
	}

	/** TODO
	 */
	public void addNode(MemoryVirtualNode node) {
		this.nodes.put(node.hashCode, node);
		this.nodeRing.add(node);
	}
}
