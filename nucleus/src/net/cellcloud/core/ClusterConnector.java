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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Queue;

import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.MessageErrorCode;
import net.cellcloud.common.MessageHandler;
import net.cellcloud.common.NonblockingConnector;
import net.cellcloud.common.Session;
import net.cellcloud.util.Util;

/** 集群连接器。
 * 
 * @author Jiangwei Xu
 */
public final class ClusterConnector extends Observable implements MessageHandler, Comparable<ClusterConnector> {

	protected final static String SUBJECT_FAILURE = "failure";
	protected final static String SUBJECT_DISCOVERING = "discovering";

	private final int bufferSize = 8192;

	private long hash;
	private InetSocketAddress address;

	private NonblockingConnector connector;
	private ByteBuffer buffer;
	private Queue<ClusterProtocol> protocolQueue;

	public ClusterConnector(InetSocketAddress address, long hash) {
		this.address = address;
		this.hash = hash;
		this.connector = new NonblockingConnector();
		this.buffer = ByteBuffer.allocate(this.bufferSize);
		this.connector.setHandler(this);
		this.protocolQueue = new LinkedList<ClusterProtocol>();
	}

	/** 返回连接器散列码。
	 */
	public long getHashCode() {
		return this.hash;
	}

	/** 返回连接器地址。
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/** 关闭连接。
	 */
	public void close() {
		this.connector.disconnect();
	}

	/** 执行发现。
	 */
	public boolean discover(int selfPort) {
		if (this.connector.isConnected()) {
			ClusterDiscoveringProtocol protocol = new ClusterDiscoveringProtocol(selfPort);
			protocol.launch(this.connector.getSession());
			return true;
		}
		else {
			if (this.connector.connect(this.address)) {
				ClusterDiscoveringProtocol protocol = new ClusterDiscoveringProtocol(selfPort);
				this.protocolQueue.offer(protocol);
				return true;
			}
		}

		return false;
	}

	@Override
	public int compareTo(ClusterConnector other) {
		return (int)(this.hash - other.hash);
	}

	@Override
	public int hashCode() {
		return (int)this.hash;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ClusterConnector) {
			return this.hash == ((ClusterConnector)other).hash;
		}
		else {
			return false;
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
			// 清空待执行协议
			this.protocolQueue.clear();

			ConnectorFailure failure = new ConnectorFailure(errorCode);
			this.distribute(failure);
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
		String str = Util.bytes2String(bytes);
		String[] array = str.split("\\\n");
		HashMap<String, String> prop = new HashMap<String, String>();
		for (int i = 0, size = array.length; i < size; ++i) {
			String[] p = array[i].split(":");
			if (p.length < 2) {
				continue;
			}
			prop.put(p[0].trim(), p[1].trim());
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

	/** 连接器故障。
	 */
	protected final class ConnectorFailure extends ClusterProtocol {

		protected int errorCode;

		protected ConnectorFailure(int errorCode) {
			super("ConnectorFailure");
			this.errorCode = errorCode;
		}

		@Override
		public void launch(Session session) {
		}

		@Override
		public void stack(Session session) {
		}

		@Override
		public void stackReject(Session session) {
		}
	}
}
