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

package net.cellcloud.adapter.smart;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import net.cellcloud.adapter.AdapterListener;
import net.cellcloud.adapter.RelationNucleusAdapter;
import net.cellcloud.adapter.gene.Gene;
import net.cellcloud.adapter.gene.GeneHeader;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.core.Endpoint;
import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.dialect.Dialect;
import net.cellcloud.talk.stuff.PrimitiveSerializer;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 使用回馈机制的自动适配器。
 * 
 * @author Ambrose Xu
 *
 */
public class SmartAdapter extends RelationNucleusAdapter {

	/**
	 * 适配器名。
	 */
	public final static String Name = "SmartAdapter";

	/**
	 * Gene 负载类型定义。
	 */
	private final static String PT_FEEDBACK = "Feedback";

	/**
	 * 回馈控制器。
	 */
	private FeedbackController controller;

	/**
	 * 关键字对应的可用终端列表。
	 */
	private ConcurrentHashMap<String, ArrayList<Endpoint>> runtimeList;

	/**
	 * 运行时列表最大长度。
	 */
	private final int maxListSize = 10000;

	/**
	 * 构造器。
	 * 
	 * @param instanceName 指定该适配器的实例名。
	 */
	public SmartAdapter(String instanceName) {
		super(SmartAdapter.Name, instanceName);
		this.controller = new FeedbackController();
		this.runtimeList = new ConcurrentHashMap<String, ArrayList<Endpoint>>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void config(Map<String, Object> parameters) {
		super.config(parameters);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStart() {
		this.controller.start();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStop() {
		this.controller.stop();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onReady() {
		Logger.i(this.getClass(), "Smart adapter '" + this.getName() + "' (" + this.getPort() + ") is ready.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onSend(Endpoint endpoint, Gene gene) {
		// Nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onReceive(Endpoint endpoint, Gene gene) {
		String pt = gene.getHeader(GeneHeader.PayloadType);
		if (null != pt && pt.equals(PT_FEEDBACK)) {
			// 处理
			this.processFeedback(endpoint, gene);
			return;
		}

		String payload = gene.getPayload();

		JSONObject json = null;
		try {
			json = new JSONObject(payload);

			Primitive primitive = new Primitive();
			PrimitiveSerializer.read(primitive, json);

			// 执行回调
			this.fireShared(endpoint, primitive.getDialect());
		} catch (JSONException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		} catch (Exception e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onTransportFailure(Endpoint endpoint, Gene gene) {
		ArrayList<String> keywords = new ArrayList<String>();
		Iterator<Entry<String, ArrayList<Endpoint>>> iter = this.runtimeList.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, ArrayList<Endpoint>> e = iter.next();
			ArrayList<Endpoint> list = e.getValue();
			synchronized (list) {
				if (list.remove(endpoint)) {
					keywords.add(e.getKey());
				}
			}
		}

		this.fireEndpointFailure(endpoint, keywords);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void share(String keyword, Dialect dialect) {
		JSONObject payload = new JSONObject();
		try {
			PrimitiveSerializer.write(payload, dialect.translate());
		} catch (JSONException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
			return;
		}

		Gene gene = new Gene(keyword);
		gene.setPayload(payload.toString());

		ArrayList<Endpoint> list = this.runtimeList.get(keyword);
		if (null == list) {
			// 对应的关键字没有建立回馈信息，所以进行广播
			super.broadcast(gene);
		}
		else {
			synchronized (list) {
				if (!list.isEmpty()) {
					// 建立了回馈信息
					super.transport(list, gene);
				}
				else {
					Logger.d(this.getClass(), "No endpoints to share dialect: " + keyword);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void share(String keyword, Endpoint endpoint, Dialect dialect) {
		JSONObject payload = new JSONObject();
		try {
			PrimitiveSerializer.write(payload, dialect.translate());
		} catch (JSONException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
			return;
		}

		Gene gene = new Gene(keyword);
		gene.setPayload(payload.toString());

		super.transport(endpoint, gene);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void encourage(String keyword, Endpoint endpoint) {
		if (this.controller.isInhibitiveEncourage(keyword, endpoint)) {
			// 抑制鼓励数据发送
			return;
		}

		Gene gene = new Gene(keyword);
		gene.setHeader(GeneHeader.PayloadType, PT_FEEDBACK);

		JSONObject json = new JSONObject();
		try {
			json.put("feedback", 1);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		gene.setPayload(json.toString());

		this.transport(endpoint, gene);

		// 记录
		this.controller.recordEncourage(keyword, endpoint);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void encourage(String keyword) {
		Gene gene = new Gene(keyword);
		gene.setHeader(GeneHeader.PayloadType, PT_FEEDBACK);

		JSONObject json = new JSONObject();
		try {
			json.put("feedback", 1);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		gene.setPayload(json.toString());

		List<Endpoint> list = this.endpointList();
		synchronized (list) {
			for (Endpoint endpoint : list) {
				// 记录
				this.controller.recordEncourage(keyword, endpoint);
			}
		}

		// 广播
		this.broadcast(gene);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void discourage(String keyword, Endpoint endpoint) {
		if (this.controller.isInhibitiveDiscourage(keyword, endpoint)) {
			// 抑制气馁数据发送
			return;
		}

		Gene gene = new Gene(keyword);
		gene.setHeader(GeneHeader.PayloadType, PT_FEEDBACK);

		JSONObject json = new JSONObject();
		try {
			json.put("feedback", -1);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		gene.setPayload(json.toString());

		this.transport(endpoint, gene);

		// 记录
		this.controller.recordDiscourage(keyword, endpoint);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void discourage(String keyword) {
		Gene gene = new Gene(keyword);
		gene.setHeader(GeneHeader.PayloadType, PT_FEEDBACK);

		JSONObject json = new JSONObject();
		try {
			json.put("feedback", -1);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		gene.setPayload(json.toString());

		List<Endpoint> list = this.endpointList();
		synchronized (list) {
			for (Endpoint endpoint : list) {
				// 记录
				this.controller.recordDiscourage(keyword, endpoint);
			}
		}

		// 广播
		this.broadcast(gene);
	}

	/**
	 * 处理接收到的回馈数据。
	 * 
	 * @param endpoint 发送该数据的终端。
	 * @param gene 数据实例。
	 */
	private void processFeedback(Endpoint endpoint, Gene gene) {
		Logger.d(this.getClass(), "Process feedback : " + endpoint.toString());

		String str = gene.getPayload();
		JSONObject payload = null;
		int feedback = 0;
		try {
			payload = new JSONObject(str);
			feedback = payload.getInt("feedback");
		} catch (JSONException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
			return;
		}

		String keyword = gene.getName();

		if (feedback > 0) {
			// 正回馈
			this.controller.updateEncourage(keyword, endpoint);
		}
		else if (feedback < 0) {
			// 负回馈
			this.controller.updateDiscourage(keyword, endpoint);
		}
		else {
			Logger.d(this.getClass(), "NO feedback info");
			return;
		}

		// 获取当前连接的终端
		List<Endpoint> epList = super.endpointList();

		// 更新实时路由表
		ArrayList<Endpoint> list = this.runtimeList.get(keyword);
		if (null == list) {
			list = new ArrayList<Endpoint>(epList.size());
			list.addAll(epList);
			this.runtimeList.put(keyword, list);

			if (this.runtimeList.size() > this.maxListSize) {
				// 删除过期 keyword
				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						Iterator<Entry<String, ArrayList<Endpoint>>> iter = runtimeList.entrySet().iterator();
						while (iter.hasNext()) {
							Entry<String, ArrayList<Endpoint>> e = iter.next();
							if (!controller.hasFeedback(e.getKey())) {
								iter.remove();
							}
						}
					}
				});
				thread.start();
			}
		}
		synchronized (list) {
			if (feedback < 0) {
				// 删除负回馈终端
				list.remove(endpoint);
			}
			else {
				// 正回馈处理
				if (!list.contains(endpoint)) {
					list.add(endpoint);
				}
			}
		}
	}

	/**
	 * 触发 onShared 回调。
	 * 
	 * @param endpoint 事件发生的终端。
	 * @param dialect 方言数据。
	 */
	private void fireShared(Endpoint endpoint, Dialect dialect) {
		for (int i = 0; i < this.listeners.size(); ++i) {
			AdapterListener l = this.listeners.get(i);
			l.onShared(this, endpoint, dialect);
		}
	}

	/**
	 * 触发 onEndpointFailure 回调。
	 * 
	 * @param endpoint 事件发生的终端。
	 * @param faultKeywords 受此影响的关键字列表。
	 */
	private void fireEndpointFailure(Endpoint endpoint, List<String> faultKeywords)  {
		for (int i = 0; i < this.listeners.size(); ++i) {
			AdapterListener l = this.listeners.get(i);
			l.onEndpointFailure(this, endpoint, faultKeywords);
		}
	}

}
