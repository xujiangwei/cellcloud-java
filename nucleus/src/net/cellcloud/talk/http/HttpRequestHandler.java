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

import org.json.JSONException;
import org.json.JSONObject;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.http.AbstractJSONHandler;
import net.cellcloud.http.CapsuleHolder;
import net.cellcloud.http.HttpHandler;
import net.cellcloud.http.HttpRequest;
import net.cellcloud.http.HttpResponse;
import net.cellcloud.http.HttpSession;
import net.cellcloud.talk.TalkDefinition;
import net.cellcloud.talk.TalkServiceKernel;
import net.cellcloud.talk.TalkTracker;

/**
 * 基于 HTTP 协议的 Cellet 请求处理器。
 * 
 * @author Ambrose Xu
 *
 */
public final class HttpRequestHandler extends AbstractJSONHandler implements CapsuleHolder {

	/** 用于 JSON 数据的 Cellet 标识键。 */
	public static final String Identifier = "identifier";
	/** 用于 JSON 数据的标签键。 */
	public static final String Tag = "tag";
	/** 用于 JSON 数据的版本信息键。 */
	public static final String Version = "version";
	/** 用于 JSON 数据的错误信息键。 */
	public static final String Error = "error";

	/** Talk 服务核心。 */
	private TalkServiceKernel talkServiceKernel;

	/**
	 * 构造函数。
	 * 
	 * @param talkServiceKernel 指定 Talk 服务核心。
	 */
	public HttpRequestHandler(TalkServiceKernel talkServiceKernel) {
		super();
		this.talkServiceKernel = talkServiceKernel;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPathSpec() {
		return "/talk/request";
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
			// {"tag": tag, "identifier": identifier}
			String data = new String(request.readRequestData(), Charset.forName("UTF-8"));
			try {
				JSONObject json = new JSONObject(data);
				String tag = json.getString(Tag);
				String identifier = json.getString(Identifier);
				// 请求 Cellet
				TalkTracker tracker = this.talkServiceKernel.processRequest(session, tag, identifier);
				if (null != tracker) {
					// 成功
					JSONObject ret = new JSONObject();
					ret.put(Tag, tag);
					ret.put(Identifier, identifier);
					ret.put(Version, tracker.getCellet(identifier).getFeature().getVersion().toString());
					this.respondWithOk(response, ret);
				}
				else {
					// 失败
					JSONObject ret = new JSONObject();
					ret.put(Tag, tag);
					ret.put(Identifier, identifier);
					ret.put(Error, new String(TalkDefinition.SC_FAILURE_NOCELLET, Charset.forName("UTF-8")));
					this.respondWithOk(response, ret);
				}
			} catch (JSONException e) {
				Logger.log(HttpRequestHandler.class, e, LogLevel.WARNING);
				this.respond(response, HttpResponse.SC_BAD_REQUEST);
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
	protected void doGet(HttpRequest request, HttpResponse response)
			throws IOException {
		this.doPost(request, response);
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
