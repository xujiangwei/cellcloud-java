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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class QuotaCalculator {

	private int quota = 10240;

	private AtomicInteger runtime;

	private Timer timer;

	private AtomicInteger blockingSize;

	public QuotaCalculator(int quotaInSecond) {
		this.quota = quotaInSecond;
		this.runtime = new AtomicInteger(quotaInSecond);
		this.blockingSize = new AtomicInteger(0);
	}

	public void setQuota(int quotaInSecond) {
		this.quota = quotaInSecond;
	}

	public int getQuota() {
		return this.quota;
	}

	public void start() {
		if (null == this.timer) {
			this.timer = new Timer("QuotaCalculator#Timer");
			this.timer.schedule(new TimerTask() {
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
			}, 1000L, 1000L);
		}
	}

	public void stop() {
		if (null != this.timer) {
			this.timer.cancel();
			this.timer.purge();
			this.timer = null;
		}

		synchronized (this.runtime) {
			this.runtime.notifyAll();
		}
	}

	public void consumeBlocking(int value, QuotaCalculatorCallback callback, Object custom) {
		if (null == this.timer) {
			return;
		}

		if (this.runtime.get() <= 0) {
//			Logger.d(this.getClass(), "Out of quota: " + this.quota);

			this.blockingSize.addAndGet(value);

			callback.onCallback(value, custom);

			synchronized (this.runtime) {
				try {
					this.runtime.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			this.blockingSize.set(this.blockingSize.get() - value);
		}

		this.runtime.set(this.runtime.get() - value);
	}

	public int getBlockingSize() {
		return this.blockingSize.get();
	}

}
