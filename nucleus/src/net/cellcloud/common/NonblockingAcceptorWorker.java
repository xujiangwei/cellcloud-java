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
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Vector;

import net.cellcloud.core.Logger;

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

	public NonblockingAcceptorWorker(NonblockingAcceptor acceptor) {
		this.acceptor = acceptor;
		this.setName("NonblockingAcceptorWorker@" + this.toString());
	}

	@Override
	public void run() {
		this.working = true;
		this.spinning = true;
		NonblockingAcceptorSession session = null;

		while (this.spinning) {
			if (!this.receiveSessions.isEmpty()) {
				// 执行接收数据任务，并移除已执行的 Session
				session = this.receiveSessions.remove(0);
				if (null != session.socket) {
					processReceive(session);
				}
			}
			if (!this.sendSessions.isEmpty()) {
				// 执行发送数据任务，并移除已执行的 Session
				session = this.sendSessions.remove(0);
				if (null != session.socket) {
					processSend(session);
				}
			}

			// 如果没有任务，则线程 wait
			synchronized (this.mutex) {
				if (this.receiveSessions.isEmpty()
					&& this.sendSessions.isEmpty()) {
					try {
						this.mutex.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			Thread.yield();
		}

		this.working = false;
	}

	/** 停止自旋
	 */
	protected void stopSpinning() {
		this.spinning = false;

		synchronized (this.mutex) {
			this.mutex.notify();
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
	protected void addReceiveSession(NonblockingAcceptorSession session, SelectionKey key) {
		session.selectionKey = key;
		this.receiveSessions.add(session);

		synchronized (this.mutex) {
			this.mutex.notify();
		}
	}

	/** 添加执行发送数据的 Session 。
	 */
	protected void addSendSession(NonblockingAcceptorSession session, SelectionKey key) {
		if (session.messages.isEmpty()) {
			return;
		}

		session.selectionKey = key;
		this.sendSessions.add(session);

		synchronized (this.mutex) {
			this.mutex.notify();
		}
	}

	/** 从所有列表中移除指定的 Session 。
	 */
	private void removeSession(NonblockingAcceptorSession session) {
		boolean exist = this.receiveSessions.remove(session);
		while (exist) {
			exist = this.receiveSessions.remove(session);
		}

		exist = this.sendSessions.remove(session);
		while (exist) {
			exist = this.sendSessions.remove(session);
		}
	}

	/** 处理接收。
	 */
	private void processReceive(NonblockingAcceptorSession session) {
		SocketChannel channel = (SocketChannel) session.selectionKey.channel();

		if (!channel.isConnected()) {
			return;
		}

		// 获取 Session 的读缓存。
		ByteBuffer buf = session.getReadBuffer();
		int read = 0;
		do {
			synchronized (buf) {
				try {
					read = channel.read(buf);
				} catch (IOException e) {
					if (null != session.socket) {
						this.acceptor.fireSessionClosed(session);
					}

					try {
						channel.close();
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}

					// 移除 Session
					this.acceptor.eraseSession(session);

					this.removeSession(session);

					session.selectionKey.cancel();

					return;
				}

				if (read == 0) {
					break;
				}
				else if (read == -1) {
					if (null != session.socket) {
						this.acceptor.fireSessionClosed(session);
					}

					try {
						channel.close();
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}

					// 移除 Session
					this.acceptor.eraseSession(session);

					this.removeSession(session);

					session.selectionKey.cancel();

					return;
				}

				buf.flip();

				byte[] array = new byte[read];
				buf.get(array);

				// 解析数据
				parse(session, array);

				buf.clear();
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

		if (!session.messages.isEmpty()) {
			// 有消息，进行发送

			Message message = null;

			// 获取 Session 的写缓存
			ByteBuffer buf = session.getWriteBuffer();
			synchronized (buf) {
				while (!session.messages.isEmpty()) {
					message = session.messages.remove(0);

					// 根据是否有数据掩码组装数据包
					if (this.acceptor.existDataMark()) {
						byte[] data = message.get();
						byte[] head = this.acceptor.getHeadMark();
						byte[] tail = this.acceptor.getTailMark();
						byte[] pd = new byte[data.length + head.length + tail.length];
						System.arraycopy(head, 0, pd, 0, head.length);
						System.arraycopy(data, 0, pd, head.length, data.length);
						System.arraycopy(tail, 0, pd, head.length + data.length, tail.length);
						buf.put(pd);
					}
					else {
						buf.put(message.get());
					}

					buf.flip();

					try {
						channel.write(buf);
					} catch (IOException e) {
						e.printStackTrace();
						Logger.e(NonblockingAcceptorWorker.class, e.getMessage());
					}

					buf.clear();

					// 回调事件
					this.acceptor.fireMessageSent(session, message);
				}
			} //# synchronized
		}
	}

	/** 解析数据格式。
	 */
	private void parse(NonblockingAcceptorSession session, byte[] data) {
		// 根据数据标志获取数据
		if (this.acceptor.existDataMark()) {
			byte[] headMark = this.acceptor.getHeadMark();
			byte[] tailMark = this.acceptor.getTailMark();

			int cursor = 0;
			int length = data.length;
			boolean head = false;
			boolean tail = false;
			byte[] buf = new byte[8192];
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
}
