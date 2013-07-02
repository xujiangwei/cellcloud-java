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

package net.cellcloud.cluster;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import net.cellcloud.common.Cryptology;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Service;
import net.cellcloud.core.Nucleus;
import net.cellcloud.util.SlidingWindowExecutor;

/** 集群控制器。
 * 
 * @author Jiangwei Xu
 */
public final class ClusterController implements Service, Observer {

	private ClusterNetwork network;
	private Timer timer;
	protected SlidingWindowExecutor executor;

	// 集群地址列表
	private ArrayList<InetSocketAddress> addressList;

	// Key: Socket address hash code
	private ConcurrentHashMap<Long, ClusterConnector> connectors;

	private int numVNode;

	private byte[] monitor = new byte[0];

	private ClusterNode root;

	// 是否自动扫描网络
	public boolean autoScanNetwork = false;

	public ClusterController(String hostname, int preferredPort, int numVNode) {
		// 创建执行器
		this.executor = SlidingWindowExecutor.newSlidingWindowThreadPool(8);

		this.network = new ClusterNetwork(hostname, preferredPort, this.executor);
		this.numVNode = numVNode;
		this.network.addObserver(this);
		this.addressList = new ArrayList<InetSocketAddress>();
		this.connectors = new ConcurrentHashMap<Long, ClusterConnector>();
	}

	@Override
	public boolean startup() {
		if (!this.network.startup()) {
			Logger.e(this.getClass(), "Error in ClusterNetwork::startup()");
			return false;
		}

		// 创建根节点
		this.root = new ClusterNode(ClusterController.hashAddress(this.network.getBindAddress())
				, this.network.getBindAddress(), this.numVNode);

		// 执行守护定时任务，间隔 5 分钟
		this.timer = new Timer();
		this.timer.scheduleAtFixedRate(new ControllerTimerTask(), 10 * 1000, 5 * 60 * 1000);

		return true;
	}

	@Override
	public void shutdown() {
		if (null != this.timer) {
			this.timer.cancel();
			this.timer = null;
		}

		if (null != this.executor) {
			this.executor.shutdown();
			this.executor = null;
		}

		this.network.shutdown();

		// 清理连接器
		Iterator<ClusterConnector> iter = this.connectors.values().iterator();
		while (iter.hasNext()) {
			ClusterConnector connector = iter.next();
			connector.deleteObserver(this);
			connector.close();
		}
		this.connectors.clear();

		if (null != this.root) {
			// 清空所有虚拟节点
			this.root.clearup();
			this.root = null;
		}
	}

	@Override
	public void update(Observable observable, Object arg) {
		if (observable instanceof ClusterConnector) {
			// 接收来自 ClusterConnector 的通知
			ClusterConnector connector = (ClusterConnector)observable;
			ClusterProtocol protocol = (ClusterProtocol)arg;
			this.update(connector, protocol);
		}
		else if (observable instanceof ClusterNetwork) {
			// 接收来自 ClusterNetwork 的通知
			ClusterNetwork network = (ClusterNetwork)observable;
			ClusterProtocol protocol = (ClusterProtocol)arg;
			this.update(network, protocol);
		}
	}

	/** 返回根节点。
	 */
	public ClusterNode getNode() {
		return this.root;
	}

	/** 添加集群地址列表。
	 */
	public void addClusterAddress(List<InetSocketAddress> addressList) {
		for (InetSocketAddress address : addressList) {
			byte[] bytes = address.getAddress().getAddress();
			if (null == bytes) {
				continue;
			}

			long addrHash = ClusterController.hashAddress(address);

			synchronized (this.monitor) {
				boolean equals = false;

				// 判断是否有重复地址
				for (InetSocketAddress addr : this.addressList) {
					long hashCode = ClusterController.hashAddress(addr);
					if (hashCode == addrHash) {
						equals = true;
						break;
					}
				}

				if (!equals) {
					this.addressList.add(address);
				}
			}
		}
	}

	/** 以阻塞方式向集群内写入数据块。
	 */
	public boolean writeChunk(Chunk chunk, long timeout) {
		// 获得目标 Hash
		long hash = ClusterController.hashChunk(chunk);
		Long targetHash = this.root.findVNodeHash(hash);
		if (null == targetHash) {
			return false;
		}

		// 判断是否是本地节点
		if (this.root.containsOwnVirtualNode(targetHash)) {
			// 是本地节点
			if (Logger.isDebugLevel()) {
				Logger.d(this.getClass(), new StringBuilder("Hit local target hash: ").append(targetHash).toString());
			}

			// 写入本地节点
			ClusterVirtualNode vnode = this.root.getOwnVirtualNode(targetHash);
			vnode.insertChunk(chunk);

			return true;
		}
		else {
			// 不是本地节点
			// 选择可用节点
			ClusterVirtualNode vnode = this.root.selectVNode(hash);
			if (null != vnode) {
				ClusterConnector connector = this.getOrCreateConnector(vnode.getCoordinate().getAddress(), hash);
				// 阻塞方式执行 Push
				ProtocolMonitor monitor = connector.doBlockingPush(targetHash, chunk, timeout);
				return (null != monitor);
			}
			else {
				Logger.e(this.getClass(), new StringBuilder("Virtual node hash code error: ").append(targetHash).toString());
				return false;
			}
		}
	}

	/** 以阻塞方式从集群内读取数据块。
	 */
	public Chunk readChunk(String label, long timeout) {
		// 获得目标 Hash
		long hash = ClusterController.hashChunk(label);
		Long targetHash = this.root.findVNodeHash(hash);
		if (null == targetHash) {
			return null;
		}

		// 判断是否是本地节点
		if (this.root.containsOwnVirtualNode(targetHash)) {
			// 是本地节点
			if (Logger.isDebugLevel()) {
				Logger.d(this.getClass(), new StringBuilder("Hit local target hash: ").append(targetHash).toString());
			}

			// 读取 Chunk
			ClusterVirtualNode vnode = this.root.getOwnVirtualNode(targetHash);
			return vnode.getChunk(label);
		}
		else {
			// 不是本地节点
			// 选择可用节点
			ClusterVirtualNode vnode = this.root.selectVNode(hash);
			if (null != vnode) {
				ClusterConnector connector = this.getOrCreateConnector(vnode.master.getCoordinate().getAddress(), hash);
				// 阻塞方式执行 Pull
				ProtocolMonitor monitor = connector.doBlockingPull(targetHash, label, timeout);
				return (null != monitor) ? monitor.chunk : null;
			}
			else {
				Logger.e(this.getClass(), new StringBuilder("Virtual node hash code error: ").append(targetHash).toString());
				return null;
			}
		}
	}

	/** 执行发现。
	 */
	private void doDiscover(List<InetSocketAddress> addressList) {
		for (InetSocketAddress address : addressList) {
			Long hash = ClusterController.hashAddress(address);
			// 获取连接器
			ClusterConnector connector = this.getOrCreateConnector(address, hash);

			// 连接器执行发现协议
			if (!connector.doDiscover(this.network.getBindAddress().getHostName(), this.network.getPort(), this.root)) {
				Logger.i(this.getClass(), new StringBuilder("Discovering error: ")
					.append(address.getAddress().getHostAddress()).append(":").append(address.getPort()).toString());

				// 执行失败，删除连接器
				this.closeAndDestroyConnector(connector);
			}
			else {
				Logger.i(this.getClass(), new StringBuilder("Discovering: ")
					.append(address.getAddress().getHostAddress()).append(":").append(address.getPort()).toString());
			}
		} // #for
	}

	/** 对指定地址进行端口猜测并进行发现。
	 */
	private boolean guessDiscover(InetSocketAddress oldAddress) {
		if (oldAddress.getPort() == this.network.getPort()) {
			// 首选端口的下一个端口号
			InetSocketAddress newAddress = new InetSocketAddress(oldAddress.getAddress().getHostAddress(), this.network.getPort() + 1);
			Logger.i(this.getClass(), "Guess discovering address: " + newAddress.getAddress().getHostAddress() + ":" + newAddress.getPort());
			ArrayList<InetSocketAddress> list = new ArrayList<InetSocketAddress>();
			list.add(newAddress);
			// 执行发现
			doDiscover(list);
			return true;
		}

		return false;
	}

	/** 定时器操作。
	 */
	private void timerHandle() {
		ArrayList<InetSocketAddress> list = null;

		synchronized (this.monitor) {
			// 根据地址列表建立集群网络
			if (!this.addressList.isEmpty()) {
				list = new ArrayList<InetSocketAddress>();

				for (InetSocketAddress address : this.addressList) {
					// 通过地址的散列码判断是否已经加入节点
					long hash = ClusterController.hashAddress(address);

					// 判断地址是否与根地址相同
					if (this.root.getHashCode() != hash) {
						// 判断是否已经是兄弟节点
						if (!this.root.isBrotherNode(hash)) {
							// 未加入集群的地址，进行发现
							list.add(address);
						}
					}
				}
			}
		}

		if (null != list && !list.isEmpty()) {
			// 尝试进行发现
			doDiscover(list);
		}
	}

	/** 处理适配器网络句柄函数 */
	private void update(ClusterNetwork network, ClusterProtocol protocol) {
		if (protocol instanceof ClusterPullProtocol) {
			ClusterPullProtocol prtl = (ClusterPullProtocol)protocol;
			// 获取目标 Hash
			long hash = prtl.getTargetHash();
			if (this.root.containsOwnVirtualNode(hash)) {
				// 目标是自己的虚节点
				ClusterVirtualNode vnode = this.root.getOwnVirtualNode(hash);
				Chunk chunk = vnode.getChunk(prtl.getChunkLabel());
				if (null != chunk) {
					// 获取到 Chunk
					prtl.setChunk(chunk);
					prtl.respond(this.root, ClusterProtocol.StateCode.SUCCESS);
				}
				else {
					// 没有获取到 Chunk
					prtl.respond(this.root, ClusterProtocol.StateCode.FAILURE);
				}
			}
			else {
				// 目标不是自己的虚节点
				prtl.respond(this.root, ClusterProtocol.StateCode.REJECT);
			}
		}
		else if (protocol instanceof ClusterPushProtocol) {
			ClusterPushProtocol prtl = (ClusterPushProtocol)protocol;
			// 获取目标 Hash
			long hash = prtl.getTargetHash();
			if (this.root.containsOwnVirtualNode(hash)) {
				if (Logger.isDebugLevel()) {
					Logger.d(this.getClass(), new StringBuilder("Hit target hash: ").append(hash).append(" at ")
							.append(this.root.getCoordinate().getAddress().getHostName())
							.append(this.root.getCoordinate().getAddress().getPort()).toString());
				}

				// 插入数据块
				Chunk chunk = prtl.getChunk();
				ClusterVirtualNode vnode = this.root.getOwnVirtualNode(hash);
				vnode.insertChunk(chunk);

				// 响应
				prtl.respond(this.root, ClusterProtocol.StateCode.SUCCESS);
			}
			else {
				if (Logger.isDebugLevel()) {
					Logger.d(this.getClass(), new StringBuilder("Don't hit target hash: ").append(hash).append(" at ")
							.append(this.root.getCoordinate().getAddress().getHostName())
							.append(this.root.getCoordinate().getAddress().getPort()).toString());
				}

				// 响应
				prtl.respond(this.root, ClusterProtocol.StateCode.REJECT);
			}
		}
		else if (protocol instanceof ClusterDiscoveringProtocol) {
			ClusterDiscoveringProtocol discovering = (ClusterDiscoveringProtocol)protocol;
			String tag = discovering.getTag();
			if (tag.equals(Nucleus.getInstance().getTagAsString())) {
				// 标签相同是同一内核，不能与自己集群
				discovering.reject();
			}
			else {
				// 获取节点信息
				List<Long> vnodes = discovering.getVNodeHash();
				if (null != vnodes) {
					// 有虚拟节点信息

					String sourceIP = discovering.getSourceIP();
					int sourcePort = discovering.getSourcePort();
					long hash = discovering.getHash();

					// 创建并添加兄弟节点
					ClusterNode brother = new ClusterNode(hash, new InetSocketAddress(sourceIP, sourcePort), vnodes);
					this.root.addBrother(brother);

					if (Logger.isDebugLevel()) {
						Logger.d(this.getClass(), new StringBuilder("Add cluster node: ")
								.append(sourceIP).append(":").append(sourcePort).toString());
					}

					// 回应对端
					discovering.respond(this.root, ClusterProtocol.StateCode.SUCCESS);
				}
			}
		}
	}

	/** 处理连接器句柄 */
	private void update(ClusterConnector connector, ClusterProtocol protocol) {
		if (protocol instanceof ClusterPullProtocol) {
			ClusterPullProtocol prtl = (ClusterPullProtocol) protocol;
			if (ClusterProtocol.StateCode.SUCCESS.getCode() == prtl.getStateCode()) {
				// 获取 Chunk 数据，并尝试通知阻塞线程
				Chunk chunk = prtl.getChunk();
				connector.notifyBlockingPull(chunk);
			}
			else {
				// 通知阻塞线程
				connector.notifyBlockingPull(prtl.getChunkLabel());
			}
		}
		else if (protocol instanceof ClusterDiscoveringProtocol) {
			ClusterDiscoveringProtocol discovering = (ClusterDiscoveringProtocol)protocol;
			if (ClusterProtocol.StateCode.REJECT.getCode() == discovering.getStateCode()) {
				if (Logger.isDebugLevel()) {
					Logger.d(this.getClass(), new StringBuilder("No cluster node: ")
							.append(connector.getAddress().getAddress().getHostAddress()).append(":")
							.append(connector.getAddress().getPort()).toString());
				}

				// 尝试进行猜测
				this.guessDiscover(connector.getAddress());
			}
			else if (ClusterProtocol.StateCode.SUCCESS.getCode() == discovering.getStateCode()) {
				// 发现操作结束，成功发现
				long hash = discovering.getHash();

				if (!this.root.isBrotherNode(hash)) {
					List<Long> vnodes = discovering.getVNodeHash();
					if (null != vnodes) {
						// 有虚拟节点信息

						// 创建并添加兄弟节点
						ClusterNode brother = new ClusterNode(hash, connector.getAddress(), vnodes);
						this.root.addBrother(brother);

						if (Logger.isDebugLevel()) {
							Logger.d(this.getClass(), new StringBuilder("Add cluster node: ")
									.append(connector.getAddress().getAddress().getHostAddress()).append(":")
									.append(connector.getAddress().getPort()).toString());
						}
					}
				}
			}
		}
		else if (protocol instanceof ClusterFailureProtocol) {
			// 故障处理
			this.closeAndDestroyConnector(connector);
			// TODO
		}
	}

	/** 返回或创建连接器。 */
	private ClusterConnector getOrCreateConnector(InetSocketAddress address, Long hash) {
		ClusterConnector connector = this.connectors.get(hash);
		if (null == connector) {
			connector = new ClusterConnector(address, hash);
			connector.addObserver(this);
			this.connectors.put(hash, connector);
		}

		return connector;
	}

	/** 关闭并销毁连接器。 */
	private void closeAndDestroyConnector(ClusterConnector connector) {
		// 关闭连接
		connector.close();

		// 删除物理连接
		this.connectors.remove(connector.getHashCode());
		connector.deleteObserver(this);
	}

	/** 计算地址 Hash 。
	 */
	public static long hashAddress(InetSocketAddress address) {
		String str = new StringBuilder().append(address.getAddress().getHostAddress())
				.append(":").append(address.getPort()).toString();
		byte[] md5 = Cryptology.getInstance().hashWithMD5(str.getBytes());
		long hash = ((long)(md5[3]&0xFF) << 24) | ((long)(md5[2]&0xFF) << 16) | ((long)(md5[1]&0xFF) << 8) | (long)(md5[0]&0xFF);
		return hash;
	}

	/** 计算节点 Hash 。
	 */
	public static long hashVNode(InetSocketAddress address, int sequence) {
		String str = new StringBuilder().append(address.getAddress().getHostAddress())
				.append(":").append(address.getPort()).append("#").append(sequence).toString();
		byte[] md5 = Cryptology.getInstance().hashWithMD5(str.getBytes());
		long hash = ((long)(md5[3]&0xFF) << 24) | ((long)(md5[2]&0xFF) << 16) | ((long)(md5[1]&0xFF) << 8) | (long)(md5[0]&0xFF);
		return hash;
	}

	/** 计算数据块 Hash 。
	 */
	public static long hashChunk(Chunk chunk) {
		String str = chunk.getLabel();
		byte[] md5 = Cryptology.getInstance().hashWithMD5(str.getBytes());
		long hash = ((long)(md5[3]&0xFF) << 24) | ((long)(md5[2]&0xFF) << 16) | ((long)(md5[1]&0xFF) << 8) | (long)(md5[0]&0xFF);
		return hash;
	}
	/** 计算数据块 Hash 。
	 */
	public static long hashChunk(String chunkLabel) {
		String str = chunkLabel;
		byte[] md5 = Cryptology.getInstance().hashWithMD5(str.getBytes());
		long hash = ((long)(md5[3]&0xFF) << 24) | ((long)(md5[2]&0xFF) << 16) | ((long)(md5[1]&0xFF) << 8) | (long)(md5[0]&0xFF);
		return hash;
	}

	/** 控制器定时任务。
	 */
	protected class ControllerTimerTask extends TimerTask {
		protected ControllerTimerTask() {
			super();
		}

		@Override
		public void run() {
			if (autoScanNetwork) {
				// 扫描网络
				network.scanNetwork();
			}

			// 定时器任务
			timerHandle();
		}
	}
}
