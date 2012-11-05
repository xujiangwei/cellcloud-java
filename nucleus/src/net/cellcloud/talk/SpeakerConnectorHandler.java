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

import net.cellcloud.common.Message;
import net.cellcloud.common.MessageHandler;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.core.Logger;

/** Speaker 连接处理器。
 * 
 * @author Jiangwei Xu
 */
public final class SpeakerConnectorHandler extends MessageHandler {

	private Speaker speaker;

	/** 构造函数。
	 */
	public SpeakerConnectorHandler(Speaker speaker) {
		this.speaker = speaker;
	}

	/**
	 * @copydoc MessageHandler::sessionCreated(Session)
	 */
	@Override
	public void sessionCreated(Session session) {
		Logger.d(SpeakerConnectorHandler.class, "sessionCreated");
	}

	/**
	 * @copydoc MessageHandler::sessionDestroyed(Session)
	 */
	@Override
	public void sessionDestroyed(Session session) {
		Logger.d(SpeakerConnectorHandler.class, "sessionDestroyed");
	}

	/**
	 * @copydoc MessageHandler::sessionOpened(Session)
	 */
	@Override
	public void sessionOpened(Session session) {
		Logger.d(SpeakerConnectorHandler.class, "sessionOpened");
	}

	/**
	 * @copydoc MessageHandler::sessionClosed(Session)
	 */
	@Override
	public void sessionClosed(Session session) {
		Logger.d(SpeakerConnectorHandler.class, "sessionClosed");
		this.speaker.notifySessionClosed();
	}

	/**
	 * @copydoc MessageHandler::messageReceived(Session, Message)
	 */
	@Override
	public void messageReceived(Session session, Message message) {
//		System.out.println("recv : " + new String(message.get()));
		// 解包
		Packet packet = Packet.unpack(message.get());
		// 解释数据包
		interpret(session, packet);
	}

	/**
	 * @copydoc MessageHandler::messageSent(Session, Message)
	 */
	@Override
	public void messageSent(Session session, Message message) {
		// Nothing
	}

	/**
	 * @copydoc MessageHandler::errorOccurred(int, Session)
	 */
	@Override
	public void errorOccurred(int errorCode, Session session) {
		Logger.d(SpeakerConnectorHandler.class, "errorOccurred : " + errorCode);
		if (errorCode == MessageHandler.EC_CONNECT_FAILED) {
			TalkService.getInstance().markLostSpeaker(this.speaker);
		}
	}

	private void interpret(Session session, Packet packet) {
		// 处理包

		byte[] tag = packet.getTag();

		if (TalkPacketDefine.TPT_DIALOGUE[2] == tag[2]
			&& TalkPacketDefine.TPT_DIALOGUE[3] == tag[3]) {
			this.speaker.processDialogue(packet, session);
		}
		else if (TalkPacketDefine.TPT_REQUEST[2] == tag[2]
			&& TalkPacketDefine.TPT_REQUEST[3] == tag[3]) {
			this.speaker.setCalled(true);

			StringBuilder buf = new StringBuilder();
			buf.append("Cellet '");
			buf.append(this.speaker.getIdentifier());
			buf.append("' has called at ");
			buf.append(this.speaker.getAddress().getAddress().getHostAddress());
			buf.append(":");
			buf.append(this.speaker.getAddress().getPort());
			Logger.d(SpeakerConnectorHandler.class, buf.toString());
			buf = null;
		}
		else if (TalkPacketDefine.TPT_CHECK[2] == tag[2]
			&& TalkPacketDefine.TPT_CHECK[3] == tag[3]) {

			// 记录标签
			byte[] ntag = packet.getSubsegment(1);
			this.speaker.recordTag(new String(ntag));

			// 发送事件
			this.speaker.fireContacted();

			// 请求 Cellet
			this.speaker.requestCellet(session);
		}
		else if (TalkPacketDefine.TPT_INTERROGATE[2] == tag[2]
			&& TalkPacketDefine.TPT_INTERROGATE[3] == tag[3]) {
			this.speaker.requestCheck(packet, session);
		}
	}
}
