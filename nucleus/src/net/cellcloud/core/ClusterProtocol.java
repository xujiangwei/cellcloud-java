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

import java.util.Date;

import net.cellcloud.common.Message;
import net.cellcloud.common.Session;
import net.cellcloud.util.Util;

/** 集群协议。
 * 
 * @author Jiangwei Xu
 */
public abstract class ClusterProtocol {

	// 协议名
	public final static String KEY_PROTOCOL = "Protocol";
	// 内核标签
	public final static String KEY_TAG = "Tag";
	// 本地时间
	public final static String KEY_DATE = "Date";
	// 状态
	public final static String KEY_STATE = "State";
	// 节点散列码
	public final static String KEY_HASHCODE = "HashCode";

	private String name;

	// 上下文会话
	protected Session contextSession;

	public ClusterProtocol(String name) {
		this.name = name;
	}

	/** 返回协议名。
	 */
	public final String getName() {
		return this.name;
	}

	/** 返回标准日期。
	 */
	public final String getStandardDate() {
		return Util.sDateFormat.format(new Date());
	}

	/** 启动协议。
	 */
	abstract public void launch(Session session);

	/** 向指定 Session 回送执行结果。
	 */
	abstract public void stack(Session session);

	/** 向指定 Session 回送拒绝。
	 */
	abstract public void stackReject(Session session);

	/** 协议收尾处理。
	 */
	protected void touch(Session session, StringBuilder buffer) {
		buffer.append("\r\n\r\n");
		Message message = new Message(buffer.toString());
		session.write(message);
	}

	/** 协议状态。
	 */
	public final class StateCode {
		/// 成功
		public final static int SUCCESS = 200;
		/// 拒绝操作
		public final static int REJECT = 201;

		private StateCode() {
		}
	}
}
