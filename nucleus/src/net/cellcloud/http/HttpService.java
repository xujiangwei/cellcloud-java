/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Service;
import net.cellcloud.core.NucleusContext;
import net.cellcloud.exception.SingletonException;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;

/** Web 服务。
 * 
 * @author Jiangwei Xu
 */
public final class HttpService implements Service {

	private static HttpService instance = null;

	private Server server = null;
	private List<Connector> connectors = null;
	private HashMap<String, HttpHandler> httpHandlers = null;

	private JettyHandler handler = null;

	public HttpService(NucleusContext context)
			throws SingletonException {
		if (null == HttpService.instance) {
			HttpService.instance = this;

			// 设置 Jetty 的日志傀儡
			org.eclipse.jetty.util.log.Log.setLog(new JettyLoggerPuppet());

			this.server = new Server();
			this.handler = new JettyHandler();
			this.connectors = new ArrayList<Connector>();
			this.httpHandlers = new HashMap<String, HttpHandler>();
		}
		else {
			throw new SingletonException(HttpService.class.getName());
		}
	}

	/** 返回单例。
	 */
	public synchronized static HttpService getInstance() {
		return HttpService.instance;
	}

	@Override
	public boolean startup() {
		// 构建错误页
		ErrorPages.build();

		Connector[] array = new Connector[this.connectors.size()];
		this.connectors.toArray(array);
		this.server.setConnectors(array);
		this.server.setHandler(this.handler);
		this.server.setGracefulShutdown(5000);
		this.server.setStopAtShutdown(true);

		// 更新 Hanlder 列表
		this.handler.updateHandlers(this.httpHandlers);

		try {
			this.server.start();
		} catch (InterruptedException e) {
			Logger.logException(e, LogLevel.ERROR);
		} catch (Exception e) {
			Logger.logException(e, LogLevel.ERROR);
		}

		return true;
	}

	@Override
	public void shutdown() {
		try {
			this.server.stop();
		} catch (Exception e) {
			Logger.logException(e, LogLevel.WARNING);
		}
	}

	/** 添加连接器。
	 */
	public void addConnector(Connector connector) {
		this.connectors.add(connector);
	}

	public void addHandler(HttpHandler handler) {
		this.httpHandlers.put(handler.getContextPath(), handler);
	}
}
