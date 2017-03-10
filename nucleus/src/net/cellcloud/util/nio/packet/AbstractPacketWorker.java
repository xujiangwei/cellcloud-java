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

package net.cellcloud.util.nio.packet;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

import net.cellcloud.common.Logger;
import net.cellcloud.util.nio.Socket;
import net.cellcloud.util.nio.SocketProperties;

/**
 * An abstract, extensible implementation of a Packet Worker. This thread waits
 * for raw data arriving from remote peers through {@link net.cellcloud.util.nio.Socket}s,
 * adds them to respective queues and then reassembles packets based on the data in these
 * queues. Each socket maintains a queue of data, for there is a possibility
 * that the packets received are fragmented and cannot be reassembled during
 * processing. In this case, processing should be delegated until additional
 * data on that socket arrives.
 */
public abstract class AbstractPacketWorker implements Runnable {

	private final ArrayList<PacketListener> listeners = new ArrayList<>();
	/**
	 * Maps a SocketChannel to a list of ByteBuffer instances
	 */
	protected final HashMap<Socket, ByteBuffer> pendingData = new HashMap<>();
	/**
	 * Sockets that need to be operated upon (i.e. have pending operations)
	 */
	protected final ArrayDeque<Socket> pendingSockets = new ArrayDeque<>();
	private boolean running = false;
	private byte[] tempArray;

	public AbstractPacketWorker() {
	}

	/**
	 * Queue data received from a {@link net.cellcloud.util.nio.Socket} for processing and
	 * reconstruction. A deep copy of the passed {@link ByteBuffer} is made
	 * locally. Following, a data queue for that {@link net.cellcloud.util.nio.Socket} is created if
	 * it does not exist, and the current piece of data received is added to
	 * that queue for later processing and reconstruction.
	 * 
	 * @param socket
	 *            The Socket data was received from
	 * @param data
	 *            The ByteBuffer containing the data (bytes) received
	 * @param count
	 *            The number of bytes received
	 * 
	 * @see #processData()
	 */
	public void addData(Socket socket, ByteBuffer data, int count) {
		tempArray = new byte[count];
		System.arraycopy(data.array(), 0, tempArray, 0, count);
		synchronized (this.pendingSockets) {
			if (!pendingSockets.contains(socket)) {
				// Check that we do not add a socket twice. Once is enough
				// to trigger processing
				pendingSockets.add(socket);
			}
			synchronized (this.pendingData) {
				ByteBuffer buffer = this.pendingData.get(socket);
				if (buffer == null) {
					// allocate a large enough buffer to hold the data we just received
					int size = (count > SocketProperties.getPacketBufSize()) ? count : SocketProperties.getPacketBufSize();
					buffer = ByteBuffer.allocate(size);
					this.pendingData.put(socket, buffer);
				}

				if (count > buffer.remaining()) {
					int diff = count - buffer.remaining();
					Logger.d(this.getClass(),
							"Buffer needs resizing, remaining " + buffer.remaining()
									+ "needed " + count + " difference " + diff);
					Logger.d(this.getClass(), "old size: " + buffer.capacity());
					// Allocate a new buffer. To minimize new buffer allocation,
					// we resize the buffer appropriately, in case this is a
					// *really* busy channel. Notes: If it is an extremely busy
					// channel, the buffer will keep growing, potentially making
					// the worker run out of memory trying to continuously
					// allocate larger and larger buffers. Also, a continuously
					// growing buffer can also indicate that the underlying
					// data is never or wrongly processed, that can indicate a
					// problem with the end application.
					int extSize = (diff > SocketProperties.getPacketBufSize()) ? diff : SocketProperties.getPacketBufSize();
					ByteBuffer temp = ByteBuffer.allocate(buffer.capacity()
							+ extSize);
					Logger.d(this.getClass(), "new size: " + temp.capacity());
					// Flip existing buffer to prepare for putting in the
					// replacement
					buffer.flip();
					Logger.d(this.getClass(), "pos " + buffer.position() + " lim " + buffer.limit() + " cap " + buffer.capacity());
					// put existing buffer into the temporary replacement
					temp.put(buffer);
					// Remove the old reference
					this.pendingData.remove(socket);
					// Replace reference
					buffer = temp;
					// associate the new buffer with the socket
					this.pendingData.put(socket, buffer);
					// Buffer is now resized appropriately, let the data be
					// added naturally
				}
				// Make a copy of the data
				buffer.put(tempArray);
				Logger.d(this.getClass(), "pos " + buffer.position() + " lim " + buffer.limit() + " cap " + buffer.capacity());
			}
			pendingSockets.notify();
		}
	}

	/**
	 * This is the main entry point of received data processing and reassembly.
	 * This is left for the application layer to decide how to process raw
	 * incoming data
	 */
	protected abstract void processData();

	/**
	 * The run() method of the {@link AbstractPacketWorker}. Here, sequential
	 * processing of data that need to be reconstructed and processed should be
	 * done in a FIFO fashion. The {@link AbstractPacketWorker} is otherwise
	 * waiting for incoming tasks via the
	 * {@link #addData(net.cellcloud.util.nio.Socket, ByteBuffer, int)} method.
	 */
	@Override
	public void run() {
		running = true;
		Logger.d(this.getClass(), "Initializing...");

		runLoop: while (running) {
			// Wait for data to become available
			synchronized (pendingSockets) {
				while (pendingSockets.isEmpty()) {
					// Check whether someone asked us to shutdown
					// If its the case, and as the queue is empty
					// we are free to break from the main loop and
					// call shutdown();
					if (!running) {
						break runLoop;
					}
					try {
						pendingSockets.wait();
					} catch (InterruptedException e) {
					}
				}
				// We have some data on a socket here
			}
			// Do something with the data here
			processData();
		}
		shutdown();
	}

	/**
	 * Check whether the {@link AbstractPacketWorker} is running.
	 * 
	 * @return true if it is running, false otherwise
	 */
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Set the running status of the {@link AbstractPacketWorker}. If the
	 * running status of the worker is set to false, the AbstractPacketWorker is
	 * interrupted (if waiting for a task) in order to cleanly shutdown.
	 * 
	 * @param running
	 *            Whether the PacketWorker should run or not
	 */
	public void setRunning(boolean running) {
		this.running = running;
		// If the worker is already blocked in queue.wait()
		// and someone asked us to shutdown,
		// we should interrupt it so that it shuts down
		// after processing all possible pending requests
		if (!running) {
			synchronized (pendingSockets) {
				pendingSockets.notify();
			}
		}
	}

	/**
	 * Shutdown procedure. This method is called if the
	 * {@link AbstractPacketWorker} was asked to shutdown; it cleanly process
	 * the shutdown procedure, clearing any queued data remaining and removing
	 * all listener references.
	 */
	private void shutdown() {
		Logger.d(this.getClass(), "Shutting down...");
		// Clear the queue
		pendingData.clear();
		pendingSockets.clear();
		// Remove all listener references
		listeners.clear();
	}

	// ----------------------- LISTENER METHODS -------------------------------//
	/**
	 * Allows registration of multiple {@link PacketListener}s to this
	 * {@link AbstractPacketWorker}.
	 * 
	 * @param listener
	 *            The listener to register to this PacketWorker
	 */
	public void addListener(PacketListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	/**
	 * Allows de-registration of multiple {@link PacketListener}s from this
	 * {@link AbstractPacketWorker}.
	 * 
	 * @param listener
	 *            The listener to unregister from this PacketWorker
	 */
	public void removeListener(PacketListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	/**
	 * Once a {@link PacketIF} has been completely reconstructed, registered
	 * listeners are notified via this method. This method creates a local copy
	 * of the already registered listeners when firing events, to avoid
	 * potential concurrent modification exceptions.
	 * 
	 * @param socket
	 *            The net.cellcloud.util.nio.Socket a complete PacketIF is reconstructed
	 * @param packet
	 *            The completely reconstructed AbstractPacket
	 * 
	 * @see PacketListener#paketArrived(net.cellcloud.util.nio.Socket, PacketIF)
	 */
	protected void fireListeners(Socket socket, Packet packet) {
		PacketListener[] temp;
		if (!listeners.isEmpty()) {
			temp = (PacketListener[]) listeners.toArray(new PacketListener[listeners.size()]);
			for (PacketListener listener : temp) {
				listener.packetArrived(socket, packet);
			}
		}
	}
}
