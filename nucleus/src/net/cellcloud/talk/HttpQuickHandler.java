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

import java.io.IOException;
import java.nio.charset.Charset;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.core.Nucleus;
import net.cellcloud.http.AbstractJSONHandler;
import net.cellcloud.http.CapsuleHolder;
import net.cellcloud.http.HttpHandler;
import net.cellcloud.http.HttpRequest;
import net.cellcloud.http.HttpResponse;
import net.cellcloud.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 快速握手服务句柄。
 * 
 * @author Jiangwei Xu
 *
 */
public class HttpQuickHandler extends AbstractJSONHandler implements CapsuleHolder {

	protected static final String Plaintext = "plaintext";
	protected static final String Tag = "tag";
	protected static final String Identifiers = "identifiers";
	protected static final String Error = "error";

	private TalkService talkService;

	public HttpQuickHandler(TalkService talkService) {
		super();
		this.talkService = talkService;
	}

	@Override
	public String getPathSpec() {
		return "/talk/quick";
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
			TalkService.Certificate cert = this.talkService.getCertificate(session);
			if (null != cert) {
				// { "plaintext": plaintext, "tag": tag }
				String data = new String(request.readRequestData(), Charset.forName("UTF-8"));
				try {
					JSONObject json = new JSONObject(data);
					// 获得明文码
					String plaintext = json.getString(Plaintext);
					// 获得 Tag
					String tag = json.getString(Tag);
					// 获取 Cellet 清单
					JSONArray identifiers = json.getJSONArray(Identifiers);

					if (null != plaintext && plaintext.equals(cert.plaintext)) {
						// 检测通过
						this.talkService.acceptSession(session, tag);

						// 请求 Cellet
						boolean requestState = false;
						for (int i = 0, size = identifiers.length(); i < size; ++i) {
							String identifier = identifiers.getString(i);

							TalkTracker tracker = this.talkService.processRequest(session, tag, identifier);
							if (null != tracker) {
								requestState = true;
							}
						}

						JSONObject responseData = new JSONObject();
						responseData.put(HttpQuickHandler.Tag, Nucleus.getInstance().getTagAsString().toString());
						responseData.put(HttpQuickHandler.Identifiers, identifiers);
						if (!requestState) {
							responseData.put(HttpQuickHandler.Error, new String(TalkDefinition.SC_FAILURE_NOCELLET, Charset.forName("UTF-8")));
						}
						this.respondWithOk(response, responseData);
					}
					else {
						// 检测失败
						this.talkService.rejectSession(session);
						this.respond(response, HttpResponse.SC_UNAUTHORIZED);
					}
				} catch (JSONException e) {
					Logger.log(HttpCheckHandler.class, e, LogLevel.WARNING);
					this.respond(response, HttpResponse.SC_BAD_REQUEST);
				}
			}
			else {
				this.respond(response, HttpResponse.SC_BAD_REQUEST);
			}
		}
		else {
			// 获取 Session 失败
			this.respond(response, HttpResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	protected void doOptions(HttpRequest request, HttpResponse response)
			throws IOException {
		response.setHeader("Access-Control-Allow-Headers", "Accept, Content-Type");
		response.setHeader("Access-Control-Allow-Methods", "POST, GET");
		response.setHeader("Access-Control-Allow-Origin", "*");
	}

}
