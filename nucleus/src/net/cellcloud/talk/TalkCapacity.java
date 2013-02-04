/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.talk;

import java.nio.charset.Charset;

/** 会话能力描述类。
 * 
 * @author Jiangwei Xu
 */
public final class TalkCapacity {

	/// 是否自动挂起已经被关闭的连接
	public boolean autoSuspend = false;
	/// 自动挂起时，连接挂起的有效时长，单位：毫秒
	public long suspendDuration = 0;

	public TalkCapacity(boolean autoSuspend, long suspendDuration) {
		this.autoSuspend = autoSuspend;
		this.suspendDuration = suspendDuration;
	}

	public final static byte[] serialize(TalkCapacity capacity) {
		StringBuilder buf = new StringBuilder();
		buf.append(capacity.autoSuspend ? "Y" : "N");
		buf.append("|");
		buf.append(capacity.suspendDuration);

		byte[] bytes = buf.toString().getBytes();
		buf = null;

		return bytes;
	}

	public final static TalkCapacity deserialize(byte[] bytes) {
		String str = new String(bytes, Charset.forName("UTF-8"));
		String[] array = str.split("\\|");
		if (array.length < 2) {
			return null;
		}

		boolean autoSuspend = array[0].equals("Y") ? true : false;
		long suspendDuration = Long.parseLong(array[1]);
		return new TalkCapacity(autoSuspend, suspendDuration);
	}
}
