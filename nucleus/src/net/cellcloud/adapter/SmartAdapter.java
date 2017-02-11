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

package net.cellcloud.adapter;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 * 
 * @author Ambrose Xu
 */
public class SmartAdapter extends RelationNucleusAdapter {

	public final static String Name = "SmartAdapter";

	private final static String PT_FEEDBACK = "Feedback";

	private FeedbackController controller;

	private ConcurrentHashMap<Endpoint, LinkedList<Gene>> sendList;

	public SmartAdapter(String instanceName) {
		super(SmartAdapter.Name, instanceName);
		this.controller = new FeedbackController();
		this.sendList = new ConcurrentHashMap<Endpoint, LinkedList<Gene>>();
	}

	@Override
	public void config(Map<String, Object> parameters) {
		super.config(parameters);
	}

	@Override
	protected void onReady() {
		Logger.i(this.getClass(), "Smart adapter (" + this.getPort() + ") is ready.");
	}

	@Override
	protected void onSend(Endpoint endpoint, Gene gene) {
	}

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
			super.fireShared(endpoint, primitive.getDialect());
		} catch (JSONException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		}
	}

	@Override
	public void share(String keyword, Dialect dialect) {
		JSONObject payload = new JSONObject();
		try {
			PrimitiveSerializer.write(payload, dialect.translate());
		} catch (JSONException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
			return;
		}

		Gene gene = new Gene(keyword);
		gene.setPayload(payload.toString());

		super.broadcast(gene);
	}

	@Override
	public void share(String keyword, Endpoint endpoint, Dialect dialect) {
		
	}

	@Override
	public void encourage(String keyword, Endpoint endpoint) {
		Gene gene = new Gene(keyword);
		gene.setHeader(GeneHeader.PayloadType, PT_FEEDBACK);
		this.transport(endpoint, gene);
	}

	@Override
	public void discourage(String keyword, Endpoint endpoint) {
		
	}

	private void processFeedback(Endpoint endpoint, Gene gene) {
		
	}

}
