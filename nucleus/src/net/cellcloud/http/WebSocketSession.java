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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.Session;

import org.eclipse.jetty.websocket.api.BatchMode;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

/**
 * WebSocket 连接的会话。
 * 
 * @author Ambrose Xu
 *
 */
public class WebSocketSession extends Session {

	private org.eclipse.jetty.websocket.api.Session rawSession;

	/**
	 * 构造函数。
	 * 
	 * @param address
	 * @param session
	 */
	public WebSocketSession(InetSocketAddress address, org.eclipse.jetty.websocket.api.Session session) {
		super(null, address);
		this.rawSession = session;
	}

	/**
	 * 会话是否已开启。
	 * 
	 * @return 如果会话已开启返回 <code>true</code> 。
	 */
	public boolean isOpen() {
		return this.rawSession.isOpen();
	}

	@Override
	public void write(Message message) {
		if (!this.rawSession.isOpen()) {
			return;
		}

		try {
			RemoteEndpoint remote = this.rawSession.getRemote();
			remote.sendString(new String(message.get(), Charset.forName("UTF-8")), null);
			if (remote.getBatchMode() == BatchMode.ON) {
				try {
					remote.flush();
				} catch (IOException e) {
					Logger.log(this.getClass(), e, LogLevel.ERROR);
				}
			}
		} catch (Exception e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
		}
	}

}
