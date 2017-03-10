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

import net.cellcloud.util.nio.Socket;

/**
 * Packet listeners listen for completely reassembled {@link Packet}s on their
 * respective {@link net.cellcloud.util.nio.Socket}, and act upon them.
 * Multiple packet listeners can be registered with a single
 * {@link net.cellcloud.util.nio.packet.AbstractPacketWorker}.
 */
public interface PacketListener {

	/**
	 * This method is called from the
	 * {@link net.cellcloud.util.nio.packet.AbstractPacketWorker} thread
	 * once it has reassembled a complete {@link Packet} on a
	 * {@link net.cellcloud.util.nio.Socket}.
	 * 
	 * @param socket
	 *            The Socket that just had one complete packet reassembled
	 * @param packet
	 *            The reassembled packet on the Socket it arrived on
	 */
	public void packetArrived(Socket socket, Packet packet);

}
