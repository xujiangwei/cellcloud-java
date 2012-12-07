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

package net.cellcloud.extras.express;

import java.io.File;
import java.net.InetSocketAddress;

/** 文件快递上下文。
 * 
 * @author Jiangwei Xu
 */
public class FileExpressContext {

	/// 成功
	public final static int EC_SUCCESS = 0;
	/// 网络故障
	public final static int EC_NETWORK_FAULT = 1;
	/// 未获得操作授权
	public final static int EC_UNAUTH = 2;
	/// 文件不存在
	public final static int EC_FILE_NOEXIST = 3;
	/// 因文件大小问题拒绝操作
	public final static int EC_REJECT_SIZE = 4;
	/// 数据包错误
	public final static int EC_PACKET_ERROR = 5;
	/// 存储器故障
	public final static int EC_STORAGE_FAULT = 6;
	/// 操作被中止
	public final static int EC_ABORT = 9;

	/// 文件上载
	protected final static int OP_UPLOAD = 1;
	/// 文件下载
	protected final static int OP_DOWNLOAD = 2;

	protected ExpressAuthCode authCode;
	private int operate;
	private InetSocketAddress address;
	private String fullPath;
	private String filePath;
	private String fileName;
	private FileAttribute attribute;

	protected long bytesLoaded = 0;
	protected long bytesTotal = 0;

	protected int errorCode = EC_SUCCESS;

	/** 用于伺服操作的构造函数。
	 */
	public FileExpressContext(ExpressAuthCode authCode,
			String path, String file, int operate) {
		this.authCode = authCode;
		this.operate = operate;

		this.fileName = file;
		this.filePath = new String(path);
		this.fullPath = new String(path);

		if (!path.endsWith("\\")
			&& !path.endsWith("/")) {
			this.filePath += File.separator;
			this.fullPath += File.separator;
		}
		this.fullPath += file;
	}

	/** 用于上载操作的构造函数。
	 */
	public FileExpressContext(String authCode, InetSocketAddress address,
			String fullPath) {
		this.operate = OP_UPLOAD;
		this.address = address;
		this.fullPath = fullPath;
		this.authCode = new ExpressAuthCode(authCode);

		int index = this.fullPath.lastIndexOf("/");
		if (index < 0) {
			index = this.fullPath.lastIndexOf("\\");
		}

		if (index < 0) {
			this.filePath = "";
			this.fileName = fullPath;
		}
		else if (index == 0) {
			this.filePath = fullPath.substring(0, 1);
			this.fileName = fullPath.substring(1, fullPath.length());
		}
		else {
			this.filePath = fullPath.substring(0, index + 1);
			this.fileName = fullPath.substring(index + 1, fullPath.length());
		}
	}

	/** 用于下载操作的构造函数。
	 */
	public FileExpressContext(String authCode,
			InetSocketAddress address, String fileName, String path) {
		this.operate = OP_DOWNLOAD;
		this.address = address;

		this.fileName = fileName;
		this.filePath = new String(path);
		this.fullPath = new String(path);
		if (!path.endsWith("\\")
			&& !path.endsWith("/")) {
			this.filePath += File.separator;
			this.fullPath += File.separator;
		}
		this.fullPath += fileName;

		this.authCode = new ExpressAuthCode(authCode);
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

	public String getFullPath() {
		return this.fullPath;
	}

	public String getFileName() {
		return this.fileName;
	}

	public String getFilePath() {
		return this.filePath;
	}

	public FileAttribute getAttribute() {
		return this.attribute;
	}

	public long getBytesLoaded() {
		return this.bytesLoaded;
	}

	public long getBytesTotal() {
		return this.bytesTotal;
	}

	public int getErrorCode() {
		return this.errorCode;
	}

	protected void setAttribute(FileAttribute attr) {
		this.attribute = attr;
	}
}
