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

package net.cellcloud.core;

import java.util.Set;

import net.cellcloud.adapter.Adapter;
import net.cellcloud.adapter.RelationNucleusAdapter;
import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.TalkService;
import net.cellcloud.talk.dialect.Dialect;

/**
 * Cellet 单元。
 * 
 * 每个 Cellet 是 Nucleus 管理的基础通信单元。
 * 
 * @author Ambrose Xu
 * 
 */
public abstract class Cellet extends AbstractCellet {

	/** 特性描述。 */
	private CelletFeature feature;
	/** 沙盒。 */
	private CelletSandbox sandbox;

	/**
	 * 构造函数。
	 */
	public Cellet() {
		super();
	}

	/**
	 * 构造函数。
	 * 
	 * @param feature 指定 Cellet 的特性。
	 */
	public Cellet(CelletFeature feature) {
		super();
		this.feature = feature;
		this.sandbox = new CelletSandbox(feature);
	}

	/**
	 * 获得 Cellet 的特性描述。
	 * 
	 * @return 返回 Cellet 的特性描述。
	 */
	public CelletFeature getFeature() {
		return this.feature;
	}

	/**
	 * 设置 Cellet 特性描述。
	 * 
	 * @param feature
	 */
	public void setFeature(CelletFeature feature) {
		if (null == this.feature) {
			this.feature = feature;
			this.sandbox = new CelletSandbox(feature);
		}
	}

	/**
	 * 发送原语到消费端进行会话。
	 * 
	 * @param targetTag 指定消费端标签。
	 * @param primitive 指定需发送的原语实例。
	 */
	public boolean talk(String targetTag, Primitive primitive) {
		return TalkService.getInstance().notice(targetTag, primitive, this, this.sandbox);
	}

	/**
	 * 发送方言到消费端进行会话。
	 * 
	 * @param targetTag 指定消费端标签。
	 * @param dialect 指定需发送的方言实例。
	 */
	public boolean talk(String targetTag, Dialect dialect) {
		return TalkService.getInstance().notice(targetTag, dialect, this, this.sandbox);
	}

	/**
	 * 关闭消费端会话。
	 * 
	 * @param targetTag
	 * @return
	 */
	public boolean hangUp(String targetTag) {
		return TalkService.getInstance().kick(targetTag, this, this.sandbox);
	}

	/**
	 * 在发送完指定的原语后关闭消费端会话。
	 * 
	 * @param targetTag
	 * @param primitive
	 * @return
	 */
	public boolean hangUpAfterTalk(String targetTag, Primitive primitive) {
		return TalkService.getInstance().kickAfterNotice(primitive, targetTag, this, this.sandbox);
	}

	/**
	 * 在发送完指定的方言后关闭消费端会话。
	 * 
	 * @param targetTag
	 * @param dialect
	 * @return
	 */
	public boolean hangUpAfterTalk(String targetTag, Dialect dialect) {
		return TalkService.getInstance().kickAfterNotice(dialect, targetTag, this, this.sandbox);
	}

	/**
	 * 获得当前与服务有连接的终端的 Tag 。
	 * 
	 * @return 返回当前与服务有连接的终端的 Tag 集合。
	 */
	protected Set<String> getEndpointTagList() {
		return TalkService.getInstance().getEndpointTagList();
	}

	/**
	 * 进行激活前准备。
	 */
	protected final void prepare() {
		Nucleus.getInstance().prepareCellet(this, this.sandbox);
	}

	/**
	 * 获得指定实例名的适配器。
	 * 
	 * @param instanceName 指定适配器实例名。
	 * @return 返回指定实例名的适配器实例。
	 */
	public RelationNucleusAdapter getAdapter(String instanceName) {
		Adapter adapter = Nucleus.getInstance().getAdapter(instanceName);
		if (null != adapter) {
			return (RelationNucleusAdapter) adapter;
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void dialogue(String tag, Primitive primitive) {
		// Nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void contacted(String tag) {
		// Nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void quitted(String tag) {
		// Nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void proxyContacted(String tag) {
		// Nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void proxyQuitted(String tag) {
		// Nothing
	}

	public void failed(String tag, int failure, Primitive primitive) {
		// Nothing
	}
}
