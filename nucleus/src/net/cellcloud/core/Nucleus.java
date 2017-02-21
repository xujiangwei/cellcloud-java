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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.cellcloud.adapter.Adapter;
import net.cellcloud.adapter.AdapterListenerProfile;
import net.cellcloud.cluster.ClusterController;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.exception.CelletSandboxException;
import net.cellcloud.exception.SingletonException;
import net.cellcloud.gateway.GatewayService;
import net.cellcloud.http.HttpService;
import net.cellcloud.talk.TalkService;
import net.cellcloud.util.Clock;

/**
 * Cell Cloud 软件栈内核类。
 * 
 * @author Ambrose Xu
 * 
 */
public final class Nucleus {

	/* 用于测试的配置文件管理
	static {
		// 读取日志配置文件
		ClassLoader cl = Nucleus.class.getClassLoader();
		InputStream inputStream = null;
		if (cl != null) {
			inputStream = cl.getResourceAsStream("logging.properties");
		}
		else {
			inputStream = ClassLoader.getSystemResourceAsStream("loggging.properties");
		}
		java.util.logging.LogManager logManager = java.util.logging.LogManager.getLogManager();
		try {
			// 重新初始化日志属性并重新读取日志配置。
			logManager.readConfiguration(inputStream);
		} catch (SecurityException e) {
			System.err.println(e);
		} catch (IOException e) {
			System.err.println(e);
		}

		try {
			if (null != inputStream) {
				inputStream.close();
			}
		} catch (IOException e) {
			// Nothing
		}
    }*/

	private static Nucleus instance = null;

	/**
	 * 是否处于工作状态。
	 */
	private AtomicBoolean working = new AtomicBoolean(false);

	/**
	 * 内核标签。使用 UUID 规则。
	 */
	private NucleusTag tag = null;

	/**
	 * 内核配置信息。
	 */
	private NucleusConfig config = null;

	/**
	 * 内核上下文。
	 */
	private NucleusContext context = null;

	/**
	 * 集群网络控制器。
	 */
	private ClusterController clusterController = null;

	/**
	 * Talk 服务。
	 */
	private TalkService talkService = null;

	/**
	 * HTTP 协议服务。
	 */
	private HttpService httpService = null;

	/**
	 * 网关服务。
	 */
	private GatewayService gatewayService = null;

	/**
	 * Jar 包内的 Cellet 类映射表。
	 */
	private ConcurrentHashMap<String, List<String>> celletJarClasses = null;

	/**
	 * 文件路径下 Class 文件的 Cellet 类映射表。
	 */
	private ConcurrentHashMap<String, List<String>> celletPathClasses = null;

	/**
	 * 当前内核内的所有 Cellet 对照表。
	 */
	private ConcurrentHashMap<String, Cellet> cellets = null;

	/**
	 * Cellet 对应的沙盒表。
	 */
	private ConcurrentHashMap<String, CelletSandbox> sandboxes = null;

	/**
	 * 内核内适配器映射表。
	 */
	private ConcurrentHashMap<String, Adapter> adapters = null;

	/**
	 * 仅用于启动时反射适配器监听器的临时存储。
	 */
	private ArrayList<AdapterListenerProfile> tempAdapterListeners = null;

	/**
	 * 构造器。
	 * 
	 * @param config 指定配置信息。
	 */
	private Nucleus(NucleusConfig config) throws SingletonException {
		if (null == Nucleus.instance) {
			Nucleus.instance = this;
			// 设置配置
			this.config = config;

			// 生成标签
			if (null != config.tag) {
				this.tag = new NucleusTag(config.tag);
			}
			else {
				this.tag = new NucleusTag();
				if (this.config.role != Role.CONSUMER) {
					Logger.d(Nucleus.class, "Nucleus Warning: No nucleus tag setting, use random tag: " + this.tag.asString());
				}
			}

			this.context = new NucleusContext();
		}
		else {
			throw new SingletonException(Nucleus.class.getName());
		}
	}

	/**
	 * 创建实例。
	 * 
	 * @param config
	 * @return
	 * @throws SingletonException
	 */
	public static Nucleus createInstance(NucleusConfig config)
			throws SingletonException {
		return new Nucleus(config);
	}

	/** 返回单例。
	 *
	 * @return
	 */
	public static Nucleus getInstance() {
		return Nucleus.instance;
	}

	/** 返回内核标签。
	 */
	public NucleusTag getTag() {
		return this.tag;
	}

	/** 返回内核标签。
	 */
	public String getTagAsString() {
		return this.tag.asString();
	}

	/** 返回配置信息实例。
	 */
	public NucleusConfig getConfig() {
		return this.config;
	}

	/** 返回集群控制器实例。
	 */
	public ClusterController getClusterController() {
		return this.clusterController;
	}

	/** 返回 Talk Service 实例。
	 */
	public TalkService getTalkService() {
		return this.talkService;
	}

	/** 启动内核。
	 * 
	 * @return 如果返回 <code>true</code> 表示启动成功，否则表示启动失败。
	 */
	public boolean startup() {
		if (this.working.get()) {
			Logger.i(Nucleus.class, "*-*-* Cell Initialized *-*-*");
			return true;
		}

		Logger.i(Nucleus.class, "*-*-* Cell Initializing *-*-*");

		// 启动时钟
		Clock.start();

		if (this.config.role == Role.NODE || this.config.role == Role.GATEWAY) {
			// 角色：节点或网关

			//-------------------- 配置集群 --------------------

			if (this.config.cluster.enabled) {
				if (null == this.clusterController) {
					this.clusterController = new ClusterController(this.config.cluster.host
							, this.config.cluster.preferredPort, this.config.cluster.numVNode);
				}
				// 添加集群地址
				if (null != this.config.cluster.addressList) {
					this.clusterController.addClusterAddress(this.config.cluster.addressList);
				}
				// 设置自动扫描网络
				this.clusterController.autoScanNetwork = this.config.cluster.autoScan
						&& (this.config.device == Device.SERVER || this.config.device == Device.DESKTOP);
				// 启动集群控制器
				if (this.clusterController.startup()) {
					Logger.i(Nucleus.class, "Starting cluster controller service success.");
				}
				else {
					Logger.e(Nucleus.class, "Starting cluster controller service failure.");
				}
			}

			//-------------------- 配置 HTTP 服务 --------------------

			if (this.config.httpd) {
				// 创建 HTTP Service
				try {
					this.httpService = new HttpService(this.context);
				} catch (SingletonException e) {
					Logger.log(Nucleus.class, e, LogLevel.WARNING);
				}
			}

			//-------------------- 配置 Talk Service --------------------

			// 创建 Talk Service
			if (this.config.talk.enabled && (null == this.talkService)) {
				try {
					this.talkService = new TalkService(this.context);
				} catch (SingletonException e) {
					Logger.log(Nucleus.class, e, LogLevel.ERROR);
					return false;
				}
			}

			if (this.config.talk.enabled) {
				// 设置服务端口号
				this.talkService.setPort(this.config.talk.port);
				// 设置 Block
				this.talkService.setBlockSize(this.config.talk.block);
				// 设置最大连接数
				this.talkService.setMaxConnections(this.config.talk.maxConnections);
				// 设置工作线程数
				this.talkService.setWorkerThreadNum(this.config.talk.numWorkerThreads);
				// 设置是否启用 HTTP 服务
				this.talkService.httpEnabled(this.config.talk.httpEnabled);
				// 设置 HTTP 端口号
				this.talkService.setHttpPort(this.config.talk.httpPort);
				// 设置 HTTPS 端口号
				this.talkService.setHttpsPort(this.config.talk.httpsPort);
				// 设置 HTTP 队列长度
				this.talkService.setHttpQueueSize(this.config.talk.httpQueueSize);
				// 设置 HTTP 会话超时时间
				this.talkService.settHttpSessionTimeout(this.config.talk.httpSessionTimeout);

				// 启动 Talk Service
				if (this.talkService.startup()) {
					Logger.i(Nucleus.class, "Starting talk service success.");
				}
				else {
					Logger.e(Nucleus.class, "Starting talk service failure.");
					return false;
				}
			}

			// 加载外部 Jar 包
			this.loadExternalJar();

			// 加载外部路径
			this.loadExternalPath();

			// 启动 Cellet
			this.activateCellets();

			// 启动适配器
			if (null != this.adapters) {
				for (Adapter adapter : this.adapters.values()) {
					adapter.setup();
				}
			}
			// 添加监听器
			if (null != this.tempAdapterListeners) {
				for (AdapterListenerProfile alp : this.tempAdapterListeners) {
					Adapter adapter = this.adapters.get(alp.instanceName);
					if (null != adapter) {
						adapter.addListener(alp.listener);
					}
					else {
						// 没有找到 Adapter
						Logger.w(this.getClass(), "Can NOT find adapter '" + alp.instanceName
								+ "' for listener '" + alp.listener.getClass().getName() + "'");
					}
					alp.listener = null;
					alp.instanceName = null;
				}
				this.tempAdapterListeners.clear();
				this.tempAdapterListeners = null;
			}

			// 启动 HTTP Service
			if (null != this.httpService) {
				// 尝试启动扩展模块
				if (this.config.talk.enabled) {
					this.talkService.startExtendHolder();
				}

				if (this.httpService.startup()) {
					Logger.i(Nucleus.class, "Starting http service success.");
				}
				else {
					Logger.i(Nucleus.class, "Starting http service failure.");
				}
			}

			// 如果是网关节点，启动网关服务
			if (Role.GATEWAY == this.config.role) {
				this.gatewayService = new GatewayService();

				if (this.gatewayService.startup()) {
					Logger.i(Nucleus.class, "Starting gateway service success.");
				}
				else {
					this.gatewayService = null;
					Logger.i(Nucleus.class, "Starting gateway service failure.");
				}
			}
		}
		else if (this.config.role == Role.CONSUMER) {
			// 角色：消费者
			if (null == this.talkService) {
				try {
					// 创建 Talk Service
					this.talkService = new TalkService(this.context);
				} catch (SingletonException e) {
					Logger.log(Nucleus.class, e, LogLevel.ERROR);
				}
			}

			// 启动守护线程
			this.talkService.startDaemon();
		}

		this.working.set(true);

		return true;
	}

	/** 关停内核。
	 */
	public void shutdown() {
		if (!this.working.get()) {
			Logger.i(Nucleus.class, "*-*-* Cell Finalized *-*-*");
			return;
		}

		Logger.i(Nucleus.class, "*-*-* Cell Finalizing *-*-*");

		this.working.set(false);
		
		if (this.config.role == Role.NODE || this.config.role == Role.GATEWAY) {
			// 角色：节点或网关

			// 关闭网关
			if (null != this.gatewayService) {
				this.gatewayService.shutdown();
			}

			// 关闭集群服务
			if (null != this.clusterController) {
				this.clusterController.shutdown();
			}

			// 停止所有 Cellet
			this.deactivateCellets();

			// 关闭 Talk Service
			if (null != this.talkService) {
				this.talkService.shutdown();
			}

			// 关闭 HTTP Service
			if (null != this.httpService) {
				this.httpService.shutdown();
			}

			// 关闭适配器
			if (null != this.adapters) {
				for (Adapter adapter : this.adapters.values()) {
					adapter.teardown();
				}
			}
		}
		else if (this.config.role == Role.CONSUMER) {
			// 角色：消费者
			this.talkService.stopDaemon();
		}

		// 关闭时钟
		Clock.stop();
	}

	/** 返回注册在该内核上的指定的 Cellet 。
	 * 
	 * @param identifier
	 * @param context
	 * @return
	 */
	public Cellet getCellet(String identifier, NucleusContext context) {
		if (null == this.cellets) {
			return null;
		}

		if (this.context == context) {
			return this.cellets.get(identifier);
		}

		return null;
	}

	/** 返回注册在该内核上的指定的 Cellet 。
	 * 
	 * @param identifier
	 * @return
	 */
	public Cellet getCellet(String identifier) {
		if (null == this.cellets) {
			return null;
		}

		return this.cellets.get(identifier);
	}

	/** 载入 Cellet JAR 包信息。
	 * 
	 * @param jarFile
	 * @param classes
	 */
	public void prepareCelletJar(String jarFile, List<String> classes) {
		if (null == this.celletJarClasses) {
			this.celletJarClasses = new ConcurrentHashMap<String, List<String>>();
		}

		this.celletJarClasses.put(jarFile, classes);
	}

	/** 载入 Cellet class 路径信息。
	 * 
	 * @param path
	 * @param classes
	 */
	public void prepareCelletPath(String path, List<String> classes) {
		if (null == this.celletPathClasses) {
			this.celletPathClasses = new ConcurrentHashMap<String, List<String>>();
		}

		this.celletPathClasses.put(path, classes);
	}

	/** 注册 Cellet 。
	 * 
	 * @param cellet
	 */
	public void registerCellet(Cellet cellet) {
		if (null == this.cellets) {
			this.cellets = new ConcurrentHashMap<String, Cellet>();
		}

		this.cellets.put(cellet.getFeature().getIdentifier(), cellet);
	}

	/** 注销 Cellet 。
	 * 
	 * @param cellet
	 */
	public void unregisterCellet(Cellet cellet) {
		if (null == this.cellets) {
			return;
		}

		this.cellets.remove(cellet.getFeature().getIdentifier());
	}

	/** 返回所有 Cellet 的 Feature 列表。
	 *
	 * @return
	 */
	public List<CelletFeature> getCelletFeatures() {
		if (null == this.cellets) {
			return null;
		}

		ArrayList<CelletFeature> list = new ArrayList<CelletFeature>(this.cellets.size());
		for (Cellet c : this.cellets.values()) {
			list.add(c.getFeature());
		}
		return list;
	}

	/** 查询并返回内核上下文。
	 */
	public boolean checkSandbox(final Cellet cellet, final CelletSandbox sandbox) {
		if (null == this.sandboxes) {
			return false;
		}

		// 判断是否是使用自定义的沙箱进行检查
		if (cellet.getFeature() != sandbox.feature || !sandbox.isSealed()) {
			return false;
		}

		CelletSandbox sb = this.sandboxes.get(sandbox.feature.getIdentifier());
		if (null != sb && sandbox == sb) {
			return true;
		}

		return false;
	}

	/** 返回指定实例名的适配器。
	 * 
	 * @param instanceName
	 */
	public Adapter getAdapter(String instanceName) {
		if (null != this.adapters) {
			return this.adapters.get(instanceName);
		}

		return null;
	}

	/** 添加适配器。支持热拔插。
	 * 
	 * @param adapter
	 */
	public void addAdapter(Adapter adapter) {
		if (null == this.adapters) {
			this.adapters = new ConcurrentHashMap<String, Adapter>();
		}

		this.adapters.put(adapter.getInstanceName(), adapter);

		if (this.working.get()) {
			adapter.setup();
		}
	}

	/** 移除适配器。支持热拔插。
	 * 
	 * @param adapter
	 */
	public void removeAdapter(Adapter adapter) {
		if (null == this.adapters) {
			return;
		}

		this.adapters.remove(adapter.getInstanceName());

		if (this.working.get()) {
			adapter.teardown();
		}
	}

	/** 返回内核实时快照。
	 * 
	 * @return
	 */
	public NucleusSnapshoot snapshoot() {
		NucleusSnapshoot snapshoot = new NucleusSnapshoot();
		snapshoot.tag = this.tag.asString().toString();
		snapshoot.systemStartTime = Clock.startTime();
		snapshoot.systemDuration = Clock.currentTimeMillis() - Clock.startTime();

		if (null != this.cellets) {
			snapshoot.celletList = new ArrayList<String>(this.cellets.size());
			for (Cellet cellet : this.cellets.values()) {
				snapshoot.celletList.add(cellet.getFeature().getIdentifier());
			}
		}

		snapshoot.talk = this.talkService.snapshot();

		return snapshoot;
	}

	/** 为启动 Cellet 进行准备操作。
	 * 
	 * @param cellet
	 * @param sandbox
	 */
	protected synchronized void prepareCellet(Cellet cellet, CelletSandbox sandbox) {
		if (null == this.sandboxes) {
			this.sandboxes = new ConcurrentHashMap<String, CelletSandbox>();
		}

		// 如果已经保存了沙箱则不能更新新的沙箱
		if (this.sandboxes.containsKey(cellet.getFeature().getIdentifier())) {
			Logger.w(Nucleus.class, "Contains same cellet sandbox - Cellet:" + cellet.getFeature().getIdentifier());
			return;
		}

		try {
			// 封闭沙箱，防止不合规的 Cellet 加载流程
			sandbox.sealOff(cellet.getFeature());
			this.sandboxes.put(cellet.getFeature().getIdentifier(), sandbox);
		} catch (CelletSandboxException e) {
			Logger.log(Nucleus.class, e, LogLevel.ERROR);
		}
	}

	/** 加载外部 Jar 文件，实例化 Cellet
	 */
	private void loadExternalJar() {
		if (null == this.celletJarClasses) {
			return;
		}

		if (null == this.cellets) {
			this.cellets = new ConcurrentHashMap<String, Cellet>();
		}

		// 遍历配置数据
		Iterator<String> iter = this.celletJarClasses.keySet().iterator();
		while (iter.hasNext()) {
			// Jar 文件名
			final String jarFilename = iter.next();

			// 判断文件是否存在
			File file = new File(jarFilename);
			if (!file.exists()) {
				Logger.w(Nucleus.class, "Jar file '"+ jarFilename +"' is not exists!");
				file = null;
				continue;
			}

			// 生成类列表
			ArrayList<String> classNameList = new ArrayList<String>();
			JarFile jarFile = null;
			try {
				jarFile = new JarFile(jarFilename);

				Logger.i(Nucleus.class, "Analysing jar file : " + jarFile.getName());

				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					String name = entries.nextElement().getName();
					if (name.endsWith("class")) {
						// 将 net/cellcloud/MyObject.class 转为 net.cellcloud.MyObject
						name = name.replaceAll("/", ".").substring(0, name.length() - 6);
						classNameList.add(name);
					}
				}
			} catch (IOException ioe) {
				Logger.log(Nucleus.class, ioe, LogLevel.WARNING);
				continue;
			} finally {
				try {
					jarFile.close();
				} catch (Exception e) {
					// Nothing
				}
			}

			// 定位文件
			URL url = null;
			try {
				url = new URL(file.toURI().toURL().toString());
			} catch (MalformedURLException e) {
				Logger.log(Nucleus.class, e, LogLevel.WARNING);
				continue;
			}

			// 加载 Class
			URLClassLoader loader = null;
			try {
				loader = new URLClassLoader(new URL[]{url}
					, Thread.currentThread().getContextClassLoader());

				// 取出 Cellet 类
				List<String> celletClasslist = this.celletJarClasses.get(jarFilename);
				// Cellet 类列表
				ArrayList<Class<?>> classes = new ArrayList<Class<?>>();

				// 加载所有的 Class
				for (int i = 0, size = classNameList.size(); i < size; ++i) {
					try {
						String className = classNameList.get(i);
						Class<?> clazz = loader.loadClass(className);
						if (celletClasslist.contains(className)) {
							classes.add(clazz);
						}

						// 分析 class
						this.analyzeClass(clazz);
					} catch (ClassNotFoundException e) {
						Logger.log(Nucleus.class, e, LogLevel.ERROR);
					}
				}

				for (int i = 0, size = classes.size(); i < size; ++i) {
					try {
						Class<?> clazz = classes.get(i);
						// 实例化 Cellet
						Cellet cellet = (Cellet) clazz.newInstance();
						// 存入列表
						this.cellets.put(cellet.getFeature().getIdentifier(), cellet);
					} catch (InstantiationException e) {
						Logger.log(Nucleus.class, e, LogLevel.ERROR);
						continue;
					} catch (IllegalAccessException e) {
						Logger.log(Nucleus.class, e, LogLevel.ERROR);
						continue;
					}
				}
			} finally {
				// 以下为 JDK7 的代码
				try {
					loader.close();
				} catch (Exception e) {
					Logger.log(Nucleus.class, e, LogLevel.ERROR);
				}
			}
		}
	}

	/** 加载外部 class 路径，实例化 Cellet
	 */
	private void loadExternalPath() {
		if (null == this.celletPathClasses) {
			return;
		}

		if (null == this.cellets) {
			this.cellets = new ConcurrentHashMap<String, Cellet>();
		}

		// 遍历配置数据
		Iterator<String> iter = this.celletPathClasses.keySet().iterator();
		while (iter.hasNext()) {
			// 路径名
			final String pathName = iter.next();

			// 判断路径是否存在
			File path = new File(pathName);
			if (!path.exists()) {
				Logger.w(Nucleus.class, "Class path '"+ path.getAbsolutePath() +"' is not exists!");
				path = null;
				continue;
			}

			int classPathLen = path.getAbsolutePath().length();

			// 生成类列表
			ArrayList<String> classNameList = new ArrayList<String>();

			// 第一遍遍历出所有的文件夹
			LinkedList<File> pathList = new LinkedList<File>();
			File[] files = path.listFiles();
			for (File f : files) {
				if (f.isDirectory()) {
					pathList.add(f);
				}
			}

			File file = null;
			while (!pathList.isEmpty()) {
				file = pathList.removeFirst();
				files = file.listFiles();
				for (File f : files) {
					if (f.isDirectory()) {
						pathList.add(f);
					}
					else {
						String name = f.getAbsolutePath();
						// 提取类名
						if (name.endsWith("class")) {
							name = name.substring(classPathLen + 1, name.length());
							// 将 net/cellcloud/MyObject.class 转为 net.cellcloud.MyObject
							name = name.replaceAll("/", ".").substring(0, name.length() - 6);
							classNameList.add(name);
						}
					}
				}
			}

			// 定位文件
			URL url = null;
			try {
				url = new URL(file.toURI().toURL().toString());
			} catch (MalformedURLException e) {
				Logger.log(Nucleus.class, e, LogLevel.WARNING);
				continue;
			}

			// 加载 Class
			URLClassLoader loader = null;
			try {
				loader = new URLClassLoader(new URL[]{url}
					, Thread.currentThread().getContextClassLoader());

				// 取出 Cellet 类
				List<String> celletClasslist = this.celletPathClasses.get(pathName);
				// Cellet 类列表
				ArrayList<Class<?>> classes = new ArrayList<Class<?>>();

				// 加载所有的 Class
				for (int i = 0, size = classNameList.size(); i < size; ++i) {
					try {
						String className = classNameList.get(i);
						Class<?> clazz = loader.loadClass(className);
						if (celletClasslist.contains(className)) {
							classes.add(clazz);
						}

						// 分析 class
						this.analyzeClass(clazz);
					} catch (ClassNotFoundException e) {
						Logger.log(Nucleus.class, e, LogLevel.ERROR);
					}
				}

				for (int i = 0, size = classes.size(); i < size; ++i) {
					try {
						Class<?> clazz = classes.get(i);
						// 实例化 Cellet
						Cellet cellet = (Cellet) clazz.newInstance();
						// 存入列表
						this.cellets.put(cellet.getFeature().getIdentifier(), cellet);
					} catch (InstantiationException e) {
						Logger.log(Nucleus.class, e, LogLevel.ERROR);
						continue;
					} catch (IllegalAccessException e) {
						Logger.log(Nucleus.class, e, LogLevel.ERROR);
						continue;
					}
				}
			} finally {
				// 以下为 JDK7 的代码
				try {
					loader.close();
				} catch (Exception e) {
					Logger.log(Nucleus.class, e, LogLevel.ERROR);
				}
			}
		}
	}

	private void analyzeClass(Class<?> clazz) {
		if (null == this.tempAdapterListeners) {
			this.tempAdapterListeners = new ArrayList<AdapterListenerProfile>();
		}
		// 分析是否是适配器监听器
		AdapterListenerProfile alp = AdapterListenerProfile.load(clazz);
		if (null != alp) {
			this.tempAdapterListeners.add(alp);
		}
	}

	/** 启动所有 Cellets
	 */
	private void activateCellets() {
		if (null != this.cellets && !this.cellets.isEmpty()) {
			Iterator<Cellet> iter = this.cellets.values().iterator();
			while (iter.hasNext()) {
				Cellet cellet = iter.next();
				try {
					// 准备
					cellet.prepare();
					// 激活
					cellet.activate();
				} catch (Exception e) {
					Logger.log(this.getClass(), e, LogLevel.ERROR);
					Logger.e(this.getClass(), "Cellet '" + cellet.getFeature().getIdentifier() + "' activate failed.");
				}
			}
		}
	}

	/** 停止所有 Cellets
	 */
	private void deactivateCellets() {
		if (null != this.cellets && !this.cellets.isEmpty()) {
			Iterator<Cellet> iter = this.cellets.values().iterator();
			while (iter.hasNext()) {
				try {
					iter.next().deactivate();
				} catch (Exception e) {
					Logger.log(this.getClass(), e, LogLevel.ERROR);
				}
			}
		}
	}
}
