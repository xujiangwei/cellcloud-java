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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import net.cellcloud.cluster.protocol.ClusterProtocol;
import net.cellcloud.cluster.protocol.ClusterProtocolFactory;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.MessageHandler;
import net.cellcloud.common.NonblockingAcceptor;
import net.cellcloud.common.Service;
import net.cellcloud.common.Session;
import net.cellcloud.util.Utils;

/**
 * 集群网络。
 * 
 * @author Ambrose Xu
 * 
 */
public final class ClusterNetwork extends Observable implements Service, MessageHandler {

	/** 物理节点网络访问主机名或地址。 */
	private String hostname = "127.0.0.1";
	/** 物理节点网络访问端口。 */
	private int port = 11099;
	/** 线程池。 */
	private ExecutorService executor;

	/** 非阻塞接收器。 */
	private NonblockingAcceptor acceptor;
	/** 数据缓冲区大小。 */
	private final int bufferSize = 8192;

	/** 是否中断本地网络。 */
	private boolean interrupted = false;
	/** 是否正在扫描可用地址。 */
	private boolean scanReachable = false;

	private ConcurrentHashMap<Long, Queue<byte[]>> sessionMessageCache;

	/**
	 * 构造函数。
	 * 
	 * @param hostname 指定本机访问名或地址。
	 * @param preferredPort 指定本地优先监听端口。
	 * @param executor 指定线程池执行器。
	 */
	public ClusterNetwork(String hostname, int preferredPort, ExecutorService executor) {
		this.hostname = hostname;
		this.port = preferredPort;
		this.executor = executor;
		this.sessionMessageCache = new ConcurrentHashMap<Long, Queue<byte[]>>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean startup() {
		if (null != this.acceptor) {
			return true;
		}

		this.interrupted = false;

		// 检测可用的端口号
		this.port = this.detectUsablePort(this.port);
 
		// 启动接收器
		this.acceptor = new NonblockingAcceptor();
		this.acceptor.setHandler(this);
		this.acceptor.setMaxConnectNum(1000);
		this.acceptor.setWorkerNum(4);
		if (!this.acceptor.bind(new InetSocketAddress(this.hostname, this.port))) {
			Logger.e(this.getClass(), new StringBuilder("Cluster network can not bind socket on ")
					.append(this.hostname).append(":").append(this.port).toString());

			this.acceptor.setHandler(null);
			this.acceptor = null;

			return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdown() {
		this.interrupted = true;

		if (null != this.acceptor) {
			this.acceptor.unbind();
			this.acceptor = null;
		}

		this.port = -1;
	}

	/**
	 * 获得绑定地址。
	 * 
	 * @return 返回绑定地址。
	 */
	public InetSocketAddress getBindAddress() {
		return this.acceptor.getBindAddress();
	}

	/**
	 * 获得监听端口。
	 * 
	 * @return 返回监听端口。
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * 阻塞模式检测可用的端口号。
	 * 
	 * @param port 指定起始检测端口。
	 * @return 返回本机可用的端口号。
	 */
	private int detectUsablePort(int port) {
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(port);
			return port;
		} catch (IOException e) {
			return detectUsablePort(port + 1);
		} finally {
			try {
				socket.close();
			} catch (Exception e) {
				// Nothing
			}
		}
	}

	/**
	 * 扫描网络。
	 */
	protected void scanNetwork() {
		// 在子线程中进行地址扫描
		if (!this.scanReachable) {
			this.scanReachable = true;

			this.executor.execute(new Runnable() {
				@Override
				public void run() {
					long start = System.currentTimeMillis();

					// 扫描局域网内可用地址
					List<InetAddress> list = scanReachableAddress();
					if (!list.isEmpty()) {
						// TODO
					}

					Logger.i(ClusterNetwork.class, new StringBuilder("Scan reachable address expended time: ")
						.append((long)((System.currentTimeMillis() - start) / 1000L)).append(" seconds").toString());

					scanReachable = false; 
				}
			});
		}
	}

	@Override
	public void sessionCreated(Session session) {
		// Nothing
	}

	@Override
	public void sessionDestroyed(Session session) {
		this.clearMessage(session);
	}

	@Override
	public void sessionOpened(Session session) {
		// Nothing
	}

	@Override
	public void sessionClosed(Session session) {
		this.clearMessage(session);
	}

	@Override
	public void messageReceived(final Session session, final Message message) {
		this.executor.execute(new Runnable() {
			@Override
			public void run() {
				ByteBuffer buffer = parseMessage(session, message);
				if (null != buffer) {
					process(session, buffer);
				}
			}
		});
	}

	@Override
	public void messageSent(Session session, Message message) {
		// Nothing
	}

	@Override
	public void errorOccurred(int errorCode, Session session) {
		// Nothing
	}

	/**
	 * 处理解析后的数据。
	 * 
	 * @param session
	 * @param buffer
	 */
	private void process(Session session, ByteBuffer buffer) {
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

		// 创建协议
		ClusterProtocol protocol = ClusterProtocolFactory.create(prop);
		if (null != protocol) {
			// 设置上下文会话
			protocol.contextSession = session;
			// 处理具体协议逻辑
			this.distribute(protocol);
		}
		else {
			Logger.w(this.getClass(), new StringBuilder("Unknown protocol:\n").append(str).toString());
		}
	}

	/**
	 * 分发协议。将协议通知观察者。
	 * 
	 * @param protocol 指定协议。
	 */
	private void distribute(ClusterProtocol protocol) {
		this.setChanged();
		this.notifyObservers(protocol);
		this.clearChanged();
	}

	/**
	 * 处理消息。
	 * 
	 * @param session
	 * @param message
	 */
	private ByteBuffer parseMessage(Session session, Message message) {
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
			ByteBuffer buf = ByteBuffer.allocate(this.bufferSize);
			Queue<byte[]> queue = this.sessionMessageCache.get(session.getId());
			if (null != queue) {
				while (!queue.isEmpty()) {
					byte[] bytes = queue.poll();
					buf.put(bytes);
				}
				this.sessionMessageCache.remove(session.getId());
			}
			buf.put(data);
			buf.flip();
			return buf;
		}
		else {
			// 数据未结束
			Queue<byte[]> queue = this.sessionMessageCache.get(session.getId());
			if (null == queue) {
				queue = new LinkedList<byte[]>();
				this.sessionMessageCache.put(session.getId(), queue);
			}
			queue.offer(data);
			return null;
		}
	}

	/**
	 * 清空指定会话的消息队列。
	 * 
	 * @param session 指定待清空的会话。
	 */
	private void clearMessage(Session session) {
		Queue<byte[]> queue = this.sessionMessageCache.get(session.getId());
		if (null != queue) {
			queue.clear();
			this.sessionMessageCache.remove(session.getId());
		}
	}

	/**
	 * 扫描网段内可用的 IP 地址。
	 * 
	 * @return 返回 IP 地址列表。
	 */
	private List<InetAddress> scanReachableAddress() {
		ArrayList<InetAddress> result = new ArrayList<InetAddress>();
		try {
			// 枚举所有接口的 IP 地址
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface ni = interfaces.nextElement();
				List<InterfaceAddress> list = ni.getInterfaceAddresses();
				for (InterfaceAddress addr : list) {
					// 如果是回环地址则继续
					if (addr.getAddress().isLoopbackAddress()) {
						continue;
					}
					// 处理 IPv4 地址
					if (Utils.isIPv4(addr.getAddress().getHostAddress())) {
						// JDK Bug
						// See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6707289
						short mark = addr.getNetworkPrefixLength();
						if (mark == 128) {
							mark = 24;
						}

						if (mark != 24) {
							// 如果不是 C 类地址，则检查下一个地址
							continue;
						}

						int[] netaddr = Utils.splitIPv4Address(addr.getAddress().getHostAddress());
						int[] netmark = Utils.convertIPv4NetworkPrefixLength(mark);

						int self = netaddr[3];

						// 生成地址
						int s1 = netaddr[0] & netmark[0];
						int s2 = netaddr[1] & netmark[1];
						int s3 = netaddr[2] & netmark[2];
//						int s4 = netaddr[3] & netmark[3];

						for (int i = 1; i < 255; ++i) {
							if (this.interrupted) {
								result.clear();
								return result;
							}

							if (self == i) {
								continue;
							}

							InetAddress ia = null;

							try {
								ia = InetAddress.getByAddress(new byte[] {(byte)s1, (byte)s2, (byte)s3, (byte)i});
							} catch (UnknownHostException uhe) {
								Logger.log(ClusterNetwork.class, uhe, LogLevel.WARNING);
								continue;
							}

							try {
								if (ia.isReachable(3000)) {
									// 添加数据
									result.add(ia);
									if (Logger.isDebugLevel()) {
										Logger.d(this.getClass(), new StringBuilder("Cluster test address: ")
												.append(ia.getHostAddress()).append(" - Reachable").toString());
									}
								}
								else {
									if (Logger.isDebugLevel()) {
										Logger.d(this.getClass(), new StringBuilder("Cluster test address: ")
												.append(ia.getHostAddress()).append(" - Unreachable").toString());
									}
								}
							} catch (IOException e) {
								Logger.log(ClusterNetwork.class, e, LogLevel.WARNING);
							}
						}
					}
					else {
						// TODO 处理 IPv6 地址
					}
				}
			}
		} catch (SocketException e) {
			Logger.log(ClusterNetwork.class, e, LogLevel.ERROR);
		}

		return result;
	}

}
