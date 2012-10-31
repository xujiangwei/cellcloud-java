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

package net.cellcloud.extras;

import java.io.File;

import net.cellcloud.util.Util;

/** 文件快递授权码。
 * 
 * @author Jiangwei Xu
 */
public final class ExpressAuthCode {

	// 写权限
	public final static int AUTH_WRITE = 0;
	// 读权限
	public final static int AUTH_READ = 1;
	// 无权限
	public final static int AUTH_NOACCESS = 2;

	private String code;
	private long origin;
	private long duration;
	private String contextPath;
	private int auth;

	/** 构造函数。
	 */
	public ExpressAuthCode(final String path, final int auth) {
		long code = Util.randomString(4).hashCode();
		long time = System.currentTimeMillis();
		this.code = Long.toString(time - code);

		this.origin = time;
		this.duration = 30 * 60 * 1000;

		this.setContextPath(path);
	}

	/** 返回授权码。
	 */
	public String getCode() {
		return this.code;
	}

	/** 返回上下文路径。
	 */
	public String getContextPath() {
		return this.contextPath;
	}
	
	/** 返回权限。
	 */
	public int getAuth() {
		return this.auth;
	}

	/** 返回有效期起始时间。
	 */
	public long getOrigin() {
		return this.origin;
	}
	/** 返回有效期限。
	 */
	public long getDuration() {
		return this.duration;
	}

	private void setContextPath(final String path) {
		this.contextPath = new String(path);
		if (!path.endsWith("\\") && !path.endsWith("/")) {
			this.contextPath += File.separator;
		}
	}

	@Override
	public int hashCode() {
		return this.code.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ExpressAuthCode) {
			return this.code.equals(((ExpressAuthCode)other).code);
		}

		return false;
	}
}
