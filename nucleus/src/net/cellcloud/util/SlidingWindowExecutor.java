/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2013 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/** 滑动窗执行器。
 * 
 * @author Jiangwei Xu
 */
public class SlidingWindowExecutor implements ExecutorService {

	protected ExecutorService executor;

	// 当前线程数快照
	protected AtomicInteger threadNum = new AtomicInteger(0);

	/// 任务队列
	protected Queue<Runnable> taskQueue;
	/// 选中执行的任务队列
	protected Queue<Runnable> activeQueue;

	// 活动窗属性
	private int windowSize = 4;
	protected byte[] monitor = new byte[0];

	private volatile boolean dispatching = false;

	protected SlidingWindowExecutor self;

	private SlidingWindowExecutor(ExecutorService executor, int windowSize) {
		this.self = this;
		this.executor = executor;
		this.windowSize = windowSize;
		this.taskQueue = new LinkedList<Runnable>();
		this.activeQueue = new LinkedList<Runnable>();
	}

	/**
	 * 创建指定窗口大小的活动滑窗线程池。
	 * @param windowSize
	 * @param maxThreadNum
	 * @return
	 */
	public static SlidingWindowExecutor newSlidingWindowThreadPool(int windowSize) {
		if (windowSize <= 0) {
			throw new IllegalArgumentException("Window size is not less than zero.");
		}

		return new SlidingWindowExecutor(Executors.newCachedThreadPool(), windowSize);
	}

	@Override
	public void execute(Runnable command) {
		synchronized (this.taskQueue) {
			this.taskQueue.offer(command);
		}

		// 启动分发任务
		if (!this.dispatching) {
			this.dispatching = true;
			this.executor.execute(new Dispatcher());
		}
		else {
			synchronized (this.monitor) {
				this.monitor.notifyAll();
			}
		}
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		return this.executor.awaitTermination(timeout, unit);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
		return null;
	}

	@Override
	public <T> List<Future<T>> invokeAll(
			Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		return null;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		return null;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout,
			TimeUnit unit) throws InterruptedException, ExecutionException,
			TimeoutException {
		return null;
	}

	@Override
	public boolean isShutdown() {
		return this.executor.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return this.executor.isTerminated();
	}

	@Override
	public void shutdown() {
		this.taskQueue.clear();

		synchronized (this.monitor) {
			this.monitor.notifyAll();
		}

		this.executor.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		this.taskQueue.clear();

		synchronized (this.monitor) {
			this.monitor.notifyAll();
		}

		return this.executor.shutdownNow();
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return null;
	}

	@Override
	public Future<?> submit(Runnable task) {
		return null;
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return null;
	}

	/**
	 * 快照线程数量。
	 * @return
	 */
	public int snapshootThreadNum() {
		return this.threadNum.get();
	}

	/**
	 * 内部线程任务。
	 */
	protected final class Dispatcher implements Runnable {
		protected Dispatcher() {
		}

		@Override
		public void run() {
			do {
				// 将任务添加到活跃队列
				while (activeQueue.size() < windowSize
						&& threadNum.get() < windowSize
						&& !taskQueue.isEmpty()) {
					Runnable task = null;
					synchronized (taskQueue) {
						task = taskQueue.poll();
					}
					if (null != task) {
						synchronized (activeQueue) {
							activeQueue.offer(task);
						}
					}
				}

				// 启动线程执行活跃任务
				while (!activeQueue.isEmpty()) {
					executor.execute(new SlidingWindowTask(self, activeQueue.poll()));
				}

				if (!taskQueue.isEmpty()) {
					synchronized (monitor) {
						try {
							monitor.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			} while (!taskQueue.isEmpty());

			dispatching = false;
		}
	}
}
