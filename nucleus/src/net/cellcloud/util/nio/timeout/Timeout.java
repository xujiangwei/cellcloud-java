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

package net.cellcloud.util.nio.timeout;

import net.cellcloud.util.nio.Socket;

/**
 * A timeout implementation to be used with a {@link TimeoutWorker}.
 * Timeouts are associated with a {@link net.cellcloud.util.nio.Socket}
 * and a {@link TimeoutListener}.
 * When a timeout is expired, it notifies the listener of the expiration,
 * at which time the listener should act on the timeout
 * (e.g. closing the socket, performing an SSL/TLS re-handshake and so on).
 * 
 * @see TimeoutWorker
 * @see TimeoutWorker
 */
public class Timeout implements Comparable<Timeout> {

	/**
	 * Unused TODO
	 */
	public static final int SSL_TIMEOUT = 0;
	/**
	 * Unused TODO
	 */
	public static final int GENERIC_TIMEOUT = 1;

	private final Socket socket;
	private final TimeoutListener listener;
	private final long timeout;
	private final long created;
	private final long delta;
	private boolean hasExpired = false;

	/**
	 * Create a new timeout for the {@link net.cellcloud.util.nio.Socket} given.
	 * If the timeout expires, it will fire
	 * {@link TimeoutListener#timeoutExpired(net.cellcloud.util.nio.Socket)}
	 * on the associated listener.
	 * 
	 * @param socket
	 *            The socket the timeout is to be associated with
	 * @param listener
	 *            The listener listening for timeout expiration on this socket
	 * @param timeout
	 *            The timeout period
	 */
	public Timeout(Socket socket, TimeoutListener listener, long timeout) {
		this.socket = socket;
		this.listener = listener;
		this.timeout = timeout;
		this.created = System.currentTimeMillis();
		this.delta = created + timeout;
	}

	/**
	 * Get the {@link net.cellcloud.util.nio.Socket} this timeout is associated with
	 * 
	 * @return the {@link net.cellcloud.util.nio.Socket} this timeout is associated with
	 */
	public Socket getSocket() {
		return this.socket;
	}

	/**
	 * Get the timeout period
	 * 
	 * @return the timeout period
	 */
	public long getTimeout() {
		return this.timeout;
	}

	/**
	 * Get the time this timeout was created
	 * 
	 * @return the time this timeout was created
	 */
	public long getCreated() {
		return this.created;
	}

	/**
	 * Get the absolute expiration time of this timeout
	 * 
	 * @return the absolute expiration time of this timeout
	 */
	public long getDelta() {
		return this.delta;
	}

	/**
	 * Sets this timeout to expired state, firing any attached listeners in the process.
	 */
	public void expired() {
		hasExpired = true;
		if (listener != null) {
			listener.timeoutExpired(socket);
		}
	}

	/**
	 * Check whether or not this timeout is expired right now.
	 * If the timeout was set to its expired state via {@link #expired()},
	 * time checking is not performed, returning true directly.
	 * 
	 * @return whether or not this timeout is expired right now
	 */
	public boolean isExpired() {
		return hasExpired ? hasExpired
				: (System.currentTimeMillis() - getDelta()) >= 0;
	}

	/**
	 * Whether or not this timeout already had its associated {@link #expired()}
	 * method called and had its {@link TimeoutListener} fired.
	 * 
	 * @return Whether or not this timeout already had its associated
	 *         {@link #expired()} method called and had its
	 *         {@link TimeoutListener} fired.
	 */
	public boolean hasExpired() {
		return hasExpired;
	}

	/**
	 * Overrides the {@link Comparable} interface to allow timeouts to be compared.
	 * Timeouts are compared based on their absolute expiration times.
	 * 
	 * @param t
	 *            The timeout to compare to
	 * @return A negative value if the current timeout's expiration time is
	 *         earlier than the one of the given timeout, a positive value if it
	 *         is later, or 0 if both timeouts expire at the same time
	 */
	@Override
	public int compareTo(Timeout t) {
		return (int) (getDelta() - t.getDelta());
	}

}
