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

package net.cellcloud.gateway;

import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import net.cellcloud.common.Session;
import net.cellcloud.gateway.GatewayService.Slave;

/**
 * 代理服务路由表。
 * 
 * @author Ambrose Xu
 *
 */
public class RoutingTable {

	/**
	 * 会话记录。键是 Session 的 ID 。
	 */
	private ConcurrentHashMap<Long, Record> sessionRecordMap;

	/**
	 * 构造函数。
	 */
	public RoutingTable() {
		this.sessionRecordMap = new ConcurrentHashMap<Long, Record>();
	}

	/**
	 * 更新会话的请求信息，将一个有效会话关联的标签、下位机和 Cellet 进行映射。
	 * 
	 * @param session 指定需更新的会话。
	 * @param tag 指定该会话对应的标签。
	 * @param slave 指定该会话路由目标下位机。
	 * @param identifier 指定该会话请求的 Cellet 。
	 */
	public void update(Session session, String tag, Slave slave, String identifier) {
		Record record = this.sessionRecordMap.get(session.getId());
		if (null == record) {
			record = new Record();
			record.session = session;
			record.tag = tag;
			record.slave = slave;
			this.sessionRecordMap.put(session.getId(), record);
		}

		if (!record.identifiers.contains(identifier)) {
			record.identifiers.add(identifier);
		}
	}

	/**
	 * 移除指定会话的记录。
	 * 
	 * @param session 指定待移除的会话。
	 * @return 如果移除成功返回对应的记录。
	 */
	public Record remove(Session session) {
		return this.sessionRecordMap.remove(session.getId());
	}

	/**
	 * 删除指定下位机关联的所有会话的记录。
	 * 
	 * @param slave 指定下位机。
	 * @return 返回被删除的会话的记录数量。
	 */
	public int remove(Slave slave) {
		int num = 0;
		Iterator<Record> iter = this.sessionRecordMap.values().iterator();
		while (iter.hasNext()) {
			Record record = iter.next();
			if (record.slave == slave) {
				iter.remove();
				++num;
			}
		}
		return num;
	}

	/**
	 * 查询指定会话对应的下位机。
	 * 
	 * @param session 指定需查询的会话。
	 * @return 返回下位机实例。
	 */
	public Slave querySlave(Session session) {
		Record record = this.sessionRecordMap.get(session.getId());
		if (null != record) {
			return record.slave;
		}

		return null;
	}

	/**
	 * 查询指定会话对应内核标签。
	 * 
	 * @param session 指定需查询的会话。
	 * @return 返回字符串形式的内核标签。
	 */
	public String queryTag(Session session) {
		Record record = this.sessionRecordMap.get(session.getId());
		if (null != record) {
			return record.tag;
		}

		return null;
	}

	/**
	 * 清空当前路由表。
	 */
	public void clear() {
		this.sessionRecordMap.clear();
	}

	/**
	 * 路由信息记录。
	 */
	public class Record {

		/** 会话。 */
		public Session session;

		/** 会话对应的标签。 */
		public String tag;

		/** 此会话的下位机。 */
		public Slave slave;

		/** 此会话请求的 Cellet 清单。 */
		public Vector<String> identifiers = new Vector<String>();

		/** 此记录创建的时间戳。 */
		public long timestamp = System.currentTimeMillis();

	}

}
