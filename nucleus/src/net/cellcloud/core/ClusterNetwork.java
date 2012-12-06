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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.MessageHandler;
import net.cellcloud.common.NonblockingAcceptor;
import net.cellcloud.common.NonblockingConnector;
import net.cellcloud.common.Service;
import net.cellcloud.common.Session;

/** 集群网络。
 * 
 * @author Jiangwei Xu
 */
public final class ClusterNetwork implements Service, MessageHandler {

	protected final static int FIRST_PORT = 11099;

	private int port = -1;
	private NonblockingAcceptor acceptor;

	private Vector<InetSocketAddress> seedAddressList;
	private ConcurrentHashMap<Integer, NonblockingConnector> connectors;

	/** 构造函数。
	 */
	public ClusterNetwork() {
		this.seedAddressList = new Vector<InetSocketAddress>();
		this.connectors = new ConcurrentHashMap<Integer, NonblockingConnector>();
	}

	@Override
	public boolean startup() {
		if (this.port > 0) {
			return true;
		}

		// 检测可用的端口号
		this.port = this.detectUsablePort(FIRST_PORT);

		// 启动接收器
		this.acceptor = new NonblockingAcceptor();
		this.acceptor.setHandler(this);
		this.acceptor.setMaxConnectNum(1024);
		this.acceptor.setWorkerNum(2);
		if (!this.acceptor.bind(this.port)) {
			Logger.e(this.getClass(), "Cluster network can not bind socket on " + this.port);
			return false;
		}

		return true;
	}

	@Override
	public void shutdown() {
		if (null != this.acceptor) {
			this.acceptor.unbind();
			this.acceptor = null;
		}

		this.port = -1;
	}

	/** 返回监听端口。
	 */
	public int getPort() {
		return this.port;
	}

	/** 阻塞模式检测可用的端口号。
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

	/** 扫描网络。
	 */
	protected void scanNetwork() {
		for (InetSocketAddress address : this.seedAddressList) {
			Integer hashCode = address.hashCode();
			if (!this.connectors.containsKey(hashCode)) {
				// TODO 连接种子地址
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
	}

	@Override
	public void sessionClosed(Session session) {
	}

	@Override
	public void messageReceived(Session session, Message message) {
	}

	@Override
	public void messageSent(Session session, Message message) {
	}

	@Override
	public void errorOccurred(int errorCode, Session session) {
	}
}
