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

import net.cellcloud.exception.CelletSandboxException;

/**
 * Celle 沙箱。
 * 
 * @author Ambrose Xu
 * 
 */
public final class CelletSandbox {

	/** 是否封闭。封闭后才能被内核正确识别。 */
	private volatile boolean sealed = false;

	/** Cellet 特性。 */
	protected CelletFeature feature;

	/**
	 * 构造函数。
	 * 
	 * @param feature 指定被管理的 Cellet 特性。
	 */
	public CelletSandbox(CelletFeature feature) {
		this.feature = feature;
	}

	/**
	 * 封闭沙箱。
	 * 
	 * @param feature 指定验证用的特性。
	 */
	public void sealOff(CelletFeature feature) throws CelletSandboxException {
		if (this.sealed) {
			return;
		}

		if (this.feature != feature) {
			throw new CelletSandboxException("Repeat seal off sandbox");
		}

		this.sealed = true;
	}

	/**
	 * 是否被正确封闭。
	 * 
	 * @return 返回是否被正确封闭。
	 */
	protected boolean isSealed() {
		return this.sealed;
	}

}
