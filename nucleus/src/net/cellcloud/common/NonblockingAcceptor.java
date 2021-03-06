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

package net.cellcloud.common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 非阻塞网络接收器。
 * 
 * @author Ambrose Xu
 * 
 */
public class NonblockingAcceptor extends MessageService implements MessageAcceptor {

	/** 缓存数据块大小。 */
	private int block = 65536;
	/** 单次写数据块大小限制。 */
	private int writeLimit = 32768;

	/** Socket 的 backlog 。 */
	private int backlog = 100000;

	/** 服务器 NIO socket channel */
	private ServerSocketChannel channel;
	/** NIO selector */
	private Selector selector;

	/** 接收器的绑定地址。 */
	private InetSocketAddress bindAddress;

	/** 事务处理线程。 */
	private Thread handleThread;
	/** 线程是否自旋。 */
	private boolean spinning;
	/** 线程是否正在运行。 */
	private boolean running;

	/** 工作器线程数组。 */
	private NonblockingAcceptorWorker[] workers;
	/** 工作器数量。 */
	private int workerNum = 8;

	/** 任务池执行器。 */
	private ScheduledExecutorService scheduledExecutor;

	/** Socket 映射 Session，Key: Socket hash code ，Value: Session */
	private ConcurrentHashMap<Integer, NonblockingAcceptorSession> socketSessionMap;
	/** Session Id 映射 Session，Key: Session Id ，Value: Session */
	private ConcurrentHashMap<Long, NonblockingAcceptorSession> idSessionMap;

	/**
	 * 构造函数。
	 */
	public NonblockingAcceptor() {
		this.spinning = false;
		this.running = false;
		this.socketSessionMap = new ConcurrentHashMap<Integer, NonblockingAcceptorSession>();
		this.idSessionMap = new ConcurrentHashMap<Long, NonblockingAcceptorSession>();
		// 默认 8 线程
		this.workerNum = 8;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean bind(int port) {
		return bind(new InetSocketAddress("0.0.0.0", port));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean bind(final InetSocketAddress address) {
		// 创建任务池
		if (null == this.scheduledExecutor) {
			this.scheduledExecutor = Executors.newScheduledThreadPool(this.workerNum);
		}

		// 创建工作线程
		if (null == this.workers) {
			// 创建工作线程
			this.workers = new NonblockingAcceptorWorker[this.workerNum];
			for (int i = 0; i < this.workerNum; ++i) {
				this.workers[i] = new NonblockingAcceptorWorker(this, this.scheduledExecutor);
			}
		}

		// 打开 Socket channel 并绑定服务
		SelectionKey skey = null;
		try {
			this.selector = Selector.open();
			this.channel = ServerSocketChannel.open();
			this.channel.configureBlocking(false);
			this.channel.socket().setReceiveBufferSize(this.block * 4);
			this.channel.socket().bind(address, this.backlog);

			skey = this.channel.register(this.selector, SelectionKey.OP_ACCEPT);

			this.bindAddress = address;
		} catch (IOException e) {
			Logger.log(NonblockingAcceptor.class, e, LogLevel.ERROR);
		} finally {
			if (null == skey && null != this.selector) {
				try {
					this.selector.close();
				} catch (IOException e) {
					// Nothing
				}
			}
			if (null == skey && null != this.channel) {
				try {
					this.channel.close();
				} catch (IOException e) {
					// Nothing
				}
			}
		}

		if (null == this.bindAddress) {
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
					if (!workers[i].isWorking()) {
						workers[i].start();
					}
				}

				// 进入事件分发循环
				try {
					loopDispatch();
				} catch (IOException ioe) {
					Logger.log(NonblockingAcceptor.class, ioe, LogLevel.WARNING);
				} catch (CancelledKeyException e) {
					if (spinning)
						Logger.log(NonblockingAcceptor.class, e, LogLevel.ERROR);
					else
						Logger.log(NonblockingAcceptor.class, e, LogLevel.DEBUG);
				} catch (Exception e) {
					Logger.log(NonblockingAcceptor.class, e, LogLevel.ERROR);
					fireErrorOccurred(null, MessageErrorCode.ACCEPT_FAILED);
				}

				running = false;
			}
		};

		// 启动线程
		this.handleThread.setName("NonblockingAcceptor@" + this.bindAddress.getAddress().getHostAddress() + ":" + this.bindAddress.getPort());
		this.handleThread.start();

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void unbind() {
		// 退出事件循环
		this.spinning = false;

		ArrayList<NonblockingAcceptorSession> sessionList = new ArrayList<NonblockingAcceptorSession>(this.socketSessionMap.size());
		sessionList.addAll(this.socketSessionMap.values());
		for (NonblockingAcceptorSession session : sessionList) {
			this.close(session);
		}

		// 关闭 Channel
		try {
			this.channel.close();
			this.channel.socket().close();
		} catch (IOException e) {
			Logger.log(NonblockingAcceptor.class, e, LogLevel.DEBUG);
		}
		try {
			this.selector.close();
		} catch (IOException e) {
			Logger.log(NonblockingAcceptor.class, e, LogLevel.DEBUG);
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
					Thread.sleep(100L);
				} catch (InterruptedException e) {
					Logger.log(NonblockingAcceptor.class, e, LogLevel.DEBUG);
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
					Thread.sleep(10L);
				} catch (InterruptedException e) {
					Logger.log(NonblockingAcceptor.class, e, LogLevel.DEBUG);
				}

				if (count >= timeout) {
					break;
				}
			}

			try {
				Thread.sleep(10L);
			} catch (InterruptedException e) {
				Logger.log(NonblockingAcceptor.class, e, LogLevel.DEBUG);
			}

			if (count >= timeout) {
				try {
					this.handleThread.interrupt();
				} catch (Exception e) {
					Logger.log(NonblockingAcceptor.class, e, LogLevel.DEBUG);
				}
			}

			this.handleThread = null;
		}

		this.socketSessionMap.clear();
		this.idSessionMap.clear();

		if (null != this.scheduledExecutor) {
			this.scheduledExecutor.shutdown();
			this.scheduledExecutor = null;
		}

		this.bindAddress = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close(Session session) {
		NonblockingAcceptorSession nas = this.idSessionMap.get(session.getId());
		if (null != nas) {
			try {
				if (null != nas.socket) {
					nas.socket.close();
				}
			} catch (IOException e) {
				Logger.log(NonblockingAcceptor.class, e, LogLevel.DEBUG);
			}

			// Erase session
			this.eraseSession(nas);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Session getSession(Long sessionId) {
		return this.idSessionMap.get(sessionId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(Session session, Message message) throws IOException {
		if (message.length() > this.writeLimit) {
			this.fireErrorOccurred(session, MessageErrorCode.WRITE_OUTOFBOUNDS);
			return;
		}

		NonblockingAcceptorSession nas = this.idSessionMap.get(session.getId());
		if (null != nas) {
			nas.putMessage(message);
		}
		else {
			this.fireErrorOccurred(session, MessageErrorCode.WRITE_FAILED);
			throw new IOException("Can NOT find session: " + session.getId());
		}
	}

	/**
	 * 判断指定的会话是否已经连接到接收器。
	 * 
	 * @param session 指定待判断的会话。
	 * @return 返回指定会话是否存在。
	 */
	public boolean hasSession(Session session) {
		return this.idSessionMap.contains(session.getId());
	}

	/**
	 * 接收器句柄线程是否正在运行。
	 * 
	 * @return 如果线程正在运行返回 <code>true</code> 。
	 */
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * 获得接收器绑定地址。
	 * 
	 * @return 返回接收器绑定地址。
	 */
	public final InetSocketAddress getBindAddress() {
		return this.bindAddress;
	}

	/**
	 * 设置工作器数量。
	 * 
	 * @param num 指定工作器数量。
	 */
	public void setWorkerNum(int num) {
		if (null == this.workers) {
			this.workerNum = num;
		}
		else {
			Logger.e(NonblockingAcceptor.class, "Can NOT set worker number");
		}
	}

	/**
	 * 获得工作器数量。
	 * 
	 * @return 返回工作器数量。
	 */
	public int getWorkerNum() {
		return this.workerNum;
	}

	/**
	 * 设置缓存数据块大小。
	 * 
	 * @param size 数据块大小。
	 */
	public void setBlockSize(int size) {
		if (size < 2048) {
			return;
		}

		if (this.block == size) {
			return;
		}

		this.block = size;
		this.writeLimit = Math.round(size * 0.5f);
	}

	/**
	 * 获得缓存数据块大小。
	 * 
	 * @return 返回缓存数据块大小。
	 */
	public int getBlockSize() {
		return this.block;
	}

	/**
	 * 获得存储了所有会话的集合。
	 * 
	 * @return 返回接收器里的所有会话。
	 */
	public Collection<NonblockingAcceptorSession> getSessions() {
		return this.socketSessionMap.values();
	}

	/**
	 * 获得所有会话数量。
	 * 
	 * @return 返回所有会话数量。
	 */
	public int numSessions() {
		return this.socketSessionMap.size();
	}

	/**
	 * 设置每一个会话读取数据的间隔。
	 * 
	 * @param intervalInMillisecond 指定以毫秒为单位的间隔。
	 */
	public void setEachSessionReadInterval(long intervalInMillisecond) {
		for (NonblockingAcceptorWorker worker : this.workers) {
			worker.setEachSessionReadInterval(intervalInMillisecond);
		}
	}

	/**
	 * 获得每一个会话读取数据的间隔。
	 * 
	 * @return 返回以毫秒为单位的间隔。
	 */
	public long getEachSessionReadInterval() {
		if (null == this.workers) {
			return -1;
		}

		return this.workers[0].getEachSessionReadInterval();
	}

	/**
	 * 设置每一个会话写入数据的间隔。
	 * 
	 * @param intervalInMillisecond 指定以毫秒为单位的间隔。
	 */
	public void setEachSessionWriteInterval(long intervalInMillisecond) {
		for (NonblockingAcceptorWorker worker : this.workers) {
			worker.setEachSessionWriteInterval(intervalInMillisecond);
		}
	}

	/**
	 * 获得每一个会话写入数据的间隔。
	 * 
	 * @return 返回以毫秒为单位的间隔。
	 */
	public long getEachSessionWriteInterval() {
		if (null == this.workers) {
			return -1;
		}

		return this.workers[0].getEachSessionWriteInterval();
	}

	/**
	 * 设置数据传输的配额，单位：字节每秒（BPS）。
	 * 
	 * @param quotaInBytesPerSecond 指定以字节每秒为单位的传输带宽。
	 */
	public void setTransmissionQuota(int quotaInBytesPerSecond) {
		for (NonblockingAcceptorWorker worker : this.workers) {
			worker.getQuotaCalculator().setQuota(quotaInBytesPerSecond);
		}
	}

	/**
	 * 获得数据传输配额。
	 * 
	 * @return 返回以字节每秒为单位的传输带宽。
	 */
	public int getTransmissionQuota() {
		if (null == this.workers) {
			return -1;
		}

		return this.workers[0].getQuotaCalculator().getQuota();
	}

	/**
	 * 获得发送数据总流量。
	 * 
	 * @return 返回发送数据总流量。
	 */
	public long getTotalTx() {
		long total = 0;
		for (NonblockingAcceptorWorker worker : this.workers) {
			total += worker.getTX();
		}
		return total;
	}

	/**
	 * 获得接收数据总流量。
	 * 
	 * @return 返回接收数据总流量。
	 */
	public long getTotalRx() {
		long total = 0;
		for (NonblockingAcceptorWorker worker : this.workers) {
			total += worker.getRX();
		}
		return total;
	}

	/**
	 * 获得各工作器的数据发送流量。
	 * 
	 * @return 返回存储了各个工作器数据发送流量的数组。
	 */
	public long[] getWorkersTx() {
		long[] ret = new long[this.workerNum];
		for (int i = 0; i < this.workerNum; ++i) {
			ret[i] = this.workers[i].getTX();
		}
		return ret;
	}

	/**
	 * 获得各工作器的数据接收流量。
	 * 
	 * @return 返回存储了各个工作器数据接收流量的数组。
	 */
	public long[] getWorkersRx() {
		long[] ret = new long[this.workerNum];
		for (int i = 0; i < this.workerNum; ++i) {
			ret[i] = this.workers[i].getRX();
		}
		return ret;
	}

	/**
	 * 从接收器里删除指定的 Session 。
	 * 
	 * @param session 指定待删除的 Session 对象。
	 */
	protected void eraseSession(NonblockingAcceptorSession session) {
		boolean exist = false;

		if (null != this.idSessionMap.remove(session.getId())) {
			exist = true;
		}

		if (null != session.socket) {
			this.socketSessionMap.remove(session.socket.hashCode());
		}

		if (exist) {
			this.fireSessionDestroyed(session);
			session.socket = null;

			Iterator<Map.Entry<Integer, NonblockingAcceptorSession>> iter = this.socketSessionMap.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<Integer, NonblockingAcceptorSession> e = iter.next();
				if (e.getValue().getId().equals(session.getId())) {
					iter.remove();
					break;
				}
			}
		}
	}

	/**
	 * 通知创建会话。
	 * 
	 * @param session 指定会话。
	 */
	protected void fireSessionCreated(Session session) {
		if (null != this.interceptor && this.interceptor.interceptCreating(session)) {
			return;
		}

		if (null != this.handler) {
			this.handler.sessionCreated(session);
		}
	}

	/**
	 * 通知启用会话。
	 * 
	 * @param session 指定会话。
	 */
	protected void fireSessionOpened(Session session) {
		if (null != this.interceptor && this.interceptor.interceptOpening(session)) {
			return;
		}

		if (null != this.handler) {
			this.handler.sessionOpened(session);
		}
	}

	/**
	 * 通知停用会话。
	 * 
	 * @param session 指定会话。
	 */
	protected void fireSessionClosed(Session session) {
		if (null != this.interceptor && this.interceptor.interceptClosing(session)) {
			return;
		}

		if (null != this.handler) {
			this.handler.sessionClosed(session);
		}
	}

	/**
	 * 通知销毁会话。
	 * 
	 * @param session 指定会话。
	 */
	protected void fireSessionDestroyed(Session session) {
		if (null != this.interceptor && this.interceptor.interceptDestroying(session)) {
			return;
		}

		if (null != this.handler) {
			this.handler.sessionDestroyed(session);
		}
	}

	/**
	 * 通知会话发生错误。
	 * 
	 * @param session 指定会话。
	 * @param errorCode 指定错误码。
	 */
	protected void fireErrorOccurred(Session session, int errorCode) {
		if (null != this.interceptor && this.interceptor.interceptError(session, errorCode)) {
			return;
		}

		if (null != this.handler) {
			this.handler.errorOccurred(errorCode, session);
		}
	}

	/**
	 * 通知会话接收到消息。
	 * 
	 * @param session 指定会话。
	 * @param message 指定消息。
	 */
	protected void fireMessageReceived(Session session, Message message) {
		if (null != this.interceptor && this.interceptor.interceptMessage(session, message)) {
			return;
		}

		if (null != this.handler) {
			this.handler.messageReceived(session, message);
		}
	}

	/**
	 * 通知会话已发出消息。
	 * 
	 * @param session 指定会话。
	 * @param message 指定消息。
	 */
	protected void fireMessageSent(NonblockingAcceptorSession session, final Message message) {
		if (null != this.handler) {
			this.handler.messageSent(session, message);
		}

		MessageTrigger trigger = message.sentTrigger;
		if (null != trigger) {
			trigger.trigger(session);
			message.sentTrigger = null;
		}
	}

	/**
	 * 事件循环。
	 * 
	 * @throws IOException
	 * @throws Exception
	 */
	private void loopDispatch() throws IOException, Exception {
		while (this.spinning) {
			if (!this.selector.isOpen()) {
				try {
					Thread.sleep(1L);
				} catch (InterruptedException e) {
					Logger.log(NonblockingAcceptor.class, e, LogLevel.DEBUG);
				}
				continue;
			}

			if (this.selector.select() > 0) {
				Iterator<SelectionKey> it = this.selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey key = (SelectionKey) it.next();
					it.remove();

					try {
						if (key.isAcceptable()) {
							accept(key);
						}
					}
					catch (Exception e) {
						// 取消
						key.cancel();

						if (this.spinning) {
							// 没有被主动终止循环
							continue;
						}
						else {
							throw e;
						}
					}

					try {
						if (key.isValid() && key.isReadable()) {
							receive(key);
						}
					}
					catch (Exception e) {
						if (this.spinning) {
							// 没有被主动终止循环
							continue;
						}
						else {
							throw e;
						}
					}

					try {
						if (key.isValid() && key.isWritable()) {
							send(key);
						}
					}
					catch (Exception e) {
						if (this.spinning) {
							// 没有被主动终止循环
							continue;
						}
						else {
							throw e;
						}
					}
				} // # while
			} // # if

			try {
				Thread.sleep(1L);
			} catch (InterruptedException e) {
				// Nothing
			}

			Thread.yield();
		} // # while
	}

	/**
	 * 处理 Accept 动作。
	 * 
	 * @param key
	 */
	private void accept(SelectionKey key) {
		ServerSocketChannel channel = (ServerSocketChannel) key.channel();

		Long sessionId = null;
		Integer socket = null;
		boolean error = false;

		try {
			// accept
			SocketChannel clientChannel = channel.accept();
			if (this.socketSessionMap.size() >= this.getMaxConnectNum()) {
				// 达到最大连接数
				clientChannel.socket().close();
				clientChannel.close();
				return;
			}

			clientChannel.socket().setReceiveBufferSize(this.block);
			clientChannel.socket().setSendBufferSize(this.block);
			clientChannel.configureBlocking(false);
			clientChannel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

			// 创建 Session
			InetSocketAddress address = new InetSocketAddress(clientChannel.socket().getInetAddress().getHostAddress(),
					clientChannel.socket().getPort());
			NonblockingAcceptorSession session = new NonblockingAcceptorSession(this, address, this.block);
			// 设置 Socket
			session.socket = clientChannel.socket();

			// 为 Session 选择工作线程
			session.worker = this.chooseWorker(session);

			// session id
			sessionId = session.getId();
			// socket
			socket = session.socket.hashCode();

			// 记录
			this.socketSessionMap.put(socket, session);
			this.idSessionMap.put(sessionId, session);

			// 回调事件
			this.fireSessionCreated(session);

			// 回调事件
			this.fireSessionOpened(session);
		} catch (IOException e) {
			error = true;
			Logger.log(this.getClass(), e, LogLevel.ERROR);
		} catch (Exception e) {
			error = true;
			Logger.log(this.getClass(), e, LogLevel.ERROR);
		}

		if (error) {
			if (null != socket) {
				this.socketSessionMap.remove(socket);
			}
			if (null != sessionId) {
				this.idSessionMap.remove(sessionId);
			}
		}
	}

	/**
	 * 处理 Read 动作。
	 * 
	 * @param key
	 */
	private void receive(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel();

		if (!channel.isConnected()) {
			return;
		}

		NonblockingAcceptorSession session = this.socketSessionMap.get(channel.socket().hashCode());
		if (null == session) {
			try {
				Logger.d(NonblockingAcceptor.class, "Not found session: " + channel.socket().getInetAddress().getHostAddress());
			} catch (Exception e) {
				// Nothing
			}
			return;
		}

		// 推入 Worker
		session.selectionKey = key;
		session.worker.pushReceiveSession(session);

		if (key.isValid()) {
			key.interestOps(key.interestOps() | SelectionKey.OP_READ);
		}
	}

	/**
	 * 处理 Write 动作。
	 * 
	 * @param key
	 */
	private void send(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel();

		if (!channel.isConnected()) {
			return;
		}

		NonblockingAcceptorSession session = this.socketSessionMap.get(channel.socket().hashCode());
		if (null == session) {
			try {
				Logger.w(NonblockingAcceptor.class, "Not found session: " + channel.socket().getInetAddress().getHostAddress());
			} catch (Exception e) {
				// Nothing
			}
			return;
		}

		// 推入 Worker
		session.selectionKey = key;
		session.worker.pushSendSession(session);

		if (key.isValid()) {
			key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
		}
	}

	/**
	 * 为指定的会话选择工作线程。
	 * 
	 * @param session
	 * @return
	 */
	private NonblockingAcceptorWorker chooseWorker(NonblockingAcceptorSession session) {
		/* 下面的算法基于 mod
		int index = (int)(session.getId().longValue() % this.workerNum);
		return this.workers[index];
		*/

		// 选择累计上行流量最小的 Worker

		NonblockingAcceptorWorker worker = this.workers[0];
		long minTx = Long.MAX_VALUE;
		long tx = 0;

		for (int i = 0; i < this.workerNum; ++i) {
			NonblockingAcceptorWorker naw = this.workers[i];
			tx = naw.getTX();
			if (tx < minTx) {
				minTx = tx;
				worker = naw;
			}
		}

		return worker;
	}

}
