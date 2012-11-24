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

package net.cellcloud.talk;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import net.cellcloud.common.Message;
import net.cellcloud.common.NonblockingAcceptor;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Service;
import net.cellcloud.common.Session;
import net.cellcloud.core.Cellet;
import net.cellcloud.core.CelletSandbox;
import net.cellcloud.core.Cryptology;
import net.cellcloud.core.Logger;
import net.cellcloud.core.Nucleus;
import net.cellcloud.core.NucleusContext;
import net.cellcloud.exception.SingletonException;
import net.cellcloud.http.HttpService;
import net.cellcloud.talk.dialect.ActionDialectFactory;
import net.cellcloud.talk.dialect.Dialect;
import net.cellcloud.talk.dialect.DialectEnumerator;
import net.cellcloud.talk.stuff.Primitive;
import net.cellcloud.util.Util;

import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/** 会话服务。
 *
 * @author Jiangwei Xu
 */
public final class TalkService implements Service {

	private static TalkService instance = null;

	private int port;
	private int httpPort;
	private boolean httpEnabled;

	private NonblockingAcceptor acceptor;
	private NucleusContext nucleusContext;
	private TalkAcceptorHandler talkHandler;

	/// 待检验 Session
	private ConcurrentHashMap<Long, Certificate> unidentifiedSessions;
	/// Session context
	private ConcurrentHashMap<Session, TalkSessionContext> sessionContexts;
	/// Tag 与 Session 上下文的映射
	private ConcurrentHashMap<String, Vector<TalkSessionContext>> tagSessionsMap;
	/// 挂起状态的上下文
	private ConcurrentHashMap<String, SuspendedTracker> suspendedTrackers;

	protected ConcurrentHashMap<String, Speaker> speakers;
	protected Vector<Speaker> lostSpeakers;

	private TalkServiceDaemon daemon;
	private ArrayList<TalkListener> listeners;

	/** 构造函数。
	 * @throws SingletonException 
	 */
	public TalkService(NucleusContext nucleusContext)
			throws SingletonException {
		if (null == TalkService.instance) {
			TalkService.instance = this;

			this.nucleusContext = nucleusContext;

			this.port = 7000;
			this.httpPort = 8181;
			this.httpEnabled = false;
			this.unidentifiedSessions = null;
			this.sessionContexts = null;
			this.tagSessionsMap = null;

			this.speakers = null;
			this.lostSpeakers = null;
			this.listeners = null;

			// 添加默认方言工厂
			DialectEnumerator.getInstance().addFactory(new ActionDialectFactory());
		}
		else {
			throw new SingletonException(TalkService.class.getName());
		}
	}

	/** 返回单例。 */
	public synchronized static TalkService getInstance() {
		return instance;
	}

	/** 设置端口。
	 */
	public void setPort(int port) {
		this.port = port;
	}
	/** 设置 HTTP 服务端口。
	 */
	public void setHttpPort(int port) {
		this.httpPort = port;
	}

	/** 启动会话服务。
	 */
	@Override
	public boolean startup() {
		if (null == this.unidentifiedSessions) {
			this.unidentifiedSessions = new ConcurrentHashMap<Long, Certificate>(); 
		}
		if (null == this.sessionContexts) {
			this.sessionContexts = new ConcurrentHashMap<Session, TalkSessionContext>();
		}
		if (null == this.tagSessionsMap) {
			this.tagSessionsMap = new ConcurrentHashMap<String, Vector<TalkSessionContext>>();
		}
		if (null == this.suspendedTrackers) {
			this.suspendedTrackers = new ConcurrentHashMap<String, SuspendedTracker>();
		}

		if (null == this.acceptor) {
			this.acceptor = new NonblockingAcceptor();
		}

		// 最大连接数
		this.acceptor.setMaxConnectNum(1024);

		// 定义包标识
		byte[] head = {0x20, 0x10, 0x11, 0x10};
		byte[] tail = {0x19, 0x78, 0x10, 0x04};
		this.acceptor.defineDataMark(head, tail);

		// 设置处理器
		if (null == this.talkHandler) {
			this.talkHandler = new TalkAcceptorHandler(this, this.acceptor.getWorkerNum());
		}
		this.acceptor.setHandler(this.talkHandler);

		boolean succeeded = this.acceptor.bind(this.port);
		if (succeeded) {
			startDaemon();
		}

		if (this.httpEnabled) {
			// 启动 HTTP 服务
			startHttpService();
		}

		return succeeded;
	}

	/** 关闭会话服务。
	 */
	@Override
	public void shutdown() {
		// 停止 HTTP 服务
		stopHttpService();

		if (null != this.acceptor) {
			this.acceptor.unbind();
		}

		stopDaemon();
	}

	/** 设置是否激活 HTTP 服务。
	 */
	public void httpEnabled(boolean enabled) {
		this.httpEnabled = enabled;
	}

	/** 启动任务表守护线程。
	 */
	public void startDaemon() {
		if (null == this.daemon) {
			this.daemon = new TalkServiceDaemon();
			this.daemon.start();
		}
	}

	/** 关闭任务表守护线程。
	 */
	public void stopDaemon() {
		if (null != this.daemon) {
			this.daemon.stopSpinning();
		}
	}

	/** 添加会话监听器。
	 */
	public void addListener(TalkListener listener) {
		if (null == this.listeners) {
			this.listeners = new ArrayList<TalkListener>();
		}

		if (!this.listeners.contains(listener)) {
			this.listeners.add(listener);
		}
	}

	/** 删除会话监听器。
	 */
	public void removeListener(TalkListener listener) {
		if (null == this.listeners) {
			return;
		}

		this.listeners.remove(listener);
	}

	/** 通知对端 Speaker 原语。
	 */
	public boolean notice(final String targetTag, final Primitive primitive,
			final Cellet cellet, final CelletSandbox sandbox) {
		// 检查 Cellet 合法性
		if (!Nucleus.getInstance().checkSandbox(cellet, sandbox)) {
			Logger.w(TalkService.class, "Illegal cellet : " + cellet.getFeature().getIdentifier());
			return false;
		}

		if (null == this.tagSessionsMap) {
			Logger.w(TalkService.class, "Unknown target tag : " + targetTag);
			return false;
		}

		Vector<TalkSessionContext> contexts = this.tagSessionsMap.get(targetTag);
		if (null == contexts) {
			Logger.d(TalkService.class, "Can't find target tag in context list : " + targetTag);

			// 尝试在已挂起的的追踪器里查找
			this.tryOfferPrimitive(targetTag, cellet, primitive);
			return false;
		}

		Message message = null;

		for (TalkSessionContext ctx : contexts) {
			// 查找上文里的制定的会话追踪器
			TalkTracker tracker = ctx.getTracker(targetTag);
			if (null != tracker) {
				// 判断是否是同一个 Cellet
				if (tracker.activeCellet == cellet) {
					Session session = ctx.getSession();
					message = this.packetDialogue(primitive);
					if (null != message) {
						session.write(message);
					}
					break;
				}
			}
		}

		return (null != message);
	}

	/** 申请调用 Cellet 服务。
	*/
	public boolean call(String identifier, InetSocketAddress address) {
		return this.call(identifier, address, null);
	}

	/** 申请调用 Cellet 服务。
	 */
	public boolean call(String identifier, InetSocketAddress address, TalkCapacity capacity) {
		if (null == this.speakers)
			this.speakers = new ConcurrentHashMap<String, Speaker>();

		if (this.speakers.containsKey(identifier))
			return false;

		Speaker speaker = new Speaker(identifier, capacity);
		speaker.call(address);

		this.speakers.put(identifier, speaker);

		return true;
	}

	/** 挂断 Cellet 服务。
	 */
	public void hangUp(String identifier) {
		if (null == this.speakers || !this.speakers.containsKey(identifier))
			return;

		Speaker speaker = this.speakers.get(identifier);
		if (null != speaker) {
			speaker.hangUp();
			this.speakers.remove(identifier);
			this.lostSpeakers.remove(speaker);
		}
	}

	/** 向指定 Cellet 发送原语。
	 */
	public boolean talk(final String identifier, Primitive primitive) {
		if (null == this.speakers)
			return false;

		Speaker speaker = this.speakers.get(identifier);
		if (null != speaker) {
			speaker.speak(primitive);
			return true;
		}

		return false;
	}

	/** 向指定 Cellet 发送方言。
	 */
	public boolean talk(final String identifier, Dialect dialect) {
		if (null == this.speakers)
			return false;

		Primitive primitive = dialect.translate(Nucleus.getInstance().getTagAsString());
		if (null != primitive) {
			return this.talk(identifier, primitive);
		}

		return false;
	}

	/** 启动 HTTP 服务。
	 */
	private void startHttpService() {
		if (null == HttpService.getInstance()) {
			Logger.e(TalkService.class, "Starts talk web service fail, reason is web model failed to start.");
			return;
		}

		SelectChannelConnector connector = new SelectChannelConnector();
		connector.setPort(this.httpPort);
		connector.setRequestHeaderSize(8192);
		connector.setThreadPool(new QueuedThreadPool(16));
		connector.setName("/api/talk");

		HttpService.getInstance().addConnector(connector);
		HttpService.getInstance().addHandler(new TalkHttpHandler());
	}

	/** 停止 HTTP 服务。
	 */
	private void stopHttpService() {
		// Nothing
	}

	/** 将指定 Speaker 标记为丢失连接。
	 * 丢失连接的 Speaker 将由 Talk Service 驱动进行重连。
	 */
	protected void markLostSpeaker(Speaker speaker) {
		if (null == this.lostSpeakers) {
			this.lostSpeakers = new Vector<Speaker>();
		}

		speaker.timestamp = System.currentTimeMillis();

		if (!this.lostSpeakers.contains(speaker)) {
			this.lostSpeakers.add(speaker);
		}
	}

	/** 通知 Dialogue 。
	*/
	protected void fireListenerDialogue(final String tag, Primitive primitive) {
		if (null == this.listeners) {
			return;
		}

		Iterator<TalkListener> iter = this.listeners.iterator();
		while (iter.hasNext()) {
			TalkListener listener = iter.next();
			listener.dialogue(tag, primitive);
		}
	}

	/** 通知新连接。
	*/
	protected void fireListenerContacted(final String tag) {
		if (null == this.listeners) {
			return;
		}

		Iterator<TalkListener> iter = this.listeners.iterator();
		while (iter.hasNext()) {
			TalkListener listener = iter.next();
			listener.contacted(tag);
		}
	}

	/** 通知断开连接。
	*/
	protected void fireListenerQuitted(final String tag) {
		if (null == this.listeners) {
			return;
		}

		Iterator<TalkListener> iter = this.listeners.iterator();
		while (iter.hasNext()) {
			TalkListener listener = iter.next();
			listener.quitted(tag);
		}
	}

	/** 通知发生错误。
	*/
	protected void fireListenerFailed(final TalkServiceFailure failure) {
		if (null == this.listeners) {
			return;
		}

		Iterator<TalkListener> iter = this.listeners.iterator();
		while (iter.hasNext()) {
			TalkListener listener = iter.next();
			listener.failed(failure);
		}
	}

	/** 开启 Session 。
	 */
	protected void openSession(Session session) {
		Long sid = session.getId();
		if (this.unidentifiedSessions.containsKey(sid)) {
			return;
		}

		Certificate cert = new Certificate();
		cert.session = session;
		cert.key = Util.randomString(8);
		cert.plaintext = Util.randomString(16);
		this.unidentifiedSessions.put(sid, cert);
	}

	/** 关闭 Session 。
	 */
	protected synchronized void closeSession(final Session session) {
		TalkSessionContext ctx = this.sessionContexts.get(session);
		if (null != ctx) {
			// 遍历此 Session 所访问的所有 Cellet
			Map<String, TalkTracker> map = ctx.getTrackers();
			Iterator<String> iter = map.keySet().iterator();
			while (iter.hasNext()) {
				String tag = iter.next();

				TalkTracker tracker = map.get(tag);
				// 判断是否需要进行挂起
				if (tracker.isAutoSuspend()) {
					// 将消费者挂起，被动挂起
					this.suspendTalk(tracker, false);
					if (null != tracker.activeCellet) {
						// 通知 Cellet 对端挂起
						tracker.activeCellet.suspended(tag);
					}
				}
				else {
					// 不进行挂起
					if (null != tracker.activeCellet) {
						// 通知 Cellet 对端退出
						tracker.activeCellet.quitted(tag);
					}
				}

				// 处理标签和上下文映射列表
				Vector<TalkSessionContext> list = this.tagSessionsMap.get(tag);
				if (list != null) {
					for (int i = 0, size = list.size(); i < size; ++i) {
						TalkSessionContext c = list.get(i);
						if (c.getSession().getId() == session.getId()) {
							list.remove(i);
							break;
						}
					}

					// 如果清单为空，则删除此记录
					if (list.isEmpty()) {
						this.tagSessionsMap.remove(tag);
					}
				}
			} // # while

			// 清理上下文记录
			this.sessionContexts.remove(session);
		}

		// 清理未授权表
		this.unidentifiedSessions.remove(session.getId());
	}

	/** 允许指定 Session 连接。
	 */
	protected synchronized void acceptSession(Session session) {
		Long sid = session.getId();
		this.unidentifiedSessions.remove(sid);

		TalkSessionContext ctx = new TalkSessionContext(session);
		ctx.tickTime = this.getTickTime();
		this.sessionContexts.put(session, ctx);
	}

	/** 拒绝指定 Session 连接。
	 */
	protected synchronized void rejectSession(Session session) {
		Long sid = session.getId();

		StringBuilder log = new StringBuilder();
		log.append("Talk service reject session (");
		log.append(sid);
		log.append("): ");
		log.append(session.getAddress().getAddress().getHostAddress());
		log.append(":");
		log.append(session.getAddress().getPort());
		Logger.w(TalkService.class, log.toString());
		log = null;

		this.unidentifiedSessions.remove(sid);
		this.sessionContexts.remove(session);
		this.acceptor.close(session);
	}

	/** 请求 Cellet 。
	 */
	protected TalkTracker processRequest(Session session, String tag, String identifier) {
		TalkSessionContext ctx = this.sessionContexts.get(session);
		if (null == ctx) {
			return null;
		}

		// 标签与上下文映射
		Vector<TalkSessionContext> list = this.tagSessionsMap.get(tag);
		if (null != list) {
			if (!list.contains(ctx)) {
				list.add(ctx);
			}
		}
		else {
			list = new Vector<TalkSessionContext>();
			list.add(ctx);
			this.tagSessionsMap.put(tag, list);
		}

		Cellet cellet = null;

		TalkTracker tracker = ctx.getTracker(tag);
		if (null != tracker) {
			if (tracker.activeCellet != null && tracker.activeCellet.getFeature().getIdentifier().equals(identifier)) {
				cellet = null;
			}
			else {
				cellet = Nucleus.getInstance().getCellet(identifier, this.nucleusContext);
				if (null != cellet) {
					tracker.activeCellet = cellet;
				}
			}
		}
		else {
			tracker = ctx.addTracker(tag, session.getAddress());
			cellet = Nucleus.getInstance().getCellet(identifier, this.nucleusContext);
			if (null != cellet) {
				tracker.activeCellet = cellet;
			}
		}

		if (null != cellet) {
			// 回调 contacted
			cellet.contacted(tag);

			// 尝试恢复被动 Talk
			if (this.tryResumePassiveTalk(tag, cellet)) {
				// 回调 resumed
				cellet.resumed(tag);
			}
		}

		return tracker;
	}

	/** 协商服务能力。
	 */
	protected TalkCapacity processConsult(Session session, String tag, TalkCapacity capacity) {
		TalkSessionContext ctx = this.sessionContexts.get(session);
		if (null == ctx) {
			return new TalkCapacity(false, 0);
		}

		TalkTracker tracker = ctx.getTracker(tag);
		if (null == tracker) {
			return new TalkCapacity(false, 0);
		}

		// 设置是否重连
		tracker.setAutoSuspend(capacity.autoSuspend);
		// 设置超时
		tracker.setSuspendDuration(capacity.suspendDuration);

		return new TalkCapacity(tracker.isAutoSuspend(), tracker.getSuspendDuration());
	}

	/** 对话 Cellet 。
	 */
	protected void processDialogue(Session session, String speakerTag, Primitive primitive) {
		TalkSessionContext ctx = this.sessionContexts.get(session);
		if (null != ctx) {
			ctx.tickTime = this.getTickTime();

			TalkTracker tracker = ctx.getTracker(speakerTag);
			if (null != tracker && null != tracker.activeCellet) {
				// 设置原语的 Cellet
				primitive.setCellet(tracker.activeCellet);
				// 回调
				tracker.activeCellet.dialogue(speakerTag, primitive);
			}
		}
	}

	/** 挂起指定的会话。
	 */
	protected void suspendCellet(Session session, String speakerTag) {
		// 挂起的会话如果网络断开则不会通知退出
	}

	/** 恢复指定的会话。
	 */
	protected void resumeCellet(Session session, String speakerTag) {
		
	}

	/** 返回 Session 证书。
	 */
	protected Certificate getCertificate(Session session) {
		return this.unidentifiedSessions.get(session.getId());
	}

	/** 处理未识别 Session 。
	 */
	protected void processUnidentifiedSessions(long time) {
		if (null == this.unidentifiedSessions || this.unidentifiedSessions.isEmpty()) {
			return;
		}

		// 存储超时的 Session
		ArrayList<Session> sessionList = null;

		Long sid = null;
		Iterator<Long> iter = this.unidentifiedSessions.keySet().iterator();
		while (iter.hasNext()) {
			sid = iter.next();
			Certificate cert = this.unidentifiedSessions.get(sid);
			if (false == cert.checked) {
				cert.checked = true;
				deliverChecking(cert.session, cert.plaintext, cert.key);
			}
			else {
				// 10 秒超时检测
				if (time - cert.time > 10000) {
					if (null == sessionList) {
						sessionList = new ArrayList<Session>();
					}

					sessionList.add(cert.session);
				}
			}
		}

		if (null != sessionList) {
			// 关闭所有超时的 Session
			Iterator<Session> siter = sessionList.iterator();
			while (siter.hasNext()) {
				Session session = siter.next();

				StringBuilder log = new StringBuilder();
				log.append("Talk service session timeout: ");
				log.append(session.getAddress().getAddress().getHostAddress());
				log.append(":");
				log.append(session.getAddress().getPort());
				Logger.i(TalkService.class, log.toString());
				log = null;

				this.unidentifiedSessions.remove(session.getId());
				this.acceptor.close(session);
			}
			sessionList = null;
		}
	}

	/** 更新 Session tick time 。
	 */
	protected void updateSessionTickTime(Session session) {
		TalkSessionContext ctx = this.sessionContexts.get(session);
		if (null != ctx) {
			ctx.tickTime = this.daemon.getTickTime();

			Logger.d(this.getClass(), "Talk service heartbeat from " + session.getAddress().getAddress().getHostAddress()
					+ ":" + session.getAddress().getPort());
		}
	}

	/** 返回时间点。
	 */
	protected long getTickTime() {
		return this.daemon.getTickTime();
	}

	/** 检查并删除挂起的会话。
	 */
	protected void checkAndDeleteSuspendedTalk() {
		// 两个判断依据，满足任一一个时即可进行删除：
		// 1、挂起会话超时
		// 2、挂起会话所标识的消费端已经和 Cellet 重建连接

		Iterator<String> iter = this.tagSessionsMap.keySet().iterator();
		while (iter.hasNext()) {
			String tag = iter.next();
			SuspendedTracker tracker = this.suspendedTrackers.get(tag);
			if (null != tracker) {
				if (tracker.isEmptyRecord()) {
					this.suspendedTrackers.remove(tag);
				}
			}
		}

		Iterator<Map.Entry<String, SuspendedTracker>> eiter = this.suspendedTrackers.entrySet().iterator();
		while (eiter.hasNext()) {
			Map.Entry<String, SuspendedTracker> entry = eiter.next();
			SuspendedTracker tracker = entry.getValue();
			if (tracker.isTimeout()) {
				// 回调退出函数
				List<Cellet> list = tracker.getCelletList();
				for (int i = 0, size = list.size(); i < size; ++i) {
					list.get(i).quitted(entry.getKey());
				}

				eiter.remove();
			}
		}
	}

	/** 挂起会话。
	 */
	private void suspendTalk(TalkTracker talkTracker, boolean initiative) {
		if (null == talkTracker.activeCellet) {
			return;
		}

		if (this.suspendedTrackers.containsKey(talkTracker.getTag())) {
			SuspendedTracker tracker = this.suspendedTrackers.get(talkTracker.getTag());
			tracker.track(talkTracker.activeCellet, initiative);
			return;
		}

		SuspendedTracker tracker = new SuspendedTracker(talkTracker.getTag());
		tracker.track(talkTracker.activeCellet, initiative);
		tracker.liveDuration = talkTracker.getSuspendDuration();
		this.suspendedTrackers.put(talkTracker.getTag(), tracker);
	}

	/** 尝试恢复被动会话。
	 */
	private boolean tryResumePassiveTalk(String tag, Cellet cellet) {
		SuspendedTracker tracker = this.suspendedTrackers.get(tag);
		if (null != tracker) {
			boolean ret = tracker.pollPrimitiveMatchPassiveness(this.daemon, cellet);
			if (ret) {
				tracker.retreat(cellet);
				return true;
			}
		}

		return false;
	}

	/** 尝试记录下挂起会话的原语。
	 */
	private void tryOfferPrimitive(String tag, Cellet cellet, Primitive primitive) {
		SuspendedTracker tracker = this.suspendedTrackers.get(tag);
		if (null != tracker) {
			tracker.offerPrimitive(cellet, primitive);
		}
	}

	/** 向指定 Session 发送识别指令。
	 */
	private void deliverChecking(Session session, String text, String key) {
		// 包格式：密文|密钥

		byte[] ciphertext = Cryptology.getInstance().simpleEncrypt(text.getBytes(), key.getBytes());

		Packet packet = new Packet(TalkDefinition.TPT_INTERROGATE, 1);
		packet.appendSubsegment(ciphertext);
		packet.appendSubsegment(key.getBytes());

		byte[] data = Packet.pack(packet);
		if (null != data) {
			Message message = new Message(data);
			this.acceptor.write(session, message);
			message = null;
		}

		packet = null;
	}

	/** 打包对话原语。
	 */
	private Message packetDialogue(Primitive primitive) {
		// 序列化原语
		ByteArrayOutputStream stream = primitive.write();

		// 封装数据包
		Packet packet = new Packet(TalkDefinition.TPT_DIALOGUE, 99);
		packet.setBody(stream.toByteArray());

		// 发送数据
		byte[] data = Packet.pack(packet);
		Message message = new Message(data);
		return message;
	}

	/** 会话身份证书。
	*/
	protected class Certificate {
		/** 构造函数。 */
		protected Certificate() {
			this.session = null;
			this.key = null;
			this.plaintext = null;
			this.time = System.currentTimeMillis();
			this.checked = false;
		}

		/// 相关 Session
		protected Session session;
		/// 密钥
		protected String key;
		/// 明文
		protected String plaintext;
		/// 时间戳
		protected long time;
		/// 是否已经发送校验请求
		protected boolean checked;
	}
}
