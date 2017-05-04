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

	/** 标准服务的端口。 */
	private int port;

	/** 每个工作线程的数据块大小。 */
	private int block;

	/** 最大连接数。 */
	private int maxConnections;

	/** 工作线程数量。 */
	private int numWorkerThreads;

	/** 会话超时时间。 */
	private long sessionTimeout;

	/** 服务器端是否启用 HTTP 服务。 */
	private boolean httpEnabled;

	/** HTTP 服务的待处理队列长度。 */
	private int httpQueueSize;

	/** HTTP 服务端口。 */
	private int httpPort;

	/** HTTPS 服务端口。 */
	private int httpsPort;

	/**
	 * HTTP 会话管理器。
	 */
	private CookieSessionManager httpSessionManager;
	/**
	 * HTTP 会话监听器。
	 */
	private HttpSessionListener httpSessionListener;

	/** HTTP 会话超时时间。 */
	private long httpSessionTimeout;

	/**
	 * WebSocket 消息服务处理器。
	 */
	private WebSocketMessageHandler wsHandler;
	/**
	 * WebSocket 服务管理器。
	 */
	private WebSocketManager wsManager;

	/**
	 * WebSocket Secure 消息服务处理器。
	 */
	private WebSocketMessageHandler wssHandler;
	/**
	 * WebSocket Secure 服务管理器。
	 */
	private WebSocketManager wssManager;

	/**
	 * 用于标准协议的非阻塞接收器。
	 */
	private NonblockingAcceptor acceptor;

	/**
	 * 内核上下文。
	 */
	private NucleusContext nucleusContext;

	/**
	 * 对话服务事件及消息处理器。
	 */
	private TalkAcceptorHandler talkHandler;

	/**
	 * 线程执行器。
	 */
	protected ExecutorService executor;

	/**
	 * 存储待验证的 Session 。键是 Session 的 ID，值是服务为此 Session 生成的临时证书。
	 */
	private ConcurrentHashMap<Long, Certificate> unidentifiedSessions;

	/**
	 * 存储 Session 与内核标签的映射关系。
	 * 键是 Session 的 ID，值是内核标签。
	 */
	private ConcurrentHashMap<Long, String> sessionTagMap;

	/**
	 * 存储内核标签对应的对话 Session 的上下文。
	 * 上下文里记录了与此标签相关的所有连接信息。
	 * 键是内核标签，值是 {@link TalkSessionContext} 对象。
	 */
	private ConcurrentHashMap<String, TalkSessionContext> tagContexts;

	/**
	 * 存储当前与服务器连接的所有终端的标签的集合。
	 */
	private ConcurrentSkipListSet<String> tagList;

	/**
	 * 存储标准协议的 Speaker 对象。
	 * 键是 Speaker 连接的 Cellet 的标识，值是 Speaker 对象。
	 */
	private ConcurrentHashMap<String, Speaker> speakerMap;

	/**
	 * 存储所有 Speaker 实例的列表。
	 */
	protected LinkedList<Speaker> speakers;

	/**
	 * 存储 HTTP 协议的 Speaker 对象。
	 * 键是 HTTP Speaker 连接的 Cellet 的标识，值是 HTTP Speaker 对象。
	 */
	private ConcurrentHashMap<String, HttpSpeaker> httpSpeakerMap;

	/**
	 * 存储所有 HTTP Speaker 实例的列表。
	 */
	protected Vector<HttpSpeaker> httpSpeakers;

	/**
	 * 服务的守护线程。
	 */
	private TalkServiceDaemon daemon;

	/**
	 * 用于客户端模式下的对话监听器。
	 */
	private ArrayList<TalkListener> listeners;

	/**
	 * 用于 Cellet 回调的监听器。
	 */
	private CelletCallbackListener callbackListener;

	/**
	 * 对话事件委派。
	 */
	private TalkDelegate delegate;

	/**
	 * 扩展的 HTTP 服务封装。
	 */
	private LinkedList<CapsuleHolder> extendHttpHolders;

	/**
	 * 用于兼容 Flash Socket 安全策略的适配器。
	 */
	private FlashSocketSecurity fss;

	/**
	 * 已处理合法连接数量，仅用于计数。
	 */
	private long numValidSessions = 0;

	/**
	 * 已处理非法连接数量，仅用于计数。
	 */
	private long numInvalidSessions = 0;

	/**
	 * 上位主机与终端之间的连接关系。
	 */
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
	 * 生成服务的实时快照。
	 * 
	 * @return 返回当前对话服务的快照。
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
	 * {@inheritDoc}
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
				Logger.w(TalkServiceKernel.class, "Start Flash socket security policy failed.");
			}
		}

		// 数据写间隔
		this.acceptor.setEachSessionWriteInterval(20L);

		return succeeded;
	}

	/**
	 * {@inheritDoc}
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

	/**
	 * 设置服务端口。
	 * 
	 * @param port 指定服务监听端口。
	 * 
	 * @throws InvalidException
	 */
	public void setPort(int port) throws InvalidException {
		if (null != this.acceptor && this.acceptor.isRunning()) {
			throw new InvalidException("Can't set the port in talk service after the start");
		}

		this.port = port;
	}

	/**
	 * 获得服务绑定端口。
	 * 
	 * @return 返回服务绑定端口。
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * 设置数据接收器缓存块大小（字节）。
	 * 每个工作线程读写数据的缓存大小。
	 * 
	 * @param size 指定缓存大小，单位：字节。
	 */
	public void setBlockSize(int size) {
		this.block = size;
	}

	/**
	 * 获得数据接收器缓存块大小。
	 * 
	 * @return 返回接收器缓存块大小。
	 */
	public int getBlockSize() {
		return this.block;
	}

	/**
	 * 设置接收器允许的最大连接数。
	 * 
	 * @param num 指定最大连接数。
	 * 
	 * @throws InvalidException
	 */
	public void setMaxConnections(int num) throws InvalidException {
		if (null != this.acceptor && this.acceptor.isRunning()) {
			throw new InvalidException("Can't set the max number of connections in talk service after the start");
		}

		this.maxConnections = num;
	}

	/**
	 * 获得接收器允许的最大连接数。
	 * 
	 * @return 返回最大连接数。
	 */
	public int getMaxConnections() {
		return this.maxConnections;
	}

	/**
	 * 设置工作器线程数。
	 * 
	 * @param num 指定工作器线程数。
	 * 
	 * @throws InvalidException
	 */
	public void setWorkerThreadNum(int num) throws InvalidException {
		if (null != this.acceptor && this.acceptor.isRunning()) {
			throw new InvalidException("Can't set the number of worker thread in talk service after the start");
		}

		this.numWorkerThreads = num;
	}

	/**
	 * 获得工作器线程数。
	 * 
	 * @return 返回工作器线程数。
	 */
	public int getWorkerThreadNum() {
		return this.numWorkerThreads;
	}

	/**
	 * 设置每个 Session 的读取数据间隔。
	 * 
	 * @param intervalInMillisecond 以毫秒为单位的时间间隔。
	 */
	protected void setEachSessionReadInterval(long intervalInMillisecond) {
		if (null == this.acceptor) {
			return;
		}

		this.acceptor.setEachSessionReadInterval(intervalInMillisecond);
	}

	/**
	 * 获得每个 Session 的读取数据间隔。
	 * 
	 * @return 返回每个 Session 的读取数据间隔。
	 */
	public long getEachSessionReadInterval() {
		return (null != this.acceptor) ? this.acceptor.getEachSessionReadInterval() : -1;
	}

	/**
	 * 设置每个 Session 的写入数据间隔。
	 * 
	 * @param intervalInMillisecond 以毫秒为单位的时间间隔。
	 */
	public void setEachSessionWriteInterval(long intervalInMillisecond) {
		if (null == this.acceptor) {
			return;
		}

		this.acceptor.setEachSessionWriteInterval(intervalInMillisecond);
	}

	/**
	 * 获得每个 Session 的写入数据间隔。
	 * 
	 * @return 返回每个 Session 的写入数据间隔。
	 */
	public long getEachSessionWriteInterval() {
		return (null != this.acceptor) ? this.acceptor.getEachSessionWriteInterval() : -1;
	}

	/**
	 * 设置工作线程发送数据配额（字节每秒，B/S）。
	 * 
	 * @param quotaInBytesPerSecond 以字节每秒为单位的带宽配额。
	 */
	public void setWorkerTransmissionQuota(int quotaInBytesPerSecond) {
		if (null == this.acceptor) {
			return;
		}

		this.acceptor.setTransmissionQuota(quotaInBytesPerSecond);
	}

	/**
	 * 获得工作线程发送数据配额（字节每秒，B/S）。
	 * 
	 * @return 返回以字节每秒为单位的带宽配额。
	 */
	public int getWorkerTransmissionQuota() {
		return (null != this.acceptor) ? this.acceptor.getTransmissionQuota() : -1;
	}

	/**
	 * 设置是否激活 HTTP 服务。
	 * 
	 * @param enabled 是否激活 HTTP 服务。
	 * 
	 * @throws InvalidException
	 */
	public void httpEnabled(boolean enabled) throws InvalidException {
		if (null != HttpService.getInstance() && HttpService.getInstance().hasCapsule("ts")) {
			throw new InvalidException("Can't set the http enabled in talk service after the start");
		}

		this.httpEnabled = enabled;
	}

	/**
	 * 设置 HTTP 服务绑定端口。
	 * 
	 * @param port 指定 HTTP 服务绑定端口。
	 */
	public void setHttpPort(int port) throws InvalidException {
		if (null != this.acceptor && this.acceptor.isRunning()) {
			throw new InvalidException("Can't set the http port in talk service after the start");
		}

		this.httpPort = port;
	}

	/**
	 * 获得 HTTP 服务绑定端口。
	 * 
	 * @return 返回 HTTP 服务绑定端口。
	 */
	public int getHttpPort() {
		return this.httpPort;
	}

	/**
	 * 设置 HTTPS 服务绑定端口。
	 * 
	 * @param port 指定 HTTPS 服务绑定端口。
	 */
	public void setHttpsPort(int port) throws InvalidException {
		if (null != this.acceptor && this.acceptor.isRunning()) {
			throw new InvalidException("Can't set the http port in talk service after the start");
		}

		this.httpsPort = port;
	}

	/**
	 * 获得 HTTPS 服务绑定端口。
	 * 
	 * @return 返回 HTTPS 服务绑定端口。
	 */
	public int getHttpsPort() {
		return this.httpsPort;
	}

	/**
	 * 设置 HTTP 服务网络连接的队列长度。
	 * 
	 * @param size 指定队列长度。
	 */
	public void setHttpQueueSize(int size) {
		this.httpQueueSize = size;
	}

	/**
	 * 获得 HTTP 服务网络连接的队列长度。
	 * 
	 * @return 返回 HTTP 服务网络连接的队列长度。
	 */
	public int getHttpQueueSize() {
		return this.httpQueueSize;
	}

	/**
	 * 设置 HTTP 会话超时时间（毫秒）。
	 * 
	 * @param timeoutInMillisecond 指定以毫秒计算的会话超时时间。
	 */
	public void setHttpSessionTimeout(long timeoutInMillisecond) {
		this.httpSessionTimeout = timeoutInMillisecond;
	}

	/**
	 * 获得 HTTP 会话超时时间（毫秒）。
	 * 
	 * @return 返回 HTTP 会话超时时间（毫秒）。
	 */
	public long getHttpSessionTimeout() {
		return this.httpSessionTimeout;
	}

	/**
	 * 启动会话服务的守护线程。
	 */
	public void startDaemon() {
		if (null == this.daemon) {
			this.daemon = new TalkServiceDaemon(this);
		}

		if (!this.daemon.running) {
			this.daemon.start();
		}
	}

	/**
	 * 停止会话服务的守护线程。
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

	/**
	 * 添加对话监听器。仅用于客户端模式下。
	 * 
	 * @param listener 指定监听器实例。
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

	/**
	 * 移除对话监听器。仅用于客户端模式下。
	 * 
	 * @param listener 指定监听器实例。
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
	 * 是否已添加指定的监听器。仅用于客户端模式下。
	 * 
	 * @param listener 待检查的监听器。
	 * @return 如果指定的监听器已经添加返回 <code>true</code> ，否则返回 <code>false</code> 。
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
	 * 获得当前与服务有连接的终端的 Tag 列表。
	 * 
	 * @return 返回存储终端 Tag 的集合。
	 */
	public Set<String> getEndpointTagList() {
		return this.tagList;
	}

	/**
	 * 向指定标签的终端发送原语。
	 * 
	 * @param targetTag 指定目标的内核标签。
	 * @param primitive 指定发送的原语数据。
	 * @param cellet 指定源 Cellet 。
	 * @param sandbox 指定 Cellet 对应的沙盒。
	 * @return 如果数据被正确送入发送队列返回 <code>true</code> 。
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
					message = this.packetDialogue(cellet, primitive, session, note);

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

	/**
	 * 向指定标签的终端发送方言。
	 * 
	 * @param targetTag 指定目标的内核标签。
	 * @param dialect 指定发送的方言数据。
	 * @param cellet 指定源 Cellet 。
	 * @param sandbox 指定 Cellet 对应的沙盒。
	 * @return 如果数据被正确送入发送队列返回 <code>true</code> 。
	 */
	public boolean notice(String targetTag, Dialect dialect, Cellet cellet, CelletSandbox sandbox) {
		Primitive primitive = dialect.reconstruct();
		if (null != primitive) {
			return this.notice(targetTag, primitive, cellet, sandbox);
		}

		return false;
	}

	/**
	 * 将指定标签的终端踢出。
	 * 
	 * @param targetTag 指定目标的内核标签。
	 * @param cellet 指定源 Cellet 。
	 * @param sandbox 指定 Cellet 对应的沙盒。
	 * @return 踢出操作被正确处理返回 <code>true</code> 。
	 */
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
	 * 查询指定终端标签对应的连接会话数量。
	 * 
	 * @param tag 指定待查询的标签。
	 * @return 返回指定标签连接数量。
	 */
	public int numSessions(String tag) {
		TalkSessionContext ctx = this.tagContexts.get(tag);
		if (null == ctx) {
			return 0;
		}

		return ctx.numSessions();
	}

	/**
	 * 调用指定标识的 Cellet 服务。
	 * 
	 * @param identifiers 指定需调用的 Cellet 的标识清单。
	 * @param address 指定服务器连接地址和端口。
	 * @return 如果调用请求被成功发送给服务器返回 <code>true</code>，否则返回 <code>false</code> 。
	 */
	public boolean call(String[] identifiers, InetSocketAddress address) {
		ArrayList<String> list = new ArrayList<String>(identifiers.length);
		for (String identifier : identifiers) {
			list.add(identifier);
		}
		return this.call(list, address);
	}

	/**
	 * 调用指定标识的 Cellet 服务。
	 * 
	 * @param identifiers 指定需调用的 Cellet 的标识清单。
	 * @param address 指定服务器连接地址和端口。
	 * @param capacity 指定协商信息。
	 * @return 如果调用请求被成功发送给服务器返回 <code>true</code>，否则返回 <code>false</code> 。
	 */
	public boolean call(String[] identifiers, InetSocketAddress address, TalkCapacity capacity) {
		ArrayList<String> list = new ArrayList<String>(identifiers.length);
		for (String identifier : identifiers) {
			list.add(identifier);
		}
		return this.call(list, address, capacity);
	}

	/**
	 * 调用指定标识的 Cellet 服务。
	 * 
	 * @param identifiers 指定需调用的 Cellet 的标识清单。
	 * @param address 指定服务器连接地址和端口。
	 * @return 如果调用请求被成功发送给服务器返回 <code>true</code>，否则返回 <code>false</code> 。
	 */
	public boolean call(List<String> identifiers, InetSocketAddress address) {
		return this.call(identifiers, address, null, false);
	}

	/**
	 * 调用指定标识的 Cellet 服务。
	 * 
	 * @param identifiers 指定需调用的 Cellet 的标识清单。
	 * @param address 指定服务器连接地址和端口。
	 * @param http 是否使用 HTTP 协议进行连接。
	 * @return 如果调用请求被成功发送给服务器返回 <code>true</code>，否则返回 <code>false</code> 。
	 */
	public boolean call(List<String> identifiers, InetSocketAddress address, boolean http) {
		return this.call(identifiers, address, null, http);
	}

	/**
	 * 调用指定标识的 Cellet 服务。
	 * 
	 * @param identifiers 指定需调用的 Cellet 的标识清单。
	 * @param address 指定服务器连接地址和端口。
	 * @param capacity 指定协商信息。
	 * @return 如果调用请求被成功发送给服务器返回 <code>true</code>，否则返回 <code>false</code> 。
	 */
	public boolean call(List<String> identifiers, InetSocketAddress address, TalkCapacity capacity) {
		return this.call(identifiers, address, capacity, false);
	}

	/**
	 * 调用指定标识的 Cellet 服务。
	 * 
	 * @param identifiers 指定需调用的 Cellet 的标识清单。
	 * @param address 指定服务器连接地址和端口。
	 * @param capacity 指定协商信息。
	 * @param http 是否使用 HTTP 协议进行连接。
	 * @return 如果调用请求被成功发送给服务器返回 <code>true</code>，否则返回 <code>false</code> 。
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

	/**
	 * 停止指定标识的 Cellet 服务。
	 * 
	 * @param identifiers 指定需要停止的 Cellet 的标识清单。
	 */
	public void hangUp(String[] identifiers) {
		ArrayList<String> list = new ArrayList<String>(identifiers.length);
		for (String id : identifiers) {
			list.add(id);
		}

		this.hangUp(list);
	}

	/**
	 * 停止指定标识的 Cellet 服务。
	 * 
	 * @param identifiers 指定需要停止的 Cellet 的标识清单。
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

	/**
	 * 向指定的 Cellet 发送原语。
	 * 
	 * @param identifier 指定目标 Cellet 的标识。
	 * @param primitive 指定原语。
	 * @return 如果数据被成功送入发送队列返回 <code>true</code> 。
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

	/**
	 * 向指定的 Cellet 发送原语。
	 * 
	 * @param identifier 指定目标 Cellet 的标识。
	 * @param dialect 指定方言。
	 * @return 如果数据被成功送入发送队列返回 <code>true</code> 。
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

		Primitive primitive = dialect.reconstruct();
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

	/**
	 * 获得访问指定 Cellet 的 {@link Speaker} 实例。
	 * 
	 * @param identifier 指定 Cellet 的标识。
	 * @return 返回访问指定 Cellet 的 Speaker 。
	 */
	public Speaker getSpeaker(String identifier) {
		return this.speakerMap.get(identifier);
	}

	/**
	 * 以代理方式向指定 Cellet 发送代理数据。
	 * 
	 * @param identifier 目标 Cellet 的标识。
	 * @param data 指定待发送的代理数据。
	 * @return 如果数据被成功入队返回 <code>true</code> 。
	 */
	public boolean proxy(String identifier, JSONObject data) {
		if (null != this.speakerMap) {
			Speaker speaker = this.speakerMap.get(identifier);
			if (null != speaker) {
				Packet packet = new Packet(TalkDefinition.TPT_PROXY, 20, 2, 0);
				packet.appendSegment(Utils.string2Bytes(data.toString()));
				speaker.pass(Packet.pack(packet));
				return true;
			}
		}

		return false;
	}

	/**
	 * 向指定 Cellet 透传数据。
	 * 
	 * @param identifier 目标 Cellet 的标识。
	 * @param data 指定待发送数据。
	 * @return 找到对应 Cellet 的 {@link Speaker} 并成功将输入发出返回 <code>true</code> 。
	 */
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

	/**
	 * 查询是否已经请求调用了指定的 Cellet 服务。
	 * 
	 * @param identifier 指定 Cellet 的标识。
	 * @return 如果已经调用返回 <code>true</code> 。
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

	/**
	 * 添加用于 HTTP 服务的服务封装。
	 * 
	 * @param holder 指定 HTTP 封装。
	 */
	public void addExtendHolder(CapsuleHolder holder) {
		if (null == this.extendHttpHolders) {
			this.extendHttpHolders = new LinkedList<CapsuleHolder>();
		}

		synchronized (this.extendHttpHolders) {
			if (this.extendHttpHolders.contains(holder)) {
				return;
			}

			this.extendHttpHolders.add(holder);
		}
	}

	/**
	 * 移除用于 HTTP 服务的服务封装。
	 * 
	 * @param holder 指定 HTTP 封装。
	 */
	public void removeExtendHolder(CapsuleHolder holder) {
		if (null == this.extendHttpHolders) {
			return;
		}

		synchronized (this.extendHttpHolders) {
			this.extendHttpHolders.remove(holder);
		}
	}

	/**
	 * 启动扩展的 HTTP 服务。
	 */
	protected void startExtendHolder() {
		HttpCapsule capsule = HttpService.getInstance().getCapsule("ts");
		if (null != capsule && null != this.extendHttpHolders) {
			synchronized (this.extendHttpHolders) {
				for (CapsuleHolder holder : this.extendHttpHolders) {
					capsule.addHolder(holder);
				}
			}
		}
	}

	/**
	 * 启动 Talk 服务模块的 HTTP 服务。
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
	 * {@inheritDoc}
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
	 * {@inheritDoc}
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
	 * {@inheritDoc}
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
	 * {@inheritDoc}
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
	 * 查找指定终端对应的 Endpoint 实例。
	 * 
	 * @param remoteTag 指定待查找终端的内核标签。
	 * @return 如果未找到指定 Endpoint 返回 <code>null</code> 值。
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
	 * @param remoteTag 待判断的远程标签。
	 * @return 如果是代理返回 <code>true</code> 。
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

	/**
	 * 启动为指定 {@link Session} 的服务。
	 * 
	 * @param session 需开启服务的 Session 。
	 * @return 返回指定 Session 的证书。
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

	/**
	 * 关闭指定 {@link Session} ，移除该 {@link Session} 。
	 * 
	 * @param session 指定待关闭的 Session 。
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
							if (acceptor.hasSession(session)) {
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
							// quitted
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

	/**
	 * 允许指定 {@link Session} 连接。
	 * 
	 * @param session 指定允许的 Session 。
	 * @param tag 指定该 Session 对应的内核标签。
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

	/**
	 * 拒绝 {@link Session} 连接并关闭该 {@link Session} 对应的 Socket 连接。
	 * 
	 * @param session
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

	/**
	 * 处理请求 Cellet 服务。
	 * 
	 * @param session 待处理请求对应的 Session 。
	 * @param tag 待处理请求的内核标签。
	 * @param identifier 此请求的目标 Cellet 标识。
	 * @return 如果成功处理请求返回会话追踪器，否则返回 <code>null</code> 值。
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
				if (!session.hasAttribute("mutex")) {
					session.addAttribute("mutex", new Object());

					// 等待处理线程
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

	/**
	 * 处理来自代理端的代理请求。
	 * 
	 * @param proxyTag 代理端的内核标签。
	 * @param tag 代理源的内核标签。
	 * @param identifier 请求的 Cellet 的标识。
	 * @param active 用于标记建立代理关系还是停止代理管理，如果设置为 <code>true</code> 表示建立代理关系。
	 * @return 如果没有找到对应的 Cellet 返回 <code>false</code> 。
	 */
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

	/**
	 * 进行协商处理。
	 * 
	 * @param session 待处理的 Session 。
	 * @param tag 待处理 Session 的内核标签。
	 * @param capacity 协商的能力描述。
	 * @return 返回协商成功的能力描述。
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

	/**
	 * 进行对话数据数据处理。
	 * 
	 * @param session 对话源 Session 。
	 * @param speakerTag Session 对应的内核标签。
	 * @param targetIdentifier 对话目标的 Cellet 标识。
	 * @param primitive 对话的原语数据。
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

	/**
	 * 获得指定 {@link Session} 的证书。
	 * 
	 * @param session 指定 Session 。
	 * @return 返回 Session 的证书。
	 */
	public Certificate getCertificate(Session session) {
		return this.unidentifiedSessions.get(session.getId());
	}

	/**
	 * 处理未被识别的 {@link Session} ，向 {@link Session} 发送握手请求。
	 * 
	 * @param time 指定当前系统运行的时间戳。
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

	/**
	 * 更新服务器端 Session 的心跳。
	 * 
	 * @param session 待更新心跳计时的 Session 。
	 * @return 心跳更新成功返回 <code>true</code> 。
	 */
	public boolean updateSessionHeartbeat(Session session) {
		String tag = this.sessionTagMap.get(session.getId());
		if (null == tag) {
			return false;
		}

		TalkSessionContext ctx = this.tagContexts.get(tag);
		if (null != ctx) {
			ctx.updateSessionHeartbeat(session, this.getTickTime());

			if (Logger.isDebugLevel()) {
				Logger.d(this.getClass(), "Talk service heartbeat from " + session.getAddress().getAddress().getHostAddress()
						+ ":" + session.getAddress().getPort());
			}

			return true;
		}

		return false;
	}

	/**
	 * 获得当前守护任务的 Tick 时间戳。
	 * 
	 * @return 返回 Tick 时间戳。
	 */
	protected long getTickTime() {
		return this.daemon.getTickTime();
	}

	/**
	 * 校验所有会话的心跳状态。
	 * 如果发现心跳超时的会话则将其连接断开并删除。
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

	/**
	 * 校验所有 HTTP 会话的心跳状态。
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

	/**
	 * 向指定 {@link Session} 发送握手校验指令。
	 * 终端接收到指令后开始执行握手程序。
	 * 
	 * @param session 指定目标终端的 Session 。
	 * @param text 指定明文串。
	 * @param key 指定密钥。
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

		// 使用 1.1 版包结构，让客户端使用 QUICK 快速握手
		Packet packet = new Packet(TalkDefinition.TPT_INTERROGATE, 1, 1, 1);
		packet.appendSegment(ciphertext);
		packet.appendSegment(key.getBytes());

		byte[] data = Packet.pack(packet);
		if (null != data) {
			Message message = new Message(data);
			this.acceptor.write(session, message);
			message = null;
		}

		packet = null;
	}

	/**
	 * 将原语打包为原始消息数据格式。
	 * 
	 * @param cellet 源 Cellet 。
	 * @param primitive 源原语。
	 * @param session 目标 Session 。
	 * @param note 数据包注解。
	 * @return 返回打包的 {@link net.cellcloud.common.Message} 格式数据。
	 */
	private Message packetDialogue(Cellet cellet, Primitive primitive, Session session, String note) {
		Message message = null;

		if (session instanceof WebSocketSession) {
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
			// 包格式：原语序列|Cellet[|NOTE]

			// 序列化原语
			ByteArrayOutputStream stream = primitive.write();

			// 封装数据包
			Packet packet = new Packet(TalkDefinition.TPT_DIALOGUE, 99, session.major, session.minor);
			packet.appendSegment(stream.toByteArray());
			packet.appendSegment(Utils.string2Bytes(cellet.getFeature().getIdentifier()));
			if (null != note) {
				packet.appendSegment(Utils.string2Bytes(note));
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

		/** 相关 Session 。 */
		public Session session;

		/** 密钥。 */
		public String key;

		/** 明文。 */
		public String plaintext;

		/** 时间戳。 */
		public long time;

		/** 是否已经发送校验请求。 */
		public boolean checked;
	}

}
