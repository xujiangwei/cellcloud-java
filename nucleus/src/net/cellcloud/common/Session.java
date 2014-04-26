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

import net.cellcloud.util.Utils;

/** 消息会话描述类。
 * 
 * @author Jiangwei Xu
 */
public class Session {

	private Long id;
	private MessageService service;
	private InetSocketAddress address;

	protected byte[] cache;
	protected int cacheSize;
	protected int cacheCursor;

	public Session(MessageService service, InetSocketAddress address) {
		this.id = Math.abs(Utils.randomLong());
		this.service = service;
		this.address = address;

		this.cacheSize = 1024;
		this.cache = new byte[this.cacheSize];
		this.cacheCursor = 0;
	}

	public Session(long id, MessageService service, InetSocketAddress address) {
		this.id = id;
		this.service = service;
		this.address = address;

		this.cacheSize = 1024;
		this.cache = new byte[this.cacheSize];
		this.cacheCursor = 0;
	}

	/** 返回会话 ID 。
	 */
	public Long getId() {
		return this.id;
	}

	/** 返回消息服务实例。
	 */
	public MessageService getService() {
		return this.service;
	}

	/** 返回会话的网络地址。
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/** 向该会话写消息。
	 */
	public void write(Message message) {
		this.service.write(this, message);
	}

	protected void resetCacheSize(int newSize) {
		if (newSize <= this.cacheSize) {
			return;
		}

		this.cacheSize = newSize;

		if (this.cacheCursor > 0) {
			byte[] cur = new byte[this.cacheCursor];
			System.arraycopy(this.cache, 0, cur, 0, this.cacheCursor);
			this.cache = new byte[newSize];
			System.arraycopy(cur, 0, this.cache, 0, this.cacheCursor);
		}
		else {
			this.cache = new byte[newSize];
		}
	}
}
