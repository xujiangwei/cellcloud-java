/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (cellcloudproject@gmail.com)

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

package net.cellcloud.common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.cellcloud.core.LogLevel;
import net.cellcloud.core.Logger;

/** 非阻塞网络接收器。
 * 
 * @author Jiangwei Xu
 */
public class NonblockingAcceptor extends MessageService implements
		MessageAcceptor {

	// 缓存数据块大小
	protected static final int BLOCK = 8192;

	private ServerSocketChannel channel;
	private Selector selector;

	private InetSocketAddress bindAddress;
	private Thread handleThread;
	private boolean spinning;
	private boolean running;

	// 工作线程数组
	private NonblockingAcceptorWorker[] workers;
	private int workerNum;

	// 存储 Session 的 Map
	private ConcurrentHashMap<Integer, NonblockingAcceptorSession> sessions;

	public NonblockingAcceptor() {
		this.spinning = false;
		this.running = false;
		this.sessions = new ConcurrentHashMap<Integer, NonblockingAcceptorSession>();
		// 默认 4 线程
		this.workerNum = 4;
	}

	@Override
	public boolean bind(int port) {
		return bind(new InetSocketAddress("0.0.0.0", port));
	}

	@Override
	public boolean bind(InetSocketAddress address) {
		// 创建工作线程
		if (null == this.workers) {
			// 创建工作线程
			this.workers = new NonblockingAcceptorWorker[this.workerNum];
			for (int i = 0; i < this.workerNum; ++i) {
				this.workers[i] = new NonblockingAcceptorWorker(this);
			}
		}

		// 打开 Socket channel 并绑定服务
		try {
			this.channel = ServerSocketChannel.open();
			this.selector = Selector.open();

			this.channel.socket().bind(address);
			this.channel.configureBlocking(false);
			this.channel.register(this.selector, SelectionKey.OP_ACCEPT);

			this.bindAddress = address;

		} catch (IOException e) {
			Logger.logException(e, LogLevel.ERROR);

			// 返回失败
			return false;
		}

		// 创建句柄线程
		this.handleThread = new Thread() {
			@Override
			public void run() {
				running = true;
				spinning = true;

				// 启动工作线程
				for (int i = 0; i < workerNum; ++i) {
					if (!workers[i].isWorking())
						workers[i].start();
				}

				// 进入事件分发循环
				try {
					loopDispatch();
				} catch (IOException ioe) {
					Logger.logException(ioe, LogLevel.WARNING);
				} catch (Exception e) {
					Logger.logException(e, LogLevel.ERROR);
				}

				running = false;
			}
		};

		// 启动线程
		this.handleThread.setName("NonblockingAcceptor@" + this.bindAddress.getAddress().getHostAddress() + ":" + this.bindAddress.getPort());
		this.handleThread.start();

		return true;
	}

	@Override
	public void unbind() {
		// 退出事件循环
		this.spinning = false;

		Iterator<NonblockingAcceptorSession> iter = this.sessions.values().iterator();
		while (iter.hasNext()) {
			NonblockingAcceptorSession session = iter.next();
			this.close(session);
		}

		// 关闭 Channel
		try {
			this.channel.close();
			this.channel.socket().close();
		} catch (IOException e) {
			Logger.logException(e, LogLevel.DEBUG);
		}
		try {
			this.selector.close();
		} catch (IOException e) {
			Logger.logException(e, LogLevel.DEBUG);
		}

		// 关闭工作线程
		if (null != this.workers) {
			for (NonblockingAcceptorWorker worker : this.workers) {
				if (worker.isWorking()) {
					worker.stopSpinning(false);
				}
			}

			int stoppedCount = 0;
			while (stoppedCount != this.workerNum) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Logger.logException(e, LogLevel.DEBUG);
				}

				for (NonblockingAcceptorWorker worker : this.workers) {
					if (!worker.isWorking()) {
						++stoppedCount;
					}
				}
			}
		}

		// 控制主线程超时退出
		final int timeout = 500;
		int count = 0;

		// 等待线程结束
		if (null != this.handleThread) {
			while (this.running) {
				++count;
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					Logger.logException(e, LogLevel.DEBUG);
				}

				if (count >= timeout) {
					break;
				}
			}

			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Logger.logException(e, LogLevel.DEBUG);
			}

			if (count >= timeout) {
				try {
					this.handleThread.interrupt();
				} catch (Exception e) {
					Logger.logException(e, LogLevel.DEBUG);
				}
			}

			this.handleThread = null;
		}

		this.sessions.clear();
	}

	@Override
	public void close(Session session) {
		Iterator<NonblockingAcceptorSession> iter = this.sessions.values().iterator();
		while (iter.hasNext()) {
			NonblockingAcceptorSession nas = iter.next();
			if (nas.getId().longValue() == session.getId().longValue()) {
				try {
					nas.socket.close();
				} catch (IOException e) {
					Logger.logException(e, LogLevel.DEBUG);
				}
				break;
			}
		}
	}

	@Override
	public void write(Session session, Message message) {
		Iterator<NonblockingAcceptorSession> iter = this.sessions.values().iterator();
		while (iter.hasNext()) {
			NonblockingAcceptorSession nas = iter.next();
			if (nas.getId().longValue() == session.getId().longValue()) {
				nas.messages.add(message);
				break;
			}
		}
	}

	@Override
	public void read(Message message, Session session) {
		// Nothing
	}

	/** 设置工作器数量。
	 */
	public void setWorkerNum(int num) {
		this.workerNum = num;
	}
	/** 返回工作器数量。
	 */
	public int getWorkerNum() {
		return this.workerNum;
	}

	/** 返回所有 Session 。
	 */
	public Collection<NonblockingAcceptorSession> getSessions() {
		return this.sessions.values();
	}

	/** 从接收器里删除指定的 Session 。
	 */
	protected synchronized void eraseSession(NonblockingAcceptorSession session) {
		if (null == session.socket) {
			return;
		}

		boolean exist = false;

		Iterator<Map.Entry<Integer, NonblockingAcceptorSession>> iter = this.sessions.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<Integer, NonblockingAcceptorSession> entry = iter.next();
			Integer hashCode = entry.getKey();
			NonblockingAcceptorSession nas = entry.getValue();
			if (nas.getId().longValue() == session.getId().longValue()) {
				this.sessions.remove(hashCode);
				exist = true;
				break;
			}
		}

		if (exist) {
			this.fireSessionDestroyed(session);
			session.socket = null;
		}
	}

	/** 通知创建会话。 */
	protected void fireSessionCreated(Session session) {
		if (null != this.handler) {
			this.handler.sessionCreated(session);
		}
	}
	/** 通知打开会话。 */
	protected void fireSessionOpened(Session session) {
		if (null != this.handler) {
			this.handler.sessionOpened(session);
		}
	}
	/** 通知关闭会话。 */
	protected void fireSessionClosed(Session session) {
		if (null != this.handler) {
			this.handler.sessionClosed(session);
		}
	}
	/** 通知销毁会话。 */
	protected void fireSessionDestroyed(Session session) {
		if (null != this.handler) {
			this.handler.sessionDestroyed(session);
		}
	}
	/** 通知会话错误。 */
	protected void fireErrorOccurred(Session session, int errorCode) {
		if (null != this.handler) {
			this.handler.errorOccurred(errorCode, session);
		}
	}
	/** 通知会话接收到消息。 */
	protected void fireMessageReceived(Session session, Message message) {
		if (null != this.handler) {
			this.handler.messageReceived(session, message);
		}
	}
	/** 通知会话已发送消息。 */
	protected void fireMessageSent(Session session, Message message) {
		if (null != this.handler) {
			this.handler.messageSent(session, message);
		}
	}
	/** 通知消息拦截。 */
	protected boolean fireIntercepted(Session session, byte[] rawData) {
		if (null != this.interceptor) {
			return this.interceptor.intercepted(session, rawData);
		}
		else {
			return false;
		}
	}

	/** 事件循环。 */
	private void loopDispatch() throws IOException, Exception {
		while (this.spinning) {
			while (this.selector.isOpen() && this.selector.select() > 0) {
				Iterator<SelectionKey> it = this.selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey key = (SelectionKey) it.next();
					it.remove();

					if (key.isAcceptable()) {
						accept(key);
					}
					else if (key.isReadable()) {
						receive(key);
					}
					else if (key.isWritable()) {
						send(key);
					}
				}

				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					Logger.logException(e, LogLevel.DEBUG);
				}
			} // # while

			Thread.yield();
		} // # while
	}

	/** 处理 Accept */
	private void accept(SelectionKey key) {
		ServerSocketChannel channel = (ServerSocketChannel)key.channel();

		try {
			SocketChannel clientChannel = channel.accept();
			if (this.sessions.size() >= this.getMaxConnectNum()) {
				clientChannel.socket().close();
				clientChannel.close();
				return;
			}

			clientChannel.configureBlocking(false);
			clientChannel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

			// 创建 Session
			InetSocketAddress address = new InetSocketAddress(clientChannel.socket().getInetAddress().getHostAddress(),
					clientChannel.socket().getPort());
			NonblockingAcceptorSession session = new NonblockingAcceptorSession(this, address);
			// 设置 Socket
			session.socket = clientChannel.socket();

			this.sessions.put(clientChannel.socket().hashCode(), session);

			// 回调事件
			this.fireSessionCreated(session);

			// 回调事件
			this.fireSessionOpened(session);
		} catch (IOException e) {
			// Nothing
		} catch (Exception e) {
			// Nothing
		}
	}

	/** 处理 Read */
	private void receive(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel();

		if (!channel.isConnected()) {
			return;
		}

		NonblockingAcceptorSession session = this.sessions.get(channel.socket().hashCode());
		if (null == session) {
			Logger.w(NonblockingAcceptor.class, "Not found session");
			return;
		}

		// 选出 Worker 为 Session 服务
		this.selectWorkerForReceive(session, key);

		try {
			if (channel.isOpen())
				channel.register(this.selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
		} catch (IOException e) {
			Logger.logException(e, LogLevel.WARNING);
		}
	}

	/** 处理 Write */
	private void send(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel();

		if (!channel.isConnected()) {
			return;
		}

		NonblockingAcceptorSession session = this.sessions.get(channel.socket().hashCode());
		if (null == session) {
			Logger.w(NonblockingAcceptor.class, "Not found session");
			return;
		}

		// 选出 Worker 为 Session 服务
		this.selectWorkerForSend(session, key);

		try {
			if (channel.isOpen())
				channel.register(this.selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
		} catch (IOException e) {
			Logger.logException(e, LogLevel.WARNING);
		}
	}

	/** 选择最优的工作线程进行数据接收。
	 */
	private void selectWorkerForReceive(NonblockingAcceptorSession session, SelectionKey key) {
		NonblockingAcceptorWorker worker = null;

		int min = Integer.MAX_VALUE;
		for (NonblockingAcceptorWorker w : this.workers) {
			if (w.getReceiveSessionNum() < min) {
				worker = w;
				min = w.getReceiveSessionNum();
			}
		}

		worker.addReceiveSession(session, key);
	}

	/** 选择最优的工作线程进行数据发送。
	 */
	private void selectWorkerForSend(NonblockingAcceptorSession session, SelectionKey key) {
		NonblockingAcceptorWorker worker = null;

		int min = Integer.MAX_VALUE;
		for (NonblockingAcceptorWorker w : this.workers) {
			if (w.getSendSessionNum() < min) {
				worker = w;
				min = w.getSendSessionNum();
			}
		}

		worker.addSendSession(session, key);
	}
}
