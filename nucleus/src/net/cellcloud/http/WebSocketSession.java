/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2014 Cell Cloud Team (www.cellcloud.net)

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

import net.cellcloud.common.Message;
import net.cellcloud.common.Session;

import org.eclipse.jetty.websocket.api.BatchMode;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

/**
 * 
 * @author Jiangwei Xu
 *
 */
public class WebSocketSession extends Session {

	private org.eclipse.jetty.websocket.api.Session rawSession;

	public WebSocketSession(InetSocketAddress address, org.eclipse.jetty.websocket.api.Session session) {
		super(null, address);
		this.rawSession = session;
	}

	@Override
	public void write(Message message) {
		RemoteEndpoint remote = this.rawSession.getRemote();
		remote.sendString(message.getAsString(), null);
		if (remote.getBatchMode() == BatchMode.ON) {
			try {
				remote.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
