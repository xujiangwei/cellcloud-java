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

package net.cellcloud.http;

import java.net.InetSocketAddress;

import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.MessageHandler;

import org.eclipse.jetty.websocket.api.WebSocketListener;

/**
 * 
 * @author Jiangwei Xu
 */
public class JettyWebSocket implements WebSocketListener {

	private org.eclipse.jetty.websocket.api.Session session;
	private MessageHandler handler;
	private WebSocketSession wsSession;

	public JettyWebSocket(MessageHandler handler) {
		this.handler = handler;
	}

	@Override
	public void onWebSocketBinary(byte[] buf, int offset, int length) {
		Logger.d(this.getClass(), "onWebSocketBinary");

		if (!this.session.isOpen()) {
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

	@Override
	public void onWebSocketText(String text) {
		//Logger.d(this.getClass(), "onWebSocketText");

		if (!this.session.isOpen()) {
			Logger.w(this.getClass(), "Session is closed");
			return;
		}

		if (null != this.handler) {
			Message message = new Message(text);
			this.handler.messageReceived(this.wsSession, message);
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

	@Override
	public void onWebSocketConnect(org.eclipse.jetty.websocket.api.Session session) {
		Logger.d(this.getClass(), "onWebSocketConnect");

		this.session = session;

		InetSocketAddress address = new InetSocketAddress(session.getRemoteAddress().getHostName()
				, session.getRemoteAddress().getPort());
		this.wsSession = new WebSocketSession(address, session);

		if (null != this.handler) {
			this.handler.sessionCreated(this.wsSession);
			this.handler.sessionOpened(this.wsSession);
		}
	}

	@Override
	public void onWebSocketClose(int code, String reason) {
		Logger.d(this.getClass(), "onWebSocketClose");

		if (null != this.handler) {
			this.handler.sessionClosed(this.wsSession);
			this.handler.sessionDestroyed(this.wsSession);
		}

		this.wsSession = null;
	}

	@Override
	public void onWebSocketError(Throwable error) {
		Logger.w(this.getClass(), "onWebSocketError: " + error.getMessage());
	}
}
