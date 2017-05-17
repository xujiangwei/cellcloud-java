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

package net.cellcloud;

/**
 * Nucleus 程序库的版本描述。
 * 
 * @author Ambrose Xu
 * 
 */
public final class Version {

	/** 主版本号。 */
	public static final int MAJOR = 1;

	/** 副版本号。 */
	public static final int MINOR = 6;

	/** 修订号。 */
	public static final int REVISION = 23;

	/** 版本名。 */
	public static final String NAME = "Xi";

	/** 版本串号。 */
	public static final int VERSION_NUMBER = 160;

	private Version() {
	}

	/**
	 * 获得字符串形式的版本描述。
	 * 
	 * @return 返回字符串形式的版本描述。
	 */
	public static String getNumbers() {
		StringBuilder buf = new StringBuilder();
		buf.append(MAJOR).append(".");
		buf.append(MINOR).append(".");
		buf.append(REVISION);
		return buf.toString();
	}

}
