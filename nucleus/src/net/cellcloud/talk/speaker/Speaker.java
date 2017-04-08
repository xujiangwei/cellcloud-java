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

package net.cellcloud.talk.speaker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import net.cellcloud.Version;
import net.cellcloud.common.Cryptology;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.NonblockingConnector;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.core.Nucleus;
import net.cellcloud.talk.CompatibilityHelper;
import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.Speakable;
import net.cellcloud.talk.SpeakerState;
import net.cellcloud.talk.TalkCapacity;
import net.cellcloud.talk.TalkDefinition;
import net.cellcloud.talk.TalkFailureCode;
import net.cellcloud.talk.TalkServiceFailure;
import net.cellcloud.talk.stuff.StuffVersion;
import net.cellcloud.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 原语对话者。
 * 
 * @author Ambrose Xu
 *
 */
public class Speaker implements Speakable {

	/** 内核标签。 */
	private byte[] nucleusTag;

	/** 访问地址。 */
	private InetSocketAddress address;
	/** 对话者事件委派。 */
	private SpeakerDelegate delegate;
	/** 对话代理监听器。 */
	private SpeakerProxyListener proxyListener;
	/** 用于建立连接的非阻塞连接器。 */
	private NonblockingConnector connector;
	/** 数据缓存区大小。 */
	private int block;

	/** 此对话者请求的 Cellet 标识清单。 */
	private Vector<String> identifierList;

	/** 对话者协商的能力描述。 */
	public TalkCapacity capacity = null;

	/** 从服务器获得密钥。 */
	private byte[] secretKey = null;

	/** 服务器端的内核标签。 */
	protected String remoteTag  = null;

	/** 是否已经验证成功，成功与服务器握手。 */
	private boolean authenticated = false;
	/** 状态。 */
	private volatile int state = SpeakerState.HANGUP;

	/** 是否需要重新连接。 */
	public boolean lost = false;
	/** 上一次重连的时间戳。 */
	public long retryTimestamp = 0;
	/** 重连次数。 */
	public int retryCount = 0;
	/** 是否已经达到最大重连次数，重连结束。 */
	public boolean retryEnd = false;

	/** 协议握手超时控制定时器。 */
	private Timer contactedTimer = null;

	/** 最近一次心跳时间戳。 */
	protected long heartbeatTime = 0;

	/**
	 * 构造函数。
	 * 
	 * @param address 指定访问地址。
	 * @param delegate 指定事件委派。
	 * @param block 指定缓存区大小。
	 */
	public Speaker(InetSocketAddress address, SpeakerDelegate delegate, int block) {
		this.nucleusTag = Nucleus.getInstance().getTagAsString().getBytes();
		this.address = address;
		this.delegate = delegate;
		this.block = block;
		this.identifierList = new Vector<String>(2);
	}

	/**
	 * 构造函数。
	 * 
	 * @param address 指定访问地址。
	 * @param delegate 指定事件委派。
	 * @param block 指定缓存区大小。
	 * @param capacity 指定协商能力。
	 */
	public Speaker(InetSocketAddress address, SpeakerDelegate delegate, int block, TalkCapacity capacity) {
		this.nucleusTag = Nucleus.getInstance().getTagAsString().getBytes();
		this.address = address;
		this.delegate = delegate;
		this.block = block;
		this.capacity = capacity;
		this.identifierList = new Vector<String>(2);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getIdentifiers() {
		return this.identifierList;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRemoteTag() {
		return this.remoteTag;
	}

	/**
	 * 获得连接地址。
	 * 
	 * @return 返回连接地址。
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean call(List<String> identifiers) {
		if (SpeakerState.CALLING == this.state) {
			// 正在 Call 返回 false
			return false;
		}

		if (null != identifiers) {
			for (String identifier : identifiers) {
				if (this.identifierList.contains(identifier)) {
					continue;
				}

				this.identifierList.add(identifier.toString());
			}
		}

		if (this.identifierList.isEmpty()) {
			Logger.w(Speaker.class, "Can not find any cellets to call in param 'identifiers'.");
			return false;
		}

		if (null == this.connector) {
			this.connector = new NonblockingConnector();
			this.connector.setBlockSize(this.block);

			byte[] headMark = {0x20, 0x10, 0x11, 0x10};
			byte[] tailMark = {0x19, 0x78, 0x10, 0x04};
			this.connector.defineDataMark(headMark, tailMark);

			this.connector.setHandler(new SpeakerConnectorHandler(this));
		}
		else {
			if (this.connector.isConnected()) {
				this.connector.disconnect();
			}
		}

		// 设置状态
		this.state = SpeakerState.HANGUP;
		this.authenticated = false;

		this.retryTimestamp = 0;

		// 进行连接
		boolean ret = this.connector.connect(this.address);
		if (ret) {
			// 开始进行调用
			this.state = SpeakerState.CALLING;
			this.lost = false;
		}

		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void hangUp() {
		this.state = SpeakerState.HANGUP;

		if (null != this.contactedTimer) {
			this.contactedTimer.cancel();
			this.contactedTimer.purge();
			this.contactedTimer = null;
		}

		if (null != this.connector) {
			Session session = this.connector.getSession();
			if (null != session) {
				// 吊销密钥
				session.deactiveSecretKey();
			}

			this.connector.disconnect();
			this.connector = null;
		}

		this.lost = false;
		this.authenticated = false;
		this.identifierList.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized boolean speak(String identifier, Primitive primitive) {
		if (null == this.connector
			|| !this.connector.isConnected()
			|| this.state != SpeakerState.CALLED) {
			return false;
		}

		// 兼容性判断
		StuffVersion version = CompatibilityHelper.match(Version.VERSION_NUMBER);
		if (version != primitive.getVersion()) {
			primitive.setVersion(version);
		}

		// 序列化原语
		ByteArrayOutputStream stream = primitive.write();

		// 封装数据包
		Packet packet = new Packet(TalkDefinition.TPT_DIALOGUE, 99, 2, 0);
		packet.appendSegment(stream.toByteArray());
		packet.appendSegment(this.nucleusTag);
		packet.appendSegment(Utils.string2Bytes(identifier));

		// 发送数据
		byte[] data = Packet.pack(packet);
		Message message = new Message(data);
		this.connector.write(message);

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCalled() {
		return (this.state == SpeakerState.CALLED && this.connector.isConnected());
	}

	/**
	 * 透传指定的数据。
	 * 
	 * @param data 指定需透传的数据。
	 */
	public void pass(byte[] data) {
		Message message = new Message(data);
		this.connector.write(message);
	}

	/**
	 * 重置连接器睡眠间隔。
	 * 
	 * @param sleepInterval 指定新的时间间隔，单位：毫秒。
	 */
	public void resetSleepInterval(long sleepInterval) {
		this.connector.resetSleepInterval(sleepInterval);
	}

	/**
	 * 设置代理监听器。
	 * 
	 * @param listener 指定代理监听器实例。
	 */
	public void setProxyListener(SpeakerProxyListener listener) {
		this.proxyListener = listener;
	}

	/**
	 * 重置状态数据。
	 */
	protected void reset() {
		this.retryTimestamp = 0;
		this.retryCount = 0;
		this.retryEnd = false;
	}

	/**
	 * 记录服务端标签。
	 * 
	 * @param tag 指定服务端标签。
	 */
	protected void recordTag(String tag) {
		this.remoteTag = tag;
		// 标记为已验证
		this.authenticated = true;
	}

	/**
	 * 发送心跳。
	 * 
	 * @return 数据成功写入发送队列返回 <code>true</code> 。
	 */
	public boolean heartbeat() {
		if (this.authenticated && !this.lost && this.connector.isConnected()) {
			Packet packet = new Packet(TalkDefinition.TPT_HEARTBEAT, 9, 2, 0);
			byte[] data = Packet.pack(packet);
			Message message = new Message(data);
			this.connector.write(message);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * 通知会话被关闭，更新内部状态。
	 */
	protected void notifySessionClosed() {
		// 判断是否为异常网络中断
		if (SpeakerState.CALLING == this.state) {
			TalkServiceFailure failure = new TalkServiceFailure(TalkFailureCode.CALL_FAILED
					, this.getClass(), this.address.getHostString(), this.address.getPort());
			failure.setSourceDescription("No network device");
			failure.setSourceCelletIdentifiers(this.identifierList);
			this.fireFailed(failure);

			// 标记为丢失
			this.lost = true;
		}
		else if (SpeakerState.CALLED == this.state) {
			TalkServiceFailure failure = new TalkServiceFailure(TalkFailureCode.TALK_LOST
					, this.getClass(), this.address.getHostString(), this.address.getPort());
			failure.setSourceDescription("Network fault, connection closed");
			failure.setSourceCelletIdentifiers(this.identifierList);
			this.fireFailed(failure);

			// 标记为丢失
			this.lost = true;
		}
		else {
			TalkServiceFailure failure = new TalkServiceFailure(TalkFailureCode.NETWORK_NOT_AVAILABLE
					, this.getClass(), this.address.getHostString(), this.address.getPort());
			failure.setSourceDescription("Session has closed");
			failure.setSourceCelletIdentifiers(this.identifierList);
			this.fireFailed(failure);

			// 标记为丢失
			this.lost = true;
		}

		this.authenticated = false;
		this.state = SpeakerState.HANGUP;

		// 通知退出
		for (String identifier : this.identifierList) {
			this.fireQuitted(identifier);
		}
	}

	/**
	 * 触发 Dialogue 回调。
	 * 
	 * @param celletIdentifier 指定 Cellet 标识。
	 * @param primitive 指定接收到的原语数据。
	 */
	protected void fireDialogue(String celletIdentifier, Primitive primitive) {
		this.delegate.onDialogue(this, celletIdentifier, primitive);
	}

	/**
	 * 触发 Contacted 回调。
	 * 
	 * @param celletIdentifier 指定 Cellet 标识。
	 */
	private synchronized void fireContacted(String celletIdentifier) {
		if (null == this.contactedTimer) {
			this.contactedTimer = new Timer("SpeakerContactedTimer");
		}
		else {
			this.contactedTimer.cancel();
			this.contactedTimer.purge();
			this.contactedTimer = null;
			this.contactedTimer = new Timer("SpeakerContactedTimer");
		}

		this.contactedTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				// 请求成功，激活链路加密
				if (capacity.secure) {
					if (!connector.getSession().isSecure()) {
						boolean ret = connector.getSession().activeSecretKey(secretKey);
						if (ret) {
							Logger.i(Speaker.class, "Active secret key for server: " + address.getHostString() + ":" + address.getPort());
						}
					}
				}
				else {
					connector.getSession().deactiveSecretKey();
				}

				for (String cid : identifierList) {
					delegate.onContacted(Speaker.this, cid);
				}

				if (null != contactedTimer) {
					contactedTimer.cancel();
					contactedTimer.purge();
					contactedTimer = null;
				}
			}
		}, 100L);
	}

	/**
	 * 触发 Quitted 回调。
	 * 
	 * @param celletIdentifier 指定 Cellet 标识。
	 */
	private void fireQuitted(String celletIdentifier) {
		if (null != this.contactedTimer) {
			this.contactedTimer.cancel();
			this.contactedTimer.purge();
			this.contactedTimer = null;
		}

		this.delegate.onQuitted(this, celletIdentifier);

		if (null != this.connector) {
			// 吊销密钥
			Session session = this.connector.getSession();
			if (null != session) {
				session.deactiveSecretKey();
			}
		}
	}

	/**
	 * 触发 Failed 回调。
	 * 
	 * @param failure 指定会话服务错误描述。
	 */
	protected void fireFailed(TalkServiceFailure failure) {
		if (failure.getCode() == TalkFailureCode.NOT_FOUND
			|| failure.getCode() == TalkFailureCode.INCORRECT_DATA
			|| failure.getCode() == TalkFailureCode.RETRY_END) {
			this.delegate.onFailed(this, failure);
		}
		else {
			this.state = SpeakerState.HANGUP;
			this.delegate.onFailed(this, failure);
			this.lost = true;
		}
	}

	/**
	 * 触发重试连接结束。
	 */
	public void fireRetryEnd() {
		TalkServiceFailure failure = new TalkServiceFailure(TalkFailureCode.RETRY_END
				, this.getClass(), this.address.getHostString(), this.address.getPort());
		failure.setSourceCelletIdentifiers(this.identifierList);
		this.fireFailed(failure);
	}

	/**
	 * 应答 Check 校验数据进行握手。
	 * 
	 * @param packet 指定接收到的 INTERROGATE 包。
	 * @param session 指定会话。
	 */
	protected void respondCheck(Packet packet, Session session) {
		// 包格式：密文|密钥

		byte[] ciphertext = packet.getSegment(0);
		byte[] key = packet.getSegment(1);

		// 写密钥
		this.secretKey = new byte[key.length];
		System.arraycopy(key, 0, this.secretKey, 0, key.length);

		// 解密
		byte[] plaintext = Cryptology.getInstance().simpleDecrypt(ciphertext, key);

		// 发送响应数据
		Packet response = new Packet(TalkDefinition.TPT_CHECK, 2, 2, 0);
		response.appendSegment(plaintext);
		response.appendSegment(this.nucleusTag);
		// 数据打包
		byte[] data = Packet.pack(response);
		Message message = new Message(data);
		session.write(message);
	}

	/**
	 * 应答能力协商。
	 */
	protected void respondConsult() {
		// 协商能力
		if (null == this.capacity) {
			this.capacity = new TalkCapacity();
		}

		// 兼容性处理
		StuffVersion version = CompatibilityHelper.match(Version.VERSION_NUMBER);
		if (version == StuffVersion.V2) {
			this.capacity.resetVersion(3);
		}

		// 包格式：源标签|能力描述序列化数据
		Packet packet = new Packet(TalkDefinition.TPT_CONSULT, 4, 2, 0);
		packet.appendSegment(this.nucleusTag);
		packet.appendSegment(TalkCapacity.serialize(this.capacity));

		byte[] data = Packet.pack(packet);
		if (null != data) {
			Message message = new Message(data);
			this.connector.write(message);
		}
	}

	/**
	 * 请求 Cellet 服务。
	 * 
	 * @param session 指定会话。
	 */
	protected void requestCellets(Session session) {
		// 包格式：Cellet标识串|标签

		for (String celletIdentifier : this.identifierList) {
			Packet packet = new Packet(TalkDefinition.TPT_REQUEST, 3, 2, 0);
			packet.appendSegment(celletIdentifier.getBytes());
			packet.appendSegment(this.nucleusTag);

			byte[] data = Packet.pack(packet);
			Message message = new Message(data);
			session.write(message);

			try {
				Thread.sleep(5L);
			} catch (InterruptedException e) {
				// Nothing
			}
		}
	}

	/**
	 * 执行协商操作。
	 * 
	 * @param packet 指定服务器发送过来的协商数据包。
	 * @param session 指定会话。
	 */
	protected void doConsult(Packet packet, Session session) {
		// 包格式：源标签(即自己的内核标签)|能力描述序列化串

		TalkCapacity newCapacity = TalkCapacity.deserialize(packet.getSegment(1));
		if (null == newCapacity) {
			return;
		}

		// 更新能力
		if (null == this.capacity) {
			this.capacity = newCapacity;
		}
		else {
			this.capacity.secure = newCapacity.secure;
			this.capacity.retry = newCapacity.retry;
			this.capacity.retryDelay = newCapacity.retryDelay;
		}

		if (Logger.isDebugLevel() && null != this.capacity) {
			StringBuilder buf = new StringBuilder();
			buf.append("Update talk capacity from '");
			buf.append(this.remoteTag);
			buf.append("' : secure=");
			buf.append(this.capacity.secure);
			buf.append(" retry=");
			buf.append(this.capacity.retry);
			buf.append(" delay=");
			buf.append(this.capacity.retryDelay);

			Logger.d(Speaker.class, buf.toString());

			buf = null;
		}
	}

	/**
	 * 执行来自服务器的请求 Cellet 应答。
	 * 
	 * @param packet 指定来自服务器的请求应答包。
	 * @param session 指定会话。
	 */
	protected void doRequest(Packet packet, Session session) {
		// 包格式：
		// 成功：请求方标签|成功码|Cellet识别串|Cellet版本
		// 失败：请求方标签|失败码

		byte[] code = packet.getSegment(1);
		if (code[0] == TalkDefinition.SC_SUCCESS[0]
			&& code[1] == TalkDefinition.SC_SUCCESS[1]
			&& code[2] == TalkDefinition.SC_SUCCESS[2]
			&& code[3] == TalkDefinition.SC_SUCCESS[3]) {
			// 变更状态
			this.state = SpeakerState.CALLED;

			String celletIdentifier = Utils.bytes2String(packet.getSegment(2));

			StringBuilder buf = new StringBuilder();
			buf.append("Cellet '");
			buf.append(celletIdentifier);
			buf.append("' has called at ");
			buf.append(this.getAddress().getAddress().getHostAddress());
			buf.append(":");
			buf.append(this.getAddress().getPort());
			Logger.i(Speaker.class, buf.toString());
			buf = null;

			// 回调事件
			this.fireContacted(celletIdentifier);
		}
		else {
			// 变更状态
			this.state = SpeakerState.HANGUP;

			// 回调事件
			TalkServiceFailure failure = new TalkServiceFailure(TalkFailureCode.NOT_FOUND, Speaker.class,
					this.address.getHostString(), this.address.getPort());
			failure.setSourceCelletIdentifiers(this.identifierList);
			this.fireFailed(failure);

			this.connector.disconnect();
		}
	}

	/**
	 * 执行来自服务器的对话数据包。
	 * 
	 * @param packet 指定来自服务器的数据包。
	 * @param session 指定会话。
	 */
	protected void doDialogue(Packet packet, Session session) {
		// 包格式：序列化的原语|Cellet

		byte[] pridata = packet.getSegment(0);
		ByteArrayInputStream stream = new ByteArrayInputStream(pridata);
		String celletIdentifier = Utils.bytes2String(packet.getSegment(1));

		// 反序列化原语
		Primitive primitive = new Primitive(this.remoteTag);
		primitive.setCelletIdentifier(celletIdentifier);
		primitive.read(stream);

		if (packet.numSegments() == 3) {
			// 来自代理的对话
			String tag = Utils.bytes2String(packet.getSegment(2));
			if (null != this.proxyListener) {
				this.proxyListener.onProxyDialogue(tag, celletIdentifier, primitive);
			}
			return;
		}

		this.fireDialogue(celletIdentifier, primitive);
	}

	/**
	 * 以快速握手方式应答服务器的握手询问。
	 * 
	 * @param packet 指定来自服务的询问包。
	 * @param session 指定会话。
	 */
	protected void respondQuick(Packet packet, Session session) {
		byte[] ciphertext = packet.getSegment(0);
		byte[] key = packet.getSegment(1);

		// 写密钥
		this.secretKey = new byte[key.length];
		System.arraycopy(key, 0, this.secretKey, 0, key.length);

		// 解密
		byte[] plaintext = Cryptology.getInstance().simpleDecrypt(ciphertext, key);

		// 协商能力
		if (null == this.capacity) {
			this.capacity = new TalkCapacity();
		}

		// 兼容性处理
		StuffVersion version = CompatibilityHelper.match(Version.VERSION_NUMBER);
		if (version == StuffVersion.V2) {
			this.capacity.resetVersion(3);
		}

		// 包格式：明文|源标签|能力描述序列化数据|CelletIdentifiers
		// 发送响应数据
		Packet response = new Packet(TalkDefinition.TPT_QUICK, 2, 2, 0);
		response.appendSegment(plaintext);
		response.appendSegment(this.nucleusTag);
		response.appendSegment(TalkCapacity.serialize(this.capacity));
		for (String celletIdentifier : this.identifierList) {
			response.appendSegment(celletIdentifier.getBytes());
		}

		byte[] data = Packet.pack(response);
		Message message = new Message(data);
		session.write(message);
		message = null;

		response = null;
	}

	/**
	 * 执行来自服务器的快速握手应答。
	 * 
	 * @param packet 指定来自服务器的快速握手回包。
	 * @param session 指定会话。
	 */
	protected void doQuick(Packet packet, Session session) {
		// 包格式：状态码|源标签|能力描述序列化数据|CelletIdentifiers

		byte[] code = packet.getSegment(0);
		if (code[0] == TalkDefinition.SC_SUCCESS[0]
			&& code[1] == TalkDefinition.SC_SUCCESS[1]
			&& code[2] == TalkDefinition.SC_SUCCESS[2]
			&& code[3] == TalkDefinition.SC_SUCCESS[3]) {
			// 记录标签
			byte[] rtag = packet.getSegment(1);
			this.recordTag(Utils.bytes2String(rtag));

			TalkCapacity newCapacity = TalkCapacity.deserialize(packet.getSegment(2));
			// 更新能力
			if (null != newCapacity) {
				if (null == this.capacity) {
					this.capacity = newCapacity;
				}
				else {
					this.capacity.secure = newCapacity.secure;
					this.capacity.retry = newCapacity.retry;
					this.capacity.retryDelay = newCapacity.retryDelay;
				}
			}

			// 变更状态
			this.state = SpeakerState.CALLED;

			for (int i = 3, size = packet.numSegments(); i < size; ++i) {
				String celletIdentifier = Utils.bytes2String(packet.getSegment(i));

				StringBuilder buf = new StringBuilder();
				buf.append("Cellet '");
				buf.append(celletIdentifier);
				buf.append("' has called at ");
				buf.append(this.getAddress().getAddress().getHostAddress());
				buf.append(":");
				buf.append(this.getAddress().getPort());
				Logger.i(Speaker.class, buf.toString());
				buf = null;

				// 回调事件
				this.fireContacted(celletIdentifier);
			}
		}
		else {
			this.state = SpeakerState.HANGUP;

			// 回调事件
			TalkServiceFailure failure = new TalkServiceFailure(TalkFailureCode.NOT_FOUND
					, Speaker.class, this.address.getHostString(), this.address.getPort());
			failure.setSourceCelletIdentifiers(this.identifierList);
			this.fireFailed(failure);
		}
	}

	/**
	 * 执行来自网关的代理回包。
	 * 
	 * @param packet 指定代理数据包。
	 * @param session 指定会话。
	 */
	protected void doProxy(Packet packet, Session session) {
		// 包格式：状态码|数据JSON

		byte[] code = packet.getSegment(0);
		if (code[0] == TalkDefinition.SC_SUCCESS[0]
			&& code[1] == TalkDefinition.SC_SUCCESS[1]
			&& code[2] == TalkDefinition.SC_SUCCESS[2]
			&& code[3] == TalkDefinition.SC_SUCCESS[3]) {

			byte[] data = packet.getSegment(1);

			JSONObject json = null;
			try {
				json = new JSONObject(Utils.bytes2String(data));
			} catch (JSONException e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			}

			// 回调事件
			if (null != this.proxyListener) {
				this.proxyListener.onProxy(this, json);
			}
		}
		else {
			// 回调错误事件
			TalkServiceFailure failure = new TalkServiceFailure(TalkFailureCode.PROXY_FAILED
					, Speaker.class, this.address.getHostString(), this.address.getPort());
			failure.setSourceCelletIdentifiers(this.identifierList);
			this.fireFailed(failure);
		}
	}

}
