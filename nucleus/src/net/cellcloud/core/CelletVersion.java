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

package net.cellcloud.core;

/**
 * Cellet 版本描述类。
 * 
 * @author Ambrose Xu
 * 
 */
public class CelletVersion {

	/** 主版本号。 */
	private int major;
	/** 副版本号。 */
	private int minor;
	/** 修订号。 */
	private int revision;

	/**
	 * 构造函数。
	 * 
	 * @param major 指定主版本号。
	 * @param minor 指定副版本号。
	 */
	public CelletVersion(int major, int minor) {
		this.major = major;
		this.minor = minor;
		this.revision = 0;
	}

	/**
	 * 构造函数。
	 * 
	 * @param major 指定主版本号。
	 * @param major 指定副版本号。
	 * @param revision 指定修订号。
	 */
	public CelletVersion(int major, int minor, int revision) {
		this.major = major;
		this.minor = minor;
		this.revision = revision;
	}

	/**
	 * 获得主版本号。
	 */
	public int getMajor() {
		return this.major;
	}

	/**
	 * 获得副版本号。
	 */
	public int getMinor() {
		return this.minor;
	}

	/**
	 * 获得修订号。
	 */
	public int getRevision() {
		return this.revision;
	}

	/**
	 * 返回字符串格式的版本信息。
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.major);
		builder.append(".");
		builder.append(this.minor);
		builder.append(".");
		builder.append(this.revision);
		String ret = builder.toString();
		builder = null;
		return ret;
	}

}
