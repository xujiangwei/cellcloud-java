/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (cellcloudproject@gmail.com)

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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 动作方言工厂。
 * 
 * @author Jiangwei Xu
 */
public final class ActionDialectFactory extends DialectFactory {

	private DialectMetaData metaData;

	private ExecutorService executor;

	public ActionDialectFactory() {
		this.metaData = new DialectMetaData(ActionDialect.DIALECT_NAME, "Action Dialect");
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

		this.executor.execute(new Runnable() {
			@Override
			public void run() {
				delegate.doAction(dialect);
			}
		});
	}

	public void shutdown() {
		if (null != this.executor) {
			this.executor.shutdown();
		}
	}
}
