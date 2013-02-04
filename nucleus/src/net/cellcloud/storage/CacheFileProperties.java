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

package net.cellcloud.storage;

import net.cellcloud.util.IntegerProperty;
import net.cellcloud.util.Properties;
import net.cellcloud.util.StringProperty;

/** 缓存文件属性。
 * 
 * @author Jiangwei Xu
 */
public final class CacheFileProperties extends Properties {

	/// 模式
	public final static String MODE = "mode";
	/// 地址
	public final static String HOST = "host";
	/// 端口
	public final static String PORT = "port";

	/// 守护服务模式
	public final static int MODE_DAEMON = 1;
	/// 客户端模式
	public final static int MODE_CLIENT = 9;

	public CacheFileProperties() {
	}

	/** 设置模式。
	 */
	public void setMode(int mode) {
		if (this.hasProperty(MODE)) {
			this.updateProperty(new IntegerProperty(MODE, mode));
		}
		else {
			this.addProperty(new IntegerProperty(MODE, mode));
		}
	}

	/** 设置地址。
	 */
	public void setAddress(String host, int port) {
		if (this.hasProperty(HOST)) {
			this.updateProperty(new StringProperty(HOST, host));
		}
		else {
			this.addProperty(new StringProperty(HOST, host));
		}

		if (this.hasProperty(PORT)) {
			this.updateProperty(new IntegerProperty(PORT, port));
		}
		else {
			this.addProperty(new IntegerProperty(PORT, port));
		}
	}
}
