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

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.MessageHandler;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.talk.command.ServerCheckCommand;
import net.cellcloud.talk.command.ServerConsultCommand;
import net.cellcloud.talk.command.ServerDialogueCommand;
import net.cellcloud.talk.command.ServerHeartbeatCommand;
import net.cellcloud.talk.command.ServerProxyCommand;
import net.cellcloud.talk.command.ServerQuickCommand;
import net.cellcloud.talk.command.ServerRequestCommand;

/**
 * Talk 服务器网络数据处理句柄。
 * 
 * @author Ambrose Xu
 * 
 */
public final class TalkAcceptorHandler implements MessageHandler {

	private TalkServiceKernel kernel;
	private LinkedList<ServerDialogueCommand> dialogueCmdQueue;
	private LinkedList<ServerHeartbeatCommand> heartbeatCmdQueue;
	private LinkedList<ServerQuickCommand> quickCmdQueue;
	private LinkedList<ServerProxyCommand> proxyCmdQueue;

	/**
	 * 构造函数。
	 */
	protected TalkAcceptorHandler(TalkServiceKernel talkServiceKernel) {
		this.kernel = talkServiceKernel;
		this.dialogueCmdQueue = new LinkedList<ServerDialogueCommand>();
		this.heartbeatCmdQueue = new LinkedList<ServerHeartbeatCommand>();
		this.quickCmdQueue = new LinkedList<ServerQuickCommand>();
		this.proxyCmdQueue = new LinkedList<ServerProxyCommand>();
	}

	@Override
	public void sessionCreated(Session session) {
		// Nothing
	}

	@Override
	public void sessionDestroyed(Session session) {
		this.kernel.closeSession(session);
	}

	@Override
	public void sessionOpened(Session session) {
		this.kernel.openSession(session);
	}

	@Override
	public void sessionClosed(Session session) {
		this.kernel.closeSession(session);
	}

	@Override
	public void messageReceived(final Session session, final Message message) {
		byte[] data = message.get();
		try {
			final Packet packet = Packet.unpack(data);
			if (null != packet) {
				this.kernel.executor.execute(new Runnable() {
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
		// 通知互斥体，唤醒设置加密线程
		Object mutex = session.getAttribute("mutex");
		if (null != mutex) {
			session.removeAttribute("mutex");
			synchronized (mutex) {
				mutex.notifyAll();
			}
		}
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
		else if (TalkDefinition.isQuick(tag)) {
			try {
				ServerQuickCommand cmd = borrowQuickCommand(session, packet);
				cmd.execute();
				returnQuickCommand(cmd);
			} catch (Exception e) {
				Logger.log(TalkAcceptorHandler.class, e, LogLevel.ERROR);
			}
		}
		else if (TalkDefinition.isProxy(tag)) {
			try {
				ServerProxyCommand cmd = borrowProxyCommand(session, packet);
				cmd.execute();
				returnProxyCommand(cmd);
			} catch (Exception e) {
				Logger.log(TalkAcceptorHandler.class, e, LogLevel.ERROR);
			}
		}
		else if (TalkDefinition.isRequest(tag)) {
			try {
				ServerRequestCommand cmd = new ServerRequestCommand(this.kernel, session, packet);
				cmd.execute();
				cmd = null;
			} catch (Exception e) {
				Logger.log(TalkAcceptorHandler.class, e, LogLevel.ERROR);
			}
		}
		else if (TalkDefinition.isConsult(tag)) {
			try {
				ServerConsultCommand cmd = new ServerConsultCommand(this.kernel, session, packet);
				cmd.execute();
				cmd = null;
			} catch (Exception e) {
				Logger.log(TalkAcceptorHandler.class, e, LogLevel.ERROR);
			}
		}
		else if (TalkDefinition.isCheck(tag)) {
			try {
				ServerCheckCommand cmd = new ServerCheckCommand(this.kernel, session, packet);
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

			if (this.dialogueCmdQueue.size() <= 1) {
				cmd = new ServerDialogueCommand(this.kernel);
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

			if (this.heartbeatCmdQueue.size() <= 1) {
				cmd = new ServerHeartbeatCommand(this.kernel);
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

	private ServerQuickCommand borrowQuickCommand(Session session, Packet packet) {
		synchronized (this.quickCmdQueue) {
			ServerQuickCommand cmd = null;

			if (this.quickCmdQueue.size() <= 1) {
				cmd = new ServerQuickCommand(this.kernel);
			}
			else {
				cmd = this.quickCmdQueue.poll();
			}

			cmd.session = session;
			cmd.packet = packet;

			return cmd;
		}
	}

	private void returnQuickCommand(ServerQuickCommand cmd) {
		synchronized (this.quickCmdQueue) {
			cmd.session = null;
			cmd.packet = null;

			this.quickCmdQueue.offer(cmd);
		}
	}

	private ServerProxyCommand borrowProxyCommand(Session session, Packet packet) {
		synchronized (this.proxyCmdQueue) {
			ServerProxyCommand cmd = null;

			if (this.proxyCmdQueue.size() <= 1) {
				cmd = new ServerProxyCommand(this.kernel);
			}
			else {
				cmd = this.proxyCmdQueue.poll();
			}

			cmd.session = session;
			cmd.packet = packet;

			return cmd;
		}
	}

	private void returnProxyCommand(ServerProxyCommand cmd) {
		synchronized (this.proxyCmdQueue) {
			cmd.session = null;
			cmd.packet = null;

			this.proxyCmdQueue.offer(cmd);
		}
	}

}
