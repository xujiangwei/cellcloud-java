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

/** 消息服务处理监听器。
 * 
 * @author Jiangwei Xu
 */
public abstract class MessageHandler {

	/// 未知的错误类型。
	public static final int EC_UNKNOWN = 0;
	/// 无效的网络地址。
	public static final int EC_ADDRESS_INVALID = 1;
	/// 错误的状态。
	public static final int EC_STATE_ERROR = 2;
	/// Socket 函数发生错误。
	public static final int EC_SOCK_FAILED = 3;
	/// 绑定服务时发生错误。
	public static final int EC_BIND_FAILED = 4;
	/// 监听连接时发生错误。
	public static final int EC_LISTEN_FAILED = 5;
	/// Accept 发生错误。
	public static final int EC_ACCEPT_FAILED = 6;
	/// 写入数据时发生错误。
	public static final int EC_WRITE_FAILED = 7;
	/// 连接超时
	public static final int EC_CONNECT_TIMEOUT = 9;


	/** 创建连接会话。
	*/
	public abstract void sessionCreated(Session session);
	
	/** 销毁连接会话。
	*/
	public abstract void sessionDestroyed(Session session);

	/** 开启连接会话。
	*/
	public abstract void sessionOpened(Session session);

	/** 关闭连接会话。
	*/
	public abstract void sessionClosed(Session session);

	/** 接收到消息。
	*/
	public abstract void messageReceived(Session session, Message message);

	/** 消息已发送。
	*/
	public abstract void messageSent(Session session, Message message);

	/** 发生错误。
	*/
	public abstract void errorOccurred(int errorCode, Session session);
}
