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
import net.cellcloud.talk.TalkServiceKernel;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 基于 HTTP 协议的接入校验处理器。
 * 
 * @author Ambrose Xu
 *
 */
public final class HttpCheckHandler extends AbstractJSONHandler implements CapsuleHolder {

	/** 用于 JSON 数据的明文键。 */
	public static final String Plaintext = "plaintext";
	/** 用于 JSON 数据的标签键。 */
	public static final String Tag = "tag";
	/** 用于 JSON 数据的错误信息键。 */
	public static final String Error = "error";

	/** Talk 服务核心。 */
	private TalkServiceKernel talkServiceKernel;

	/**
	 * 构造函数。
	 * 
	 * @param talkServiceKernel 指定 Talk 服务核心。
	 */
	public HttpCheckHandler(TalkServiceKernel talkServiceKernel) {
		super();
		this.talkServiceKernel = talkServiceKernel;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPathSpec() {
		return "/talk/check";
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
	protected void doPost(HttpRequest request, HttpResponse response)
		throws IOException {
		HttpSession session = request.getSession();
		if (null != session) {
			TalkServiceKernel.Certificate cert = this.talkServiceKernel.getCertificate(session);
			if (null != cert) {
				// { "plaintext": plaintext, "tag": tag }
				String data = new String(request.readRequestData(), Charset.forName("UTF-8"));
				try {
					JSONObject json = new JSONObject(data);
					// 获得明文码
					String plaintext = json.getString(Plaintext);
					// 获得 Tag
					String tag = json.getString(Tag);
					if (null != plaintext && plaintext.equals(cert.plaintext)) {
						// 检测通过
						this.talkServiceKernel.acceptSession(session, tag);
						// 返回数据
						JSONObject ret = new JSONObject();
						ret.put(Tag, Nucleus.getInstance().getTagAsString());
						this.respondWithOk(response, ret);
					}
					else {
						// 检测失败
						this.talkServiceKernel.rejectSession(session);
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

}
