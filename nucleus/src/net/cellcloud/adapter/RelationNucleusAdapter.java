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

package net.cellcloud.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.cellcloud.adapter.gene.Gene;
import net.cellcloud.adapter.gene.GeneHeader;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.QuotaCalculator;
import net.cellcloud.common.QuotaCalculatorCallback;
import net.cellcloud.core.Endpoint;
import net.cellcloud.core.Nucleus;
import net.cellcloud.core.Role;
import net.cellcloud.talk.dialect.Dialect;
import net.cellcloud.util.rudp.ReliableServerSocket;
import net.cellcloud.util.rudp.ReliableSocket;
import net.cellcloud.util.rudp.ReliableSocketStateListener;

/**
 * 关联内核适配器接口。
 * 
 * @author Ambrose Xu
 * 
 */
public abstract class RelationNucleusAdapter implements Adapter {

	/**
	 * 适配器名称。
	 */
	private String name;

	/**
	 * 适配器实例名。
	 */
	private String instanceName;

	/**
	 * 适配器的对外主机名或地址。
	 */
	private String host = "127.0.0.1";

	/**
	 * 适配器的监听端口。
	 */
	private int port = 9813;

	/**
	 * 与自己关联的终端清单。
	 */
	private LinkedList<Endpoint> endpointList;
	/**
	 * 关键终端对应的 Socket 映射。
	 */
	private HashMap<Endpoint, ReliableSocket> clientSocketMap;

	/**
	 * 故障节点列表。
	 */
	private LinkedList<Endpoint> failureList;

	/**
	 * 连接到适配器的其他终端的 Socket 描述传与其终端的映射。
	 * 用于通过 Socket 的描述串查找其对应的终端。
	 * <p>
	 * Key: Socket address "host:port"
	 */
	private ConcurrentHashMap<String, Endpoint> socketMap;

	/**
	 * 连接超时时间。
	 */
	private int connectTimeout = 10 * 1000;
	/**
	 * Socket 超时时间。
	 */
	private int soTimeout = 30 * 1000;

	/**
	 * 用于接收客户端数据的任务列表。
	 */
	private LinkedList<ReceiverTask> receiverTaskList;

	/**
	 * 服务器网络线程。
	 */
	private ServerThread thread;

	/**
	 * 服务器的 Socket 实例。
	 */
	private ReliableServerSocket serverSocket;

	/**
	 * 监听器列表。
	 */
	protected ArrayList<AdapterListener> listeners;

	/**
	 * 计划任务执行器服务。
	 */
	private ScheduledExecutorService scheduledExecutor;

	/**
	 * 配额计算器。用于控制数据发送的带宽。
	 */
	private QuotaCalculator quota;
	/**
	 * 配额适配器的拥塞回调。
	 */
	private QuotaCallback quotaCallback;

	/**
	 * Gene 数据的全局序号。自动累加。
	 */
	private AtomicLong geneSeq;

	/**
	 * 接收到的 Gene 序号。用于压制重复数据（因为 RUDP 未进行数据压制）。
	 */
	private ConcurrentHashMap<Endpoint, Vector<Long>> receivedGeneSeq;

	/**
	 * 心跳计划任务的 Future 句柄。
	 */
	private ScheduledFuture<?> heartbeatFuture;

	/**
	 * 心跳包的 Gene 名。
	 */
	private final static String sHeartbeat = "CellCloudHeartbeat";

	/**
	 * 构造器。
	 * 构建指定名称的适配器。
	 * 
	 * @param name 指定适配器的名称。
	 * @param instanceName 指定适配器的实例名。
	 */
	public RelationNucleusAdapter(String name, String instanceName) {
		this.name = name;
		this.instanceName = instanceName;

		this.endpointList = new LinkedList<Endpoint>();
		this.clientSocketMap = new HashMap<Endpoint, ReliableSocket>();
		this.socketMap = new ConcurrentHashMap<String, Endpoint>();

		this.failureList = new LinkedList<Endpoint>();

		this.receiverTaskList = new LinkedList<ReceiverTask>();

		this.listeners = new ArrayList<AdapterListener>();

		this.scheduledExecutor = Executors.newScheduledThreadPool(2);

		this.quota = new QuotaCalculator(this.scheduledExecutor, 1024 * 1024);
		this.quotaCallback = new QuotaCallback();

		this.geneSeq = new AtomicLong(0);
		this.receivedGeneSeq = new ConcurrentHashMap<Endpoint, Vector<Long>>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final String getName() {
		return this.name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final String getInstanceName() {
		return this.instanceName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void config(Map<String, Object> parameters) {
		Object port = parameters.get("port");
		if (null != port) {
			this.port = Integer.parseInt(port.toString());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setup() {
		if (null == this.thread) {
			this.thread = new ServerThread();
			this.thread.setDaemon(true);
			this.thread.start();
		}

		this.quota.start();

		// callback start
		this.onStart();

		this.heartbeatFuture = this.scheduledExecutor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				Gene gene = new Gene(sHeartbeat);
				broadcast(gene);
			}
		}, 5L, 5L * 60L, TimeUnit.SECONDS);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void teardown() {
		if (null != this.heartbeatFuture) {
			this.heartbeatFuture.cancel(false);
			this.heartbeatFuture = null;
		}

		// callback stop
		this.onStop();

		this.quota.stop();

		this.scheduledExecutor.shutdown();

		if (null != this.thread) {
			this.thread.terminate();
			this.thread = null;
		}

		ArrayList<ReceiverTask> taskList = new ArrayList<ReceiverTask>(this.receiverTaskList.size());
		taskList.addAll(this.receiverTaskList);
		for (ReceiverTask task : taskList) {
			task.terminate();
		}
		this.receiverTaskList.clear();

		synchronized (this.endpointList) {
			for (ReliableSocket socket : this.clientSocketMap.values()) {
				try {
					if (!socket.isClosed()) {
						socket.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			this.endpointList.clear();
			this.clientSocketMap.clear();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isReady() {
		return (null != this.thread);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addEndpoint(Endpoint endpoint) {
		synchronized (this.endpointList) {
			this.endpointList.add(endpoint);
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeEndpoint(Endpoint endpoint) {
		ReliableSocket socket = null;

		synchronized (this.endpointList) {
			socket = this.clientSocketMap.remove(endpoint);
			this.endpointList.remove(endpoint);
		}

		if (null != socket) {
			try {
				socket.close();
			} catch (Exception e) {
				// Nothing
			}
		}

		this.receivedGeneSeq.remove(endpoint);

		synchronized (this.failureList) {
			this.failureList.remove(endpoint);
		}
	}

	/**
	 * 返回所有预置终端列表。
	 * 
	 * @return 直接返回的终端列表实例。
	 */
	protected List<Endpoint> endpointList() {
		return this.endpointList;
	}

	/**
	 * 判读指定终端是否是故障节点。
	 * 
	 * @param endpoint 指定待判断节点。
	 * @return 如果给定的终端是故障节点返回 <code>true</code>，否则返回 <code>false</code> 。
	 */
	public boolean isFailureEndpoint(Endpoint endpoint) {
		synchronized (this.failureList) {
			return this.failureList.contains(endpoint);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addListener(AdapterListener listener) {
		synchronized (this.listeners) {
			this.listeners.add(listener);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeListener(AdapterListener listener) {
		synchronized (this.listeners) {
			this.listeners.remove(listener);
		}
	}

	/**
	 * 设置该适配器终端主机名或地址。
	 * 
	 * @param host 指定字符串格式的主机名或地址。
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * 获得该适配器终端的主机名或地址。
	 * 
	 * @return 返回字符串形式的主机名或地址。
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * 设置该适配器终端的监听端口。
	 * 
	 * @param port 指定端口号。
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 获得该适配器终端的监听端口。
	 * 
	 * @return 返回整数形式的监听端口号。
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * 设置数据传输配额。单位：每秒字节数。
	 * 
	 * @param quotaInSecond 指定每秒传输的带宽，单位：B/S (Bytes Per Second) 。
	 */
	public void setTransmissionQuota(int quotaInSecond) {
		this.quota.setQuota(quotaInSecond);
	}

	/**
	 * 获得数据传输配额。单位：每秒字节数。
	 * 
	 * @return 返回每秒传输带宽配额，单位：B/S (Bytes Per Second) 。
	 */
	public int getTransmissionQuota() {
		return this.quota.getQuota();
	}

	/**
	 * 向关联的终端分享方言。
	 * 
	 * 使用该方法，适配器会根据适配和路由规则进行数据发送。
	 * 
	 * @param keyword 指定该方言的关键字。该关键字用于索引或关联该方言数据。
	 * @param dialect 指定方言数据。
	 */
	public abstract void share(String keyword, Dialect dialect);

	/**
	 * 向指定的终端分享方言。
	 * 
	 * @param keyword 指定该方言的关键字。该关键字用于索引或关联该方言数据。
	 * @param endpoint 指定目标终端。
	 * @param dialect 指定方言数据。
	 */
	public abstract void share(String keyword, Endpoint endpoint, Dialect dialect);

	/**
	 * 忽略当前适配或路由规则向终端列表里的所有终端进行数据广播（自动跳过故障终端）。
	 * 
	 * @param gene 指定待发送的 Gene 数据。
	 */
	protected void broadcast(Gene gene) {
		if (this.endpointList.isEmpty()) {
			return;
		}

		this.transport(this.endpointList, gene);
	}

	/**
	 * 向指定终端传送数据。
	 * 
	 * @param destination 指定目标终端。
	 * @param gene 指定 Gene 数据。
	 */
	protected void transport(Endpoint destination, Gene gene) {
		ArrayList<Endpoint> list = new ArrayList<Endpoint>(1);
		list.add(destination);
		this.transport(list, gene);
	}

	/**
	 * 向指定终端列表里的终端传送数据。
	 * 
	 * @param destinationList 指定目标终端列表。
	 * @param gene 指定 Gene 数据。
	 */
	protected void transport(List<Endpoint> destinationList, Gene gene) {
		// 填充 Gene 头
		gene.setHeader(GeneHeader.SourceTag, Nucleus.getInstance().getTagAsString());
		gene.setHeader(GeneHeader.Host, this.host);
		gene.setHeader(GeneHeader.Port, String.valueOf(this.port));

		long seq = this.geneSeq.getAndIncrement();
		gene.setHeader(GeneHeader.Seq, String.valueOf(seq));
		if (seq == Long.MAX_VALUE || seq < 0) {
			this.geneSeq.set(0);
		}

		// 打包
		byte[] bytes = Gene.pack(gene);

		for (int i = 0; i < destinationList.size(); ++i) {
			Endpoint ep = destinationList.get(i);

			synchronized (this.failureList) {
				// 如果是故障节点，则跳过
				if (this.failureList.contains(ep)) {
					continue;
				}
			}

			// 传输数据
			if (!this.transport(ep, bytes)) {
				this.fireTransportFailure(ep, gene);
			}
			else {
				this.fireSend(ep, gene);
			}
		}
	}

	/**
	 * 向指定终端发送字节流。
	 * 
	 * 当发送数据流超过配额时，此方法会被阻塞，直到下一次有效配额分配。
	 * 
	 * @param destination 指定目标终端。
	 * @param data 指定待发送字节流数组。
	 * @return 如果发送数据时发生错误返回 <code>false</code>，否则返回 <code>true</code> 。
	 */
	private synchronized boolean transport(Endpoint destination, byte[] data) {
		ReliableSocket socket = null;
		boolean newSocket = false;

		synchronized (this.endpointList) {
			socket = this.clientSocketMap.get(destination);
		}

		if (null == socket) {
			try {
				socket = new ReliableSocket();
			} catch (IOException e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
				return false;
			}

			newSocket = true;
		}

		if (!socket.isConnected()) {
			InetSocketAddress address = new InetSocketAddress(destination.getHost(), destination.getPort());
			try {
				Logger.d(this.getClass(), "Try connect adapter: " + destination.toString());

				socket.connect(address, this.connectTimeout);
			} catch (Exception e) {
				Logger.log(this.getClass(), e, LogLevel.WARNING);
				return false;
			}
		}

		if (newSocket) {
			boolean dual = false;

			// 在连接建立期间，如果服务器接收到连接，则 socketMap 里已经有 value 一样的 Endpoint 实例
			Iterator<Endpoint> iter = this.socketMap.values().iterator();
			while (iter.hasNext()) {
				Endpoint dst = iter.next();
				if (dst.equals(destination)) {
					dual = true;
					break;
				}
			}

			if (dual) {
				try {
					socket.close();
				} catch (IOException e) {
					Logger.log(this.getClass(), e, LogLevel.DEBUG);
				}

				socket = this.clientSocketMap.get(destination);
				if (null == socket) {
					try {
						Thread.sleep(1L);
					} catch (InterruptedException e) {
						// Nothing
					}
					socket = this.clientSocketMap.get(destination);
				}
			}
			else {
				// 新 socket
				this.clientSocketMap.put(destination, socket);

				// 启动接收数据任务
				ReceiverTask task = new ReceiverTask((ReliableSocket) socket);
				synchronized (this.receiverTaskList) {
					this.receiverTaskList.add(task);
				}
				task.start();
			}
		}

		// 配额计算，并阻塞
		this.quota.consumeBlocking(data.length, this.quotaCallback, destination);

		OutputStream os = null;
		try {
			os = socket.getOutputStream();
			os.write(data);
			os.flush();
		} catch (Exception e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
			return false;
		}

		return true;
	}

	/**
	 * 当启动适配器时该方法被回调。
	 */
	protected abstract void onStart();

	/**
	 * 当适配器准备就绪时该方法被回调。
	 */
	protected abstract void onReady();

	/**
	 * 当停止适配器时该方法被回调。
	 */
	protected abstract void onStop();

	/**
	 * 当有 Gene 数据被成功发送时该方法被回调。
	 * 
	 * @param endpoint 目的终端。
	 * @param gene 数据。
	 */
	protected abstract void onSend(Endpoint endpoint, Gene gene);

	/**
	 * 当有 Gene 数据被成功接收时该方法被回调。
	 * 
	 * @param endpoint 来源终端。
	 * @param gene 数据。
	 */
	protected abstract void onReceive(Endpoint endpoint, Gene gene);

	/**
	 * 当数据传送发生故障时该方法被回调。
	 * 
	 * @param endpoint 发生故障的终端。
	 * @param gene 故障发生时正在传送的数据。当没有数据发生故障时为空指针。
	 */
	protected abstract void onTransportFailure(Endpoint endpoint, Gene gene);

	/**
	 * 触发数据发送回调。
	 * 
	 * @param endpoint 指定目的终端。
	 * @param gene 指定数据。
	 */
	private void fireSend(Endpoint endpoint, Gene gene) {
		// 去除故障节点
		synchronized (this.failureList) {
			if (!this.failureList.isEmpty()) {
				this.failureList.remove(endpoint);
			}
		}

		this.onSend(endpoint, gene);
	}

	/**
	 * 触发接收数据回调。
	 * 
	 * @param endpoint 指定数据源终端。
	 * @param gene 指定数据。
	 * @param socket 指定数据源的 Socket 。
	 */
	private void fireReceive(Endpoint endpoint, Gene gene, ReliableSocket socket) {
		// 判断数据是否重复，压制重复数据
		Long seq = Long.valueOf(gene.getHeader(GeneHeader.Seq));
		Vector<Long> list = this.receivedGeneSeq.get(endpoint);
		if (null != list) {
			if (list.contains(seq)) {
				return;
			}
		}
		else {
			list = new Vector<Long>();
			this.receivedGeneSeq.put(endpoint, list);
		}
		// 添加新 seq
		list.add(seq);
		if (list.size() > 100) {
			list.remove(0);
		}

		// 查找终端
		Endpoint source = this.findEndpoint(endpoint);

		if (null == source) {
			// 未找到终端，增加新终端

			synchronized (this.endpointList) {
				this.endpointList.add(endpoint);
			}

			source = endpoint;
		}

		// 匹配 socket
		synchronized (this.endpointList) {
			if (!this.clientSocketMap.containsKey(source)) {
				this.clientSocketMap.put(source, socket);
			}
		}

		// 去除故障节点
		synchronized (this.failureList) {
			if (!this.failureList.isEmpty()) {
				this.failureList.remove(endpoint);
			}
		}

		if (gene.getName().equals(sHeartbeat)) {
			// 处理心跳
			Logger.i(this.getClass(), "Update endpoint '" + endpoint.toString() + "' with heartbeat");
			return;
		}

		this.onReceive(source, gene);
	}

	/**
	 * 触发数据传输故障回调。
	 * 
	 * @param endpoint 指定故障终端。
	 * @param gene 指定发生故障时的 Gene 。
	 */
	private void fireTransportFailure(Endpoint endpoint, Gene gene) {
		Logger.e(this.getClass(), "Transport failed: " + endpoint.toString());

		synchronized (this.failureList) {
			if (!this.failureList.contains(endpoint)) {
				this.failureList.add(endpoint);
			}
		}

		// 跳过心跳包
		if (null == gene || gene.getName().equals(sHeartbeat)) {
			this.onTransportFailure(endpoint, null);
		}
		else {
			this.onTransportFailure(endpoint, gene);
		}
	}

	/**
	 * 查找指定终端对应的终端实例。
	 * 
	 * @param endpoint 待查找的终端。
	 * @return 返回查找到的终端的实例。
	 */
	private Endpoint findEndpoint(Endpoint endpoint) {
		if (null == endpoint) {
			return null;
		}

		synchronized (this.endpointList) {
			for (Endpoint ep : this.endpointList) {
				if (ep.equals(endpoint)) {
					if (null == ep.getTag()) {
						ep.setTag(endpoint.getTag());
					}
					return ep;
				}
			}
		}

		return null;
	}

	/**
	 * 服务器守护线程。
	 */
	private class ServerThread extends Thread {
		private boolean stopped = false;

		public ServerThread() {
			super(name + "-" + instanceName);
		}

		/**
		 * 停止线程。
		 */
		public void terminate() {
			this.stopped = true;

			if (null != serverSocket) {
				serverSocket.close();
			}
		}

		@Override
		public void run() {
			try {
				serverSocket = new ReliableServerSocket(port);
			} catch (SocketException e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			} catch (IOException e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			}

			if (null == serverSocket) {
				return;
			}

			Logger.i(this.getClass(), "Adapter '" + this.getName() + "' started at " + port);

			// 回调就绪
			onReady();

			while (!this.stopped) {
				Socket socket = null;
				try {
					socket = serverSocket.accept();
				} catch (Exception e) {
					Logger.log(this.getClass(), e, LogLevel.ERROR);
					continue;
				}

				if (null == socket) {
					try {
						Thread.sleep(100L);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					continue;
				}

				try {
					socket.setKeepAlive(true);
					socket.setSoTimeout(soTimeout);
				} catch (SocketException e) {
					Logger.log(this.getClass(), e, LogLevel.WARNING);
				}

				// 启动接收数据任务
				ReceiverTask task = new ReceiverTask((ReliableSocket) socket);
				synchronized (receiverTaskList) {
					receiverTaskList.add(task);
				}
				task.start();
			}

			if (null != serverSocket) {
				if (!serverSocket.isClosed()) {
					serverSocket.close();
				}
			}
		}
	}

	/**
	 * 客户端数据接收任务线程。
	 */
	protected class ReceiverTask extends Thread implements ReliableSocketStateListener {

		/** 线程自旋。 */
		private boolean spinning = false;
		/** Socket 句柄。 */
		private ReliableSocket socket;

		/** 远程主机名或地址。 */
		private String remoteHost;
		/** 远程主机的端口。 */
		private int remotePort;
		/** Socket 描述串。 */
		private String socketString;

		/**
		 * 构造器。
		 * 
		 * @param socket 指定 Socket 句柄。
		 */
		public ReceiverTask(ReliableSocket socket) {
			super("ReceiverTask-" + socket.getInetAddress().getHostAddress());
			this.socket = socket;
			this.socket.addStateListener(this);

			this.remoteHost = this.socket.getInetAddress().getHostAddress().toString();
			this.remotePort = this.socket.getPort();

			this.socketString = this.remoteHost + ":" + this.remotePort;
		}

		/**
		 * 获得远程主机名或地址。
		 * 
		 * @return 返回字符串形式的主机名或地址。
		 */
		public String getRemoteHost() {
			return this.remoteHost;
		}

		/**
		 * 获得远程主机端口。
		 * 
		 * @return 返回端口号。
		 */
		public int getRemotePort() {
			return this.remotePort;
		}

		/**
		 * 停止线程。
		 */
		public void terminate() {
			this.spinning = false;

			try {
				if (!this.socket.isClosed()) {
					this.socket.close();
				}
			} catch (IOException e) {
				Logger.log(this.getClass(), e, LogLevel.DEBUG);
			}

			receiverTaskList.remove(this);
		}

		@Override
		public void run() {
			this.spinning = true;

			try {
				InputStream in = this.socket.getInputStream();
				ByteBuffer packet = ByteBuffer.allocate(65536);
				byte[] buf = new byte[4096];

				while (this.spinning) {
					packet.clear();

					int total = 0;
					int len = -1;
					boolean eop = false;

					try {
						while ((len = in.read(buf)) > 0) {
							packet.put(buf, 0, len);
							total += len;

							if (in.available() > 0) {
								continue;
							}
							else {
								// 判断数据结尾符
								eop = true;
								if (packet.position() >= 5) {
									for (int i = packet.position() - 5, index = 0; index < 5; ++i, ++index) {
										if (packet.get(i) != Gene.EOP_BYTES[index]) {
											eop = false;
											break;
										}
									}
								}

								if (eop) {
									break;
								}
								else {
									continue;
								}
							}
						}
					} catch (SocketTimeoutException e) {
						Logger.log(this.getClass(), e, LogLevel.ERROR);
					}
					catch (Exception e) {
						Logger.log(this.getClass(), e, LogLevel.ERROR);
					}

					if (packet.position() <= 0) {
						try {
							Thread.sleep(20L);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						continue;
					}

					// flip
					packet.flip();
					byte[] data = new byte[total];
					packet.get(data, 0, total);

					Gene gene = Gene.unpack(data);
					if (null == gene) {
						Logger.w(this.getClass(), "Parse gene data failed, from " + this.socketString);
						data = null;
						continue;
					}

					// 读取 Gene 头
					String tag = gene.getHeader(GeneHeader.SourceTag);
					String host = gene.getHeader(GeneHeader.Host);
					int port = Integer.parseInt(gene.getHeader(GeneHeader.Port));
					// 创建 Endpoint
					Endpoint endpoint = new Endpoint(tag, Role.NODE, host, port);

					// 更新 socket map
					if (!socketMap.containsKey(this.socketString)) {
						socketMap.put(this.socketString, endpoint);
					}

					// 回调
					fireReceive(endpoint, gene, this.socket);

					data = null;
				}

				packet = null;
				buf = null;
			} catch (IOException e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			} catch (Exception e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			}

			// 删除映射关系
			Endpoint endpoint = socketMap.remove(this.socketString);
			if (null != endpoint) {
				synchronized (endpointList) {
					// 删除
					clientSocketMap.remove(endpoint);
				}

				boolean failureCB = false;
				synchronized (failureList) {
					// 添加到故障节点
					if (!failureList.contains(endpoint)) {
						failureList.add(endpoint);
						failureCB = true;
					}
				}
				if (failureCB) {
					// 回调故障
					fireTransportFailure(endpoint, null);
				}

				// 删除接收 Seq
				receivedGeneSeq.remove(endpoint);
			}

			Logger.d(this.getClass(), "Receiver thread stopped: " + this.socketString);
		}

		@Override
		public void connectionOpened(ReliableSocket sock) {
			Logger.d(this.getClass(), "connectionOpened");
		}

		@Override
		public void connectionRefused(ReliableSocket sock) {
			Logger.d(this.getClass(), "connectionRefused");
		}

		@Override
		public void connectionClosed(ReliableSocket sock) {
			Logger.d(this.getClass(), "connectionClosed");

			this.terminate();
		}

		@Override
		public void connectionFailure(ReliableSocket sock) {
			Logger.d(this.getClass(), "connectionFailure");
		}

		@Override
		public void connectionReset(ReliableSocket sock) {
			Logger.d(this.getClass(), "connectionReset");
		}
	}

	/**
	 * 配额计算发生拥塞的回调接口实现。
	 */
	private class QuotaCallback implements QuotaCalculatorCallback {

		/**
		 * 构造器。
		 */
		public QuotaCallback() {
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onCallback(int size, Object custom) {
			for (int i = 0; i < listeners.size(); ++i) {
				AdapterListener l = listeners.get(i);
				l.onCongested(RelationNucleusAdapter.this, (Endpoint) custom, size);
			}
		}
	}

}
