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

/**
 * 数据报协议接收器会话类。
 * 
 * @author Ambrose Xu
 * 
 */
public class DatagramAcceptorSession extends Session {

	/** 标记 Session 是否被移除。 */
	protected boolean removed = false;

	/** 最近一次活跃的时间戳。 */
	protected long activeTimestamp;

	/** 映射的键。 */
	protected String mapKey;

	/**
	 * 构造函数。
	 * 
	 * @param service 消息服务。
	 * @param address Session 对应的终端地址。
	 */
	public DatagramAcceptorSession(MessageService service, InetSocketAddress address) {
		super(service, address);
		this.activeTimestamp = System.currentTimeMillis();
	}

}
