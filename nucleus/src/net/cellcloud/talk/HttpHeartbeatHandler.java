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
import net.cellcloud.talk.stuff.PrimitiveSerializer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * HTTP 心跳处理器。
 * 
 * @author Jiangwei Xu
 *
 */
public final class HttpHeartbeatHandler extends AbstractJSONHandler implements CapsuleHolder {

	protected static final String Primitives = "primitives";

	public HttpHeartbeatHandler() {
	}

	@Override
	public String getPathSpec() {
		return "/talk/hb";
	}

	@Override
	public HttpHandler getHttpHandler() {
		return this;
	}

	@Override
	protected void doGet(HttpRequest request, HttpResponse response)
		throws IOException {
		HttpSession session = request.getSession();
		if (null != session) {
			// 心跳
			session.heartbeat();

			// 获取消息队列
			Queue<Message> queue = session.getQueue();
			if (!queue.isEmpty()) {
				ArrayList<Primitive> primitives = new ArrayList<Primitive>(queue.size());
				for (int i = 0, size = queue.size(); i < size; ++i) {
					// 消息出队
					Message message = queue.poll();
					// 解包
					Packet packet = Packet.unpack(message.get());
					if (null != packet) {
						// 将包数据转为输入流进行反序列化
						byte[] body = packet.getBody();
						ByteArrayInputStream stream = new ByteArrayInputStream(body);

						// 反序列化
						Primitive prim = new Primitive(Nucleus.getInstance().getTagAsString());
						prim.read(stream);

						// 添加到数组
						primitives.add(prim);
					}
				}

				JSONArray jsonPrimitives = this.convert(primitives);
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
	 * 将原语队列转为 JSON 数组。
	 * @param queue
	 * @return
	 */
	private JSONArray convert(ArrayList<Primitive> list) {
		JSONArray ret = new JSONArray();

		try {
			for (Primitive prim : list) {
				JSONObject json = new JSONObject();
				PrimitiveSerializer.write(json, prim);
				// 写入数组
				ret.put(json);
			}
		} catch (JSONException e) {
			// Nothing
		}

		return ret;
	}
}
