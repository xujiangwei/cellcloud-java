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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据报协议接收器。
 * 
 * @author Ambrose Xu
 * 
 */
public class DatagramAcceptor extends MessageService implements MessageAcceptor {

	/** Socket 绑定地址。 */
	private InetSocketAddress socketAddress = null;

	/** 会话超期时间（毫秒），默认 10 分钟。 */
	private long sessionExpired = 10L * 60L * 1000L;

	/** 超期检查定时器。 */
	private Timer expiredTimer;

	/** Socket 等待超时（毫秒），默认 30 秒。 */
	private int soTimeout = 30000;

	/** 读数据的缓存块大小（字节），默认 16 KB。 */
	private int block = 16 * 1024;

	/** UDP 绑定 Socket 。 */
	private DatagramSocket udpSocket;

	/** 数据分发处理线程。 */
	private LoopDispatchThread mainThread;

	/** 线程池执行器。 */
	private ExecutorService executor = null;
	/** 最大并发数据处理线程数，默认 4 。 */
	private int maxThreads = 4;
	/** 当前活跃线程数量。 */
	private AtomicInteger numThreads = new AtomicInteger(0);

	/** 会话 "IP:Port" 键对应的 Session 实例。 */
	private ConcurrentHashMap<String, DatagramAcceptorSession> sessionMap;
	/** 会话 ID 对应的 Sesson 实例。 */
	private ConcurrentHashMap<Long, DatagramAcceptorSession> idSessionMap;

	/** 写数据队列。 */
	private LinkedList<Message> udpWriteQueue;
	/** 写数据队列依次对应的 Session 队列。 */
	private LinkedList<DatagramAcceptorSession> udpWriteSessionQueue;
	/** 写数据间隔。 */
	private long writeInterval;
	/** 最近一次写数据时间戳。 */
	private AtomicLong lastWriting;

	/**
	 * 构造函数。
	 * 
	 * @param sessionExpired
	 */
	public DatagramAcceptor(long sessionExpired) {
		super();
		this.sessionExpired = sessionExpired;
		this.sessionMap = new ConcurrentHashMap<String, DatagramAcceptorSession>();
		this.idSessionMap = new ConcurrentHashMap<Long, DatagramAcceptorSession>();
		this.writeInterval = 20L;
		this.lastWriting = new AtomicLong(0L);
		this.setMaxConnectNum(5000);
	}

	public void setWriteInterval(long intervalInMillisecond) {
		this.writeInterval = intervalInMillisecond;
	}

	public void setSoTimeout(int timeoutInMillisecond) {
		this.soTimeout = timeoutInMillisecond;
	}

	public void setBlockSize(int size) {
		this.block = size;
	}

	public void setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
	}

	public InetSocketAddress getBindAddress() {
		return this.socketAddress;
	}

	@Override
	public boolean bind(int port) {
		InetSocketAddress address = new InetSocketAddress(port);
		return this.bind(address);
	}

	@Override
	public boolean bind(InetSocketAddress address) {
		if (null != this.udpSocket) {
			return false;
		}

		if (null == this.executor) {
			this.executor = Executors.newCachedThreadPool();
		}

		this.socketAddress = address;

		try {
			this.udpSocket = new DatagramSocket(this.socketAddress);
			this.udpSocket.setSoTimeout(this.soTimeout);
			this.udpSocket.setReceiveBufferSize(this.block + this.block);
			this.udpSocket.setSendBufferSize(this.block + this.block);
		} catch (SocketException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
			return false;
		} catch (Exception e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
			return false;
		}

		if (null == this.udpWriteQueue) {
			this.udpWriteQueue = new LinkedList<Message>();
		}
		if (null == this.udpWriteSessionQueue) {
			this.udpWriteSessionQueue = new LinkedList<DatagramAcceptorSession>();
		}

		this.mainThread = new LoopDispatchThread();
		this.mainThread.start();

		if (null == this.expiredTimer) {
			this.expiredTimer = new Timer();
			this.expiredTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					// 检查超期
					checkExpire();
				}
			}, 1000L, 10000L);
		}

		return true;
	}

	@Override
	public void unbind() {
		// 关闭线程
		if (null != this.mainThread) {
			this.mainThread.shutdown();
			this.mainThread = null;
		}

		if (null != this.expiredTimer) {
			this.expiredTimer.cancel();
			this.expiredTimer.purge();
			this.expiredTimer = null;
		}

		if (null != this.udpSocket) {
			this.udpSocket.close();
			this.udpSocket = null;
		}

		if (null != this.udpWriteQueue) {
			this.udpWriteQueue.clear();
		}
		if (null != this.udpWriteSessionQueue) {
			this.udpWriteSessionQueue.clear();
		}

		if (null != this.executor) {
			this.executor.shutdown();
			this.executor = null;
		}

		for (DatagramAcceptorSession session : this.sessionMap.values()) {
			if (!session.removed) {
				if (null != this.handler) {
					this.handler.sessionClosed(session);

					session.removed = true;

					this.handler.sessionDestroyed(session);
				}
			}
		}

		// 清空 Session
		this.sessionMap.clear();
		this.idSessionMap.clear();
	}

	@Override
	public void close(Session session) {
		if (!(session instanceof DatagramAcceptorSession)) {
			return;
		}

		DatagramAcceptorSession bas = (DatagramAcceptorSession) session;
		String key = bas.mapKey;

		if (this.sessionMap.containsKey(key)) {
			// 标记移除
			bas.removed = true;

			if (null != this.handler) {
				this.handler.sessionClosed(session);
			}

			// 移除
			this.sessionMap.remove(key);
			this.idSessionMap.remove(session.getId());

			// 删除所有待发送数据
			synchronized (this.udpWriteQueue) {
				int index = 0;
				Iterator<DatagramAcceptorSession> iter = this.udpWriteSessionQueue.iterator();
				while (iter.hasNext()) {
					DatagramAcceptorSession s = iter.next();
					if (s.equals(bas)) {
						this.udpWriteQueue.remove(index);
						iter.remove();
					}

					++index;
				}
			}

			if (null != this.handler) {
				this.handler.sessionDestroyed(session);
			}
		}
	}

	@Override
	public Session getSession(Long sessionId) {
		return this.idSessionMap.get(sessionId);
	}

	@Override
	public void write(Session session, Message message) {
		if (!(session instanceof DatagramAcceptorSession)) {
			return;
		}

		DatagramAcceptorSession bas = (DatagramAcceptorSession) session;
		if (bas.removed) {
			if (null != this.handler) {
				this.handler.errorOccurred(MessageErrorCode.STATE_ERROR, session);
			}
			return;
		}

		synchronized (this.udpWriteQueue) {
			this.udpWriteQueue.addLast(message);
			this.udpWriteSessionQueue.addLast(bas);
		}

		if (this.numThreads.get() >= this.maxThreads) {
			return;
		}

		this.executor.execute(new Runnable() {
			@Override
			public void run() {
				numThreads.incrementAndGet();

				while (null != udpSocket && !udpWriteQueue.isEmpty()) {
					if (writeInterval > 0L && lastWriting.get() > 0) {
						long d = System.currentTimeMillis() - lastWriting.get();
						if (d < writeInterval) {
							try {
								Thread.sleep(writeInterval - d);
							} catch (InterruptedException e) {
								// Nothing
							}
						}
					}

					Message umessage = null;
					DatagramAcceptorSession usession = null;
					synchronized (udpWriteQueue) {
						if (udpWriteQueue.isEmpty()) {
							break;
						}

						umessage = udpWriteQueue.removeFirst();
						usession = udpWriteSessionQueue.removeFirst();

						if (null != umessage) {
							try {
								DatagramPacket dp = new DatagramPacket(umessage.get(), umessage.length(), usession.getAddress());

								if (null == udpSocket) {
									continue;
								}

								udpSocket.send(dp);

								// 更新活跃时间
								usession.activeTimestamp = System.currentTimeMillis();

								lastWriting.set(usession.activeTimestamp);

								if (null != handler) {
									handler.messageSent(usession, umessage);
								}
							} catch (SocketException e) {
								Logger.log(this.getClass(), e, LogLevel.ERROR);
								if (null != handler) {
									handler.errorOccurred(MessageErrorCode.SOCKET_FAILED, usession);
								}
							} catch (IOException e) {
								Logger.log(this.getClass(), e, LogLevel.ERROR);
								if (null != handler) {
									handler.errorOccurred(MessageErrorCode.WRITE_FAILED, usession);
								}
							}
						}
					}
				} //#while

				numThreads.decrementAndGet();
			}
		});
	}


	private synchronized DatagramAcceptorSession updateSession(String ip, int port) {
		// 计算会话 Key
		String sessionKey = this.calcSessionKey(ip, port);

		DatagramAcceptorSession session = this.sessionMap.get(sessionKey);

		if (null == session) {
			InetSocketAddress isa = new InetSocketAddress(ip, port);

			session = new DatagramAcceptorSession(this, isa);
			session.mapKey = sessionKey;

			if (null != this.handler) {
				this.handler.sessionCreated(session);
			}

			this.sessionMap.put(sessionKey, session);
			this.idSessionMap.put(session.getId(), session);

			if (null != this.handler) {
				this.handler.sessionOpened(session);
			}
		}

		return session;
	}

	private String calcSessionKey(String ip, int port) {
		StringBuilder buf = new StringBuilder(ip);
		buf.append(":");
		buf.append(port);

		String ret = buf.toString();
		buf = null;
		return ret;
	}

	private void checkExpire() {
		// 当前时间
		long time = System.currentTimeMillis();

		// 清理清单
		ArrayList<DatagramAcceptorSession> removedList = new ArrayList<DatagramAcceptorSession>();

		Iterator<DatagramAcceptorSession> iter = this.sessionMap.values().iterator();
		while (iter.hasNext()) {
			DatagramAcceptorSession session = iter.next();
			if (time - session.activeTimestamp > this.sessionExpired) {
				removedList.add(session);
			}
		}

		if (!removedList.isEmpty()) {
			Logger.d(DatagramAcceptor.class, "Remove expired session : " + this.sessionMap.size()
					+ " -> " + (this.sessionMap.size() - removedList.size()));

			for (DatagramAcceptorSession session : removedList) {
				close(session);
			}
			removedList.clear();
		}
		removedList = null;
	}


	protected class LoopDispatchThread extends Thread {

		private boolean spinning = false;
		private boolean running = false;

		protected LoopDispatchThread() {
			super("DatagramAcceptor@LoopDispatchThread");
		}

		public void shutdown() {
			this.spinning = false;
		}

		public boolean isRunning() {
			return this.running;
		}

		@Override
		public void run() {
			Logger.d(this.getClass(), "LoopDispatch#start: " + socketAddress.getPort());

			this.spinning = true;
			this.running = true;

			while (null != udpSocket && this.spinning) {
				try {
					Thread.sleep(1L);
				} catch (InterruptedException e) {
					Logger.log(this.getClass(), e, LogLevel.DEBUG);
				}

				byte[] buf = new byte[block];
				DatagramPacket packet = new DatagramPacket(buf, block);

				try {
					// 接收数据
					if (!udpSocket.isClosed()) {
						udpSocket.receive(packet);
					}
					else {
						break;
					}

					// 处理接收
					this.receive(packet);

				} catch (SocketTimeoutException e) {
					// Nothing
					continue;
				} catch (SocketException e) {
					Logger.log(this.getClass(), e, LogLevel.DEBUG);
					continue;
				} catch (IOException e) {
					Logger.log(this.getClass(), e, LogLevel.WARNING);
					continue;
				} catch (NullPointerException e) {
					// Nothing
					continue;
				}
			}

			this.running = false;
			Logger.d(this.getClass(), "LoopDispatch#stop");
		}

		private void receive(final DatagramPacket packet) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					numThreads.incrementAndGet();

					InetAddress addr = packet.getAddress();
					String ip = addr.getHostAddress();
					int port = packet.getPort();
					byte[] data = new byte[packet.getLength()];
					System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());

					// 匹配 Session
					DatagramAcceptorSession session = updateSession(ip, port);
					Message message = new Message(data);

					// 更新活跃时间
					session.activeTimestamp = System.currentTimeMillis();

					if (null != handler) {
						handler.messageReceived(session, message);
					}

					numThreads.decrementAndGet();
				}
			});
		}
	}

}
