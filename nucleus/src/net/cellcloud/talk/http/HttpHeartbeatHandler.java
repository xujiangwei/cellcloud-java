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

package net.cellcloud.talk.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Queue;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.Packet;
import net.cellcloud.core.Nucleus;
import net.cellcloud.http.AbstractJSONHandler;
import net.cellcloud.http.CapsuleHolder;
import net.cellcloud.http.HttpHandler;
import net.cellcloud.http.HttpRequest;
import net.cellcloud.http.HttpResponse;
import net.cellcloud.http.HttpSession;
import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.TalkServiceKernel;
import net.cellcloud.talk.stuff.PrimitiveSerializer;
import net.cellcloud.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 基于 HTTP 协议的心跳处理器。
 * 
 * @author Ambrose Xu
 *
 */
public final class HttpHeartbeatHandler extends AbstractJSONHandler implements CapsuleHolder {

	/** 用于 JSON 数据的标识键。 */
	protected static final String Identifier = "identifier";
	/** 用于 JSON 数据的原语键。 */
	protected static final String Primitive = "primitive";
	/** 用于 JSON 数据的原语列表键。 */
	protected static final String Primitives = "primitives";

	/** Talk 服务核心。 */
	private TalkServiceKernel talkServiceKernel;

	/**
	 * 构造函数。
	 * 
	 * @param talkServiceKernel 指定 Talk 服务核心。
	 */
	public HttpHeartbeatHandler(TalkServiceKernel talkServiceKernel) {
		this.talkServiceKernel = talkServiceKernel;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPathSpec() {
		return "/talk/hb";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HttpHandler getHttpHandler() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doGet(HttpRequest request, HttpResponse response)
		throws IOException {
		HttpSession session = request.getSession();
		if (null != session) {
			// 心跳
			session.heartbeat();

			// 更新心跳
			this.talkServiceKernel.updateSessionHeartbeat(session);

			// 获取消息队列
			Queue<Message> queue = session.getQueue();
			if (!queue.isEmpty()) {
				ArrayList<String> identifiers = new ArrayList<String>(queue.size());
				ArrayList<Primitive> primitives = new ArrayList<Primitive>(queue.size());
				for (int i = 0, size = queue.size(); i < size; ++i) {
					// 消息出队
					Message message = queue.poll();
					// 解包
					Packet packet = Packet.unpack(message.get());
					if (null != packet) {
						// 将包数据转为输入流进行反序列化
						byte[] primData = packet.getSubsegment(0);
						ByteArrayInputStream stream = new ByteArrayInputStream(primData);

						// 反序列化
						Primitive prim = new Primitive(Nucleus.getInstance().getTagAsString());
						prim.read(stream);

						// 添加到数组
						identifiers.add(Utils.bytes2String(packet.getSubsegment(1)));
						primitives.add(prim);
					}
				}

				JSONArray jsonPrimitives = this.convert(identifiers, primitives);
				JSONObject json = new JSONObject();
				try {
					json.put(Primitives, jsonPrimitives);
				} catch (JSONException e) {
					Logger.log(getClass(), e, LogLevel.ERROR);
				}

				// 返回数据
				this.respondWithOk(response, json);
			}
			else {
				this.respondWithOk(response);
			}
		}
		else {
			this.respond(response, HttpResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doPost(HttpRequest request, HttpResponse response)
			throws IOException {
		this.doGet(request, response);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doOptions(HttpRequest request, HttpResponse response)
			throws IOException {
		response.setHeader("Access-Control-Allow-Headers", "Accept, Content-Type");
		response.setHeader("Access-Control-Allow-Methods", "POST, GET");
		response.setHeader("Access-Control-Allow-Origin", "*");
	}

	/**
	 * 将原语队列转为 JSON 数组。
	 * 
	 * @param identifiers 指定目标 Cellet 的标识清单。
	 * @param list 指定需转换的原语列表。
	 * @return 返回转换后的 JSON 数组。
	 */
	private JSONArray convert(ArrayList<String> identifiers, ArrayList<Primitive> list) {
		JSONArray ret = new JSONArray();

		try {
			for (int i = 0, size = identifiers.size(); i < size; ++i) {
				String identifier = identifiers.get(i);
				Primitive prim = list.get(i);

				JSONObject primJson = new JSONObject();
				PrimitiveSerializer.write(primJson, prim);

				JSONObject json = new JSONObject();
				json.put(Identifier, identifier);
				json.put(Primitive, primJson);

				// 写入数组
				ret.put(json);
			}
		} catch (JSONException e) {
			// Nothing
		}

		return ret;
	}

}
