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

package net.cellcloud.util.nio.timeout.worker;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.util.nio.MinContainer;

/**
 * A threaded implementation of a worker processing {@link Timeout}s required by
 * the {@link ch.dermitza.securenio.AbstractSelector}.
 * <p>
 * The worker processes timeouts in an expiration-based FIFO fashion. Every
 * existing Timeout is being waited upon until either cancelled or expired. If a
 * timeout expires, the {@link Timeout#expired()} method is called on that
 * timeout.
 * <p>
 * Every insertion or removal of Timeouts causes the TimeoutWorker to wake up
 * from waiting, recalculate the shortest timeout to wait for, and go back to
 * sleep. The {@link TimeoutWorker} is otherwise waiting for incoming tasks via
 * the {@link #insert(Timeout)} method.
 */
public class TimeoutWorker implements Runnable {

	private final MinContainer<Timeout> timeouts = new MinContainer<>();

	private Timeout currentTO = null;
	private long waitTime;
	private boolean running = false;
	private int inserted = 0;
	private int expired = 0;
	private int cancelled = 0;

	public TimeoutWorker() {
	}

	/**
	 * Insert a {@link Timeout} that needs to be waited upon.
	 * Every Timeout insertion causes the TimeoutWorker to wake up from waiting,
	 * recalculate the shortest timeout to wait for, and go back to sleep.
	 * <p>
	 * This implementation is thread-safe.
	 * 
	 * @param timeout
	 *            The Timeout to be waited upon.
	 */
	public synchronized void insert(Timeout timeout) {
		timeouts.add(timeout);
		inserted++;
		this.notify();
	}

	/**
	 * Cancel an already waited-on {@link Timeout}.
	 * Every Timeout removal causes the TimeoutWorker to wake up from waiting,
	 * recalculate the shortest timeout to wait for, and go back to sleep.
	 * <p>
	 * This implementation is thread-safe.
	 * 
	 * @param timeout
	 *            The timeout to cancel
	 */
	public synchronized void cancel(Timeout timeout) {
		if (timeouts.isEmpty()) {
			// Nothing to cancel
			return;
		}
		if (timeouts.remove(timeout)) {
			cancelled++;
		} else {
			Logger.i(this.getClass(), "Trying to cancel already removed timeout");
		}
		Logger.d(this.getClass(), "Timeout cancelled at " + System.currentTimeMillis() + ", expiring at: " + timeout.getDelta());
		this.notifyAll();
	}

	/**
	 * Check whether the {@link TimeoutWorker} is running.
	 * <p>
	 * This implementation is thread-safe.
	 * 
	 * @return true if it is running, false otherwise
	 */
	public synchronized boolean isRunning() {
		return this.running;
	}

	/**
	 * Set the running status of the {@link TimeoutWorker}. If the running
	 * status of the worker is set to false, the TimeoutWorker is interrupted
	 * (if waiting for a task) in order to cleanly shutdown.
	 * <p>
	 * This implementation is thread-safe.
	 * 
	 * @param running
	 *            Whether the TaskWorker should run
	 */
	public synchronized void setRunning(boolean running) {
		this.running = running;
		if (!running) {
			this.notifyAll();
		}
	}

	/**
	 * The run() method of the {@link TimeoutWorker}. Here, every existing
	 * Timeout is being waited upon until either cancelled or expired.
	 * If a timeout expires, the {@link Timeout#expired()} method is called on that timeout.
	 * <p>
	 * Every insertion or removal of Timeouts causes the TimeoutWorker to wake
	 * up from waiting, recalculate the shortest timeout to wait for, and go
	 * back to sleep.
	 */
	@Override
	public void run() {
		Logger.d(this.getClass(), "Initializing...");
		running = true;
		runLoop: while (running) {
			synchronized (this) {
				while (timeouts.isEmpty()) {
					Logger.d(this.getClass(), "Waiting for timeout");
					// Check whether someone asked us to shutdown
					// If its the case, and as the queue is empty
					// we are free to break from the main loop and
					// call shutdown();
					if (!running) {
						break runLoop;
					}
					try {
						this.wait();
					} catch (InterruptedException ie) {
						// TimeoutWorker lock interrupted on empty container
						Logger.log(this.getClass(), ie, LogLevel.INFO);
					}
				}
				// At least one timeout was added, loop until timeouts are empty
				while (!timeouts.isEmpty()) {
					updateCurrentTimeout();
					// It could be the case that while getting the next time to
					// wait on, all timeouts have been removed, if this is the
					// case, break on waiting, otherwise the thread is stuck on
					// waiting forever
					if (timeouts.isEmpty()) {
						break;
					}
					try {
						Logger.d(this.getClass(), "Waiting at " + System.currentTimeMillis() + " until: " + System.currentTimeMillis() + waitTime);
						this.wait(waitTime);
					} catch (InterruptedException ie) {
						// TimeoutWorker lock interrupted while waiting on timeout
						Logger.log(this.getClass(), ie, LogLevel.INFO);
					}
					// Here, either the timeout expired or the lock was notified
					// due to at least one new timeout being added
					if (!running) {
						break runLoop;
					}
				}
				Logger.d(this.getClass(), "Out of timeoutWait loop, no more timeouts");
			}
		}

		shutdown();
	}

	/**
	 * Shutdown procedure. This method is called if the {@link TimeoutWorker}
	 * was asked to shutdown; it cleanly process the shutdown procedure.
	 * <p>
	 * This implementation is thread-safe.
	 */
	private synchronized void shutdown() {
		Logger.d(this.getClass(), "Shutting down...");
		Logger.d(this.getClass(), "Processed " + inserted + " timeouts, "
						+ expired + " expired, " + cancelled + " cancelled");
		timeouts.clear();
	}

	/**
	 * This method is called every time the TimeoutWorker thread is interrupted,
	 * either due to insertion, removal, or an expiration of a {@link Timeout}.
	 * Calling this method checks the current Timeout for expiration and if it
	 * has expired, calls its {@link Timeout#expired()} method.
	 * It then tries to get the next Timeout to be waited on, cancelling any already expired
	 * timeouts while doing so, and calculating the minimum wait time for the
	 * TimeoutWorker to wait for.
	 */
	private void updateCurrentTimeout() {
		if ((currentTO = timeouts.getMin()) == null) {
			return;
		}

		while (currentTO.isExpired() && !timeouts.isEmpty()) {
			Logger.d(this.getClass(), "Timeout expired at " + System.currentTimeMillis()
					+ ", expiring at: " + currentTO.getDelta());
			currentTO.expired();
			if (timeouts.remove(currentTO)) {
				expired++;
			} else {
				Logger.d(this.getClass(), "Trying to remove already removed timeout");
			}

			if ((currentTO = timeouts.getMin()) == null) {
				return;
			}
		}
		// We either have no remaining timeout or one timeout we should wait on.
		// Calculate the new minimum waiting time
		waitTime = currentTO.getDelta() - System.currentTimeMillis();
	}

}
