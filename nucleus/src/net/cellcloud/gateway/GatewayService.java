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
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Service;
import net.cellcloud.common.Session;
import net.cellcloud.core.Cellet;
import net.cellcloud.core.CelletFeature;
import net.cellcloud.core.CelletSandbox;
import net.cellcloud.core.CelletVersion;
import net.cellcloud.core.Nucleus;
import net.cellcloud.exception.CelletSandboxException;
import net.cellcloud.talk.TalkCapacity;
import net.cellcloud.talk.TalkServiceKernel;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 网关服务。
 * 
 * @author Ambrose Xu
 *
 */
public class GatewayService implements Service {

	private ExecutorService executor;

	private ConcurrentHashMap<String, CelletSandbox> sandboxes;

	private ArrayList<Slave> slaves;
	private Vector<Slave> onlineSlaves;

	private RoutingTable routingTable;

	private ProxyForwarder forwarder;

	private ConcurrentHashMap<String, PuppetCellet> proxyCelletMap;

	protected TalkServiceKernel serverKernel;

	public GatewayService(ConcurrentHashMap<String, CelletSandbox> sandboxes) {
		this.executor = Executors.newCachedThreadPool();
		this.sandboxes = sandboxes;
		this.slaves = new ArrayList<Slave>();
		this.onlineSlaves = new Vector<Slave>();
		this.routingTable = new RoutingTable();
		this.forwarder = new ProxyForwarder(this.routingTable, this.executor);
		this.proxyCelletMap = new ConcurrentHashMap<String, PuppetCellet>();
	}

	public void addSlave(InetSocketAddress address, List<String> celletIdentifiers) {
		Slave slave = new Slave(address, celletIdentifiers);
		this.slaves.add(slave);
	}

	public void setTalkServiceKernel(TalkServiceKernel kernel) {
		this.serverKernel = kernel;
	}

	@Override
	public boolean startup() {
		if (this.slaves.isEmpty()) {
			return false;
		}

		// 设置消息拦截器
		this.serverKernel.setInterceptor(this.forwarder);

		// 初始化 Call
		for (int i = 0; i < this.slaves.size(); ++i) {
			Slave slave = this.slaves.get(i);
			// 启动会话服务
			slave.kernel.startDaemon();

			// 设置为代理模式
			TalkCapacity capacity = new TalkCapacity(false, Integer.MAX_VALUE, 10000L, true);
			// call cellet
			if (slave.kernel.call(slave.celletIdentifiers, slave.address, capacity)) {
				// 设置代理监听器
				slave.kernel.getSpeaker(slave.celletIdentifiers.get(0)).setProxyListener(slave.listener);
			}

			Logger.i(this.getClass(), "Gateway slave: " + slave.address.getHostString() + ":" + slave.address.getPort());
		}

		return true;
	}

	@Override
	public void shutdown() {
		for (int i = 0; i < this.slaves.size(); ++i) {
			this.slaves.get(i).kernel.stopDaemon();
		}

		this.serverKernel.setInterceptor(null);

		this.slaves.clear();
		this.onlineSlaves.clear();
		this.routingTable.clear();
		this.proxyCelletMap.clear();

		this.executor.shutdown();
	}

	public synchronized Cellet getCellet(String identifier) {
		PuppetCellet cellet = this.proxyCelletMap.get(identifier);
		if (null == cellet) {
			cellet = this.createCellet(identifier);
			// 创建沙箱
			CelletSandbox sandbox = new CelletSandbox(cellet.getFeature());
			try {
				sandbox.sealOff(cellet.getFeature());
			} catch (CelletSandboxException e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			}
			this.sandboxes.put(identifier, sandbox);
			// 添加 Cellet
			this.proxyCelletMap.put(identifier.toString(), cellet);
		}

		return cellet;
	}

	protected CelletSandbox getSandbox(String identifier) {
		return this.sandboxes.get(identifier);
	}

	public void updateRouting(Session session, String tag, String identifier) {
		// 计算当前 Session 路由的目标 Slave
		// 计算远端主机地址字符串形式 Hash 值，然后进行取模，以模数作为索引分配 Slave
		int mod = Math.abs(session.getAddress().getHostString().hashCode()) % this.onlineSlaves.size();
		Slave slave = this.onlineSlaves.get(mod);

		// 更新路由器表
		this.routingTable.update(session, tag, slave, identifier);

		JSONObject proxy = new JSONObject();
		try {
			proxy.put("proxy", Nucleus.getInstance().getTagAsString());
			proxy.put("sid", session.getId().longValue());
			proxy.put("tag", tag.toString());
			proxy.put("identifier", identifier.toString());
			proxy.put("active", true);
		} catch (JSONException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
		}

		// 发送代理请求
		slave.kernel.proxy(slave.celletIdentifiers.get(0), proxy);

		synchronized (slave.kernel) {
			try {
				slave.kernel.wait(1000L);
			} catch (InterruptedException e) {
				Logger.log(this.getClass(), e, LogLevel.WARNING);
			}
		}
	}

	/**
	 * 添加在线工作的下位节点。
	 * 
	 * @param slave
	 */
	protected void addOnlineSlave(Slave slave) {
		if (this.onlineSlaves.contains(slave)) {
			return;
		}

		slave.state = SlaveState.Online;
		this.onlineSlaves.add(slave);
	}

	/**
	 * 移除在线工作的下位节点。
	 * 
	 * @param slave
	 */
	protected void removeOnlineSlave(Slave slave) {
		slave.state = SlaveState.Offline;
		this.onlineSlaves.remove(slave);
	}

	private PuppetCellet createCellet(String identifier) {
		CelletVersion version = new CelletVersion(1, 0, 0);
		PuppetCellet cellet = new PuppetCellet(new CelletFeature(identifier.toString(), version));
		return cellet;
	}

	/**
	 * 下位节点信息。
	 */
	public class Slave {

		public TalkServiceKernel kernel;
		public InetSocketAddress address;
		public ArrayList<String> celletIdentifiers;
		public ProxyTalkListener listener;
		public SlaveState state = SlaveState.Unknown;

		public Slave(InetSocketAddress address, List<String> celletIdentifiers) {
			this.kernel = new TalkServiceKernel(null);
			this.address = address;
			this.celletIdentifiers = new ArrayList<String>(celletIdentifiers);
			this.listener = new ProxyTalkListener(GatewayService.this, this);
			this.kernel.addListener(this.listener);
		}
	}

	/**
	 * 下位机状态。
	 */
	public enum SlaveState {
		Online,

		Offline,

		Unknown
	}

}
