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

import java.util.List;

import net.cellcloud.util.property.BooleanProperty;
import net.cellcloud.util.property.IntegerProperty;
import net.cellcloud.util.property.ListProperty;
import net.cellcloud.util.property.LongProperty;
import net.cellcloud.util.property.Properties;
import net.cellcloud.util.property.PropertyReference;

/**
 * A static helper implementation for setting up static runtime options.
 * The options are located in a {@link Properties} file.
 * For further information on the options set, please look at the supplied setup.properties.
 */
public class SocketProperties {

	private static final Properties props = new Properties();

	private SocketProperties() {
		// Static class, disallow instantiation
	}

	/**
	 * Return whether the selector should handle {@link javax.net.ssl.SSLEngine} tasks in the same thread.
	 * If not, a {@link net.cellcloud.util.nio.secure.TaskWorker} is initialized and
	 * used for that purpose.
	 * 
	 * @return whether the selector should handle {@link javax.net.ssl.SSLEngine} tasks in the same thread.
	 * 
	 * @see net.cellcloud.util.nio.secure.TaskWorker
	 */
	public static boolean getSelectorSingleThreaded() {
		PropertyReference ref = props.getProperty("selector.single_threaded");
		if (null == ref) {
			return false;
		}

		return ((BooleanProperty) ref).getValueAsBoolean();
	}

	/**
	 * Return whether the selector thread should process all {@link ChangeRequest}s at each iteration.
	 * If not, the values from {@link #getMaxChanges()} and {@link #getSelectorTimeoutMS()} are used.
	 * 
	 * @return whether the selector thread should process all {@link ChangeRequest}s
	 * 
	 * @see #getMaxChanges()
	 * @see #getSelectorTimeoutMS()
	 * @see AbstractSelector#processChanges()
	 */
	public static boolean getSelectorProcessAll() {
		PropertyReference ref = props.getProperty("selector.process_all_changes");
		if (null == ref) {
			return true;
		}

		return ((BooleanProperty) ref).getValueAsBoolean();
	}

	/**
	 * If the selector thread should process all {@link ChangeRequest}s at each iteration,
	 * this method returns the maximum changes to be processed at each iteration.
	 * 
	 * @return the maximum {@link ChangeRequest}s processed at each iteration
	 * 
	 * @see #getMaxChanges()
	 * @see #getSelectorTimeoutMS()
	 * @see AbstractSelector#processChanges()
	 */
	public static int getMaxChanges() {
		PropertyReference ref = props.getProperty("selector.max_changes");
		if (null == ref) {
			return 100;
		}

		return ((IntegerProperty) ref).getValueAsInt();
	}

	/**
	 * If the selector thread should process all {@link ChangeRequest}s at each iteration,
	 * this method returns the maximum time (MS) the selector should wait on a
	 * select() before returning to process the remaining changes.
	 * 
	 * @return the maximum time (MS) the selector should wait on a select()
	 *         before returning to process the remaining changes.
	 * 
	 * @see #getMaxChanges()
	 * @see #getSelectorTimeoutMS()
	 * @see AbstractSelector#processChanges()
	 */
	public static long getSelectorTimeoutMS() {
		PropertyReference ref = props.getProperty("selector.timeout_ms");
		if (null == ref) {
			return 10L;
		}

		return ((LongProperty) ref).getValueAsLong();
	}

	/**
	 * Returns the timeout period (MS) for a {@link net.cellcloud.util.nio.secure.SecureSocket}
	 * to wait on an empty buffer during handshaking.
	 * 
	 * @return the timeout period (MS) for a
	 *         {@link net.cellcloud.util.nio.secure.SecureSocket} to wait
	 *         on an empty buffer during handshaking.
	 */
	public static long getTimeoutMS() {
		PropertyReference ref = props.getProperty("timeout.period_ms");
		if (null == ref) {
			return 20000L;
		}

		return ((LongProperty) ref).getValueAsLong();
	}

	/**
	 * Returns the size of the backlog (socket number) of a
	 * {@link java.nio.channels.ServerSocketChannel}.
	 * 
	 * @return the size of the backlog (socket number) of a
	 *         {@link java.nio.channels.ServerSocketChannel}.
	 */
	public static int getBacklog() {
		PropertyReference ref = props.getProperty("socket.backlog");
		if (null == ref) {
			return 100000;
		}

		return ((IntegerProperty) ref).getValueAsInt();
	}

	/**
	 * Returns the default size (bytes) of the
	 * {@link net.cellcloud.util.nio.packet.AbstractPacketWorker}.
	 * Note that the size can grow if the data received overflows on a particular socket.
	 * 
	 * @return the default size (bytes) of the
	 *         {@link net.cellcloud.util.nio.packet.AbstractPacketWorker}.
	 * 
	 * @see net.cellcloud.util.nio.packet.AbstractPacketWorker#addData(Socket, java.nio.ByteBuffer, int)
	 */
	public static int getPacketBufSize() {
		PropertyReference ref = props.getProperty("packetworker.buffer_size");
		if (null == ref) {
			return 4096;
		}

		return ((IntegerProperty) ref).getValueAsInt();
	}

	/**
	 * Returns the SO_SNDBUF size (bytes) to be set for each socket.
	 * 
	 * @return the SO_SNDBUF size (bytes) to be set for each socket.
	 * 
	 * @see java.net.StandardSocketOptions#SO_SNDBUF
	 */
	public static int getSoSndBuf() {
		PropertyReference ref = props.getProperty("socket.so_sndbuf");
		if (null == ref) {
			return 4096;
		}

		return ((IntegerProperty) ref).getValueAsInt();
	}

	/**
	 * Returns the SO_RCVBUF size (bytes) to be set for each socket.
	 * 
	 * @return the SO_RCVBUF size (bytes) to be set for each socket.
	 * 
	 * @see java.net.StandardSocketOptions#SO_RCVBUF
	 */
	public static int getSoRcvBuf() {
		PropertyReference ref = props.getProperty("socket.so_rcvbuf");
		if (null == ref) {
			return 4096;
		}

		return ((IntegerProperty) ref).getValueAsInt();
	}

	/**
	 * Returns the IP_TOS to be set for each socket.
	 * 
	 * @return the IP_TOS to be set for each socket.
	 * 
	 * @see java.net.StandardSocketOptions#IP_TOS
	 */
	public static int getIPTos() {
		PropertyReference ref = props.getProperty("socket.ip_tos");
		if (null == ref) {
			return 0;
		}

		return ((IntegerProperty) ref).getValueAsInt();
	}

	/**
	 * Get the enabled protocols to be used with a {@link javax.net.ssl.SSLEngine}.
	 * 
	 * @return the enabled protocols to be used with a {@link javax.net.ssl.SSLEngine}.
	 */
	public static String[] getProtocols() {
		PropertyReference ref = props.getProperty("secure.protocols");
		if (null == ref) {
			return new String[]{ "SSLv3", "TLSv1.2" };
		}

		@SuppressWarnings("unchecked")
		ListProperty<String> value = (ListProperty<String>) ref;
		List<String> list = value.getValueAsList();
		String[] ret = new String[list.size()];
		for (int i = 0; i < list.size(); ++i) {
			ret[i] = list.get(i);
		}
		return ret;
	}

	/**
	 * Get the enabled cipher suites to be used with a {@link javax.net.ssl.SSLEngine}.
	 * 
	 * @return the enabled cipher suites to be used with a {@link javax.net.ssl.SSLEngine}.
	 */
	public static String[] getCipherSuites() {
		PropertyReference ref = props.getProperty("secure.cipher_suites");
		if (null == ref) {
			return new String[]{
				"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
				"TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
				"TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
				"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
				"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
				"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
				"TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
				"TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
				"TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
				"TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
				"TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
				"TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
				"TLS_RSA_WITH_AES_128_CBC_SHA256",
				"TLS_RSA_WITH_AES_128_CBC_SHA" };
		}

		@SuppressWarnings("unchecked")
		ListProperty<String> value = (ListProperty<String>) ref;
		List<String> list = value.getValueAsList();
		String[] ret = new String[list.size()];
		for (int i = 0; i < list.size(); ++i) {
			ret[i] = list.get(i);
		}
		return ret;
	}

	/**
	 * Returns the TCP_NODELAY size (bytes) to be set for each socket.
	 * 
	 * @return the TCP_NODELAY size (bytes) to be set for each socket.
	 * 
	 * @see java.net.StandardSocketOptions#TCP_NODELAY
	 */
	public static boolean getTCPNoDelay() {
		PropertyReference ref = props.getProperty("socket.tcp_nodelay");
		if (null == ref) {
			return true;
		}

		return ((BooleanProperty) ref).getValueAsBoolean();
	}

	/**
	 * Returns the SO_KEEPALIVE size (bytes) to be set for each socket.
	 * 
	 * @return the SO_KEEPALIVE size (bytes) to be set for each socket.
	 * 
	 * @see java.net.StandardSocketOptions#SO_KEEPALIVE
	 */
	public static boolean getKeepAlive() {
		PropertyReference ref = props.getProperty("socket.so_keepalive");
		if (null == ref) {
			return false;
		}

		return ((BooleanProperty) ref).getValueAsBoolean();
	}

	/**
	 * Returns the SO_REUSEADDR size (bytes) to be set for each socket.
	 * 
	 * @return the SO_REUSEADDR size (bytes) to be set for each socket.
	 * 
	 * @see java.net.StandardSocketOptions#SO_REUSEADDR
	 */
	public static boolean getReuseAddress() {
		PropertyReference ref = props.getProperty("socket.so_reuseaddr");
		if (null == ref) {
			return false;
		}

		return ((BooleanProperty) ref).getValueAsBoolean();
	}

}
