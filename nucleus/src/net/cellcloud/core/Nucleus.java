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

import net.cellcloud.exception.SingletonException;
import net.cellcloud.talk.TalkService;

/** Cell Cloud 软件栈内核类。
 * 
 * @author Jiangwei Xu
 */
public final class Nucleus {

	private static Nucleus instance = null;

	private NucleusTag tag;
	private NucleusConfig config;
	private NucleusContext context;

	private TalkService talkService;

	private ConcurrentHashMap<String, ArrayList<String>> celletJarClasses;
	private ConcurrentHashMap<String, Cellet> cellets;

	/** 构造函数。
	 */
	public Nucleus() throws SingletonException {
		if (null == Nucleus.instance) {
			Nucleus.instance = this;

			this.tag = new NucleusTag();
			this.context = new NucleusContext();

			this.talkService = new TalkService(this.context);

			this.celletJarClasses = null;
			this.cellets = new ConcurrentHashMap<String, Cellet>();
		}
		else {
			throw new SingletonException(Nucleus.class.getName());
		}
	}

	/** 构造函数。
	 */
	public Nucleus(NucleusConfig config) throws SingletonException {
		if (null == Nucleus.instance) {
			Nucleus.instance = this;
			this.config = config;

			this.tag = new NucleusTag();
			this.context = new NucleusContext();

			this.talkService = new TalkService(this.context);

			this.celletJarClasses = null;
			this.cellets = new ConcurrentHashMap<String, Cellet>();
		}
		else {
			throw new SingletonException(Nucleus.class.getName());
		}
	}

	/** 返回单例。 */
	public synchronized static Nucleus getInstance() {
		return Nucleus.instance;
	}

	/** 设置配置。 */
	public void setConfig(NucleusConfig config) {
		this.config = config;
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

		if ((this.config.role & NucleusConfig.Role.NODE) != 0) {

			// 启动 Talk Service
			if (!this.talkService.startup()) {
				Logger.i(Nucleus.class, "Talk Service starts failed");
				return false;
			}

			Logger.i(Nucleus.class, "Talk Service starts successfully");

			// 加载外部 Jar 包
			this.loadExternalJar();

			// 启动 Cellet
			this.activateCellets();
		}

		if ((this.config.role & NucleusConfig.Role.CONSUMER) != 0) {
			this.talkService.startSchedule();
		}

		return true;
	}

	/** 关停内核。 */
	public void shutdown() {
		Logger.i(Nucleus.class, "*-*-* Cell Finalizing *-*-*");

		if ((this.config.role & NucleusConfig.Role.NODE) != 0) {
			// 停止所有 Cellet
			this.deactivateCellets();

			// 关闭 Talk Service
			this.talkService.shutdown();
		}

		if ((this.config.role & NucleusConfig.Role.CONSUMER) != 0) {
			this.talkService.stopSchedule();
		}
	}

	/** 返回指定的 Cellet 。
	 */
	public Cellet getCellet(final String identifier, final NucleusContext context) {
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
		this.cellets.put(cellet.getFeature().getIdentifier(), cellet);
	}

	/** 注销 Cellet 。
	*/
	public void unregisterCellet(Cellet cellet) {
		this.cellets.remove(cellet.getFeature().getIdentifier());
	}

	/** 加载外部 Jar 文件。
	 */
	private void loadExternalJar() {
		if (null != this.celletJarClasses) {
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
	}

	/** 启动所有 Cellets
	 */
	private void activateCellets() {
		if (!this.cellets.isEmpty()) {
			Iterator<Cellet> iter = this.cellets.values().iterator();
			while (iter.hasNext()) {
				iter.next().activate();
			}
		}
	}

	/** 停止所有 Cellets
	 */
	private void deactivateCellets() {
		if (!this.cellets.isEmpty()) {
			Iterator<Cellet> iter = this.cellets.values().iterator();
			while (iter.hasNext()) {
				iter.next().deactivate();
			}
		}
	}
}
