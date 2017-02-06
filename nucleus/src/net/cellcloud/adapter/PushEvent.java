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

import net.cellcloud.core.Endpoint;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 推送事件。
 * 
 * @author Ambrose Xu
 */
public class PushEvent {

	protected String name;
	protected JSONObject payload;
	protected Endpoint destination;
	protected Endpoint source;

	public PushEvent(String name, JSONObject payload) {
		this.name = name;
		this.payload = payload;
	}

	public PushEvent(Endpoint destination, String name, JSONObject payload) {
		this.destination = destination;
		this.name = name;
		this.payload = payload;
	}

	protected PushEvent(Endpoint source, Gene gene) {
		this.source = source;
		this.name = gene.getName();
		String type = gene.getHeader("ContentType");
		if (type.equalsIgnoreCase("json")) {
			try {
				this.payload = new JSONObject(gene.getBody());
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	public String getName() {
		return this.name;
	}

	public JSONObject getPayload() {
		return this.payload;
	}

	public Endpoint getSource() {
		return this.source;
	}

	protected Gene toGene() {
		Gene gene = new Gene(this.name);
		gene.setHeader("ContentType", "json");
		gene.setBody(this.payload.toString());
		return gene;
	}

}
