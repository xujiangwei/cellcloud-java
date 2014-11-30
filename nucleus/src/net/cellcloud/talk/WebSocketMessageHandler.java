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

/**
 * 
 * @author Jiangwei Xu
 *
 */
public class WebSocketMessageHandler implements MessageHandler {

	protected final static String TALK_PACKET_TAG = "tpt";
	protected final static String TALK_PACKET = "packet";

	protected final static String TPT_INTERROGATE = "interrogate";
	protected final static String TPT_CHECK = "check";
	protected final static String TPT_REQUEST = "request";
	protected final static String TPT_DIALOGUE = "dialogue";
	protected final static String TPT_HEARTBEAT = "hb";

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
		this.service.openSession(session);
	}

	@Override
	public void sessionClosed(Session session) {
		this.service.closeSession(session);
	}

	@Override
	public void messageReceived(Session session, Message message) {
		try {
			JSONObject data = new JSONObject(message.getAsString());
			String packetTag = data.getString(TALK_PACKET_TAG);
			if (null != packetTag) {
				if (packetTag.equals(TPT_DIALOGUE)) {
					
				}
				else if (packetTag.equals(TPT_HEARTBEAT)) {
					
				}
				else if (packetTag.equals(TPT_REQUEST)) {
					
				}
				else if (packetTag.equals(TPT_CHECK)) {
					this.processCheck(data.getJSONObject(TALK_PACKET), session);
				}
				else {
					Logger.w(this.getClass(), "Unknown TPT: " + packetTag);
				}
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

	private void processCheck(JSONObject data, Session session) {
		
	}
}
