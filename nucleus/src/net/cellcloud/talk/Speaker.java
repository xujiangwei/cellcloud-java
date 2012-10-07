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
import net.cellcloud.core.Nucleus;
import net.cellcloud.talk.stuff.Primitive;

/** 对话者描述类。
 * 
 * @author Jiangwei Xu
 */
public class Speaker {

	private byte[] nucleusTag;

	private String celletIdentifier;
	private NonblockingConnector connector;

	private String remoteTag;

	private boolean called = false;
	private boolean authenticated = false;

	protected long timestamp = 0;

	/** 构造函数。
	 */
	public Speaker(String identifier) {
		this.nucleusTag = Nucleus.getInstance().getTagAsString().getBytes();
		this.celletIdentifier = identifier;
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

		// 进行连接。
		this.connector.connect(address);
	}

	/** 中断与 Cellet 的服务。
	*/
	public void hangUp() {
		if (null != this.connector) {
			if (this.connector.isConnected()) {
				this.connector.disconnect();
			}
		}
	}

	/** 向 Cellet 发送原语数据。
	 */
	public synchronized void speak(Primitive primitive) {
		// 序列化原语
		ByteArrayOutputStream stream = primitive.write();

		// 封装数据包
		Packet packet = new Packet(TalkPacketDefine.TPT_DIALOGUE, 99);
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
		return this.called;
	}

	/**
	 * @private
	 */
	protected void setCalled(boolean called) {
		this.called = called;
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
			Packet packet = new Packet(TalkPacketDefine.TPT_HEARTBEAT, 99);
			byte[] data = Packet.pack(packet);
			Message message = new Message(data);
			this.connector.write(message);
		}
	}

	protected void notifySessionClosed() {
		this.authenticated = false;
		this.called = false;

		// 通知退出
		fireQuitted();

		// 标记为丢失连接的 Speaker
		TalkService.getInstance().markLostSpeaker(this);
	}

	protected void fireDialogue(Primitive primitive) {
		TalkService.getInstance().fireListenerDialogue(this.remoteTag , primitive);
	}

	protected void fireContacted() {
		TalkService.getInstance().fireListenerContacted(this.remoteTag);
	}

	protected void fireQuitted() {
		TalkService.getInstance().fireListenerQuitted(this.remoteTag);
	}

	protected void requestCheck(Packet packet, Session session) {
		// 包格式：密文|密钥

		byte[] ciphertext = packet.getSubsegment(0);
		byte[] key = packet.getSubsegment(1);

		// 解密
		byte[] plaintext = Cryptology.getInstance().simpleDecrypt(ciphertext, key);

		// 发送响应数据
		Packet response = new Packet(TalkPacketDefine.TPT_CHECK, 1);
		response.appendSubsegment(plaintext);
		// 数据打包
		byte[] data = Packet.pack(response);
		Message message = new Message(data);
		session.write(message);
	}

	protected void requestCellet(Session session) {
		// 包格式：Cellet标识串|标签

		Packet packet = new Packet(TalkPacketDefine.TPT_REQUEST, 2);
		packet.appendSubsegment(this.celletIdentifier.getBytes());
		packet.appendSubsegment(this.nucleusTag);

		byte[] data = Packet.pack(packet);
		Message message = new Message(data);
		session.write(message);
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
}
