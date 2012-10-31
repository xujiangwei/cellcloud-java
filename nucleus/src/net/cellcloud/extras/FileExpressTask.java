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

import java.nio.ByteBuffer;

import net.cellcloud.common.Message;
import net.cellcloud.common.MessageHandler;
import net.cellcloud.common.Session;

/** 文件传输任务类。
 * 
 * @author Jiangwei Xu
 */
public final class FileExpressTask extends MessageHandler implements Runnable {

	/// 未知状态
	protected final static int EXPRESS_STATE_UNKNOWN = 0;
	/// 通信连接丢失状态
	protected final static int EXPRESS_STATE_LOST = 1;
	/// 属性确认状态
	protected final static int EXPRESS_STATE_ATTR = 2;
	/// 准备状态
	protected final static int EXPRESS_STATE_PREPARE = 3;
	/// 开始状态
	protected final static int EXPRESS_STATE_BEGIN = 4;
	/// 数据传输状态
	protected final static int EXPRESS_STATE_DATA = 5;
	/// 结束状态
	protected final static int EXPRESS_STATE_END = 6;
	/// 未通过授权检查
	protected final static int EXPRESS_STATE_UNAUTH = 9;
	/// 任务完成状态
	protected final static int EXPRESS_STATE_EXIT = 11;

	private FileExpressContext context;
	private int state;
	private ByteBuffer dataCache;

	public FileExpressTask(FileExpressContext context) {
		this.context = context;
		this.state = EXPRESS_STATE_UNKNOWN;
		this.dataCache = null;
	}

	@Override
	public void run() {
		switch (this.context.getOperate()) {
		case FileExpressContext.OP_DOWNLOAD:
			download();
			break;
		case FileExpressContext.OP_UPLOAD:
			upload();
			break;
		default:
			break;
		}
	}

	public void abort() {
		
	}

	private void download() {
		this.state = EXPRESS_STATE_LOST;

		if (null == this.dataCache) {
			this.dataCache = ByteBuffer.allocate(FileExpressDefinition.CACHE_SIZE);
		}

		
	}

	private void upload() {
		
	}

	@Override
	public void sessionCreated(Session session) {
	}

	@Override
	public void sessionDestroyed(Session session) {
	}

	@Override
	public void sessionOpened(Session session) {
	}

	@Override
	public void sessionClosed(Session session) {
	}

	@Override
	public void messageReceived(Session session, Message message) {
	}

	@Override
	public void messageSent(Session session, Message message) {
	}

	@Override
	public void errorOccurred(int errorCode, Session session) {
	}
}
