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

import java.io.IOException;
import java.nio.charset.Charset;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.http.AbstractJSONHandler;
import net.cellcloud.http.CapsuleHolder;
import net.cellcloud.http.HttpHandler;
import net.cellcloud.http.HttpRequest;
import net.cellcloud.http.HttpResponse;
import net.cellcloud.http.HttpSession;
import net.cellcloud.talk.stuff.PrimitiveSerializer;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * HTTP 协议的对话处理器。
 * 
 * @author Jiangwei Xu
 *
 */
public final class HttpDialogueHandler extends AbstractJSONHandler implements CapsuleHolder {

	protected static final String Tag = "tag";
	protected static final String Primitive = "primitive";

	private TalkService talkService;

	public HttpDialogueHandler(TalkService service) {
		super();
		this.talkService = service;
	}

	@Override
	public String getPathSpec() {
		return "/talk/dialogue";
	}

	@Override
	public HttpHandler getHttpHandler() {
		return this;
	}

	@Override
	protected void doPost(HttpRequest request, HttpResponse response)
		throws IOException {
		HttpSession session = request.getSession();
		if (null != session) {
			try {
				// 读取包体数据
				JSONObject json = new JSONObject(new String(request.readRequestData(), Charset.forName("UTF-8")));
				// 解析 JSON 数据
				String speakerTag = json.getString(Tag);
				JSONObject primitiveJSON = json.getJSONObject(Primitive);
				// 解析原语
				Primitive primitive = new Primitive(speakerTag);
				PrimitiveSerializer.read(primitive, primitiveJSON);
				// 处理原语
				this.talkService.processDialogue(session, speakerTag, primitive);
				// 响应
				this.respondWithOk(response);
			} catch (JSONException e) {
				Logger.log(HttpDialogueHandler.class, e, LogLevel.ERROR);
				this.respond(response, HttpResponse.SC_BAD_REQUEST);
			}
		}
		else {
			this.respond(response, HttpResponse.SC_UNAUTHORIZED);
		}
	}
}
