package net.cellcloud.talk;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;

import net.cellcloud.common.Service;
import net.cellcloud.core.Cellet;
import net.cellcloud.core.CelletSandbox;
import net.cellcloud.core.Endpoint;
import net.cellcloud.core.NucleusContext;
import net.cellcloud.exception.InvalidException;
import net.cellcloud.exception.SingletonException;
import net.cellcloud.http.CapsuleHolder;
import net.cellcloud.talk.dialect.ActionDialectFactory;
import net.cellcloud.talk.dialect.ChunkDialectFactory;
import net.cellcloud.talk.dialect.Dialect;
import net.cellcloud.talk.dialect.DialectEnumerator;

/**
 * 会话服务。
 *
 * @author Ambrose Xu
 * 
 */
public class TalkService implements Service {

	private static TalkService instance = null;

	/** 服务内核实现。 */
	protected TalkServiceKernel kernel;

	/**
	 * 构造函数。
	 * 
	 * @param nucleusContext 指定内核上下文。
	 * 
	 * @throws SingletonException
	 */
	public TalkService(NucleusContext nucleusContext) throws SingletonException {
		if (null == TalkService.instance) {
			TalkService.instance = this;

			this.kernel = new TalkServiceKernel(nucleusContext);

			// 添加默认方言工厂
			DialectEnumerator.getInstance().addFactory(new ActionDialectFactory(this.kernel.executor));
			DialectEnumerator.getInstance().addFactory(new ChunkDialectFactory(this.kernel.executor));
		}
		else {
			throw new SingletonException(TalkService.class.getName());
		}
	}

	/**
	 * 获得会话服务单例。
	 * 
	 * @return 返回会话服务单例。
	 */
	public static TalkService getInstance() {
		return TalkService.instance;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean startup() {
		return this.kernel.startup();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdown() {
		this.kernel.shutdown();
	}

	/**
	 * 设置服务端口。
	 * 
	 * @param port 指定服务监听端口。
	 * 
	 * @throws InvalidException
	 */
	public void setPort(int port) throws InvalidException {
		this.kernel.setPort(port);
	}

	/**
	 * 获得服务绑定端口。
	 * 
	 * @return 返回服务绑定端口。
	 */
	public int getPort() {
		return this.kernel.getPort();
	}

	/**
	 * 设置数据接收器缓存块大小（字节）。
	 * 每个工作线程读写数据的缓存大小。
	 * 
	 * @param size 指定缓存大小，单位：字节。
	 */
	public void setBlockSize(int size) {
		this.kernel.setBlockSize(size);
	}

	/**
	 * 获得数据接收器缓存块大小。
	 * 
	 * @return 返回接收器缓存块大小。
	 */
	public int getBlockSize() {
		return this.kernel.getBlockSize();
	}

	/**
	 * 设置接收器允许的最大连接数。
	 * 
	 * @param num 指定最大连接数。
	 * 
	 * @throws InvalidException
	 */
	public void setMaxConnections(int num) throws InvalidException {
		this.kernel.setMaxConnections(num);
	}

	/**
	 * 获得接收器允许的最大连接数。
	 * 
	 * @return 返回最大连接数。
	 */
	public int getMaxConnections() {
		return this.kernel.getMaxConnections();
	}

	/**
	 * 设置工作器线程数。
	 * 
	 * @param num 指定工作器线程数。
	 * 
	 * @throws InvalidException
	 */
	public void setWorkerThreadNum(int num) throws InvalidException {
		this.kernel.setWorkerThreadNum(num);
	}

	/**
	 * 获得工作器线程数。
	 * 
	 * @return 返回工作器线程数。
	 */
	public int getWorkerThreadNum() {
		return this.kernel.getWorkerThreadNum();
	}

	/**
	 * 设置每个工作器最大允许带宽（字节每秒，B/S）。
	 * 
	 * @param bandwidthInBytesPerSecond 指定带宽，单位：字节每秒。
	 */
	public void setMaxWorkerBandwidth(int bandwidthInBytesPerSecond) {
		this.kernel.setWorkerTransmissionQuota(bandwidthInBytesPerSecond);
	}

	/**
	 * 设置是否启用 HTTP 服务器。
	 * 
	 * @param enabled 指定是否启用。
	 * 
	 * @throws InvalidException
	 */
	public void httpEnabled(boolean enabled) throws InvalidException {
		this.kernel.httpEnabled(enabled);
	}

	/**
	 * 设置 HTTP 服务绑定端口。
	 * 
	 * @param port 指定 HTTP 服务绑定端口。
	 */
	public void setHttpPort(int port) throws InvalidException {
		this.kernel.setHttpPort(port);
	}

	/**
	 * 获得 HTTP 服务绑定端口。
	 * 
	 * @return 返回 HTTP 服务绑定端口。
	 */
	public int getHttpPort() {
		return this.kernel.getHttpPort();
	}

	/**
	 * 设置 HTTPS 服务绑定端口。
	 * 
	 * @param port 指定 HTTPS 服务绑定端口。
	 */
	public void setHttpsPort(int port) throws InvalidException {
		this.kernel.setHttpsPort(port);
	}

	/**
	 * 获得 HTTPS 服务绑定端口。
	 * 
	 * @return 返回 HTTPS 服务绑定端口。
	 */
	public int getHttpsPort() {
		return this.kernel.getHttpsPort();
	}

	/**
	 * 设置 HTTP 服务网络连接的队列长度。
	 * 
	 * @param size 指定队列长度。
	 */
	public void setHttpQueueSize(int size) {
		this.kernel.setHttpQueueSize(size);
	}

	/**
	 * 获得 HTTP 服务网络连接的队列长度。
	 * 
	 * @return 返回 HTTP 服务网络连接的队列长度。
	 */
	public int getHttpQueueSize() {
		return this.kernel.getHttpQueueSize();
	}

	/**
	 * 设置 HTTP 会话超时时间（毫秒）。
	 * 
	 * @param timeoutInMillisecond 指定以毫秒计算的会话超时时间。
	 */
	public void setHttpSessionTimeout(long timeoutInMillisecond) {
		this.kernel.setHttpSessionTimeout(timeoutInMillisecond);
	}

	/**
	 * 获得 HTTP 会话超时时间（毫秒）。
	 * 
	 * @return 返回 HTTP 会话超时时间（毫秒）。
	 */
	public long getHttpSessionTimeout() {
		return this.kernel.getHttpSessionTimeout();
	}

	/**
	 * 启动会话服务的守护线程。
	 */
	public void startDaemon() {
		this.kernel.startDaemon();
	}

	/**
	 * 停止会话服务的守护线程。
	 */
	public void stopDaemon() {
		this.kernel.stopDaemon();
	}

	/**
	 * 启动扩展的 HTTP Holder 。
	 */
	public void startExtendHolder() {
		this.kernel.startExtendHolder();
	}

	/**
	 * 生成服务的实时快照。
	 * 
	 * @return 返回当前对话服务的快照。
	 */
	public TalkSnapshoot snapshot() {
		return this.kernel.snapshot();
	}

	/**
	 * 获得当前与服务有连接的终端的 Tag 列表。
	 * 
	 * @return 返回存储终端 Tag 的集合。
	 */
	public Set<String> getEndpointTagList() {
		return this.kernel.getEndpointTagList();
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
		return this.kernel.notice(targetTag, primitive, cellet, sandbox);
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
		return this.kernel.notice(targetTag, dialect, cellet, sandbox);
	}

	/**
	 * 将指定标签的终端踢出。
	 * 
	 * @param targetTag 指定目标的内核标签。
	 * @param cellet 指定源 Cellet 。
	 * @param sandbox 指定 Cellet 对应的沙盒。
	 * @return 踢出操作被正确处理返回 <code>true</code> 。
	 */
	public boolean kick(String targetTag, Cellet cellet, CelletSandbox sandbox) {
		return this.kernel.kick(targetTag, cellet, sandbox);
	}

	/**
	 * 查询指定终端标签对应的连接会话数量。
	 * 
	 * @param tag 指定待查询的标签。
	 * @return 返回指定标签连接数量。
	 */
	public int numSessions(String tag) {
		return this.kernel.numSessions(tag);
	}

	/**
	 * 查找指定终端对应的 Endpoint 实例。
	 * 
	 * @param remoteTag 指定待查找终端的内核标签。
	 * @return 如果未找到指定 Endpoint 返回 <code>null</code> 值。
	 */
	public Endpoint findEndpoint(String remoteTag) {
		return this.kernel.findEndpoint(remoteTag);
	}

	/**
	 * 调用指定标识的 Cellet 服务。
	 * 
	 * @param identifiers 指定需调用的 Cellet 的标识清单。
	 * @param address 指定服务器连接地址和端口。
	 * @return 如果调用请求被成功发送给服务器返回 <code>true</code>，否则返回 <code>false</code> 。
	 */
	public boolean call(String[] identifiers, InetSocketAddress address) {
		return this.kernel.call(identifiers, address);
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
		return this.kernel.call(identifiers, address, capacity);
	}

	/**
	 * 调用指定标识的 Cellet 服务。
	 * 
	 * @param identifiers 指定需调用的 Cellet 的标识清单。
	 * @param address 指定服务器连接地址和端口。
	 * @return 如果调用请求被成功发送给服务器返回 <code>true</code>，否则返回 <code>false</code> 。
	 */
	public boolean call(List<String> identifiers, InetSocketAddress address) {
		return this.kernel.call(identifiers, address);
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
		return this.kernel.call(identifiers, address, http);
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
		return this.kernel.call(identifiers, address, capacity);
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
	public boolean call(List<String> identifiers, InetSocketAddress address, TalkCapacity capacity, boolean http) {
		return this.kernel.call(identifiers, address, capacity, http);
	}

	/**
	 * 停止指定标识的 Cellet 服务。
	 * 
	 * @param identifiers 指定需要停止的 Cellet 的标识清单。
	 */
	public void hangUp(String[] identifiers) {
		this.kernel.hangUp(identifiers);
	}

	/**
	 * 停止指定标识的 Cellet 服务。
	 * 
	 * @param identifiers 指定需要停止的 Cellet 的标识清单。
	 */
	public void hangUp(List<String> identifiers) {
		this.kernel.hangUp(identifiers);
	}

	/**
	 * 向指定的 Cellet 发送原语。
	 * 
	 * @param identifier 指定目标 Cellet 的标识。
	 * @param primitive 指定原语。
	 * @return 如果数据被成功送入发送队列返回 <code>true</code> 。
	 */
	public boolean talk(String identifier, Primitive primitive) {
		return this.kernel.talk(identifier, primitive);
	}

	/**
	 * 向指定的 Cellet 发送原语。
	 * 
	 * @param identifier 指定目标 Cellet 的标识。
	 * @param dialect 指定方言。
	 * @return 如果数据被成功送入发送队列返回 <code>true</code> 。
	 */
	public boolean talk(String identifier, Dialect dialect) {
		return this.kernel.talk(identifier, dialect);
	}

	/**
	 * 查询是否已经请求调用了指定的 Cellet 服务。
	 * 
	 * @param identifier 指定 Cellet 的标识。
	 * @return 如果已经调用返回 <code>true</code> 。
	 */
	public boolean isCalled(String identifier) {
		return this.kernel.isCalled(identifier);
	}

	/**
	 * 添加对话监听器。仅用于客户端模式下。
	 * 
	 * @param listener 指定监听器实例。
	 */
	public void addListener(TalkListener listener) {
		this.kernel.addListener(listener);
	}

	/**
	 * 移除对话监听器。仅用于客户端模式下。
	 * 
	 * @param listener 指定监听器实例。
	 */
	public void removeListener(TalkListener listener) {
		this.kernel.removeListener(listener);
	}

	/**
	 * 添加用于 HTTP 服务的服务封装。
	 * 
	 * @param holder 指定 HTTP 封装。
	 */
	public void addExtendHolder(CapsuleHolder holder) {
		this.kernel.addExtendHolder(holder);
	}

	/**
	 * 移除用于 HTTP 服务的服务封装。
	 * 
	 * @param holder 指定 HTTP 封装。
	 */
	public void removeExtendHolder(CapsuleHolder holder) {
		this.kernel.removeExtendHolder(holder);
	}

}
