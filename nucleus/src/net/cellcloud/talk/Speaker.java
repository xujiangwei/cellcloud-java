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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import net.cellcloud.common.Cryptology;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.NonblockingConnector;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.core.Nucleus;
import net.cellcloud.util.Utils;

/**
 * 对话者。
 * 
 * @author Jiangwei Xu
 *
 */
public class Speaker implements Speakable {

	private byte[] nucleusTag;

	private InetSocketAddress address;
	private SpeakerDelegate delegate;
	private NonblockingConnector connector;
	private int block;

	private List<String> identifierList;

	protected TalkCapacity capacity;

	protected String remoteTag;

	private boolean authenticated = false;
	private volatile int state = SpeakerState.HANGUP;

	// 是否需要重新连接
	protected boolean lost = false;
	protected long retryTimestamp = 0;
	protected int retryCounts = 0;
	protected boolean retryEnd = false;

	/** 构造函数。
	 */
	public Speaker(InetSocketAddress address, SpeakerDelegate delegate, int block) {
		this.nucleusTag = Nucleus.getInstance().getTagAsString().getBytes();
		this.address = address;
		this.delegate = delegate;
		this.block = block;
		this.identifierList = new ArrayList<String>(2);
	}

	/** 构造函数。
	 */
	public Speaker(InetSocketAddress address, SpeakerDelegate delegate, int block, TalkCapacity capacity) {
		this.nucleusTag = Nucleus.getInstance().getTagAsString().getBytes();
		this.address = address;
		this.delegate = delegate;
		this.block = block;
		this.capacity = capacity;
		this.identifierList = new ArrayList<String>(2);
	}

	/** 返回 Cellet Identifier 列表。
	 */
	@Override
	public List<String> getIdentifiers() {
		return this.identifierList;
	}

	@Override
	public String getRemoteTag() {
		return this.remoteTag;
	}

	/** 返回连接地址。
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/** 向指定地址发起请求 Cellet 服务。
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

		// 进行连接
		boolean ret = this.connector.connect(this.address);
		if (ret) {
			// 开始进行调用
			this.state = SpeakerState.CALLING;
			this.lost = false;
			this.retryTimestamp = 0;
		}

		return ret;
	}

	/** 挂起服务。
	 */
	@Override
	public void suspend(long duration) {
		if (this.state == SpeakerState.CALLED) {
			// 包格式：内核标签|有效时长

			Packet packet = new Packet(TalkDefinition.TPT_SUSPEND, 5, 1, 0);
			packet.appendSubsegment(this.nucleusTag);
			packet.appendSubsegment(Utils.string2Bytes(Long.toString(duration)));

			byte[] data = Packet.pack(packet);
			if (null != data) {
				// 发送数据
				Message message = new Message(data);
				this.connector.write(message);

				// 更新状态
				this.state = SpeakerState.SUSPENDED;
			}
		}
	}

	/** 恢复服务。
	 */
	@Override
	public void resume(long startTime) {
		if (this.state == SpeakerState.SUSPENDED
			|| this.state == SpeakerState.CALLED) {
			// 包格式：内核标签|需要恢复的原语起始时间戳

			Packet packet = new Packet(TalkDefinition.TPT_RESUME, 6, 1, 0);
			packet.appendSubsegment(this.nucleusTag);
			packet.appendSubsegment(Utils.string2Bytes(Long.toString(startTime)));

			byte[] data = Packet.pack(packet);
			if (null != data) {
				// 发送数据
				Message message = new Message(data);
				this.connector.write(message);

				// 恢复状态
				this.state = SpeakerState.CALLED;
			}
		}
	}

	/** 挂断与 Cellet 的服务。
	 */
	@Override
	public synchronized void hangUp() {
		if (null != this.connector) {
			this.connector.disconnect();
			this.connector = null;
		}

		this.lost = false;
		this.authenticated = false;
		this.state = SpeakerState.HANGUP;
		this.identifierList.clear();
	}

	/** 向 Cellet 发送原语数据。
	 */
	@Override
	public synchronized boolean speak(String identifier, Primitive primitive) {
		if (null == this.connector
			|| !this.connector.isConnected()
			|| this.state != SpeakerState.CALLED) {
			return false;
		}

		// 序列化原语
		ByteArrayOutputStream stream = primitive.write();

		// 封装数据包
		Packet packet = new Packet(TalkDefinition.TPT_DIALOGUE, 99, 1, 0);
		packet.appendSubsegment(stream.toByteArray());
		packet.appendSubsegment(this.nucleusTag);
		packet.appendSubsegment(Utils.string2Bytes(identifier));

		// 发送数据
		byte[] data = Packet.pack(packet);
		Message message = new Message(data);
		this.connector.write(message);

		return true;
	}

	/** 是否已经与 Cellet 建立服务。
	 */
	@Override
	public boolean isCalled() {
		return this.state == SpeakerState.CALLED;
	}

	/** Cellet 服务器是否已经被挂起。
	 */
	@Override
	public boolean isSuspended() {
		return this.state == SpeakerState.SUSPENDED;
	}

	/**
	 * 重置状态数据。
	 */
	protected void reset() {
		this.retryTimestamp = 0;
		this.retryCounts = 0;
		this.retryEnd = false;
	}

	/** 记录服务端 Tag */
	protected void recordTag(String tag) {
		this.remoteTag = tag;
		// 标记为已验证
		this.authenticated = true;
	}

	/** 发送心跳。 */
	protected void heartbeat() {
		if (this.authenticated && !this.lost) {
			Packet packet = new Packet(TalkDefinition.TPT_HEARTBEAT, 9, 1, 0);
			byte[] data = Packet.pack(packet);
			Message message = new Message(data);
			this.connector.write(message);
		}
	}

	protected void notifySessionClosed() {
		// 判断是否要通知被挂起
		if (null != this.capacity && SpeakerState.CALLED == this.state) {
			if (this.capacity.autoSuspend) {
				this.state = SpeakerState.SUSPENDED;
				this.fireSuspended(System.currentTimeMillis(), SuspendMode.PASSIVE);

				// 标记为丢失
				this.lost = true;
			}
		}

		// 判断是否为异常网络中断
		if (SpeakerState.CALLING == this.state) {
			TalkServiceFailure failure = new TalkServiceFailure(TalkFailureCode.CALL_FAILED
					, this.getClass());
			failure.setSourceDescription("No network device");
			failure.setSourceCelletIdentifiers(this.identifierList);
			this.fireFailed(failure);

			// 标记为丢失
			this.lost = true;
		}
		else if (SpeakerState.CALLED == this.state) {
			TalkServiceFailure failure = new TalkServiceFailure(TalkFailureCode.TALK_LOST
					, this.getClass());
			failure.setSourceDescription("Network fault, connection closed");
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

	protected void fireDialogue(String celletIdentifier, Primitive primitive) {
		this.delegate.onDialogue(this, celletIdentifier, primitive);
	}

	private void fireContacted(String celletIdentifier) {
		this.delegate.onContacted(this, celletIdentifier);
	}

	private void fireQuitted(String celletIdentifier) {
		this.delegate.onQuitted(this, celletIdentifier);
	}

	private void fireSuspended(long timestamp, int mode) {
		this.delegate.onSuspended(this, timestamp, mode);
	}

	protected void fireResumed(long timestamp, Primitive primitive) {
		this.delegate.onResumed(this, timestamp, primitive);
	}

	protected void fireFailed(TalkServiceFailure failure) {
		if (failure.getCode() == TalkFailureCode.CALL_FAILED) {
			this.state = SpeakerState.HANGUP;
		}

		this.delegate.onFailed(this, failure);
	}

	protected void fireRetryEnd() {
		TalkServiceFailure failure = new TalkServiceFailure(TalkFailureCode.RETRY_END, this.getClass());
		failure.setSourceCelletIdentifiers(this.identifierList);
		this.fireFailed(failure);
	}

	protected void requestCheck(Packet packet, Session session) {
		// 包格式：密文|密钥

		byte[] ciphertext = packet.getSubsegment(0);
		byte[] key = packet.getSubsegment(1);

		// 解密
		byte[] plaintext = Cryptology.getInstance().simpleDecrypt(ciphertext, key);

		// 发送响应数据
		Packet response = new Packet(TalkDefinition.TPT_CHECK, 2, 1, 0);
		response.appendSubsegment(plaintext);
		response.appendSubsegment(this.nucleusTag);
		// 数据打包
		byte[] data = Packet.pack(response);
		Message message = new Message(data);
		session.write(message);
	}

	protected void requestCellets(Session session) {
		// 包格式：Cellet标识串|标签

		for (String celletIdentifier : this.identifierList) {
			Packet packet = new Packet(TalkDefinition.TPT_REQUEST, 3, 1, 0);
			packet.appendSubsegment(celletIdentifier.getBytes());
			packet.appendSubsegment(this.nucleusTag);

			byte[] data = Packet.pack(packet);
			Message message = new Message(data);
			session.write(message);

			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	protected void doConsult(Packet packet, Session session) {
		// 包格式：源标签(即自己的内核标签)|能力描述序列化串

		TalkCapacity newCapacity = TalkCapacity.deserialize(packet.getSubsegment(1));
		if (null == newCapacity) {
			return;
		}

		// 进行对比
		if (null != this.capacity) {
			if (newCapacity.autoSuspend != this.capacity.autoSuspend
				|| newCapacity.suspendDuration != this.capacity.suspendDuration) {
				StringBuilder buf = new StringBuilder();
				buf.append("Talk capacity has changed from '");
				buf.append(this.remoteTag);
				buf.append("' : AutoSuspend=");
				buf.append(newCapacity.autoSuspend);
				buf.append(" SuspendDuration=");
				buf.append(newCapacity.suspendDuration);
				Logger.w(Speaker.class, buf.toString());
				buf = null;
			}
		}

		// 设置新值
		this.capacity = newCapacity;

		if (Logger.isDebugLevel() && null != this.capacity) {
			StringBuilder buf = new StringBuilder();
			buf.append("Update talk capacity from '");
			buf.append(this.remoteTag);
			buf.append("' : AutoSuspend=");
			buf.append(this.capacity.autoSuspend);
			buf.append(" SuspendDuration=");
			buf.append(this.capacity.suspendDuration);

			Logger.d(Speaker.class, buf.toString());

			buf = null;
		}
	}

	protected void doReply(Packet packet, Session session) {
		// 包格式：
		// 成功：请求方标签|成功码|Cellet识别串|Cellet版本
		// 失败：请求方标签|失败码

		byte[] code = packet.getSubsegment(1);
		if (code[0] == TalkDefinition.SC_SUCCESS[0]
			&& code[1] == TalkDefinition.SC_SUCCESS[1]
			&& code[2] == TalkDefinition.SC_SUCCESS[2]
			&& code[3] == TalkDefinition.SC_SUCCESS[3]) {
			// 变更状态
			this.state = SpeakerState.CALLED;

			String celletIdentifier = Utils.bytes2String(packet.getSubsegment(2));

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
			TalkServiceFailure failure = new TalkServiceFailure(TalkFailureCode.NOTFOUND_CELLET
					, Speaker.class);
			failure.setSourceCelletIdentifiers(this.identifierList);
			this.fireFailed(failure);

			this.connector.disconnect();
		}

		// 如果调用成功，则开始协商能力
		if (SpeakerState.CALLED == this.state && null != this.capacity) {
			this.consult(this.capacity);
		}
	}

	protected void doDialogue(Packet packet, Session session) {
		// 包格式：序列化的原语|Cellet

		byte[] pridata = packet.getSubsegment(0);
		ByteArrayInputStream stream = new ByteArrayInputStream(pridata);
		String celletIdentifier = Utils.bytes2String(packet.getSubsegment(1));

		// 反序列化原语
		Primitive primitive = new Primitive(this.remoteTag);
		primitive.setCelletIdentifier(celletIdentifier);
		primitive.read(stream);

		this.fireDialogue(celletIdentifier, primitive);
	}

	protected void doSuspend(Packet packet, Session session) {
		// 包格式：请求方标签|成功码|时间戳

		byte[] code = packet.getSubsegment(1);
		if (TalkDefinition.SC_SUCCESS[0] == code[0] && TalkDefinition.SC_SUCCESS[1] == code[1]
			&& TalkDefinition.SC_SUCCESS[2] == code[2] && TalkDefinition.SC_SUCCESS[3] == code[3]) {
			// 更新状态
			this.state = SpeakerState.SUSPENDED;

			long timestamp = Long.parseLong(Utils.bytes2String(packet.getSubsegment(2)));
			this.fireSuspended(timestamp, SuspendMode.INITATIVE);
		}
		else {
			this.state = SpeakerState.CALLED;
		}
	}

	protected void doResume(Packet packet, Session session) {
		// 包格式：目的标签|时间戳|原语序列|Cellet

		long timestamp = Long.parseLong(Utils.bytes2String(packet.getSubsegment(1)));
		byte[] pridata = packet.getSubsegment(2);
		ByteArrayInputStream stream = new ByteArrayInputStream(pridata);
		String celletIdentifier = Utils.bytes2String(packet.getSubsegment(3));

		// 反序列化原语
		Primitive primitive = new Primitive(this.remoteTag);
		primitive.setCelletIdentifier(celletIdentifier);
		primitive.read(stream);

		this.fireResumed(timestamp, primitive);
	}

	/** 向 Cellet 协商能力
	 */
	private void consult(TalkCapacity capacity) {
		// 包格式：源标签|能力描述序列化数据

		Packet packet = new Packet(TalkDefinition.TPT_CONSULT, 4, 1, 0);
		packet.appendSubsegment(Utils.string2Bytes(Nucleus.getInstance().getTagAsString()));
		packet.appendSubsegment(TalkCapacity.serialize(capacity));

		byte[] data = Packet.pack(packet);
		if (null != data) {
			Message message = new Message(data);
			this.connector.write(message);
		}
	}
}
