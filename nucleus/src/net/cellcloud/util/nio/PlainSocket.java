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

/**
 * A plain socket implementation of {@link Socket}. As a plain socket behaves
 * no different than a {@link SocketChannel}, this class can also be seen as a
 * wrapper with no additional functionality. Note that this class is declared as
 * final as it should NOT be extended.
 * <p>
 * All required methods from {@link Socket} that deal with the secure
 * implementation ({@link net.cellcloud.util.nio.secure.SecureSocket})
 * have no effect in this implementation.
 */
public final class PlainSocket implements Socket {

	/**
	 * The underlying {@link SocketChannel}
	 */
	private final SocketChannel channel;

	/**
	 * Create a plain socket (i.e. no encryption) instance of the
	 * {@link Socket} interface. This instance has empty placeholder methods
	 * for the SSL/TLS-related methods.
	 * 
	 * @param channel
	 *            The associated underlying {@link SocketChannel}.
	 */
	public PlainSocket(SocketChannel channel) {
		this.channel = channel;
	}

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
	@Override
	public SelectableChannel configureBlocking(boolean block) throws IOException {
		return channel.configureBlocking(block);
	}

	/**
	 * Pass-through implementation of
	 * {@link SocketChannel#read(ByteBuffer buffer)}
	 * 
	 * @param buffer
	 *            The buffer into which bytes are to be transferred
	 * @return The number of bytes read, possibly zero,
	 *         or -1 if the channel has reached end-of-stream
	 * @throws IOException
	 *             Propagated exceptions from the underlying
	 *             {@link SocketChannel#read(ByteBuffer buffer)} implementation.
	 */
	@Override
	public int read(ByteBuffer buffer) throws IOException {
		return channel.read(buffer);
	}

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
	@Override
	public int write(ByteBuffer buffer) throws IOException {
		return channel.write(buffer);
	}

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
	@Override
	public boolean connect(SocketAddress remote) throws IOException {
		return channel.connect(remote);
	}

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
	@Override
	public SelectionKey register(Selector sel, int ops) throws ClosedChannelException {
		return channel.register(sel, ops);
	}

	/**
	 * Returns the underlying {@link SocketChannel}. This is done in order to
	 * register the current socket with a {@link Selector}, as only the
	 * {@link SocketChannel} implementation is allowed to be associated with a
	 * {@link Selector}.
	 * 
	 * @return the underlying SocketChannel
	 */
	@Override
	public SocketChannel getSocket() {
		return channel;
	}

	/**
	 * Pass-through implementation of {@link SocketChannel#close()}
	 * 
	 * @throws IOException
	 *             Propagated exceptions from the underlying
	 *             SocketChannel.close() implementation.
	 */
	@Override
	public void close() throws IOException {
		channel.close();
	}

	/**
	 * Pass-through implementation of {@link SocketChannel#finishConnect()}
	 * 
	 * @return true if, and only if, this channel's socket is now connected
	 * @throws IOException
	 *             Propagated exceptions from the underlying
	 *             {@link SocketChannel#finishConnect()} implementation.
	 */
	@Override
	public boolean disconnect() throws IOException {
		return channel.finishConnect();
	}

	// ---------------------- SSL/TLS METHODS (UNUSED) ------------------------ //
	/**
	 * Empty implementation satisfying {@link Socket#processHandshake()}.
	 * No handshaking is present on a {@link PlainSocket}.
	 * This method has NO effect.
	 */
	@Override
	public void processHandshake() {
		// we don't need to do any handshaking on a plain channel
	}

	/**
	 * Empty implementation satisfying {@link Socket#handshakePending()}.
	 * No handshaking is present on a {@link PlainSocket}.
	 * This method has NO effect.
	 * 
	 * @return Always false, no handshake is pending on a PlainSocket
	 */
	@Override
	public boolean handshakePending() {
		// No handshaking happens in a plain socket
		return false;
	}

	/**
	 * Empty implementation satisfying {@link Socket#updateResult()}.
	 * No {@link javax.net.ssl.SSLEngineResult} is present on a {@link PlainSocket}.
	 * This method has NO effect.
	 */
	@Override
	public void updateResult() {
		// Nothing to do in a plain channel
	}

	/**
	 * Empty implementation satisfying
	 * {@link Socket#setTaskPending(boolean taskPending)}.
	 * No task is pending on a {@link PlainSocket}. This method has NO effect.
	 * 
	 * @param taskPending
	 *            unused
	 */
	@Override
	public void setTaskPending(boolean taskPending) {
		// no pending task for a plain socket, do nothing
	}

	/**
	 * Empty implementation satisfying {@link Socket#invalidateSession()}.
	 * No {@link javax.net.ssl.SSLSession} is present on a {@link PlainSocket}.
	 * This method has NO effect.
	 */
	@Override
	public void invalidateSession() {
		// Nothing to do here
	}

	/**
	 * Empty implementation satisfying {@link Socket#initHandshake()}.
	 * No handshaking is present on a {@link PlainSocket}.
	 * This method has NO effect.
	 * 
	 * @throws IOException
	 *             Never thrown.
	 */
	@Override
	public void initHandshake() throws IOException {
		// Nothing to do here
	}

}
