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
import net.cellcloud.util.Clock;

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
	 * 过期失效的会话记录。
	 */
	private Vector<Record> expiredRecord;

	/**
	 * 终端地址对应的下位机映射。
	 */
	private ConcurrentHashMap<String, Slave> addressMap;

	/**
	 * 构造函数。
	 */
	public RoutingTable() {
		this.sessionRecordMap = new ConcurrentHashMap<Long, Record>();
		this.expiredRecord = new Vector<Record>();
		this.addressMap = new ConcurrentHashMap<String, Slave>();
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

			this.addressMap.put(session.getAddress().getHostString().toString(), slave);
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
		this.addressMap.remove(session.getAddress().getHostString());
		Record record = this.sessionRecordMap.remove(session.getId());
		if (null != record) {
			record.expiredTimestamp = Clock.currentTimeMillis();
			this.expiredRecord.add(record);
		}
		return record;
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
				this.expiredRecord.remove(record);
				++num;
			}
		}
		Iterator<Slave> siter = this.addressMap.values().iterator();
		while (siter.hasNext()) {
			if (siter.next() == slave) {
				siter.remove();
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
	 * 通过地址查询对应的下位机。
	 * 
	 * @param remoteAddress 指定需查询的终端地址。
	 * @return 返回下位机。
	 */
	public Slave querySlaveByAddress(String remoteAddress) {
		return this.addressMap.get(remoteAddress);
	}

	/**
	 * 通过终端的内核标签查找指定的下位机。
	 * 
	 * @param tag
	 * @return
	 */
	public Slave querySlaveByTag(String tag) {
		Iterator<Record> iter = this.sessionRecordMap.values().iterator();
		while (iter.hasNext()) {
			Record r = iter.next();
			if (r.tag.equals(tag)) {
				return r.slave;
			}
		}

		iter = this.expiredRecord.iterator();
		while (iter.hasNext()) {
			Record r = iter.next();
			if (r.tag.equals(tag)) {
				return r.slave;
			}
		}

		return null;
	}

	/**
	 * 更新地址对应的下位机。
	 * 
	 * @param address 指定地址。
	 * @param slave 指定下位机。
	 */
	public void updateAddress(String address, Slave slave) {
		this.addressMap.put(address, slave);
	}

	/**
	 * 刷新过期记录。
	 */
	public void refreshExpiredRecord() {
		if (this.expiredRecord.isEmpty()) {
			return;
		}

		Iterator<Record> iter = this.expiredRecord.iterator();
		while (iter.hasNext()) {
			Record record = iter.next();
			if (Clock.currentTimeMillis() - record.expiredTimestamp > 60000L) {
				iter.remove();
			}
		}
	}

	/**
	 * 清空当前路由表。
	 */
	public void clear() {
		this.sessionRecordMap.clear();
		this.addressMap.clear();
		this.expiredRecord.clear();
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
		public long timestamp = Clock.currentTimeMillis();

		/** 过期时的时间戳。 */
		public long expiredTimestamp = 0;

	}

}
