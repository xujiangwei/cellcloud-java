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

package net.cellcloud.talk.dialect;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 动作方言工厂。
 * 
 * @author Jiangwei Xu
 */
public final class ActionDialectFactory extends DialectFactory {

	private DialectMetaData metaData;

	private ExecutorService executor;
	private int maxThreadCount;
	private int threadCount;
	private LinkedList<ActionDialect> dialects;
	private LinkedList<ActionDelegate> delegates;

	public ActionDialectFactory() {
		this.metaData = new DialectMetaData(ActionDialect.DIALECT_NAME, "Action Dialect");
		this.maxThreadCount = 16;
		this.threadCount = 0;
		this.dialects = new LinkedList<ActionDialect>();
		this.delegates = new LinkedList<ActionDelegate>();
	}

	@Override
	public DialectMetaData getMetaData() {
		return this.metaData;
	}

	@Override
	public Dialect create(String tracker) {
		return new ActionDialect(tracker);
	}

	/** 执行动作。
	 */
	protected void doAction(final ActionDialect dialect, final ActionDelegate delegate) {
		if (null == this.executor) {
			this.executor = Executors.newCachedThreadPool();
		}

		synchronized (this.metaData) {
			this.dialects.add(dialect);
			this.delegates.add(delegate);
		}

		if (this.threadCount < this.maxThreadCount) {
			// 线程数量未达到最大线程数，启动新线程

			this.executor.execute(new Runnable() {
				@Override
				public void run() {
					++threadCount;

					while (!dialects.isEmpty()) {
						ActionDelegate adg = null;
						ActionDialect adl = null;
						synchronized (metaData) {
							if (dialects.isEmpty()) {
								break;
							}
							adg = delegates.remove(0);
							adl = dialects.remove(0);
						}

						// Do action
						if (null != adg) {
							adg.doAction(adl);
						}
					}

					--threadCount;
				}
			});
		}
	}

	public void shutdown() {
		synchronized (this.metaData) {
			this.dialects.clear();
			this.delegates.clear();
		}

		if (null != this.executor) {
			this.executor.shutdown();
			this.executor = null;
		}
	}
}
