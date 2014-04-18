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

package net.cellcloud.core;

/** Cellet 特性描述。
 * 
 * @author Jiangwei Xu
 */
public final class CelletFeature {

	/// 服务标识
	private String identifier;
	private CelletVersion version;

	/** 构造函数。
	 */
	public CelletFeature(String identifier, CelletVersion version) {
		this.identifier = identifier;
		this.version = version;
	}

	/** 返回 Cellet 服务标识。
	 * 服务标识唯一标识网络中的 Cellet 服务集群。
	 */
	public String getIdentifier() {
		return this.identifier;
	}

	/** 返回 Cellet 版本。
	 */
	public CelletVersion getVersion() {
		return this.version;
	}
}
