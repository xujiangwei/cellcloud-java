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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.cellcloud.adapter.RelationNucleusAdapter;
import net.cellcloud.cluster.ClusterController;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.exception.CelletSandboxException;
import net.cellcloud.exception.SingletonException;
import net.cellcloud.http.HttpService;
import net.cellcloud.talk.TalkService;

/** Cell Cloud 软件栈内核类。
 * 
 * @author Jiangwei Xu
 */
public final class Nucleus {

	private static Nucleus instance = null;

	private NucleusTag tag = null;
	private NucleusConfig config = null;
	private NucleusContext context = null;

	// 集群网络控制
	private ClusterController clusterController = null;

	// Talk 服务
	private TalkService talkService = null;
	// Web 服务
	private HttpService httpService = null;

	// Cellet
	private ConcurrentHashMap<String, ArrayList<String>> celletJarClasses = null;
	private ConcurrentHashMap<String, Cellet> cellets = null;
	private ConcurrentHashMap<String, CelletSandbox> sandboxes = null;

	// RNA
	private ConcurrentHashMap<String, RelationNucleusAdapter> adapters = null;

	/** 构造函数。
	 */
	public Nucleus(NucleusConfig config)
			throws SingletonException {
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
				if (this.config.role != NucleusConfig.Role.CONSUMER)
					Logger.w(Nucleus.class, "Nucleus Warning: No nucleus tag setting, use random tag: " + this.tag.asString());
			}

			this.context = new NucleusContext();
		}
		else {
			throw new SingletonException(Nucleus.class.getName());
		}
	}

	/** 返回单例。 */
	public static Nucleus getInstance() {
		return Nucleus.instance;
	}

	/** 返回内核标签。 */
	public NucleusTag getTag() {
		return this.tag;
	}
	/** 返回内核标签。 */
	public String getTagAsString() {
		return this.tag.asString();
	}

	/** 返回配置。
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

	/** 启动内核。 */
	public boolean startup() {
		Logger.i(Nucleus.class, "*-*-* Cell Initializing *-*-*");

		// 角色：节点
		if ((this.config.role & NucleusConfig.Role.NODE) != 0) {

			//---- 配置集群 ----

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
					&& (this.config.device == NucleusConfig.Device.SERVER
						|| this.config.device == NucleusConfig.Device.DESKTOP);
			// 启动集群控制器
			if (this.clusterController.startup()) {
				Logger.i(Nucleus.class, "Starting cluster controller service success.");
			}
			else {
				Logger.e(Nucleus.class, "Starting cluster controller service failure.");
			}

			//---- 配置 HTTP 服务  ----

			if (this.config.httpd) {
				// 创建 HTTP Service
				try {
					this.httpService = new HttpService(this.context);
				} catch (SingletonException e) {
					Logger.log(Nucleus.class, e, LogLevel.WARNING);
				}
			}

			//---- 配置 Talk Service  ----

			// 创建 Talk Service
			if (this.config.talk.enable && (null == this.talkService)) {
				try {
					this.talkService = new TalkService(this.context);
				} catch (SingletonException e) {
					Logger.log(Nucleus.class, e, LogLevel.ERROR);
				}
			}

			if (this.config.talk.enable) {
				// 设置服务端口号
				this.talkService.setPort(this.config.talk.port);

				// 启动 Talk Service
				if (this.talkService.startup()) {
					Logger.i(Nucleus.class, "Starting talk service success.");
				}
				else {
					Logger.e(Nucleus.class, "Starting talk service failure.");
				}
			}

			//---- 配置结束 ----

			// 加载外部 Jar 包
			this.loadExternalJar();

			// 启动 Cellet
			this.activateCellets();

			// 启动 HTTP Service
			if (null != this.httpService) {
				if (this.httpService.startup()) {
					Logger.i(Nucleus.class, "Starting http service success.");
				}
				else {
					Logger.i(Nucleus.class, "Starting http service failure.");
				}
			}
		}

		// 角色：消费者
		if ((this.config.role & NucleusConfig.Role.CONSUMER) != 0) {
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

		return true;
	}

	/** 关停内核。 */
	public void shutdown() {
		Logger.i(Nucleus.class, "*-*-* Cell Finalizing *-*-*");

		// 角色：节点
		if ((this.config.role & NucleusConfig.Role.NODE) != 0) {
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
		}

		// 角色：消费者
		if ((this.config.role & NucleusConfig.Role.CONSUMER) != 0) {
			this.talkService.stopDaemon();
		}
	}

	/** 返回注册在该内核上的指定的 Cellet 。
	 */
	public Cellet getCellet(final String identifier, final NucleusContext context) {
		if (null == this.cellets) {
			return null;
		}

		if (this.context == context) {
			return this.cellets.get(identifier);
		}

		return null;
	}

	/** 载入 Cellet JAR 包信息。
	 */
	public void prepareCelletJar(String jarFile, ArrayList<String> classes) {
		if (null == this.celletJarClasses) {
			this.celletJarClasses = new ConcurrentHashMap<String, ArrayList<String>>();
		}

		this.celletJarClasses.put(jarFile, classes);
	}

	/** 注册 Cellet 。
	*/
	public void registerCellet(Cellet cellet) {
		if (null == this.cellets) {
			this.cellets = new ConcurrentHashMap<String, Cellet>();
		}

		this.cellets.put(cellet.getFeature().getIdentifier(), cellet);
	}

	/** 注销 Cellet 。
	*/
	public void unregisterCellet(Cellet cellet) {
		if (null == this.cellets) {
			return;
		}

		this.cellets.remove(cellet.getFeature().getIdentifier());
	}

	/**
	 * 返回所有 Cellet 的 Feature 列表。
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

	/** TODO
	 */
	public RelationNucleusAdapter getAdapter(final String name) {
		return null;
	}

	/** TODO
	 */
	public void addAdapter(RelationNucleusAdapter adapter) {
		if (null == this.adapters) {
			this.adapters = new ConcurrentHashMap<String, RelationNucleusAdapter>();
		}
	}

	/** TODO
	 */
	public void removeAdapter(RelationNucleusAdapter adapter) {
		
	}

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

	/** 加载外部 Jar 文件。
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
				ArrayList<String> celletClasslist = this.celletJarClasses.get(jarFilename);
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

	/** 启动所有 Cellets
	 */
	private void activateCellets() {
		if (null != this.cellets && !this.cellets.isEmpty()) {
			Iterator<Cellet> iter = this.cellets.values().iterator();
			while (iter.hasNext()) {
				Cellet cellet = iter.next();
				// 准备
				cellet.prepare();
				// 激活
				cellet.activate();
			}
		}
	}

	/** 停止所有 Cellets
	 */
	private void deactivateCellets() {
		if (null != this.cellets && !this.cellets.isEmpty()) {
			Iterator<Cellet> iter = this.cellets.values().iterator();
			while (iter.hasNext()) {
				iter.next().deactivate();
			}
		}
	}
}
