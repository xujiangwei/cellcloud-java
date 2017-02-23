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

package net.cellcloud.talk;

import java.nio.charset.Charset;

/**
 * 会话能力描述类。
 * 
 * @author Ambrose Xu
 * 
 */
public final class TalkCapacity {

	/** 版本描述。 */
	private int version = 3;

	/** 内核的版本串号。 */
	private int versionNumber = 150;

	/** 是否为加密会话。 */
	public boolean secure = false;

	/** 重复尝试连接的次数。 */
	public int retryAttempts = 0;
	/** 两次连接中间隔时间，单位：毫秒。 */
	public long retryDelay = 5000L;

	/** 是否以代理方式进行访问。 */
	public boolean proxy = false;

	/**
	 * 构造函数。
	 */
	public TalkCapacity() {
	}

	/**
	 * 构造函数。
	 * 
	 * @param retryAttempts
	 * @param retryDelay
	 */
	public TalkCapacity(int retryAttempts, long retryDelay) {
		this(false, retryAttempts, retryDelay);
	}

	/**
	 * 构造函数。
	 * 
	 * @param secure
	 * @param retryAttempts
	 * @param retryDelay
	 */
	public TalkCapacity(boolean secure, int retryAttempts, long retryDelay) {
		this(secure, retryAttempts, retryDelay, false);
	}

	/**
	 * 构造函数。
	 * 
	 * @param secure
	 * @param retryAttempts
	 * @param retryDelay
	 * @param proxy
	 */
	public TalkCapacity(boolean secure, int retryAttempts, long retryDelay, boolean proxy) {
		this.secure = secure;
		this.retryAttempts = retryAttempts;
		this.retryDelay = retryDelay;
		this.proxy = proxy;

		if (this.retryAttempts == Integer.MAX_VALUE) {
			this.retryAttempts -= 1;
		}
	}

	/**
	 * 重置描述版本号。
	 * 
	 * @param version 指定新版本描述。
	 */
	public void resetVersion(int version) {
		this.version = version;

		if (version == 1) {
			this.versionNumber = 130;
		}
		else if (version == 2 || version == 3) {
			this.versionNumber = 150;
		}
	}

	/**
	 * 获得版本串号。
	 * 
	 * @return 返回版本串号。
	 */
	protected int getVersionNumber() {
		return this.versionNumber;
	}

	/**
	 * 序列化 TalkCapacity 实例。
	 * 
	 * @param capacity 指定 TalkCapacity 实例。
	 * @return 返回序列化结果。
	 */
	public final static byte[] serialize(TalkCapacity capacity) {
		StringBuilder buf = new StringBuilder();
		if (capacity.version == 1) {
			buf.append(1);
			buf.append("|");
			buf.append(capacity.secure ? "Y" : "N");
			buf.append("|");
			buf.append(capacity.retryAttempts);
			buf.append("|");
			buf.append(capacity.retryDelay);
		}
		else if (capacity.version == 2) {
			buf.append(2);
			buf.append("|");
			buf.append(capacity.secure ? "Y" : "N");
			buf.append("|");
			buf.append(capacity.retryAttempts);
			buf.append("|");
			buf.append(capacity.retryDelay);
			buf.append("|");
			buf.append(capacity.versionNumber);
		}
		else if (capacity.version == 3) {
			buf.append(3);
			buf.append("|");
			buf.append(capacity.secure ? "Y" : "N");
			buf.append("|");
			buf.append(capacity.retryAttempts);
			buf.append("|");
			buf.append(capacity.retryDelay);
			buf.append("|");
			buf.append(capacity.proxy ? "Y" : "N");
			buf.append("|");
			buf.append(capacity.versionNumber);
		}

		byte[] bytes = buf.toString().getBytes();
		buf = null;

		return bytes;
	}

	/**
	 * 反序列化 TalkCapacity 实例。
	 * 
	 * @param bytes 指定待反序列化的数据。
	 * @return 返回 TalkCapacity 实例。
	 */
	public final static TalkCapacity deserialize(byte[] bytes) {
		String str = new String(bytes, Charset.forName("UTF-8"));
		String[] array = str.split("\\|");
		if (array.length < 4) {
			return null;
		}

		TalkCapacity cap = new TalkCapacity();

		cap.version = Integer.parseInt(array[0]);
		if (cap.version == 1) {
			cap.secure = array[1].equalsIgnoreCase("Y") ? true : false;
			cap.retryAttempts = Integer.parseInt(array[2]);
			cap.retryDelay = Integer.parseInt(array[3]);
		}
		else if (cap.version == 2) {
			cap.secure = array[1].equalsIgnoreCase("Y") ? true : false;
			cap.retryAttempts = Integer.parseInt(array[2]);
			cap.retryDelay = Integer.parseInt(array[3]);
			cap.versionNumber = Integer.parseInt(array[4]);
		}
		else if (cap.version == 3) {
			cap.secure = array[1].equalsIgnoreCase("Y") ? true : false;
			cap.retryAttempts = Integer.parseInt(array[2]);
			cap.retryDelay = Integer.parseInt(array[3]);
			cap.proxy = array[4].equalsIgnoreCase("Y") ? true : false;
			cap.versionNumber = Integer.parseInt(array[5]);
		}
		else {
			// 尝试兼容未知版本号
			try {
				cap.secure = array[1].equalsIgnoreCase("Y") ? true : false;
				cap.retryAttempts = Integer.parseInt(array[2]);
				cap.retryDelay = Integer.parseInt(array[3]);
			} catch (Exception e) {
				// Nothing
			}
		}

		return cap;
	}

}
