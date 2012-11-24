/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (cellcloudproject@gmail.com)

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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

	// 核心服务
	private TalkService talkService = null;
	private HttpService httpService = null;

	// Cellet
	private ConcurrentHashMap<String, ArrayList<String>> celletJarClasses = null;
	private ConcurrentHashMap<String, Cellet> cellets = null;
	private ConcurrentHashMap<String, CelletSandbox> sandboxes = null;

	/** 构造函数。
	 */
	public Nucleus(NucleusConfig config)
			throws SingletonException {
		if (null == Nucleus.instance) {
			Nucleus.instance = this;
			// 设置配置
			this.config = config;

			// 生成标签
			if (null != config.tag)
				this.tag = new NucleusTag(config.tag);
			else
				this.tag = new NucleusTag();

			this.context = new NucleusContext();
			this.talkService = new TalkService(this.context);
		}
		else {
			throw new SingletonException(Nucleus.class.getName());
		}
	}

	/** 返回单例。 */
	public synchronized static Nucleus getInstance() {
		return Nucleus.instance;
	}

	/** 返回内核标签。 */
	public String getTagAsString() {
		return this.tag.asString();
	}

	/** 返回 Talk Service 实例。 */
	public TalkService getTalkService() {
		return this.talkService;
	}

	/** 启动内核。 */
	public boolean startup() {
		Logger.i(Nucleus.class, "*-*-* Cell Initializing *-*-*");

		// 设置 Jetty 的日志傀儡
		org.eclipse.jetty.util.log.Log.setLog(new JettyLoggerPuppet());

		// 角色：节点
		if ((this.config.role & NucleusConfig.Role.NODE) != 0) {
			if (this.config.http) {
				// 创建 Web Service
				try {
					this.httpService = new HttpService(this.context);
				} catch (SingletonException e) {
					Logger.w(Nucleus.class, "Creates web service singleton exception!");
				}
			}

			// 启动 Talk Service
			if (this.talkService.startup()) {
				Logger.i(Nucleus.class, "Starting talk service success.");
			}
			else {
				Logger.i(Nucleus.class, "Starting talk service fail.");
			}

			// 加载外部 Jar 包
			this.loadExternalJar();

			// 启动 Cellet
			this.activateCellets();

			// 启动 Web Service
			if (null != this.httpService) {
				if (this.httpService.startup()) {
					Logger.i(Nucleus.class, "Starting web service success.");
				}
				else {
					Logger.i(Nucleus.class, "Starting web service fail.");
				}
			}
		}

		// 角色：消费者
		if ((this.config.role & NucleusConfig.Role.CONSUMER) != 0) {
			this.talkService.startDaemon();
		}

		return true;
	}

	/** 关停内核。 */
	public void shutdown() {
		Logger.i(Nucleus.class, "*-*-* Cell Finalizing *-*-*");

		// 角色：节点
		if ((this.config.role & NucleusConfig.Role.NODE) != 0) {
			// 停止所有 Cellet
			this.deactivateCellets();

			// 关闭 Talk Service
			this.talkService.shutdown();

			// 关闭 Web Service
			if (null != this.httpService) {
				this.httpService.shutdown();
			}
		}

		// 角色：消费者
		if ((this.config.role & NucleusConfig.Role.CONSUMER) != 0) {
			this.talkService.stopDaemon();
		}
	}

	/** 返回指定的 Cellet 。
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
			Logger.e(Nucleus.class, "Error in prepareCellet() - Cellet:" + cellet.getFeature().getIdentifier());
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
			try {
				JarFile jarFile = new JarFile(jarFilename);

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

				jarFile.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
				continue;
			}

			// 定位文件
			URL url = null;
			try {
				url = new URL("file:" + jarFilename);
			} catch (MalformedURLException e) {
				e.printStackTrace();
				continue;
			}

			// 加载 Class
			URLClassLoader loader = new URLClassLoader(new URL[]{url}
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
					e.printStackTrace();
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
					e.printStackTrace();
					continue;
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					continue;
				}
			}

			/* 以下为 JDK7 的代码
			try {
				loader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			*/
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
