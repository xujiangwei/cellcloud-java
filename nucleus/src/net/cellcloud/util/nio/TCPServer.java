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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLEngine;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.util.nio.packet.AbstractPacketWorker;
import net.cellcloud.util.nio.packet.Packet;
import net.cellcloud.util.nio.secure.SecureSocket;

/**
 * A TCP Server implementation of {@link AbstractSelector}. This implementation
 * is purely non-blocking and can handle both {@link PlainSocket}s and
 * {@link SecureSocket}s.
 */
public class TCPServer extends AbstractSelector {

	private ServerSocketChannel ssc;

	/**
	 * Create a TCPServer instance.
	 * 
	 * @param address
	 *            The address to bind to
	 * @param port
	 *            The port to listen on
	 * @param packetWorker
	 *            The instance of packet worker to use
	 * @param usingSSL
	 *            Whether we are using SSL/TLS
	 * @param needClientAuth
	 *            Whether we need clients to also authenticate
	 */
	public TCPServer(InetAddress address, int port,
			AbstractPacketWorker packetWorker, boolean usingSSL,
			boolean needClientAuth) {
		super(address, port, packetWorker, usingSSL, false, needClientAuth);
	}

	/**
	 * Send an {@link Packet} over the specified {@link Socket}.
	 * 
	 * @param sc
	 *            The Socket to send the packet through.
	 * @param packet
	 *            The Packet to send through the associated Socket.
	 * 
	 * @see AbstractSelector#send(ch.dermitza.securenio.socket.Socket,
	 *      java.nio.ByteBuffer)
	 */
	public void send(Socket sc, Packet packet) {
		send(sc, packet.toBytes());
	}

	/**
	 * Initialize a server connection. This method initializes a
	 * {@link ServerSocketChannel}, configures it to non-blocking, binds it to
	 * the specified (if any) host and port, sets the specified backlog and
	 * registers it with the underlying {@link java.nio.channels.Selector}
	 * instance with an OP_ACCEPT {@link SelectionKey}.
	 * 
	 * @throws IOException
	 *             Propagates all underlying IOExceptions as thrown, to be
	 *             handled by the application layer.
	 * 
	 * @see AbstractSelector#run()
	 */
	@Override
	protected void initConnection() throws IOException {

		// Create a new non-blocking server socket channel
		Logger.d(this.getClass(), "Creating NB ServerSocketChannel");
		ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);

		// Bind the server socket to the specified address and port
		Logger.i(this.getClass(), "Binding ServerSocket to *:" + port);
		InetSocketAddress isa = new InetSocketAddress(address, port);
		// ssc.socket().setReuseAddress(true);
		ssc.socket().bind(isa, SocketProperties.getBacklog());

		// Register the server socket channel, indicating an interest in
		// accepting new connections
		Logger.d(this.getClass(), "Registering ServerChannel to selector");
		ssc.register(selector, SelectionKey.OP_ACCEPT);

	}

	/**
	 * Accepts incoming connections and binds new non-blocking {@link Socket}
	 * instances to them. If this server implementation is using SSL/TLS, it
	 * also sets up the {@link SSLEngine}, to be used.
	 * 
	 * @param key
	 *            The selection key with the underlying {@link SocketChannel} to
	 *            be accepted
	 * 
	 * @see AbstractSelector#run()
	 */
	@Override
	protected void accept(SelectionKey key) {
		SocketChannel socketChannel = null;
		Socket socket = null;
		String peerHost = null;
		int peerPort = 0;
		try {
			// Accept the connection and make it non-blocking
			socketChannel = ssc.accept();
			socketChannel.configureBlocking(false);
			socketChannel.setOption(StandardSocketOptions.TCP_NODELAY,
					SocketProperties.getTCPNoDelay());
			socketChannel.setOption(StandardSocketOptions.SO_SNDBUF,
					SocketProperties.getSoSndBuf());
			socketChannel.setOption(StandardSocketOptions.SO_RCVBUF,
					SocketProperties.getSoRcvBuf());
			socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE,
					SocketProperties.getKeepAlive());
			socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR,
					SocketProperties.getReuseAddress());
			socketChannel.setOption(StandardSocketOptions.IP_TOS,
					SocketProperties.getIPTos());

			// Register the new SocketChannel with our Selector, indicating
			// we'd like to be notified when there's data waiting to be read
			socketChannel.register(selector, SelectionKey.OP_READ);

			// Now wrap it in our container
			if (usingSSL) {
				peerHost = socketChannel.socket().getInetAddress()
						.getHostAddress();
				peerPort = socketChannel.socket().getPort();
				SSLEngine engine = setupEngine(peerHost, peerPort);

				socket = new SecureSocket(socketChannel, engine,
						singleThreaded, taskWorker, toWorker, this, this);
			} else {
				socket = new PlainSocket(socketChannel);
			}
		} catch (IOException ioe) {
			Logger.e(this.getClass(), "IOE accepting the connection");
			Logger.log(this.getClass(), ioe, LogLevel.ERROR);
			// If accepting the connection failed, close the socket and remove
			// any references to it
			closeSocket(socket);
			return;
		}
		// Finally, add the socket to our socket container
		container.addSocket(socketChannel, socket);
		Logger.d(this.getClass(), peerHost + ":" + peerPort + " connected");
	}

	/**
	 * As this is the server implementation, it is NOT allowed to call this
	 * method which is only useful for client implementations. This
	 * implementation will throw a {@link NoSuchMethodError} if it is called and
	 * do nothing else.
	 * 
	 * @param key
	 *            The selection key to be used for connecting.
	 * 
	 * @see AbstractSelector#run()
	 */
	@Override
	protected void connect(SelectionKey key) {
		throw new NoSuchMethodError("connect() is never called in client");
	}
}
