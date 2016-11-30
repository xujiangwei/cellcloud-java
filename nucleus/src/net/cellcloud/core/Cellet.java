/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2014 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.core;

import java.util.Set;

import net.cellcloud.adapter.RelationNucleusAdapter;
import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.TalkService;
import net.cellcloud.talk.dialect.Dialect;

/** Cellet 管理单元。
 * 
 * @author Jiangwei Xu
 */
public abstract class Cellet extends AbstractCellet {

	private CelletFeature feature;
	private CelletSandbox sandbox;

	/**
	 * 构造函数。
	 */
	public Cellet() {
		super();
	}

	/** 构造函数。
	 */
	public Cellet(CelletFeature feature) {
		super();
		this.feature = feature;
		this.sandbox = new CelletSandbox(feature);
	}

	/** 返回 Cellet 的特性描述。
	 */
	public CelletFeature getFeature() {
		return this.feature;
	}

	/**
	 * 设置 Cellet 特性描述。
	 * @param feature
	 */
	public synchronized void setFeature(CelletFeature feature) {
		if (null == this.feature) {
			this.feature = feature;
			this.sandbox = new CelletSandbox(feature);
		}
	}

	/** 发送原语到消费端进行会话。
	 */
	public boolean talk(final String targetTag, final Primitive primitive) {
		return TalkService.getInstance().notice(targetTag, primitive, this, this.sandbox);
	}
	/** 发送方言到消费端进行会话。
	 */
	public boolean talk(final String targetTag, final Dialect dialect) {
		return TalkService.getInstance().notice(targetTag, dialect, this, this.sandbox);
	}

	/**
	 * 关闭消费端会话。
	 * 
	 * @param targetTag
	 * @return
	 */
	public boolean hangUp(final String targetTag) {
		return TalkService.getInstance().kick(targetTag, this, this.sandbox);
	}

	/**
	 * 返回服务器当前的会话者清单。
	 * @return
	 */
	protected Set<String> getTalkerList() {
		return TalkService.getInstance().getTalkerList();
	}

	/** 进行激活前准备。
	 */
	protected final void prepare() {
		Nucleus.getInstance().prepareCellet(this, this.sandbox);
	}

	/** 加载指定的适配器。
	 */
	public RelationNucleusAdapter loadRelationNucleusAdapter() {
		return null;
	}

	/** 卸载指定的适配器。
	 */
	public void unloadRelationNucleusAdapter() {
		
	}

	/**
	 * @copydoc AbstractCellet::dialogue(String,String)
	 */
	@Override
	public void dialogue(final String tag, final Primitive primitive) {
		// Nothing
	}

	/**
	 * @copydoc AbstractCellet::contacted(String)
	 */
	@Override
	public void contacted(final String tag) {
		// Nothing
	}

	/**
	 * @copydoc AbstractCellet::quitted(String)
	 */
	@Override
	public void quitted(final String tag) {
		// Nothing
	}
}
