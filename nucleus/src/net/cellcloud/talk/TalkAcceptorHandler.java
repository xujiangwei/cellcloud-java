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

/** Talk 服务句柄。
 * 
 * @author Jiangwei Xu
 */
public final class TalkAcceptorHandler extends MessageHandler {

	private TalkService talkService;

	/** 构造函数。
	 */
	protected TalkAcceptorHandler(TalkService talkService) {
		this.talkService = talkService;
	}

	@Override
	public void sessionCreated(Session session) {
		// Nothing
	}

	@Override
	public void sessionDestroyed(Session session) {
		// Nothing
	}

	@Override
	public void sessionOpened(Session session) {
		this.talkService.openSession(session);
	}

	@Override
	public void sessionClosed(Session session) {
		this.talkService.closeSession(session);
	}

	@Override
	public void messageReceived(Session session, Message message) {
		byte[] data = message.get();
		Packet packet = Packet.unpack(data);
		if (null != packet) {
			interpret(session, packet);
		}
	}

	@Override
	public void messageSent(Session session, Message message) {
		// Nothing
	}

	@Override
	public void errorOccurred(int errorCode, Session session) {
		// Nothing
	}

	private void interpret(Session session, Packet packet) {
		byte[] tag = packet.getTag();

		if (TalkPacketDefine.isDialogue(tag)) {
			TalkDialogueCelletCommand cmd = new TalkDialogueCelletCommand(this.talkService, session, packet);
			cmd.execute();
			cmd = null;
		}
		else if (TalkPacketDefine.isHeartbeat(tag)) {
			TalkHeartbeatCommand cmd = new TalkHeartbeatCommand(this.talkService, session, packet);
			cmd.execute();
			cmd = null;
		}
		else if (TalkPacketDefine.isRequest(tag)) {
			TalkRequestCelletCommand cmd = new TalkRequestCelletCommand(this.talkService, session, packet);
			cmd.execute();
			cmd = null;
		}
		else if (TalkPacketDefine.isCheck(tag)) {
			TalkCheckCommand cmd = new TalkCheckCommand(this.talkService, session, packet);
			cmd.execute();
			cmd = null;
		}
	}
}
