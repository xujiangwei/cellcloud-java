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

import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.Session;

import net.cellcloud.common.Logger;

import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;

/**
 * 
 * @author Jiangwei Xu
 */
public class DefaultWebSocket {

	public DefaultWebSocket() {
	}

	@OnWebSocketMessage
	public void onBinary(Session session, byte buf[], int offset, int length) throws IOException {
		if (!session.isOpen()) {
			Logger.w(DefaultWebSocket.class, "Session is closed");
			return;
		}

		Async remote = session.getAsyncRemote();
		remote.sendBinary(ByteBuffer.wrap(buf, offset, length), null);
		if (remote.getBatchingAllowed())
			remote.flushBatch();
	}

	@OnWebSocketMessage
	public void onText(Session session, String message) throws IOException {
		if (!session.isOpen()) {
			Logger.w(DefaultWebSocket.class, "Session is closed");
			return;
		}

		Async remote = session.getAsyncRemote();
		remote.sendText(message, null);
		if (remote.getBatchingAllowed())
			remote.flushBatch();
	}
}
