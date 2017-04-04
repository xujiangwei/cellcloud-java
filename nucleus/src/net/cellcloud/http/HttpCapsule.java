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

package net.cellcloud.http;

import java.util.ArrayList;
import java.util.List;


/**
 * HTTP 服务节点封装。
 * 
 * @author Ambrose Xu
 * 
 */
public class HttpCapsule {

	/** 封装容器名。 */
	private String name;

	/** 封装的 HTTP 接入处理器。 */
	private ArrayList<CapsuleHolder> holders;

	/** 使用的会话管理器。 */
	private SessionManager sessionManager;

	/**
	 * 构造函数。
	 * 
	 * @param name 指定容器名称。
	 */
	public HttpCapsule(String name) {
		this.name = name;
		this.holders = new ArrayList<CapsuleHolder>();
	}

	/**
	 * 获得容器名。
	 * 
	 * @return 返回容器名。
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 获得 Holder 列表。
	 *
	 * @return 返回 Holder 列表。
	 */
	public List<CapsuleHolder> getHolders() {
		return this.holders;
	}

	/**
	 * 添加接入器。
	 * 
	 * @param holder 指定接入器对象。
	 */
	public void addHolder(CapsuleHolder holder) {
		this.holders.add(holder);

		HttpHandler hh = holder.getHttpHandler();
		if (null != hh) {
			hh.setSessionManager(this.sessionManager);
		}
	}

	/**
	 * 移除接入器。
	 * 
	 * @param holder 指定接入器对象。
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
	 * 
	 * @param sessionManager 指定会话管理器。
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
	 * 获得当前有效的会话管理器。
	 * 
	 * @return 返回会话管理器。
	 */
	public SessionManager getSessionManager() {
		return this.sessionManager;
	}

}
