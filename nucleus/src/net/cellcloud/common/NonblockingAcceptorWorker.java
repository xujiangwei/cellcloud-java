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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 非阻塞网络工作器线程。
 * 
 * 此线程负责处理其关联的会话的数据读、写操作。
 * 
 * @author Ambrose Xu
 *
 */
public final class NonblockingAcceptorWorker extends Thread {

	/** 控制线程生命周期的条件变量。 */
	private Object mutex = new Object();
	/** 是否处于自旋。 */
	private boolean spinning = false;
	/** 是否正在工作。 */
	private boolean working = false;

	/** 关联的接收器。 */
	private NonblockingAcceptor acceptor;

	/** 输出传输配额计算器。 */
	private QuotaCalculator transmissionQuota;

	/** 需要执行接收数据任务的 Session 列表。 */
	private Vector<NonblockingAcceptorSession> receiveSessions = new Vector<NonblockingAcceptorSession>();
	/** 需要执行发送数据任务的 Session 列表。 */
	private Vector<NonblockingAcceptorSession> sendSessions = new Vector<NonblockingAcceptorSession>();

	/** 发送数据流量统计。 */
	private long tx = 0;
	/** 接收数据流量统计。 */
	private long rx = 0;

	/** 每个会话的读间隔。 */
	private long eachSessionReadInterval = -1;
	/** 每个会话的写间隔。 */
	private long eachSessionWriteInterval = -1;

	/** 接收消息时的数组缓存池。 */
	private LinkedList<ArrayList<byte[]>> tenantablePool = new LinkedList<ArrayList<byte[]>>();

	/**
	 * 构造函数。
	 * 
	 * @param acceptor 消息接收器。
	 * @param scheduledExecutor 任务执行器。
	 */
	public NonblockingAcceptorWorker(NonblockingAcceptor acceptor, ScheduledExecutorService scheduledExecutor) {
		this.acceptor = acceptor;
		this.transmissionQuota = new QuotaCalculator(scheduledExecutor, 1024 * 1024);
		this.setName("NonblockingAcceptorWorker@" + this.toString());
	}

	/**
	 * 设置每个会话读数据间隔。
	 * 
	 * @param intervalMs 以毫秒为单位的时间间隔。
	 */
	protected void setEachSessionReadInterval(long intervalMs) {
		this.eachSessionReadInterval = intervalMs;
	}

	/**
	 * 获得每个会话读数据间隔。
	 * 
	 * @return 返回每个会话读数据间隔。
	 */
	protected long getEachSessionReadInterval() {
		return this.eachSessionReadInterval;
	}

	/**
	 * 设置每个会话写数据间隔。
	 * 
	 * @param intervalMs
	 */
	protected void setEachSessionWriteInterval(long intervalMs) {
		this.eachSessionWriteInterval = intervalMs;
	}

	/**
	 * 获得每个会话写数据间隔。
	 * 
	 * @return 返回每个会话写数据间隔。
	 */
	protected long getEachSessionWriteInterval() {
		return this.eachSessionWriteInterval;
	}

	/**
	 * 获得配额计算器。
	 * 
	 * @return 返回配额计算器。
	 */
	protected QuotaCalculator getQuotaCalculator() {
		return this.transmissionQuota;
	}

	@Override
	public void run() {
		this.working = true;
		this.spinning = true;
		this.tx = 0;
		this.rx = 0;
		NonblockingAcceptorSession session = null;

		// 启动配额管理
		this.transmissionQuota.start();

		long time = System.currentTimeMillis();
		// 使用 time 计数方式来减少 System.currentTimeMillis() 的调用次数，提高效率
		int timeCounts = 0;

		while (this.spinning) {
			// 如果没有任务，则线程 wait
			synchronized (this.mutex) {
				if (this.receiveSessions.isEmpty()
					&& this.sendSessions.isEmpty()
					&& this.spinning) {
					try {
						this.mutex.wait();
					} catch (InterruptedException e) {
						Logger.log(NonblockingAcceptorWorker.class, e, LogLevel.DEBUG);
					}

					time = System.currentTimeMillis();
					timeCounts = 0;
				}
			}

			long ctime = time + timeCounts;

			try {
				if (!this.receiveSessions.isEmpty()) {
					// 执行接收数据任务，并移除已执行的 Session
					session = this.receiveSessions.remove(0);
					if (ctime - session.readTime > this.eachSessionReadInterval) {
						if (null != session.socket) {
							processReceive(session);
							session.readTime = ctime;
						}
					}
					else {
						this.receiveSessions.add(session);
					}
				}

				if (!this.sendSessions.isEmpty()) {
					// 执行发送数据任务，并移除已执行的 Session
					session = this.sendSessions.remove(0);
					if (ctime - session.writeTime > this.eachSessionWriteInterval) {
						if (null != session.socket) {
							processSend(session);
							session.writeTime = ctime;
						}
					}
					else {
						this.sendSessions.add(session);
					}
				}
			} catch (Exception e) {
				Logger.log(this.getClass(), e, LogLevel.WARNING);
			}

			// 时间计数
			++timeCounts;
			if (timeCounts >= 1000) {
				time = System.currentTimeMillis();
				timeCounts = 0;
			}
		}

		// 停止配额管理
		this.transmissionQuota.stop();

		this.working = false;
		this.tenantablePool.clear();
	}

	/**
	 * 获得发送流量。
	 * 
	 * @return 返回以字节为单位的流量。
	 */
	protected long getTX() {
		return this.tx;
	}

	/**
	 * 获得接收流量。
	 * 
	 * @return 返回以字节为单位的流量。
	 */
	protected long getRX() {
		return this.rx;
	}

	/**
	 * 停止工作线程自旋。
	 * 
	 * @param blockingCheck
	 */
	protected void stopSpinning(boolean blockingCheck) {
		this.spinning = false;

		synchronized (this.mutex) {
			this.mutex.notifyAll();
		}

		if (blockingCheck) {
			while (this.working) {
				try {
					Thread.sleep(10L);
				} catch (InterruptedException e) {
					Logger.log(NonblockingAcceptorWorker.class, e, LogLevel.DEBUG);
				}
			}
		}
	}

	/**
	 * 返回线程是否正在工作。
	 */
	protected boolean isWorking() {
		return this.working;
	}

	/**
	 * 返回当前未处理的接收任务 Session 数量。
	 */
	protected int getReceiveSessionNum() {
		return this.receiveSessions.size();
	}

	/**
	 * 返回当前未处理的发送任务 Session 数量。
	 */
	protected int getSendSessionNum() {
		return this.sendSessions.size();
	}

	/**
	 * 添加执行接收数据的 Session 。
	 * 
	 * @param session 指定 Session 对象。
	 */
	protected void pushReceiveSession(NonblockingAcceptorSession session) {
		if (!this.spinning) {
			return;
		}

		this.receiveSessions.add(session);

		synchronized (this.mutex) {
			this.mutex.notifyAll();
		}
	}

	/**
	 * 添加执行发送数据的 Session 。
	 * 
	 * @param session 指定 Session 对象。
	 */
	protected void pushSendSession(NonblockingAcceptorSession session) {
		if (!this.spinning) {
			return;
		}

		if (session.isMessageEmpty()) {
			return;
		}

		this.sendSessions.add(session);

		synchronized (this.mutex) {
			this.mutex.notifyAll();
		}
	}

	/**
	 * 从所有列表中移除指定的 Session 。
	 * 
	 * @param session 指定 Session 对象。
	 */
	private void removeSession(NonblockingAcceptorSession session) {
		try {
			boolean exist = this.receiveSessions.remove(session);
			while (exist) {
				exist = this.receiveSessions.remove(session);
			}

			exist = this.sendSessions.remove(session);
			while (exist) {
				exist = this.sendSessions.remove(session);
			}
		} catch (Exception e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		}
	}

	/**
	 * 处理数据接收。
	 * 
	 * @param session
	 */
	private void processReceive(NonblockingAcceptorSession session) {
		SocketChannel channel = (SocketChannel) session.selectionKey.channel();

		if (!channel.isConnected()) {
			return;
		}

		int totalReaded = 0;
		ByteBuffer buffer = ByteBuffer.allocate(session.getBlock() + session.getBlock());

		int read = 0;
		do {
			read = 0;
			// 创建读缓存
			ByteBuffer buf = ByteBuffer.allocate(16384);

			synchronized (session) {
				try {
					if (channel.isOpen()) {
						read = channel.read(buf);
					}
					else {
						read = -1;
					}
				} catch (IOException e) {
					if (Logger.isDebugLevel()) {
						Logger.d(this.getClass(), "Remote host has closed the connection.");
					}

					if (null != session.socket) {
						this.acceptor.fireSessionClosed(session);
					}

					// 移除 Session
					this.acceptor.eraseSession(session);

					try {
						if (channel.isOpen())
							channel.close();
					} catch (IOException ioe) {
						Logger.log(NonblockingAcceptorWorker.class, ioe, LogLevel.DEBUG);
					}

					this.removeSession(session);

					session.selectionKey.cancel();

					buf = null;

					return;
				}

				if (read == 0) {
					buf = null;
					break;
				}
				else if (read == -1) {
					if (null != session.socket) {
						this.acceptor.fireSessionClosed(session);
					}

					// 移除 Session
					this.acceptor.eraseSession(session);

					try {
						if (channel.isOpen())
							channel.close();
					} catch (IOException ioe) {
						Logger.log(NonblockingAcceptorWorker.class, ioe, LogLevel.DEBUG);
					}

					this.removeSession(session);

					session.selectionKey.cancel();

					buf = null;

					return;
				}
			} // #synchronized

			// 计算长度
			totalReaded += read;

			if (buf.position() != 0) {
				buf.flip();
			}
			// 合并
			buffer.put(buf);
		} while (read > 0);

		if (0 == totalReaded) {
			// 没有读取到数据
			return;
		}

		// 统计流量
		if (this.rx > Long.MAX_VALUE - totalReaded) {
			this.rx = 0;
		}
		this.rx += totalReaded;

		buffer.flip();

		byte[] array = new byte[totalReaded];
		buffer.get(array);

		// 解析数据
		this.parse(session, array);

		buffer.clear();
		buffer = null;
	}

	/**
	 * 处理数据发送。
	 * 
	 * @param session
	 */
	private void processSend(NonblockingAcceptorSession session) {
		SocketChannel channel = (SocketChannel) session.selectionKey.channel();

		if (!channel.isConnected()) {
			return;
		}

		if (!session.isMessageEmpty()) {
			// 有消息，进行发送
			Message message = null;

			synchronized (session) {
				// 遍历待发信息
				while (!session.isMessageEmpty()) {
					message = session.pollMessage();
					if (null == message) {
						break;
					}

					// 是否进行消息加密
					byte[] key = session.getSecretKey();
					if (null != key) {
						this.encryptMessage(message, key);
					}

					// 创建写缓存
					ByteBuffer buf = null;

					// 根据是否有数据掩码组装数据包
					if (this.acceptor.existDataMark()) {
						byte[] data = message.get();
						byte[] head = this.acceptor.getHeadMark();
						byte[] tail = this.acceptor.getTailMark();
						byte[] pd = new byte[data.length + head.length + tail.length];
						System.arraycopy(head, 0, pd, 0, head.length);
						System.arraycopy(data, 0, pd, head.length, data.length);
						System.arraycopy(tail, 0, pd, head.length + data.length, tail.length);
						buf = ByteBuffer.wrap(pd);
					}
					else {
						buf = ByteBuffer.wrap(message.get());
					}

					try {
						int size = channel.write(buf);

						// 统计流量
						if (size > 0) {
							if (this.tx > Long.MAX_VALUE - size) {
								this.tx = 0;
							}

							this.tx += size;

							// 配额控制
							this.transmissionQuota.consumeBlocking(size, null, null);
						}
					} catch (IOException e) {
						Logger.log(NonblockingAcceptorWorker.class, e, LogLevel.WARNING);

						if (null != session.socket) {
							this.acceptor.fireSessionClosed(session);
						}

						// 移除 Session
						this.acceptor.eraseSession(session);

						try {
							if (channel.isOpen()) {
								channel.close();
							}
						} catch (IOException ioe) {
							Logger.log(NonblockingAcceptorWorker.class, ioe, LogLevel.DEBUG);
						}

						this.removeSession(session);

						session.selectionKey.cancel();

						buf = null;

						return;
					}

					buf = null;

					// 回调事件
					this.acceptor.fireMessageSent(session, message);
				}
			} // #synchronized
		}
	}

	/**
	 * 解析并通知数据接收。
	 * 
	 * @param session
	 * @param data
	 */
	private void parse(NonblockingAcceptorSession session, byte[] data) {
		try {
			// 根据数据标志获取数据
			if (this.acceptor.existDataMark()) {
				ArrayList<byte[]> out = this.borrowList();
				// 进行递归提取
				this.extract(out, session, data);

				if (!out.isEmpty()) {
					for (byte[] bytes : out) {
						Message message = new Message(bytes);

						// 是否是加密会话，如果是则进行解密
						byte[] key = session.getSecretKey();
						if (null != key) {
							this.decryptMessage(message, key);
						}

						this.acceptor.fireMessageReceived(session, message);
					}

					out.clear();
				}
				this.returnList(out);
			}
			else {
				Message message = new Message(data);

				// 是否是加密会话，如果是则进行解密
				byte[] key = session.getSecretKey();
				if (null != key) {
					this.decryptMessage(message, key);
				}

				this.acceptor.fireMessageReceived(session, message);
			}
		} catch (Exception e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
		}
	}

	/**
	 * 借出数据字节数组。
	 * 
	 * @return
	 */
	private ArrayList<byte[]> borrowList() {
		synchronized (this.tenantablePool) {
			if (this.tenantablePool.isEmpty()) {
				return new ArrayList<byte[]>(2);
			}

			return this.tenantablePool.removeFirst();
		}
	}

	/**
	 * 归还数据字节数组。
	 * 
	 * @param list
	 */
	private void returnList(ArrayList<byte[]> list) {
		list.clear();

		synchronized (this.tenantablePool) {
			this.tenantablePool.add(list);
		}
	}

	/**
	 * 数据提取并输出。
	 * 
	 * @param out 接收数据的数组。
	 * @param session 会话。
	 * @param data 待提取的数据。
	 */
	private void extract(final ArrayList<byte[]> out, final NonblockingAcceptorSession session, final byte[] data) {
		final byte[] headMark = this.acceptor.getHeadMark();
		final byte[] tailMark = this.acceptor.getTailMark();

		// 当数据小于标签长度时直接缓存
//		if (data.length < headMark.length) {
//			if (session.cacheCursor + data.length > session.getCacheSize()) {
//				// 重置 cache 大小
//				session.resetCacheSize(session.cacheCursor + data.length);
//			}
//			System.arraycopy(data, 0, session.cache, session.cacheCursor, data.length);
//			session.cacheCursor += data.length;
//			return;
//		}

		byte[] real = data;
		if (session.cacheCursor > 0) {
			real = new byte[session.cacheCursor + data.length];
			System.arraycopy(session.cache, 0, real, 0, session.cacheCursor);
			System.arraycopy(data, 0, real, session.cacheCursor, data.length);
			session.cacheCursor = 0;
		}

		// 当数据小于标签长度时直接缓存
		if (real.length < headMark.length) {
			if (session.cacheCursor + real.length > session.getCacheSize()) {
				// 重置 cache 大小
				session.resetCacheSize(session.cacheCursor + real.length);
			}
			System.arraycopy(real, 0, session.cache, session.cacheCursor, real.length);
			session.cacheCursor += real.length;
			return;
		}

		int index = 0;
		int len = real.length;
		int headPos = -1;
		int tailPos = -1;

		if (0 == compareBytes(headMark, 0, real, index, headMark.length)) {
			// 有头标签
			index += headMark.length;
			// 记录数据位置头
			headPos = index;
			// 判断是否有尾标签，依次计数
			int ret = -1;
			while (index < len) {
				if (real[index] == tailMark[0]) {
					ret = compareBytes(tailMark, 0, real, index, tailMark.length);
					if (0 == ret) {
						// 找到尾标签
						tailPos = index;
						break;
					}
					else if (1 == ret) {
						// 越界
						break;
					}
					else {
						// 未找到尾标签
						++index;
					}
				}
				else {
					++index;
				}
			}

			if (headPos > 0 && tailPos > 0) {
				byte[] outBytes = new byte[tailPos - headPos];
				System.arraycopy(real, headPos, outBytes, 0, tailPos - headPos);
				out.add(outBytes);

				int newLen = len - tailPos - tailMark.length;
				if (newLen > 0) {
					byte[] newBytes = new byte[newLen];
					System.arraycopy(real, tailPos + tailMark.length, newBytes, 0, newLen);

					// 递归
					extract(out, session, newBytes);
				}
			}
			else {
				// 没有尾标签
				// 仅进行缓存
				if (len + session.cacheCursor > session.getCacheSize()) {
					// 缓存扩容
					session.resetCacheSize(len + session.cacheCursor);
				}

				System.arraycopy(real, 0, session.cache, session.cacheCursor, len);
				session.cacheCursor += len;
			}
		}
		else {
			// 没有头标签
			// 尝试找到头标签
			byte[] markBuf = new byte[headMark.length];
			int searchIndex = 0;
			int searchCounts = 0;
			do {
				// 判断数据是否越界
				if (searchIndex + headMark.length > len) {
					// 越界，删除索引之前的所有数据
					byte[] newReal = new byte[len - searchIndex];
					System.arraycopy(real, searchIndex, newReal, 0, newReal.length);

					if (session.cacheCursor + newReal.length > session.getCacheSize()) {
						// 重置 cache 大小
						session.resetCacheSize(session.cacheCursor + newReal.length);
					}
					System.arraycopy(newReal, 0, session.cache, session.cacheCursor, newReal.length);
					session.cacheCursor += newReal.length;
					// 退出循环
					break;
				}

				// 复制数据到待测试缓存
				System.arraycopy(real, searchIndex, markBuf, 0, headMark.length);

				for (int i = 0; i < markBuf.length; ++i) {
					if (markBuf[i] == headMark[i]) {
						++searchCounts;
					}
					else {
						break;
					}
				}

				if (searchCounts == headMark.length) {
					// 找到 head mark
					byte[] newReal = new byte[len - searchIndex];
					System.arraycopy(real, searchIndex, newReal, 0, newReal.length);
					extract(out, session, newReal);
					return;
				}

				// 更新索引
				++searchIndex;

				// 重置计数
				searchCounts = 0;
			} while (searchIndex < len);
		}

//		byte[] newBytes = new byte[len - headMark.length];
//		System.arraycopy(real, headMark.length, newBytes, 0, newBytes.length);
//		extract(out, session, newBytes);
	}

	/**
	 * 比较字节数组是否相等。
	 * 
	 * @param b1 指定字节数组1。
	 * @param offsetB1 指定字节数组1操作偏移。
	 * @param b2 指定字节数组2。
	 * @param offsetB2 指定字节数组2操作偏移。
	 * @param length 指定数组比较长度。
	 * @return 返回 <code>0</code> 表示匹配，<code>-1</code> 表示不匹配，<code>1</code> 表示越界
	 */
	private int compareBytes(byte[] b1, int offsetB1, byte[] b2, int offsetB2, int length) {
		for (int i = 0; i < length; ++i) {
			// FIXME XJW 2015-12-30 判断数组越界
			if (offsetB1 + i >= b1.length || offsetB2 + i >= b2.length) {
				return 1;
			}

			if (b1[offsetB1 + i] != b2[offsetB2 + i]) {
				return -1;
			}
		}

		return 0;
	}

	/**
	 * 加密消息。
	 * 
	 * @param message 指定待加密消息。
	 * @param key 指定加密密钥。
	 */
	private void encryptMessage(Message message, byte[] key) {
		byte[] plaintext = message.get();
		byte[] ciphertext = Cryptology.getInstance().simpleEncrypt(plaintext, key);
		message.set(ciphertext);
	}

	/**
	 * 解密消息。
	 * 
	 * @param message 指定待解密消息。
	 * @param key 指定解密密钥。
	 */
	private void decryptMessage(Message message, byte[] key) {
		byte[] ciphertext = message.get();
		byte[] plaintext = Cryptology.getInstance().simpleDecrypt(ciphertext, key);
		message.set(plaintext);
	}

}
