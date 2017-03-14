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

import java.nio.channels.SelectableChannel;
import java.util.HashMap;

/**
 * A container implementation containing {@link SelectableChannel},
 * {@link Socket} pairs.
 * This container is used by the {@link net.cellcloud.util.nio.AbstractSelector}
 * to keep track of valid sockets.
 * While it is used from a client implementation, it is most useful to
 * a server implementation that deals with multiple connected sockets (clients).
 * <p>
 * This implementation is synchronized and thread-safe.
 */
public class SocketContainer {

	/**
	 * The underlying HashMap containing the pairs of {@link SelectableChannel}
	 * and {@link Socket}.
	 */
	private final HashMap<SelectableChannel, Socket> sockets = new HashMap<>();

	/**
	 * Get the {@link Socket} that is paired to the given {@link SelectableChannel} key.
	 * 
	 * @param key
	 *            the {@link SelectableChannel} key whose associated value is to
	 *            be returned
	 * @return the {@link Socket} to which the specified
	 *         {@link SelectableChannel} is mapped, or null if this map contains
	 *         no mapping for the {@link SelectableChannel}.
	 */
	public synchronized Socket getSocket(SelectableChannel key) {
		return sockets.get(key);
	}

	/**
	 * Returns true if this map contains a mapping for the specified
	 * {@link SelectableChannel} key.
	 * 
	 * @param key
	 *            {@link SelectableChannel} key whose presence in this map is to
	 *            be tested
	 * @return true if this map contains a {@link Socket} mapping for the
	 *         specified {@link SelectableChannel} key
	 */
	public synchronized boolean containsKey(SelectableChannel key) {
		return sockets.containsKey(key);
	}

	/**
	 * Associates the specified {@link Socket} with the specified
	 * {@link SelectableChannel} in this map.
	 * If the map previously contained a mapping for the {@link SelectableChannel},
	 * the old {@link Socket} is replaced by the specified {@link Socket}.
	 * 
	 * @param key
	 *            the {@link SelectableChannel} key with which the specified
	 *            value is to be associated
	 * @param socket
	 *            {@link Socket} to be associated with the specified key
	 */
	public synchronized void addSocket(SelectableChannel key, Socket socket) {
		sockets.put(key, socket);
		// System.out.println("Sockets: " + size());
	}

	/**
	 * Remove the {@link Socket} that is paired to the given
	 * {@link SelectableChannel} key.
	 * 
	 * @param key
	 *            the {@link SelectableChannel} key whose associated value is to
	 *            be removed from the map
	 * @return the previous {@link Socket} associated with
	 *         {@link SelectableChannel} is mapped, or null if there was no
	 *         mapping for {@link SelectableChannel}.
	 */
	public synchronized Socket removeSocket(SelectableChannel key) {
		// System.out.println("Sockets: " + (size() - 1));
		return sockets.remove(key);
	}

	/**
	 * Removes all {@link SelectableChannel}, {@link Socket} mappings from this map.
	 * The map will be empty after this call returns.
	 */
	public synchronized void clear() {
		sockets.clear();
	}

	/**
	 * Returns the number of {@link SelectableChannel}, {@link Socket}
	 * mappings in this map. If the map contains more than Integer.MAX_VALUE
	 * elements, returns Integer.MAX_VALUE.
	 * 
	 * @return the number of {@link SelectableChannel}, {@link Socket}
	 *         mappings in this map
	 */
	public synchronized int size() {
		return sockets.size();
	}

}
