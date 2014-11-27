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

import java.io.IOException;
import java.nio.ByteBuffer;

import net.cellcloud.common.Logger;

import org.eclipse.jetty.websocket.api.BatchMode;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.WebSocketListener;

/**
 * 
 * @author Jiangwei Xu
 */
public class DefaultWebSocket implements WebSocketListener {

	private org.eclipse.jetty.websocket.api.Session session;

	public DefaultWebSocket() {
		this.session = null;
	}

	@Override
	public void onWebSocketBinary(byte[] buf, int offset, int length) {
		Logger.d(this.getClass(), "onWebSocketBinary");

		if (!this.session.isOpen())
		{
			Logger.w(this.getClass(), "Session is closed");
			return;
		}

		RemoteEndpoint remote = this.session.getRemote();
		remote.sendBytes(ByteBuffer.wrap(buf, offset, length), null);
		if (remote.getBatchMode() == BatchMode.ON) {
			try {
				remote.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onWebSocketText(String text) {
		Logger.d(this.getClass(), "onWebSocketText");

		if (!this.session.isOpen()) {
			Logger.w(this.getClass(), "Session is closed");
			return;
		}

		RemoteEndpoint remote = this.session.getRemote();
		remote.sendString(text, null);
		if (remote.getBatchMode() == BatchMode.ON) {
			try {
				remote.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onWebSocketConnect(org.eclipse.jetty.websocket.api.Session session) {
		Logger.d(this.getClass(), "onWebSocketConnect");

		this.session = session;
	}

	@Override
	public void onWebSocketClose(int code, String reason) {
		Logger.d(this.getClass(), "onWebSocketClose");
	}

	@Override
	public void onWebSocketError(Throwable error) {
		Logger.d(this.getClass(), "onWebSocketError");
	}
}
