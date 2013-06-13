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

package net.cellcloud.extras.express;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.MessageHandler;
import net.cellcloud.common.NonblockingAcceptor;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.exception.StorageException;
import net.cellcloud.storage.FileStorage;
import net.cellcloud.storage.ResultSet;
import net.cellcloud.util.Utils;

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
		this.executor = null;
	}

	/** 设置线程池。
	 */
	public void setThreadPool(ExecutorService executorService) {
		this.executor = executorService;
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
		if (null == this.executor) {
			this.executor = Executors.newCachedThreadPool();
		}
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
		if (null == this.executor) {
			this.executor = Executors.newCachedThreadPool();
		}
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
		// 包格式：授权码|文件名|数据起始位|数据结束位|数据
		if (packet.getSubsegmentCount() != 5) {
			Logger.w(this.getClass(), "Packet format error in responseData()");
			return;
		}

		// 获取授权码
		String authCode = Utils.bytes2String(packet.getSubsegment(0));

		// 验证 Session
		if (false == checkSession(session, authCode)) {
			reject(session);
			return;
		}

		String filename = Utils.bytes2String(packet.getSubsegment(1));
		long start = Long.parseLong(Utils.bytes2String(packet.getSubsegment(2)));
		long end = Long.parseLong(Utils.bytes2String(packet.getSubsegment(3)));
		byte[] fileData = packet.getSubsegment(4);

		// 保存数据
		SessionRecord record = this.sessionRecords.get(session.getId());
		if (record.writeFile(filename, fileData, start, end - start) > 0) {
			// 包格式：文件名|数据进度
			Packet response = new Packet(FileExpressDefinition.PT_DATA_RECEIPT, 6, 1, 0);
			response.appendSubsegment(packet.getSubsegment(1));
			response.appendSubsegment(packet.getSubsegment(3));
			byte[] data = Packet.pack(packet);
			Message message = new Message(data);
			session.write(message);

			FileExpressContext ctx = record.getContext(filename);
			ctx.bytesLoaded = end;
			this.expressProgress(ctx);
		}
		else {
			FileExpressContext ctx = record.getContext(filename);
			ctx.errorCode = FileExpressContext.EC_STORAGE_FAULT;
			this.expressError(ctx);

			// 服务器出错，关闭与该 Session 的连接
			this.reject(session);
		}
	}

	private void responseDataReceipt(final Session session, final Packet packet) {
		// 包格式：授权码|文件名|新数据进度
		if (packet.getSubsegmentCount() < 3) {
			Logger.w(this.getClass(), "Packet format error in responseDataReceipt()");
			return;
		}

		// 获取授权码
		String authCode = Utils.bytes2String(packet.getSubsegment(0));

		// 验证 Session
		if (false == checkSession(session, authCode)) {
			reject(session);
			return;
		}

		SessionRecord record = this.sessionRecords.get(session.getId());

		String filename = Utils.bytes2String(packet.getSubsegment(1));
		long offset = Long.parseLong(Utils.bytes2String(packet.getSubsegment(2)));
		// 读文件
		byte[] fileData = record.readFile(filename, offset, FileExpressDefinition.CHUNK_SIZE);
		if (null != fileData) {
			long end = offset + fileData.length;

			// 包格式：授权码|文件名|数据起始位|数据结束位|数据
			Packet response = new Packet(FileExpressDefinition.PT_DATA, 5, 1, 0);
			response.appendSubsegment(packet.getSubsegment(0));
			response.appendSubsegment(packet.getSubsegment(1));
			response.appendSubsegment(Utils.string2Bytes(Long.toString(offset)));
			response.appendSubsegment(Utils.string2Bytes(Long.toString(end)));
			response.appendSubsegment(fileData);
			byte[] data = Packet.pack(response);
			Message message = new Message(data);
			session.write(message);

			FileExpressContext ctx = record.getContext(filename);
			ctx.bytesLoaded = end;
			this.expressProgress(ctx);
		}
		else {
			// 文件发送完毕

			FileExpressContext ctx = record.getContext(filename);

			// 包格式：文件名|文件长度
			Packet response = new Packet(FileExpressDefinition.PT_END, 7, 1, 0);
			response.appendSubsegment(packet.getSubsegment(0));
			response.appendSubsegment(Utils.string2Bytes(Long.toString(ctx.getAttribute().size())));
			byte[] data = Packet.pack(response);
			Message message = new Message(data);
			session.write(message);
		}
	}

	private void responseBegin(final Session session, final Packet packet) {
		// 包格式：授权码|文件名|文件长度|操作
		if (packet.getSubsegmentCount() < 4) {
			Logger.w(this.getClass(), "Packet format error in responseBegin()");
			return;
		}

		// 获取授权码
		String authCode = Utils.bytes2String(packet.getSubsegment(0));

		// 验证 Session
		if (false == checkSession(session, authCode)) {
			reject(session);
			return;
		}

		SessionRecord record = this.sessionRecords.get(session.getId());
		FileExpressServoContext ctx = this.servoContexts.get(authCode);

		// 准备文件
		String filename = Utils.bytes2String(packet.getSubsegment(1));
		long fileSize = Long.parseLong(Utils.bytes2String(packet.getSubsegment(2)));
		int operate = Integer.parseInt(Utils.bytes2String(packet.getSubsegment(3)));
		FileExpressContext fec = record.prepareFile(authCode, ctx.getAttribute(filename), filename, fileSize, operate);

		// 包格式：文件名|文件长度
		Packet response = new Packet(FileExpressDefinition.PT_BEGIN, 3, 1, 0);
		response.appendSubsegment(packet.getSubsegment(1));
		response.appendSubsegment(packet.getSubsegment(2));
		byte[] data = Packet.pack(response);
		Message message = new Message(data);
		session.write(message);

		// 回调-开始
		this.expressStarted(fec);
	}

	private void responseEnd(final Session session, final Packet packet) {
		// 包结构：授权码|文件名|文件长度|操作
		if (packet.getSubsegmentCount() < 4) {
			Logger.w(this.getClass(), "Packet format error in responseEnd()");
			return;
		}

		// 获取授权码
		String authCode = Utils.bytes2String(packet.getSubsegment(0));

		// 验证 Session
		if (false == checkSession(session, authCode)) {
			reject(session);
			return;
		}

		FileExpressServoContext servoctx = this.servoContexts.get(authCode);
		if (null == servoctx) {
			Logger.e(this.getClass(),
					new StringBuilder("Can not find servo context with '").append(authCode).append("'").toString());
			reject(session);
			return;
		}

		// 提取文件名
		String filename = Utils.bytes2String(packet.getSubsegment(1));

		// 关闭对应的记录
		SessionRecord record = this.sessionRecords.get(session.getId());
		FileExpressContext ctx = record.closeFile(filename);

		// 如果是上传，则向客户端回包
		if (ctx.getOperate() == FileExpressContext.OP_UPLOAD) {
			// 包格式：文件名|文件长度
			Packet response = new Packet(FileExpressDefinition.PT_END, 7, 1, 0);
			response.appendSubsegment(packet.getSubsegment(1));
			response.appendSubsegment(packet.getSubsegment(2));
			byte[] data = Packet.pack(response);
			Message message = new Message(data);
			session.write(message);
		}

		// 回调-完成
		this.expressCompleted(ctx);
	}

	private void responseOffer(final Session session, final Packet packet) {
		// 包格式：授权码|文件名|文件操作起始位置
		if (packet.getSubsegmentCount() < 3) {
			Logger.w(this.getClass(), "Packet format error in responseOffer()");
			return;
		}

		// 获取授权码
		String authCode = Utils.bytes2String(packet.getSubsegment(0));

		// 验证 Session
		if (false == checkSession(session, authCode)) {
			reject(session);
			return;
		}

		SessionRecord record = this.sessionRecords.get(session.getId());

		// 包格式：授权码|文件名|数据起始位|数据结束位|数据

		String filename = Utils.bytes2String(packet.getSubsegment(1));
		long offset = Long.parseLong(Utils.bytes2String(packet.getSubsegment(2)));
		byte[] fileData = record.readFile(filename, offset, FileExpressDefinition.CHUNK_SIZE);
		if (null == fileData) {
			Logger.e(this.getClass(),
					new StringBuilder("Read file error - file:'").append(filename).append("'").toString());
			return;
		}

		long end = offset + fileData.length;

		Packet response = new Packet(FileExpressDefinition.PT_OFFER, 3, 1, 0);
		response.appendSubsegment(packet.getSubsegment(0));
		response.appendSubsegment(packet.getSubsegment(1));
		response.appendSubsegment(packet.getSubsegment(2));
		response.appendSubsegment(Utils.string2Bytes(Long.toString(end)));
		response.appendSubsegment(fileData);

		byte[] data = Packet.pack(response);
		if (data != null) {
			Message message = new Message(data);
			session.write(message);
		}
	}

	private void responseAttribute(final Session session, final Packet packet) {
		// 包格式：授权码|文件名
		if (packet.getSubsegmentCount() != 2) {
			Logger.w(this.getClass(), "Packet format error in responseAttribute()");
			return;
		}

		// 获取授权码
		String authCode = Utils.bytes2String(packet.getSubsegment(0));

		// 验证 Session
		if (false == checkSession(session, authCode)) {
			reject(session);
			return;
		}

		FileExpressServoContext servoctx = this.servoContexts.get(authCode);
		if (null == servoctx) {
			return;
		}

		String filename = Utils.bytes2String(packet.getSubsegment(1));

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

		String authCodeStr = Utils.bytes2String(authCode);
		ExpressAuthCode eac = this.authCodes.get(authCodeStr);
		if (null != eac) {
			auth = true;

			// 查询会话记录
			SessionRecord record = this.sessionRecords.get(session.getId());
			if (null == record) {
				record = new SessionRecord();
				this.sessionRecords.put(session.getId(), record);
			}
			record.addAuthCode(eac);
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

	/** 中断传输。
	 */
	private void interrupt(Session session) {
		SessionRecord record = this.sessionRecords.get(session.getId());
		if (null == record) {
			return;
		}

		// 移除 Session 记录
		this.sessionRecords.remove(session.getId());

		// 回调-发生错误
		Iterator<FileExpressContext> iter = record.getContextList().iterator();
		while (iter.hasNext()) {
			FileExpressContext ctx = iter.next();
			ctx.errorCode = FileExpressContext.EC_ABORT;
			this.expressError(ctx);
		}

		// 关闭对应记录上的所有文件
		record.closeAllFiles();
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
		// 错误处理
		Logger.e(this.getClass(), new StringBuilder("File express a session error occurred - code=")
			.append(errorCode).append(" session=").append(session.getAddress().getAddress().getHostAddress()).toString());
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
	protected final class SessionRecord {

		// Key: auth code string, value: express auth code instance
		private ConcurrentHashMap<String, ExpressAuthCode> authCodes;
		// Key: filename
		private ConcurrentHashMap<String, FileExpressContext> contexts;
		// Key: filename
		private ConcurrentHashMap<String, ResultSet> resultSets;

		protected SessionRecord() {
			this.authCodes = new ConcurrentHashMap<String, ExpressAuthCode>();
			this.contexts = new ConcurrentHashMap<String, FileExpressContext>();
			this.resultSets = new ConcurrentHashMap<String, ResultSet>();
		}

		protected boolean containsAuthCode(final String authCode) {
			return this.authCodes.containsKey(authCode);
		}

		protected void addAuthCode(final ExpressAuthCode authCode) {
			this.authCodes.put(authCode.getCode(), authCode);
		}

		protected void removeAuthCode(final ExpressAuthCode authCode) {
			if (this.authCodes.containsKey(authCode.getCode())) {
				this.authCodes.remove(authCode.getCode());
			}
		}

		protected Collection<FileExpressContext> getContextList() {
			return this.contexts.values();
		}

		/** 返回指定文件的上下文。
		 */
		protected FileExpressContext getContext(final String filename) {
			return this.contexts.get(filename);
		}

		/** 准备文件。
		 */
		protected FileExpressContext prepareFile(final String authCode, final FileAttribute attribute
				, final String filename, final long fileSize, final int operate) {
			// 创建对应的上下文
			FileExpressContext ctx = null;
			if (this.contexts.containsKey(filename)) {
				ctx = this.contexts.get(filename);
			}
			else {
				ExpressAuthCode ac = this.authCodes.get(authCode);
				ctx = new FileExpressContext(ac, ac.getContextPath(), filename, operate);
				// 设置属性
				ctx.setAttribute(attribute);
				this.contexts.put(filename, ctx);
			}

			if (operate == FileExpressContext.OP_UPLOAD) {
				// 如果是上传操作，则更新文件大小
				ResultSet rs = this.findOrCreateResultSet(filename);
				rs.updateLong(FileStorage.LABEL_LONG_SIZE, fileSize);
			}

			// 设置总进度
			ctx.bytesTotal = fileSize;

			return ctx;
		}

		/** 关闭指定文件。
		 */
		protected FileExpressContext closeFile(final String filename) {
			ResultSet resultSet = this.resultSets.get(filename);
			if (null != resultSet) {
				resultSet.close();
				this.resultSets.remove(filename);
			}

			FileExpressContext ctx = this.contexts.get(filename);
			this.contexts.remove(filename);
			return ctx;
		}

		/** 读文件。
		 */
		protected byte[] readFile(final String filename, final long offset, final long length) {
			// 获取 ResultSet
			ResultSet resultSet = this.findOrCreateResultSet(filename);
			// 读文件数据
			return resultSet.getRaw(FileStorage.LABEL_RAW_DATA, offset, length);
		}

		/** 写文件。
		 */
		protected long writeFile(final String filename, byte[] data, final long offset,
				final long length) {
			// 获取 ResultSet
			ResultSet resultSet = this.findOrCreateResultSet(filename);
			// 写文件数据
			try {
				resultSet.updateRaw(FileStorage.LABEL_RAW_DATA, data, offset, length);
				return length;
			} catch (StorageException e) {
				Logger.log(e, LogLevel.ERROR);
				return 0;
			}
		}

		/** 关闭所有文件。
		 */
		protected void closeAllFiles() {
			Iterator<ResultSet> iter = this.resultSets.values().iterator();
			while (iter.hasNext()) {
				ResultSet rs = iter.next();
				rs.close();
			}

			this.contexts.clear();
			this.resultSets.clear();
		}

		private ResultSet findOrCreateResultSet(final String filename) {
			if (this.resultSets.containsKey(filename)) {
				return this.resultSets.get(filename);
			}
			else {
				ResultSet resultSet = null;
				try {
					FileExpressContext ctx = this.contexts.get(filename);
					resultSet = mainStorage.store(mainStorage.createWriteStatement(ctx.getFullPath()));
				} catch (StorageException e) {
					Logger.log(e, LogLevel.ERROR);
					return null;
				}
				// 移动游标
				resultSet.next();
				// 记录
				this.resultSets.put(filename, resultSet);
				return resultSet;
			}
		}
	}
}
