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
import net.cellcloud.talk.command.ServerProxyDialogueResponseCommand;
import net.cellcloud.talk.command.ServerQuickCommand;
import net.cellcloud.talk.command.ServerRequestCommand;

/**
 * Talk 服务器网络数据处理器。
 * 
 * @author Ambrose Xu
 * 
 */
public final class TalkAcceptorHandler implements MessageHandler {

	/** 会话服务核心。 */
	private TalkServiceKernel kernel;
	/** 用于优化内存操作的对话命令队列。 */
	private LinkedList<ServerDialogueCommand> dialogueCmdQueue;
	/** 用于优化内存操作的心跳命令队列。 */
	private LinkedList<ServerHeartbeatCommand> heartbeatCmdQueue;
	/** 用于优化内存操作的快速握手命令队列。 */
	private LinkedList<ServerQuickCommand> quickCmdQueue;
	/** 用于优化内存操作的代理命令队列。 */
	private LinkedList<ServerProxyCommand> proxyCmdQueue;

	/**
	 * 构造函数。
	 * 
	 * @param talkServiceKernel 指定服务核心对象。
	 */
	protected TalkAcceptorHandler(TalkServiceKernel talkServiceKernel) {
		this.kernel = talkServiceKernel;
		this.dialogueCmdQueue = new LinkedList<ServerDialogueCommand>();
		this.heartbeatCmdQueue = new LinkedList<ServerHeartbeatCommand>();
		this.quickCmdQueue = new LinkedList<ServerQuickCommand>();
		this.proxyCmdQueue = new LinkedList<ServerProxyCommand>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionCreated(Session session) {
		// Nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionDestroyed(Session session) {
		this.kernel.closeSession(session);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionOpened(Session session) {
		this.kernel.openSession(session);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionClosed(Session session) {
		this.kernel.closeSession(session);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void messageReceived(final Session session, final Message message) {
		byte[] data = message.get();
		try {
			final Packet packet = Packet.unpack(data);
			if (null != packet) {
				this.kernel.executor.execute(new Runnable() {
					@Override
					public void run() {
						process(session, packet);
					}
				});
			}
		} catch (NumberFormatException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		} catch (ArrayIndexOutOfBoundsException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		}
	}

	/**
	 * {@inheritDoc}
	 */
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void errorOccurred(int errorCode, Session session) {
		Logger.d(this.getClass(), "Network error: " + errorCode + ", session: " + session.getAddress().getHostString());
	}

	/**
	 * 进行包分析并处理数据。
	 * 
	 * @param session 指定数据关联的会话上下文。
	 * @param packet 指定数据包。
	 */
	private void process(Session session, Packet packet) {
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
		else if (TalkDefinition.isProxyDialogueResponse(tag)) {
			try {
				ServerProxyDialogueResponseCommand cmd = new ServerProxyDialogueResponseCommand(this.kernel, session, packet);
				cmd.execute();
				cmd = null;
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

	/**
	 * 借出对话命令。
	 * 
	 * @param session 指定关联会话上下文。
	 * @param packet 指定关联的数据包。
	 * @return 返回服务器对话命令。
	 */
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

	/**
	 * 归还对话命令。
	 * 
	 * @param cmd 指定需归还的服务器对话命令。
	 */
	private void returnDialogueCommand(ServerDialogueCommand cmd) {
		synchronized (this.dialogueCmdQueue) {
			cmd.session = null;
			cmd.packet = null;

			this.dialogueCmdQueue.offer(cmd);
		}
	}

	/**
	 * 借出心跳命令。
	 * 
	 * @param session 指定关联会话上下文。
	 * @param packet 指定关联的数据包。
	 * @return 返回服务器心跳命令。
	 */
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

	/**
	 * 归还心跳命令。
	 * 
	 * @param cmd 指定需归还的服务器心跳命令。
	 */
	private void returnHeartbeatCommand(ServerHeartbeatCommand cmd) {
		synchronized (this.heartbeatCmdQueue) {
			cmd.session = null;
			cmd.packet = null;

			this.heartbeatCmdQueue.offer(cmd);
		}
	}

	/**
	 * 借出快速握手命令。
	 * 
	 * @param session 指定关联会话上下文。
	 * @param packet 指定关联的数据包。
	 * @return 返回服务器快速握手命令。
	 */
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

	/**
	 * 归还快速握手命令。
	 * 
	 * @param cmd 指定需归还的服务器快速握手命令。
	 */
	private void returnQuickCommand(ServerQuickCommand cmd) {
		synchronized (this.quickCmdQueue) {
			cmd.session = null;
			cmd.packet = null;

			this.quickCmdQueue.offer(cmd);
		}
	}

	/**
	 * 借出代理命令。
	 * 
	 * @param session 指定关联会话上下文。
	 * @param packet 指定关联的数据包。
	 * @return 返回服务器代理命令。
	 */
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

	/**
	 * 归还代理命令。
	 * 
	 * @param cmd 指定需归还的服务器代理命令。
	 */
	private void returnProxyCommand(ServerProxyCommand cmd) {
		synchronized (this.proxyCmdQueue) {
			cmd.session = null;
			cmd.packet = null;

			this.proxyCmdQueue.offer(cmd);
		}
	}

}
