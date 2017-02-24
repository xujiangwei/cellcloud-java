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

	/** 线程池执行器。 */
	private ExecutorService executor;

	/** 傀儡 Cellet 对应的沙盒。键为 Cellet 的标识。 */
	private ConcurrentHashMap<String, CelletSandbox> sandboxes;

	/** 是否使用散列码路由算法。 */
	private boolean hashRouting = true;

	/** 下位机列表。 */
	private ArrayList<Slave> slaves;
	/** 在线的下位机列表。 */
	private Vector<Slave> onlineSlaves;

	/** 路由表。 */
	private RoutingTable routingTable;

	/** 代理访问。代理访问通过拦截数据包的方式依据路由表信息来转发数据包。 */
	private ProxyForwarder forwarder;

	/** 傀儡 Cellet 映射。 */
	private ConcurrentHashMap<String, PuppetCellet> puppetCelletMap;

	/** 当前服务器的会话核心。 */
	protected TalkServiceKernel serverKernel;

	/**
	 * 构造函数。
	 * 
	 * @param sandboxes 与内核共享的沙盒映射。
	 */
	public GatewayService(ConcurrentHashMap<String, CelletSandbox> sandboxes) {
		this.executor = Executors.newCachedThreadPool();
		this.sandboxes = sandboxes;
		this.slaves = new ArrayList<Slave>();
		this.onlineSlaves = new Vector<Slave>();
		this.routingTable = new RoutingTable();
		this.forwarder = new ProxyForwarder(this.routingTable, this.executor);
		this.puppetCelletMap = new ConcurrentHashMap<String, PuppetCellet>();
	}

	/**
	 * 添加下位机。
	 * 
	 * @param address 指定下位机的访问地址。
	 * @param celletIdentifiers 指定需要代理的下位机上的 Cellet 标识清单。
	 */
	public void addSlave(InetSocketAddress address, List<String> celletIdentifiers) {
		Slave slave = new Slave(address, celletIdentifiers);
		this.slaves.add(slave);
	}

	/**
	 * 移除下位机。
	 * 
	 * @param slave 指定需移除的下位机。
	 */
	public void removeSlave(Slave slave) {
		this.slaves.remove(slave);

		// 移除在线的下位机
		if (this.onlineSlaves.remove(slave)) {
			slave.kernel.shutdown();
			this.routingTable.remove(slave);
		}
	}

	/**
	 * 设置 Talk 服务核心。
	 * 
	 * @param kernel 指定 Talk 服务核心。
	 */
	public void setTalkServiceKernel(TalkServiceKernel kernel) {
		this.serverKernel = kernel;
	}

	/**
	 * 设置路由规则。
	 * <p>
	 * 支持的规则有 "Hash" 和 "Balance" 两种。
	 * "Hash" 规则通过计算终端的散列码建立路由，"Balance" 规则通过平衡每个下位机的连接数建立路由。
	 * 
	 * @param rule 指定规则。
	 */
	public void setRoutingRule(String rule) {
		if (rule.equalsIgnoreCase("Hash")) {
			this.hashRouting = true;
		}
		else {
			this.hashRouting = false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
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

				// 创建对应的 Cellet 傀儡
				for (String identifier : slave.celletIdentifiers) {
					this.getCellet(identifier);
				}

				Logger.i(this.getClass(), "Gateway slave: " + slave.address.getHostString() + ":" + slave.address.getPort());
			}
			else {
				Logger.e(this.getClass(), "Call cellets failed: " + slave.address.getHostString() + ":"
						+ slave.address.getPort() + " - " + slave.celletIdentifiers.toString());
			}
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdown() {
		for (int i = 0; i < this.slaves.size(); ++i) {
			this.slaves.get(i).kernel.stopDaemon();
		}

		this.serverKernel.setInterceptor(null, null);

		this.slaves.clear();
		this.onlineSlaves.clear();
		this.routingTable.clear();
		this.puppetCelletMap.clear();

		this.executor.shutdown();
	}

	/**
	 * 获得指定标识的 Cellet 。
	 * 
	 * @param identifier 指定 Cellet 的标识。
	 * @return 返回 Cellet 实例。
	 */
	protected Cellet getCellet(String identifier) {
		PuppetCellet cellet = this.puppetCelletMap.get(identifier);
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
			this.puppetCelletMap.put(identifier.toString(), cellet);

			Logger.i(this.getClass(), "Create puppet cellet: " + identifier);
		}

		return cellet;
	}

	/**
	 * 获得指定 Cellet 标识的沙盒。
	 * 
	 * @param identifier 指定 Cellet 标识。
	 * @return 返回 Cellet 对应的沙盒。
	 */
	protected CelletSandbox getSandbox(String identifier) {
		return this.sandboxes.get(identifier);
	}

	/**
	 * 更新路由信息。
	 * 
	 * @param session 指定需更新的会话。
	 * @param tag 指定会话对应的内核标签。
	 * @param identifier 指定该会话需要访问的 Cellet 标识。
	 * @return 返回指定 Cellet 标识的 Cellet 实例。
	 */
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
	 * @param slave 指定需添加的下位机。
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
	 * @param slave 指定需移除的下位机。
	 */
	protected void removeOnlineSlave(Slave slave) {
		slave.state = SlaveState.Offline;
		this.onlineSlaves.remove(slave);
		slave.clear();
	}

	/**
	 * 创建指定标识的傀儡 Cellet 。
	 * 
	 * @param identifier 指定标识。
	 * @return 返回 Cellet 实例。
	 */
	private PuppetCellet createCellet(String identifier) {
		CelletVersion version = new CelletVersion(1, 0, 0);
		PuppetCellet cellet = new PuppetCellet(new CelletFeature(identifier.toString(), version));
		return cellet;
	}

	/**
	 * 下位节点信息。
	 */
	public class Slave {

		/** 下位机使用的 Talk 核心。 */
		public TalkServiceKernel kernel;
		/** 下位机的目标地址。 */
		public InetSocketAddress address;
		/** 目标 Cellet 列表。 */
		public ArrayList<String> celletIdentifiers;
		/** Talk 监听器。 */
		public ProxyTalkListener listener;
		/** 状态。 */
		public SlaveState state = SlaveState.Unknown;

		/** 下位机运行时会话列表。键为会话对应的内核标签。 */
		private ConcurrentHashMap<String, Session> runtimeSessions;

		/**
		 * 构造函数。
		 * 
		 * @param address 指定目标地址。
		 * @param celletIdentifiers 指定需要请求的 Cellet 清单。
		 */
		public Slave(InetSocketAddress address, List<String> celletIdentifiers) {
			this.kernel = new TalkServiceKernel(null);
			this.address = address;
			this.celletIdentifiers = new ArrayList<String>(celletIdentifiers);
			this.listener = new ProxyTalkListener(GatewayService.this, this);
			this.kernel.addListener(this.listener);

			this.runtimeSessions = new ConcurrentHashMap<String, Session>();
		}

		/**
		 * 添加被管理的会话。
		 * 
		 * @param tag 指定会话的内核标签。
		 * @param session 指定会话实例。
		 */
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
