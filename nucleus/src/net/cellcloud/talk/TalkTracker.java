/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (www.cellcloud.net)

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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.cellcloud.core.Cellet;

/** Talk 追踪器。
 * 
 * @author Jiangwei Xu
 */
public final class TalkTracker {

	private TalkCapacity capacity = null;
	private LinkedList<Cellet> cellets = null;

	protected TalkTracker() {
		this.cellets = new LinkedList<Cellet>();
		this.capacity = new TalkCapacity();
	}

	public void setCapacity(TalkCapacity capacity) {
		this.capacity = capacity;
	}

	public TalkCapacity getCapacity() {
		return this.capacity;
	}

	protected void addCellet(Cellet cellet) {
		synchronized (this.cellets) {
			if (this.cellets.contains(cellet)) {
				return;
			}

			this.cellets.add(cellet);
		}
	}

	protected void removeCellet(Cellet cellet) {
		synchronized (this.cellets) {
			this.cellets.remove(cellet);
		}
	}

	public Cellet getCellet(String identifier) {
		synchronized (this.cellets) {
			for (Cellet cellet : this.cellets) {
				if (cellet.getFeature().getIdentifier().equals(identifier)) {
					return cellet;
				}
			}
		}
		return null;
	}

	protected boolean hasCellet(Cellet cellet) {
		synchronized (this.cellets) {
			return this.cellets.contains(cellet);
		}
	}
	protected boolean hasCellet(String identifier) {
		synchronized (this.cellets) {
			for (Cellet cellet : this.cellets) {
				if (cellet.getFeature().getIdentifier().equals(identifier)) {
					return true;
				}
			}
		}
		return false;
	}

	protected List<Cellet> getCelletList() {
		ArrayList<Cellet> list = new ArrayList<Cellet>();
		synchronized (this.cellets) {
			list.addAll(this.cellets);
		}
		return list;
	}
}
