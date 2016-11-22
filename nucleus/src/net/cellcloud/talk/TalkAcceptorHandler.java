/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2013 Cell Cloud Team (www.cellcloud.net)

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

import java.util.LinkedList;
import java.util.Queue;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.MessageHandler;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;

/** Talk 服务句柄。
 * 
 * @author Jiangwei Xu
 */
public final class TalkAcceptorHandler implements MessageHandler {

	private TalkService talkService;
	private Queue<ServerDialogueCommand> dialogueCmdQueue;
	private Queue<ServerHeartbeatCommand> heartbeatCmdQueue;

	/** 构造函数。
	 */
	protected TalkAcceptorHandler(TalkService talkService) {
		this.talkService = talkService;
		this.dialogueCmdQueue = new LinkedList<ServerDialogueCommand>();
		this.heartbeatCmdQueue = new LinkedList<ServerHeartbeatCommand>();
	}

	@Override
	public void sessionCreated(Session session) {
		// Nothing
	}

	@Override
	public void sessionDestroyed(Session session) {
		this.talkService.closeSession(session);
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
	public void messageReceived(final Session session, final Message message) {
		byte[] data = message.get();
		try {
			final Packet packet = Packet.unpack(data);
			if (null != packet) {
				this.talkService.executor.execute(new Runnable() {
					@Override
					public void run() {
						interpret(session, packet);
					}
				});
			}
		} catch (NumberFormatException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		} catch (ArrayIndexOutOfBoundsException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		}
	}

	@Override
	public void messageSent(Session session, Message message) {
		// Nothing
	}

	@Override
	public void errorOccurred(int errorCode, Session session) {
		Logger.d(this.getClass(), "Network error: " + errorCode + ", session: " + session.getAddress().getHostString());
	}

	private void interpret(Session session, Packet packet) {
		byte[] tag = packet.getTag();

		if (TalkDefinition.isDialogue(tag)) {
			try {
				ServerDialogueCommand cmd = borrowDialogueCommand(session, packet);
				cmd.execute();
				returnDialogueCommand(cmd);
			} catch (Exception e) {
				Logger.log(TalkAcceptorHandler.class, e, LogLevel.ERROR);
			}
		}
		else if (TalkDefinition.isHeartbeat(tag)) {
			try {
				ServerHeartbeatCommand cmd = borrowHeartbeatCommand(session, packet);
				cmd.execute();
				returnHeartbeatCommand(cmd);
			} catch (Exception e) {
				Logger.log(TalkAcceptorHandler.class, e, LogLevel.ERROR);
			}
		}
//		else if (TalkDefinition.isSuspend(tag)) {
//			try {
//				ServerSuspendCommand cmd = new ServerSuspendCommand(this.talkService, session, packet);
//				cmd.execute();
//				cmd = null;
//			} catch (Exception e) {
//				Logger.log(TalkAcceptorHandler.class, e, LogLevel.ERROR);
//			}
//		}
//		else if (TalkDefinition.isResume(tag)) {
//			try {
//				ServerResumeCommand cmd = new ServerResumeCommand(this.talkService, session, packet);
//				cmd.execute();
//				cmd = null;
//			} catch (Exception e) {
//				Logger.log(TalkAcceptorHandler.class, e, LogLevel.ERROR);
//			}
//		}
		else if (TalkDefinition.isRequest(tag)) {
			try {
				ServerRequestCommand cmd = new ServerRequestCommand(this.talkService, session, packet);
				cmd.execute();
				cmd = null;
			} catch (Exception e) {
				Logger.log(TalkAcceptorHandler.class, e, LogLevel.ERROR);
			}
		}
		else if (TalkDefinition.isConsult(tag)) {
			try {
				ServerConsultCommand cmd = new ServerConsultCommand(this.talkService, session, packet);
				cmd.execute();
				cmd = null;
			} catch (Exception e) {
				Logger.log(TalkAcceptorHandler.class, e, LogLevel.ERROR);
			}
		}
		else if (TalkDefinition.isCheck(tag)) {
			try {
				ServerCheckCommand cmd = new ServerCheckCommand(this.talkService, session, packet);
				cmd.execute();
				cmd = null;
			} catch (Exception e) {
				Logger.log(TalkAcceptorHandler.class, e, LogLevel.ERROR);
			}
		}
	}

	private ServerDialogueCommand borrowDialogueCommand(Session session, Packet packet) {
		synchronized (this.dialogueCmdQueue) {
			ServerDialogueCommand cmd = null;

			if (this.dialogueCmdQueue.isEmpty()) {
				cmd = new ServerDialogueCommand(this.talkService);
			}
			else {
				cmd = this.dialogueCmdQueue.poll();
			}

			cmd.session = session;
			cmd.packet = packet;

			return cmd;
		}
	}

	private void returnDialogueCommand(ServerDialogueCommand cmd) {
		synchronized (this.dialogueCmdQueue) {
			cmd.session = null;
			cmd.packet = null;

			this.dialogueCmdQueue.offer(cmd);
		}
	}

	private ServerHeartbeatCommand borrowHeartbeatCommand(Session session, Packet packet) {
		synchronized (this.heartbeatCmdQueue) {
			ServerHeartbeatCommand cmd = null;

			if (this.heartbeatCmdQueue.isEmpty()) {
				cmd = new ServerHeartbeatCommand(this.talkService);
			}
			else {
				cmd = this.heartbeatCmdQueue.poll();
			}

			cmd.session = session;
			cmd.packet = packet;

			return cmd;
		}
	}

	private void returnHeartbeatCommand(ServerHeartbeatCommand cmd) {
		synchronized (this.heartbeatCmdQueue) {
			cmd.session = null;
			cmd.packet = null;

			this.heartbeatCmdQueue.offer(cmd);
		}
	}
}
