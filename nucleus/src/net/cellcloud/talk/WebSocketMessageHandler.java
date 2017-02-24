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

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.MessageHandler;
import net.cellcloud.common.Session;
import net.cellcloud.core.Nucleus;
import net.cellcloud.talk.http.HttpCheckHandler;
import net.cellcloud.talk.http.HttpDialogueHandler;
import net.cellcloud.talk.http.HttpQuickHandler;
import net.cellcloud.talk.http.HttpRequestHandler;
import net.cellcloud.talk.stuff.PrimitiveSerializer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Jiangwei Xu
 *
 */
public class WebSocketMessageHandler implements MessageHandler {

	protected final static String TALK_PACKET_TAG = "tpt";
	protected final static String TALK_PACKET_VERSION = "ver";
	public final static String TALK_PACKET = "packet";

	protected final static String TPT_INTERROGATE = "interrogate";
	protected final static String TPT_CHECK = "check";
	protected final static String TPT_REQUEST = "request";
	protected final static String TPT_DIALOGUE = "dialogue";
	protected final static String TPT_HEARTBEAT = "hb";
	protected final static String TPT_QUICK = "quick";

	private TalkServiceKernel service;
	private LinkedList<Task> taskList;
	private AtomicInteger taskCounts;

	public WebSocketMessageHandler(TalkServiceKernel service) {
		this.service = service;
		this.taskList = new LinkedList<Task>();
		this.taskCounts = new AtomicInteger(0);
	}

	public int numIdleTasks() {
		synchronized (this.taskList) {
			return this.taskList.size();
		}
	}

	public int numActiveTasks() {
		return this.taskCounts.get();
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
			JSONObject data = new JSONObject(new String(message.get(), Charset.forName("UTF-8")));
			String packetTag = data.getString(TALK_PACKET_TAG);
			if (null != packetTag) {
				if (packetTag.equals(TPT_DIALOGUE)) {
					this.processDialogue(data.getJSONObject(TALK_PACKET), session);
				}
				else if (packetTag.equals(TPT_QUICK)) {
					this.processQuick(data.getJSONObject(TALK_PACKET), session);
				}
				else if (packetTag.equals(TPT_HEARTBEAT)) {
					// 更新心跳
					this.service.updateSessionHeartbeat(session);
				}
				else if (packetTag.equals(TPT_REQUEST)) {
					this.processRequest(data.getJSONObject(TALK_PACKET), session);
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

	private void processDialogue(JSONObject data, Session session) {
		// 异步执行任务
		this.service.executor.execute(this.borrowTask(data, session));
	}

	private void processQuick(JSONObject data, Session session) {
		TalkServiceKernel.Certificate cert = this.service.getCertificate(session);
		if (null == cert) {
			Logger.w(this.getClass(), "Can not fined certificate for session: " + session.getId());
			return;
		}

		// { "plaintext": plaintext, "tag": tag, "identifiers": identifiers }
		try {
			// 获得明文码
			String plaintext = data.getString(HttpQuickHandler.Plaintext);
			// 获得 Tag
			String tag = data.getString(HttpQuickHandler.Tag);
			// 获取 Cellet 清单
			JSONArray identifiers = data.getJSONArray(HttpQuickHandler.Identifiers);

			if (null != plaintext && plaintext.equals(cert.plaintext)) {
				// 检测通过
				this.service.acceptSession(session, tag);

				// 请求 Cellet
				boolean request = false;
				for (int i = 0, size = identifiers.length(); i < size; ++i) {
					String identifier = identifiers.getString(i);

					TalkTracker tracker = this.service.processRequest(session, tag, identifier);
					if (null != tracker) {
						request = true;
					}
					else {
						request = false;
						break;
					}
				}

				JSONObject response = new JSONObject();
				response.put(TALK_PACKET_TAG, TPT_QUICK);

				if (request) {
					JSONObject packet = new JSONObject();
					packet.put(HttpQuickHandler.Tag, Nucleus.getInstance().getTagAsString().toString());
					packet.put(HttpQuickHandler.Identifiers, identifiers);
					response.put(TALK_PACKET, packet);
				}
				else {
					JSONObject packet = new JSONObject();
					packet.put(HttpQuickHandler.Tag, Nucleus.getInstance().getTagAsString().toString());
					packet.put(HttpQuickHandler.Identifiers, identifiers);
					packet.put(HttpQuickHandler.Error, new String(TalkDefinition.SC_FAILURE_NOCELLET, Charset.forName("UTF-8")));
					response.put(TALK_PACKET, packet);
				}

				Message message = new Message(response.toString().getBytes(Charset.forName("UTF-8")));
				session.write(message);
			}
			else {
				// 拒绝 session
				this.service.rejectSession(session);
			}
		} catch (JSONException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		}
	}

	private void processCheck(JSONObject data, Session session) {
		TalkServiceKernel.Certificate cert = this.service.getCertificate(session);
		if (null == cert) {
			Logger.w(this.getClass(), "Can not fined certificate for session: " + session.getId());
			return;
		}

		// {"plaintext": plaintext, "tag": tag}
		try {
			// 获得明文码
			String plaintext = data.getString(HttpCheckHandler.Plaintext);
			// 获得 Tag
			String tag = data.getString(HttpCheckHandler.Tag);

			if (null != plaintext && plaintext.equals(cert.plaintext)) {
				// 检测通过
				this.service.acceptSession(session, tag);

				// 返回数据
				JSONObject ret = new JSONObject();
				ret.put(TALK_PACKET_TAG, TPT_CHECK);

				JSONObject packet = new JSONObject();
				packet.put(HttpCheckHandler.Tag, Nucleus.getInstance().getTagAsString());
				ret.put(TALK_PACKET, packet);

				Message message = new Message(ret.toString().getBytes(Charset.forName("UTF-8")));
				session.write(message);
			}
			else {
				// 检测失败

				// 返回数据
				JSONObject ret = new JSONObject();
				ret.put(TALK_PACKET_TAG, TPT_CHECK);

				JSONObject packet = new JSONObject();
				packet.put(HttpCheckHandler.Error, new String(TalkDefinition.SC_FAILURE, Charset.forName("UTF-8")));
				ret.put(TALK_PACKET, packet);

				Message message = new Message(ret.toString().getBytes(Charset.forName("UTF-8")));
				session.write(message);

				// 拒绝 session
				this.service.rejectSession(session);
			}
		} catch (JSONException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		}
	}

	private void processRequest(JSONObject data, Session session) {
		// {"tag": tag, "identifier": identifier}
		try {
			String tag = data.getString(HttpRequestHandler.Tag);
			String identifier = data.getString(HttpRequestHandler.Identifier);
			// 请求 Cellet
			TalkTracker tracker = this.service.processRequest(session, tag, identifier);
			if (null != tracker) {
				// 成功
				JSONObject packet = new JSONObject();
				packet.put(HttpRequestHandler.Tag, tag);
				packet.put(HttpRequestHandler.Identifier, identifier);
				packet.put(HttpRequestHandler.Version, tracker.getCellet(identifier).getFeature().getVersion().toString());

				JSONObject ret = new JSONObject();
				ret.put(TALK_PACKET_TAG, TPT_REQUEST);
				ret.put(TALK_PACKET, packet);

				Message message = new Message(ret.toString().getBytes(Charset.forName("UTF-8")));
				session.write(message);
			}
			else {
				// 失败
				JSONObject packet = new JSONObject();
				packet.put(HttpRequestHandler.Tag, tag);
				packet.put(HttpRequestHandler.Identifier, identifier);
				packet.put(HttpRequestHandler.Error,
						new String(TalkDefinition.SC_FAILURE_NOCELLET, Charset.forName("UTF-8")));

				JSONObject ret = new JSONObject();
				ret.put(TALK_PACKET_TAG, TPT_REQUEST);
				ret.put(TALK_PACKET, packet);

				Message message = new Message(ret.toString().getBytes(Charset.forName("UTF-8")));
				session.write(message);
			}
		} catch (JSONException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		}
	}

	private Task borrowTask(final JSONObject data, final Session session) {
		Task task = null;

		synchronized (this.taskList) {
			if (this.taskList.isEmpty()) {
				task = new Task();
			}
			else {
				task = this.taskList.poll();
			}
		}

		this.taskCounts.incrementAndGet();

		task.data = data;
		task.session = session;
		return task;
	}

	private void returnTask(Task task) {
		task.data = null;
		task.session = null;

		this.taskCounts.decrementAndGet();

		synchronized (this.taskList) {
			this.taskList.push(task);
		}
	}

	/**
	 * 
	 * @author Jiangwei Xu
	 */
	protected class Task implements Runnable {
		protected JSONObject data;
		protected Session session;

		protected Task() {
		}

		@Override
		public void run() {
			try {
				String speakerTag = this.data.getString(HttpDialogueHandler.Tag);
				String celletIdentifier = this.data.getString(HttpDialogueHandler.Identifier);
				JSONObject primitiveJSON = this.data.getJSONObject(HttpDialogueHandler.Primitive);
				// 解析原语
				Primitive primitive = new Primitive(speakerTag);
				PrimitiveSerializer.read(primitive, primitiveJSON);
				// 处理原语
				service.processDialogue(this.session, speakerTag, celletIdentifier, primitive);
			} catch (JSONException e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			}

			// 归还任务
			returnTask(this);
		}
	}
}
