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

import java.net.InetSocketAddress;

import net.cellcloud.common.Session;
import net.cellcloud.core.Endpoint;
import net.cellcloud.core.NucleusConfig;

/** Talk 会话上下文。
 * 
 * @author Jiangwei Xu
 */
public final class TalkSessionContext {

	private Session session;

	private String tag;

	private Endpoint endpoint;

	private TalkTracker tracker;

	public long tickTime = 0;

	/** 构造函数。
	 */
	public TalkSessionContext(Session session, String tag, InetSocketAddress address) {
		this.session = session;
		this.tag = tag;
		this.endpoint = new Endpoint(tag, NucleusConfig.Role.CONSUMER, address);
		this.tracker = new TalkTracker();
	}

	/** 返回上下文对应的 Session 。
	 */
	public Session getSession() {
		return this.session;
	}

	/**
	 * 返回标签。
	 * @return
	 */
	public String getTag() {
		return this.tag;
	}

	/** 返回终端。
	 */
	public Endpoint getEndpoint() {
		return this.endpoint;
	}

	/**
	 * 返回追踪器。
	 * @return
	 */
	public TalkTracker getTracker() {
		return this.tracker;
	}
}
