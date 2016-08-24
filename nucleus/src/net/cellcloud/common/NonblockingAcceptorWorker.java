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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;


/** 非阻塞网络接收器工作线程。
 * 
 * @author Jiangwei Xu
 */
public final class NonblockingAcceptorWorker extends Thread {

	// 控制线程生命周期的条件变量
	private byte[] mutex = new byte[0];
	// 是否处于自旋
	private boolean spinning = false;
	// 是否正在工作
	private boolean working = false;

	private NonblockingAcceptor acceptor;

	// 需要执行接收数据任务的 Session 列表
	private Vector<NonblockingAcceptorSession> receiveSessions = new Vector<NonblockingAcceptorSession>();
	// 需要执行发送数据任务的 Session 列表
	private Vector<NonblockingAcceptorSession> sendSessions = new Vector<NonblockingAcceptorSession>();

	// 发送数据流量统计
	private long tx = 0;
	// 接收数据流量统计
	private long rx = 0;

	private long eachSessionReadInterval = -1;
	private long eachSessionWriteInterval = -1;

	// 接收消息时的数组缓存池
	private LinkedList<ArrayList<byte[]>> tenantablePool = new LinkedList<ArrayList<byte[]>>();

	public NonblockingAcceptorWorker(NonblockingAcceptor acceptor) {
		this.acceptor = acceptor;
		this.setName("NonblockingAcceptorWorker@" + this.toString());
	}

	protected void setEachSessionReadInterval(long intervalMs) {
		this.eachSessionReadInterval = intervalMs;
	}

	protected void setEachSessionWriteInterval(long intervalMs) {
		this.eachSessionWriteInterval = intervalMs;
	}

	@Override
	public void run() {
		this.working = true;
		this.spinning = true;
		this.tx = 0;
		this.rx = 0;
		NonblockingAcceptorSession session = null;

		long time = System.currentTimeMillis();
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
							session.readTime = ctime - 1;
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
							session.writeTime = ctime - 1;
						}
					}
					else {
						this.sendSessions.add(session);
					}
				}
			} catch (Exception e) {
				Logger.log(this.getClass(), e, LogLevel.WARNING);
			}

			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// Nothing
			}

			Thread.yield();

			// 时间计数
			++timeCounts;
			if (timeCounts >= 1000) {
				time = System.currentTimeMillis();
				timeCounts = 0;
			}
		}

		this.working = false;
		this.tenantablePool.clear();
	}

	protected long getTX() {
		return this.tx;
	}

	protected long getRX() {
		return this.rx;
	}

	/** 停止自旋
	 */
	protected void stopSpinning(boolean blockingCheck) {
		this.spinning = false;

		synchronized (this.mutex) {
			this.mutex.notifyAll();
		}

		if (blockingCheck) {
			while (this.working) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					Logger.log(NonblockingAcceptorWorker.class, e, LogLevel.DEBUG);
				}
			}
		}
	}

	/** 返回线程是否正在工作。
	 */
	protected boolean isWorking() {
		return this.working;
	}

	/** 返回当前未处理的接收任务 Session 数量。
	 */
	protected int getReceiveSessionNum() {
		return this.receiveSessions.size();
	}

	/** 返回当前未处理的发送任务 Session 数量。
	 */
	protected int getSendSessionNum() {
		return this.sendSessions.size();
	}

	/** 添加执行接收数据的 Session 。
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

	/** 添加执行发送数据的 Session 。
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

	/** 从所有列表中移除指定的 Session 。
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

	/** 处理接收。
	 */
	private void processReceive(NonblockingAcceptorSession session) {
		SocketChannel channel = (SocketChannel) session.selectionKey.channel();

		if (!channel.isConnected()) {
			return;
		}

		int read = 0;
		do {
			// 创建读缓存。
			ByteBuffer buf = ByteBuffer.allocate(session.getBlock());

			synchronized (session) {
				try {
					if (channel.isOpen())
						read = channel.read(buf);
					else
						read = -1;
				} catch (IOException e) {
					if (Logger.isDebugLevel()) {
						Logger.d(this.getClass(), "Remote host has closed the connection.");
					}

					if (null != session.socket) {
						this.acceptor.fireSessionClosed(session);
					}

					try {
						if (channel.isOpen())
							channel.close();
					} catch (IOException ioe) {
						Logger.log(NonblockingAcceptorWorker.class, ioe, LogLevel.DEBUG);
					}

					// 移除 Session
					this.acceptor.eraseSession(session);
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

					try {
						if (channel.isOpen())
							channel.close();
					} catch (IOException ioe) {
						Logger.log(NonblockingAcceptorWorker.class, ioe, LogLevel.DEBUG);
					}

					// 移除 Session
					this.acceptor.eraseSession(session);
					this.removeSession(session);

					session.selectionKey.cancel();

					buf = null;

					return;
				}

				// 统计流量
				if (this.rx > Long.MAX_VALUE - read) {
					this.rx = 0;
				}
				this.rx += read;

				buf.flip();

				byte[] array = new byte[read];
				buf.get(array);

				// 解析数据
				this.parse(session, array);

				buf = null;
			}
		} while (read > 0);
	}

	/** 处理发送。
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
						}
					} catch (IOException e) {
						Logger.log(NonblockingAcceptorWorker.class, e, LogLevel.WARNING);

						if (null != session.socket) {
							this.acceptor.fireSessionClosed(session);
						}

						try {
							if (channel.isOpen()) {
								channel.close();
							}
						} catch (IOException ioe) {
							Logger.log(NonblockingAcceptorWorker.class, ioe, LogLevel.DEBUG);
						}

						// 移除 Session
						this.acceptor.eraseSession(session);
						this.removeSession(session);

						session.selectionKey.cancel();

						buf = null;

						return;
					}

					buf = null;

					// 回调事件
					this.acceptor.fireMessageSent(session, message);
				}
			} //# synchronized
		}
	}

	private void parse(NonblockingAcceptorSession session, byte[] data) {
		// 拦截器返回 true 则该数据被拦截，不再进行数据解析。
		if (this.acceptor.fireIntercepted(session, data)) {
			return;
		}

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

	private ArrayList<byte[]> borrowList() {
		synchronized (this.tenantablePool) {
			if (this.tenantablePool.isEmpty()) {
				return new ArrayList<byte[]>(2);
			}

			return this.tenantablePool.removeFirst();
		}
	}

	private void returnList(ArrayList<byte[]> list) {
		synchronized (this.tenantablePool) {
			this.tenantablePool.add(list);
		}
	}

	/** 解析数据格式。
	 * @deprecated
	 */
	protected void parseData(NonblockingAcceptorSession session, byte[] data) {
		// 拦截器返回 true 则该数据被拦截，不再进行数据解析。
		if (this.acceptor.fireIntercepted(session, data)) {
			return;
		}

		// 根据数据标志获取数据
		if (this.acceptor.existDataMark()) {
			byte[] headMark = this.acceptor.getHeadMark();
			byte[] tailMark = this.acceptor.getTailMark();

			int cursor = 0;
			int length = data.length;
			boolean head = false;
			boolean tail = false;
			byte[] buf = new byte[this.acceptor.block];
			int bufIndex = 0;

			while (cursor < length) {
				head = true;
				tail = true;

				byte b = data[cursor];

				// 判断是否是头标识
				if (b == headMark[0]) {
					for (int i = 1, len = headMark.length; i < len; ++i) {
						if (data[cursor + i] != headMark[i]) {
							head = false;
							break;
						}
					}
				}
				else {
					head = false;
				}

				// 判断是否是尾标识
				if (b == tailMark[0]) {
					for (int i = 1, len = tailMark.length; i < len; ++i) {
						if (data[cursor + i] != tailMark[i]) {
							tail = false;
							break;
						}
					}
				}
				else {
					tail = false;
				}

				if (head) {
					// 遇到头标识，开始记录数据
					cursor += headMark.length;
					bufIndex = 0;
					buf[bufIndex] = data[cursor];
				}
				else if (tail) {
					// 遇到尾标识，提取 buf 内数据
					byte[] pdata = new byte[bufIndex + 1];
					System.arraycopy(buf, 0, pdata, 0, bufIndex + 1);
					Message message = new Message(pdata);
					this.acceptor.fireMessageReceived(session, message);

					cursor += tailMark.length;
					// 后面要移动到下一个字节因此这里先减1
					cursor -= 1;
				}
				else {
					++bufIndex;
					buf[bufIndex] = b;
				}

				// 下一个字节
				++cursor;
			}

			buf = null;
		}
		else {
			Message message = new Message(data);
			this.acceptor.fireMessageReceived(session, message);
		}
	}

	/**
	 * 数据提取并输出。
	 */
	private void extract(final ArrayList<byte[]> out, final NonblockingAcceptorSession session, final byte[] data) {
		final byte[] headMark = this.acceptor.getHeadMark();
		final byte[] tailMark = this.acceptor.getTailMark();

		// 当数据小于标签长度时直接缓存
		if (data.length < headMark.length) {
			System.arraycopy(data, 0, session.cache, session.cacheCursor, data.length);
			session.cacheCursor += data.length;
			return;
		}

		byte[] real = data;
		if (session.cacheCursor > 0) {
			real = new byte[session.cacheCursor + data.length];
			System.arraycopy(session.cache, 0, real, 0, session.cacheCursor);
			System.arraycopy(data, 0, real, session.cacheCursor, data.length);
			session.cacheCursor = 0;
		}

		int index = 0;
		int len = real.length;
		int headPos = -1;
		int tailPos = -1;

		if (compareBytes(headMark, 0, real, index, headMark.length)) {
			// 有头标签
			index += headMark.length;
			// 记录数据位置头
			headPos = index;
			// 判断是否有尾标签
			while (index < len) {
//				if (real[index] == tailMark[0]) {
					if (compareBytes(tailMark, 0, real, index, tailMark.length)) {
						// 找到尾标签
						tailPos = index;
						break;
					}
					else {
						++index;
					}
//				}
//				else {
//					++index;
//				}
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
				if (len + session.cacheCursor > session.cacheSize) {
					// 缓存扩容
					session.resetCacheSize(len + session.cacheCursor);
				}

				System.arraycopy(real, 0, session.cache, session.cacheCursor, len);
				session.cacheCursor += len;
			}

			return;
		}

		byte[] newBytes = new byte[len - headMark.length];
		System.arraycopy(real, headMark.length, newBytes, 0, newBytes.length);
		extract(out, session, newBytes);
	}

	private boolean compareBytes(byte[] b1, int offsetB1, byte[] b2, int offsetB2, int length) {
		for (int i = 0; i < length; ++i) {
			// FIXME XJW 2015-12-30 判断数组越界
			if (offsetB1 + i >= b1.length || offsetB2 + i >= b2.length) {
				return false;
			}

			if (b1[offsetB1 + i] != b2[offsetB2 + i]) {
				return false;
			}
		}
		return true;
	}

	private void encryptMessage(Message message, byte[] key) {
		byte[] plaintext = message.get();
		byte[] ciphertext = Cryptology.getInstance().simpleEncrypt(plaintext, key);
		message.set(ciphertext);
	}

	private void decryptMessage(Message message, byte[] key) {
		byte[] ciphertext = message.get();
		byte[] plaintext = Cryptology.getInstance().simpleDecrypt(ciphertext, key);
		message.set(plaintext);
	}
}
