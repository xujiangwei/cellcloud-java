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

package net.cellcloud.common;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** 配置计算器。
 * 
 * @author Ambrose Xu
 *
 */
public class QuotaCalculator {

	private int quota = 102400;

	private AtomicInteger runtime;

	private ScheduledExecutorService scheduledExecutor;
	private ScheduledFuture<?> future;

	private volatile long lastBlockingSize;
	private volatile long lastBlockingTime;

	public QuotaCalculator(ScheduledExecutorService scheduledExecutor, int quotaInSecond) {
		this.scheduledExecutor = scheduledExecutor;
		this.quota = quotaInSecond;
		this.runtime = new AtomicInteger(quotaInSecond);
		this.lastBlockingSize = 0;
		this.lastBlockingTime = 0;
	}

	public void setQuota(int quotaInSecond) {
		this.quota = quotaInSecond;
	}

	public int getQuota() {
		return this.quota;
	}

	public long getLastBlockingSize() {
		return this.lastBlockingSize;
	}

	public long getLastBlockingTime() {
		return this.lastBlockingTime;
	}

	public void start() {
		this.future = this.scheduledExecutor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				if (runtime.get() < 0) {
					runtime.set(quota + runtime.get());
				}
				else {
					runtime.set(quota);
				}

				synchronized (runtime) {
					runtime.notifyAll();
				}
			}
		}, 1L, 1L, TimeUnit.SECONDS);
	}

	public void stop() {
		if (null != this.future) {
			this.future.cancel(false);
			this.future = null;
		}

		synchronized (this.runtime) {
			this.runtime.notifyAll();
		}
	}

	public void consumeBlocking(int value, QuotaCalculatorCallback callback, Object custom) {
		if (null == this.future) {
			return;
		}

		if (this.runtime.get() <= 0) {
//			Logger.d(this.getClass(), "Out of quota: " + this.quota);

			this.lastBlockingTime = System.currentTimeMillis();

			this.lastBlockingSize += value;

			if (null != callback) {
				callback.onCallback(value, custom);
			}

			synchronized (this.runtime) {
				try {
					this.runtime.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			this.lastBlockingSize -= value;
		}

		this.runtime.set(this.runtime.get() - value);
	}

}
