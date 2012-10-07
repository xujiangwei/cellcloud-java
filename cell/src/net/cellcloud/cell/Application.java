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

package net.cellcloud.cell;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.cellcloud.Version;
import net.cellcloud.cell.log.FileLogger;
import net.cellcloud.core.Logger;
import net.cellcloud.core.Nucleus;
import net.cellcloud.core.NucleusConfig;
import net.cellcloud.exception.SingletonException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Cell Cloud 默认容器应用。
 * 
 * @author Jiangwei Xu
 */
public final class Application {

	private Nucleus nucleus;

	private boolean consoleMode;
	private boolean spinning;
	private byte[] monitor;

	private Console console;

	public Application(boolean consoleMode) {
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

		buf.append("Copyright (c) 2009,2012 Cell Cloud Team, www.cellcloud.net\n");
		buf.append("-----------------------------------------------------------------------");

		System.out.println(buf);

		buf = null;

		this.monitor = new byte[0];

		this.consoleMode = consoleMode;
	}

	/** 启动程序。
	 */
	protected boolean startup() {
		FileLogger.getInstance().open("cell.log");

		NucleusConfig config = new NucleusConfig();
		config.role = NucleusConfig.Role.NODE;
		config.device = NucleusConfig.Device.SERVER;

		try {
			this.nucleus = new Nucleus(config);
		} catch (SingletonException e) {
			Logger.e(Application.class, e.getMessage());
			return false;
		}

		// 加载内核配置
		if (!loadConfig(this.nucleus)) {
			return false;
		}

		if (!this.nucleus.startup()) {
			return false;
		}

		this.spinning = true;

		return true;
	}

	/** 关闭程序。
	 */
	protected void shutdown() {
		if (null != this.nucleus) {
			this.nucleus.shutdown();
		}

		FileLogger.getInstance().close();
	}

	/** 运行（阻塞线程）。
	 */
	protected void run() {
		if (this.consoleMode) {
			this.console = new Console(this);
			while (this.spinning) {
				if (!this.console.processInput()) {
					this.spinning = false;
				}
			}
			this.console = null;
		}
		else {
			synchronized (this.monitor) {
				try {
					this.monitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			System.out.println("Exit cell application...");
		}
	}

	/** 停止程序。
	 */
	public void stop() {
		this.spinning = false;

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
	private boolean loadConfig(Nucleus nucleus) {
		try {
			// 检测配置文件
			String resourcePath = this.getClass().getClassLoader().getResource(".").getPath();
			String fileName = resourcePath + "nucleus.xml";
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

			fileName = resourcePath + "nucleus.xml";
			file = new File(fileName);
			if (file.exists()) {
				// 解析文件

				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document document = db.parse(fileName);

				// 读取 Cellet
				NodeList list = document.getElementsByTagName("cellet");
				for (int i = 0; i < list.getLength(); ++i) {
					Node node = list.item(i);
					String path = node.getAttributes().getNamedItem("path").getNodeValue();
					String jar = node.getAttributes().getNamedItem("jar").getNodeValue();

					ArrayList<String> classes = new ArrayList<String>();
					for (int n = 0; n < node.getChildNodes().getLength(); ++n) {
						if (node.getChildNodes().item(n).getNodeType() == Node.ELEMENT_NODE) {
							classes.add(node.getChildNodes().item(n).getTextContent());
						}
					}

					// 添加 Jar
					nucleus.prepareCelletJar(path + jar, classes);
				}
			}

			return true;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}
}
