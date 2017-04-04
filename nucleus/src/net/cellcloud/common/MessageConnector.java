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
 * 消息连接器。
 * 
 * @author Ambrose Xu
 * 
 */
public interface MessageConnector {

	/**
	 * 连接远端的消息接收器。
	 * 
	 * @param address 指定连接地址。
	 * @return 如果连接请求发出返回 <code>true</code> 。
	 */
	public boolean connect(InetSocketAddress address);

	/**
	 * 关闭已建立的连接。
	 */
	public void disconnect();

	/**
	 * 设置连接超时值。
	 * 
	 * @param timeout 指定以毫秒为单位的超时时间。
	 */
	public void setConnectTimeout(long timeout);

	/**
	 * 设置数据缓存块大小。
	 * 
	 * @param size 指定以字节为单位的缓存块大小。
	 */
	public void setBlockSize(int size);

	/**
	 * 获得会话实例。
	 * 
	 * @return 返回会话实例。
	 */
	public Session getSession();

}
