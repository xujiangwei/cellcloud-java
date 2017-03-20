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

package net.cellcloud.talk;

/**
 * Talk Service 快照。
 * 
 * @author Ambrose Xu
 * 
 */
public final class TalkSnapshoot {

	/** 当前一共累计的有效验证的会话数量。 */
	public long numValidSessions = 0;
	/** 当前一共累计的无效验证的会话数量。 */
	public long numInvalidSessions = 0;

	/** 标准协议的服务绑定端口。 */
	public int port = 0;
	/** 标准协议的当前连接数。  */
	public int connections = 0;
	/** 标准协议的最大连接数。 */
	public int maxConnections = 0;
	/** 标准协议的工作线程数量。  */
	public int numWorkers = 0;
	/** 标准协议的每个工作线程接收的数据流量（字节）。 */
	public long[] networkRx = null;
	/** 标准协议的每个工作线程发送的数据流量（字节）。 */
	public long[] networkTx = null;

	/** WebSocket 协议的服务绑定端口。 */
	public int webSocketPort = 0;
	/** WebSocket 协议的当前连接数。 */
	public int webSocketConnections = 0;
	/** WebSocket 协议的空闲任务数。 */
	public int webSocketIdleTasks = 0;
	/** WebSocket 协议的活跃任务数。 */
	public int webSocketActiveTasks = 0;
	/** WebSocket 协议的总接收数据流量（字节）。 */
	public long webSocketRx = 0;
	/** WebSocket 协议的总发送数据流量（字节）。 */
	public long webSocketTx = 0;

	/** WebSocketSecure 协议的服务绑定端口。 */
	public int webSocketSecurePort = 0;
	/** WebSocketSecure 协议的当前连接数。 */
	public int webSocketSecureConnections = 0;
	/** WebSocketSecure 协议的空闲任务数。 */
	public int webSocketSecureIdleTasks = 0;
	/** WebSocketSecure 协议的活跃任务数。 */
	public int webSocketSecureActiveTasks = 0;
	/** WebSocketSecure 协议的总接收数据流量（字节）。 */
	public long webSocketSecureRx = 0;
	/** WebSocketSecure 协议的总发送数据流量（字节）。 */
	public long webSocketSecureTx = 0;

	/** HTTP 协议的服务绑定端口。 */
	public int httpPort = 0;
	/** HTTPS 协议的服务绑定端口。 */
	public int httpsPort = 0;
	/** HTTP/HTTPS 协议服务的最大队列长度。 */
	public int httpQueueSize = 0;
	/** HTTP/HTTPS 协议当前活跃会话数量。 */
	public int httpSessionNum = 0;
	/** HTTP/HTTPS 协议最大允许活跃会话数。 */
	public int httpSessionMaxNum = 0;
	/** HTTP/HTTPS 协议会话有效期。 */
	public long httpSessionExpires = 0;

	/** 当前正在运行的动作方言线程数量。 */
	public int actionDialectThreadNum = 0;
	/** 动作方言最大线程数量。 */
	public int actionDialectMaxThreadNum = 0;
	/** 处于待处理状态的动作方言数量。 */
	public int actionDialectPendingNum = 0;

	/** 区块方言缓存数量。 */
	public int chunkDialectCacheNum = 0;
	/** 区块方言当前内存缓存大小。 */
	public long chunkDialectCacheMemSize = 0;
	/** 区块方言最大内存缓存大小。 */
	public long chunkDialectMaxCacheMemSize = 0;
	/** 区块方言当前队列长度。 */
	public int chunkDialectQueueSize = 0;

	/**
	 * 构造函数。
	 */
	protected TalkSnapshoot() {
	}

}
