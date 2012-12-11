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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import net.cellcloud.common.Cryptology;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Service;

/** 集群控制器。
 * 
 * @author Jiangwei Xu
 */
public final class ClusterController implements Service, Observer {

	private Timer timer;
	private ClusterNetwork network;

	// 种子地址
	private ArrayList<InetAddress> seedAddressList;

	// Key: Socket address hash code
	private ConcurrentHashMap<Long, ClusterConnector> discoveringConnectors;

	private byte[] monitor = new byte[0];
	private TreeMap<Long, EndpointNode> endpoints;
	private EndpointNode root;

	public ClusterController() {
		this.network = new ClusterNetwork();
		this.network.addObserver(this);
		this.seedAddressList = new ArrayList<InetAddress>();
		this.discoveringConnectors = new ConcurrentHashMap<Long, ClusterConnector>();
		this.endpoints = new TreeMap<Long, EndpointNode>();
	}

	@Override
	public boolean startup() {
		if (!this.network.startup()) {
			Logger.e(this.getClass(), "Error in ClusterNetwork::startup()");
			return false;
		}

		InetSocketAddress local = new InetSocketAddress("0.0.0.0", this.network.getPort());
		this.root = new EndpointNode(this.hashSocketAddress(local), local);

		// 启动定时器，间隔 5 分钟
		if (null != this.timer) {
			this.timer.cancel();
		}
		this.timer = new Timer("ClusterControllerTimer");
		this.timer.schedule(new TimerTask() {
			@Override
			public void run() {
				//扫描网络
				network.scanNetwork();

				// 定时器任务
				timerHandle();
			}
		}, 10 * 1000, 5 * 60 * 1000);

		return true;
	}

	@Override
	public void shutdown() {
		if (null != this.timer) {
			this.timer.cancel();
			this.timer = null;
		}

		this.network.shutdown();

		// 清理连接器
		Iterator<ClusterConnector> iter = this.discoveringConnectors.values().iterator();
		while (iter.hasNext()) {
			ClusterConnector c = iter.next();
			c.closeConnector();
		}
		this.discoveringConnectors.clear();

		Iterator<EndpointNode> niter = this.endpoints.values().iterator();
		while (niter.hasNext()) {
			EndpointNode node = niter.next();
			node.closeAllConnectors();
		}
		this.endpoints.clear();
	}

	@Override
	public void update(Observable observable, Object arg) {
		if (observable instanceof ClusterConnector) {
			// 接收来自 ClusterConnector 的通知
			ClusterConnector connector = (ClusterConnector)observable;
			ClusterConnectorSubject subject = (ClusterConnectorSubject)arg;
			this.update(connector, subject);
		}
	}

	/** 添加集群地址列表。
	 */
	public void addClusterAddress(List<InetAddress> addressList) {
		for (InetAddress address : addressList) {
			byte[] bytes = address.getAddress();
			if (null == bytes) {
				continue;
			}

			synchronized (this.monitor) {
				boolean equal = false;
				for (InetAddress addr : this.seedAddressList) {
					equal = true;
					byte[] a = addr.getAddress();
					for (int i = 0; i < a.length; ++i) {
						if (a[i] != bytes[i]) {
							equal = false;
							break;
						}
					}
					if (equal) {
						break;
					}
				}

				if (!equal) {
					this.seedAddressList.add(address);
				}
			}
		}
	}

	/** 生成 Socket 地址 Hash 。
	 */
	public long hashSocketAddress(InetSocketAddress address) {
		String str = new StringBuilder().append(address.getAddress().getHostAddress())
				.append(":").append(address.getPort()).toString();
		byte[] md5 = Cryptology.getInstance().hashWithMD5(str.getBytes());
		long hash = Cryptology.getInstance().fastHash(md5);
		return hash;
	}

	/** 执行发现。
	 */
	private void doDiscover(List<InetSocketAddress> addressList) {
		for (InetSocketAddress address : addressList) {
			Long hash = this.hashSocketAddress(address);
			ClusterConnector connector = this.discoveringConnectors.get(hash);
			if (null == connector) {
				connector = new ClusterConnector(address, hash.longValue());
				connector.addObserver(this);
				this.discoveringConnectors.put(hash, connector);
			}
			// 连接器执行发现协议
			if (!connector.discover(this.network.getPort())) {
				this.discoveringConnectors.remove(hash);
				connector.deleteObserver(this);
			}
		} // #for
	}

	/** 对指定地址进行端口猜测并进行发现。
	 */
	private boolean guessDiscover(InetSocketAddress oldAddress) {
		if (oldAddress.getPort() == ClusterNetwork.PREFERRED_PORT) {
			// 首选端口的下一个端口号
			InetSocketAddress newAddress = new InetSocketAddress(oldAddress.getAddress().getHostAddress(), ClusterNetwork.PREFERRED_PORT + 1);
			Logger.i(this.getClass(), "Guess address " + newAddress.getAddress().getHostAddress() + ":" + newAddress.getPort()
					+ " to discover.");
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
		// 检查种子地址
		synchronized (this.monitor) {
			if (!this.seedAddressList.isEmpty()) {
				ArrayList<InetSocketAddress> list = new ArrayList<InetSocketAddress>();
				for (InetAddress address : this.seedAddressList) {
					// 通过地址的散列码判断是否已经加入节点
					InetSocketAddress sa = new InetSocketAddress(address, ClusterNetwork.PREFERRED_PORT);
					Long hash = this.hashSocketAddress(sa);
					if (!this.endpoints.containsKey(hash)) {
						// 未加入集群的地址
						list.add(sa);
					}
				}
				if (!list.isEmpty()) {
					doDiscover(list);
				}
			}
		}
	}

	private void update(ClusterConnector connector, ClusterConnectorSubject subject) {
		if (subject.subject.equals(ClusterConnector.SUBJECT_DISCOVERING)) {
			// 主题：发现
			synchronized (this.monitor) {
				if (subject.completed) {
					// 发现操作结束，成功发现
					this.discoveringConnectors.remove(connector.getHashCode());
					connector.deleteObserver(this);

					long hash = connector.getHashCode();
					// 挂接节点到根
					EndpointNode node = new EndpointNode(hash, connector.getAddress());
					this.root.addChild(node, connector);
					this.endpoints.put(hash, node);

					if (Logger.isDebugLevel()) {
						Logger.d(this.getClass(), "Add cluster physical node: "
								+ connector.getAddress().getAddress().getHostAddress() + ":"
								+ connector.getAddress().getPort());
					}
				}
				else {
					// 发现操作终止，没有发现
					this.discoveringConnectors.remove(connector.getHashCode());
					connector.deleteObserver(this);

					if (Logger.isDebugLevel()) {
						Logger.d(this.getClass(), "No cluster Physical node: "
								+ connector.getAddress().getAddress().getHostAddress() + ":"
								+ connector.getAddress().getPort());
					}
				}
			}
		}
		else if (subject.subject.equals(ClusterConnector.SUBJECT_FAILURE)) {
			// 检查是否是正在执行发现协议
			if (this.discoveringConnectors.containsKey(connector.getHashCode())) {
				this.discoveringConnectors.remove(connector.getHashCode());
				connector.deleteObserver(this);

				// 尝试进行猜测发现
				this.guessDiscover(connector.getAddress());
			}
		}
	}
}
