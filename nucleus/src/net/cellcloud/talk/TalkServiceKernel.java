/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2017 Cell Cloud Team (www.cellcloud.net)

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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.cellcloud.common.Cryptology;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.MessageInterceptor;
import net.cellcloud.common.NonblockingAcceptor;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Service;
import net.cellcloud.common.Session;
import net.cellcloud.core.Cellet;
import net.cellcloud.core.CelletSandbox;
import net.cellcloud.core.Endpoint;
import net.cellcloud.core.Nucleus;
import net.cellcloud.core.NucleusContext;
import net.cellcloud.core.Role;
import net.cellcloud.exception.InvalidException;
import net.cellcloud.gateway.GatewayService;
import net.cellcloud.gateway.Hostlink;
import net.cellcloud.http.CapsuleHolder;
import net.cellcloud.http.CookieSessionManager;
import net.cellcloud.http.HttpCapsule;
import net.cellcloud.http.HttpService;
import net.cellcloud.http.HttpSession;
import net.cellcloud.http.WebSocketManager;
import net.cellcloud.http.WebSocketSession;
import net.cellcloud.talk.dialect.ActionDialect;
import net.cellcloud.talk.dialect.ActionDialectFactory;
import net.cellcloud.talk.dialect.ChunkDialect;
import net.cellcloud.talk.dialect.ChunkDialectFactory;
import net.cellcloud.talk.dialect.Dialect;
import net.cellcloud.talk.dialect.DialectEnumerator;
import net.cellcloud.talk.http.HttpCheckHandler;
import net.cellcloud.talk.http.HttpDialogueHandler;
import net.cellcloud.talk.http.HttpHeartbeatHandler;
import net.cellcloud.talk.http.HttpInterceptable;
import net.cellcloud.talk.http.HttpInterrogationHandler;
import net.cellcloud.talk.http.HttpQuickHandler;
import net.cellcloud.talk.http.HttpRequestHandler;
import net.cellcloud.talk.http.HttpSessionListener;
import net.cellcloud.talk.http.HttpSpeaker;
import net.cellcloud.talk.speaker.Speaker;
import net.cellcloud.talk.speaker.SpeakerDelegate;
import net.cellcloud.talk.stuff.PrimitiveSerializer;
import net.cellcloud.talk.stuff.StuffVersion;
import net.cellcloud.util.Clock;
import net.cellcloud.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 会话服务内核。
 *
 * @author Ambrose Xu
 * 
 */
public final class TalkServiceKernel implements Service, SpeakerDelegate {

	private int port;
	private int block;
	private int maxConnections;
	private int numWorkerThreads;

	private final long sessionTimeout;

	// 服务器端是否启用 HTTP 服务
	private boolean httpEnabled;
	private int httpQueueSize;

	private int httpPort;
	private int httpsPort;

	private CookieSessionManager httpSessionManager;
	private HttpSessionListener httpSessionListener;
	private long httpSessionTimeout;

	private WebSocketMessageHandler wsHandler;
	private WebSocketManager wsManager;

	private WebSocketMessageHandler wssHandler;
	private WebSocketManager wssManager;

	private NonblockingAcceptor acceptor;
	private NucleusContext nucleusContext;
	private TalkAcceptorHandler talkHandler;

	// 线程执行器
	protected ExecutorService executor;

	/// 待检验 Session
	private ConcurrentHashMap<Long, Certificate> unidentifiedSessions;
	/// Session 与 Tag 的映射
	private ConcurrentHashMap<Long, String> sessionTagMap;
	/// Tag 与 Session context 的映射
	private ConcurrentHashMap<String, TalkSessionContext> tagContexts;

	/// Tag 缓存列表
	private ConcurrentSkipListSet<String> tagList;

	// 私有协议 Speaker
	private ConcurrentHashMap<String, Speaker> speakerMap;
	protected LinkedList<Speaker> speakers;
	// HTTP 协议 Speaker
	private ConcurrentHashMap<String, HttpSpeaker> httpSpeakerMap;
	protected Vector<HttpSpeaker> httpSpeakers;

	private TalkServiceDaemon daemon;
	private ArrayList<TalkListener> listeners;

	private CelletCallbackListener callbackListener;

	private TalkDelegate delegate;

	private LinkedList<CapsuleHolder> extendHttpHolders;

	// 用于兼容 Flash Socket 安全策略的适配器
	private FlashSocketSecurity fss;

	// 已处理合法连接数量
	private long numValidSessions = 0;
	// 已处理非法连接数量
	private long numInvalidSessions = 0;

	/** 上位主机与终端之间的连接关系。 */
	private Hostlink hostlink;

	/**
	 * 构造函数。
	 * 
	 * @param nucleusContext 指定内核上下文。
	 */
	public TalkServiceKernel(NucleusContext nucleusContext) {
		if (null != nucleusContext) {
			nucleusContext.talkKernel = this;
			this.nucleusContext = nucleusContext;
		}

		this.port = 7000;
		this.block = 65536;
		this.maxConnections = 5000;
		this.numWorkerThreads = 8;

		this.httpEnabled = true;
		this.httpQueueSize = 1000;

		this.httpPort = 7070;
		this.httpsPort = 7080;

		// 15 分钟
		this.sessionTimeout = 15L * 60L * 1000L;

		// 30 分钟
		this.httpSessionTimeout = 30L * 60L * 1000L;

		// 创建执行器
		this.executor = Executors.newCachedThreadPool();

		this.callbackListener = DialectEnumerator.getInstance();
		this.delegate = DialectEnumerator.getInstance();
	}

	/**
	 * 生成系统瞬时快照。
	 * @return
	 */
	public TalkSnapshoot snapshot() {
		TalkSnapshoot ts = new TalkSnapshoot();
		ts.numValidSessions = this.numValidSessions;
		ts.numInvalidSessions = this.numInvalidSessions;

		if (null != this.acceptor) {
			ts.port = this.getPort();
			ts.connections = this.acceptor.numSessions();
			ts.maxConnections = this.acceptor.getMaxConnectNum();
			ts.numWorkers = this.acceptor.getWorkerNum();
			ts.networkRx = this.acceptor.getWorkersRx();
			ts.networkTx = this.acceptor.getWorkersTx();
		}

		if (null != HttpService.getInstance()) {
			if (null != this.wsHandler) {
				ts.webSocketPort = HttpService.getInstance().getWebSocketPort();
				ts.webSocketConnections = HttpService.getInstance().getWebSocketSessionNum();
				ts.webSocketIdleTasks = this.wsHandler.numIdleTasks();
				ts.webSocketActiveTasks = this.wsHandler.numActiveTasks();
				ts.webSocketRx = HttpService.getInstance().getTotalWSRx();
				ts.webSocketTx = HttpService.getInstance().getTotalWSTx();
			}

			if (null != this.wssHandler) {
				ts.webSocketSecurePort = HttpService.getInstance().getWebSocketSecurePort();
				ts.webSocketSecureConnections = HttpService.getInstance().getWebSocketSecureSessionNum();
				ts.webSocketSecureIdleTasks = this.wssHandler.numIdleTasks();
				ts.webSocketSecureActiveTasks = this.wssHandler.numActiveTasks();
				ts.webSocketSecureRx = HttpService.getInstance().getTotalWSSRx();
				ts.webSocketSecureTx = HttpService.getInstance().getTotalWSSTx();
			}

			ts.httpPort = HttpService.getInstance().getHttpPort();
			ts.httpsPort = HttpService.getInstance().getHttpsPort();
			ts.httpQueueSize = this.httpQueueSize;
			ts.httpSessionNum = this.httpSessionManager.getSessionNum();
			ts.httpSessionMaxNum = this.httpSessionManager.getMaxSessionNum();
			ts.httpSessionExpires = this.httpSessionManager.getSessionExpires();
		}

		ActionDialectFactory adf = (ActionDialectFactory) DialectEnumerator.getInstance().getFactory(ActionDialect.DIALECT_NAME);
		ts.actionDialectThreadNum = adf.getThreadCounts();
		ts.actionDialectMaxThreadNum = adf.getMaxThreadCounts();
		ts.actionDialectPendingNum = adf.getPendingNum();

		ChunkDialectFactory cdf = (ChunkDialectFactory) DialectEnumerator.getInstance().getFactory(ChunkDialect.DIALECT_NAME);
		ts.chunkDialectCacheNum = cdf.getCacheNum();
		ts.chunkDialectCacheMemSize = cdf.getCacheMemorySize();
		ts.chunkDialectMaxCacheMemSize = cdf.getMaxCacheMemorySize();
		ts.chunkDialectQueueSize = cdf.getSListSize();

		return ts;
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
		if (null == this.sessionTagMap) {
			this.sessionTagMap = new ConcurrentHashMap<Long, String>();
		}
		if (null == this.tagContexts) {
			this.tagContexts = new ConcurrentHashMap<String, TalkSessionContext>();
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
		this.acceptor.setMaxConnectNum(this.maxConnections);
		// 工作线程数
		this.acceptor.setWorkerNum(this.numWorkerThreads);

		// 启动 acceptor
		boolean succeeded = this.acceptor.bind(this.port);
		if (succeeded) {
			this.startDaemon();
		}

		if (succeeded && this.httpEnabled) {
			// 启动 HTTP 服务
			startHttpService();
		}

		// 启动 FSS
		if (null == this.fss) {
			this.fss = new FlashSocketSecurity();
			if (!this.fss.startup()) {
				Logger.e(TalkServiceKernel.class, "Start Flash socket security policy failed.");
			}
		}

		// 数据写间隔
		this.acceptor.setEachSessionWriteInterval(20L);

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
			HttpCapsule hc = HttpService.getInstance().getCapsule("ts");
			HttpService.getInstance().removeCapsule(hc);
		}

		if (null != this.tagContexts) {
			// 关闭所有会话
			Iterator<Map.Entry<String, TalkSessionContext>> iter = this.tagContexts.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, TalkSessionContext> entry = iter.next();
				TalkSessionContext ctx = entry.getValue();
				for (Session session : ctx.getSessions()) {
					TalkTracker tracker = ctx.getTracker(session);
					TalkCapacity capacity = tracker.getCapacity();
					for (Cellet cellet : tracker.getCelletList()) {
						if (capacity.proxy) {
							cellet.proxyQuitted(ctx.getTag());
						}
						else {
							cellet.quitted(ctx.getTag());
						}
					}
				}
			}

			this.sessionTagMap.clear();
			this.tagContexts.clear();
			this.tagList.clear();
		}

		if (null != this.speakers) {
			synchronized (this.speakers) {
				for (Speaker speaker : this.speakers) {
					speaker.hangUp();
				}
				this.speakers.clear();
			}
		}
		if (null != this.httpSpeakers) {
			for (HttpSpeaker speaker : this.httpSpeakers) {
				speaker.hangUp();
			}
			this.httpSpeakers.clear();
		}
		if (null != this.speakerMap) {
			this.speakerMap.clear();
		}
		if (null != this.httpSpeakerMap) {
			this.httpSpeakerMap.clear();
		}

		if (null != this.fss) {
			this.fss.shutdown();
			this.fss = null;
		}
	}

	/** 设置服务端口。
	 *
	 * @note 在 startup 之前设置才能生效。
	 * @param port 指定服务监听端口。
	 */
	public void setPort(int port) throws InvalidException {
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

	/**
	 * 设置适配器缓存块大小。
	 * @param size
	 */
	public void setBlockSize(int size) {
		this.block = size;
	}

	public int getBlockSize() {
		return this.block;
	}

	/**
	 * 设置最大连接数。
	 * @param num 指定最大连接数。
	 */
	public void setMaxConnections(int num) throws InvalidException {
		if (null != this.acceptor && this.acceptor.isRunning()) {
			throw new InvalidException("Can't set the max number of connections in talk service after the start");
		}

		this.maxConnections = num;
	}

	public int getMaxConnections() {
		return this.maxConnections;
	}

	/**
	 * 设置工作线程数。
	 * 
	 * @param num
	 */
	public void setWorkerThreadNum(int num) throws InvalidException {
		if (null != this.acceptor && this.acceptor.isRunning()) {
			throw new InvalidException("Can't set the number of worker thread in talk service after the start");
		}

		this.numWorkerThreads = num;
	}

	public int getWorkerThreadNum() {
		return this.numWorkerThreads;
	}

	public void setEachSessionReadInterval(long intervalInMillisecond) {
		if (null == this.acceptor) {
			return;
		}

		this.acceptor.setEachSessionReadInterval(intervalInMillisecond);
	}

	public long getEachSessionReadInterval() {
		return (null != this.acceptor) ? this.acceptor.getEachSessionReadInterval() : -1;
	}

	public void setEachSessionWriteInterval(long intervalInMillisecond) {
		if (null == this.acceptor) {
			return;
		}

		this.acceptor.setEachSessionWriteInterval(intervalInMillisecond);
	}

	public long getEachSessionWriteInterval() {
		return (null != this.acceptor) ? this.acceptor.getEachSessionWriteInterval() : -1;
	}

	public void setSessionTransmissionQuota(int quotaInBytesPerSecond) {
		if (null == this.acceptor) {
			return;
		}

		this.acceptor.setTransmissionQuota(quotaInBytesPerSecond);
	}

	public int getSessionTransmissionQuota() {
		return (null != this.acceptor) ? this.acceptor.getTransmissionQuota() : -1;
	}

	/** 设置是否激活 HTTP 服务。
	 */
	public void httpEnabled(boolean enabled) throws InvalidException {
		if (null != HttpService.getInstance() && HttpService.getInstance().hasCapsule("ts")) {
			throw new InvalidException("Can't set the http enabled in talk service after the start");
		}

		this.httpEnabled = enabled;
	}

	/**
	 * 设置 HTTP 服务端口。
	 * 
	 * @param port
	 * @note 在 startup 之前设置才能生效。
	 */
	public void setHttpPort(int port) {
		if (null != this.acceptor && this.acceptor.isRunning()) {
			throw new InvalidException("Can't set the http port in talk service after the start");
		}

		this.httpPort = port;
	}

	/**
	 * 设置 HTTPS 服务端口。
	 * 
	 * @param port
	 */
	public void setHttpsPort(int port) {
		if (null != this.acceptor && this.acceptor.isRunning()) {
			throw new InvalidException("Can't set the http port in talk service after the start");
		}

		this.httpsPort = port;
	}

	/**
	 * 返回 HTTP 服务端口。
	 * @return
	 */
	public int getHttpPort() {
		return this.httpPort;
	}

	/**
	 * 返回 HTTPS 服务端口。
	 * @return
	 */
	public int getHttpsPort() {
		return this.httpsPort;
	}

	/** 设置 HTTP 服务的允许接收连接的队列长度。
	 * @param value
	 */
	public void setHttpQueueSize(int value) {
		this.httpQueueSize = value;
	}

	/** 设置 HTTP 会话超时时间。
	 */
	public void settHttpSessionTimeout(long timeoutInMillisecond) {
		this.httpSessionTimeout = timeoutInMillisecond;
	}

	/** 启动任务表守护线程。
	 */
	public void startDaemon() {
		if (null == this.daemon) {
			this.daemon = new TalkServiceDaemon(this);
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
					Thread.sleep(10L);
				} catch (InterruptedException e) {
					Logger.log(TalkServiceKernel.class, e, LogLevel.DEBUG);
				}
			}

			this.daemon = null;
		}
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

	/**
	 * 向对端 Speaker 发送原语。
	 * 
	 * @param targetTag 指定目标的内核标签。
	 * @param primitive 指定发送的原语数据。
	 * @param cellet 指定源 Cellet 。
	 * @param sandbox 指定 Cellet 对应的沙盒。
	 * @return 如果原语数据被正确送入发送队列返回 <code>true</code> 。
	 */
	public boolean notice(String targetTag, Primitive primitive, Cellet cellet, CelletSandbox sandbox) {
		// 检查 Cellet 合法性
		if (!Nucleus.getInstance().checkSandbox(cellet, sandbox)) {
			Logger.w(TalkServiceKernel.class, "Illegal cellet : " + cellet.getFeature().getIdentifier());
			return false;
		}

		String verifiedTag = targetTag;
		String note = null;

		TalkSessionContext context = this.tagContexts.get(targetTag);
		if (null == context) {
			// 判断是否是代理
			if (null != this.hostlink) {
				verifiedTag = this.hostlink.searchHost(targetTag);
				if (null != verifiedTag) {
					context = this.tagContexts.get(verifiedTag);
				}

				if (null == context) {
					Logger.w(TalkServiceKernel.class, "Can't find target tag in hostlink : " + targetTag);
					// 因为没有直接发送出去原语，所以返回 false
					return false;
				}
				else {
					note = targetTag;
				}
			}
			else {
				Logger.w(TalkServiceKernel.class, "Can't find target tag in context list : " + targetTag);
				// 因为没有直接发送出去原语，所以返回 false
				return false;
			}
		}

		Message message = null;

		synchronized (context) {
			for (Session session : context.getSessions()) {
				// 返回 tracker
				TalkTracker tracker = context.getTracker(session);

				if (tracker.hasCellet(cellet)) {

					// 兼容性处理
					StuffVersion sv = CompatibilityHelper.match(tracker.getCapacity().getVersionNumber());
					// 设置语素版本
					primitive.setVersion(sv);

					// 对方言进行是否劫持处理
					if (null != this.callbackListener && primitive.isDialectal()) {
						boolean ret = this.callbackListener.doTalk(cellet, targetTag, primitive.getDialect());
						if (!ret) {
							// 劫持会话
							return true;
						}
					}

					// 检查是否加密连接
					TalkCapacity cap = tracker.getCapacity();
					if (null != cap && cap.secure && !session.isSecure()) {
						session.activeSecretKey((byte[]) session.getAttribute("key"));
					}

					// 打包
					message = this.packetDialogue(cellet, primitive, (session instanceof WebSocketSession), note);

					if (null != message) {
						session.write(message);
					}
					else {
						Logger.e(this.getClass(), "Packet error");
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

	public boolean kick(final String targetTag, final Cellet cellet, final CelletSandbox sandbox) {
		// 检查 Cellet 合法性
		if (!Nucleus.getInstance().checkSandbox(cellet, sandbox)) {
			Logger.w(TalkServiceKernel.class, "Illegal cellet : " + cellet.getFeature().getIdentifier());
			return false;
		}

		TalkSessionContext context = this.tagContexts.get(targetTag);
		if (null == context) {
			if (Logger.isDebugLevel()) {
				Logger.w(TalkServiceKernel.class, "Can't find target tag in context list : " + targetTag);
			}

			// 因为没有直接发送出去原语，所以返回 false
			return false;
		}

		ArrayList<Session> list = new ArrayList<Session>();
		synchronized (context) {
			List<Session> slist = context.getSessions();
			list.addAll(slist);
		}

		for (Session session : list) {
			this.acceptor.close(session);
		}

		list.clear();
		list = null;

		return true;
	}

	/**
	 * 返回指定 Tag 的会话数量。
	 */
	public int numSessions(String tag) {
		TalkSessionContext ctx = this.tagContexts.get(tag);
		if (null == ctx) {
			return 0;
		}

		return ctx.numSessions();
	}

	/** 申请调用 Cellet 服务。
	 * 
	 * @note Client
	 */
	public boolean call(String[] identifiers, InetSocketAddress address) {
		ArrayList<String> list = new ArrayList<String>(identifiers.length);
		for (String identifier : identifiers) {
			list.add(identifier);
		}
		return this.call(list, address);
	}

	/** 申请调用 Cellet 服务。
	 * 
	 * @note Client
	 */
	public boolean call(String[] identifiers, InetSocketAddress address, TalkCapacity capacity) {
		ArrayList<String> list = new ArrayList<String>(identifiers.length);
		for (String identifier : identifiers) {
			list.add(identifier);
		}
		return this.call(list, address, capacity);
	}

	/** 申请调用 Cellet 服务。
	 * 
	 * @note Client
	 */
	public boolean call(List<String> identifiers, InetSocketAddress address) {
		return this.call(identifiers, address, null, false);
	}

	/**
	 * 申请调用 Cellet 服务。
	 * 
	 * @param identifier
	 * @param address
	 * @return
	 * 
	 * @note Client
	 */
	public boolean call(List<String> identifiers, InetSocketAddress address, boolean http) {
		return this.call(identifiers, address, null, http);
	}

	/**
	 * 申请调用 Cellet 服务。
	 * 
	 * @param identifier
	 * @param address
	 * @param capacity
	 * @return
	 * 
	 * @note Client
	 */
	public boolean call(List<String> identifiers, InetSocketAddress address, TalkCapacity capacity) {
		return this.call(identifiers, address, capacity, false);
	}

	/** 申请调用 Cellet 服务。
	 * 
	 * @note Client
	 */
	public synchronized boolean call(List<String> identifiers, InetSocketAddress address, TalkCapacity capacity, boolean http) {
		if (!http) {
			// 私有协议 Speaker

			if (null == this.speakers) {
				this.speakers = new LinkedList<Speaker>();
				this.speakerMap = new ConcurrentHashMap<String, Speaker>();
			}

			for (String identifier : identifiers) {
				if (this.speakerMap.containsKey(identifier)) {
					// 列表里已经有对应的 Cellet，不允许再次 Call
					return false;
				}
			}

			// 创建新的 Speaker
			Speaker speaker = new Speaker(address, this, this.block, capacity);
			synchronized (this.speakers) {
				this.speakers.add(speaker);
			}

			// FIXME 28/11/14 原先在 call 检查 Speaker 是否是 Lost 状态，如果 lost 是 true，则置为 false
			// 复位 Speaker 参数
			//speaker.reset();

			for (String identifier : identifiers) {
				this.speakerMap.put(identifier, speaker);
			}

			// Call
			return speaker.call(identifiers);
		}
		else {
			// HTTP 协议 Speaker

			// TODO
			if (null == this.httpSpeakers) {
				this.httpSpeakers= new Vector<HttpSpeaker>();
				this.httpSpeakerMap = new ConcurrentHashMap<String, HttpSpeaker>();
			}

			for (String identifier : identifiers) {
				if (this.httpSpeakerMap.containsKey(identifier)) {
					// 列表里已经有对应的 Cellet，不允许再次 Call
					return false;
				}
			}

			// 创建新的 HttpSpeaker
			HttpSpeaker speaker = new HttpSpeaker(address, this, 30, this.executor);
			this.httpSpeakers.add(speaker);

			for (String identifier : identifiers) {
				this.httpSpeakerMap.put(identifier, speaker);
			}

			// Call
			return speaker.call(identifiers);
		}
	}

	public void hangUp(String[] identifiers) {
		ArrayList<String> list = new ArrayList<String>(identifiers.length);
		for (String id : identifiers) {
			list.add(id);
		}

		this.hangUp(list);
	}

	/** 挂断 Cellet 服务。
	 * 
	 * @note Client
	 */
	public void hangUp(List<String> identifiers) {
		if (null != this.speakerMap) {
			for (String identifier : identifiers) {
				if (this.speakerMap.containsKey(identifier)) {
					// 删除
					Speaker speaker = this.speakerMap.remove(identifier);

					// 删除其他关联的 speaker
					for (String celletIdentifier : speaker.getIdentifiers()) {
						this.speakerMap.remove(celletIdentifier);
					}

					synchronized (this.speakers) {
						this.speakers.remove(speaker);
					}

					speaker.hangUp();
				}
			}
		}

		if (null != this.httpSpeakerMap) {
			for (String identifier : identifiers) {
				if (this.httpSpeakerMap.containsKey(identifier)) {
					HttpSpeaker speaker = this.httpSpeakerMap.remove(identifier);

					for (String celletIdentifier : speaker.getIdentifiers()) {
						this.httpSpeakerMap.remove(celletIdentifier);
					}

					this.httpSpeakers.remove(speaker);

					speaker.hangUp();
				}
			}
		}
	}

	/** 向指定 Cellet 发送原语。
	 * 
	 * @note Client
	 */
	public boolean talk(String identifier, Primitive primitive) {
		if (null != this.speakerMap) {
			Speaker speaker = this.speakerMap.get(identifier);
			if (null != speaker) {
				// Speak
				return speaker.speak(identifier, primitive);
			}
		}

		if (null != this.httpSpeakerMap) {
			HttpSpeaker speaker = this.httpSpeakerMap.get(identifier);
			if (null != speaker) {
				// Speak
				return speaker.speak(identifier, primitive);
			}
		}

		return false;
	}

	/** 向指定 Cellet 发送方言。
	 * 
	 * @note Client
	 */
	public boolean talk(String identifier, Dialect dialect) {
		if (null == this.speakerMap && null == this.httpSpeakerMap)
			return false;

		// 通知委派
		if (null != this.delegate) {
			boolean ret = this.delegate.doTalk(identifier, dialect);
			if (!ret) {
				// 委派劫持，依然返回 true
				return true;
			}
		}

		Primitive primitive = dialect.translate();
		if (null != primitive) {
			boolean ret = this.talk(identifier, primitive);

			// 发送成功，通知委派
			if (ret && null != this.delegate) {
				this.delegate.didTalk(identifier, dialect);
			}

			return ret;
		}

		return false;
	}

	public Speaker getSpeaker(String identifier) {
		return this.speakerMap.get(identifier);
	}

	public boolean proxy(String identifier, JSONObject data) {
		if (null != this.speakerMap) {
			Speaker speaker = this.speakerMap.get(identifier);
			if (null != speaker) {
				Packet packet = new Packet(TalkDefinition.TPT_PROXY, 20, 1, 0);
				packet.appendSubsegment(Utils.string2Bytes(data.toString()));
				speaker.pass(Packet.pack(packet));
				return true;
			}
		}

		return false;
	}

	public boolean pass(String identifier, byte[] data) {
		if (null != this.speakerMap) {
			Speaker speaker = this.speakerMap.get(identifier);
			if (null != speaker) {
				speaker.pass(data);
				return true;
			}
		}

		return false;
	}

	/** 是否已经与 Cellet 建立服务。
	 * 
	 * @note Client
	 */
	public boolean isCalled(final String identifier) {
		if (null != this.speakerMap) {
			Speaker speaker = this.speakerMap.get(identifier);
			if (null != speaker) {
				return speaker.isCalled();
			}
		}

		if (null != this.httpSpeakerMap) {
			HttpSpeaker speaker = this.httpSpeakerMap.get(identifier);
			if (null != speaker) {
				return speaker.isCalled();
			}
		}

		return false;
	}

	public synchronized void addExtendHolder(CapsuleHolder holder) {
		if (null == this.extendHttpHolders) {
			this.extendHttpHolders = new LinkedList<CapsuleHolder>();
		}

		if (this.extendHttpHolders.contains(holder)) {
			return;
		}

		this.extendHttpHolders.add(holder);
	}

	public synchronized void removeExtendHolder(CapsuleHolder holder) {
		if (null == this.extendHttpHolders) {
			return;
		}

		this.extendHttpHolders.remove(holder);
	}

	protected void startExtendHolder() {
		HttpCapsule capsule = HttpService.getInstance().getCapsule("ts");
		if (null != capsule && null != this.extendHttpHolders) {
			for (CapsuleHolder holder : this.extendHttpHolders) {
				capsule.addHolder(holder);
			}
		}
	}

	/** 启动 HTTP 服务。
	 */
	private void startHttpService() {
		if (null == HttpService.getInstance()) {
			Logger.w(TalkServiceKernel.class, "Starts talk http service failed, http service is not started.");
			return;
		}

		long idleTimeout = 30L * 60L * 1000L;

		// 激活 HTTP 服务
		HttpService.getInstance().activateHttp(new int[]{this.httpPort}, idleTimeout, this.httpQueueSize);

		// 激活 HTTPS 服务
		HttpService.getInstance().activateHttpSecure(this.httpsPort, idleTimeout, this.httpQueueSize,
				Nucleus.getInstance().getConfig().talk.keystore,
				Nucleus.getInstance().getConfig().talk.keyStorePassword,
				Nucleus.getInstance().getConfig().talk.keyManagerPassword);

		// 激活 WS 服务
		this.wsHandler = new WebSocketMessageHandler(this);
		this.wsManager = HttpService.getInstance().activeWebSocket(this.httpPort + 1
				, this.httpQueueSize, this.wsHandler);

		// 激活 WSS 服务
		this.wssHandler = new WebSocketMessageHandler(this);
		this.wssManager = HttpService.getInstance().activeWebSocketSecure(this.httpsPort + 1
				, this.httpQueueSize, this.wssHandler
				, Nucleus.getInstance().getConfig().talk.keystore
				, Nucleus.getInstance().getConfig().talk.keyStorePassword
				, Nucleus.getInstance().getConfig().talk.keyManagerPassword);
		if (null == this.wssManager) {
			this.wssHandler = null;
		}

		// 创建 Session 管理器
		this.httpSessionManager = new CookieSessionManager();

		// 添加监听器
		this.httpSessionListener = new HttpSessionListener(this);
		this.httpSessionManager.addSessionListener(this.httpSessionListener);

		// 创建服务节点
		HttpCapsule capsule = new HttpCapsule("ts");
		// 设置 Session 管理器
		capsule.setSessionManager(this.httpSessionManager);
		// 依次添加 Holder 点
		capsule.addHolder(new HttpInterrogationHandler(this));
		capsule.addHolder(new HttpQuickHandler(this));
		capsule.addHolder(new HttpCheckHandler(this));
		capsule.addHolder(new HttpRequestHandler(this));
		capsule.addHolder(new HttpDialogueHandler(this));
		capsule.addHolder(new HttpHeartbeatHandler(this));

		// 添加 HTTP 服务节点
		HttpService.getInstance().addCapsule(capsule);
	}

	/**
	 * 通知 Dialogue 。
	 */
	@Override
	public void onDialogue(Speakable speaker, String identifier, Primitive primitive) {
		boolean delegated = (null != this.delegate && primitive.isDialectal());
		if (delegated) {
			boolean ret = this.delegate.doDialogue(identifier, primitive.getDialect());
			if (!ret) {
				// 劫持对话
				return;
			}
		}

		if (null != this.listeners) {
			synchronized (this.listeners) {
				for (int i = 0; i < this.listeners.size(); ++i) {
					this.listeners.get(i).dialogue(identifier, primitive);
				}
			}
		}

		if (delegated) {
			this.delegate.didDialogue(identifier, primitive.getDialect());
		}
	}

	/**
	 * 通知新连接。
	 */
	@Override
	public void onContacted(Speakable speaker, String identifier) {
		if (null == this.listeners) {
			return;
		}

		String tag = speaker.getRemoteTag();
		synchronized (this.listeners) {
			for (int i = 0; i < this.listeners.size(); ++i) {
				this.listeners.get(i).contacted(identifier, tag);
			}
		}
	}

	/**
	 * 通知断开连接。
	 */
	@Override
	public void onQuitted(Speakable speaker, String identifier) {
		if (null == this.listeners) {
			return;
		}

		String tag = speaker.getRemoteTag();
		synchronized (this.listeners) {
			for (int i = 0; i < this.listeners.size(); ++i) {
				this.listeners.get(i).quitted(identifier, tag);
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

		String tag = speaker.getRemoteTag();
		synchronized (this.listeners) {
			for (int i = 0; i < this.listeners.size(); ++i) {
				this.listeners.get(i).failed(tag, failure);
			}
		}
	}

	/**
	 * 查找指定的 Endpoint 。
	 * 
	 * @param remoteTag
	 * @return
	 */
	public Endpoint findEndpoint(String remoteTag) {
		TalkSessionContext ctx = this.tagContexts.get(remoteTag);
		if (null != ctx) {
			return ctx.getEndpoint();
		}

		return null;
	}

	/**
	 * 判断对应的 Tag 是否是代理。
	 * 
	 * @param remoteTag
	 * @return
	 */
	public boolean isProxy(String remoteTag) {
		TalkSessionContext ctx = this.tagContexts.get(remoteTag);
		if (null != ctx) {
			return ctx.getTrackers().get(0).getCapacity().proxy;
		}

		return false;
	}

	/**
	 * 设置数据接收器的数据拦截器。
	 * 
	 * @param interceptor 指定消息数据拦截器。
	 */
	public void setInterceptor(MessageInterceptor interceptor, HttpInterceptable httpInterceptable) {
		this.acceptor.setInterceptor(interceptor);

		if (null != this.wsManager) {
			this.wsManager.setInterceptor(interceptor);
		}
		if (null != this.wssManager) {
			this.wssManager.setInterceptor(interceptor);
		}

		if (null != httpInterceptable && null != this.httpSessionManager) {
			HttpCapsule cap = HttpService.getInstance().getCapsule("ts");
			List<CapsuleHolder> list = cap.getHolders();
			for (CapsuleHolder ch : list) {
				if (ch instanceof HttpDialogueHandler) {
					HttpDialogueHandler handler = (HttpDialogueHandler) ch;
					handler.setInterceptor(httpInterceptable);
					break;
				}
			}
		}
	}

	/** 开启 Session 。
	 */
	public synchronized Certificate openSession(Session session) {
		Long sid = session.getId();
		if (this.unidentifiedSessions.containsKey(sid)) {
			return this.unidentifiedSessions.get(sid);
		}

		// 生成随机 Key
		String key = Utils.randomString(8);

		// 将密钥记录到会话属性里
		session.addAttribute("key", Utils.string2Bytes(key));

		Certificate cert = new Certificate();
		cert.session = session;
		cert.key = key;
		cert.plaintext = Utils.randomString(16);
		this.unidentifiedSessions.put(sid, cert);

		return cert;
	}

	/** 关闭 Session 。
	 */
	public synchronized void closeSession(final Session session) {
		String tag = this.sessionTagMap.get(session.getId());
		if (null != tag) {
			TalkSessionContext ctx = this.tagContexts.get(tag);
			if (null != ctx) {
				if (!this.executor.isShutdown()) {
					// 关闭 Socket
					this.executor.execute(new Runnable() {
						@Override
						public void run() {
							if (acceptor.existSession(session)) {
								acceptor.close(session);
							}
						}
					});
				}

				// 先取出 tracker
				TalkTracker tracker = ctx.getTracker(session);

				// 从上下文移除 Session
				ctx.removeSession(session);

				if (ctx.numSessions() == 0) {
					Logger.i(this.getClass(), "Clear session: " + tag);

					// 清理上下文记录
					this.tagContexts.remove(tag);
					this.tagList.remove(tag);
				}

				// 进行回调
				if (null != tracker) {
					TalkCapacity capacity = tracker.getCapacity();
					for (Cellet cellet : tracker.getCelletList()) {
						if (capacity.proxy) {
							// proxyQuitted
							cellet.proxyQuitted(tag);

							// 将该代理上的所有被代理终端正确 quitted
							if (null != this.hostlink) {
								// 查找所有下位标签
								List<String> list = this.hostlink.listLinkedTag(tag);
								for (String targetTag : list) {
									cellet.quitted(targetTag);
									// 删除下位机
									this.hostlink.removeLink(targetTag);
								}
							}
						}
						else {
							// proxyQuitted
							cellet.quitted(tag);
						}
					}
				}
			}

			// 删除此条会话记录
			this.sessionTagMap.remove(session.getId());
		}
		else {
			Logger.d(this.getClass(), "Can NOT find tag with session: " + session.getAddress().getHostString());
		}

		// 清理未授权表
		this.unidentifiedSessions.remove(session.getId());
	}

	/** 允许指定 Session 连接。
	 */
	public synchronized void acceptSession(Session session, String tag) {
		Long sid = session.getId();
		this.unidentifiedSessions.remove(sid);

		// Session -> Tag
		this.sessionTagMap.put(session.getId(), tag);

		// Tag -> Context
		TalkSessionContext ctx = this.tagContexts.get(tag);
		if (null != ctx) {
			// 该 Tag 已存在则增加 session
			ctx.addSession(session);
		}
		else {
			// 创建新的上下文
			ctx = new TalkSessionContext(tag, session);
			ctx.dialogueTickTime = this.getTickTime();
			this.tagContexts.put(tag, ctx);
		}

		// 缓存 Tag
		if (!this.tagList.contains(tag)) {
			this.tagList.add(tag);
		}

		// 计数
		++this.numValidSessions;
	}

	/** 拒绝指定 Session 连接。
	 */
	public synchronized void rejectSession(Session session) {
		Long sid = session.getId();

		StringBuilder log = new StringBuilder();
		log.append("Talk service reject session (");
		log.append(sid);
		log.append("): ");
		log.append(session.getAddress().getAddress().getHostAddress());
		log.append(":");
		log.append(session.getAddress().getPort());
		Logger.w(TalkServiceKernel.class, log.toString());
		log = null;

		this.unidentifiedSessions.remove(sid);

		// 删除 Tag context
		String tag = this.sessionTagMap.remove(sid);
		if (null != tag) {
			TalkSessionContext ctx = this.tagContexts.get(tag);
			if (null != ctx) {
				ctx.removeSession(session);
			}
		}

		if (!(session instanceof HttpSession)
			&& !(session instanceof WebSocketSession)) {
			this.acceptor.close(session);
		}
		else if (session instanceof WebSocketSession) {
			WebSocketSession ws = (WebSocketSession) session;
			if (null != this.wssManager && this.wssManager.hasSession(ws)) {
				this.wssManager.close(ws);
			}
			else {
				this.wsManager.close(ws);
			}
		}

		// 计数
		++this.numInvalidSessions;
	}

	/** 请求 Cellet 。
	 */
	public TalkTracker processRequest(final Session session, final String tag, final String identifier) {
		TalkSessionContext ctx = this.tagContexts.get(tag);
		if (null == ctx) {
			Logger.w(TalkServiceKernel.class, "Can NOT find tag: " + tag);
			return null;
		}

		Cellet cellet = null;
		if (this.nucleusContext.role == Role.GATEWAY) {
			GatewayService gateway = Nucleus.getInstance().getGatewayService();
			cellet = gateway.updateRouting(session, tag, identifier);
		}
		else {
			cellet = Nucleus.getInstance().getCellet(identifier, this.nucleusContext);
		}

		TalkTracker tracker = null;

		if (null != cellet) {
			TalkCapacity capacity = null;
			synchronized (ctx) {
				tracker = ctx.getTracker(session);
				capacity = tracker.getCapacity();

				if (null != tracker && !tracker.hasCellet(cellet)) {
					tracker.addCellet(cellet);
				}

				// 使用互斥体处理
				Object mutex = null;
				if (session.hasAttribute("mutex")) {
					mutex = session.getAttribute("mutex");
				}
				else {
					mutex = new Object();
					session.addAttribute("mutex", mutex);
				}
				synchronized (mutex) {
					mutex.notifyAll();
				}

				Thread thread = new Thread("Mutex-" + tag) {
					@Override
					public void run() {
						Object mutex = session.getAttribute("mutex");
						if (null != mutex) {
							synchronized (mutex) {
								try {
									mutex.wait(1000L);
								} catch (InterruptedException e) {
									Logger.log(this.getClass(), e, LogLevel.ERROR);
								}
							}
						}

						try {
							Thread.sleep(100L);
						} catch (InterruptedException e) {
							Logger.log(this.getClass(), e, LogLevel.ERROR);
						}

						TalkSessionContext ctx = tagContexts.get(tag);
						if (null != ctx) {
							TalkTracker tt = ctx.getTracker(session);
							TalkCapacity capacity = (null != tt) ? tt.getCapacity() : null;
							if (null != capacity) {
								if (capacity.secure && !session.isSecure()) {
									boolean ret = session.activeSecretKey((byte[]) session.getAttribute("key"));
									if (ret) {
										Endpoint ep = ctx.getEndpoint();
										Logger.i(Speaker.class, "Active secret key for client: " + ep.getHost() + ":" + ep.getPort());
									}
								}
								else if (!capacity.secure && session.isSecure()) {
									session.deactiveSecretKey();
								}
							}
						}

						session.removeAttribute("mutex");
					}
				};
				thread.start();
			}

			// 判断是否是代理
			if (capacity.proxy) {
				// 回调 proxyContacted
				cellet.proxyContacted(tag);

				if (null == this.hostlink) {
					this.hostlink = new Hostlink();
				}
			}
			else {
				// 回调 contacted
				cellet.contacted(tag);
			}
		}

		return tracker;
	}

	public boolean processProxy(String proxyTag, String tag, String identifier, boolean active) {
		// 查找 Cellet
		Cellet cellet = Nucleus.getInstance().getCellet(identifier, this.nucleusContext);
		if (null == cellet) {
			return false;
		}

		if (active) {
			// 添加记录
			this.hostlink.addLink(tag, proxyTag);

			// 回调对应 Cellet 的 contacted
			cellet.contacted(tag);
		}
		else {
			// 删除记录
			this.hostlink.removeLink(tag);

			// 回调对应 Cellet 的 quitted
			cellet.quitted(tag);
		}

		return true;
	}

	/** 协商服务能力。
	 */
	public TalkCapacity processConsult(Session session, String tag, TalkCapacity capacity) {
		TalkSessionContext ctx = this.tagContexts.get(tag);
		if (null == ctx) {
			return capacity;
		}

		// 协商终端能力
		TalkTracker tracker = ctx.getTracker(session);
		if (null != tracker) {
			tracker.setCapacity(capacity);
		}
		else {
			Logger.e(this.getClass(), "Can not find talk tracker for session: " + session.getAddress().getHostString());
		}

		return capacity;
	}

	/** 对话 Cellet 。
	 */
	public void processDialogue(Session session, String speakerTag, String targetIdentifier, Primitive primitive) {
		TalkSessionContext ctx = this.tagContexts.get(speakerTag);

		// 判断是否是来自网关
		if (null == ctx && null != this.hostlink) {
			String verifiedTag = this.hostlink.searchHost(speakerTag);
			if (null != verifiedTag) {
				ctx = this.tagContexts.get(verifiedTag);
			}
		}

		if (null != ctx) {
			TalkTracker tracker = ctx.getTracker(session);
			if (null != tracker) {
				// 更新时间
				ctx.dialogueTickTime = this.getTickTime();

				Cellet cellet = tracker.getCellet(targetIdentifier);
				if (null != cellet) {
					primitive.setCelletIdentifier(cellet.getFeature().getIdentifier());
					primitive.setCellet(cellet);

					if (null != this.callbackListener && primitive.isDialectal()) {
						boolean ret = this.callbackListener.doDialogue(cellet, speakerTag, primitive.getDialect());
						if (!ret) {
							// 被劫持，直接返回
							return;
						}
					}

					// 回调 Cellet
					cellet.dialogue(speakerTag, primitive);
				}
			}
			else {
				Logger.e(TalkServiceKernel.class, "Can NOT find talk tracker in 'tracker': " + speakerTag +
						" from " + session.getAddress().getHostString());
			}
		}
		else {
			Logger.e(TalkServiceKernel.class, "Can NOT find speaker tag in 'tagContexts': " + speakerTag +
					" from " + session.getAddress().getHostString());
		}
	}

	/** 返回 Session 证书。
	 */
	public Certificate getCertificate(Session session) {
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
				// 20 秒超时检测
				if (time - cert.time > 20000L) {
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
				Logger.i(TalkServiceKernel.class, log.toString());
				log = null;

				// 计数
				++this.numInvalidSessions;

				// 从记录中删除
				this.unidentifiedSessions.remove(session.getId());

				if (session instanceof HttpSession) {
					// 删除 HTTP 的 Session
					this.httpSessionManager.unmanage((HttpSession)session);
				}
				else if (session instanceof WebSocketSession) {
					// 删除 WebSocket 的 Session
					WebSocketSession ws = (WebSocketSession) session;
					if (null != this.wssManager && this.wssManager.hasSession(ws)) {
						this.wssManager.close(ws);
					}
					else {
						this.wsManager.close(ws);
					}
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
	public void updateSessionHeartbeat(Session session) {
		String tag = this.sessionTagMap.get(session.getId());
		if (null == tag) {
			return;
		}

		TalkSessionContext ctx = this.tagContexts.get(tag);
		if (null != ctx) {
			ctx.updateSessionHeartbeat(session, this.getTickTime());

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

	/**
	 * 检查会话是否超时。
	 * @param timeout
	 */
	protected void checkSessionHeartbeat() {
		if (null == this.tagContexts) {
			return;
		}

		LinkedList<Session> closeList = new LinkedList<Session>();

		for (Map.Entry<String, TalkSessionContext> entry : this.tagContexts.entrySet()) {
			TalkSessionContext ctx = entry.getValue();

			List<Session> sl = ctx.getSessions();
			for (Session s : sl) {
				long time = ctx.getSessionHeartbeat(s);
				if (time == 0) {
					continue;
				}

				if (this.daemon.getTickTime() - time > this.sessionTimeout) {
					// 超时的 Session 添加到关闭列表
					closeList.add(s);
					Logger.d(this.getClass(), "Session timeout in heartbeat: " + s.getAddress().getHostString());
				}
			}
		}

		for (Session session : closeList) {
			this.closeSession(session);
		}

		closeList.clear();
		closeList = null;
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

	/** 向指定 Session 发送识别指令。
	 */
	private void deliverChecking(Session session, String text, String key) {
		// 如果是来自 HTTP 协议的 Session 则直接返回
		if (session instanceof HttpSession) {
			return;
		}

		// 是否是 WebSocket 的 Session
		if (session instanceof WebSocketSession) {
			WebSocketSession ws = (WebSocketSession) session;

			byte[] ciphertext = Cryptology.getInstance().simpleEncrypt(text.getBytes(), key.getBytes());
			JSONObject data = new JSONObject();
			try {
				JSONObject packet = new JSONObject();
				// {"ciphertext": ciphertext, "key": key}
				packet.put(HttpInterrogationHandler.Ciphertext, Cryptology.getInstance().encodeBase64(ciphertext));
				packet.put(HttpInterrogationHandler.Key, key);

				data.put(WebSocketMessageHandler.TALK_PACKET_TAG, WebSocketMessageHandler.TPT_INTERROGATE);
				data.put(WebSocketMessageHandler.TALK_PACKET_VERSION, "1.1");
				data.put(WebSocketMessageHandler.TALK_PACKET, packet);
			} catch (JSONException e) {
				Logger.log(TalkServiceKernel.class, e, LogLevel.ERROR);
			}

			Message message = new Message(data.toString().getBytes(Charset.forName("UTF-8")));
			if (null != this.wssManager && this.wssManager.hasSession(ws)) {
				this.wssManager.write(ws, message);
			}
			else {
				this.wsManager.write(ws, message);
			}
			message = null;
			return;
		}

		// 包格式：密文|密钥

		byte[] ciphertext = Cryptology.getInstance().simpleEncrypt(text.getBytes(), key.getBytes());

		// 使用 1.1 版包结构，支持 QUICK 快速握手
		Packet packet = new Packet(TalkDefinition.TPT_INTERROGATE, 1, 1, 1);
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
	private Message packetDialogue(Cellet cellet, Primitive primitive, boolean jsonFormat, String note) {
		Message message = null;

		if (jsonFormat) {
			try {
				JSONObject primJson = new JSONObject();
				PrimitiveSerializer.write(primJson, primitive);

				JSONObject packet = new JSONObject();
				packet.put(HttpDialogueHandler.Primitive, primJson);
				packet.put(HttpDialogueHandler.Identifier, cellet.getFeature().getIdentifier());
				if (null != note) {
					packet.put(HttpDialogueHandler.Note, note);
				}

				JSONObject data = new JSONObject();
				data.put(WebSocketMessageHandler.TALK_PACKET_TAG, WebSocketMessageHandler.TPT_DIALOGUE);
				data.put(WebSocketMessageHandler.TALK_PACKET, packet);

				// 创建 message
				message = new Message(data.toString().getBytes(Charset.forName("UTF-8")));
			} catch (JSONException e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			}
		}
		else {
			// 包格式：原语序列|Cellet

			// 序列化原语
			ByteArrayOutputStream stream = primitive.write();

			// 封装数据包
			Packet packet = new Packet(TalkDefinition.TPT_DIALOGUE, 99, 1, 0);
			packet.appendSubsegment(stream.toByteArray());
			packet.appendSubsegment(Utils.string2Bytes(cellet.getFeature().getIdentifier()));
			if (null != note) {
				packet.appendSubsegment(Utils.string2Bytes(note));
			}

			// 打包数据
			byte[] data = Packet.pack(packet);
			message = new Message(data);
		}

		return message;
	}

	/**
	 * 会话身份证书。
	 */
	public class Certificate {
		/** 构造函数。 */
		protected Certificate() {
			this.session = null;
			this.key = null;
			this.plaintext = null;
			this.time = Clock.currentTimeMillis();
			this.checked = false;
		}

		/// 相关 Session
		public Session session;
		/// 密钥
		public String key;
		/// 明文
		public String plaintext;
		/// 时间戳
		public long time;
		/// 是否已经发送校验请求
		public boolean checked;
	}

}
