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

import java.util.List;

import net.cellcloud.core.Endpoint;
import net.cellcloud.talk.dialect.Dialect;

/**
 * 适配器监听器。
 * 
 * @author Ambrose Xu
 * 
 */
public interface AdapterListener {

	/**
	 * 当收到对端分享的方言时被回调。
	 * 
	 * @param adapter 发生此回调事件的适配器实例。
	 * @param endpoint 发送该方言的终端。
	 * @param dialect 方言数据。
	 */
	public void onShared(Adapter adapter, Endpoint endpoint, Dialect dialect);

	/**
	 * 当发送数据发生拥塞时被回调。
	 * 
	 * @param adapter 发生此回调事件的适配器实例。
	 * @param destination 导致此次拥塞发生时数据发送的目标终端。
	 * @param dataSize 导致此次拥塞时的数据大小。
	 */
	public void onCongested(Adapter adapter, Endpoint destination, int dataSize);

	/**
	 * 当外联节点发生故障时被回调。
	 * 
	 * @param adapter 发生此回调事件的适配器实例。
	 * @param endpoint 发生故障的终端。
	 * @param faultKeywords 由此故障可能影响的关键字列表。
	 */
	public void onEndpointFailure(Adapter adapter, Endpoint endpoint, List<String> faultKeywords);

}
