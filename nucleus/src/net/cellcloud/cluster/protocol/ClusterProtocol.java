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

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.cellcloud.cluster.ClusterNode;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
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
	/** 数据键：序号。 */
	public final static String KEY_SN = "SN";
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
	 * 构造函数。
	 * 
	 * @param name 指定协议名构建协议。
	 */
	public ClusterProtocol(String name) {
		this.name = name;
		this.prop = null;
	}

	/**
	 * 构造函数。
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
		Object state = this.prop.get(KEY_STATE);
		if (null == state) {
			return -1;
		}

		if (state instanceof Integer) {
			return ((Integer) state).intValue();
		}
		else {
			return Integer.parseInt(state.toString());
		}
	}

	/**
	 * 获得协议的 SN 。
	 * 
	 * @return 返回协议的 SN 。
	 */
	public String getSN() {
		Object sn = this.prop.get(KEY_SN);

		if (sn instanceof String) {
			return (String) sn;
		}
		else {
			return sn.toString();
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
	 * 设置指定键值的属性。
	 * 
	 * @param key 指定数据键。
	 * @param value 指定数据值。
	 */
	public void setProp(String key, Object value) {
		if (null == this.prop) {
			this.prop = new HashMap<String, Object>();
		}

		this.prop.put(key, value);
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

		try {
			session.write(message);
		} catch (IOException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		}
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
