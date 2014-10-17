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
import net.cellcloud.cell.log.FileLogger;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.core.Nucleus;
import net.cellcloud.core.NucleusConfig;
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

	public Application(boolean consoleMode, String logFile) {
		StringBuilder buf = new StringBuilder();
		buf.append("Cell Cloud ");
		buf.append(Version.MAJOR);
		buf.append(".");
		buf.append(Version.MINOR);
		buf.append(".");
		buf.append(Version.REVISION);
		buf.append(" (Build Java - ");
		buf.append(Version.NAME);
		buf.append(")\n");

		buf.append(" ___ ___ __  __     ___ __  ___ _ _ ___\n");
		buf.append("| __| __| | | |    | __| | |   | | | _ \\\n");
		buf.append("| |_| _|| |_| |_   | |_| |_| | | | | | |\n");
		buf.append("|___|___|___|___|  |___|___|___|___|___/\n\n");

		buf.append("Copyright (c) 2009,2014 Cell Cloud Team, www.cellcloud.net\n");
		buf.append("-----------------------------------------------------------------------");

		System.out.println(buf);
		buf = null;

		this.monitor = new byte[0];

		if (consoleMode)
			this.console = new Console(this);
		else
			this.console = null;

		// 使用文件日志
		if (null != logFile)
			FileLogger.getInstance().open("logs" + File.separator + logFile);
	}

	/** 启动程序。
	 */
	protected boolean startup() {
		NucleusConfig config = new NucleusConfig();
		config.role = NucleusConfig.Role.NODE;
		config.device = NucleusConfig.Device.SERVER;

		// 加载内核配置
		HashMap<String, ArrayList<String>> cellets = this.loadConfig(config);
		if (null == cellets || cellets.isEmpty()) {
			Logger.e(Application.class, "Can not find cellet in config file, start failed!");
			return false;
		}

		Nucleus nucleus = null;
		try {
			if (null == Nucleus.getInstance()) {
				nucleus = Nucleus.createInstance(config);
			}
		} catch (SingletonException e) {
			Logger.log(Application.class, e, LogLevel.ERROR);
			return false;
		}

		// 为内核准备 Cellet 信息
		Iterator<Map.Entry<String, ArrayList<String>>> iter = cellets.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, ArrayList<String>> e = iter.next();
			nucleus.prepareCelletJar(e.getKey(), e.getValue());
		}

		if (!nucleus.startup()) {
			return false;
		}

		cellets.clear();
		cellets = null;

		this.spinning = true;

		return true;
	}

	/** 关闭程序。
	 */
	protected void shutdown() {
		if (null != Nucleus.getInstance()) {
			Nucleus.getInstance().shutdown();
		}

		FileLogger.getInstance().close();
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
				this.monitor.notify();
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
	private HashMap<String, ArrayList<String>> loadConfig(NucleusConfig config) {
		HashMap<String, ArrayList<String>> celletMap = new HashMap<String, ArrayList<String>>();

		try {
			// 检测配置文件
			URL pathURL = this.getClass().getClassLoader().getResource(".");
			String resourcePath = (null != pathURL) ? pathURL.getPath() : "./";
			String fileName = resourcePath + "cell.xml";
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

			fileName = resourcePath + "cell.xml";
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
					NodeList talks = el.getElementsByTagName("talk");
					if (talks.getLength() > 0) {
						Element elTalk = (Element) talks.item(0);
						// port
						NodeList nl = elTalk.getElementsByTagName("port");
						if (nl.getLength() > 0) {
							config.talk.port = Integer.parseInt(nl.item(0).getTextContent());
						}
						// block
						nl = elTalk.getElementsByTagName("block");
						if (nl.getLength() > 0) {
							config.talk.block = Integer.parseInt(nl.item(0).getTextContent());
						}
						// httpd
						nl = elTalk.getElementsByTagName("httpd");
						if (nl.getLength() > 0) {
							config.talk.httpd = Boolean.parseBoolean(nl.item(0).getTextContent());
						}
					}
				}

				// 读取 cellet
				list = document.getElementsByTagName("cellet");
				for (int i = 0; i < list.getLength(); ++i) {
					Node node = list.item(i);
					String jar = node.getAttributes().getNamedItem("jar").getNodeValue();

					ArrayList<String> classes = new ArrayList<String>();
					for (int n = 0; n < node.getChildNodes().getLength(); ++n) {
						if (node.getChildNodes().item(n).getNodeType() == Node.ELEMENT_NODE) {
							classes.add(node.getChildNodes().item(n).getTextContent());
						}
					}

					// 添加 Jar
					celletMap.put(jar, classes);
				}
			}
		} catch (ParserConfigurationException e) {
			Logger.log(Application.class, e, LogLevel.ERROR);
		} catch (SAXException e) {
			Logger.log(Application.class, e, LogLevel.ERROR);
		} catch (IOException e) {
			Logger.log(Application.class, e, LogLevel.ERROR);
		}

		return celletMap;
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
}
