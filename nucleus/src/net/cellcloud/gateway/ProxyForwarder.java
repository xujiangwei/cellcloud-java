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

package net.cellcloud.gateway;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;

import net.cellcloud.Version;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.MessageInterceptor;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.core.Nucleus;
import net.cellcloud.gateway.GatewayService.Slave;
import net.cellcloud.gateway.RoutingTable.Record;
import net.cellcloud.http.HttpSession;
import net.cellcloud.http.WebSocketSession;
import net.cellcloud.talk.CompatibilityHelper;
import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.TalkDefinition;
import net.cellcloud.talk.WebSocketMessageHandler;
import net.cellcloud.talk.http.HttpDialogueHandler;
import net.cellcloud.talk.http.HttpInterceptable;
import net.cellcloud.talk.stuff.PrimitiveSerializer;
import net.cellcloud.talk.stuff.StuffVersion;
import net.cellcloud.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public class ProxyForwarder implements MessageInterceptor, HttpInterceptable {

	private final static byte WS_TPT_DIALOGUE_1 = 'd';
	private final static byte WS_TPT_DIALOGUE_2 = 'i';
	private final static byte WS_TPT_DIALOGUE_3 = 'a';
	private final static byte WS_TPT_DIALOGUE_4 = 'l';
	private final static byte WS_TPT_DIALOGUE_5 = 'o';
	private final static byte WS_TPT_DIALOGUE_6 = 'g';
	private final static byte WS_TPT_DIALOGUE_7 = 'u';
	private final static byte WS_TPT_DIALOGUE_8 = 'e';

	private RoutingTable routingTable;

	private ExecutorService executor;

	public ProxyForwarder(RoutingTable routingTable, ExecutorService executor) {
		this.routingTable = routingTable;
		this.executor = executor;
	}

	@Override
	public boolean interceptCreating(Session session) {
		// Nothing
		return false;
	}

	@Override
	public boolean interceptDestroying(Session session) {
		// Nothing
		return false;
	}

	@Override
	public boolean interceptOpening(Session session) {
		// Nothing
		return false;
	}

	@Override
	public boolean interceptClosing(final Session session) {
		this.executor.execute(new Runnable() {
			@Override
			public void run() {
				// 会话被销毁
				Slave slave = routingTable.querySlave(session);
				if (null != slave) {
					// 移除路由信息
					Record record = routingTable.remove(session);

					for (int i = 0; i < record.runtimeIdentifiers.size(); ++i) {
						JSONObject proxy = new JSONObject();
						try {
							proxy.put("proxy", Nucleus.getInstance().getTagAsString());
							proxy.put("sid", session.getId().longValue());
							proxy.put("tag", record.tag);
							proxy.put("identifier", record.runtimeIdentifiers.get(i));
							proxy.put("active", false);
						} catch (JSONException e) {
							Logger.log(ProxyForwarder.class, e, LogLevel.WARNING);
						}

						// 通过代理协议关闭路由
						slave.kernel.proxy(slave.celletIdentifiers.get(0), proxy);
					}

					// 从下位机移除
					slave.removeSession(record.tag);
				}
			}
		});

		return false;
	}

	@Override
	public boolean interceptMessage(final Session session, Message message) {
		final byte[] data = message.get();

		if (session instanceof WebSocketSession) {
			// WS Speaker
			boolean dialogue = false;
			for (int i = 0, len = data.length - 8; i < len; ++i) {
				if (data[i] == WS_TPT_DIALOGUE_1
					&& data[i + 1] == WS_TPT_DIALOGUE_2
					&& data[i + 2] == WS_TPT_DIALOGUE_3
					&& data[i + 3] == WS_TPT_DIALOGUE_4
					&& data[i + 4] == WS_TPT_DIALOGUE_5
					&& data[i + 5] == WS_TPT_DIALOGUE_6
					&& data[i + 6] == WS_TPT_DIALOGUE_7
					&& data[i + 7] == WS_TPT_DIALOGUE_8) {
					dialogue = true;
					break;
				}
			}

			if (dialogue) {
				// 执行发送任务
				this.executor.execute(new Runnable() {
					@Override
					public void run() {
						Slave slave = routingTable.querySlave(session);
						if (null != slave) {
							JSONObject json = null;
							try {
								json = new JSONObject(new String(data, Charset.forName("UTF-8")));
							} catch (JSONException e) {
								Logger.log(ProxyForwarder.class, e, LogLevel.ERROR);
								return;
							}
							// 查找该 Session 对应的 Tag
							String speakerTag = routingTable.queryTag(session);
							if (null == speakerTag) {
								Logger.e(ProxyForwarder.class, "Can NOT query session tag: " + 
										session.getAddress().getHostString());
								return;
							}
							// 将 JSON 格式转为 Packet
							Packet dataPacket = convert(json, speakerTag);
							if (null == dataPacket) {
								Logger.e(ProxyForwarder.class, "Converts JSON to Packet error: " + 
										session.getAddress().getHostString());
								return;
							}

							if (!slave.kernel.pass(slave.celletIdentifiers.get(0), Packet.pack(dataPacket))) {
								Logger.w(ProxyForwarder.class,
										"Pass dialogue data failed (WS), cellet identifier: " + slave.celletIdentifiers.get(0));
							}
						}
						else {
							Logger.w(ProxyForwarder.class,
									"Can NOT find routing info for session (WS) '" + session.getAddress().getHostString());
						}
					}
				});

				// 拦截
				return true;
			}
		}
		else {
			// 一般 Speaker
			try {
				Packet packet = Packet.unpack(data);
				if (null != packet) {
					byte[] ptag = packet.getTag();

					// 拦截对话包
					if (TalkDefinition.isDialogue(ptag)) {
						// 执行发送任务
						this.executor.execute(new Runnable() {
							@Override
							public void run() {
								Slave slave = routingTable.querySlave(session);
								if (null != slave) {
									if (!slave.kernel.pass(slave.celletIdentifiers.get(0), data)) {
										Logger.w(ProxyForwarder.class,
												"Pass dialogue data failed, cellet identifier: " + slave.celletIdentifiers.get(0));
									}
								}
								else {
									Logger.w(ProxyForwarder.class,
											"Can NOT find routing info for session '" + session.getAddress().getHostString());
								}
							}
						});

						// 拦截
						return true;
					}
				}
			} catch (NumberFormatException e) {
				Logger.log(this.getClass(), e, LogLevel.WARNING);
			} catch (ArrayIndexOutOfBoundsException e) {
				Logger.log(this.getClass(), e, LogLevel.WARNING);
			}
		}

		return false;
	}

	@Override
	public boolean interceptError(Session session, int errorCode) {
		// Nothing
		return false;
	}

	@Override
	public boolean intercept(final HttpSession session, final String speakerTag, final String celletIdentifier, final Primitive primitive) {
		this.executor.execute(new Runnable() {
			@Override
			public void run() {
				// 兼容性判断
				StuffVersion version = CompatibilityHelper.match(Version.VERSION_NUMBER);
				if (version != primitive.getVersion()) {
					primitive.setVersion(version);
				}

				// 序列化原语
				ByteArrayOutputStream stream = primitive.write();

				// 封装数据包
				Packet packet = new Packet(TalkDefinition.TPT_DIALOGUE, 99, 1, 0);
				packet.appendSubsegment(stream.toByteArray());
				packet.appendSubsegment(Utils.string2Bytes(speakerTag));
				packet.appendSubsegment(Utils.string2Bytes(celletIdentifier));

				Slave slave = routingTable.querySlave(session);
				if (null != slave) {
					if (!slave.kernel.pass(slave.celletIdentifiers.get(0), Packet.pack(packet))) {
						Logger.w(ProxyForwarder.class,
								"Pass dialogue data failed (HTTP), cellet identifier: " + slave.celletIdentifiers.get(0));
					}
				}
				else {
					Logger.w(ProxyForwarder.class,
							"Can NOT find routing info for session (HTTP) '" + session.getAddress().getHostString());
				}
			}
		});

		return true;
	}

	/**
	 * 将 JSON 格式转数据包。
	 * 
	 * @param json
	 * @param speakerTag
	 * @return
	 */
	private Packet convert(JSONObject json, String speakerTag) {
		try {
			// 读取包内容
			JSONObject jsonPacket = json.getJSONObject(WebSocketMessageHandler.TALK_PACKET);

			// Cellet Identifier
			String celletIdentifier = jsonPacket.getString(HttpDialogueHandler.Identifier);
			// Primitive JSON
			JSONObject primitiveJSON = jsonPacket.getJSONObject(HttpDialogueHandler.Primitive);

			// 反序列化
			Primitive primitive = new Primitive(speakerTag);
			PrimitiveSerializer.read(primitive, primitiveJSON);

			// 兼容性判断
			StuffVersion version = CompatibilityHelper.match(Version.VERSION_NUMBER);
			if (version != primitive.getVersion()) {
				primitive.setVersion(version);
			}

			// 序列化原语
			ByteArrayOutputStream stream = primitive.write();

			// 封装数据包
			Packet packet = new Packet(TalkDefinition.TPT_DIALOGUE, 99, 1, 0);
			packet.appendSubsegment(stream.toByteArray());
			packet.appendSubsegment(Utils.string2Bytes(speakerTag));
			packet.appendSubsegment(Utils.string2Bytes(celletIdentifier));

			return packet;
		} catch (JSONException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
		}

		return null;
	}

}
