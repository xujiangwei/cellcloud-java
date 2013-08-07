/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2013 Cell Cloud Team (www.cellcloud.net)

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

/** 内核参数配置描述。
 * 
 * @author Jiangwei Xu
 */
public final class NucleusConfig {

	/** 内核工作角色定义。
	 */
	public final class Role {

		/// 计算。
		/// 内核启动标准的 Talk 服务和 Cellet 管理器。
		public static final byte NODE = 0x01;

		/// 存储。
		/// 内核启动存储管理器。
		public static final byte STORAGE = 0x02;

		/// 网关。
		/// 内核启动标准的 Talk 服务并启动代理模式。
		public static final byte GATE = 0x04;

		/// 消费。
		/// 内存启动 Talk 会话机制。
		public static final byte CONSUMER = 0x08;
	}

	/** 设备平台。
	 */
	public final class Device {
		/// 手机
		public static final byte PHONE = 1;

		/// 平板
		public static final byte TABLET = 3;

		/// 台式机
		public static final byte DESKTOP = 5;

		/// 服务器
		public static final byte SERVER = 7;
	}

	/// 自定义内核标签
	public String tag = null;

	/// 角色
	public byte role = Role.NODE;

	/// 设备
	public byte device = Device.SERVER;

	/// 是否启用 HTTP 服务器
	public boolean httpd = true;

	/// Talk Service 配置
	public TalkConfig talk;

	/// 集群配置
	public ClusterConfig cluster;

	public NucleusConfig() {
		this.talk = new TalkConfig();
		this.cluster = new ClusterConfig();
	}

	/**
	 * 会话服务器配置项。
	 */
	public final class TalkConfig {
		/// 是否启用 Talk 服务
		public boolean enable = true;

		/// Talk 服务端口
		public int port = 7000;

		/// 是否使用 HTTP 服务
		public boolean httpd = false;

		private TalkConfig() {
		}
	}

	/**
	 * 集群配置项。
	 */
	public final class ClusterConfig {
		/// 集群绑定主机名
		public String host = "127.0.0.1";

		/// 集群服务首选端口
		public int preferredPort = 11099;

		/// 虚拟节点数量
		public int numVNode = 3;

		/// 集群地址表
		public List<InetSocketAddress> addressList = null;

		/// 是否自动扫描地址
		public boolean autoScan = false;

		private ClusterConfig() {
		}
	}
}
