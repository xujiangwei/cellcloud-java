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
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 数据报连接器。
 * 
 * @author Ambrose Xu
 */
public class DatagramConnector extends MessageService implements MessageConnector {

	private DatagramSocket socket;

	private Session session = null;

	// 30 秒
	private int soTimeout = 30000;

	// 块大小
	private int block = 16 * 1024;

	private Thread handleThread;
	private boolean spinning = false;
	private boolean running = false;

	private LinkedList<Message> writeQueue;
	private Thread writeThread;
	private AtomicBoolean writing;

	public DatagramConnector() {
		super();
		this.writeQueue = new LinkedList<Message>();
		this.writing = new AtomicBoolean(false);
	}

	@Override
	public boolean connect(InetSocketAddress address) {
		if (null != this.socket) {
			return false;
		}

		// 尝试 3 次
		for (int i = 1; i <= 3; ++i) {
			try {
				this.socket = new DatagramSocket(address.getPort() + i);
				this.socket.setSoTimeout(this.soTimeout);
				this.socket.setReceiveBufferSize(this.block + this.block);
				this.socket.setSendBufferSize(this.block + this.block);
				break;
			} catch (SocketException e) {
				// Noting
			}
		}

		if (null == this.socket) {
			return false;
		}

		// 创建 Session
		this.session = new Session(this, address);

		if (null != this.handler) {
			this.handler.sessionCreated(this.session);
		}

		this.handleThread = new Thread() {
			@Override
			public void run() {
				if (null != handler) {
					handler.sessionOpened(session);
				}

				loopDispatch();

				if (null != handler) {
					handler.sessionClosed(session);
				}

				if (null != handler) {
					handler.sessionDestroyed(session);
				}
			}
		};
		this.handleThread.start();

		return true;
	}

	@Override
	public void disconnect() {
		synchronized (this.writeQueue) {
			this.writeQueue.clear();
		}

		this.spinning = false;

		if (null != this.socket) {
			this.socket.close();
			this.socket = null;
		}

		int counts = 10;
		while (this.running) {
			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) {
				// Nothing
			}

			--counts;
			if (counts == 0) {
				break;
			}
		}
	}

	@Override
	public void setConnectTimeout(long timeout) {
		if (timeout >= Integer.MAX_VALUE) {
			return;
		}

		this.soTimeout = (int) timeout;
	}

	@Override
	public void setBlockSize(int size) {
		this.block = size;
	}

	@Override
	public Session getSession() {
		return this.session;
	}

	public void write(Message message) {
		this.write(this.session, message);
	}

	@Override
	public void write(Session session, Message message) {
		if (null == this.session) {
			return;
		}

		if (session.getId().longValue() != this.session.getId().longValue()) {
			if (null != this.handler) {
				this.handler.errorOccurred(MessageErrorCode.STATE_ERROR, session);
			}
			return;
		}

		if (null == this.socket) {
			if (null != this.handler) {
				this.handler.errorOccurred(MessageErrorCode.WRITE_FAILED, session);
			}
			return;
		}

		synchronized (this.writeQueue) {
			this.writeQueue.addLast(message);
		}

		if (!this.writing.get()) {
			this.writing.set(true);

			this.writeThread = new Thread() {
				@Override
				public void run() {
					// 默认等待 10 秒
					int counts = 500;

					do {
						if (counts == 0 || null == socket) {
							break;
						}

						try {
							Thread.sleep(20L);
						} catch (InterruptedException e) {
							// Nothing
						}

						Message msg = null;
						synchronized (writeQueue) {
							if (writeQueue.isEmpty()) {
								--counts;
							}
							else {
								counts = 500;
								msg = writeQueue.removeFirst();
							}
						}

						if (null == msg) {
							continue;
						}

						try {
							// 创建发送包
							DatagramPacket dp = new DatagramPacket(msg.get(), msg.length(),
									DatagramConnector.this.session.getAddress());

							// 发送
							socket.send(dp);

							if (null != handler) {
								handler.messageSent(DatagramConnector.this.session, msg);
							}
						} catch (SocketException e) {
							Logger.log(this.getClass(), e, LogLevel.ERROR);
							if (null != handler) {
								handler.errorOccurred(MessageErrorCode.SOCKET_FAILED, DatagramConnector.this.session);
							}
						} catch (IOException e) {
							Logger.log(this.getClass(), e, LogLevel.ERROR);
							if (null != handler) {
								handler.errorOccurred(MessageErrorCode.WRITE_FAILED, DatagramConnector.this.session);
							}
						}
					} while (!writeQueue.isEmpty() || counts > 0);

					writing.set(false);
				}
			};
			this.writeThread.start();
		}
	}

	private void loopDispatch() {
		this.spinning = true;
		this.running = true;

		while (null != this.socket && this.spinning) {
			try {
				Thread.sleep(10L);
			} catch (InterruptedException e) {
				// Nothing
			}

			byte[] buf = new byte[this.block];
			DatagramPacket dp = new DatagramPacket(buf, this.block);

			try {
				this.socket.receive(dp);
			} catch (SocketTimeoutException e) {
				buf = null;
				dp = null;
				continue;
			} catch (IOException e) {
				buf = null;
				dp = null;
				continue;
			}

			// 创建 message
			byte[] data = new byte[dp.getLength()];
			System.arraycopy(dp.getData(), 0, data, 0, dp.getLength());
			Message message = new Message(data);
			if (null != this.handler) {
				this.handler.messageReceived(this.session, message);
			}

			buf = null;
			dp = null;
		}

		this.running = false;
	}
}
