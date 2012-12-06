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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.MessageHandler;
import net.cellcloud.common.NonblockingAcceptor;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.storage.FileStorage;
import net.cellcloud.util.Util;

/** 文件传输服务类。
 * 
 * @author Jiangwei Xu
 * 
 * Upload 流程：
 * C-S Auth
 * S-C Auth
 * C-S Attr
 * S-C Attr
 * C-S Begin
 * S-C Begin
 * C-S Data *
 * S-C Data-Receipt *
 * C-S End
 * S-C End
 * 
 * Download 流程：
 * C-S Auth
 * S-C Auth
 * C-S Attr
 * S-C Attr
 * C-S Begin
 * S-C Begin
 * C-S Offer
 * S-C Data *
 * C-S Data-Receipt *
 * S-C End
 * C-S End
 */
public final class FileExpress implements MessageHandler, ExpressTaskListener {

	private NonblockingAcceptor acceptor;

	private ConcurrentHashMap<Long, SessionRecord> sessionRecords;
	private ConcurrentHashMap<String, ExpressAuthCode> authCodes;
	private ConcurrentHashMap<String, FileExpressServoContext> servoContexts;

	private ExecutorService executor;
	private FileStorage mainStorage;

	private ArrayList<FileExpressListener> listeners;
	private byte[] listenerMonitor = new byte[0];

	public FileExpress() {
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
			if (!dir.mkdirs()) {
				// 创建目录失败
				return false;
			}
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
	public void startServer(InetSocketAddress address, int maxConnNum,
			FileStorage storage) {
		if (null == this.sessionRecords) {
			this.sessionRecords = new ConcurrentHashMap<Long, SessionRecord>();
		}
		if (null == this.authCodes) {
			this.authCodes = new ConcurrentHashMap<String, ExpressAuthCode>();
		}
		if (null == this.servoContexts) {
			this.servoContexts = new ConcurrentHashMap<String, FileExpressServoContext>();
		}

		// 设置存储器
		this.mainStorage = storage;

		if (null == this.acceptor) {
			this.acceptor = new NonblockingAcceptor();
		}

		// 设置数据掩码
		byte[] head = {0x10, 0x04, 0x11, 0x24};
		byte[] tail = {0x11, 0x24, 0x10, 0x04};
		this.acceptor.defineDataMark(head, tail);

		// 设置最大连接数
		this.acceptor.setMaxConnectNum(maxConnNum);

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
		if (null != this.authCodes) {
			this.authCodes.put(authCode.getCode(), authCode);
		}
	}

	/** 移除授权码。
	 */
	public void removeAuthCode(final String authCodeString) {
		if (null != this.authCodes) {
			this.authCodes.remove(authCodeString);
		}
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
		// 包格式：授权码|文件名|文件操作起始位置
		
		if (packet.getSubsegmentNumber() < 3) {
			return;
		}

		// 获取授权码
		String authCode = Util.bytes2String(packet.getSubsegment(0));

		// 验证 Session
		if (false == checkSession(session, authCode)) {
			reject(session);
			return;
		}

		FileExpressServoContext servoctx = this.servoContexts.get(authCode);
		if (null == servoctx) {
			Logger.e(this.getClass(),
					new StringBuilder("Can not find servo context with '").append(authCode).append("'").toString());
			return;
		}

		// 包格式：授权码|文件名|数据起始位|数据结束位|数据

		String filename = Util.bytes2String(packet.getSubsegment(1));
		long offset = Long.parseLong(Util.bytes2String(packet.getSubsegment(2)));
		byte[] fileData = servoctx.readFile(filename, offset, FileExpressDefinition.CHUNK_SIZE);
		if (null == fileData) {
			Logger.e(this.getClass(),
					new StringBuilder("Read file error - file:'").append(filename).append("'").toString());
			return;
		}

		Packet response = new Packet(FileExpressDefinition.PT_OFFER, 3, 1, 0);
		response.appendSubsegment(packet.getSubsegment(0));
	}

	private void responseAttribute(final Session session, final Packet packet) {
		// 包格式：授权码|文件名
		if (packet.getSubsegmentNumber() != 2) {
			return;
		}

		// 获取授权码
		String authCode = Util.bytes2String(packet.getSubsegment(0));

		// 验证 Session
		if (false == checkSession(session, authCode)) {
			reject(session);
			return;
		}

		FileExpressServoContext servoctx = this.servoContexts.get(authCode);
		if (null == servoctx) {
			return;
		}

		String filename = Util.bytes2String(packet.getSubsegment(1));

		// 包格式：文件名|属性序列
		FileAttribute attr = servoctx.getAttribute(filename);
		byte[] attrseri = attr.serialize();

		Packet response = new Packet(FileExpressDefinition.PT_ATTR, 2, 1, 0);
		response.appendSubsegment(packet.getSubsegment(1));
		response.appendSubsegment(attrseri);
		byte[] data = Packet.pack(response);
		if (null != data) {
			Message message = new Message(data);
			session.write(message);
		}
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

		Packet response = new Packet(FileExpressDefinition.PT_AUTH, 1, 1, 0);

		if (auth) {
			// 添加上下文
			if (!this.servoContexts.contains(authCodeStr)) {
				FileExpressServoContext context = new FileExpressServoContext(eac, this.mainStorage);
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

	private void reject(Session session) {
		Packet packet = new Packet(FileExpressDefinition.PT_REJECT, 10);
		byte[] data = Packet.pack(packet);
		if (data != null) {
			Message message = new Message(data);
			session.write(message);
		}
		else {
			this.acceptor.close(session);
		}
	}

	private boolean checkSession(Session session, String authCode) {
		SessionRecord record = this.sessionRecords.get(session.getId());
		if (null != record) {
			return record.containsAuthCode(authCode);
		}

		return false;
	}

	/** 终端传输。
	 */
	private void interrupt(Session session) {
		SessionRecord record = this.sessionRecords.get(session.getId());
		if (null == record) {
			return;
		}

		// 移除 Session 记录
		this.sessionRecords.remove(session.getId());

		// 根据授权码查找到上下文
		Vector<String> list = record.getAuthCodeList();
		for (String ac : list) {
			FileExpressServoContext ctx = this.servoContexts.get(ac);

			// 关闭上下文里的文件
			Vector<String> filenames = record.getFileNameList();
			for (String fn : filenames) {
				expressError(ctx.getContext(fn));
				ctx.closeFile(fn);
			}
		}
	}

	@Override
	public void sessionCreated(Session session) {
		// Nothing
	}

	@Override
	public void sessionDestroyed(Session session) {
		this.interrupt(session);
	}

	@Override
	public void sessionOpened(Session session) {
		// Nothing
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
		// Nothing
	}

	@Override
	public void errorOccurred(int errorCode, Session session) {
		// TODO 错误处理
	}

	private void maintainSevoContext() {
		Iterator<FileExpressServoContext> iter = this.servoContexts.values().iterator();
		while (iter.hasNext()) {
			FileExpressServoContext ctx = iter.next();
			// 检查剩余时间，并删除过期的上下文
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

		protected boolean containsAuthCode(final String authCode) {
			return this.authCodes.contains(authCode);
		}

		protected Vector<String> getAuthCodeList() {
			return this.authCodes;
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

		protected Vector<String> getFileNameList() {
			return this.fileNames;
		}
	}
}
