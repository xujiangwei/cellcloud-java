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

package net.cellcloud.http;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.LinkedList;

import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.MessageErrorCode;
import net.cellcloud.common.MessageHandler;
import net.cellcloud.common.MessageInterceptor;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * 基于 Jetty WebSocket 的 WebSocket 管理器实现。
 * 
 * @author Ambrose Xu
 * 
 */
@WebSocket(maxTextMessageSize = 64 * 1024, maxBinaryMessageSize = 64 * 1024)
public final class JettyWebSocket implements WebSocketManager {

	/** 消息处理器。 */
	private MessageHandler handler;
	/** 消息拦截器。 */
	private MessageInterceptor interceptor;

	/** 当前 WebSocket 上连接的所有 Jetty 会话。 */
	private LinkedList<Session> sessions;
	/** 当前 WebSocket 上连接的所有会话。 */
	private LinkedList<WebSocketSession> wsSessions;

	/** 接收的数据流量。 */
	private long rx = 0;
	/** 发送的数据流量。 */
	private long tx = 0;

	/**
	 * 构造函数。
	 * 
	 * @param handler 指定消息处理器。
	 */
	public JettyWebSocket(MessageHandler handler) {
		this.handler = handler;
		this.sessions = new LinkedList<Session>();
		this.wsSessions = new LinkedList<WebSocketSession>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setInterceptor(MessageInterceptor interceptor) {
		this.interceptor = interceptor;
	}

	/**
	 * 当前连接的会话数量。
	 * 
	 * @return 返回当前连接的会话数量。
	 */
	public int numSessions() {
		synchronized (this.sessions) {
			return this.sessions.size();
		}
	}

	/**
	 * 获得累计的接收数据流量。
	 * 
	 * @return 返回累计的接收数据流量。
	 */
	public long getTotalRx() {
		return this.rx;
	}

	/**
	 * 获得累计的发送数据流量。
	 * 
	 * @return 返回累计的发送数据流量。
	 */
	public long getTotalTx() {
		return this.tx;
	}

	/**
	 * 当接收到二进制数据时该方法被调用。
	 * 
	 * @param session 数据对应的会话。
	 * @param buf 接收到的数据。
	 * @param offset 数据偏移量。
	 * @param length 数据长度。
	 */
	@OnWebSocketMessage
	public void onWebSocketBinary(Session session, byte[] buf, int offset, int length) {
		Logger.d(this.getClass(), "onWebSocketBinary");

		if (!session.isOpen()) {
			Logger.w(this.getClass(), "Session is closed");
			return;
		}

		/*
		RemoteEndpoint remote = this.session.getRemote();
		remote.sendBytes(ByteBuffer.wrap(buf, offset, length), null);
		if (remote.getBatchMode() == BatchMode.ON) {
			try {
				remote.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		*/
	}

	/**
	 * 当接收到文本数据时该方法被调用。
	 * 
	 * @param session 数据对应的会话。
	 * @param text 接收到的文本数据。
	 */
	@OnWebSocketMessage
	public void onWebSocketText(Session session, String text) {
		//Logger.d(this.getClass(), "onWebSocketText");

		if (!session.isOpen()) {
			Logger.w(this.getClass(), "Session is closed");
			return;
		}

		WebSocketSession wsSession = null;
		synchronized (this.sessions) {
			int index = this.sessions.indexOf(session);
			wsSession = this.wsSessions.get(index);
		}

		// 接收流量计数
		this.rx += text.length();

		if (null != this.handler) {
			Message message = new Message(text.getBytes(Charset.forName("UTF-8")));

			// 判断是否拦截
			if (false == (null != this.interceptor && this.interceptor.interceptMessage(wsSession, message))) {
				this.handler.messageReceived(wsSession, message);
			}
		}

		/*
		RemoteEndpoint remote = this.session.getRemote();
		remote.sendString(text, null);
		if (remote.getBatchMode() == BatchMode.ON) {
			try {
				remote.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		*/
	}

	/**
	 * 当有新会话连接时调用该方法。
	 * 
	 * @param session 连接的新会话。
	 */
	@OnWebSocketConnect
	public void onWebSocketConnect(Session session) {
		Logger.d(this.getClass(), "onWebSocketConnect");

		// 设置闲置超时时间
		session.setIdleTimeout(60L * 60L * 1000L);

		synchronized (this.sessions) {
			int index = this.sessions.indexOf(session);
			if (index >= 0) {
				this.sessions.remove(index);
				this.wsSessions.remove(index);
			}
		}

		InetSocketAddress address = new InetSocketAddress(session.getRemoteAddress().getAddress().getHostAddress()
				, session.getRemoteAddress().getPort());
		WebSocketSession wsSession = new WebSocketSession(address, session);

		// 添加 session
		synchronized (this.sessions) {
			this.sessions.add(session);
			this.wsSessions.add(wsSession);
		}

		if (null != this.handler) {
			if (false == (null != this.interceptor && this.interceptor.interceptCreating(wsSession))) {
				this.handler.sessionCreated(wsSession);
			}

			if (false == (null != this.interceptor && this.interceptor.interceptOpening(wsSession))) {
				this.handler.sessionOpened(wsSession);
			}
		}
	}

	/**
	 * 当有会话关闭时调用该方法。
	 * 
	 * @param session 被关系的会话。
	 * @param code 关闭代码。
	 * @param reason 关闭原因描述。
	 */
	@OnWebSocketClose
	public void onWebSocketClose(Session session, int code, String reason) {
		Logger.d(this.getClass(), "onWebSocketClose");

		// 如果 session 是 open 状态，则不删除
//		if (session.isOpen()) {
//			Logger.d(this.getClass(), "onWebSocketClose # Session is open");
//			return;
//		}

		WebSocketSession wsSession = null;

		int index = -1;
		synchronized (this.sessions) {
			index = this.sessions.indexOf(session);
			if (index < 0) {
				return;
			}

			wsSession = this.wsSessions.get(index);

			this.sessions.remove(index);
			this.wsSessions.remove(index);
		}

		if (null != this.handler) {
			if (false == (null != this.interceptor && this.interceptor.interceptClosing(wsSession))) {
				this.handler.sessionClosed(wsSession);
			}

			if (false == (null != this.interceptor && this.interceptor.interceptDestroying(wsSession))) {
				this.handler.sessionDestroyed(wsSession);
			}
		}
	}

	/**
	 * 当会话发生错误时调用该方法。
	 * 
	 * @param session 发成错误的会话。
	 * @param cause 异常实例。
	 */
	@OnWebSocketError
	public void onWebSocketError(Session session, Throwable cause) {
		Logger.w(this.getClass(), "onWebSocketError: " + cause.getMessage());

		WebSocketSession wsSession = null;

		synchronized (this.sessions) {
			int index = this.sessions.indexOf(session);
			if (index >= 0) {
				wsSession = this.wsSessions.get(index);
			}
		}

		if (null != this.handler) {
			if (false == (null != this.interceptor && this.interceptor.interceptError(wsSession, MessageErrorCode.SOCKET_FAILED))) {
				this.handler.errorOccurred(MessageErrorCode.SOCKET_FAILED, wsSession);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasSession(WebSocketSession session) {
		synchronized (this.sessions) {
			return this.wsSessions.contains(session);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(WebSocketSession session, Message message) {
		session.write(message);

		// 发送流量计数
		this.tx += message.length();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close(WebSocketSession session) {
		Session rawSession = null;

		synchronized (this.sessions) {
			int index = this.wsSessions.indexOf(session);
			if (index >= 0) {
				rawSession = this.sessions.get(index);
			}
		}

		if (null != rawSession) {
			rawSession.close(1000, "Server close this session");
		}
	}

	/*
	private void checkSessionTimeout() {
		ArrayList<Session> closedList = new ArrayList<Session>();

		synchronized (this.sessions) {
			long time = Clock.currentTimeMillis();

			for (int i = 0; i < this.sessions.size(); ++i) {
				Session rawSession = this.sessions.get(i);
				WebSocketSession wsSession = this.wsSessions.get(i);

				if (time - wsSession.getHeartbeat() >= this.timeout) {
					closedList.add(rawSession);
				}
			}
		}

		if (!closedList.isEmpty()) {
			for (Session s : closedList) {
				s.close(1000, "Server close this session");
			}
			closedList.clear();
		}
		closedList = null;
	}
	*/

}
