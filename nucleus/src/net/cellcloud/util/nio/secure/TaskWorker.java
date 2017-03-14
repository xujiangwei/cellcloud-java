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

package net.cellcloud.util.nio.secure;

import java.util.ArrayDeque;

import net.cellcloud.common.Logger;

/**
 * A threaded implementation of a worker processing tasks required by an
 * {@link javax.net.ssl.SSLEngine} that is associated with a {@link SecureSocket}.
 * <p>
 * The worker sequentially processes tasks that need to be completed,
 * in a FIFO fashion, notifying the associated {@link TaskListener} once they have been completed.
 * The {@link TaskWorker} is otherwise waiting for incoming tasks via the {@link #addSocket(SecureSocket)} method.
 */
public class TaskWorker implements Runnable {

	private final ArrayDeque<SecureSocket> queue = new ArrayDeque<>();

	private boolean running = false;

	private final TaskListener listener;

	/**
	 * Create a {@link TaskWorker} instance with a single {@link TaskListener} reference.
	 * The {@link TaskListener} is notified whenever any task has finished being processed by the TaskWorker.
	 * 
	 * @param listener
	 *            The {@link TaskListener} to be notified of completed tasks
	 */
	public TaskWorker(TaskListener listener) {
		this.listener = listener;
	}

	/**
	 * Add a {@link SecureSocket} with an underlying
	 * {@link javax.net.ssl.SSLEngine} that requires a task to be run.
	 * Tasks are run in a FIFO queue according to order of socket insertion.
	 * 
	 * @param socket
	 *            The SecureSocket that requires a task to be run
	 */
	public void addSocket(SecureSocket socket) {
		synchronized (queue) {
			queue.add(socket);
			// fireListenersNeeded(task.getSocket());
			queue.notifyAll();
		}
	}

	/**
	 * The run() method of the {@link TaskWorker}.
	 * Here, sequential processing of {@link javax.net.ssl.SSLEngine} tasks that need to be completed is
	 * done in a FIFO fashion, notifying the associated {@link TaskListener}
	 * once they have been completed. The {@link TaskWorker} is otherwise
	 * waiting for incoming tasks via the {@link #addSocket(SecureSocket)} method.
	 */
	@Override
	public void run() {
		Logger.d(this.getClass(), "Initializing...");
		running = true;
		SecureSocket socket;

		runLoop: while (running) {
			// Wait for data to become available
			synchronized (queue) {
				while (queue.isEmpty()) {
					// Check whether someone asked us to shutdown
					// If its the case, and as the queue is empty
					// we are free to break from the main loop and
					// call shutdown();
					if (!running) {
						break runLoop;
					}
					try {
						queue.wait();
					} catch (InterruptedException e) {
					}
				}
				// Queue has some data here get the first instance
				socket = queue.remove();
			}
			// Run the runnable in this thread
			Runnable r;
			while ((r = socket.getEngine().getDelegatedTask()) != null) {
				r.run();
			}
			// Runnable finished running here, signal the listener
			listener.taskComplete(socket);
		}

		shutdown();
	}

	/**
	 * Check whether the {@link TaskWorker} is running.
	 * 
	 * @return true if it is running, false otherwise
	 */
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Set the running status of the {@link TaskWorker}. If the running status
	 * of the worker is set to false, the TaskWorker is interrupted (if waiting
	 * for a task) in order to cleanly shutdown.
	 * 
	 * @param running
	 *            Whether the TaskWorker should run or not
	 */
	public void setRunning(boolean running) {
		this.running = running;
		// If the worker is already blocked in queue.wait()
		// and someone asked us to shutdown,
		// we should interrupt it so that it shuts down
		// after processing all possible pending requests
		if (!running) {
			synchronized (queue) {
				queue.notifyAll();
			}
		}
	}

	/**
	 * Shutdown procedure.
	 * This method is called if the {@link TaskWorker} was asked to shutdown; it cleanly process the shutdown procedure.
	 */
	private void shutdown() {
		Logger.d(this.getClass(), "Shutting down...");
		// Clear the queue
		queue.clear();
		// Remove all listener references
		// listeners.clear();
	}

}
