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

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.MessageHandler;
import net.cellcloud.common.Session;

import org.json.JSONException;
import org.json.JSONObject;

public class WebSocketMessageHandler implements MessageHandler {

	protected final static String TALK_PACKET_TAG = "tpt";
	protected final static String TPT_INTERROGATE = "int";

	private TalkService service;

	public WebSocketMessageHandler(TalkService service) {
		this.service = service;
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
		// Nothing
	}

	@Override
	public void sessionClosed(Session session) {
		// Nothing
	}

	@Override
	public void messageReceived(Session session, Message message) {
		try {
			JSONObject data = new JSONObject(message.getAsString());
			String packetTag = data.getString(TALK_PACKET_TAG);
			if (null != packetTag) {
				
			}
			else {
				Logger.w(this.getClass(), "No talk packet tag in web socket stream");
			}
		} catch (JSONException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
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

	private void processInterrogate(JSONObject data, Session session) {
		
	}

	private void processCheck(JSONObject data, Session session) {
		
	}
}
