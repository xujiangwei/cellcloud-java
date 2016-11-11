/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2014 Cell Cloud Team (www.cellcloud.net)

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

	/// 是否为加密会话
	protected boolean secure = false;

	/// 重复尝试连接的次数
	protected int retryAttempts = 0;
	/// 两次连接中间隔时间，单位毫秒
	protected long retryDelay = 5000L;

	public TalkCapacity() {
	}

	/**
	 * 构造函数。
	 * @param retryAttempts
	 * @param retryDelay
	 */
	public TalkCapacity(int retryAttempts, long retryDelay) {
		this(false, retryAttempts, retryDelay);
	}

	/**
	 * 构造函数。
	 * @param secure
	 * @param retryAttempts
	 * @param retryDelay
	 */
	public TalkCapacity(boolean secure, int retryAttempts, long retryDelay) {
		this.secure = secure;
		this.retryAttempts = retryAttempts;
		this.retryDelay = retryDelay;

		if (this.retryAttempts == Integer.MAX_VALUE) {
			this.retryAttempts -= 1;
		}
	}

	public final static byte[] serialize(TalkCapacity capacity) {
		StringBuilder buf = new StringBuilder();
		buf.append(1);
		buf.append("|");
		buf.append(capacity.secure ? "Y" : "N");
		buf.append("|");
		buf.append(capacity.retryAttempts);
		buf.append("|");
		buf.append(capacity.retryDelay);

		byte[] bytes = buf.toString().getBytes();
		buf = null;

		return bytes;
	}

	public final static TalkCapacity deserialize(byte[] bytes) {
		String str = new String(bytes, Charset.forName("UTF-8"));
		String[] array = str.split("\\|");
		if (array.length < 4) {
			return null;
		}

		TalkCapacity cap = new TalkCapacity();
		int version = Integer.parseInt(array[0]);
		if (version == 1) {
			cap.secure = array[1].equalsIgnoreCase("Y") ? true : false;
			cap.retryAttempts = Integer.parseInt(array[2]);
			cap.retryDelay = Integer.parseInt(array[3]);
		}
		return cap;
	}
}
