/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (cellcloudproject@gmail.com)

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

package net.cellcloud.core;

import java.net.InetAddress;
import java.util.zip.CRC32;

import net.cellcloud.util.Util;

/** 终端节点。
 * 
 * @author Jiangwei Xu
 */
public final class EndpointNode extends Endpoint {

	private long hashCode;

	/** 构造函数。
	 */
	public EndpointNode(int service, InetAddress addr) {
		super(Nucleus.getInstance().getTag(), NucleusConfig.Role.NODE, addr);

		String stay = new StringBuilder(addr.getHostAddress().toString())
			.append(":").append(service).toString();
		CRC32 crc = new CRC32();
		crc.update(Util.string2Bytes(stay));
		this.hashCode = crc.getValue();
		crc = null;
	}

	/** 节点 Hash 值。
	 */
	public long getHashCode() {
		return this.hashCode;
	}
}
