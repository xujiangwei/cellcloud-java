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

import java.net.InetSocketAddress;

import net.cellcloud.util.Util;

/** 消息会话描述类。
 * 
 * @author Jiangwei Xu
 */
public class Session {

	private Long id;
	private MessageService service;
	private InetSocketAddress address;

	public Session(MessageService service, InetSocketAddress address) {
		this.id = Math.abs(Util.randomLong());
		this.service = service;
		this.address = address;
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
}
