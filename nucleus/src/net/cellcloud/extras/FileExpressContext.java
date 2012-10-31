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

package net.cellcloud.extras;

import java.net.InetSocketAddress;

/** 文件快递上下文。
 * 
 * @author Jiangwei Xu
 */
public class FileExpressContext {

	/// 文件伺服
	protected final static int OP_SERVO = 0;
	/// 文件上载
	protected final static int OP_UPLOAD = 1;
	/// 文件下载
	protected final static int OP_DOWNLOAD = 2;

	/// 成功
	protected final static int EC_SUCCESS = 0;
	/// 网络故障
	protected final static int EC_NETWORK_FAULT = 1;
	/// 未获得操作授权
	protected final static int EC_UNAUTH = 2;
	/// 文件不存在
	protected final static int EC_FILE_NOEXIST = 3;
	/// 因文件大小问题拒绝操作
	protected final static int EC_REJECT_SIZE = 4;
	/// 数据包错误
	protected final static int EC_PACKET_ERROR = 5;


	private ExpressAuthCode authCode;
	private int operate;
	private InetSocketAddress address;
	private String fullPath;

	/** 用于伺服操作的构造函数。
	 */
	public FileExpressContext(ExpressAuthCode authCode) {
		this.authCode = authCode;
		this.operate = OP_SERVO;
	}

	/** 用于上载操作的构造函数。
	 */
	public FileExpressContext(String authCode, InetSocketAddress address,
			String fullPath) {
		this.operate = OP_UPLOAD;
		this.address = address;
		this.fullPath = fullPath;
	}

	/** 用于下载操作的构造函数。
	 */
	public FileExpressContext(String authCode,
			InetSocketAddress address, String fileName, String path) {
		this.operate = OP_DOWNLOAD;
		this.address = address;
	}

	public ExpressAuthCode getAuthCode() {
		return this.authCode;
	}

	public int getOperate() {
		return this.operate;
	}

	public InetSocketAddress getAddress() {
		return this.address;
	}
}
