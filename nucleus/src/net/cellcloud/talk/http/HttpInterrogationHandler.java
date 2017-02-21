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

package net.cellcloud.talk.http;

import java.io.IOException;

import net.cellcloud.common.Cryptology;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
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
 * 会话询问。
 * 
 * @author Jiangwei Xu
 *
 */
public final class HttpInterrogationHandler extends AbstractJSONHandler implements CapsuleHolder {

	public static final String Ciphertext = "ciphertext";
	public static final String Key = "key";
	public static final String Version = "ver";

	private TalkServiceKernel talkServiceKernel;

	public HttpInterrogationHandler(TalkServiceKernel talkServiceKernel) {
		super();
		this.talkServiceKernel = talkServiceKernel;
	}

	@Override
	public String getPathSpec() {
		return "/talk/int";
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
			TalkServiceKernel.Certificate cert = this.talkServiceKernel.openSession(session);
			if (null != cert) {
				byte[] ciphertext = Cryptology.getInstance().simpleEncrypt(cert.plaintext.getBytes(), cert.key.getBytes());
				JSONObject json = new JSONObject();
				try {
					// {"ciphertext": ciphertext, "key": key}
					json.put(Ciphertext, Cryptology.getInstance().encodeBase64(ciphertext));
					json.put(Key, cert.key);
					json.put(Version, "1.1");
				} catch (JSONException e) {
					Logger.log(HttpInterrogationHandler.class, e, LogLevel.ERROR);
				}
				// 响应
				this.respondWithOk(response, json);
			}
			else {
				// 返回 Certificate 为 null
				this.respond(response, HttpResponse.SC_NOT_FOUND);
			}
		}
		else {
			// 获取 Session 失败
			this.respond(response, HttpResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	protected void doPost(HttpRequest request, HttpResponse response)
			throws IOException {
		this.doGet(request, response);
	}

	@Override
	protected void doOptions(HttpRequest request, HttpResponse response)
			throws IOException {
		response.setHeader("Access-Control-Allow-Headers", "Accept, Content-Type");
		response.setHeader("Access-Control-Allow-Methods", "POST, GET");
		response.setHeader("Access-Control-Allow-Origin", "*");
	}
}
