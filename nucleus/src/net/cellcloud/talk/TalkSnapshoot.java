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

package net.cellcloud.talk;

/** Talk service 快照。
 * 
 * @author Jiangwei Xu
 */
public final class TalkSnapshoot {

	public long numValidSessions = 0;
	public long numInvalidSessions = 0;

	public int port = 0;
	public int connections = 0;
	public int maxConnections = 0;
	public int numWorkers = 0;
	public long[] networkRx = null;
	public long[] networkTx = null;

	public int webSocketPort = 0;
	public int webSocketConnections = 0;
	public int webSocketIdleTasks = 0;
	public int webSocketActiveTasks = 0;
	public long webSocketRx = 0;
	public long webSocketTx = 0;

	public int webSocketSecurePort = 0;
	public int webSocketSecureConnections = 0;
	public int webSocketSecureIdleTasks = 0;
	public int webSocketSecureActiveTasks = 0;
	public long webSocketSecureRx = 0;
	public long webSocketSecureTx = 0;

	public int httpPort = 0;
	public int httpsPort = 0;
	public int httpQueueSize = 0;
	public int httpSessionNum = 0;
	public int httpSessionMaxNum = 0;
	public long httpSessionExpires = 0;

	public int actionDialectThreadNum = 0;
	public int actionDialectMaxThreadNum = 0;
	public int actionDialectPendingNum = 0;

	public int chunkDialectCacheNum = 0;
	public long chunkDialectCacheMemSize = 0;
	public long chunkDialectMaxCacheMemSize = 0;
	public int chunkDialectQueueSize = 0;

	protected TalkSnapshoot() {
	}
}
