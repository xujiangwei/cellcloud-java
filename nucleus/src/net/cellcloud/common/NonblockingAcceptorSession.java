/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (www.cellcloud.net)

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

/** 非阻塞网络接收器会话。
 * 
 * @author Jiangwei Xu
 */
public class NonblockingAcceptorSession extends Session {

	private int block;

	protected long readTime = 0;
	protected long writeTime = 0;

	// 待发送消息列表
	private LinkedList<Message> messages = new LinkedList<Message>();

	protected SelectionKey selectionKey = null;
	protected Socket socket = null;

	// 所属的工作线程
	protected NonblockingAcceptorWorker worker = null;

	/** 构造函数。
	 */
	public NonblockingAcceptorSession(MessageService service,
			InetSocketAddress address, int block) {
		super(service, address);
		this.block = block;
		this.readTime = Clock.currentTimeMillis();
		this.writeTime = Clock.currentTimeMillis();
	}

	/** 返回缓存大小。 */
	public int getBlock() {
		return this.block;
	}

	protected void addMessage(Message message) {
		synchronized (this.messages) {
			this.messages.add(message);
		}
	}

	protected boolean isMessageEmpty() {
		synchronized (this.messages) {
			return this.messages.isEmpty();
		}
	}

	protected Message pollMessage() {
		synchronized (this.messages) {
			return this.messages.poll();
		}
	}
}
