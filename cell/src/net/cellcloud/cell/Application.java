/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2016 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.cell;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.cellcloud.Version;
import net.cellcloud.cell.log.RollFileLogger;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.LogManager;
import net.cellcloud.common.Logger;
import net.cellcloud.core.Device;
import net.cellcloud.core.Nucleus;
import net.cellcloud.core.NucleusConfig;
import net.cellcloud.core.Role;
import net.cellcloud.exception.SingletonException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Cell Cloud 默认容器应用。
 * 
 * @author Jiangwei Xu
 */
public final class Application {

	private boolean spinning;
	private byte[] monitor;

	private Console console;

	private String configFile;

	public Application(Arguments args) {
		StringBuilder buf = new StringBuilder();
		buf.append("Cell Server version: ");
		buf.append(VersionInfo.MAJOR).append(".").append(VersionInfo.MINOR).append(".").append(VersionInfo.REVISION);
		buf.append("\n");
		buf.append("Nucleus version: ");
		buf.append(Version.getNumbers());
		buf.append(" (Build Java - ").append(Version.NAME).append(")\n");

		buf.append("-----------------------------------------------------------------------\n");
		buf.append(" ___ ___ __  __     ___ __  ___ _ _ ___\n");
		buf.append("| __| __| | | |    | __| | |   | | | _ \\\n");
		buf.append("| |_| _|| |_| |_   | |_| |_| | | | | | |\n");
		buf.append("|___|___|___|___|  |___|___|___|___|___/\n\n");

		buf.append("Copyright (c) 2009,2017 Cell Cloud Team, www.cellcloud.net\n");
		buf.append("-----------------------------------------------------------------------");

		System.out.println(buf.toString());

		this.monitor = new byte[0];

		if (args.console) {
			this.console = new Console(this);
		}
		else {
			this.console = null;
			LogManager.getInstance().addHandle(LogManager.createSystemOutHandle());
		}

		// 使用文件日志
		if (null != args.logFile) {
			RollFileLogger fileLogger = new RollFileLogger("CellFileLogger");
			fileLogger.open("logs" + File.separator + args.logFile);

			// 设置日志操作器
			LogManager.getInstance().addHandle(fileLogger);
		}

		// 记录版本日志
		Logger.i(Application.class, buf.toString());

		// 配置文件
		this.configFile = args.confileFile;

		buf = null;
	}

	/** 启动程序。
	 */
	protected boolean startup() {
		NucleusConfig config = new NucleusConfig();
		config.role = Role.NODE;
		config.device = Device.SERVER;

		HashMap<String, ArrayList<String>> jarCelletMap = new HashMap<String, ArrayList<String>>();
		HashMap<String, ArrayList<String>> pathCelletMap = new HashMap<String, ArrayList<String>>();

		// 加载内核配置
		if (null != this.configFile) {
			this.loadConfig(this.configFile, config, jarCelletMap, pathCelletMap);
			if (jarCelletMap.isEmpty() && pathCelletMap.isEmpty()) {
				// 没有 Cellet 服务被找到
				Logger.w(Application.class, "Can NOT find cellet in config file!");
			}
		}

		Nucleus nucleus = Nucleus.getInstance();
		if (null == nucleus) {
			try {
				nucleus = Nucleus.createInstance(config);
			} catch (SingletonException e) {
				Logger.log(Application.class, e, LogLevel.ERROR);
				return false;
			}
		}

		// 为内核准备 Cellet 信息
		if (!jarCelletMap.isEmpty()) {
			Iterator<Map.Entry<String, ArrayList<String>>> iter = jarCelletMap.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, ArrayList<String>> e = iter.next();
				nucleus.prepareCelletJar(e.getKey(), e.getValue());
			}

			jarCelletMap.clear();
			jarCelletMap = null;
		}
		if (!pathCelletMap.isEmpty()) {
			Iterator<Map.Entry<String, ArrayList<String>>> iter = pathCelletMap.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, ArrayList<String>> e = iter.next();
				nucleus.prepareCelletPath(e.getKey(), e.getValue());
			}

			pathCelletMap.clear();
			pathCelletMap = null;
		}

		// 启动内核
		if (!nucleus.startup()) {
			Logger.e(Application.class, "Nucleus start failed!");
			return false;
		}

		this.spinning = true;

		return true;
	}

	/** 关闭程序。
	 */
	protected void shutdown() {
		if (null != Nucleus.getInstance()) {
			Nucleus.getInstance().shutdown();
		}

		RollFileLogger handle = (RollFileLogger) LogManager.getInstance().getHandle("CellFileLogger");
		if (null != handle) {
			// 移除并关闭
			LogManager.getInstance().removeHandle(handle);
			handle.close();
		}
	}

	/** 运行（阻塞线程）。
	 */
	protected void run() {
		if (null != this.console) {
			while (this.spinning) {
				if (!this.console.processInput()) {
					this.spinning = false;
				}
			}
		}
		else {
			synchronized (this.monitor) {
				try {
					this.monitor.wait();
				} catch (InterruptedException e) {
					Logger.log(Application.class, e, LogLevel.ERROR);
				}
			}

			System.out.println("Exit cell...");
		}
	}

	/** 停止程序。
	 */
	public void stop() {
		this.spinning = false;

		if (null != this.console) {
			this.console.quitAndClose();
			this.console = null;
		}

		try {
			synchronized (this.monitor) {
				this.monitor.notifyAll();
			}
		} catch (IllegalMonitorStateException e) {
			// Nothing
		}
	}

	/** 返回控制台。
	 */
	protected Console getConsole() {
		return this.console;
	}

	/** 加载配置。
	 */
	private void loadConfig(String configFile, NucleusConfig config,
			Map<String, ArrayList<String>> jarCelletMap,
			Map<String, ArrayList<String>> pathCelletMap) {

		try {
			// 检测配置文件
			URL pathURL = this.getClass().getClassLoader().getResource(".");
			String resourcePath = (null != pathURL) ? pathURL.getPath() : "./";
			String fileName = resourcePath + configFile;
			File file = new File(fileName);
			if (!file.exists()) {
				String[] array = resourcePath.split("/");
				StringBuilder path = new StringBuilder();
				path.append("/");
				for (int i = 0; i < array.length - 1; ++i) {
					if (array[i].length() == 0)
						continue;

					path.append(array[i]);
					path.append("/");
				}
				resourcePath = path.toString();
				path = null;
			}

			fileName = resourcePath + configFile;
			file = new File(fileName);
			if (file.exists()) {
				// 解析文件

				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document document = db.parse(fileName);

				// 读取 nucleus
				NodeList list = document.getElementsByTagName("nucleus");
				if (list.getLength() > 0) {
					Element el = (Element) list.item(0);

					// tag
					NodeList nl = el.getElementsByTagName("tag");
					if (nl.getLength() > 0) {
						config.tag = nl.item(0).getTextContent().toString();
						Logger.i(this.getClass(), "nucleus.tag = " + config.tag);
					}

					// httpd
					config.httpd = Boolean.parseBoolean(el.getElementsByTagName("httpd").item(0).getTextContent());
					Logger.i(this.getClass(), "nucleus.httpd = " + config.httpd);

					// talk config
					NodeList talks = el.getElementsByTagName("talk");
					if (talks.getLength() > 0) {
						Element elTalk = (Element) talks.item(0);
						// port
						nl = elTalk.getElementsByTagName("port");
						if (nl.getLength() > 0) {
							try {
								config.talk.port = Integer.parseInt(nl.item(0).getTextContent());
								Logger.i(this.getClass(), "nucleus.talk.port = " + config.talk.port);
							} catch (NumberFormatException e) {
								Logger.log(this.getClass(), e, LogLevel.WARNING);
							}
						}
						// block
						nl = elTalk.getElementsByTagName("block");
						if (nl.getLength() > 0) {
							try {
								config.talk.block = Integer.parseInt(nl.item(0).getTextContent());
								Logger.i(this.getClass(), "nucleus.talk.block = " + config.talk.block);
							} catch (NumberFormatException e) {
								Logger.log(this.getClass(), e, LogLevel.WARNING);
							}
						}
						// connections
						nl = elTalk.getElementsByTagName("connections");
						if (nl.getLength() > 0) {
							try {
								config.talk.maxConnections = Integer.parseInt(nl.item(0).getTextContent());
								Logger.i(this.getClass(), "nucleus.talk.connections = " + config.talk.maxConnections);
							} catch (NumberFormatException e) {
								Logger.log(this.getClass(), e, LogLevel.WARNING);
							}
						}
						// workers
						nl = elTalk.getElementsByTagName("workers");
						if (nl.getLength() > 0) {
							try {
								config.talk.numWorkerThreads = Integer.parseInt(nl.item(0).getTextContent());
								Logger.i(this.getClass(), "nucleus.talk.numWorkerThreads = " + config.talk.numWorkerThreads);
							} catch (NumberFormatException e) {
								Logger.log(this.getClass(), e, LogLevel.WARNING);
							}
						}
						// http
						nl = elTalk.getElementsByTagName("http");
						if (nl.getLength() > 0) {
							Element elHttp = (Element) nl.item(0);

							// http enabled
							nl = elHttp.getElementsByTagName("enabled");
							if (nl.getLength() > 0) {
								config.talk.httpEnabled = Boolean.parseBoolean(nl.item(0).getTextContent());
								Logger.i(this.getClass(), "nucleus.talk.http.enabled = " + config.talk.httpEnabled);
							}
							// http port
							nl = elHttp.getElementsByTagName("port");
							if (nl.getLength() > 0) {
								try {
									config.talk.httpPort = Integer.parseInt(nl.item(0).getTextContent());
									Logger.i(this.getClass(), "nucleus.talk.http.port = " + config.talk.httpPort);
								} catch (NumberFormatException e) {
									Logger.log(this.getClass(), e, LogLevel.WARNING);
								}

								// 自动推演 https port
								config.talk.httpsPort = config.talk.httpPort + 10;
								Logger.i(this.getClass(), "nucleus.talk.https.port = " + config.talk.httpsPort);
							}
							// http queue size
							nl = elHttp.getElementsByTagName("queue");
							if (nl.getLength() > 0) {
								try {
									config.talk.httpQueueSize = Integer.parseInt(nl.item(0).getTextContent());
									Logger.i(this.getClass(), "nucleus.talk.http.queue = " + config.talk.httpQueueSize);
								} catch (NumberFormatException e) {
									Logger.log(this.getClass(), e, LogLevel.WARNING);
								}
							}
						}
						// ssl
						nl = elTalk.getElementsByTagName("ssl");
						if (nl.getLength() > 0) {
							Element elSsl = (Element) nl.item(0);

							// keystore
							nl = elSsl.getElementsByTagName("keystore");
							if (nl.getLength() > 0) {
								config.talk.keystore = nl.item(0).getTextContent().toString();
								Logger.i(this.getClass(), "nucleus.talk.ssl.keystore = " + config.talk.keystore);
							}
							// password
							nl = elSsl.getElementsByTagName("password");
							if (nl.getLength() > 0) {
								config.talk.keyStorePassword = nl.item(0).getTextContent().toString();
								config.talk.keyManagerPassword = config.talk.keyStorePassword;
								Logger.i(this.getClass(), "nucleus.talk.ssl.password = " + config.talk.keyStorePassword);
							}
						}
					}
				}

				// 读取 adapter
				list = document.getElementsByTagName("adapter");
				for (int i = 0; i < list.getLength(); ++i) {
					
				}

				// 读取 cellet
				list = document.getElementsByTagName("cellet");
				for (int i = 0; i < list.getLength(); ++i) {
					Node node = list.item(i);

					Node attr = node.getAttributes().getNamedItem("jar");
					if (null != attr) {
						String jar = attr.getNodeValue();

						ArrayList<String> classes = new ArrayList<String>();
						for (int n = 0; n < node.getChildNodes().getLength(); ++n) {
							if (node.getChildNodes().item(n).getNodeType() == Node.ELEMENT_NODE) {
								classes.add(node.getChildNodes().item(n).getTextContent());
							}
						}

						// 添加 Jar
						jarCelletMap.put(jar, classes);
					}
					else {
						attr = node.getAttributes().getNamedItem("path");
						if (null != attr) {
							String path = attr.getNodeValue();

							ArrayList<String> classes = new ArrayList<String>();
							for (int n = 0; n < node.getChildNodes().getLength(); ++n) {
								if (node.getChildNodes().item(n).getNodeType() == Node.ELEMENT_NODE) {
									classes.add(node.getChildNodes().item(n).getTextContent());
								}
							}

							// 添加 Path
							pathCelletMap.put(path, classes);
						}
					}
				}
			}
		} catch (ParserConfigurationException e) {
			Logger.log(Application.class, e, LogLevel.ERROR);
		} catch (SAXException e) {
			Logger.log(Application.class, e, LogLevel.ERROR);
		} catch (IOException e) {
			Logger.log(Application.class, e, LogLevel.ERROR);
		}
	}

	/** 加载所有库文件
	 */
	protected boolean loadLibraries() {
		String parentPath = "libs/";
		File file = new File(parentPath);
		if (!file.exists()) {
			parentPath = "../libs/";
			file = new File(parentPath);
		}

		if (!file.exists()) {
			return false;
		}

		// 枚举 libs 目录下的所有 jar 文件，并进行装载

		ArrayList<URL> urls = new ArrayList<URL>();
		ArrayList<String> classNameList = new ArrayList<String>();

		File[] files = file.listFiles();
		for (File f : files) {
			if (f.getName().endsWith("jar")) {
				JarFile jarFile = null;
				try {
					jarFile = new JarFile(parentPath + f.getName());
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}

				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					String name = entries.nextElement().getName();
					if (name.endsWith("class")) {
						// 将 net/cellcloud/MyObject.class 转为 net.cellcloud.MyObject
						name = name.replaceAll("/", ".").substring(0, name.length() - 6);
						classNameList.add(name);
					}
				}

				try {
					jarFile.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}

				try {
					URL url = new URL(f.toURI().toURL().toString());
					urls.add(url);
				} catch (MalformedURLException e) {
					e.printStackTrace();
					continue;
				}
			}
		}

		// 加载 Class
		URLClassLoader loader = null;
		try {
			loader = new URLClassLoader(urls.toArray(new URL[urls.size()])
					, Thread.currentThread().getContextClassLoader());

			for (String className : classNameList) {
				try {
					loader.loadClass(className);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					continue;
				} catch (NoClassDefFoundError e) {
					e.printStackTrace();
					continue;
				}
			}
		} finally {
			try {
				loader.close();
			} catch (Exception e) {
				// Nothing
			}
		}

		return true;
	}

	private class AdapterInfo {
		protected int port;
	}

}
