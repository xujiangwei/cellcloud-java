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

/** 消息错误码。
 * 
 * @author Jiangwei Xu
 */
public final class MessageErrorCode {

	/// 未知的错误类型。
	public static final int UNKNOWN = 100;

	/// 无效的网络地址。
	public static final int ADDRESS_INVALID = 101;
	/// 错误的状态。
	public static final int STATE_ERROR = 102;

	/// Socket 函数发生错误。
	public static final int SOCKET_FAILED = 200;
	/// 绑定服务时发生错误。
	public static final int BIND_FAILED = 201;
	/// 监听连接时发生错误。
	public static final int LISTEN_FAILED = 202;
	/// Accept 发生错误。
	public static final int ACCEPT_FAILED = 203;

	/// 连接失败。
	public static final int CONNECT_FAILED = 300;
	/// 连接超时。
	public static final int CONNECT_TIMEOUT = 301;

	/// 写数据超时。
	public static final int WRITE_TIMEOUT = 401;
	/// 读数据超时。
	public static final int READ_TIMEOUT = 402;
	/// 写入数据时发生错误。
	public static final int WRITE_FAILED = 403;
	/// 读取数据时发生错误。
	public static final int READ_FAILED = 404;

	private MessageErrorCode() {
	}
}
