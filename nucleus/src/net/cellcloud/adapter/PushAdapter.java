/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2015 Cell Cloud Team (www.cellcloud.net)

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

import net.cellcloud.common.Logger;
import net.cellcloud.core.Endpoint;

public class PushAdapter extends RelationNucleusAdapter {

	public final static String Name = "PushAdapter";

	private LinkedList<PushAdapterListener> listeners;

	public PushAdapter() {
		super(PushAdapter.Name);
		this.listeners = new LinkedList<PushAdapterListener>();
	}

	@Override
	protected void onReady() {
		Logger.d(this.getClass(), "Push adapter is ready.");
	}

	@Override
	protected void onReceive(Endpoint endpoint, Gene gene) {
		for (PushAdapterListener l : this.listeners) {
			l.onEvent(this, new PushEvent(endpoint, gene));
		}
	}

	/**
	 * 推送事件。
	 * 
	 * @param event
	 */
	public void pushEvent(PushEvent event) {
		if (null == event.destination) {
			super.broadcast(event.toGene());
		}
		else {
			super.transport(event.destination, event.toGene());
		}
	}

	public void addListener(PushAdapterListener listener) {
		synchronized (this.listeners) {
			this.listeners.add(listener);
		}
	}

	public void removeListener(PushAdapterListener listener) {
		synchronized (this.listeners) {
			this.listeners.remove(listener);
		}
	}
}
