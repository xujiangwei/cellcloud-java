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
import java.util.LinkedList;
import java.util.List;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Service;
import net.cellcloud.core.NucleusContext;
import net.cellcloud.exception.SingletonException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

/** HTTP 服务。
 * 
 * @author Jiangwei Xu
 */
public final class HttpService implements Service {

	private static HttpService instance = null;

	private Server server = null;

	private LinkedList<HttpCapsule> httpCapsules = null;

	/**
	 * 构造函数。
	 * @param context
	 * @throws SingletonException
	 */
	public HttpService(NucleusContext context)
			throws SingletonException {
		if (null == HttpService.instance) {
			HttpService.instance = this;

			// 设置 Jetty 的日志傀儡
			org.eclipse.jetty.util.log.Log.setLog(new JettyLoggerPuppet());

			// 创建服务器
			this.server = new Server();

			// 设置错误处理句柄
			this.server.addBean(new DefaultErrorHandler());

			this.httpCapsules = new LinkedList<HttpCapsule>();
		}
		else {
			throw new SingletonException(HttpService.class.getName());
		}
	}

	/** 返回单例。
	 */
	public static HttpService getInstance() {
		return HttpService.instance;
	}

	/** 启动服务。
	 */
	@Override
	public boolean startup() {
		ArrayList<ServerConnector> connectorList = new ArrayList<ServerConnector>(this.httpCapsules.size());
		ArrayList<ContextHandler> contextList = new ArrayList<ContextHandler>();

		for (HttpCapsule hc : this.httpCapsules) {
			ServerConnector sc = new ServerConnector(this.server);
			sc.setPort(hc.getPort());
			sc.setAcceptQueueSize(hc.getQueueSize());
			// 添加连接器
			connectorList.add(sc);

			// 添加上下文处理器
			List<CapsuleHolder> holders = hc.getCapsuleHolders();
			for (CapsuleHolder holder : holders) {
				ContextHandler context = new ContextHandler(holder.getPathSpec());
				context.setHandler(holder.getHttpHandler());
				contextList.add(context);
			}
		}

		ServerConnector[] connectors = new ServerConnector[connectorList.size()];
		connectorList.toArray(connectors);
		this.server.setConnectors(connectors);

		ContextHandler[] handlers = new ContextHandler[contextList.size()];
		contextList.toArray(handlers);
		ContextHandlerCollection contexts = new ContextHandlerCollection();
		contexts.setHandlers(handlers);
		this.server.setHandler(contexts);

		this.server.setStopTimeout(5000);
		this.server.setStopAtShutdown(true);

		try {
			this.server.start();
		} catch (InterruptedException e) {
			Logger.log(HttpService.class, e, LogLevel.ERROR);
		} catch (Exception e) {
			Logger.log(HttpService.class, e, LogLevel.ERROR);
		}

		return true;
	}

	@Override
	public void shutdown() {
		try {
			this.server.stop();
		} catch (Exception e) {
			Logger.log(HttpService.class, e, LogLevel.WARNING);
		}
	}

	/** 添加服务封装器。
	 */
	public void addCapsule(HttpCapsule capsule) {
		this.httpCapsules.add(capsule);
	}
}
