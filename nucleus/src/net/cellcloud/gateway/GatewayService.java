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

	private boolean hashRouting = true;

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

	public void removeSlave(Slave slave) {
		this.slaves.remove(slave);
		this.onlineSlaves.remove(slave);
	}

	public void setTalkServiceKernel(TalkServiceKernel kernel) {
		this.serverKernel = kernel;
	}

	public void setRoutingRule(String rule) {
		if (rule.equalsIgnoreCase("Hash")) {
			this.hashRouting = true;
		}
		else {
			this.hashRouting = false;
		}
	}

	@Override
	public boolean startup() {
		if (this.slaves.isEmpty()) {
			return false;
		}

		// 设置消息拦截器
		this.serverKernel.setInterceptor(this.forwarder, this.forwarder);

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
			else {
				Logger.e(this.getClass(), "Call cellets failed: " + slave.address.getHostString() + ":"
						+ slave.address.getPort() + " - " + slave.celletIdentifiers.toString());
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

		this.serverKernel.setInterceptor(null, null);

		this.slaves.clear();
		this.onlineSlaves.clear();
		this.routingTable.clear();
		this.proxyCelletMap.clear();

		this.executor.shutdown();
	}

	protected synchronized Cellet getCellet(String identifier) {
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

	public Cellet updateRouting(Session session, String tag, String identifier) {
		if (this.onlineSlaves.isEmpty()) {
			Logger.e(this.getClass(), "No online slave to working.");
			return null;
		}

		// 计算当前 Session 路由
		Slave slave = null;
		if (this.hashRouting) {
			// 计算远端主机地址字符串形式 Hash 值，然后进行取模，以模数作为索引分配 Slave
			int mod = Math.abs(session.getAddress().getHostString().hashCode()) % this.onlineSlaves.size();
			slave = this.onlineSlaves.get(mod);
		}
		else {
			// 查找通信链路数量最小的下位机
			int value = Integer.MAX_VALUE;
			for (int i = 0, size = this.onlineSlaves.size(); i < size; ++i) {
				Slave candidate = this.onlineSlaves.get(i);
				if (candidate.numSessions() < value) {
					value = candidate.numSessions();
					slave = candidate;
				}
			}
		}

		if (null == slave) {
			Logger.e(this.getClass(), "Can NOT find slave for " + session.getAddress().getHostString() + ":" + session.getAddress().getPort());
			return null;
		}

		// 更新路由表
		this.routingTable.update(session, tag, slave, identifier);

		// 添加目标
		slave.addSession(tag, session);

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

		return this.getCellet(identifier);
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
		slave.clear();
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

		private ConcurrentHashMap<String, Session> runtimeSessions;

		public Slave(InetSocketAddress address, List<String> celletIdentifiers) {
			this.kernel = new TalkServiceKernel(null);
			this.address = address;
			this.celletIdentifiers = new ArrayList<String>(celletIdentifiers);
			this.listener = new ProxyTalkListener(GatewayService.this, this);
			this.kernel.addListener(this.listener);

			this.runtimeSessions = new ConcurrentHashMap<String, Session>();
		}

		public void addSession(String tag, Session session) {
			this.runtimeSessions.put(tag, session);
		}

		public void removeSession(String tag) {
			this.runtimeSessions.remove(tag);
		}

		public int numSessions() {
			return this.runtimeSessions.size();
		}

		public void clear() {
			this.runtimeSessions.clear();
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
