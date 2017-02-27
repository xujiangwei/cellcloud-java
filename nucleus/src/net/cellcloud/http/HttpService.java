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

package net.cellcloud.http;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.MessageHandler;
import net.cellcloud.common.Service;
import net.cellcloud.core.NucleusContext;
import net.cellcloud.exception.SingletonException;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.resource.FileResource;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/** HTTP 服务。
 * 
 * @author Jiangwei Xu
 */
public final class HttpService implements Service {

	private static HttpService instance = null;

	private Server httpServer = null;
	private Server httpsServer = null;

	private HttpCrossDomainHandler httpCrossDomainHandler = null;
	private HttpCrossDomainHandler httpsCrossDomainHandler = null;

	protected LinkedList<HttpCapsule> capsules = null;

	// HTTP URI 上下文 Holder
	protected ConcurrentHashMap<String, CapsuleHolder> holders;

	// WebSocket
	private Server wsServer = null;
	private int wsPort = 7777;
	private int wsQueueSize = 1000;
	private JettyWebSocket webSocket;

	// WebSocket Secure
	private Server wssServer = null;
	private int wssPort = 7778;
	private int wssQueueSize = 1000;
	private JettyWebSocket webSocketSecure;

	/**
	 * 构造函数。
	 * @param context
	 * @throws SingletonException
	 */
	public HttpService(NucleusContext context) throws SingletonException {
		if (null == HttpService.instance) {
			HttpService.instance = this;

			// 设置 Jetty 的日志傀儡
			org.eclipse.jetty.util.log.Log.setLog(new JettyLoggerPuppet());

			this.capsules = new LinkedList<HttpCapsule>();

			this.holders = new ConcurrentHashMap<String, CapsuleHolder>();
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
		// 启动 HTTP 服务器
		if (null != this.httpServer) {
			ArrayList<ContextHandler> contextList = new ArrayList<ContextHandler>();

			for (HttpCapsule hc : this.capsules) {
				// 添加上下文处理器
				List<CapsuleHolder> holders = hc.getHolders();
				for (CapsuleHolder holder : holders) {
					ContextHandler context = new ContextHandler(holder.getPathSpec());
					context.setHandler(holder.getHttpHandler());
					contextList.add(context);

					// 记录 Holder
					this.holders.put(holder.getPathSpec(), holder);
				}
			}

			// 添加跨域支持
			this.httpCrossDomainHandler = new HttpCrossDomainHandler(this);
			ContextHandler cdContext = new ContextHandler(this.httpCrossDomainHandler.getPathSpec());
			cdContext.setHandler(this.httpCrossDomainHandler);
			contextList.add(cdContext);

			// 处理器
			ContextHandler[] handlers = new ContextHandler[contextList.size()];
			contextList.toArray(handlers);
			ContextHandlerCollection contexts = new ContextHandlerCollection();
			contexts.setHandlers(handlers);
			this.httpServer.setHandler(contexts);

			try {
				this.httpServer.start();
				Logger.i(this.getClass(), "Started HTTP server at " + ((ServerConnector)this.httpServer.getConnectors()[0]).getPort());
			} catch (InterruptedException e) {
				Logger.log(HttpService.class, e, LogLevel.ERROR);
			} catch (Exception e) {
				Logger.log(HttpService.class, e, LogLevel.ERROR);
			}
		}

		// 启动 HTTPS 服务器
		if (null != this.httpsServer) {
			ArrayList<ContextHandler> httpsContextList = new ArrayList<ContextHandler>();

			for (HttpCapsule hc : this.capsules) {
				// 添加上下文处理器
				List<CapsuleHolder> holders = hc.getHolders();
				for (CapsuleHolder holder : holders) {
					ContextHandler httpsContext = new ContextHandler(holder.getPathSpec());
					httpsContext.setHandler(holder.getHttpHandler());
					httpsContextList.add(httpsContext);
				}
			}

			// 添加跨域支持
			this.httpsCrossDomainHandler = new HttpCrossDomainHandler(this);
			ContextHandler httpsCDContext = new ContextHandler(this.httpsCrossDomainHandler.getPathSpec());
			httpsCDContext.setHandler(this.httpsCrossDomainHandler);
			httpsContextList.add(httpsCDContext);

			// 处理器
			ContextHandler[] httpsHandlers = new ContextHandler[httpsContextList.size()];
			httpsContextList.toArray(httpsHandlers);
			ContextHandlerCollection httpsContexts = new ContextHandlerCollection();
			httpsContexts.setHandlers(httpsHandlers);
			this.httpsServer.setHandler(httpsContexts);

			try {
				this.httpsServer.start();
				Logger.i(this.getClass(), "Started HTTPS server at " + ((ServerConnector)(this.httpsServer.getConnectors()[0])).getPort());
			} catch (InterruptedException e) {
				Logger.log(HttpService.class, e, LogLevel.ERROR);
			} catch (Exception e) {
				Logger.log(HttpService.class, e, LogLevel.ERROR);
			}
		}

		// WebSocket 支持
		if (null != this.wsServer) {
			try {
				this.wsServer.start();
				Logger.i(this.getClass(), "Started WS server at " + this.wsPort);
			} catch (Exception e) {
				Logger.log(HttpService.class, e, LogLevel.ERROR);
			}
		}

		// WebSocket Secure 支持
		if (null != this.wssServer) {
			try {
				this.wssServer.start();
				Logger.i(this.getClass(), "Started WSS server at " + this.wssPort);
			} catch (Exception e) {
				Logger.log(HttpService.class, e, LogLevel.ERROR);
			}
		}

		return true;
	}

	@Override
	public void shutdown() {
		if (null != this.httpServer) {
			try {
				this.httpServer.stop();
			} catch (Exception e) {
				Logger.log(HttpService.class, e, LogLevel.WARNING);
			}

			this.httpServer = null;
		}

		if (null != this.httpsServer) {
			try {
				this.httpsServer.stop();
			} catch (Exception e) {
				Logger.log(HttpService.class, e, LogLevel.WARNING);
			}

			this.httpsServer = null;
		}

		if (null != this.wsServer) {
			try {
				this.wsServer.stop();
			} catch (Exception e) {
				Logger.log(HttpService.class, e, LogLevel.WARNING);
			}

			this.wsServer = null;
		}

		if (null != this.wssServer) {
			try {
				this.wssServer.stop();
			} catch (Exception e) {
				Logger.log(HttpService.class, e, LogLevel.WARNING);
			}

			this.wssServer = null;
		}

		if (null != this.holders) {
			this.holders.clear();
		}
	}

	/** 添加服务节点。
	 */
	public void addCapsule(HttpCapsule capsule) {
		this.capsules.add(capsule);
	}

	/** 删除服务节点。
	 */
	public void removeCapsule(HttpCapsule capsule) {
		if (null == capsule) {
			return;
		}

		this.capsules.remove(capsule);
	}

	public List<HttpCapsule> getCapsules() {
		ArrayList<HttpCapsule> list = new ArrayList<HttpCapsule>(this.capsules.size());
		list.addAll(this.capsules);
		return list;
	}

	public HttpCapsule getCapsule(String name) {
		for (HttpCapsule capsule : this.capsules) {
			if (capsule.getName().equals(name)) {
				return capsule;
			}
		}

		return null;
	}

	public boolean hasCapsule(String name) {
		for (HttpCapsule capsule : this.capsules) {
			if (capsule.getName().equals(name)) {
				return true;
			}
		}

		return false;
	}

	public boolean activateHttp(int[] ports, long idleTimeout, int acceptQueueSize) {
		if (ports.length == 0) {
			return false;
		}

		// 创建 HTTP 服务器
		this.httpServer = new Server();
		// 设置错误处理句柄
		this.httpServer.addBean(new DefaultErrorHandler());

		// 连接器
		ServerConnector[] connectors = new ServerConnector[ports.length];
		for (int i = 0; i < ports.length; ++i) {
			@SuppressWarnings("resource")
			ServerConnector connector = new ServerConnector(this.httpServer);
			connector.setPort(ports[i]);
			connector.setIdleTimeout(idleTimeout);
			connector.setAcceptQueueSize(acceptQueueSize);
			connectors[i] = connector;
		}
		this.httpServer.setConnectors(connectors);

		this.httpServer.setStopTimeout(1000);
		this.httpServer.setStopAtShutdown(true);

		return true;
	}

	public boolean activateHttpSecure(int port, long idleTimeout, int acceptQueueSize, String jksResource, String keyStorePassword, String keyManagerPassword) {
		if (null == jksResource || null == keyStorePassword || null == keyManagerPassword) {
			Logger.w(this.getClass(), "No key store password, can NOT start HTTP Secure service");
			return false;
		}

		// 创建 HTTPS 服务器
		this.httpsServer = new Server();
		// 设置错误处理句柄
		this.httpsServer.addBean(new DefaultErrorHandler());

		HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(new HttpConfiguration());

		SslContextFactory sslContextFactory = new SslContextFactory();
		URL url = this.getClass().getResource(jksResource);

		Logger.i(this.getClass(), "HTTPS key store file: " + url.toString());

		try {
			sslContextFactory.setKeyStoreResource(new FileResource(url));
		} catch (IOException | URISyntaxException exception) {
			Logger.log(this.getClass(), exception, LogLevel.ERROR);
			this.httpsServer = null;
			return false;
		}

		sslContextFactory.setKeyStorePassword(keyStorePassword);
		sslContextFactory.setKeyManagerPassword(keyManagerPassword);

		ServerConnector httpsConnector = new ServerConnector(this.httpsServer,
				new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),  httpConnectionFactory);
		httpsConnector.setPort(port);
		httpsConnector.setIdleTimeout(idleTimeout);
		httpsConnector.setAcceptQueueSize(acceptQueueSize);
		this.httpsServer.addConnector(httpsConnector);

		this.httpsServer.setStopTimeout(1000);
		this.httpsServer.setStopAtShutdown(true);

		return true;
	}

	public WebSocketManager activeWebSocketSecure(int port, int queueSize, MessageHandler handler
			, String jksResource, String keyStorePassword, String keyManagerPassword) {
		if (null == jksResource || null == keyStorePassword || null == keyManagerPassword || port <= 80) {
			Logger.w(this.getClass(), "No key store password, can NOT start Web Socket Secure service");
			return null;
		}

		if (null != this.wssServer) {
			return this.webSocketSecure;
		}

		this.wssPort = port;
		this.wssQueueSize = queueSize;
		this.webSocketSecure = new JettyWebSocket(handler);

		this.wssServer = new Server();

		SslContextFactory sslContextFactory = new SslContextFactory();
		try {
			URL url = this.getClass().getResource(jksResource);

			Logger.i(this.getClass(), "WSS key store file: " + url.toString());

			sslContextFactory.setKeyStoreResource(new FileResource(url));
		} catch (IOException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
			this.wssServer = null;
			this.webSocketSecure = null;
			return null;
		} catch (URISyntaxException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
			this.wssServer = null;
			this.webSocketSecure = null;
			return null;
		} catch (Exception e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
			this.wssServer = null;
			this.webSocketSecure = null;
			return null;
		}
		sslContextFactory.setKeyStorePassword(keyStorePassword);
		sslContextFactory.setKeyManagerPassword(keyManagerPassword);
		SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString());
		HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(new HttpConfiguration());
		ServerConnector sslConnector = new ServerConnector(this.wssServer, sslConnectionFactory, httpConnectionFactory);
		sslConnector.setPort(this.wssPort);
		sslConnector.setAcceptQueueSize(this.wssQueueSize);

		this.wssServer.addConnector(sslConnector);

		JettyWebSocketHandler wsh = new JettyWebSocketHandler(this.webSocketSecure);

		ContextHandler wsContextHandler = new ContextHandler();
		wsContextHandler.setHandler(wsh);
		wsContextHandler.setContextPath("/wss");

//		ResourceHandler rHandler = new ResourceHandler();
//		rHandler.setDirectoriesListed(true);
//		rHandler.setResourceBase("wss");
//		wsh.setHandler(rHandler);

		this.wssServer.setHandler(wsh);

		return this.webSocketSecure;
	}

	public void deactiveWebSocketSecure() {
		if (this.wssServer != null) {
			try {
				this.wssServer.stop();
			} catch (Exception e) {
				Logger.log(HttpService.class, e, LogLevel.WARNING);
			}

			this.wssServer = null;
			this.webSocketSecure = null;
		}
	}

	/**
	 * 激活 WebSocket 服务。
	 * @param port 指定 WebSocket 接口。
	 */
	public WebSocketManager activeWebSocket(int port, int queueSize, MessageHandler handler) {
		if (port <= 80) {
			return null;
		}

		if (null != this.wsServer) {
			return this.webSocket;
		}

		this.wsPort = port;
		this.wsQueueSize = queueSize;
		this.webSocket = new JettyWebSocket(handler);

		this.wsServer = new Server();

		ServerConnector connector = new ServerConnector(this.wsServer);
		connector.setPort(this.wsPort);
		connector.setAcceptQueueSize(this.wsQueueSize);

		this.wsServer.addConnector(connector);

		JettyWebSocketHandler wsh = new JettyWebSocketHandler(this.webSocket);

		ContextHandler wsContextHandler = new ContextHandler();
		wsContextHandler.setHandler(wsh);
		wsContextHandler.setContextPath("/ws");

//		ResourceHandler rHandler = new ResourceHandler();
//		rHandler.setDirectoriesListed(true);
//		rHandler.setResourceBase("ws");
//		wsh.setHandler(rHandler);

		this.wsServer.setHandler(wsh);

		return this.webSocket;
	}

	/**
	 * 禁用 WebSocket 服务。
	 */
	public void deactiveWebSocket() {
		if (this.wsServer != null) {
			try {
				this.wsServer.stop();
			} catch (Exception e) {
				Logger.log(HttpService.class, e, LogLevel.WARNING);
			}

			this.wsServer = null;
			this.webSocket = null;
		}
	}

	public int getHttpPort() {
		return (null != this.httpServer) ? ((ServerConnector)this.httpServer.getConnectors()[0]).getPort() : 0;
	}

	public int getHttpsPort() {
		return (null != this.httpsServer) ? ((ServerConnector)(this.httpsServer.getConnectors()[0])).getPort() : 0;
	}

	public int getWebSocketPort() {
		return this.wsPort;
	}

	public int getWebSocketSecurePort() {
		return this.wssPort;
	}

	public int getWebSocketSessionNum() {
		if (null != this.webSocket) {
			return this.webSocket.numSessions();
		}

		return 0;
	}

	public int getWebSocketSecureSessionNum() {
		if (null != this.webSocketSecure) {
			return this.webSocketSecure.numSessions();
		}

		return 0;
	}

	public long getTotalWSRx() {
		if (null != this.webSocket) {
			return this.webSocket.getTotalRx();
		}

		return 0;
	}

	public long getTotalWSSRx() {
		if (null != this.webSocketSecure) {
			return this.webSocketSecure.getTotalRx();
		}

		return 0;
	}

	public long getTotalWSTx() {
		if (null != this.webSocket) {
			return this.webSocket.getTotalTx();
		}

		return 0;
	}

	public long getTotalWSSTx() {
		if (null != this.webSocketSecure) {
			return this.webSocketSecure.getTotalTx();
		}

		return 0;
	}

}
