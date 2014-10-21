/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2014 Cell Cloud Team (www.cellcloud.net)

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
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;

import net.cellcloud.common.Cryptology;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.NonblockingAcceptor;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Service;
import net.cellcloud.common.Session;
import net.cellcloud.core.Cellet;
import net.cellcloud.core.CelletSandbox;
import net.cellcloud.core.Nucleus;
import net.cellcloud.core.NucleusContext;
import net.cellcloud.exception.InvalidException;
import net.cellcloud.exception.SingletonException;
import net.cellcloud.http.CookieSessionManager;
import net.cellcloud.http.HttpCapsule;
import net.cellcloud.http.HttpService;
import net.cellcloud.http.HttpSession;
import net.cellcloud.talk.dialect.ActionDialectFactory;
import net.cellcloud.talk.dialect.Dialect;
import net.cellcloud.talk.dialect.DialectEnumerator;
import net.cellcloud.util.CachedQueueExecutor;
import net.cellcloud.util.Utils;

/** 会话服务。
 *
 * @author Jiangwei Xu
 */
public final class TalkService implements Service, SpeakerDelegate {

	private static TalkService instance = null;

	private int port;
	private int block;

	private int httpPort;
	// 服务器端是否启用 HTTP 服务
	private boolean httpEnabled;

	private CookieSessionManager httpSessionManager;
	private HttpSessionListener httpSessionListener;
	private long httpSessionTimeout;

	private NonblockingAcceptor acceptor;
	private NucleusContext nucleusContext;
	private TalkAcceptorHandler talkHandler;

	// 线程执行器
	protected ExecutorService executor;

	/// 待检验 Session
	private ConcurrentHashMap<Long, Certificate> unidentifiedSessions;
	/// Session context
	private ConcurrentHashMap<Session, TalkSessionContext> sessionContexts;
	/// Tag 与 Session 上下文的映射
	private ConcurrentHashMap<String, Vector<TalkSessionContext>> tagSessionsMap;
	/// 挂起状态的上下文
	private ConcurrentHashMap<String, SuspendedTracker> suspendedTrackers;

	/// Tag 缓存列表
	private ConcurrentSkipListSet<String> tagList;

	// 私有协议 Speaker
	protected ConcurrentHashMap<String, Speaker> speakers;
	// HTTP 协议 Speaker
	protected ConcurrentHashMap<String, HttpSpeaker> httpSpeakers;

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
			this.block = 8192;

			this.httpPort = 7070;
			this.httpEnabled = true;

			// 5 分钟
			this.httpSessionTimeout = 5 * 60 * 1000;

			// 创建执行器
			this.executor = CachedQueueExecutor.newCachedQueueThreadPool(8);

			// 添加默认方言工厂
			DialectEnumerator.getInstance().addFactory(new ActionDialectFactory());
		}
		else {
			throw new SingletonException(TalkService.class.getName());
		}
	}

	/**
	 * 返回会话服务单例。
	 */
	public static TalkService getInstance() {
		return TalkService.instance;
	}

	/**
	 * 启动会话服务。
	 * @return 如果启动成功，则返回 true，否则返回 false
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
		if (null == this.tagList) {
			this.tagList = new ConcurrentSkipListSet<String>();
		}

		if (null == this.acceptor) {
			// 创建网络适配器
			this.acceptor = new NonblockingAcceptor();
			this.acceptor.setBlockSize(this.block);

			// 定义包标识
			byte[] head = {0x20, 0x10, 0x11, 0x10};
			byte[] tail = {0x19, 0x78, 0x10, 0x04};
			this.acceptor.defineDataMark(head, tail);

			// 设置处理器
			this.talkHandler = new TalkAcceptorHandler(this);
			this.acceptor.setHandler(this.talkHandler);
		}

		// 最大连接数
		this.acceptor.setMaxConnectNum(1000);

		boolean succeeded = this.acceptor.bind(this.port);
		if (succeeded) {
			startDaemon();
		}

		if (succeeded && this.httpEnabled) {
			// 启动 HTTP 服务
			startHttpService();
		}

		return succeeded;
	}

	/** 关闭会话服务。
	 */
	@Override
	public void shutdown() {
		if (null != this.acceptor) {
			this.acceptor.unbind();
		}

		stopDaemon();

		if (null != this.executor) {
			this.executor.shutdown();
		}

		if (this.httpEnabled && null != HttpService.getInstance()) {
			HttpService.getInstance().removeCapsule(this.httpPort);
		}
	}

	/** 设置服务端口。
	 * @note 在 startup 之前设置才能生效。
	 * @param port 指定服务监听端口。
	 */
	public void setPort(int port) {
		if (null != this.acceptor && this.acceptor.isRunning()) {
			throw new InvalidException("Can't set the port in talk service after the start");
		}

		this.port = port;
	}

	/** 返回服务端口。
	 * @return
	 */
	public int getPort() {
		return this.port;
	}

	/** 设置 HTTP 服务端口。
	 * @note 在 startup 之前设置才能生效。
	 */
	public void setHttpPort(int port) {
		if (null != this.acceptor && this.acceptor.isRunning()) {
			throw new InvalidException("Can't set the http port in talk service after the start");
		}

		this.httpPort = port;
	}

	/** 返回 HTTP 服务端口。
	 * @return
	 */
	public int getHttpPort() {
		return this.httpPort;
	}

	/** 设置是否激活 HTTP 服务。
	 */
	public void httpEnabled(boolean enabled) {
		if (null != HttpService.getInstance() && HttpService.getInstance().hasCapsule(this.httpPort)) {
			throw new InvalidException("Can't set the http enabled in talk service after the start");
		}

		this.httpEnabled = enabled;
	}

	/** 设置 HTTP 会话超时时间。
	 */
	public void settHttpSessionTimeout(long timeoutInMillisecond) {
		this.httpSessionTimeout = timeoutInMillisecond;
	}

	/**
	 * 设置适配器缓存块大小。
	 * @param size
	 */
	public void setBlockSize(int size) {
		this.block = size;
	}

	/** 启动任务表守护线程。
	 */
	public void startDaemon() {
		if (null == this.daemon) {
			this.daemon = new TalkServiceDaemon();
		}

		if (!this.daemon.running)
			this.daemon.start();
	}

	/** 关闭任务表守护线程。
	 */
	public void stopDaemon() {
		if (null != this.daemon) {
			this.daemon.stopSpinning();

			// 阻塞等待线程退出
			while (this.daemon.running) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					Logger.log(TalkService.class, e, LogLevel.DEBUG);
				}
			}

			this.daemon = null;
		}
	}

	///@Server
	/** 查找指定 Cellet 里的标签对应的服务追踪器。
	 */
	public TalkTracker findTracker(Cellet cellet, String tag) {
		Vector<TalkSessionContext> list = this.tagSessionsMap.get(tag);
		if (null == list) {
			return null;
		}

		for (TalkSessionContext context : list) {
			TalkTracker tt = context.getTracker(tag);
			if (null == tt) {
				continue;
			}

			if (tt.activeCellet == cellet) {
				return tt;
			}
		}

		return null;
	}

	/** 添加会话监听器。
	 */
	public void addListener(TalkListener listener) {
		if (null == this.listeners) {
			this.listeners = new ArrayList<TalkListener>();
		}

		synchronized (this.listeners) {
			if (!this.listeners.contains(listener)) {
				this.listeners.add(listener);
			}
		}
	}

	/** 删除会话监听器。
	 */
	public void removeListener(TalkListener listener) {
		if (null == this.listeners) {
			return;
		}

		synchronized (this.listeners) {
			this.listeners.remove(listener);
		}
	}

	/**
	 * 是否已添加指定的监听器。
	 * @param listener
	 * @return
	 */
	public boolean hasListener(TalkListener listener) {
		if (null == this.listeners) {
			return false;
		}

		synchronized (this.listeners) {
			return this.listeners.contains(listener);
		}
	}

	/**
	 * 返回当前所有会话者的 Tag 列表。
	 * @return
	 */
	public Set<String> getTalkerList() {
		return this.tagList;
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
			if (Logger.isDebugLevel()) {
				Logger.d(TalkService.class, "Can't find target tag in context list : " + targetTag);
			}

			// 尝试在已挂起的的追踪器里查找
			this.tryOfferPrimitive(targetTag, cellet, primitive);

			// 因为没有直接发送出去原语，所以返回 false
			return false;
		}

		// 尝试在已挂起的的追踪器里查找
		if (this.tryOfferPrimitive(targetTag, cellet, primitive)) {
			// 因为没有直接发送出去原语，所以返回 false
			return false;
		}

		Message message = null;

		synchronized (contexts) {
			for (TalkSessionContext ctx : contexts) {
				// 查找上文里指定的会话追踪器
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
		}

		return (null != message);
	}

	/** 通知对端 Speaker 方言。
	 */
	public boolean notice(final String targetTag, final Dialect dialect,
			final Cellet cellet, final CelletSandbox sandbox) {
		Primitive primitive = dialect.translate();
		if (null != primitive) {
			return this.notice(targetTag, primitive, cellet, sandbox);
		}
		return false;
	}

	/** 申请调用 Cellet 服务。
	 * 
	 * @note Client
	 */
	public boolean call(String identifier, InetSocketAddress address) {
		return this.call(identifier, address, null, false);
	}

	/**
	 * 申请调用 Cellet 服务。
	 * 
	 * @param identifier
	 * @param address
	 * @return
	 */
	public boolean call(String identifier, InetSocketAddress address, boolean http) {
		return this.call(identifier, address, null, http);
	}

	/**
	 * 申请调用 Cellet 服务。
	 * 
	 * @param identifier
	 * @param address
	 * @param capacity
	 * @return
	 */
	public boolean call(String identifier, InetSocketAddress address, TalkCapacity capacity) {
		return this.call(identifier, address, capacity, false);
	}

	/** 申请调用 Cellet 服务。
	 * 
	 * @note Client
	 */
	public boolean call(String identifier, InetSocketAddress address, TalkCapacity capacity, boolean http) {
		if (!http) {
			// 私有协议 Speaker

			if (null == this.speakers)
				this.speakers = new ConcurrentHashMap<String, Speaker>();

			Speaker speaker = null;

			if (this.speakers.containsKey(identifier)) {
				// 检查 Speaker 是否是 Lost 状态 
				speaker = this.speakers.get(identifier);
				if (speaker.lost) {
					speaker.lost = false;
				}
			}
			else {
				speaker = new Speaker(identifier, this, this.block, capacity);
				this.speakers.put(identifier, speaker);
			}

			// 复位参数
			speaker.reset();

			// Call
			return speaker.call(address);
		}
		else {
			// HTTP 协议 Speaker

			if (null == this.httpSpeakers)
				this.httpSpeakers = new ConcurrentHashMap<String, HttpSpeaker>();

			HttpSpeaker speaker = null;

			if (this.httpSpeakers.containsKey(identifier)) {
				speaker = this.httpSpeakers.get(identifier);
			}
			else {
				speaker = new HttpSpeaker(identifier, this, 2);
				this.httpSpeakers.put(identifier, speaker);
			}

			// Call
			return speaker.call(address);
		}
	}

	/** 挂起 Cellet 调用。
	 * 
	 * @note Client
	 */
	public void suspend(final String identifier, final long duration) {
		if (null == this.speakers || !this.speakers.containsKey(identifier))
			return;

		Speaker speaker = this.speakers.get(identifier);
		if (null != speaker) {
			speaker.suspend(duration);
		}
	}

	/** 恢复 Cellet 调用。
	 * 
	 * @note Client
	 */
	public void resume(final String identifier, final long startTime) {
		if (null == this.speakers || !this.speakers.containsKey(identifier))
			return;

		Speaker speaker = this.speakers.get(identifier);
		if (null != speaker) {
			speaker.resume(startTime);
		}
	}

	/** 挂断 Cellet 服务。
	 * 
	 * @note Client
	 */
	public void hangUp(String identifier) {
		if (null != this.speakers && this.speakers.containsKey(identifier)) {
			Speaker speaker = this.speakers.get(identifier);
			speaker.hangUp();
			this.speakers.remove(identifier);
		}

		if (null != this.httpSpeakers && this.httpSpeakers.containsKey(identifier)) {
			HttpSpeaker hs = this.httpSpeakers.get(identifier);
			hs.hangUp();
			this.httpSpeakers.remove(identifier);
		}
	}

	/** 向指定 Cellet 发送原语。
	 * 
	 * @note Client
	 */
	public boolean talk(final String identifier, final Primitive primitive) {
		if (null != this.speakers) {
			Speaker speaker = this.speakers.get(identifier);
			if (null != speaker) {
				// Speak
				return speaker.speak(primitive);
			}
		}

		if (null != this.httpSpeakers) {
			HttpSpeaker hs = this.httpSpeakers.get(identifier);
			if (null != hs) {
				// Speak
				return hs.speak(primitive);
			}
		}

		return false;
	}

	/** 向指定 Cellet 发送方言。
	 * 
	 * @note Client
	 */
	public boolean talk(final String identifier, final Dialect dialect) {
		if (null == this.speakers && null == this.httpSpeakers)
			return false;

		Primitive primitive = dialect.translate();
		if (null != primitive) {
			return this.talk(identifier, primitive);
		}

		return false;
	}

	/** 是否已经与 Cellet 建立服务。
	 * 
	 * @note Client
	 */
	public boolean isCalled(final String identifier) {
		if (null != this.speakers) {
			Speaker speaker = this.speakers.get(identifier);
			if (null != speaker) {
				return speaker.isCalled();
			}
		}

		if (null != this.httpSpeakers) {
			HttpSpeaker hs = this.httpSpeakers.get(identifier);
			if (null != hs) {
				return hs.isCalled();
			}
		}

		return false;
	}
	/** Cellet 服务是否已经被挂起。
	 * 
	 * @note Client
	 */
	public boolean isSuspended(final String identifier) {
		if (null == this.speakers)
			return false;

		Speaker speaker = this.speakers.get(identifier);
		if (null != speaker) {
			return speaker.isSuspended();
		}

		return false;
	}

	/** 启动 HTTP 服务。
	 */
	private void startHttpService() {
		if (null == HttpService.getInstance()) {
			Logger.w(TalkService.class, "Starts talk http service failed, http model is not started.");
			return;
		}

		// 创建 Session 管理器
		this.httpSessionManager = new CookieSessionManager();

		// 添加监听器
		this.httpSessionListener = new HttpSessionListener();
		this.httpSessionManager.addSessionListener(this.httpSessionListener);

		// 创建服务节点
		HttpCapsule capsule = new HttpCapsule(this.httpPort, 1000);
		// 设置 Session 管理器
		capsule.setSessionManager(this.httpSessionManager);
		// 依次添加 Holder 点
		capsule.addHolder(new HttpInterrogationHandler(this));
		capsule.addHolder(new HttpCheckHandler(this));
		capsule.addHolder(new HttpRequestHandler(this));
		capsule.addHolder(new HttpDialogueHandler(this));
		capsule.addHolder(new HttpHeartbeatHandler());

		// 添加 HTTP 服务节点
		HttpService.getInstance().addCapsule(capsule);
	}

	/**
	 * 通知 Dialogue 。
	 */
	@Override
	public void onDialogue(Speakable speaker, Primitive primitive) {
		if (null == this.listeners) {
			return;
		}

		String identifier = speaker.getIdentifier();
		synchronized (this.listeners) {
			for (TalkListener listener : this.listeners) {
				listener.dialogue(identifier, primitive);
			}
		}
	}

	/**
	 * 通知新连接。
	 */
	@Override
	public void onContacted(Speakable speaker) {
		if (null == this.listeners) {
			return;
		}

		String identifier = speaker.getIdentifier();
		String tag = speaker.getRemoteTag();
		synchronized (this.listeners) {
			for (TalkListener listener : this.listeners) {
				listener.contacted(identifier, tag);
			}
		}
	}

	/**
	 * 通知断开连接。
	 */
	@Override
	public void onQuitted(Speakable speaker) {
		if (null == this.listeners) {
			return;
		}

		String identifier = speaker.getIdentifier();
		String tag = speaker.getRemoteTag();
		synchronized (this.listeners) {
			for (TalkListener listener : this.listeners) {
				listener.quitted(identifier, tag);
			}
		}
	}

	/**
	 * 通知挂起。
	 */
	@Override
	public void onSuspended(Speakable speaker, long timestamp, int mode) {
		if (null == this.listeners) {
			return;
		}

		String identifier = speaker.getIdentifier();
		String tag = speaker.getRemoteTag();
		synchronized (this.listeners) {
			for (TalkListener listener : this.listeners) {
				listener.suspended(identifier, tag, timestamp, mode);
			}
		}
	}

	/**
	 * 通知恢复。
	 */
	@Override
	public void onResumed(Speakable speaker, long timestamp, Primitive primitive) {
		if (null == this.listeners) {
			return;
		}

		String identifier = speaker.getIdentifier();
		String tag = speaker.getRemoteTag();
		synchronized (this.listeners) {
			for (TalkListener listener : this.listeners) {
				listener.resumed(identifier, tag, timestamp, primitive);
			}
		}
	}

	/**
	 * 通知发生错误。
	 */
	@Override
	public void onFailed(Speakable speaker, TalkServiceFailure failure) {
		if (null == this.listeners) {
			return;
		}

		String identifier = speaker.getIdentifier();
		String tag = speaker.getRemoteTag();
		synchronized (this.listeners) {
			for (TalkListener listener : this.listeners) {
				listener.failed(identifier, tag, failure);
			}
		}
	}

	/** 开启 Session 。
	 */
	protected Certificate openSession(Session session) {
		Long sid = session.getId();
		if (this.unidentifiedSessions.containsKey(sid)) {
			return this.unidentifiedSessions.get(sid);
		}

		Certificate cert = new Certificate();
		cert.session = session;
		cert.key = Utils.randomString(8);
		cert.plaintext = Utils.randomString(16);
		this.unidentifiedSessions.put(sid, cert);

		return cert;
	}

	/** 关闭 Session 。
	 */
	protected synchronized void closeSession(final Session session) {
		TalkSessionContext ctx = this.sessionContexts.get(session);
		if (null != ctx) {
			// 遍历此 Session 所访问的所有 Cellet
			Map<String, TalkTracker> map = ctx.getTrackers();
			Iterator<Map.Entry<String, TalkTracker>> iter = map.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, TalkTracker> entry = iter.next();
				String tag = entry.getKey();
				TalkTracker tracker = entry.getValue();

				// 判断是否需要进行挂起
				if (tracker.isAutoSuspend()) {
					// 将消费者挂起，被动挂起
					this.suspendTalk(tracker, SuspendMode.PASSIVE);
					if (null != tracker.activeCellet) {
						// 通知 Cellet 对端挂起
						tracker.activeCellet.suspended(tag);
					}
				}
				else if (this.suspendedTrackers.containsKey(tag)) {
					// 已经挂起的对端，判断是否有指定 Cellet 上的挂起记录
					if (null != tracker.activeCellet) {
						SuspendedTracker st = this.suspendedTrackers.get(tag);
						if (!st.exist(tracker.activeCellet)) {
							// 没有记录，对端退出
							tracker.activeCellet.quitted(tag);
						}
						else {
							// 有记录，对端挂起
							// FIXME 01/01/2013 如果已经挂起说明之前已经是主动挂起了，
							// 不需要再回调事件。
							//tracker.activeCellet.suspended(tag);
						}
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
						this.tagList.remove(tag);
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

		if (!(session instanceof HttpSession)) {
			this.acceptor.close(session);
		}
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
			this.tagList.add(tag);
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
			// 尝试恢复被动挂起的 Talk
			if (this.tryResumeTalk(tag, cellet, SuspendMode.PASSIVE, 0)) {
				// 回调 resumed
				cellet.resumed(tag);
			}
			else {
				// 回调 contacted
				cellet.contacted(tag);
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
				// 设置原语的 Cellet 标识
				primitive.setCelletIdentifier(tracker.activeCellet.getFeature().getIdentifier());
				primitive.setCellet(tracker.activeCellet);
				// 回调
				tracker.activeCellet.dialogue(speakerTag, primitive);
			}
		}
	}

	/** 挂起指定的会话。
	 */
	protected boolean processSuspend(Session session, String speakerTag, long duration) {
		TalkSessionContext ctx = this.sessionContexts.get(session);
		if (null == ctx) {
			return false;
		}

		TalkTracker talkTracker = ctx.getTracker(speakerTag);
		if (null != talkTracker) {
			// 进行主动挂起
			SuspendedTracker st = this.suspendTalk(talkTracker, SuspendMode.INITATIVE);
			if (null != st) {
				// 更新有效时长
				st.liveDuration = duration;

				// 回调 Cellet 接口
				talkTracker.activeCellet.suspended(speakerTag);

				return true;
			}
		}

		return false;
	}

	/** 恢复指定的会话。
	 */
	protected void processResume(Session session, String speakerTag, long startTime) {
		TalkSessionContext ctx = this.sessionContexts.get(session);
		if (null == ctx) {
			return;
		}

		TalkTracker tt = ctx.getTracker(speakerTag);
		if (null == tt || null == tt.activeCellet) {
			return;
		}

		// 尝试回送原语
		if (this.tryResumeTalk(speakerTag, tt.activeCellet, SuspendMode.INITATIVE, startTime)) {
			// 回调恢复
			tt.activeCellet.resumed(speakerTag);
		}
	}

	/** 恢复之前被挂起的原语。
	 */
	protected void noticeResume(Cellet cellet, String targetTag
			, Queue<Long> timestampQueue, Queue<Primitive> primitiveQueue, long startTime) {
		Vector<TalkSessionContext> contexts = this.tagSessionsMap.get(targetTag);
		if (null == contexts) {
			if (Logger.isDebugLevel()) {
				Logger.d(TalkService.class, "Not find session by remote tag");
			}
			return;
		}

		Message message = null;

		synchronized (contexts) {
			for (TalkSessionContext ctx : contexts) {
				// 查找上文里指定的会话追踪器
				TalkTracker tracker = ctx.getTracker(targetTag);
				if (null != tracker) {
					// 判断是否是同一个 Cellet
					if (tracker.activeCellet == cellet) {
						Session session = ctx.getSession();

						// 发送所有原语
						for (int i = 0, size = timestampQueue.size(); i < size; ++i) {
							Long timestamp = timestampQueue.poll();
							Primitive primitive = primitiveQueue.poll();
							if (timestamp.longValue() >= startTime) {
								message = this.packetResume(targetTag, timestamp, primitive);
								if (null != message) {
									session.write(message);
								}
							}
						}

						break;
					}
				}
			}
		}
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

		Iterator<Map.Entry<Long, Certificate>> iter = this.unidentifiedSessions.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<Long, Certificate> e = iter.next();
			Certificate cert = e.getValue();
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

				// 从记录中删除
				this.unidentifiedSessions.remove(session.getId());

				if (session instanceof HttpSession) {
					// 删除 HTTP 的 Session
					this.httpSessionManager.unmanage((HttpSession)session);
				}
				else {
					// 关闭私有协议的 Session
					this.acceptor.close(session);
				}
			}

			sessionList.clear();
			sessionList = null;
		}
	}

	/** 更新 Session tick time 。
	 */
	protected void updateSessionTickTime(Session session) {
		TalkSessionContext ctx = this.sessionContexts.get(session);
		if (null != ctx) {
			ctx.tickTime = this.daemon.getTickTime();

			if (Logger.isDebugLevel()) {
				Logger.d(this.getClass(), "Talk service heartbeat from " + session.getAddress().getAddress().getHostAddress()
						+ ":" + session.getAddress().getPort());
			}
		}
	}

	/** 返回时间点。
	 */
	protected long getTickTime() {
		return this.daemon.getTickTime();
	}

	/** 检查 HTTP Session 心跳。
	 */
	protected void checkHttpSessionHeartbeat() {
		if (null == this.httpSessionManager) {
			return;
		}

		this.executor.execute(new Runnable() {
			@Override
			public void run() {
				long time = daemon.getTickTime();
				List<HttpSession> list = httpSessionManager.getSessions();
				for (HttpSession session : list) {
					if (time - session.getHeartbeat() > httpSessionTimeout) {
						httpSessionManager.unmanage(session);
					}
				}
			}
		});
	}

	/** 检查并删除挂起的会话。
	 */
	protected void checkAndDeleteSuspendedTalk() {
		// 两个判断依据，满足任一一个时即可进行删除：
		// 1、挂起会话超时
		// 2、挂起会话所标识的消费端已经和 Cellet 重建连接

		if (null == this.tagSessionsMap) {
			return;
		}

		// 检查超时的挂起会话
		Iterator<Map.Entry<String, SuspendedTracker>> eiter = this.suspendedTrackers.entrySet().iterator();
		while (eiter.hasNext()) {
			Map.Entry<String, SuspendedTracker> entry = eiter.next();
			SuspendedTracker tracker = entry.getValue();
			if (tracker.isTimeout()) {
				// 如果当前指定的对端已经不在线则，通知 Cellet 对端已退出。
				if (!this.tagSessionsMap.containsKey(tracker.getTag())) {
					// 回调退出函数
					List<Cellet> list = tracker.getCelletList();
					for (int i = 0, size = list.size(); i < size; ++i) {
						list.get(i).quitted(entry.getKey());
					}
				}

				// 删除对应标签的挂起记录
				eiter.remove();
			}
		}
	}

	/** 挂起会话。
	 */
	private SuspendedTracker suspendTalk(TalkTracker talkTracker, int suspendMode) {
		if (null == talkTracker.activeCellet) {
			return null;
		}

		if (this.suspendedTrackers.containsKey(talkTracker.getTag())) {
			SuspendedTracker tracker = this.suspendedTrackers.get(talkTracker.getTag());
			tracker.track(talkTracker.activeCellet, suspendMode);
			return tracker;
		}

		SuspendedTracker tracker = new SuspendedTracker(talkTracker.getTag());
		tracker.track(talkTracker.activeCellet, suspendMode);
		tracker.liveDuration = talkTracker.getSuspendDuration();
		this.suspendedTrackers.put(talkTracker.getTag(), tracker);
		return tracker;
	}

	/** 尝试恢复被动会话。
	 */
	private synchronized boolean tryResumeTalk(String tag, Cellet cellet, int suspendMode, long startTime) {
		SuspendedTracker tracker = this.suspendedTrackers.get(tag);
		if (null != tracker) {
			boolean ret = tracker.pollPrimitiveMatchMode(this.executor, cellet, suspendMode, startTime);
			if (ret) {
				tracker.retreat(cellet);
				return true;
			}
		}

		return false;
	}

	/** 尝试记录挂起会话的原语。
	 */
	private boolean tryOfferPrimitive(String tag, Cellet cellet, Primitive primitive) {
		SuspendedTracker tracker = this.suspendedTrackers.get(tag);
		if (null != tracker) {
			tracker.offerPrimitive(cellet, System.currentTimeMillis(), primitive);
			return true;
		}

		return false;
	}

	/** 向指定 Session 发送识别指令。
	 */
	private void deliverChecking(Session session, String text, String key) {
		// 如果是来自 HTTP 协议的 Session 则直接返回
		if (session instanceof HttpSession) {
			return;
		}

		// 包格式：密文|密钥

		byte[] ciphertext = Cryptology.getInstance().simpleEncrypt(text.getBytes(), key.getBytes());

		Packet packet = new Packet(TalkDefinition.TPT_INTERROGATE, 1, 1, 0);
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

	private Message packetResume(String targetTag, Long timestamp, Primitive primitive) {
		// 包格式：目的标签|时间戳|原语序列

		// 序列化原语
		ByteArrayOutputStream stream = primitive.write();

		// 封装数据包
		Packet packet = new Packet(TalkDefinition.TPT_RESUME, 6, 1, 0);
		packet.appendSubsegment(Utils.string2Bytes(targetTag));
		packet.appendSubsegment(Utils.string2Bytes(timestamp.toString()));
		packet.appendSubsegment(stream.toByteArray());

		// 打包数据
		byte[] data = Packet.pack(packet);
		Message message = new Message(data);
		return message;
	}

	/** 打包对话原语。
	 */
	private Message packetDialogue(Primitive primitive) {
		// 包格式：原语序列

		// 序列化原语
		ByteArrayOutputStream stream = primitive.write();

		// 封装数据包
		Packet packet = new Packet(TalkDefinition.TPT_DIALOGUE, 99, 1, 0);
		packet.setBody(stream.toByteArray());

		// 打包数据
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
