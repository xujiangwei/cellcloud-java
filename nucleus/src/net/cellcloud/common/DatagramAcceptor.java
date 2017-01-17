/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2016 Cell Cloud Team (www.cellcloud.net)

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import net.cellcloud.util.CachedQueueExecutor;

/**
 * 数据报适配器。
 * 
 * @author Ambrose Xu
 */
public class DatagramAcceptor extends MessageService implements MessageAcceptor {

	private InetSocketAddress socketAddress = null;

	// 会话超期时间，单位：毫秒
	private long sessionExpire = 10L * 60L * 1000L;

	// 30 秒
	private int soTimeout = 30000;

	// 块大小
	private int block = 16 * 1024;

	private DatagramSocket udpSocket;

	// 数据分发线程
	private LoopDispatchThread mainThread;

	private ExecutorService executor = null;
	private int maxThreads = 4;

	// Key: IP:Port
	private ConcurrentHashMap<String, DatagramAcceptorSession> sessionMap;
	private ConcurrentHashMap<Long, DatagramAcceptorSession> idSessionMap;

	private LinkedList<Message> udpWriteQueue;
	private LinkedList<DatagramAcceptorSession> udpWriteSessionQueue;
	private AtomicBoolean writeRunning;

	public DatagramAcceptor(long sessionExpire) {
		super();
		this.sessionExpire = sessionExpire;
		this.sessionMap = new ConcurrentHashMap<String, DatagramAcceptorSession>();
		this.idSessionMap = new ConcurrentHashMap<Long, DatagramAcceptorSession>();
		this.writeRunning = new AtomicBoolean(false);
		this.setMaxConnectNum(5000);
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
			this.executor = CachedQueueExecutor.newCachedQueueThreadPool(this.maxThreads);
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

		return true;
	}

	@Override
	public void unbind() {
		// 关闭线程
		if (null != this.mainThread) {
			this.mainThread.shutdown();
			this.mainThread = null;
		}

		if (null != this.udpSocket) {
			this.udpSocket.close();
			this.udpSocket = null;
		}

		if (null != this.udpWriteQueue) {
			this.udpWriteQueue.clear();
			this.udpWriteQueue = null;
		}
		if (null != this.udpWriteSessionQueue) {
			this.udpWriteSessionQueue.clear();
			this.udpWriteSessionQueue = null;
		}

		if (null != this.executor) {
			this.executor.shutdown();
			this.executor = null;
		}

		this.writeRunning.set(false);

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
		String key = this.calcSessionKey(bas.getAddress().getHostString(), bas.getAddress().getPort());

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

		if (!this.writeRunning.get()) {
			this.writeRunning.set(true);

			this.executor.execute(new Runnable() {
				@Override
				public void run() {
					while (null != udpSocket && !udpWriteQueue.isEmpty()) {
						Message umessage = null;
						DatagramAcceptorSession usession = null;
						synchronized (udpWriteQueue) {
							umessage = udpWriteQueue.removeFirst();
							usession = udpWriteSessionQueue.removeFirst();
						}

						if (null != umessage) {
							try {
								DatagramPacket dp = new DatagramPacket(umessage.get(), umessage.length(), usession.getAddress());

								if (null == udpSocket) {
									continue;
								}

								udpSocket.send(dp);

								// 更新活跃时间
								usession.activeTimestamp = System.currentTimeMillis();

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
					} //#while

					// 运行结束
					writeRunning.set(false);
				}
			});
		}
	}


	private synchronized DatagramAcceptorSession updateSession(String ip, int port) {
		// 计算会话 Key
		String sessionKey = this.calcSessionKey(ip, port);

		DatagramAcceptorSession session = this.sessionMap.get(sessionKey);

		if (null == session) {
			InetSocketAddress isa = new InetSocketAddress(ip, port);

			session = new DatagramAcceptorSession(this, isa);

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


	protected class LoopDispatchThread extends Thread {

		private boolean spinning = false;
		private boolean running = false;

		private boolean checkExpireLock = false;
		private int checkExpireCounts = 0;

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

				// 检查超期
				this.checkExpire();

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
					InetAddress addr = packet.getAddress();
					String ip = addr.getHostAddress();
					int port = packet.getPort();
					byte[] data = new byte[packet.getLength()];
					System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());

					// 匹配 Session
					DatagramAcceptorSession session = updateSession(ip, port);
					Message message = new Message(data);

					if (null != handler) {
						handler.messageReceived(session, message);
					}

					// 更新活跃时间
					session.activeTimestamp = System.currentTimeMillis();
				}
			});
		}

		private void checkExpire() {
			if (this.checkExpireLock) {
				return;
			}

			// 计数
			++this.checkExpireCounts;

			// 连接数小于最大连接数时进行计数判断
			if (sessionMap.size() < getMaxConnectNum() && this.checkExpireCounts < 10000) {
				return;
			}

			// 上锁
			this.checkExpireLock = true;
			// 重置计数
			this.checkExpireCounts = 0;

			executor.execute(new Runnable() {
				@Override
				public void run() {
					Logger.d(DatagramAcceptor.class, "DatagramAcceptor check session expire: " + sessionMap.size());

					// 当前时间
					long time = System.currentTimeMillis();

					// 清理清单
					ArrayList<DatagramAcceptorSession> removedList = new ArrayList<DatagramAcceptorSession>();

					Iterator<DatagramAcceptorSession> iter = sessionMap.values().iterator();
					while (iter.hasNext()) {
						DatagramAcceptorSession session = iter.next();
						if (time - session.activeTimestamp > sessionExpire) {
							removedList.add(session);
						}
					}

					if (!removedList.isEmpty()) {
						for (DatagramAcceptorSession session : removedList) {
							close(session);
						}
						removedList.clear();
					}
					removedList = null;

					// 解锁
					checkExpireLock = false;
				}
			});
		}
	}

}
