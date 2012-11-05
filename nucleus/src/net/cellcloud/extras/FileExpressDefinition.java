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

/** 定义辅助类。
 * 
 * @author Jiangwei Xu
 */
public final class FileExpressDefinition {

	// 默认端口
	protected final static int PORT = 7100;

	// 文件块大小
	protected final static int CHUNK_SIZE = 1048576;
	// 文件拆分大小
	protected final static int SPLIT_SIZE = 4194304;

	// 文件数据区大小
	protected final static int FILEDATA_SIZE = 7168;

	protected final static int CACHE_SIZE = 8192;

	// 文件权限
	protected final static byte[] AUTH_WRITE = {'w'};
	protected final static byte[] AUTH_READ = {'r'};
	protected final static byte[] AUTH_NOACCESS = {'n','o'};

	// 数据包标签

	// 权限校验
	protected final static byte[] PT_AUTH = {'A', 'U', 'T', 'H'};
	// 拒绝操作
	protected final static byte[] PT_REJECT = {'R', 'E', 'J', 'T'};
	// 文件属性
	protected final static byte[] PT_ATTR = {'A', 'T', 'T', 'R'};
	// 文件数据传输开始
	protected final static byte[] PT_BEGIN = {'B', 'E', 'G', 'N'};
	// 文件数据传输
	protected final static byte[] PT_DATA = {'D', 'A', 'T', 'A'};
	// 文件数据传输确认
	protected final static byte[] PT_DATA_RECEIPT = {'D', 'A', 'R', 'E'};
	// 文件数据接收通知
	protected final static byte[] PT_OFFER = {'O', 'F', 'E', 'R'};
	// 文件数据传输结束
	protected final static byte[] PT_END = {'F', 'E', 'N', 'D'};

	private FileExpressDefinition() {
	}
}
