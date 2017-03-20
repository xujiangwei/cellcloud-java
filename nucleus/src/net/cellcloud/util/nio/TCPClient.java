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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLEngine;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.util.nio.packet.AbstractPacketWorker;
import net.cellcloud.util.nio.packet.Packet;
import net.cellcloud.util.nio.secure.SecureSocket;

/**
 * A TCP Client implementation of {@link AbstractSelector}.
 * This implementation is purely non-blocking and can handle both
 * {@link PlainSocket}s and {@link SecureSocket}s.
 */
public class TCPClient extends AbstractSelector {

	private Socket sc;

	private boolean connected = false;

	/**
	 * Create a TCPClient instance
	 * 
	 * @param address
	 *            The address to connect to
	 * @param port
	 *            The port to connect to
	 * @param packetWorker
	 *            The instance of packet worker to use
	 * @param usingSSL
	 *            Whether we are using SSL/TLS
	 * @param needClientAuth
	 *            Whether this client should also authenticate with the server.
	 */
	public TCPClient(InetAddress address, int port, AbstractPacketWorker packetWorker,
			boolean usingSSL, boolean needClientAuth) {
		super(address, port, packetWorker, usingSSL, true, needClientAuth);
	}

	/**
	 * Send an {@link Packet} over the this client's {@link Socket}.
	 * Since the client only has one socket, no socket parameter is necessary.
	 * 
	 * @param packet
	 *            The Packet to send through the associated Socket.
	 * 
	 * @see AbstractSelector#send(net.cellcloud.util.nio.Socket, java.nio.ByteBuffer)
	 */
	public void send(Packet packet) {
		// Sometimes during testing send is called before the socket is
		// even initialized. Does this happen on actual single client code?
		if (sc != null) {
			send(sc, packet.toBytes());
		}
	}

	/**
	 * Invalidate the SSL/TLS session (if any) on the underlying {@link Socket}.
	 * As this client implementation only has a single socket,
	 * no parameter is needed.
	 * 
	 * @see AbstractSelector#invalidateSession(Socket)
	 */
	public void invalidateSession() {
		if (sc != null) {
			invalidateSession(sc);
		}
	}

	/**
	 * Initialize a client connection. This method initializes a {@link SocketChannel},
	 * configures it to non-blocking, and registers it
	 * with the underlying {@link java.nio.channels.Selector} instance with an
	 * OP_CONNECT {@link SelectionKey}.
	 * If this client implementation is using SSL/TLS,
	 * it also sets up the {@link SSLEngine}, to be used.
	 * 
	 * @throws IOException
	 *             Propagates all underlying IOExceptions as thrown, to be
	 *             handled by the application layer.
	 * 
	 * @see AbstractSelector#run()
	 */
	@Override
	protected void initConnection() throws IOException {
		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(false);
		channel.connect(new InetSocketAddress(address, port));

		// As part of the registration we'll register an interest in connection events.
		// These are raised when a channel is ready to complete connection establishment.
		channel.register(selector, SelectionKey.OP_CONNECT);
		channel.setOption(StandardSocketOptions.SO_SNDBUF, SocketProperties.getSoSndBuf());
		channel.setOption(StandardSocketOptions.SO_RCVBUF, SocketProperties.getSoRcvBuf());
		channel.setOption(StandardSocketOptions.SO_KEEPALIVE, SocketProperties.getKeepAlive());
		channel.setOption(StandardSocketOptions.SO_REUSEADDR, SocketProperties.getReuseAddress());
		channel.setOption(StandardSocketOptions.IP_TOS, SocketProperties.getIPTos());

		// now wrap the channel
		if (usingSSL) {
			String peerHost = channel.socket().getInetAddress().getHostAddress();
			int peerPort = channel.socket().getPort();
			SSLEngine engine = setupEngine(peerHost, peerPort);

			sc = new SecureSocket(channel, engine, singleThreaded, taskWorker, toWorker, this, this);
		}
		else {
			sc = new PlainSocket(channel);
		}

		// add the socket to the container
		container.addSocket(sc.getSocket(), sc);
	}

	/**
	 * As this is the client implementation, it is NOT allowed to call this
	 * method which is only useful for server implementations.
	 * This implementation will throw a {@link NoSuchMethodError}
	 * if it is called and do nothing else.
	 * 
	 * @param key
	 *            The selection key with the underlying {@link SocketChannel} to
	 *            be accepted
	 * 
	 * @see AbstractSelector#run()
	 */
	@Override
	protected void accept(SelectionKey key) {
		// This is severe if it happens
		throw new NoSuchMethodError("accept() is never called in client");
	}

	/**
	 * Finish the connection to the server. This method also instantiates an
	 * SSLEngine handshake if the underlying {@link Socket} is a secure socket.
	 * Finally, after the connection has been established, the socket is
	 * registered to the underlying {@link java.nio.channels.Selector}, with a
	 * {@link SelectionKey} of OP_READ, signalling it is ready to read data.
	 * 
	 * @param key
	 *            The selection key with the underlying {@link SocketChannel}
	 *            that needs a connection finalization.
	 */
	@Override
	protected void connect(SelectionKey key) {
		// Finish the connection. If the connection operation failed this will raise an IOException.
		try {
			// TCP_NODELAY should be called after we are ready to connect,
			// otherwise the socket does not recognize the option.
			sc.getSocket().setOption(StandardSocketOptions.TCP_NODELAY, SocketProperties.getTCPNoDelay());
			sc.disconnect();
		} catch (IOException ioe) {
			// Cancel the channel's registration with our selector
			// since it faled to connect. At this point, there is no
			// reason to keep the client running. Perhaps we can issue
			// a reconnection attempt at a later stage, TODO.
			setRunning(false);
			Logger.e(this.getClass(), "IOE at finishConnect(), shutting down");
			Logger.log(this.getClass(), ioe, LogLevel.ERROR);
			key.cancel();
			return;
		}

		// We are connected at this point
		connected = true;
		// Register an interest in writing on this channel
		key.interestOps(SelectionKey.OP_READ);
	}

	/**
	 * Returns whether or not this client is connected, if and only if it is
	 * running (returns false otherwise).
	 * 
	 * @return whether or not this client is connected, if and only if it is
	 *         running (returns false otherwise)
	 */
	public boolean isConnected() {
		return isRunning() ? this.connected : false;
	}

	/**
	 * This method overrides the default
	 * {@link AbstractSelector#closeSocket(Socket)} method, to also stop this
	 * client from running, as this client implementation only has one associated {@link Socket}.
	 * 
	 * @param socket
	 *            The Socket to be closed
	 * 
	 * @see AbstractSelector#closeSocket(Socket)
	 */
	@Override
	protected void closeSocket(Socket socket) {
		// This method is only called from AbstractTCPSelector
		// If this method has been called in a client implementation,
		// as a client, we must shutdown cleanly. Stop running
		setRunning(false);
		// and close the socket
		super.closeSocket(socket);
	}

}
