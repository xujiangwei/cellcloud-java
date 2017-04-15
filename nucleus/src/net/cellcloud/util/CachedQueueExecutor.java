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

package net.cellcloud.util;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 可缓存的队列执行器。
 * 
 * @author Ambrose Xu
 *
 */
public final class CachedQueueExecutor implements ExecutorService {

	private ExecutorService executor;
	private int maxThreads = 8;
	private AtomicInteger numThreads = new AtomicInteger(0);

	private ConcurrentLinkedQueue<Runnable> queue;

	/**
	 * 构造函数。
	 * 
	 * @param maxThreads 指定最大并发线程数量。
	 */
	private CachedQueueExecutor(int maxThreads) {
		this.executor = Executors.newCachedThreadPool();
		this.maxThreads = maxThreads;
		this.queue = new ConcurrentLinkedQueue<Runnable>();
	}

	/**
	 * 创建可缓存队列执行器。
	 * 
	 * @param maxThreads 指定最大并发线程数量。
	 * @return
	 */
	public static CachedQueueExecutor newCachedQueueThreadPool(int maxThreads) {
		if (maxThreads <= 0) {
			throw new IllegalArgumentException("Max threads is not less than zero.");
		}

		return new CachedQueueExecutor(maxThreads);
	}

	@Override
	public void execute(Runnable command) {
		this.queue.offer(command);

		if (this.numThreads.get() < this.maxThreads) {
			// 线程计数
			this.numThreads.incrementAndGet();

			this.executor.execute(new QueueTask());
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
		this.queue.clear();
		this.executor.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		this.queue.clear();
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
	 * 执行队列任务。
	 */
	protected final class QueueTask implements Runnable {
		protected QueueTask() {
		}

		@Override
		public void run() {
			do {
				Runnable task = queue.poll();
				if (null != task) {
					task.run();
				}
			} while (!queue.isEmpty());

			numThreads.decrementAndGet();
		}
	}

}
