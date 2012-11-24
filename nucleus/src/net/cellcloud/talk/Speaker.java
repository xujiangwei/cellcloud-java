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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;

import net.cellcloud.common.Message;
import net.cellcloud.common.NonblockingConnector;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.core.Cryptology;
import net.cellcloud.core.Logger;
import net.cellcloud.core.Nucleus;
import net.cellcloud.talk.stuff.Primitive;
import net.cellcloud.util.Util;

/** 对话者描述类。
 * 
 * @author Jiangwei Xu
 */
public class Speaker {

	private byte[] nucleusTag;

	private String celletIdentifier;
	private NonblockingConnector connector;

	private TalkCapacity capacity;

	private String remoteTag;

	private boolean authenticated = false;
	private int state = SpeakerState.HANGUP;

	protected long timestamp = 0;

	/** 构造函数。
	 */
	public Speaker(String identifier) {
		this.nucleusTag = Nucleus.getInstance().getTagAsString().getBytes();
		this.celletIdentifier = identifier;
		this.capacity = null;
		this.connector = null;
	}

	/** 构造函数。
	 */
	public Speaker(String identifier, TalkCapacity capacity) {
		this.nucleusTag = Nucleus.getInstance().getTagAsString().getBytes();
		this.celletIdentifier = identifier;
		this.capacity = capacity;
		this.connector = null;
	}

	/** 返回 Cellet Identifier 。
	 */
	public String getIdentifier() {
		return this.celletIdentifier;
	}

	/** 返回连接地址。
	 */
	public InetSocketAddress getAddress() {
		if (null == this.connector)
			return null;

		return this.connector.getAddress();
	}

	/** 向指定地址发起请求 Cellet 服务。
	 */
	public void call(InetSocketAddress address) {
		if (null == this.connector) {
			this.connector = new NonblockingConnector();

			byte[] headMark = {0x20, 0x10, 0x11, 0x10};
			byte[] tailMark = {0x19, 0x78, 0x10, 0x04};
			this.connector.defineDataMark(headMark, tailMark);

			this.connector.setHandler(new SpeakerConnectorHandler(this));
		}

		if (this.connector.isConnected()) {
			return;
		}

		// 设置状态
		this.state = SpeakerState.HANGUP;

		// 进行连接
		this.connector.connect(address);

		// 开始进行调用
		this.state = SpeakerState.CALLING;
	}

	/** 中断与 Cellet 的服务。
	*/
	public void hangUp() {
		if (null != this.connector) {
			if (this.connector.isConnected()) {
				this.connector.disconnect();
			}
		}

		this.state = SpeakerState.HANGUP;
	}

	/** 向 Cellet 发送原语数据。
	 */
	public synchronized void speak(Primitive primitive) {
		// 序列化原语
		ByteArrayOutputStream stream = primitive.write();

		// 封装数据包
		Packet packet = new Packet(TalkDefinition.TPT_DIALOGUE, 99);
		packet.appendSubsegment(stream.toByteArray());
		packet.appendSubsegment(this.nucleusTag);

		// 发送数据
		byte[] data = Packet.pack(packet);
		Message message = new Message(data);
		this.connector.write(message);
	}

	/** 是否已接受请求。
	 */
	public boolean isCalled() {
		return this.state == SpeakerState.CALLED;
	}

	/** 记录服务端 Tag */
	protected void recordTag(String tag) {
		this.remoteTag = tag;
		// 标记为已验证
		this.authenticated = true;
	}

	/** 发送心跳。 */
	protected void heartbeat() {
		if (this.authenticated) {
			Packet packet = new Packet(TalkDefinition.TPT_HEARTBEAT, 99);
			byte[] data = Packet.pack(packet);
			Message message = new Message(data);
			this.connector.write(message);
		}
	}

	protected void notifySessionClosed() {
		this.authenticated = false;
		this.state = SpeakerState.HANGUP;

		// 通知退出
		fireQuitted();
	}

	protected void fireDialogue(Primitive primitive) {
		TalkService.getInstance().fireListenerDialogue(this.remoteTag , primitive);
	}

	private void fireContacted() {
		TalkService.getInstance().fireListenerContacted(this.remoteTag);
	}

	protected void fireQuitted() {
		TalkService.getInstance().fireListenerQuitted(this.remoteTag);
	}

	protected void fireFailed(TalkServiceFailure failure) {
		TalkService.getInstance().fireListenerFailed(failure);
	}

	protected void requestCheck(Packet packet, Session session) {
		// 包格式：密文|密钥

		byte[] ciphertext = packet.getSubsegment(0);
		byte[] key = packet.getSubsegment(1);

		// 解密
		byte[] plaintext = Cryptology.getInstance().simpleDecrypt(ciphertext, key);

		// 发送响应数据
		Packet response = new Packet(TalkDefinition.TPT_CHECK, 1);
		response.appendSubsegment(plaintext);
		// 数据打包
		byte[] data = Packet.pack(response);
		Message message = new Message(data);
		session.write(message);
	}

	protected void requestCellet(Session session) {
		// 包格式：Cellet标识串|标签

		Packet packet = new Packet(TalkDefinition.TPT_REQUEST, 2);
		packet.appendSubsegment(this.celletIdentifier.getBytes());
		packet.appendSubsegment(this.nucleusTag);

		byte[] data = Packet.pack(packet);
		Message message = new Message(data);
		session.write(message);
	}

	protected void processConsult(Packet packet, Session session) {
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
				buf.append("Talk capacity has changed from ");
				buf.append(this.celletIdentifier);
				buf.append(" : AutoSuspend=");
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
			buf.append("Update talk capacity from ");
			buf.append(this.celletIdentifier);
			buf.append(" : AutoSuspend=");
			buf.append(this.capacity.autoSuspend);
			buf.append(" SuspendDuration=");
			buf.append(this.capacity.suspendDuration);

			Logger.d(Speaker.class, buf.toString());

			buf = null;
		}
	}

	protected void processRequestReply(Packet packet, Session session) {
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

			StringBuilder buf = new StringBuilder();
			buf.append("Cellet '");
			buf.append(this.celletIdentifier);
			buf.append("' has called at ");
			buf.append(this.getAddress().getAddress().getHostAddress());
			buf.append(":");
			buf.append(this.getAddress().getPort());
			Logger.d(Speaker.class, buf.toString());
			buf = null;

			// 回调事件
			this.fireContacted();
		}
		else {
			// 变更状态
			this.state = SpeakerState.HANGUP;

			// 回调事件
			TalkServiceFailure failure = new TalkServiceFailure(TalkFailureCode.NOTFOUND_CELLET
					, Speaker.class);
			failure.setSourceCelletIdentifier(this.celletIdentifier);
			this.fireFailed(failure);

			this.connector.disconnect();
		}

		// 如果调用成功，则开始协商能力
		if (SpeakerState.CALLED == this.state && null != this.capacity) {
			this.consult(this.capacity);
		}
	}

	protected void processDialogue(Packet packet, Session session) {
		// 包格式：序列化的原语

		byte[] pridata = packet.getBody();
		ByteArrayInputStream stream = new ByteArrayInputStream(pridata);

		// 反序列化原语
		Primitive primitive = new Primitive(this.remoteTag);
		primitive.read(stream);

		this.fireDialogue(primitive);
	}

	/** 向 Cellet 协商能力
	 */
	private void consult(TalkCapacity capacity) {
		// 包格式：源标签|能力描述序列化数据

		Packet packet = new Packet(TalkDefinition.TPT_CONSULT, 5, 1, 0);
		packet.appendSubsegment(Util.string2Bytes(Nucleus.getInstance().getTagAsString()));
		packet.appendSubsegment(TalkCapacity.serialize(capacity));

		byte[] data = Packet.pack(packet);
		if (null != data) {
			Message message = new Message(data);
			this.connector.write(message);
		}
	}
}
