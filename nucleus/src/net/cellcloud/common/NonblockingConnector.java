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
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutorService;

import net.cellcloud.util.CachedQueueExecutor;

/**
 * 非阻塞式网络连接器。
 * 
 * @author Ambrose Xu
 * 
 */
public class NonblockingConnector extends MessageService implements MessageConnector {

	/** 缓冲块大小。 */
	private int block = 65536;
	/** 单次写数据大小限制。 */
	private int writeLimit = 32768;

	/** 连接器连接的地址。 */
	private InetSocketAddress address;
	/** 连接超时时间。 */
	private long connectTimeout;
	/** NIO socket channel */
	private SocketChannel channel;
	/** NIO selector */
	private Selector selector;

	/** 对应的会话对象。 */
	private Session session;

	/** 数据处理线程。 */
	private Thread handleThread;
	/** 线程是否自旋。 */
	private boolean spinning = false;
	/** 线程是否正在运行。 */
	private boolean running = false;

	/** 线程睡眠间隔。 */
	private long sleepInterval = 20L;

	/** 待发送消息列表。 */
	private Vector<Message> messages;

	/** 是否关闭连接。 */
	private boolean closed = false;

	/**
	 * 线程池执行器。
	 */
	private ExecutorService executor = null;

	/**
	 * 构造函数。
	 */
	public NonblockingConnector() {
		this.connectTimeout = 10000L;
		this.messages = new Vector<Message>();
	}

	/**
	 * 获得连接地址。
	 * 
	 * @return 返回连接地址。
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean connect(InetSocketAddress address) {
		if (this.channel != null && this.channel.isConnected()) {
			Logger.w(NonblockingConnector.class, "Connector has connected to " + address.getAddress().getHostAddress());
			return true;
		}

		if (this.running && null != this.channel) {
			this.spinning = false;

			try {
				if (this.channel.isOpen()) {
					this.channel.close();
				}

				if (null != this.selector) {
					this.selector.close();
				}
			} catch (IOException e) {
				Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);
			}

			while (this.running) {
				try {
					Thread.sleep(10L);
				} catch (InterruptedException e) {
					Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);
					break;
				}
			}
		}

		// 状态初始化
		this.messages.clear();
		this.address = address;

		try {
			this.channel = SocketChannel.open();
			this.channel.configureBlocking(false);

			// 配置
			// 以下为 JDK7 的代码
			this.channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
			this.channel.setOption(StandardSocketOptions.SO_RCVBUF, this.block);
			this.channel.setOption(StandardSocketOptions.SO_SNDBUF, this.block);
			// 以下为 JDK6 的代码
			/*
			this.channel.socket().setKeepAlive(true);
			this.channel.socket().setReceiveBufferSize(this.block + 64);
			this.channel.socket().setSendBufferSize(this.block + 64);
			*/

			this.selector = Selector.open();
			// 注册事件
			this.channel.register(this.selector, SelectionKey.OP_CONNECT);

			// 连接
			this.channel.connect(this.address);
		} catch (IOException e) {
			Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);

			// 回调错误
			this.fireErrorOccurred(MessageErrorCode.SOCKET_FAILED);

			try {
				if (null != this.channel) {
					this.channel.close();
				}
			} catch (Exception ce) {
				// Nothing
			}
			try {
				if (null != this.selector) {
					this.selector.close();
				}
			} catch (Exception se) {
				// Nothing
			}

			return false;
		} catch (Exception e) {
			Logger.log(NonblockingConnector.class, e, LogLevel.WARNING);
			return false;
		}

		if (null != this.executor) {
			this.executor.shutdown();
		}

		this.executor = CachedQueueExecutor.newCachedQueueThreadPool(2);

		// 创建 Session
		this.session = new Session(this, this.address);

		this.handleThread = new Thread() {
			@Override
			public void run() {
				running = true;

				// 通知 Session 创建。
				fireSessionCreated();

				try {
					loopDispatch();
				} catch (Exception e) {
					spinning = false;
					Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);
				}

				if (null != executor) {
					executor.shutdown();
					executor = null;
				}

				// 通知 Session 销毁。
				fireSessionDestroyed();

				running = false;

				try {
					if (null != selector && selector.isOpen())
						selector.close();
					if (null != channel && channel.isOpen())
						channel.close();
				} catch (IOException e) {
					// Nothing
				}
			}
		};
		this.handleThread.setName(new StringBuilder("NonblockingConnector[").append(this.handleThread).append("]@")
			.append(this.address.getAddress().getHostAddress()).append(":").append(this.address.getPort()).toString());
		// 启动线程
		this.handleThread.start();

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void disconnect() {
		this.spinning = false;

		if (null != this.channel) {
			if (this.channel.isConnected()) {
				fireSessionClosed();
			}

			try {
				if (this.channel.isOpen()) {
					this.channel.close();
				}
			} catch (Exception e) {
				Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);
			}

			try {
				this.channel.socket().close();
			} catch (Exception e) {
				//Logger.logException(e, LogLevel.DEBUG);
			}
		}

		if (null != this.selector && this.selector.isOpen()) {
			try {
				this.selector.wakeup();
				this.selector.close();
			} catch (Exception e) {
				Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);
			}
		}

		int count = 0;
		while (this.running) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);
			}

			if (++count >= 300) {
				this.handleThread.interrupt();
				this.running = false;
			}
		}

		if (null != this.executor) {
			this.executor.shutdown();
			this.executor = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setConnectTimeout(long timeout) {
		this.connectTimeout = timeout;
	}

	/**
	 * 获得连接超时时间。
	 * 
	 * @return 返回以毫秒为单位的时间长度。
	 */
	public long getConnectTimeout() {
		return this.connectTimeout;
	}

	/**
	 * 重置线程 sleep 间隔。
	 * 
	 * @param sleepInterval 指定以毫秒为单位的间隔。
	 */
	public void resetSleepInterval(long sleepInterval) {
		this.sleepInterval = sleepInterval;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBlockSize(int size) {
		if (size < 2048) {
			return;
		}

		if (this.block == size) {
			return;
		}

		this.block = size;
		this.writeLimit = Math.round(size * 0.5f);

		if (null != this.channel) {
			try {
				this.channel.socket().setReceiveBufferSize(this.block);
				this.channel.socket().setSendBufferSize(this.block);
			} catch (Exception e) {
				// ignore
			}
		}
	}

	/**
	 * 获得缓存快大小。
	 * 
	 * @return 返回缓存块大小。
	 */
	public int getBlockSize() {
		return this.block;
	}

	/**
	 * 是否已建立连接。
	 * 
	 * @return 如果已经建立连接返回 <code>true</code> 。
	 */
	public boolean isConnected() {
		return (null != this.channel && this.channel.isConnected());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Session getSession() {
		return this.session;
	}

	/**
	 * 写消息数据给已连接的服务器。
	 * 
	 * @param message 指定消息。
	 */
	public void write(Message message) {
		this.write(null, message);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(Session session, Message message) {
		if (message.length() > this.writeLimit) {
			this.fireErrorOccurred(MessageErrorCode.WRITE_OUTOFBOUNDS);
			return;
		}

		this.messages.add(message);
	}

	/**
	 * 通知会话创建。
	 */
	private void fireSessionCreated() {
		if (null != this.handler) {
			this.handler.sessionCreated(this.session);
		}
	}

	/**
	 * 通知会话启用。
	 */
	private void fireSessionOpened() {
		if (null != this.handler) {
			this.closed = false;
			this.handler.sessionOpened(this.session);
		}
	}

	/**
	 * 通知会话停用。
	 */
	private void fireSessionClosed() {
		if (null != this.handler) {
			if (!this.closed) {
				this.closed = true;
				this.handler.sessionClosed(this.session);
			}
		}
	}

	/**
	 * 通知会话销毁。
	 */
	private void fireSessionDestroyed() {
		if (null != this.handler) {
			this.handler.sessionDestroyed(this.session);
		}
	}

	/**
	 * 通知发生连接错误。
	 * 
	 * @param errorCode 错误码。
	 */
	private void fireErrorOccurred(int errorCode) {
		if (null != this.handler) {
			this.handler.errorOccurred(errorCode, this.session);
		}
	}

	/**
	 * 循环事件分发处理。
	 * 
	 * @throws Exception
	 */
	private void loopDispatch() throws Exception {
		// 自旋
		this.spinning = true;

		while (this.spinning) {
			if (!this.selector.isOpen()) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);
				}
				continue;
			}

			if (this.selector.select(this.channel.isConnected() ? 0 : this.connectTimeout) > 0) {
				Set<SelectionKey> keys = this.selector.selectedKeys();
				Iterator<SelectionKey> it = keys.iterator();
				while (it.hasNext()) {
					SelectionKey key = (SelectionKey) it.next();
					it.remove();

					// 当前通道选择器产生连接已经准备就绪事件，并且客户端套接字通道尚未连接到服务端套接字通道
					if (key.isConnectable()) {
						if (!this.doConnect(key)) {
							this.spinning = false;
							return;
						}
						else {
							// 连接成功，打开 Session
							fireSessionOpened();
						}
					}
					if (key.isValid() && key.isReadable()) {
						receive(key);
					}
					if (key.isValid() && key.isWritable()) {
						send(key);
					}
				} //# while

				if (!this.spinning) {
					break;
				}

				try {
					Thread.sleep(this.sleepInterval);
				} catch (InterruptedException e) {
				}

				Thread.yield();
			}
		} // # while

		// 关闭会话
		this.fireSessionClosed();
	}

	/**
	 * 执行连接事件。
	 * 
	 * @param key 
	 * @return
	 */
	private boolean doConnect(SelectionKey key) {
		// 获取创建通道选择器事件键的套接字通道
		SocketChannel channel = (SocketChannel) key.channel();

		// 判断此通道上是否正在进行连接操作。  
        // 完成套接字通道的连接过程。
		if (channel.isConnectionPending()) {
			try {
				channel.finishConnect();
			} catch (IOException e) {
				Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);

				try {
					this.channel.close();
					this.selector.close();
				} catch (IOException ce) {
					Logger.log(NonblockingConnector.class, ce, LogLevel.DEBUG);
				}

				// 连接失败
				fireErrorOccurred(MessageErrorCode.CONNECT_TIMEOUT);
				return false;
			}
		}

		if (key.isValid()) {
			key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
			key.interestOps(key.interestOps() | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		}

		return true;
	}

	/**
	 * 执行数据接收事件。
	 * 
	 * @param key
	 */
	private void receive(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel();

		if (!channel.isConnected()) {
			return;
		}

		int read = 0;

		ByteBuffer readBuffer = ByteBuffer.allocate(this.block + this.block);
		int totalRead = 0;

		do {
			read = 0;
			ByteBuffer buf = ByteBuffer.allocate(16384);

			try {
				read = channel.read(buf);
			} catch (IOException e) {
	//			Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);

				fireSessionClosed();

				try {
					if (null != this.channel)
						this.channel.close();
					if (null != this.selector)
						this.selector.close();
				} catch (IOException ce) {
					Logger.log(NonblockingConnector.class, ce, LogLevel.DEBUG);
				}

				// 不能继续进行数据接收
				this.spinning = false;

				buf = null;
				readBuffer = null;

				return;
			}

			if (read == 0) {
				buf = null;
				break;
			}
			else if (read == -1) {
				fireSessionClosed();

				try {
					this.channel.close();
					this.selector.close();
				} catch (IOException ce) {
					Logger.log(NonblockingConnector.class, ce, LogLevel.DEBUG);
				}

				// 不能继续进行数据接收
				this.spinning = false;

				buf = null;

				return;
			}
			else {
				// 长度计算
				totalRead += read;

				// 合并
				if (buf.position() != 0) {
					buf.flip();
				}
				readBuffer.put(buf);
			}
		} while (read > 0);

		// 就绪
		readBuffer.flip();

		byte[] array = new byte[totalRead];
		readBuffer.get(array);

		try {
			this.process(array);
		} catch (ArrayIndexOutOfBoundsException e) {
			this.session.cacheCursor = 0;
			Logger.log(NonblockingConnector.class, e, LogLevel.WARNING);
		}

		readBuffer.clear();
		readBuffer = null;

		if (key.isValid()) {
			key.interestOps(key.interestOps() | SelectionKey.OP_READ);
		}
	}

	/**
	 * 执行数据发送事件。
	 * 
	 * @param key
	 */
	private void send(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel();

		if (!channel.isConnected()) {
			fireSessionClosed();
			return;
		}

		try {
			if (!this.messages.isEmpty()) {
				// 有消息，进行发送

				Message message = null;
				for (int i = 0, len = this.messages.size(); i < len; ++i) {
					try {
						message = this.messages.remove(0);
					} catch (IndexOutOfBoundsException e) {
						break;
					}

					byte[] skey = this.session.getSecretKey();
					if (null != skey) {
						this.encryptMessage(message, skey);
					}

					ByteBuffer writeBuffer = null;
					if (this.hasDataMark()) {
						byte[] data = message.get();
						byte[] head = this.getHeadMark();
						byte[] tail = this.getTailMark();
						byte[] pd = new byte[data.length + head.length + tail.length];
						System.arraycopy(head, 0, pd, 0, head.length);
						System.arraycopy(data, 0, pd, head.length, data.length);
						System.arraycopy(tail, 0, pd, head.length + data.length, tail.length);
						writeBuffer = ByteBuffer.wrap(pd);
					}
					else {
						writeBuffer = ByteBuffer.wrap(message.get());
					}

					channel.write(writeBuffer);

					writeBuffer = null;

					if (null != this.handler) {
						this.handler.messageSent(this.session, message);
					}
				}
			}
		} catch (IOException e) {
			Logger.log(NonblockingConnector.class, e, LogLevel.WARNING);
		}

		if (key.isValid()) {
			key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
		}
	}

	/**
	 * 解析并处理消息。
	 * 
	 * @param data 接收到的数据数组。
	 */
	private void process(byte[] data) {
		// 根据数据标志获取数据
		if (this.hasDataMark()) {
			LinkedList<byte[]> output = new LinkedList<byte[]>();
			// 数据递归提取
			this.extract(output, data);

			if (!output.isEmpty()) {
				for (byte[] bytes : output) {
					final Message message = new Message(bytes);

					byte[] skey = this.session.getSecretKey();
					if (null != skey) {
						this.decryptMessage(message, skey);
					}

					this.executor.execute(new Runnable() {
						@Override
						public void run() {
							if (null != handler) {
								handler.messageReceived(session, message);
							}
						}
					});
				}
			}
			output = null;
		}
		else {
			Message message = new Message(data);

			byte[] skey = this.session.getSecretKey();
			if (null != skey) {
				this.decryptMessage(message, skey);
			}

			if (null != this.handler) {
				this.handler.messageReceived(this.session, message);
			}
		}
	}

	/**
	 * @deprecated
	 */
	protected void processData(byte[] data) {
		// 根据数据标志获取数据
		if (this.hasDataMark()) {
			byte[] headMark = this.getHeadMark();
			byte[] tailMark = this.getTailMark();

			int cursor = 0;
			int length = data.length;
			boolean head = false;
			boolean tail = false;
			byte[] buf = new byte[this.block];
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
					if (null != this.handler) {
						this.handler.messageReceived(this.session, message);
					}

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
			if (null != this.handler) {
				this.handler.messageReceived(this.session, message);
			}
		}
	}

	/**
	 * 数据提取并输出。
	 * 
	 * @param out 解析之后的输出数据。
	 * @param data 待处理数据。
	 */
	private void extract(final LinkedList<byte[]> output, final byte[] data) {
		final byte[] headMark = this.getHeadMark();
		final byte[] tailMark = this.getTailMark();

		byte[] real = data;
		if (this.session.cacheCursor > 0) {
			real = new byte[this.session.cacheCursor + data.length];
			System.arraycopy(this.session.cache, 0, real, 0, this.session.cacheCursor);
			System.arraycopy(data, 0, real, this.session.cacheCursor, data.length);
			// 重置缓存
			this.session.resetCache();
		}

		int index = 0;
		final int len = real.length;
		int headPos = -1;
		int tailPos = -1;
		int ret = -1;

		ret = compareBytes(headMark, 0, real, index, headMark.length);
		if (0 == ret) {
			// 有头标签
			index = headMark.length;
			// 记录数据位置头
			headPos = index;
			// 判断是否有尾标签，依次计数
			ret = -1;
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
				output.add(outBytes);

				int newLen = len - tailPos - tailMark.length;
				if (newLen > 0) {
					byte[] newBytes = new byte[newLen];
					System.arraycopy(real, tailPos + tailMark.length, newBytes, 0, newLen);

					// 递归
					extract(output, newBytes);
				}
			}
			else {
				// 没有尾标签，仅进行缓存
				if (len + this.session.cacheCursor > this.session.getCacheSize()) {
					// 缓存扩容
					this.session.resetCacheSize(len + this.session.cacheCursor);
				}

				System.arraycopy(real, 0, this.session.cache, this.session.cacheCursor, len);
				this.session.cacheCursor += len;
			}
		}
		else if (-1 == ret){
			// 没有头标签
			// 尝试查找
			ret = -1;
			while (index < len) {
				if (real[index] == headMark[0]) {
					ret = compareBytes(headMark, 0, real, index, headMark.length);
					if (0 == ret) {
						// 找到头标签
						headPos = index;
						break;
					}
					else if (1 == ret) {
						// 越界
						break;
					}
					else {
						// 未找到头标签
						++index;
					}
				}
				else {
					++index;
				}
			}

			if (headPos > 0) {
				// 找到头标签
				byte[] newBytes = new byte[len - headPos];
				System.arraycopy(real, headPos, newBytes, 0, newBytes.length);

				// 递归
				extract(output, newBytes);
			}
			else {
				// 没有找到头标签，尝试判断结束位置
				byte backwardOne = real[len - 1];
				byte backwardTwo = real[len - 2];
				byte backwardThree = real[len - 3];
				int pos = -1;
				int cplen = 0;
				if (headMark[0] == backwardOne) {
					pos = len - 1;
					cplen = 1;
				}
				else if (headMark[0] == backwardTwo && headMark[1] == backwardOne) {
					pos = len - 2;
					cplen = 2;
				}
				else if (headMark[0] == backwardThree && headMark[1] == backwardTwo && headMark[2] == backwardOne) {
					pos = len - 3;
					cplen = 3;
				}

				if (pos >= 0) {
					// 有可能是数据头，进行缓存
					if (cplen + this.session.cacheCursor > this.session.getCacheSize()) {
						// 缓存扩容
						this.session.resetCacheSize(cplen + this.session.cacheCursor);
					}

					System.arraycopy(real, pos, this.session.cache, this.session.cacheCursor, cplen);
					this.session.cacheCursor += cplen;
				}
			}

			/*
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

					if (this.session.cacheCursor + newReal.length > this.session.getCacheSize()) {
						// 重置 cache 大小
						this.session.resetCacheSize(this.session.cacheCursor + newReal.length);
					}
					System.arraycopy(newReal, 0, this.session.cache, this.session.cacheCursor, newReal.length);
					this.session.cacheCursor += newReal.length;
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
					extract(out, newReal);
					return;
				}

				// 更新索引
				++searchIndex;

				// 重置计数
				searchCounts = 0;
			} while (searchIndex < len);
			*/
		}
		else {
			// 数据越界，直接缓存
			if (this.session.cacheCursor + real.length > this.session.getCacheSize()) {
				// 重置 cache 大小
				this.session.resetCacheSize(this.session.cacheCursor + real.length);
			}
			System.arraycopy(real, 0, this.session.cache, this.session.cacheCursor, real.length);
			this.session.cacheCursor += real.length;
		}
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
