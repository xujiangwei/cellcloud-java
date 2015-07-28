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
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.core.Nucleus;
import net.cellcloud.util.CachedQueueExecutor;

import org.json.JSONException;
import org.json.JSONObject;

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

	private LinkedList<Gene> geneList;

	private ServerThread thread;
	private CachedQueueExecutor executor;

	private UDTServerSocket serverSocket;

	/** 构建指定名称的适配器。
	 */
	public RelationNucleusAdapter(String name) {
		this.name = name;
		this.executor = CachedQueueExecutor.newCachedQueueThreadPool(4);
		this.geneList = new LinkedList<Gene>();
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
	 * @param gene
	 */
	public void addGene(Gene gene) {
		synchronized (this.geneList) {
			this.geneList.add(gene);
		}
	}

	/**
	 * 
	 * @param gene
	 */
	public void removeGene(Gene gene) {
		synchronized (this.geneList) {
			this.geneList.remove(gene);
		}
	}

	protected Gene findGeneByHost(String host) {
		synchronized (this.geneList) {
			for (Gene gene : this.geneList) {
				if (gene.getHost().equals(host)) {
					return gene;
				}
			}
		}

		return null;
	}

	protected void transport(Gene destination, AdapterEvent event) {
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

		// 连接
		try {
			client.connect(destination.getHost(), destination.getPort());
		} catch (UnknownHostException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
		} catch (InterruptedException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
		} catch (IOException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
		}

		// 发送数据
		try {
			client.sendBlocking(this.parseEvent(event));
		} catch (IOException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
		} catch (InterruptedException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
		}

		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// 关闭
		try {
			client.shutdown();
		} catch (IOException e) {
			// Nothing
		}
	}

	/**
	 * 准备就绪。
	 */
	protected abstract void onReady();

	/**
	 * 收到事件。
	 * @param event
	 */
	protected abstract void onEvent(AdapterEvent event);

	/** 返回内核标签。 */
	public String getNucleusTag() {
		return Nucleus.getInstance().getTagAsString();
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

				// 回调事件
				AdapterEvent event = parseEvent(new String(packet.array(), "UTF-8"), this.socket);
				if (null != event) {
					onEvent(event);
				}
			} catch (IOException e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			}
		}
	}

	private AdapterEvent parseEvent(String data, UDTSocket socket) {
		String[] ret = data.split("\\\r\\\n");
		if (ret.length != 2) {
			Logger.w(this.getClass(), "Data format error");
			return null;
		}

		String host = socket.getSession().getDestination().getAddress().getHostAddress();
		// 通过主机地址找到 Gene
		Gene gene = findGeneByHost(host);
		if (null == gene) {
			Logger.w(this.getClass(), "Can not find gene with '" + host + "'");
			gene = new Gene(host);
			addGene(gene);
		}

		AdapterEvent event = null;
		try {
			event = new AdapterEvent(ret[0].trim(), new JSONObject(ret[1]), gene);
		} catch (JSONException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
		}
		return event;
	}

	private byte[] parseEvent(AdapterEvent event) {
		StringBuilder buf = new StringBuilder();
		buf.append(event.name);
		buf.append("\r\n");
		if (null != event.body) {
			buf.append(event.body.toString());
		}
		else {
			buf.append("{}");
		}

		byte[] ret = null;
		try {
			ret = buf.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Nothing
		}
		return ret;
	}
}
