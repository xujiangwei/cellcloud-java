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

	/** 标准配额数值。 */
	private volatile int quota = 102400;

	/** 运行时配额计数。 */
	private AtomicInteger runtime;

	/** 任务执行器。 */
	private ScheduledExecutorService scheduledExecutor;
	/** 当前任务的 Furture 。 */
	private ScheduledFuture<?> future;

	/** 上一次拥塞大小。 */
	private volatile long lastBlockingSize;
	/** 上一次拥塞的时间戳。 */
	private volatile long lastBlockingTime;

	/**
	 * 构造函数。
	 * 
	 * @param scheduledExecutor 任务执行器。
	 * @param quotaInSecond 以秒为计算周期的配额。
	 */
	public QuotaCalculator(ScheduledExecutorService scheduledExecutor, int quotaInSecond) {
		this.scheduledExecutor = scheduledExecutor;
		this.quota = quotaInSecond;
		this.runtime = new AtomicInteger(quotaInSecond);
		this.lastBlockingSize = 0;
		this.lastBlockingTime = 0;
	}

	/**
	 * 设置配额值。
	 * 
	 * @param quotaInSecond 以秒为计算周期的配额。
	 */
	public void setQuota(int quotaInSecond) {
		this.quota = quotaInSecond;
	}

	/**
	 * 获得配额值。
	 * 
	 * @return 返回以秒为计算周期的配额。
	 */
	public int getQuota() {
		return this.quota;
	}

	/**
	 * 获得最近一次阻塞的数据大小。
	 * 
	 * @return 返回最近一次阻塞的数据大小。
	 */
	public long getLastBlockingSize() {
		return this.lastBlockingSize;
	}

	/**
	 * 获得最近一次阻塞时的时间戳。
	 * 
	 * @return 返回最近一次阻塞时的时间戳。
	 */
	public long getLastBlockingTime() {
		return this.lastBlockingTime;
	}

	/**
	 * 启动计算器。
	 */
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

	/**
	 * 停止计算器。
	 */
	public void stop() {
		if (null != this.future) {
			this.future.cancel(false);
			this.future = null;
		}

		synchronized (this.runtime) {
			this.runtime.notifyAll();
		}
	}

	/**
	 * 消耗配额。
	 * 
	 * @param value 指定消耗值。
	 * @param callback 指定发生阻塞时的回调。
	 * @param custom 指定回调时的自定义数据对象。
	 */
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
