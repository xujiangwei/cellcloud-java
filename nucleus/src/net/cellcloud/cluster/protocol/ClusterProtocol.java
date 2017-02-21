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

package net.cellcloud.cluster.protocol;

import java.util.Date;
import java.util.Map;

import net.cellcloud.cluster.ClusterNode;
import net.cellcloud.common.Message;
import net.cellcloud.common.Session;
import net.cellcloud.util.Utils;

/**
 * 集群协议接口描述。
 * 
 * @author Ambrose Xu
 * 
 */
public abstract class ClusterProtocol {

	/** 数据键：协议名。 */
	public final static String KEY_PROTOCOL = "Protocol";
	/** 数据键：内核标签。 */
	public final static String KEY_TAG = "Tag";
	/** 数据键：本地时间。 */
	public final static String KEY_DATE = "Date";
	/** 数据键：状态。 */
	public final static String KEY_STATE = "State";
	/** 数据键：节点散列码。 */
	public final static String KEY_HASH = "Hash";
	/** 数据键：负载。 */
	public final static String KEY_PAYLOAD = "Payload";

	/** 数据负载分割符。 */
	public final static byte[] SEPARATOR = new byte[]{ 2, 0, 1, 3, 0, 9, 0, 8 };

	/** 协议名称。 */
	private String name;

	/** 上下文会话。 */
	public Session contextSession;

	/** 属性描述。 */
	private Map<String, Object> prop;

	/**
	 * 构造器。
	 * 
	 * @param name 指定协议名构建协议。
	 */
	public ClusterProtocol(String name) {
		this.name = name;
		this.prop = null;
	}

	/**
	 * 构造器。
	 * 
	 * @param name 指定协议名构建协议。
	 * @param prop 指定协议属性。
	 */
	public ClusterProtocol(String name, Map<String, Object> prop) {
		this.name = name;
		this.prop = prop;
	}

	/**
	 * 获得协议名称。
	 * 
	 * @return 返回协议名。
	 */
	public final String getName() {
		return this.name;
	}

	/**
	 * 获得标准日期描述。
	 * 
	 * @return 返回字符串形式的标准日期描述。
	 */
	public final String getStandardDate() {
		return Utils.gsDateFormat.format(new Date());
	}

	/**
	 * 获得节点标签。
	 * 
	 * @return 返回协议内传输的标签。
	 */
	public final String getTag() {
		return this.prop.get(KEY_TAG).toString();
	}

	/**
	 * 获得协议状态。
	 * 
	 * @return 返回整数形式的协议状态。
	 */
	public int getStateCode() {
		Object oState = this.prop.get(KEY_STATE);
		if (null == oState) {
			return -1;
		}

		if (oState instanceof Integer) {
			return ((Integer) oState).intValue();
		}
		else {
			return Integer.parseInt(oState.toString());
		}
	}

	/**
	 * 获得物理节点的 Hash 值。
	 * 
	 * @return 返回物理节点 Hash 值。
	 */
	public long getHash() {
		Object oHash = this.prop.get(KEY_HASH);

		if (oHash instanceof Long) {
			return ((Long) oHash).longValue();
		}
		else {
			return Long.parseLong(oHash.toString());
		}
	}

	/**
	 * 获得指定键对应的属性值。
	 * 
	 * @param key 指定数据键。
	 * @return 返回字符串形式的指定键对应的属性值。
	 */
	public Object getProp(String key) {
		return (null != this.prop) ? this.prop.get(key) : null;
	}

	/**
	 * 启动并执行协议。
	 * 
	 * @param session 指定执行协议的会话。
	 */
	abstract public void launch(Session session);

	/**
	 * 向指定 Session 回送执行结果。
	 * 
	 * @param node 指定应答节点。
	 * @param state 指定应当状态。
	 * @param custom 指定需要应答的自定义数据。
	 */
	abstract public void respond(ClusterNode node, StateCode state, Object custom);

	/**
	 * 协议打包处理并发送。
	 * 
	 * @param session 指定会话。
	 * @param buffer 指定存储数据的缓存。
	 */
	protected void touch(Session session, StringBuilder buffer, byte[] payload) {
		Message message = null;
		if (null != payload) {
			byte[] data = Utils.string2Bytes(buffer.toString());
			byte[] tail = new byte[]{ '\r', '\n', '\r', '\n' };
			// 合并后的数据
			byte[] bytes = new byte[data.length + payload.length + tail.length];
			System.arraycopy(data, 0, bytes, 0, data.length);
			System.arraycopy(payload, 0, bytes, data.length, payload.length);
			System.arraycopy(tail, 0, bytes, data.length + payload.length, tail.length);
			message = new Message(bytes);
			data = null;
			tail = null;
		}
		else {
			buffer.append("\r\n\r\n");
			message = new Message(Utils.string2Bytes(buffer.toString()));
		}

		session.write(message);
	}

	/**
	 * 协议状态。
	 */
	public enum StateCode {

		/** 成功。 */
		SUCCESS(200),

		/** 操作被拒绝。 */
		REJECT(201),

		/** 操作失败。 */
		FAILURE(400);

		private int code;

		private StateCode(int code) {
			this.code = code;
		}

		/**
		 * 返回状态编码。
		 * 
		 * @return 返回状态编码。
		 */
		public int getCode() {
			return this.code;
		}

		@Override
		public String toString() {
			return String.valueOf(this.code);
		}

	}

}
