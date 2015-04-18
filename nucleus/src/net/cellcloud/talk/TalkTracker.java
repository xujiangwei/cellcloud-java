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
import java.util.List;
import java.util.Vector;

import net.cellcloud.core.Cellet;

/** Talk 追踪器。
 * 
 * @author Jiangwei Xu
 */
public final class TalkTracker {
	private boolean autoSuspend = false;
	private long suspendDuration = 5000;

	private Vector<Cellet> cellets = null;

	protected TalkTracker() {
		this.cellets = new Vector<Cellet>();
	}

	/** 返回是否进行自动挂起。
	 */
	public boolean isAutoSuspend() {
		return this.autoSuspend;
	}

	/** 设置是否支持自动挂起。
	 */
	protected void setAutoSuspend(boolean value) {
		this.autoSuspend = value;
	}

	/** 返回挂起时长。
	 */
	public long getSuspendDuration() {
		return this.suspendDuration;
	}

	/** 设置挂起时长。
	 */
	protected long setSuspendDuration(long duration) {
		if (duration < 5000)
			this.suspendDuration = 5000;
		else
			this.suspendDuration = duration;

		return this.suspendDuration;
	}

	protected void addCellet(Cellet cellet) {
		if (this.cellets.contains(cellet)) {
			return;
		}

		this.cellets.add(cellet);
	}

	protected void removeCellet(Cellet cellet) {
		this.cellets.remove(cellet);
	}

	protected Cellet getCellet(String identifier) {
		for (Cellet cellet : this.cellets) {
			if (cellet.getFeature().getIdentifier().equals(identifier)) {
				return cellet;
			}
		}
		return null;
	}

	protected boolean hasCellet(Cellet cellet) {
		return this.cellets.contains(cellet);
	}
	protected boolean hasCellet(String identifier) {
		for (Cellet cellet : this.cellets) {
			if (cellet.getFeature().getIdentifier().equals(identifier)) {
				return true;
			}
		}
		return false;
	}

	protected List<Cellet> getCelletList() {
		ArrayList<Cellet> list = new ArrayList<Cellet>();
		list.addAll(this.cellets);
		return list;
	}
}
