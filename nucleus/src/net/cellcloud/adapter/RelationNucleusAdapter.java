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
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import udt.UDTClient;
import udt.UDTInputStream;
import udt.UDTServerSocket;
import udt.UDTSession;
import udt.UDTSocket;

/** 关联内核适配器接口。
 * 
 * @author Jiangwei Xu
 */
public abstract class RelationNucleusAdapter implements Adapter {

	private String name;
	private String instanceName;

	private String host = "127.0.0.1";
	private int port = 9813;

	private LinkedList<Endpoint> endpointList;
	private LinkedList<UDTClient> clientList;

	private LinkedList<ReceiverTask> receiverTaskList;

	private ServerThread thread;
	private ExecutorService executor;

	private UDTServerSocket serverSocket;

	private ArrayList<AdapterListener> listeners;

	private QuotaCalculator quota;
	private QuotaCallback quotaCallback;

	private AtomicLong geneSeq;

	/** 构建指定名称的适配器。
	 */
	public RelationNucleusAdapter(String name, String instanceName) {
		this.name = name;
		this.instanceName = instanceName;
		this.executor = Executors.newCachedThreadPool(); //CachedQueueExecutor.newCachedQueueThreadPool(4);
		this.endpointList = new LinkedList<Endpoint>();
		this.clientList = new LinkedList<UDTClient>();
		this.receiverTaskList = new LinkedList<ReceiverTask>();
		this.listeners = new ArrayList<AdapterListener>();
		this.quota = new QuotaCalculator(100 * 1024);
		this.quotaCallback = new QuotaCallback();
		this.geneSeq = new AtomicLong(0);
	}

	@Override
	public final String getName() {
		return this.name;
	}

	@Override
	public final String getInstanceName() {
		return this.instanceName;
	}

	@Override
	public void config(Map<String, Object> parameters) {
		Object port = parameters.get("port");
		if (null != port) {
			this.port = Integer.parseInt(port.toString());
		}
	}

	@Override
	public void setup() {
		if (null == this.thread) {
			this.thread = new ServerThread();
			this.thread.setDaemon(true);
			this.thread.start();
		}

		synchronized (this.endpointList) {
			for (int i = 0; i < this.endpointList.size(); ++i) {
				// 获取客户端
				UDTClient client = null;
				try {
					// 创建连接客户端
					client = new UDTClient(InetAddress.getLocalHost());
				} catch (SocketException e) {
					Logger.log(this.getClass(), e, LogLevel.ERROR);
				} catch (UnknownHostException e) {
					Logger.log(this.getClass(), e, LogLevel.ERROR);
				}

				this.clientList.add(client);
			}
		}

		this.quota.start();
	}

	@Override
	public void teardown() {
		this.quota.stop();

		this.executor.shutdown();

		if (null != this.thread) {
			this.thread.terminate();
			this.thread = null;
		}

		synchronized (this.endpointList) {
			for (UDTClient c : this.clientList) {
				try {
					if (c.isReady()) {
						c.shutdown();
					}

					Iterator<UDTSession> iter = c.getEndpoint().getSessions().iterator();
					while (iter.hasNext()) {
						UDTSession session = iter.next();
						session.setState(UDTSession.invalid);
					}
				} catch (IOException e) {
					Logger.log(this.getClass(), e, LogLevel.WARNING);
				} catch (Exception e) {
					Logger.log(this.getClass(), e, LogLevel.DEBUG);
				}
			}

			this.endpointList.clear();
			this.clientList.clear();
		}

		synchronized (this.receiverTaskList) {
			for (ReceiverTask task : this.receiverTaskList) {
				task.stop();
			}

			this.receiverTaskList.clear();
		}
	}

	@Override
	public boolean isReady() {
		return (null != this.thread);
	}

	/**
	 * 
	 * @param endpoint
	 */
	@Override
	public boolean addEndpoint(Endpoint endpoint) {
		synchronized (this.endpointList) {
			this.endpointList.add(endpoint);
		}

		return true;
	}

	/**
	 * 
	 * @param endpoint
	 */
	@Override
	public void removeEndpoint(Endpoint endpoint) {
		UDTClient client = null;

		synchronized (this.endpointList) {
			int index = this.endpointList.indexOf(endpoint);
			if (index < 0) {
				return;
			}

			this.endpointList.remove(index);
			client = this.clientList.remove(index);
		}

		if (null != client) {
			try {
				client.shutdown();
			} catch (Exception e) {
				// Nothing
			}
		}
	}

	/**
	 * 
	 * @return
	 */
	protected List<Endpoint> endpointList() {
		return this.endpointList;
	}

	@Override
	public void addListener(AdapterListener listener) {
		synchronized (this.listeners) {
			this.listeners.add(listener);
		}
	}

	@Override
	public void removeListener(AdapterListener listener) {
		synchronized (this.listeners) {
			this.listeners.remove(listener);
		}
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getHost() {
		return this.host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return this.port;
	}

	public void setQuotaPerSecond(int quotaInSecond) {
		this.quota.setQuota(quotaInSecond);
	}

	public int getQuotaPerSecond() {
		return this.quota.getQuota();
	}

	/**
	 * 
	 * @param keyword
	 * @param endpoint
	 */
	@Override
	public abstract void encourage(String keyword, Endpoint endpoint);

	/**
	 * 
	 * @param keyword
	 * @param endpoint
	 */
	@Override
	public abstract void discourage(String keyword, Endpoint endpoint);

	/**
	 * 向关联的终端分享方言。
	 * 
	 * @param dialect
	 */
	public abstract void share(String keyword, Dialect dialect);

	/**
	 * 
	 * @param keyword
	 * @param dialect
	 */
	public abstract void share(String keyword, Endpoint endpoint, Dialect dialect);

	protected void fireShared(Endpoint endpoint, Dialect dialect) {
		synchronized (this.listeners) {
			for (AdapterListener l : this.listeners) {
				l.onShared(this, endpoint, dialect);
			}
		}
	}

	/**
	 * 广播。
	 * 
	 * @param index
	 */
	protected void broadcast(Gene gene) {
		if (this.endpointList.isEmpty()) {
			return;
		}

		this.broadcast(this.endpointList, gene);
	}

	protected void broadcast(List<Endpoint> destinationList, Gene gene) {
		for (int i = 0; i < destinationList.size(); ++i) {
			Endpoint ep = destinationList.get(i);
			// 传输数据
			if (!transport(ep, gene)) {
				Logger.e(this.getClass(), "Transport failed: " + ep.getCoordinate().getAddress());
			}
		}
	}

	protected boolean transport(Endpoint destination, Gene gene) {
		UDTClient client = null;

		synchronized (this.endpointList) {
			int index = this.endpointList.indexOf(destination);
			client = this.clientList.get(index);
		}

		if (null == client) {
			return false;
		}

		// 连接
		try {
			if (!client.isReady()) {
				client.connect(destination.getCoordinate().getAddress(),
						destination.getCoordinate().getPort());
			}
		} catch (UnknownHostException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
			return false;
		} catch (InterruptedException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
			return false;
		} catch (IOException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
			return false;
		}

		// 填充 Gene 头
		gene.setHeader(GeneHeader.SourceTag, Nucleus.getInstance().getTagAsString());
		gene.setHeader(GeneHeader.Host, this.host);
		gene.setHeader(GeneHeader.Port, String.valueOf(this.port));
		gene.setHeader(GeneHeader.Seq, String.valueOf(this.geneSeq.getAndIncrement()));

		// 打包
		byte[] bytes = Gene.pack(gene);

		// 配额计算，并阻塞
		this.quota.consumeBlocking(bytes.length, this.quotaCallback, destination);

		// 发送数据
		try {
			client.sendBlocking(bytes);
		} catch (IOException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
			return false;
		} catch (InterruptedException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
			return false;
		}

		this.fireSend(destination, gene);

		return true;
	}

	/**
	 * 准备就绪。
	 */
	protected abstract void onReady();

	/**
	 * 
	 * @param endpoint
	 * @param gene
	 */
	protected abstract void onSend(Endpoint endpoint, Gene gene);

	/**
	 * 
	 * @param endpoint
	 * @param gene
	 */
	protected abstract void onReceive(Endpoint endpoint, Gene gene);

	private void fireSend(Endpoint endpoint, Gene gene) {
		this.onSend(endpoint, gene);
	}

	private void fireReceive(Endpoint endpoint, Gene gene) {
		// 查找终端
		Endpoint source = this.findEndpoint(endpoint);

		if (null == source) {
			// 未找到终端，增加新终端

			synchronized (this.endpointList) {
				UDTClient client = null;
				try {
					// 创建连接客户端
					client = new UDTClient(InetAddress.getLocalHost());
				} catch (SocketException e) {
					Logger.log(this.getClass(), e, LogLevel.ERROR);
				} catch (UnknownHostException e) {
					Logger.log(this.getClass(), e, LogLevel.ERROR);
				}

				this.endpointList.add(endpoint);
				this.clientList.add(client);
			}

			source = endpoint;
		}

		this.onReceive(source, gene);
	}

	private Endpoint findEndpoint(Endpoint endpoint) {
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

	private class ServerThread extends Thread {
		private boolean stopped = false;

		public ServerThread() {
			super(name + "-" + instanceName);
		}

		public void terminate() {
			this.stopped = true;

			if (null != serverSocket) {
				serverSocket.shutDown();
				serverSocket = null;
			}
		}

		@Override
		public void run() {
			try {
				serverSocket = new UDTServerSocket(InetAddress.getByName("0.0.0.0"), port);
			} catch (SocketException e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			} catch (UnknownHostException e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			}

			if (null == serverSocket) {
				return;
			}

			Logger.i(this.getClass(), "Adapter '" + this.getName() + "' started at " + port);

			// 回调就绪
			onReady();

			while (!this.stopped) {
				UDTSocket socket = null;
				try {
					socket = serverSocket.accept();
				} catch (InterruptedException e) {
					Logger.log(this.getClass(), e, LogLevel.ERROR);
				}

				if (null == socket) {
					try {
						Thread.sleep(100L);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					continue;
				}

				// 执行任务
				ReceiverTask task = new ReceiverTask(socket);
				synchronized (receiverTaskList) {
					receiverTaskList.add(task);
				}

				executor.execute(task);
			}

			if (null != serverSocket) {
				serverSocket.shutDown();
				serverSocket = null;
			}
		}
	}

	private class ReceiverTask implements Runnable {

		private boolean spinning = false;
		private UDTSocket socket;

		public ReceiverTask(UDTSocket socket) {
			this.socket = socket;
		}

		public void stop() {
			this.spinning = false;

			try {
				this.socket.close();
			} catch (IOException e) {
				Logger.log(this.getClass(), e, LogLevel.DEBUG);
			}
		}

		@Override
		public void run() {
			this.spinning = true;

			try {
				UDTInputStream in = this.socket.getInputStream();
				in.setBlocking(true);
				ByteBuffer packet = ByteBuffer.allocate(65536);
				byte[] buf = new byte[4096];

				while (this.spinning) {
					packet.clear();

					int total = 0;
					int len = -1;
					while ((len = in.read(buf)) > 0) {
						packet.put(buf, 0, len);
						total += len;
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

					String host = this.socket.getSession().getDestination().getAddress().getHostAddress();
					int port = this.socket.getSession().getDestination().getPort();

					Gene gene = Gene.unpack(data);
					if (null == gene) {
						Logger.e(this.getClass(), "Parse gene data failed, from " + host + ":" + port);
						data = null;
						return;
					}

					// 读取 Gene 头
					String tag = gene.getHeader(GeneHeader.SourceTag);
					host = gene.getHeader(GeneHeader.Host);
					port = Integer.parseInt(gene.getHeader(GeneHeader.Port));
					// 创建 Endpoint
					Endpoint endpoint = new Endpoint(tag, Role.NODE, host, port);

					// 回调
					fireReceive(endpoint, gene);

					data = null;
				}

				packet = null;
				buf = null;
			} catch (IOException e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			}
		}
	}

	private class QuotaCallback implements QuotaCalculatorCallback {

		public QuotaCallback() {
		}

		@Override
		public void onCallback(int size, Object custom) {
			synchronized (listeners) {
				for (AdapterListener l : listeners) {
					l.onCongested(RelationNucleusAdapter.this, (Endpoint) custom, size);
				}
			}
		}
	}

}
