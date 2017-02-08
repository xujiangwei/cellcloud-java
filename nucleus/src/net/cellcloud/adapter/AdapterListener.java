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

package net.cellcloud.adapter;

import net.cellcloud.core.Endpoint;
import net.cellcloud.talk.dialect.Dialect;

/**
 * 
 * @author Ambrose Xu
 */
public interface AdapterListener {

	/**
	 * 当收到对端分享的方言时被回调。
	 * 
	 * @param adapter
	 * @param endpoint
	 * @param dialect
	 */
	public void onShared(Adapter adapter, Endpoint endpoint, Dialect dialect);

	/**
	 * 当发送数据发生拥塞时被回调。
	 * 
	 * @param adapter
	 * @param destination
	 * @param dataSize
	 */
	public void onCongested(Adapter adapter, Endpoint destination, int dataSize);

}
