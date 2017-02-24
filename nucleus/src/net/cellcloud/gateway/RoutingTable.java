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

	private ConcurrentHashMap<Long, Record> sessionRecordMap;

	public RoutingTable() {
		this.sessionRecordMap = new ConcurrentHashMap<Long, Record>();
	}

	public void update(Session session, String tag, Slave slave, String identifier) {
		Record record = this.sessionRecordMap.get(session.getId());
		if (null == record) {
			record = new Record();
			record.session = session;
			record.tag = tag;
			record.slave = slave;
			this.sessionRecordMap.put(session.getId(), record);
		}

		if (!record.runtimeIdentifiers.contains(identifier)) {
			record.runtimeIdentifiers.add(identifier);
		}
	}

	public Record remove(Session session) {
		return this.sessionRecordMap.remove(session.getId());
	}

	public void remove(Slave slave) {
		Iterator<Record> iter = this.sessionRecordMap.values().iterator();
		while (iter.hasNext()) {
			Record record = iter.next();
			if (record.slave == slave) {
				iter.remove();
			}
		}
	}

	public Slave querySlave(Session session) {
		Record record = this.sessionRecordMap.get(session.getId());
		if (null != record) {
			return record.slave;
		}

		return null;
	}

	public String queryTag(Session session) {
		Record record = this.sessionRecordMap.get(session.getId());
		if (null != record) {
			return record.tag;
		}

		return null;
	}

	public void clear() {
		this.sessionRecordMap.clear();
	}

	public class Record {
		public Session session;
		public String tag;
		public Slave slave;
		public Vector<String> runtimeIdentifiers = new Vector<String>();
		public long timestamp = System.currentTimeMillis();
	}

}
