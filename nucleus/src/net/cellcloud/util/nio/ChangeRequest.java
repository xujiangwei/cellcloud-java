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

package net.cellcloud.util.nio;

/**
 * A ChangeRequest defines a request for some operation that needs to be
 * executed on the selector thread.
 * 
 * ChangeRequests are created and queued from threads that interact with the
 * selector thread and are necessary as the result of some particular operation
 * being completed (e.g. an SSLEngineTask having finished).
 */
public final class ChangeRequest {

	/**
	 * This type concerns an {@link javax.net.ssl.SSLEngine} task that has just
	 * finished running on the
	 * {@link ch.dermitza.securenio.socket.secure.TaskWorker} thread.
	 * <p>
	 * This type of task is used if and only if the
	 * {@link ch.dermitza.securenio.socket.secure.SecureSocket} is processing
	 * tasks via the {@link ch.dermitza.securenio.socket.secure.TaskWorker}
	 * thread (multi-threaded implementation).
	 */
	public static final int TYPE_TASK = 0;

	/**
	 * This type concerns switching the interestOps of a key associated with a
	 * particular socket.
	 */
	public static final int TYPE_OPS = 1;

	/**
	 * This type concerns a timeout that has expired on the given socket. As
	 * such, the socket needs to be closed.
	 */
	public static final int TYPE_TIMEOUT = 2;

	/**
	 * This type concerns an {@link javax.net.ssl.SSLSession} that has been
	 * invalidated. As such, we need to re-initiate handshaking.
	 */
	public static final int TYPE_SESSION = 3;

	/**
	 * The Socketable associated with this ChangeRequest
	 */
	private final Socket socket;

	/**
	 * The type associated with this ChangeRequest
	 */
	private final int type;

	/**
	 * The interestOps associated with this ChangeRequest. If the type of this
	 * ChangeRequest is anything other than TYPE_OPS, the interestOps can be set
	 * to anything safely.
	 */
	private final int interestOps;

	/**
	 * A ChangeRequest defines a request for some operation that needs to be
	 * executed on the selector thread.
	 * 
	 * ChangeRequests are created and queued from threads that interact with the
	 * selector thread and are necessary as the result of some particular
	 * operation being completed (e.g. an SSLEngine task having finished).
	 * 
	 * @param sc
	 *            The Socketable associated with this ChangeRequest
	 * @param type
	 *            The type associated with this ChangeRequest
	 * @param interestOps
	 *            The interestOps (SelectionKey.interestOps) associated with
	 *            this ChangeRequest. If the type of this ChangeRequest is
	 *            anything other than TYPE_OPS, the interestOps can be set to
	 *            anything safely.
	 */
	public ChangeRequest(Socket socket, int type, int interestOps) {
		this.socket = socket;
		this.type = type;
		this.interestOps = interestOps;
	}

	/**
	 * Get the Socketable associated with this ChangeRequest
	 * 
	 * @return The Socketable associated with this ChangeRequest
	 */
	public Socket getChannel() {
		return this.socket;
	}

	/**
	 * Get the type associated with this ChangeRequest
	 * 
	 * @return The type associated with this ChangeRequest
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * Get the interestOps associated with this ChangeRequest
	 * 
	 * @return The interestOps associated with this ChangeRequest.
	 * 	       Return of this method is unspecified for ChangeRequests with types other than TYPE_OPS.
	 */
	public int getOps() {
		return this.interestOps;
	}

}
