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

package net.cellcloud.http;

import java.util.ArrayList;
import java.util.List;


/** HTTP 服务节点封装。
 * 
 * @author Jiangwei Xu
 */
public class HttpCapsule {

	private String name;

	private int queueSize;

	private ArrayList<CapsuleHolder> holders;

	private SessionManager sessionManager;

	public HttpCapsule(String name, int queueSize) {
		this.name = name;
		this.queueSize = queueSize;
		this.holders = new ArrayList<CapsuleHolder>();
	}

	public String getName() {
		return this.name;
	}

	/** 返回连接器队列长度。
	 * @return
	 */
	public int getQueueSize() {
		return this.queueSize;
	}

	/** 返回 Holder 列表。
	 * @return
	 */
	public List<CapsuleHolder> getHolders() {
		return this.holders;
	}

	/** 添加接入器。
	 * @param holder
	 */
	public void addHolder(CapsuleHolder holder) {
		this.holders.add(holder);

		HttpHandler hh = holder.getHttpHandler();
		if (null != hh) {
			hh.setSessionManager(this.sessionManager);
		}
	}

	/** 移除接入器。
	 * @param holder
	 */
	public void removeHolder(CapsuleHolder holder) {
		this.holders.remove(holder);

		HttpHandler hh = holder.getHttpHandler();
		if (null != hh) {
			hh.setSessionManager(null);
		}
	}

	/**
	 * 设置会话管理器。
	 * @param sessionManager
	 */
	public void setSessionManager(SessionManager sessionManager) {
		this.sessionManager = sessionManager;

		for (CapsuleHolder holder : this.holders) {
			HttpHandler hh = holder.getHttpHandler();
			if (null != hh) {
				hh.setSessionManager(this.sessionManager);
			}
		}
	}

	/**
	 * 返回会话管理器。
	 * @return
	 */
	public SessionManager getSessionManager() {
		return this.sessionManager;
	}
}
