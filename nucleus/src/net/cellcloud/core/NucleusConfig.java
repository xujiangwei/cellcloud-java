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

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 内核参数配置描述。
 * 
 * @author Ambrose Xu
 * 
 */
public final class NucleusConfig {

	/** 内核标签。 */
	public String tag = null;

	/** 内核的工作角色。 */
	public Role role = Role.NODE;

	/** 内核运行的设备类型。 */
	public Device device = Device.SERVER;

	/** 是否启用 HTTP 服务器。 */
	public boolean httpd = true;

	/** Talk Service 配置。 */
	public TalkConfig talk;

	/** Gateway Service 配置。 */
	public GatewayConfig gateway;

	/** 集群配置。 */
	public ClusterConfig cluster;

	/**
	 * 构造函数。
	 */
	public NucleusConfig() {
		this.talk = new TalkConfig();
		this.gateway = new GatewayConfig();
		this.cluster = new ClusterConfig();
	}

	/**
	 * 会话服务器配置项。
	 */
	public final class TalkConfig {
		/** 是否启用 Talk 服务。 */
		public boolean enabled = true;

		/** 服务端口。 */
		public int port = 7000;

		/** 服务器使用的每个会话的 Block 区块大小。 */
		public int block = 65536;

		/** 最大连接数。 */
		public int maxConnections = 5000;

		/** 工作线程数。 */
		public int numWorkerThreads = 8;

		/** 是否使用 HTTP 服务。 */
		public boolean httpEnabled = true;

		/** HTTP 服务端口号。 */
		public int httpPort = 7070;

		/** HTTPS 服务端口号。 */
		public int httpsPort = 7080;

		/** HTTP 连接队列长度。 */
		public int httpQueueSize = 2000;

		/** HTTP 服务会话超时时间，单位：毫秒，默认 5 分钟。 */
		public long httpSessionTimeout = 5L * 60L * 1000L;

		/** JKS 文件路径。 */
		public String keystore = "/nucleus.jks";

		/** JKS 相关的 Password 。 */
		public String keyStorePassword = null;
		/** JKS 相关的 Password 。 */
		public String keyManagerPassword = null;

		private TalkConfig() {
		}
	}

	/**
	 * 网关服务配置项。
	 */
	public final class GatewayConfig {

		/** 网关路由规则。 */
		public String routingRule = "Hash";

		/** 下位机地址列表。 */
		public List<String> slaveHostList = null;

		/** 下位机端口列表。 */
		public List<Integer> slavePortList = null;

		/** 下位机对应的 HTTP 端口列表。 */
		public List<Integer> slaveHttpPortList = null;

		/** 代理的 Cellet 识别串列表。 */
		public List<String> celletIdentifiers = null;

		/** 代理的 HTTP 请求的 URI 。 */
		public List<String> httpURIList = null;

		private GatewayConfig() {
		}
	}

	/**
	 * 集群配置项。
	 */
	public final class ClusterConfig {
		/** 是否启用集群。 */
		public boolean enabled = false;

		/** 集群绑定主机名或地址。 */
		public String host = "127.0.0.1";

		/** 集群服务首选端口。 */
		public int preferredPort = 11099;

		/** 虚拟节点数量。 */
		public int numVNode = 3;

		/** 集群地址表。 */
		public List<InetSocketAddress> addressList = null;

		/** 是否自动扫描地址。 */
		public boolean autoScan = false;

		private ClusterConfig() {
		}
	}

}
