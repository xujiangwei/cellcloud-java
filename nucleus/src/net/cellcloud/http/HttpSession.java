/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2013 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.http;

import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.cellcloud.common.Message;
import net.cellcloud.common.Session;
import net.cellcloud.util.Clock;

/**
 * HTTP 会话。
 * 
 * @author Jiangwei Xu
 *
 */
public class HttpSession extends Session {

	// 绝对时间时间戳，单位：毫秒
	private long timestamp;
	// 过期时间，单位：毫秒
	private long expires;
	// 心跳
	private long heartbeat;

	// 推送消息队列
	private ConcurrentLinkedQueue<Message> queue;

	/**
	 * 构造函数。
	 * @param address
	 * @param expires
	 */
	public HttpSession(InetSocketAddress address, long expires) {
		super(null, address);
		this.timestamp = System.currentTimeMillis();
		this.expires = expires;
		this.queue = new ConcurrentLinkedQueue<Message>();
		this.heartbeat = this.timestamp;
	}

	/**
	 * 构造函数。
	 * @param id
	 * @param address
	 * @param expires
	 */
	public HttpSession(long id, InetSocketAddress address, long expires) {
		super(id, null, address);
		this.timestamp = System.currentTimeMillis();
		this.expires = expires;
		this.queue = new ConcurrentLinkedQueue<Message>();
		this.heartbeat = this.timestamp;
	}

	/**
	 * 返回会话时间戳。
	 * @return
	 */
	public long getTimestamp() {
		return this.timestamp;
	}

	/**
	 * 返回会话过期时间。
	 * @return
	 */
	public long getExpires() {
		return this.expires;
	}

	/**
	 * 返回最近一次心跳时间戳。
	 * @return
	 */
	public long getHeartbeat() {
		return this.heartbeat;
	}

	/**
	 * 心跳。
	 */
	public void heartbeat() {
		this.heartbeat = Clock.currentTimeMillis();
	}

	@Override
	public final void write(Message message) {
		this.queue.offer(message);
	}

	/**
	 * 返回消息队列。
	 * @return
	 */
	public final Queue<Message> getQueue() {
		return this.queue;
	}
}
