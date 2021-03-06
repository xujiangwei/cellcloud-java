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

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import net.cellcloud.common.Cryptology;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.core.Nucleus;
import net.cellcloud.http.HttpResponse;
import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.Speakable;
import net.cellcloud.talk.SpeakerState;
import net.cellcloud.talk.TalkFailureCode;
import net.cellcloud.talk.TalkServiceFailure;
import net.cellcloud.talk.speaker.SpeakerDelegate;
import net.cellcloud.talk.stuff.PrimitiveSerializer;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 基于 HTTP 协议的对话者。
 * 
 * @author Ambrose Xu
 *
 */
public class HttpSpeaker implements Speakable {

	/** 请求握手验证的 URI */
	private static final String URI_INTERROGATION = "/talk/int";
	/** 验证的 URI */
	private static final String URI_CHECK = "/talk/check";
	/** 请求 Cellet 的 URI */
	private static final String URI_REQUEST = "/talk/request";
	/** 会话数据发送 URI */
	private static final String URI_DIALOGUE = "/talk/dialogue";
	/** 心跳 URI */
	private static final String URI_HEARTBEAT = "/talk/hb";

	/** 对话者事件委派。 */
	private SpeakerDelegate delegate;

	/** 当前状态。 */
	private volatile int state = SpeakerState.HANGUP;

	/** 当前请求的 Cellet 标识列表。 */
	private ArrayList<String> identifierList;
	/** 当前访问地址。 */
	private InetSocketAddress address;
	/** 使用的 HTTP 客户端。 */
	private HttpClient client;

	/** 当前请求的全局 Cookie 。 */
	private String cookie;

	/** 服务器的 Tag 。 */
	private String remoteTag;

	/** 心跳控制计数。 */
	private int hbTick;
	/** 心跳计数周期。 */
	private int hbPeriod;
	/** 心跳失败累计次数。 */
	private int hbFailedCounts;
	/** 最大心跳失败次数。 */
	private int hbMaxFailed;

	/** 线程执行器。 */
	private ExecutorService executor;

	/**
	 * 构造函数。
	 * 
	 * @param address 指定访问地址。
	 * @param delegate 指定事件委派。
	 * @param heartbeatPeriod 指定心跳周期。
	 * @param executor 指定线程执行器。
	 */
	public HttpSpeaker(InetSocketAddress address, SpeakerDelegate delegate, int heartbeatPeriod, ExecutorService executor) {
		this.address = address;
		this.delegate = delegate;
		this.client = new HttpClient();
		this.client.setConnectTimeout(10000L);
		this.hbTick = 0;
		this.hbPeriod = heartbeatPeriod;
		this.hbFailedCounts = 0;
		this.hbMaxFailed = 10;
		this.identifierList = new ArrayList<String>(2);
		this.executor = executor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getIdentifiers() {
		return this.identifierList;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRemoteTag() {
		return this.remoteTag;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean call(List<String> identifiers) {
		if (SpeakerState.CALLING == this.state) {
			// 正在 Call 返回 false
			return false;
		}

		if (this.client.isStarting()) {
			// 正在启动则返回失败
			return false;
		}

		if (null != identifiers) {
			for (String identifier : identifiers) {
				if (this.identifierList.contains(identifier)) {
					continue;
				}
	
				this.identifierList.add(identifier);
			}
		}

		if (this.identifierList.isEmpty()) {
			return false;
		}

		// 尝试启动
		if (!this.client.isStarted()) {
			try {
				// 启动
				this.client.start();

				int counts = 0;
				while (!this.client.isStarted()) {
					Thread.sleep(100L);

					++counts;
					if (counts > 15) {
						break;
					}
				}
			} catch (Exception e) {
				Logger.log(HttpSpeaker.class, e, LogLevel.ERROR);
				return false;
			}
		}

		// 更新状态
		this.state = SpeakerState.CALLING;

		// 拼装 URL
		StringBuilder url = new StringBuilder("http://");
		url.append(this.address.getHostString()).append(":").append(this.address.getPort());
		url.append(URI_INTERROGATION);

		try {
			ContentResponse response = this.client.newRequest(url.toString()).method(HttpMethod.GET).send();
			if (response.getStatus() == HttpResponse.SC_OK) {
				// 获取服务器提供的 Cookie
				this.cookie = response.getHeaders().get(HttpHeader.SET_COOKIE);

				// 解析数据
				JSONObject data = this.readContent(response.getContent());
				String ciphertextBase64 = data.getString(HttpInterrogationHandler.Ciphertext);
				final String key = data.getString(HttpInterrogationHandler.Key);
				final byte[] ciphertext = Cryptology.getInstance().decodeBase64(ciphertextBase64);
				// 发送 Check 请求
				this.executor.execute(new Runnable() {
					@Override
					public void run() {
						requestCheck(ciphertext, key.getBytes(Charset.forName("UTF-8")));
					}
				});
				return true;
			}
		} catch (InterruptedException | TimeoutException | ExecutionException | JSONException e) {
			Logger.log(HttpSpeaker.class, e, LogLevel.ERROR);
			// 设置状态
			this.state = SpeakerState.HANGUP;
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void hangUp() {
		// TODO 发送 Hang up 请求

		try {
			this.client.stop();
		} catch (Exception e) {
			Logger.log(HttpSpeaker.class, e, LogLevel.DEBUG);
		}

		if (this.state != SpeakerState.HANGUP) {
			this.state = SpeakerState.HANGUP;

			for (String identifier : this.identifierList) {
				this.fireQuitted(identifier);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean speak(String celletIdentifier, Primitive primitive) {
		if (this.state != SpeakerState.CALLED
			|| !this.client.isStarted()) {
			return false;
		}

		JSONObject json = new JSONObject();
		try {
			// 源 Tag
			json.put(HttpDialogueHandler.Tag, Nucleus.getInstance().getTagAsString());
			// 原语 JSON
			JSONObject primJSON = new JSONObject();
			PrimitiveSerializer.write(primJSON, primitive);
			json.put(HttpDialogueHandler.Primitive, primJSON);
			// Cellet
			json.put(HttpDialogueHandler.Identifier, celletIdentifier);
		} catch (JSONException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
			return false;
		}

		// URL
		StringBuilder url = new StringBuilder("http://");
		url.append(this.address.getHostString()).append(":").append(this.address.getPort());
		url.append(URI_DIALOGUE);

		// 数据内容
		StringContentProvider content = new StringContentProvider(json.toString(), "UTF-8");
		try {
			// 发送请求
			ContentResponse response = this.client.newRequest(url.toString())
											.method(HttpMethod.POST)
											.header(HttpHeader.COOKIE, this.cookie)
											.content(content)
											.send();
			if (response.getStatus() == HttpResponse.SC_OK) {
				// 发送数据成功
				// 获取队列长度
				JSONObject data = this.readContent(response.getContent());
				if (data.has(HttpDialogueHandler.Queue)) {
					int size = data.getInt(HttpDialogueHandler.Queue);
					if (size > 0) {
						// 心跳 Tick 清零
						this.hbTick = 0;

						this.executor.execute(new Runnable() {
							@Override
							public void run() {
								requestHeartbeat();
							}
						});
					}
				}
			}
			else {
				Logger.w(this.getClass(), "Send dialogue data failed : " + response.getStatus());
			}
		} catch (InterruptedException | TimeoutException | ExecutionException e) {
			return false;
		} catch (JSONException e) {
			return false;
		}

		url = null;

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCalled() {
		return (this.state == SpeakerState.CALLED);
	}

	/**
	 * 每秒计时。
	 */
	public void tick() {
		if (this.state != SpeakerState.CALLED) {
			return;
		}

		// tick 计数
		++this.hbTick;

		if (this.hbTick >= this.hbPeriod) {
			this.hbTick = 0;

			if (Logger.isDebugLevel()) {
				Logger.d(HttpSpeaker.class, "Http heartbeat request : " + this.address.getHostString());
			}

			// 执行心跳
			this.executor.execute(new Runnable() {
				@Override
				public void run() {
					requestHeartbeat();
				}
			});
		}
	}

	/**
	 * 发送心跳数据。
	 */
	private void requestHeartbeat() {
		// 拼装 URL
		StringBuilder url = new StringBuilder("http://");
		url.append(this.address.getHostString()).append(":").append(this.address.getPort());
		url.append(URI_HEARTBEAT);

		// 发送请求
		try {
			ContentResponse response = this.client.newRequest(url.toString())
											.method(HttpMethod.GET)
											.header(HttpHeader.COOKIE, this.cookie)
											.send();
			if (response.getStatus() == HttpResponse.SC_OK) {
				// 失败次数清空
				this.hbFailedCounts = 0;

				JSONObject responseData = this.readContent(response.getContent());
				if (responseData.has(HttpHeartbeatHandler.Primitives)) {
					// 读取原语数组
					JSONArray primitives = responseData.getJSONArray(HttpHeartbeatHandler.Primitives);
					for (int i = 0, size = primitives.length(); i < size; ++i) {
						JSONObject primJSON = primitives.getJSONObject(i);
						// 进行对话处理
						this.doDialogue(primJSON);
					}
				}
			}
			else {
				// 记录失败
				++this.hbFailedCounts;
				Logger.w(HttpSpeaker.class, "Heartbeat failed");
			}
		} catch (InterruptedException | TimeoutException | ExecutionException e) {
			// 记录失败次数
			++this.hbFailedCounts;
		} catch (JSONException e) {
			Logger.log(getClass(), e, LogLevel.ERROR);
		}

		// hbMaxFailed 次失败之后通知关闭
		if (this.hbFailedCounts >= this.hbMaxFailed) {
			this.hangUp();
		}
	}

	/**
	 * 发送 Check 数据。
	 * 
	 * @param ciphertext 指定服务器发来的密文。
	 * @param key 指定密钥。
	 */
	private void requestCheck(byte[] ciphertext, byte[] key) {
		if (null == this.address) {
			return;
		}

		// 解密
		byte[] plaintext = Cryptology.getInstance().simpleDecrypt(ciphertext, key);

		JSONObject data = new JSONObject();
		try {
			// 明文
			data.put(HttpCheckHandler.Plaintext, new String(plaintext, Charset.forName("UTF-8")));
			// 自 Tag
			data.put(HttpCheckHandler.Tag, Nucleus.getInstance().getTagAsString());
		} catch (JSONException e) {
			Logger.log(HttpSpeaker.class, e, LogLevel.ERROR);
			return;
		}

		// 拼装 URL
		StringBuilder url = new StringBuilder("http://");
		url.append(this.address.getHostString()).append(":").append(this.address.getPort());
		url.append(URI_CHECK);

		StringContentProvider content = new StringContentProvider(data.toString(), "UTF-8");
		try {
			// 发送请求
			ContentResponse response = this.client.newRequest(url.toString())
											.method(HttpMethod.POST)
											.header(HttpHeader.COOKIE, this.cookie)
											.content(content)
											.send();
			if (response.getStatus() == HttpResponse.SC_OK) {
				// 获取数据
				JSONObject responseData = this.readContent(response.getContent());
				if (null != responseData) {
					this.remoteTag = responseData.getString(HttpCheckHandler.Tag);

					// 尝试请求 Cellet
					this.executor.execute(new Runnable() {
						@Override
						public void run() {
							requestCellets();
						}
					});
				}
				else {
					Logger.e(HttpSpeaker.class, "Can not get tag data from check action");
				}
			}
			else {
				Logger.e(HttpSpeaker.class, "Request check failed: " + response.getStatus());

				TalkServiceFailure failure = new TalkServiceFailure(TalkFailureCode.CALL_FAILED
						, this.getClass(), this.address.getHostString(), this.address.getPort());
				failure.setSourceCelletIdentifiers(this.identifierList);
				this.fireFailed(failure);
			}
		} catch (InterruptedException | TimeoutException | ExecutionException | JSONException e) {
			Logger.log(HttpSpeaker.class, e, LogLevel.ERROR);
		}

		url = null;
	}

	/**
	 * 发送请求 Cellet 数据。
	 */
	private void requestCellets() {
		// 拼装 URL
		StringBuilder url = new StringBuilder("http://");
		url.append(this.address.getHostString()).append(":").append(this.address.getPort());
		url.append(URI_REQUEST);

		for (String identifier : this.identifierList) {
			JSONObject data = new JSONObject();
			try {
				// 请求 identifier
				data.put(HttpRequestHandler.Identifier, identifier);
				// 源 Tag
				data.put(HttpRequestHandler.Tag, Nucleus.getInstance().getTagAsString());
			} catch (JSONException e) {
				Logger.log(getClass(), e, LogLevel.ERROR);
			}

			StringContentProvider content = new StringContentProvider(data.toString(), "UTF-8");
			try {
				// 发送请求
				ContentResponse response = this.client.newRequest(url.toString())
												.method(HttpMethod.POST)
												.header(HttpHeader.COOKIE, this.cookie)
												.content(content)
												.send();
				if (response.getStatus() == HttpResponse.SC_OK) {
					StringBuilder buf = new StringBuilder();
					buf.append("Cellet '");
					buf.append(identifier);
					buf.append("' has called at ");
					buf.append(this.address.getAddress().getHostAddress());
					buf.append(":");
					buf.append(this.address.getPort());
					Logger.i(HttpSpeaker.class, buf.toString());
					buf = null;

					// 变更状态
					this.state = SpeakerState.CALLED;

					// 回调事件
					this.fireContacted(identifier);
				}
				else {
					Logger.e(HttpSpeaker.class, "Request cellet failed: " + response.getStatus());
				}
			} catch (InterruptedException | TimeoutException | ExecutionException e) {
				Logger.log(getClass(), e, LogLevel.ERROR);
			}
		}
	}

	/**
	 * 执行会话处理。
	 * 
	 * @param data 指定接收到的 JSON 数据。
	 * @throws JSONException
	 */
	private void doDialogue(JSONObject data) throws JSONException {
		String identifier = data.getString(HttpDialogueHandler.Identifier);
		JSONObject primData = data.getJSONObject(HttpDialogueHandler.Primitive);
		// 解析原语
		Primitive primitive = new Primitive(this.remoteTag);
		primitive.setCelletIdentifier(identifier);
		PrimitiveSerializer.read(primitive, primData);

		this.fireDialogue(identifier, primitive);
	}

	/**
	 * 触发接收到对话数据回调。
	 * 
	 * @param celletIdentifier 指定触发的 Cellet 标识。
	 * @param primitive 指定接收到的原语数据。
	 */
	private void fireDialogue(String celletIdentifier, Primitive primitive) {
		this.delegate.onDialogue(this, celletIdentifier, primitive);
	}

	/**
	 * 触发接通服务回调。
	 * 
	 * @param celletIdentifier 指定接通服务的 Cellet 标识。
	 */
	private void fireContacted(String celletIdentifier) {
		this.delegate.onContacted(this, celletIdentifier);
	}

	/**
	 * 触发退出服务回调。
	 * 
	 * @param celletIdentifier 指定退出服务的 Cellet 标识。
	 */
	private void fireQuitted(String celletIdentifier) {
		this.delegate.onQuitted(this, celletIdentifier);
	}

	/**
	 * 触发发生错误回调。
	 * 
	 * @param failure 指定会话服务错误。
	 */
	private void fireFailed(TalkServiceFailure failure) {
		this.delegate.onFailed(this, failure);
	}

	/**
	 * 读取 HTTP 返回的数据内容。
	 * 
	 * @param content 指定待读取分析的数据数组。
	 * @return 返回数据的 JSON 形式。
	 * @throws JSONException
	 */
	private JSONObject readContent(byte[] content) throws JSONException {
		JSONObject ret = null;

		try {
			ret = new JSONObject(new String(content, Charset.forName("UTF-8")));
		} catch (JSONException e) {
			throw e;
		}

		return ret;
	}

}
