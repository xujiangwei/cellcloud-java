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

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public interface Socket {

	/**
	 * Returns the underlying {@link SocketChannel}.
	 * This is done in order to register the current socket with a {@link Selector},
	 * as only the {@link SocketChannel} implementation is allowed to be associated
	 * with a {@link Selector}.
	 * 
	 * @return the underlying SocketChannel
	 */
	SocketChannel getSocket();

	// ------------------ PASS-THROUGH IMPLEMENTATIONS ------------------------ //
	/**
	 * Pass-through implementation of
	 * {@link SocketChannel#connect(SocketAddress remote)}
	 * 
	 * @param remote
	 *            The remote address to which this channel is to be connected
	 * @return true if a connection was established, false if this channel is in
	 *         non-blocking mode and the connection operation is in progress
	 * @throws IOException
	 *             Propagated exceptions from the underlying
	 *             {@link SocketChannel#connect(SocketAddress remote)}
	 *             implementation.
	 */
	boolean connect(SocketAddress remote) throws IOException;

	/**
	 * Pass-through implementation of {@link SocketChannel#finishConnect()}
	 * 
	 * @return true if, and only if, this channel's socket is now connected
	 * @throws IOException
	 *             Propagated exceptions from the underlying
	 *             {@link SocketChannel#finishConnect()} implementation.
	 */
	boolean disconnect() throws IOException;

	/**
	 * Pass-through implementation of
	 * {@link SocketChannel#register(Selector sel, int ops)}
	 * 
	 * @param sel
	 *            The selector with which this channel is to be registered
	 * @param ops
	 *            The interest set for the resulting key
	 * @return A key representing the registration of this channel with the
	 *         given selector
	 * @throws ClosedChannelException
	 *             Propagated exceptions from the underlying
	 *             {@link SocketChannel#register(Selector sel, int ops)}
	 *             implementation.
	 */
	SelectionKey register(Selector sel, int ops) throws ClosedChannelException;

	/**
	 * Pass-through implementation of
	 * {@link SocketChannel#configureBlocking(boolean block)}
	 * 
	 * @param block
	 *            If true then this channel will be placed in blocking mode;
	 *            if false then it will be placed in non-blocking mode
	 * @return This selectable channel
	 * @throws IOException
	 *             Propagated exceptions from the underlying
	 *             {@link SocketChannel#configureBlocking(boolean block)}
	 *             implementation.
	 */
	SelectableChannel configureBlocking(boolean block) throws IOException;

	/**
	 * Pass-through implementation of
	 * {@link SocketChannel#read(ByteBuffer buffer)}
	 * 
	 * @param buffer
	 *            The buffer into which bytes are to be transferred
	 * @return The number of bytes read, possibly zero, or -1 if the channel has
	 *         reached end-of-stream
	 * @throws IOException
	 *             Propagated exceptions from the underlying
	 *             {@link SocketChannel#read(ByteBuffer buffer)} implementation.
	 */
	int read(ByteBuffer buffer) throws IOException;

	/**
	 * Pass-through implementation of
	 * {@link SocketChannel#write(ByteBuffer buffer)}
	 * 
	 * @param buffer
	 * @return The number of bytes written, possibly zero
	 * @throws IOException
	 *             Propagated exceptions from the underlying
	 *             {@link SocketChannel#write(ByteBuffer buffer)}
	 *             implementation.
	 */
	int write(ByteBuffer buffer) throws IOException;

	/**
	 * Pass-through implementation of {@link SocketChannel#close()}
	 * 
	 * @throws IOException
	 *             Propagated exceptions from the underlying
	 *             SocketChannel.close() implementation.
	 */
	void close() throws IOException;


	// -------------------- SSL/TLS-SPECIFIC IMPLEMENTATIONS ------------------ //

	/**
	 * Invalidate the current {@link javax.net.ssl.SSLSession}. This method
	 * could be periodically used via a {@link Timeout} to perform SSL/TLS
	 * session rotation if needed.
	 * <p>
	 * This method has NO EFFECT for a {@link PlainSocket} implementation.
	 */
	void invalidateSession();

	/**
	 * Initialize SSL/TLS handshaking.
	 * <p>
	 * This method has NO EFFECT for a {@link PlainSocket} implementation.
	 * 
	 * @throws IOException
	 *             propagated exceptions from {@link #processHandshake()}
	 * @throws javax.net.ssl.SSLException
	 *             if there is an SSL/TLS problem with the call to the
	 *             underlying {@link javax.net.ssl.SSLEngine#beginHandshake()}.
	 * @see net.cellcloud.util.nio.secure.SecureSocket#initHandshake()
	 */
	void initHandshake() throws IOException;

	/**
	 * Update the {@link javax.net.ssl.SSLEngineResult} and
	 * {@link #setTaskPending(boolean)} to false.
	 * <p>
	 * This method has NO EFFECT for a {@link PlainSocket} implementation.
	 * 
	 * @see net.cellcloud.util.nio.secure.SecureSocket#updateResult()
	 */
	void updateResult();

	/**
	 * Used to identify whether the handshaking performed from the underlying
	 * {@link javax.net.ssl.SSLEngine} is still pending.
	 * <p>
	 * This method has NO EFFECT for a {@link PlainSocket} implementation.
	 * 
	 * @return whether the SSL/TLS handshaking is still pending. Always false
	 *         for a {@link PlainSocket} implementation
	 * @see net.cellcloud.util.nio.secure.SecureSocket#handshakePending()
	 */
	boolean handshakePending();

	/**
	 * Sets whether or not there is an {@link javax.net.ssl.SSLEngine} task
	 * pending, during an SSL/TLS handshake.
	 * <p>
	 * This method has NO EFFECT for a {@link PlainSocket} implementation.
	 * 
	 * @param taskPending
	 *            Set whether or not there is a {@link javax.net.ssl.SSLEngine}
	 *            task pending for the {@link javax.net.ssl.SSLEngine}
	 *            associated with the underlying {@link SocketChannel}. Has no
	 *            effect in a single threaded implementation.
	 * @see net.cellcloud.util.nio.secure.SecureSocket#setTaskPending(boolean)
	 */
	void setTaskPending(boolean taskPending);

	/**
	 * Performs SSL/TLS handshaking.
	 * <p>
	 * This method has NO EFFECT for a {@link PlainSocket} implementation.
	 * 
	 * @throws IOException
	 *             If there is an underlying IOException while performing the
	 *             handshake
	 * @throws javax.net.ssl.SSLException
	 *             If there is an exception thrown by the underlying
	 *             {@link javax.net.ssl.SSLEngine} while performing the
	 *             handshake
	 * @see net.cellcloud.util.nio.secure.SecureSocket#processHandshake()
	 */
	void processHandshake() throws IOException;

}
