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

package net.cellcloud.talk.dialect;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.core.Cellet;

/**
 * 动作方言工厂。
 * 
 * @author Ambrose Xu
 * 
 */
public final class ActionDialectFactory extends DialectFactory {

	/**
	 * 方言元数据。
	 */
	private DialectMetaData metaData;

	/**
	 * 线程池执行器。
	 */
	private ExecutorService executor;

	/** 最大线程数。 */
	private int maxThreadNum;
	/** 当前活跃的线程计数。 */
	private AtomicInteger threadCount;
	/** 当前待处理方言列表。 */
	private LinkedList<ActionDialect> dialects;
	/** 当前待处理方言的委派列表。 */
	private LinkedList<ActionDelegate> delegates;

	/**
	 * 构造函数。
	 * 
	 * @param executor 指定线程池执行器。
	 */
	public ActionDialectFactory(ExecutorService executor) {
		this.metaData = new DialectMetaData(ActionDialect.DIALECT_NAME, "Action Dialect");
		this.executor = executor;
		this.maxThreadNum = 32;
		this.threadCount = new AtomicInteger(0);
		this.dialects = new LinkedList<ActionDialect>();
		this.delegates = new LinkedList<ActionDelegate>();
	}

	/**
	 * 获得当前活跃线程数量。
	 * 
	 * @return 返回当前正在工作的线程数量。
	 */
	public int getThreadCounts() {
		return this.threadCount.get();
	}

	/**
	 * 获得最大线程数量。
	 * 
	 * @return 返回工厂允许最大并发线程数量。
	 */
	public int getMaxThreadCounts() {
		return this.maxThreadNum;
	}

	/**
	 * 获得待处理的动作方言数量。
	 * 
	 * @return 返回待处理数量。
	 */
	public int getPendingNum() {
		synchronized (this.metaData) {
			return this.dialects.size();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DialectMetaData getMetaData() {
		return this.metaData;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Dialect create(String tracker) {
		return new ActionDialect(tracker);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdown() {
		synchronized (this.metaData) {
			this.dialects.clear();
			this.delegates.clear();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean onTalk(String identifier, Dialect dialect) {
		// 不截获
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean onDialogue(String identifier, Dialect dialect) {
		// 不截获
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean onTalk(Cellet cellet, String targetTag, Dialect dialect) {
		// 不截获
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean onDialogue(Cellet cellet, String sourceTag, Dialect dialect) {
		// 不截获
		return true;
	}

	/**
	 * 执行动作。
	 */
	protected void doAction(ActionDialect dialect, ActionDelegate delegate) {
		synchronized (this.metaData) {
			this.dialects.add(dialect);
			this.delegates.add(delegate);
		}

		if (this.threadCount.get() < this.maxThreadNum) {
			// 线程数量未达到最大线程数，启动新线程

			// 更新线程计数
			this.threadCount.incrementAndGet();

			this.executor.execute(new Runnable() {
				@Override
				public void run() {
					while (!dialects.isEmpty()) {
						ActionDelegate adg = null;
						ActionDialect adl = null;
						synchronized (metaData) {
							if (dialects.isEmpty()) {
								break;
							}
							adg = delegates.removeFirst();
							adl = dialects.removeFirst();
						}

						// Do action
						if (null != adg) {
							if (null != adl.getCustomContext() && Logger.isDebugLevel()) {
								Logger.d(this.getClass(), "Before do action: " + adl.getAction() + " -> " + adl.getCustomContext().toString());
							}

							try {
								adg.doAction(adl);
							} catch (Exception e) {
								Logger.log(this.getClass(), e, LogLevel.ERROR);
							}

							if (null != adl.getCustomContext() && Logger.isDebugLevel()) {
								Logger.d(this.getClass(), "After do action: " + adl.getAction() + " -> " + adl.getCustomContext().toString());
							}
						}
					}

					// 更新线程计数
					threadCount.decrementAndGet();
				}
			});
		}
	}

}
