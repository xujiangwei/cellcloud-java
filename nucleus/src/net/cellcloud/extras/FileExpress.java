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

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.cellcloud.common.Message;
import net.cellcloud.common.MessageHandler;
import net.cellcloud.common.NonblockingAcceptor;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.util.Util;

/** 文件传输服务类。
 * @author Jiangwei Xu
 */
public final class FileExpress extends MessageHandler implements ExpressTaskListener {

	private NonblockingAcceptor acceptor;

	private ConcurrentHashMap<Long, SessionRecord> sessionRecords;
	private ConcurrentHashMap<String, ExpressAuthCode> authCodes;
	private ConcurrentHashMap<String, FileExpressServoContext> servoContexts;

	private ExecutorService executor;

	private ArrayList<FileExpressListener> listeners;
	private byte[] listenerMonitor = new byte[0];

	public FileExpress() {
		this.sessionRecords = new ConcurrentHashMap<Long, SessionRecord>();
		this.authCodes = new ConcurrentHashMap<String, ExpressAuthCode>();
		this.servoContexts = new ConcurrentHashMap<String, FileExpressServoContext>();
		this.executor = Executors.newCachedThreadPool();
	}

	/** 添加监听器。
	 */
	public void addListener(FileExpressListener listener) {
		synchronized (this.listenerMonitor) {
			if (null == this.listeners) {
				this.listeners = new ArrayList<FileExpressListener>();
			}

			if (!this.listeners.contains(listener)) {
				this.listeners.add(listener);
			}
		}
	}

	/** 移除监听器。
	 */
	public void removeListener(FileExpressListener listener) {
		synchronized (this.listenerMonitor) {
			if (null != this.listeners) {
				this.listeners.remove(listener);
			}
		}
	}

	/** 下载文件。
	 */
	public boolean download(InetSocketAddress address, final String fileName,
			final String fileLocalPath, final String authCode) {
		String localPath = fileLocalPath;
		if (!fileLocalPath.endsWith("\\") && !fileLocalPath.endsWith("/")) {
			localPath += File.separator;
		}

		// 检查并创建目录
		File dir = new File(localPath);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		dir = null;

		// 创建上下文
		FileExpressContext ctx = new FileExpressContext(authCode, address, fileName, localPath);

		// 创建任务
		FileExpressTask task = new FileExpressTask(ctx);
		task.setListener(this);

		// 提交任务执行
		this.executor.execute(task);

		return true;
	}

	/** 上传文件。
	 */
	public boolean upload(InetSocketAddress address, final String fullPath,
			final String authCode) {

		// 创建上下文
		FileExpressContext ctx = new FileExpressContext(authCode, address, fullPath);

		// 创建任务
		FileExpressTask task = new FileExpressTask(ctx);
		task.setListener(this);

		// 提交任务执行
		this.executor.execute(task);

		return true;
	}

	/** 启动为服务器模式。
	 */
	public void startServer(InetSocketAddress address) {
		if (null == this.acceptor) {
			this.acceptor = new NonblockingAcceptor();
		}

		// 设置数据掩码
		byte[] head = {0x10, 0x04, 0x11, 0x24};
		byte[] tail = {0x11, 0x24, 0x10, 0x04};
		this.acceptor.defineDataMark(head, tail);

		// 设置最大连接数
		this.acceptor.setMaxConnectNum(32);

		// 设置处理器
		this.acceptor.setHandler(this);

		// 绑定服务端口
		this.acceptor.bind(address);
	}

	/** 关闭服务器模式。
	 */
	public void stopServer() {
		if (null == this.acceptor) {
			return;
		}

		this.acceptor.unbind();

		// 清空授权码
		this.authCodes.clear();
	}

	/** 添加授权码。
	 */
	public void addAuthCode(ExpressAuthCode authCode) {
		// TODO
	}

	/** 移除授权码。
	 */
	public void removeAuthCode(final String authCodeString) {
		// TODO
	}

	private void interpret(final Session session, final Packet packet) {
		byte[] tag = packet.getTag();

		if (tag[0] == FileExpressDefinition.PT_DATA[0] && tag[1] == FileExpressDefinition.PT_DATA[1]
			&& tag[2] == FileExpressDefinition.PT_DATA[2] && tag[3] == FileExpressDefinition.PT_DATA[3]) {
			responseData(session, packet);
		}
		else if (tag[0] == FileExpressDefinition.PT_DATA_RECEIPT[0] && tag[1] == FileExpressDefinition.PT_DATA_RECEIPT[1]
			&& tag[2] == FileExpressDefinition.PT_DATA_RECEIPT[2] && tag[3] == FileExpressDefinition.PT_DATA_RECEIPT[3]) {
			responseDataReceipt(session, packet);
		}
		else if (tag[0] == FileExpressDefinition.PT_BEGIN[0] && tag[1] == FileExpressDefinition.PT_BEGIN[1]
			&& tag[2] == FileExpressDefinition.PT_BEGIN[2] && tag[3] == FileExpressDefinition.PT_BEGIN[3]) {
			responseBegin(session, packet);
		}
		else if (tag[0] == FileExpressDefinition.PT_END[0] && tag[1] == FileExpressDefinition.PT_END[1]
			&& tag[2] == FileExpressDefinition.PT_END[2] && tag[3] == FileExpressDefinition.PT_END[3]) {
			responseEnd(session, packet);
		}
		else if (tag[0] == FileExpressDefinition.PT_OFFER[0] && tag[1] == FileExpressDefinition.PT_OFFER[1]
			&& tag[2] == FileExpressDefinition.PT_OFFER[2] && tag[3] == FileExpressDefinition.PT_OFFER[3]) {
			responseOffer(session, packet);
		}
		else if (tag[0] == FileExpressDefinition.PT_ATTR[0] && tag[1] == FileExpressDefinition.PT_ATTR[1]
			&& tag[2] == FileExpressDefinition.PT_ATTR[2] && tag[3] == FileExpressDefinition.PT_ATTR[3]) {
			responseAttribute(session, packet);
		}
		else if (tag[0] == FileExpressDefinition.PT_AUTH[0] && tag[1] == FileExpressDefinition.PT_AUTH[1]
			&& tag[2] == FileExpressDefinition.PT_AUTH[2] && tag[3] == FileExpressDefinition.PT_AUTH[3]) {
			authenticate(session, packet);
		}
	}

	private void responseData(final Session session, final Packet packet) {
		
	}

	private void responseDataReceipt(final Session session, final Packet packet) {
		
	}

	private void responseBegin(final Session session, final Packet packet) {
		
	}

	private void responseEnd(final Session session, final Packet packet) {
		
	}

	private void responseOffer(final Session session, final Packet packet) {
		
	}

	private void responseAttribute(final Session session, final Packet packet) {
		
	}

	private void authenticate(final Session session, final Packet packet) {
		// 包格式：授权码
		byte[] authCode = packet.getSubsegment(0);

		if (null == authCode) {
			// 包格式错误
			this.acceptor.close(session);
			return;
		}

		boolean auth = false;

		String authCodeStr = Util.bytes2String(authCode);
		ExpressAuthCode eac = this.authCodes.get(authCodeStr);
		if (null != eac) {
			auth = true;

			// 查询会话记录
			SessionRecord record = this.sessionRecords.get(session.getId());
			if (null == record) {
				record = new SessionRecord();
				this.sessionRecords.put(session.getId(), record);
			}

			record.addAuthCode(authCodeStr);
		}

		// 包格式：授权码能力描述

		Packet response = new Packet(FileExpressDefinition.PT_AUTH, 1);

		if (auth) {
			// 添加上下文
			if (!this.servoContexts.contains(authCodeStr)) {
				FileExpressServoContext context = new FileExpressServoContext(eac);
				this.servoContexts.put(authCodeStr, context);
			}

			byte[] cap = null;
			switch (eac.getAuth()) {
			case ExpressAuthCode.AUTH_WRITE:
				cap = FileExpressDefinition.AUTH_WRITE;
				break;
			case ExpressAuthCode.AUTH_READ:
				cap = FileExpressDefinition.AUTH_READ;
				break;
			default:
				cap = FileExpressDefinition.AUTH_NOACCESS;
				break;
			}

			response.appendSubsegment(cap);
		}
		else {
			response.appendSubsegment(FileExpressDefinition.AUTH_NOACCESS);
		}

		// 发送响应包
		byte[] data = Packet.pack(response);
		Message message = new Message(data);
		session.write(message);

		// 检查并维护伺服上下文
		maintainSevoContext();
	}

	private void interrupt(Session session) {
		
	}

	@Override
	public void sessionCreated(Session session) {

	}

	@Override
	public void sessionDestroyed(Session session) {
		this.interrupt(session);
	}

	@Override
	public void sessionOpened(Session session) {

	}

	@Override
	public void sessionClosed(Session session) {
		this.interrupt(session);
	}

	@Override
	public void messageReceived(Session session, Message message) {
		Packet packet = Packet.unpack(message.get());
		if (null != packet) {
			this.interpret(session, packet);
		}
	}

	@Override
	public void messageSent(Session session, Message message) {

	}

	@Override
	public void errorOccurred(int errorCode, Session session) {

	}

	private void maintainSevoContext() {
		Iterator<FileExpressServoContext> iter = this.servoContexts.values().iterator();
		while (iter.hasNext()) {
			FileExpressServoContext ctx = iter.next();
			if (ctx.getRemainingTime() <= 0) {
				iter.remove();
			}
		}
	}

	@Override
	public void expressStarted(FileExpressContext context) {
		synchronized (this.listenerMonitor) {
			if (null == this.listeners) {
				return;
			}

			FileExpressListener listener = null;
			for (int i = 0, size = this.listeners.size(); i < size; ++i) {
				listener = this.listeners.get(i);
				listener.expressStarted(context);
			}
		}
	}

	@Override
	public void expressCompleted(FileExpressContext context) {
		synchronized (this.listenerMonitor) {
			if (null == this.listeners) {
				return;
			}

			FileExpressListener listener = null;
			for (int i = 0, size = this.listeners.size(); i < size; ++i) {
				listener = this.listeners.get(i);
				listener.expressCompleted(context);
			}
		}
	}

	@Override
	public void expressProgress(FileExpressContext context) {
		synchronized (this.listenerMonitor) {
			if (null == this.listeners) {
				return;
			}

			FileExpressListener listener = null;
			for (int i = 0, size = this.listeners.size(); i < size; ++i) {
				listener = this.listeners.get(i);
				listener.expressProgress(context);
			}
		}
	}

	@Override
	public void expressError(FileExpressContext context) {
		synchronized (this.listenerMonitor) {
			if (null == this.listeners) {
				return;
			}

			FileExpressListener listener = null;
			for (int i = 0, size = this.listeners.size(); i < size; ++i) {
				listener = this.listeners.get(i);
				listener.expressError(context);
			}
		}
	}

	/** 会话工作记录
	*/
	protected class SessionRecord {

		private Vector<String> authCodes;
		private Vector<String> fileNames;

		protected SessionRecord() {
			this.authCodes = new Vector<String>();
			this.fileNames = new Vector<String>();
		}

		protected void addAuthCode(final String authCode) {
			if (this.authCodes.contains(authCode)) {
				return;
			}

			this.authCodes.add(authCode);
		}

		protected void removeAuthCode(final String authCode) {
			if (this.authCodes.contains(authCode)) {
				this.authCodes.remove(authCode);
			}
		}

		protected void addFileName(final String fileName) {
			if (this.fileNames.contains(fileName)) {
				return;
			}

			this.fileNames.add(fileName);
		}

		protected void removeFileName(final String fileName) {
			if (this.fileNames.contains(fileName)) {
				this.fileNames.remove(fileName);
			}
		}
	}
}
