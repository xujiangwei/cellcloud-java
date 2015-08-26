/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2015 Cell Cloud Team (www.cellcloud.net)

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
import java.util.LinkedList;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.core.Endpoint;
import net.cellcloud.core.Nucleus;
import net.cellcloud.util.CachedQueueExecutor;
import udt.UDTClient;
import udt.UDTInputStream;
import udt.UDTServerSocket;
import udt.UDTSocket;

/** 关联内核适配器接口。
 * 
 * @author Jiangwei Xu
 */
public abstract class RelationNucleusAdapter implements Adapter {

	private String name;

	private final int port = 9813;

	private LinkedList<Endpoint> endpointList;
	private LinkedList<UDTClient> clientList;

	private ServerThread thread;
	private CachedQueueExecutor executor;

	private UDTServerSocket serverSocket;

	/** 构建指定名称的适配器。
	 */
	public RelationNucleusAdapter(String name) {
		this.name = name;
		this.executor = CachedQueueExecutor.newCachedQueueThreadPool(4);
		this.endpointList = new LinkedList<Endpoint>();
		this.clientList = new LinkedList<UDTClient>();
	}

	@Override
	public final String getName() {
		return this.name;
	}

	@Override
	public void setup() {
		this.thread = new ServerThread();
		this.thread.start();
	}

	@Override
	public void teardown() {
		this.thread.terminate();
		this.thread = null;
	}

	@Override
	public boolean isReady() {
		return (null != this.thread);
	}

	/**
	 * 
	 * @param endpoint
	 */
	public boolean addEndpoint(Endpoint endpoint) {
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

		if (null == client) {
			return false;
		}

		synchronized (this.endpointList) {
			this.clientList.add(client);
			this.endpointList.add(endpoint);
		}

		return true;
	}

	/**
	 * 
	 * @param endpoint
	 */
	public void removeEndpoint(Endpoint endpoint) {
		UDTClient client = null;

		synchronized (this.endpointList) {
			int index = this.endpointList.indexOf(endpoint);
			if (index < 0) {
				return;
			}

			this.endpointList.remove(endpoint);
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
	 * 广播。
	 * @param index
	 */
	protected void broadcast(final Gene data) {
		final ArrayList<Endpoint> list = new ArrayList<Endpoint>(this.endpointList.size());

		synchronized (this.endpointList) {
			list.addAll(this.endpointList);
		}

		this.executor.execute(new Runnable() {
			@Override
			public void run() {
				for (Endpoint ep : list) {

					// 传输数据
					transport(ep, data);

					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				list.clear();
			}
		});
	}

	protected boolean transport(Endpoint destination, Gene data) {
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
				client.connect(destination.getCoordinate().getAddress().getHostString(), destination.getCoordinate().getAddress().getPort());
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

		// 发送数据
		try {
			client.sendBlocking(data.packet());
		} catch (IOException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
			return false;
		} catch (InterruptedException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
			return false;
		}

		return true;
	}

	/**
	 * 准备就绪。
	 */
	protected abstract void onReady();

	/**
	 * 收到事件。
	 * @param event
	 */
	protected abstract void onReceive(Endpoint endpoint, Gene gene);

	/** 返回内核标签。 */
	public String getNucleusTag() {
		return Nucleus.getInstance().getTagAsString();
	}

	private Endpoint findEndpoint(String host) {
		synchronized (this.endpointList) {
			for (Endpoint ep : this.endpointList) {
				if (ep.getCoordinate().getAddress().getHostString().equals(host)) {
					return ep;
				}
			}
		}

		return null;
	}

	private class ServerThread extends Thread {
		private boolean stopped = false;

		public ServerThread() {
		}

		public void terminate() {
			this.stopped = true;
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
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					continue;
				}

				// 执行任务
				executor.execute(new ClientTask(socket));
			}

			if (null != serverSocket) {
				serverSocket.shutDown();
				serverSocket = null;
			}
		}
	}

	private class ClientTask implements Runnable {
		private UDTSocket socket;

		public ClientTask(UDTSocket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			try {
				UDTInputStream in = this.socket.getInputStream();
				ByteBuffer packet = ByteBuffer.allocate(65536);
				byte[] buf = new byte[4096];
				int len = -1;
				while ((len = in.read(buf)) > 0) {
					packet.put(buf, 0, len);
				}

				String host = this.socket.getSession().getDestination().getAddress().getHostAddress();
				Endpoint endpoint = findEndpoint(host);
				if (null == endpoint) {
					Logger.e(this.getClass(), "Can not find host by " + host);
					return;
				}

				Gene gene = parse(new String(packet.array(), "UTF-8"));
				if (null == gene) {
					Logger.e(this.getClass(), "Parse gene data failed, from " + host);
					return;
				}

				// 回调
				onReceive(endpoint, gene);
			} catch (IOException e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			}
		}
	}

	private Gene parse(String data) {
		String[] ret = data.split("\\\r\\\n");
		if (ret.length < 2) {
			Logger.w(RelationNucleusAdapter.class, "Data format error");
			return null;
		}

		String name = ret[0];
		Gene gene = new Gene(name);

		for (int i = 1; i < ret.length; ++i) {
			String r = ret[i];

			if (r.length() == 0) {
				gene.setBody(ret[i+1]);
				break;
			}

			int index = r.indexOf(":");
			if (index > 0) {
				String key = r.substring(0, index);
				String value = r.substring(index + 1);
				gene.setHeader(key.trim(), value.trim());
			}
		}

		return gene;
	}
}
