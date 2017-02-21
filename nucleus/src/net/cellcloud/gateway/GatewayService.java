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

package net.cellcloud.gateway;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import net.cellcloud.common.Service;
import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.TalkListener;
import net.cellcloud.talk.TalkService;
import net.cellcloud.talk.TalkServiceFailure;

/**
 * 网关服务。
 * 
 * @author Ambrose Xu
 *
 */
public class GatewayService implements Service, TalkListener {

	private ArrayList<InetSocketAddress> slaveAddress;
	private ArrayList<ArrayList<String>> slaveCellets;

	private RoutingTable routingTable;

	private ProxyForwarder forwarder;

	public GatewayService() {
		this.slaveAddress = new ArrayList<InetSocketAddress>();
		this.slaveCellets = new ArrayList<ArrayList<String>>();
		this.routingTable = new RoutingTable();
		this.forwarder = new ProxyForwarder(this.routingTable);
	}

	@Override
	public boolean startup() {
		// 添加监听器
		TalkService.getInstance().addListener(this);

		// 设置数据拦截器
		TalkService.getInstance().setInterceptor(this.forwarder);

		return true;
	}

	@Override
	public void shutdown() {
		// 删除监听器
		TalkService.getInstance().removeListener(this);
	}

	@Override
	public void dialogue(String identifier, Primitive primitive) {
		
	}

	@Override
	public void contacted(String identifier, String tag) {
		
	}

	@Override
	public void quitted(String identifier, String tag) {
		
	}

	@Override
	public void failed(String tag, TalkServiceFailure failure) {
		
	}

}
