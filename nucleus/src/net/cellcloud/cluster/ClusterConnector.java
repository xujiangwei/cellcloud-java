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
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import net.cellcloud.common.Cryptology;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.MessageErrorCode;
import net.cellcloud.common.MessageHandler;
import net.cellcloud.common.NonblockingConnector;
import net.cellcloud.common.Session;
import net.cellcloud.util.Utils;

/** 集群连接器。
 * 
 * @author Jiangwei Xu
 */
public final class ClusterConnector extends Observable implements MessageHandler {

	protected final static String SUBJECT_FAILURE = "failure";
	protected final static String SUBJECT_DISCOVERING = "discovering";

	private final int bufferSize = 8192;

	private Long hashCode;
	private InetSocketAddress address;

	private NonblockingConnector connector;
	private ByteBuffer buffer;
	private Queue<ClusterProtocol> protocolQueue;

	private ConcurrentHashMap<Long, ProtocolMonitor> monitors;

	public ClusterConnector(InetSocketAddress address, Long hashCode) {
		this.address = address;
		this.hashCode = hashCode;
		this.connector = new NonblockingConnector();
		this.buffer = ByteBuffer.allocate(this.bufferSize);
		this.connector.setHandler(this);
		this.protocolQueue = new LinkedList<ClusterProtocol>();
		this.monitors = new ConcurrentHashMap<Long, ProtocolMonitor>();
	}

	/** 返回连接器地址。
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/** 返回连接器 Hash 码。
	 */
	public Long getHashCode() {
		return this.hashCode;
	}

	/** 关闭连接。
	 */
	public void close() {
		this.connector.disconnect();
	}

	/** 执行发现。
	 */
	public boolean doDiscover(String sourceIP, int sourcePort, ClusterNode node) {
		if (this.connector.isConnected()) {
			ClusterDiscoveringProtocol protocol = new ClusterDiscoveringProtocol(sourceIP, sourcePort, node);
			protocol.launch(this.connector.getSession());
			return true;
		}
		else {
			ClusterDiscoveringProtocol protocol = new ClusterDiscoveringProtocol(sourceIP, sourcePort, node);
			synchronized (this.protocolQueue) {
				this.protocolQueue.offer(protocol);
			}

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

	/** 以同步方式执行数据推送。
	 */
	public ProtocolMonitor doBlockingPush(long targetHash, Chunk chunk, long timeout) {
		ClusterPushProtocol protocol = new ClusterPushProtocol(targetHash, chunk);

		if (this.connector.isConnected()) {
			protocol.launch(this.connector.getSession());
			ProtocolMonitor monitor = new ProtocolMonitor(protocol);
			monitor.blocking = true;
			monitor.chunk = chunk;
			return monitor;
		}
		else {
			synchronized (this.protocolQueue) {
				this.protocolQueue.offer(protocol);
			}

			// 连接
			if (!this.connector.connect(this.address)) {
				// 请求连接失败
				this.protocolQueue.remove(protocol);
				return null;
			}
			else {
				Long lh = Cryptology.getInstance().fastHash(chunk.getLabel());
				ProtocolMonitor monitor = this.getOrCreateMonitor(lh, protocol);
				monitor.blocking = true;
				synchronized (monitor) {
					try {
						monitor.wait(timeout);
					} catch (InterruptedException e) {
						Logger.log(ClusterConnector.class, e, LogLevel.ERROR);
						this.destroyMonitor(lh);
						return null;
					}
				}

				// 删除监听器
				this.destroyMonitor(lh);

				monitor.chunk = chunk;
				return monitor;
			}
		}
	}

	/** 以同步方式执行数据拉取。
	 */
	public ProtocolMonitor doBlockingPull(long targetHash, String chunkLabel, long timeout) {
		ClusterPullProtocol protocol = new ClusterPullProtocol(targetHash, chunkLabel);

		if (this.connector.isConnected()) {
			protocol.launch(this.connector.getSession());
		}
		else {
			synchronized (this.protocolQueue) {
				this.protocolQueue.offer(protocol);
			}

			// 连接
			if (!this.connector.connect(this.address)) {
				// 请求连接失败
				this.protocolQueue.remove(protocol);
				return null;
			}
		}

		Long lh = Cryptology.getInstance().fastHash(chunkLabel);
		ProtocolMonitor monitor = this.getOrCreateMonitor(lh, protocol);
		monitor.blocking = true;
		synchronized (monitor) {
			try {
				monitor.wait(timeout);
			} catch (InterruptedException e) {
				Logger.log(ClusterConnector.class, e, LogLevel.ERROR);
				return null;
			} finally {
				// 删除监听器
				this.destroyMonitor(lh);
			}
		}

		return monitor;
	}

	// 通知阻塞线程
	protected void notifyBlockingPull(Chunk chunk) {
		Long lh = Cryptology.getInstance().fastHash(chunk.getLabel());
		ProtocolMonitor monitor = this.getMonitor(lh);
		if (null != monitor) {
			synchronized (monitor) {
				monitor.chunk = chunk;
				monitor.notifyAll();
			}

			// 删除监听器
			this.destroyMonitor(lh);
		}
	}
	// 通知阻塞线程
	protected void notifyBlockingPull(String chunkLabel) {
		Long lh = Cryptology.getInstance().fastHash(chunkLabel);
		ProtocolMonitor monitor = this.getMonitor(lh);
		if (null != monitor) {
			synchronized (monitor) {
				monitor.notifyAll();
			}

			// 删除监听器
			this.destroyMonitor(lh);
		}
	}

	/** 以异步方式执行数据推送。
	 */
	public ProtocolMonitor doPush(long targetHash, Chunk chunk) {
		if (this.connector.isConnected()) {
			ClusterPushProtocol protocol = new ClusterPushProtocol(targetHash, chunk);
			protocol.launch(this.connector.getSession());
			return new ProtocolMonitor(protocol);
		}
		else {
			ClusterPushProtocol protocol = new ClusterPushProtocol(targetHash, chunk);
			synchronized (this.protocolQueue) {
				this.protocolQueue.offer(protocol);
			}

			// 连接
			if (!this.connector.connect(this.address)) {
				// 请求连接失败
				this.protocolQueue.remove(protocol);
				return null;
			}
			else {
				Long lh = Cryptology.getInstance().fastHash(chunk.getLabel());
				ProtocolMonitor monitor = this.getOrCreateMonitor(lh, protocol);
				return monitor;
			}
		}
	}

	@Override
	public void sessionCreated(Session session) {
	}

	@Override
	public void sessionDestroyed(Session session) {
	}

	@Override
	public void sessionOpened(Session session) {
		while (!this.protocolQueue.isEmpty()) {
			ClusterProtocol protocol = this.protocolQueue.poll();
			protocol.launch(session);

			Chunk chunk = null;
			if (protocol instanceof ClusterPushProtocol) {
				ClusterPushProtocol pushPrtl = (ClusterPushProtocol) protocol;
				chunk = pushPrtl.getChunk();
			}

			if (null != chunk) {
				Long lh = Cryptology.getInstance().fastHash(chunk.getLabel());
				ProtocolMonitor monitor = this.getMonitor(lh);
				if (null != monitor) {
					if (monitor.blocking) {
						synchronized (monitor) {
							monitor.notifyAll();
						}
					}

					this.destroyMonitor(lh);
				}
			}
		}
	}

	@Override
	public void sessionClosed(Session session) {
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
		if (errorCode == MessageErrorCode.CONNECT_TIMEOUT
			|| errorCode == MessageErrorCode.CONNECT_FAILED) {
			while (!this.protocolQueue.isEmpty()) {
				ClusterProtocol prtl = this.protocolQueue.poll();
				ClusterFailureProtocol failure = new ClusterFailureProtocol(ClusterFailure.DisappearingNode, prtl);
				this.distribute(failure);

				Chunk chunk = null;
				if (prtl instanceof ClusterPushProtocol) {
					ClusterPushProtocol pushPrtl = (ClusterPushProtocol) prtl;
					chunk = pushPrtl.getChunk();
				}

				if (null != chunk) {
					Long lh = Cryptology.getInstance().fastHash(chunk.getLabel());
					ProtocolMonitor monitor = this.getMonitor(lh);
					if (null != monitor) {
						if (monitor.blocking) {
							synchronized (monitor) {
								monitor.notifyAll();
							}
						}

						this.destroyMonitor(lh);
					}
				}
			}
		}
	}

	/** 解析消息。
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

	/** 处理解析后的消息。
	 */
	private void process(ByteBuffer buffer) {
		byte[] bytes = new byte[buffer.limit()];
		buffer.get(bytes);
		String str = Utils.bytes2String(bytes);
		String[] array = str.split("\\\n");
		HashMap<String, String> prop = new HashMap<String, String>();
		for (String line : array) {
			int index = line.indexOf(":");
			if (index > 0) {
				String key = line.substring(0, index).trim();
				String value = line.substring(index + 1, line.length()).trim();
				prop.put(key, value);
			}
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

	/** 处理具体协议。
	 */
	private void distribute(ClusterProtocol protocol) {
		protocol.contextSession = this.connector.getSession();

		this.setChanged();
		this.notifyObservers(protocol);
		this.clearChanged();
	}

	private ProtocolMonitor getOrCreateMonitor(Long hash, ClusterProtocol protocol) {
		if (this.monitors.containsKey(hash)) {
			return this.monitors.get(hash);
		}
		else {
			ProtocolMonitor m = new ProtocolMonitor(protocol);
			this.monitors.put(hash, m);
			return m;
		}
	}

	private ProtocolMonitor getMonitor(Long hash) {
		return this.monitors.get(hash);
	}

	private void destroyMonitor(Long hash) {
		this.monitors.remove(hash);
	}
}
