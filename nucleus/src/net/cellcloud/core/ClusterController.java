/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2013 Cell Cloud Team (cellcloudproject@gmail.com)

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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Service;

/** 集群控制器。
 * 
 * @author Jiangwei Xu
 */
public final class ClusterController implements Service {

	private Timer timer;
	private EndpointNode self;	
	private ClusterNetwork network;

	public ClusterController() {
		this.network = new ClusterNetwork();
	}

	@Override
	public boolean startup() {
		InetAddress addr = null;
		try {
			addr = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			Logger.logException(e, LogLevel.ERROR);
			return false;
		}

		if (!this.network.startup()) {
			Logger.e(this.getClass(), "Error in ClusterNetwork::startup()");
			return false;
		}

		// 生成表示自己的节点
		this.self = new EndpointNode(this.network.getPort(), addr);
		// 记录节点信息
		Logger.i(this.getClass(), new StringBuilder("Cluster node local hash code : ")
			.append(this.self.getHashCode()).toString());

		// 启动定时器，每 5 分钟扫描网络一次
		if (null != this.timer) {
			this.timer.cancel();
		}

		this.timer = new Timer();
		this.timer.schedule(new TimerTask(){
			@Override
			public void run() {
				// 扫描网络
				network.scanNetwork();
			}
		}, 10 * 1000, 5 * 60 * 1000);

		return true;
	}

	@Override
	public void shutdown() {
		if (null != this.timer) {
			this.timer.cancel();
			this.timer = null;
		}

		this.network.shutdown();
	}
}
