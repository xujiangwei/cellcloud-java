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
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Observable;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import net.cellcloud.cluster.protocol.ClusterDiscoveringProtocol;
import net.cellcloud.cluster.protocol.ClusterFailureProtocol;
import net.cellcloud.cluster.protocol.ClusterProtocol;
import net.cellcloud.cluster.protocol.ClusterProtocolFactory;
import net.cellcloud.cluster.protocol.ClusterPullProtocol;
import net.cellcloud.cluster.protocol.ClusterPushProtocol;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.MessageHandler;
import net.cellcloud.common.NonblockingConnector;
import net.cellcloud.common.Session;
import net.cellcloud.util.Utils;

/**
 * 集群连接器。
 * 
 * @author Ambrose Xu
 *
 */
public final class ClusterConnector extends Observable implements MessageHandler {

	/** 连接器对应的目标物理节点的 Hash 值。 */
	private Long remoteHashCode;
	/** 连接器工作的 Socket 地址。 */
	private InetSocketAddress address;

	/** 非阻塞连接器。 */
	private NonblockingConnector connector;
	/** 接收数据的临时缓存。 */
	private ByteBuffer buffer;
	/** 连接器 Buffer 区大小。 */
	private int bufferSize = 8192 * 8;
	/** 待处理协议队列。 */
	private Queue<ClusterProtocol> protocolQueue;
	/** 线程池执行器。 */
	private ExecutorService executor;
	/** 最大执行任务数。 */
	private final int maxTask = 16;
	/** 任务计数。 */
	private AtomicInteger numTask;

	/** 协议监听器。存储来自不同虚拟节点的协议请求，键为协议的 SN 值。 */
	private ConcurrentHashMap<String, ProtocolMonitor> monitors;

	/**
	 * 构造函数。
	 * 
	 * @param address 指定待连接地址。
	 * @param hashCode 指定目标节点的 Hash 值。
	 */
	public ClusterConnector(InetSocketAddress address, Long hashCode, ExecutorService executor) {
		this.address = address;
		this.remoteHashCode = hashCode;
		this.executor = executor;
		this.numTask = new AtomicInteger(0);
		this.connector = new NonblockingConnector();
		this.buffer = ByteBuffer.allocate(this.bufferSize);
		this.connector.setHandler(this);
		this.protocolQueue = new ConcurrentLinkedQueue<ClusterProtocol>();
		this.monitors = new ConcurrentHashMap<String, ProtocolMonitor>();
	}

	/**
	 * 获得连接器连接的 Socket 地址。
	 * 
	 * @return 返回连接器地址。
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/**
	 * 获得连接目标的 Hash 值。
	 * 
	 * @return 返回连接器 Hash 码。
	 */
	public Long getHashCode() {
		return this.remoteHashCode;
	}

	/**
	 * 关闭连接。
	 */
	public void close() {
		// 清空发送队列
		this.protocolQueue.clear();

		this.connector.disconnect();
	}

	/**
	 * 执行发现协议。
	 * 
	 * @param sourceIP 指定源IP。
	 * @param sourcePort 指定源端口。
	 * @param sourceNode 指定源节点。
	 * @return 当协议被正确执行时返回 <code>true</code>，否则返回 <code>false</code> 。
	 */
	public boolean doDiscover(String sourceIP, int sourcePort, ClusterNode sourceNode) {
		if (this.connector.isConnected()) {
			ClusterDiscoveringProtocol protocol = new ClusterDiscoveringProtocol(sourceIP, sourcePort, sourceNode);
			protocol.launch(this.connector.getSession());
			return true;
		}
		else {
			ClusterDiscoveringProtocol protocol = new ClusterDiscoveringProtocol(sourceIP, sourcePort, sourceNode);
			this.protocolQueue.offer(protocol);

			// 连接
			if (!this.connector.connect(this.address)) {
				// 请求连接失败
				this.protocolQueue.remove(protocol);
				return false;
			}
			else {
				// 请求连接成功
				return true;
			}
		}
	}

	/**
	 * 以阻塞方式执行数据推送。
	 * 
	 * @param chunk 指定推送的数据。
	 * @param timeout 指定阻塞的超时时间，单位：毫秒。
	 * @return 返回线程监视器。
	 */
	public ProtocolMonitor doBlockingPush(Chunk chunk, long timeout) {
		ClusterPushProtocol protocol = new ClusterPushProtocol(this.remoteHashCode.longValue(), chunk);
		this.protocolQueue.offer(protocol);

		if (!this.connector.isConnected()) {
			// 连接
			if (!this.connector.connect(this.address)) {
				// 请求连接失败
				this.protocolQueue.remove(protocol);
				return null;
			}
		}
		else {
			if (!this.protocolQueue.isEmpty() && this.numTask.get() < this.maxTask) {
				// 更新计数
				this.numTask.incrementAndGet();

				this.executor.execute(new QueueTask(this.connector.getSession()));
			}
		}

		// 获取对应的监视器
		ProtocolMonitor monitor = this.getOrCreateMonitor(protocol);
		monitor.chunk = chunk;
		synchronized (monitor) {
			try {
				monitor.wait(timeout);
			} catch (InterruptedException e) {
				Logger.log(ClusterConnector.class, e, LogLevel.ERROR);
				return null;
			} finally {
				// 删除监视器
				this.destroyMonitor(monitor);
			}
		}

		return monitor;
	}

	/**
	 * 以阻塞方式执行数据拉取。
	 * 
	 * @param chunkLabel 指定待来去的区块标签的数据。
	 * @param timeout 指定阻塞的超时时间，单位：毫秒。
	 * @return 返回线程监视器。
	 */
	public ProtocolMonitor doBlockingPull(String chunkLabel, long timeout) {
		ClusterPullProtocol protocol = new ClusterPullProtocol(this.remoteHashCode.longValue(), chunkLabel);
		this.protocolQueue.offer(protocol);

		if (!this.connector.isConnected()) {
			// 连接
			if (!this.connector.connect(this.address)) {
				// 请求连接失败
				this.protocolQueue.remove(protocol);
				return null;
			}
		}
		else {
			if (!this.protocolQueue.isEmpty() && this.numTask.get() < this.maxTask) {
				// 更新计数
				this.numTask.incrementAndGet();

				this.executor.execute(new QueueTask(this.connector.getSession()));
			}
		}

		// 获取对应的监视器
		ProtocolMonitor monitor = this.getOrCreateMonitor(protocol);
		synchronized (monitor) {
			try {
				monitor.wait(timeout);
			} catch (InterruptedException e) {
				Logger.log(ClusterConnector.class, e, LogLevel.ERROR);
				return null;
			} finally {
				// 删除监视器
				this.destroyMonitor(monitor);
			}
		}

		return monitor;
	}

	/**
	 * 通知阻塞线程结束等待。
	 * 
	 * @param chunk 指定被阻塞的数据块。
	 */
	protected void notifyBlocking(ClusterProtocol protocol) {
		ProtocolMonitor monitor = this.getMonitor(protocol.getSN());
		if (null != monitor) {

			if (protocol instanceof ClusterPullProtocol) {
				// 处理 Pull 协议的数据
				ClusterPullProtocol prtl = (ClusterPullProtocol) protocol;
				monitor.chunk = prtl.getChunk();
			}

			synchronized (monitor) {
				monitor.notifyAll();
			}

			// 删除监听器
			this.destroyMonitor(monitor);
		}
	}

	/**
	 * 以非阻塞方式执行数据推送。
	 * 
	 * @param targetHash 指定推送目标的 Hash 值。
	 * @param chunk 指定推送的数据。
	 * @return 返回推送协议实例。
	 */
	public ClusterPushProtocol doPush(long targetHash, Chunk chunk) {
		if (this.connector.isConnected()) {
			ClusterPushProtocol protocol = new ClusterPushProtocol(targetHash, chunk);
			protocol.launch(this.connector.getSession());
			return protocol;
		}
		else {
			ClusterPushProtocol protocol = new ClusterPushProtocol(targetHash, chunk);
			this.protocolQueue.offer(protocol);

			// 连接
			if (!this.connector.connect(this.address)) {
				// 请求连接失败
				this.protocolQueue.remove(protocol);
				return null;
			}

			if (!this.protocolQueue.isEmpty() && this.numTask.get() < this.maxTask) {
				// 更新计数
				this.numTask.incrementAndGet();

				this.executor.execute(new QueueTask(this.connector.getSession()));
			}

			return protocol;
		}
	}

	@Override
	public void sessionCreated(Session session) {
		// Nothing
	}

	@Override
	public void sessionDestroyed(Session session) {
		// Nothing
	}

	@Override
	public void sessionOpened(Session session) {
		if (!this.protocolQueue.isEmpty() && this.numTask.get() < this.maxTask) {
			// 更新计数
			this.numTask.incrementAndGet();

			this.executor.execute(new QueueTask(session));
		}
	}

	@Override
	public void sessionClosed(Session session) {
		// Nothing
	}

	@Override
	public void messageReceived(Session session, Message message) {
		ByteBuffer buf = this.parseMessage(message);
		if (null != buf) {
			this.process(buf);
			buf.clear();
		}
	}

	@Override
	public void messageSent(Session session, Message message) {
		// Nothing
	}

	@Override
	public void errorOccurred(int errorCode, Session session) {
		while (!this.protocolQueue.isEmpty()) {
			ClusterProtocol prtl = this.protocolQueue.poll();
			if (null == prtl) {
				break;
			}

			// 先解除阻塞
			this.notifyBlocking(prtl);

			// 再通知错误
			ClusterFailureProtocol failure = new ClusterFailureProtocol(ClusterFailure.DisappearingNode, prtl);
			this.distribute(failure);
		}
	}

	/**
	 * 解析原始消息数据格式。
	 * 
	 * @return 返回去除消息结束符的原始数据字节缓存。
	 */
	private ByteBuffer parseMessage(Message message) {
		byte[] data = message.get();
		// 判断数据是否结束
		int endIndex = -1;
		for (int i = 0, size = data.length; i < size; ++i) {
			byte b = data[i];
			if (b == '\r') {
				if (i + 3 < size
					&& data[i+1] == '\n'
					&& data[i+2] == '\r'
					&& data[i+3] == '\n') {
					endIndex = i - 1;
					break;
				}
			}
		}
		if (endIndex > 0) {
			// 数据结束
			this.buffer.put(data);
			this.buffer.flip();
			return this.buffer;
		}
		else {
			// 数据未结束
			this.buffer.put(data);
			return null;
		}
	}

	/**
	 * 处理解析后的消息。
	 * 
	 * @param buffer 指定原始字节数据。
	 */
	private void process(ByteBuffer buffer) {
		byte[] bytes = new byte[buffer.limit()];
		buffer.get(bytes);

		int total = bytes.length;
		for (int i = 0, len = bytes.length; i < len; ++i) {
			if (i + ClusterProtocol.SEPARATOR.length >= len) {
				break;
			}

			if (bytes[i] != ClusterProtocol.SEPARATOR[0]) {
				continue;
			}

			boolean match = true;
			for (int n = 0; n < ClusterProtocol.SEPARATOR.length; ++n) {
				byte b = bytes[i + n];
				if (b != ClusterProtocol.SEPARATOR[n]) {
					match = false;
					break;
				}
			}

			if (match) {
				total = i - 1;
				break;
			}
		}

		String str = null;
		byte[] payload = null;
		if (total != bytes.length) {
			byte[] buf = new byte[total];
			System.arraycopy(bytes, 0, buf, 0, total);
			str = Utils.bytes2String(buf);
			buf = null;

			payload = new byte[bytes.length - total - ClusterProtocol.SEPARATOR.length];
			System.arraycopy(bytes, total + ClusterProtocol.SEPARATOR.length,
					payload, 0, payload.length);
		}
		else {
			str = Utils.bytes2String(bytes);
		}
		bytes = null;

		String[] array = str.split("\\\n");
		HashMap<String, Object> prop = new HashMap<String, Object>();
		for (String line : array) {
			int index = line.indexOf(":");
			if (index > 0) {
				String key = line.substring(0, index).trim();
				String value = line.substring(index + 1, line.length()).trim();
				prop.put(key, value);
			}
		}
		if (null != payload) {
			prop.put(ClusterProtocol.KEY_PAYLOAD, payload);
		}

		ClusterProtocol protocol = ClusterProtocolFactory.create(prop);
		if (null != protocol) {
			// 处理协议
			this.distribute(protocol);
		}
		else {
			Logger.w(this.getClass(), new StringBuilder("Unknown protocol:\n").append(str).toString());
		}
	}

	/**
	 * 处理协议。将协议通知观察者。
	 * 
	 * @param protocol 指定需要处理的协议。
	 */
	private void distribute(ClusterProtocol protocol) {
		protocol.contextSession = this.connector.getSession();

		// 解除阻塞
		this.notifyBlocking(protocol);

		// 发送通知
		this.setChanged();
		this.notifyObservers(protocol);
		this.clearChanged();
	}

	/**
	 * 获得或者创建对应 SN 的协议监视器。
	 * 
	 * @param protocol 指定需要监视的协议。
	 * @return 返回协议监视器实例。
	 */
	private ProtocolMonitor getOrCreateMonitor(ClusterProtocol protocol) {
		String sn = protocol.getSN();
		if (this.monitors.containsKey(sn)) {
			return this.monitors.get(sn);
		}
		else {
			ProtocolMonitor m = new ProtocolMonitor(protocol);
			this.monitors.put(sn, m);
			return m;
		}
	}

	/**
	 * 获得对应 SN 的监视器。
	 * 
	 * @param sn 指定协议的 SN 值。
	 * @return 返回协议监视器实例。
	 */
	private ProtocolMonitor getMonitor(String sn) {
		return this.monitors.get(sn);
	}

	/**
	 * 销毁对应 SN 的监视器。
	 * 
	 * @param hash 指定协议的 SN 值。
	 */
	private void destroyMonitor(ProtocolMonitor monitor) {
		this.monitors.remove(monitor.getSN());
	}

	/**
	 * 协议队列任务。
	 */
	private class QueueTask implements Runnable {

		private Session session;

		private QueueTask(Session session) {
			this.session = session;
		}

		@Override
		public void run() {
			while (!protocolQueue.isEmpty()) {
				ClusterProtocol protocol = protocolQueue.poll();
				if (null == protocol) {
					break;
				}

				// 执行协议
				protocol.launch(this.session);
			}

			// 更新计数
			numTask.decrementAndGet();
		}
	}

}
