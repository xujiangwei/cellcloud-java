/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2013 Cell Cloud Team (cellcloudproject@gmail.com)

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

import java.net.InetSocketAddress;
import java.util.Observable;

import net.cellcloud.common.Message;
import net.cellcloud.common.MessageHandler;
import net.cellcloud.common.NonblockingConnector;
import net.cellcloud.common.Session;

/** 集群连接器。
 * 
 * @author Jiangwei Xu
 */
public final class ClusterConnector extends Observable implements MessageHandler, Comparable<ClusterConnector> {

	protected final static String SUBJECT_DISCOVERING = "discovering";

	private long hash;
	private InetSocketAddress address;

	private NonblockingConnector connector;
	private ClusterConnectorFuture discoverFuture;

	public ClusterConnector(InetSocketAddress address, long hash) {
		this.address = address;
		this.hash = hash;
		this.connector = new NonblockingConnector();
	}

	/** 返回连接器散列码。
	 */
	public long getHashCode() {
		return this.hash;
	}

	/** 返回连接器地址。
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/** 执行发现。
	 */
	public ClusterConnectorFuture discover() {
		this.discoverFuture = new ClusterConnectorFuture(SUBJECT_DISCOVERING);
		if (this.connector.isConnected()) {
			this.discoverFuture.started = true;
		}
		else {
//			if (this.connector.connect(this.address)) {
//				future.started = true;
//			}
//			else {
//				future.started = false;
//				future.aborted(this);
//			}
		}

		return this.discoverFuture;
	}

	@Override
	public int compareTo(ClusterConnector other) {
		return (int)(this.hash - other.hash);
	}

	@Override
	public int hashCode() {
		return (int)this.hash;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ClusterConnector) {
			return this.hash == ((ClusterConnector)other).hash;
		}
		else {
			return false;
		}
	}

	@Override
	public void sessionCreated(Session session) {
	}

	@Override
	public void sessionDestroyed(Session session) {
	}

	@Override
	public void sessionOpened(Session session) {
	}

	@Override
	public void sessionClosed(Session session) {
	}

	@Override
	public void messageReceived(Session session, Message message) {
	}

	@Override
	public void messageSent(Session session, Message message) {
		// Nothing
	}

	@Override
	public void errorOccurred(int errorCode, Session session) {
	}

	protected void notifyDiscoveringAborted() {
		synchronized (this) {
			this.setChanged();
			this.notifyObservers(this.discoverFuture);
			this.clearChanged();
		}
	}

	protected void notifyDiscoveringCompleted() {
		synchronized (this) {
			this.setChanged();
			this.notifyObservers(this.discoverFuture);
			this.clearChanged();
		}
	}
}
