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

/**
 * A network packet interface, containing required methods to create an
 * application-level packet, to reconstrust such a packet from raw bytes received,
 * and to get the contents of this packet as raw bytes.
 * The implementation specific details are application dependent.
 */
public interface Packet {

	/**
	 * Reconstruct this Packet from the given ByteBuffer
	 * 
	 * @param source
	 *            The ByteBuffer to reconstruct this Packet from
	 */
	public void reconstruct(ByteBuffer source);

	/**
	 * Get the contents of this Packet as a ByteBuffer
	 * 
	 * @return the contents of this Packet as a ByteBuffer
	 */
	public ByteBuffer toBytes();

}
