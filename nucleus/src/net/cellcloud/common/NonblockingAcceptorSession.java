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

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.util.LinkedList;

import net.cellcloud.util.Clock;

/**
 * 非阻塞网络接收器会话。
 * 
 * @author Ambrose Xu
 * 
 */
public class NonblockingAcceptorSession extends Session {

	/** 数据块大小。 */
	private int block;

	/** 最近一次读数据时间。 */
	protected long readTime = 0;
	/** 最近一次写数据时间。 */
	protected long writeTime = 0;

	/** 待发送消息列表。 */
	private LinkedList<Message> messages = new LinkedList<Message>();

	protected SelectionKey selectionKey = null;

	/** 当前会话对应的 Socket 。 */
	protected Socket socket = null;

	/** 所属的工作器。 */
	protected NonblockingAcceptorWorker worker = null;

	/**
	 * 构造函数。
	 * 
	 * @param service 消息服务。
	 * @param address 对应的地址。
	 * @param block 数据块大小。
	 */
	public NonblockingAcceptorSession(MessageService service, InetSocketAddress address, int block) {
		super(service, address);
		this.block = block;
		this.readTime = Clock.currentTimeMillis();
		this.writeTime = Clock.currentTimeMillis();
	}

	/**
	 * 得到缓存块大小。
	 * 
	 * @return 返回缓存块大小。
	 */
	public int getBlock() {
		return this.block;
	}

	/**
	 * 添加消息到发送缓存。
	 * 
	 * @param message 待添加的消息。
	 */
	protected void addMessage(Message message) {
		synchronized (this.messages) {
			this.messages.add(message);
		}
	}

	/**
	 * 消息队列是否为空。
	 * 
	 * @return 如果消息队列为空则返回 <code>true</code> 。
	 */
	protected boolean isMessageEmpty() {
		synchronized (this.messages) {
			return this.messages.isEmpty();
		}
	}

	/**
	 * 将消息队列里的第一条消息出队。
	 * 
	 * @return 返回消息队列里的第一条消息。
	 */
	protected Message pollMessage() {
		synchronized (this.messages) {
			return this.messages.poll();
		}
	}

}
