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

package net.cellcloud.talk;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.cellcloud.core.Cellet;
import net.cellcloud.talk.stuff.StuffVersion;

/**
 * Talk 追踪器。
 * 
 * @author Ambrose Xu
 * 
 */
public final class TalkTracker {

	/** 当前对话的能力描述。 */
	private TalkCapacity capacity = null;

	/** 当前对话请求的 Cellet 清单。 */
	private LinkedList<Cellet> cellets = null;

	/** 当前 Session 使用的语素版本。 */
	protected StuffVersion stuffVersion = null;

	/**
	 * 构造函数。
	 */
	protected TalkTracker() {
		this.cellets = new LinkedList<Cellet>();
		this.capacity = new TalkCapacity();
	}

	/**
	 * 设置新的能力描述。
	 * 
	 * @param capacity 指定新的 {@link TalkCapacity} 对象。
	 */
	public void setCapacity(TalkCapacity capacity) {
		this.capacity = capacity;
	}

	/**
	 * 获得当前能力描述。
	 * 
	 * @return 返回当前能力描述对象 {@link TalkCapacity} 。
	 */
	public TalkCapacity getCapacity() {
		return this.capacity;
	}

	/**
	 * 添加 Cellet 。
	 * 
	 * @param cellet 待添加 Cellet 实例。
	 */
	protected void addCellet(Cellet cellet) {
		synchronized (this.cellets) {
			if (this.cellets.contains(cellet)) {
				return;
			}

			this.cellets.add(cellet);
		}
	}

	/**
	 * 移除 Cellet 。
	 * 
	 * @param cellet 待移除 Cellet 实例。
	 */
	protected void removeCellet(Cellet cellet) {
		synchronized (this.cellets) {
			this.cellets.remove(cellet);
		}
	}

	/**
	 * 获得指定标识的 Cellet 实例。
	 * 
	 * @param identifier 指定 Cellet 标识。
	 * @return 如果没有找到指定的 Cellet 返回 <code>null</code> 值。
	 */
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

	/**
	 * 当前追中器里是否包含了指定的 Cellet 。
	 * 
	 * @param cellet 待判断的 Cellet 实例。
	 * @return 如果包含指定的 Cellet 返回 <code>true</code> 。
	 */
	protected boolean hasCellet(Cellet cellet) {
		synchronized (this.cellets) {
			return this.cellets.contains(cellet);
		}
	}

	/**
	 * 当前追中器里是否包含了指定的 Cellet 。
	 * 
	 * @param identifier 待判断的 Cellet 标识。
	 * @return 如果包含指定的 Cellet 返回 <code>true</code> 。
	 */
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

	/**
	 * 获得包含的 Cellet 清单。
	 * 
	 * @return 返回包含的 Cellet 清单。
	 */
	protected List<Cellet> getCelletList() {
		ArrayList<Cellet> list = new ArrayList<Cellet>();
		synchronized (this.cellets) {
			list.addAll(this.cellets);
		}
		return list;
	}

}
