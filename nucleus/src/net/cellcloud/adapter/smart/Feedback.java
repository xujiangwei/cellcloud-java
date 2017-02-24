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

package net.cellcloud.adapter.smart;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.cellcloud.core.Endpoint;
import net.cellcloud.util.Clock;

/**
 * 用于记录对端回馈信息的类。包括正、负回馈计数和时间戳。
 * 
 * @author Ambrose Xu
 *
 */
public class Feedback {

	/**
	 * 该回馈信息对应的关键字。
	 */
	private String keyword;

	/**
	 * 最近一次数据更新的时间戳。
	 */
	private long timestamp;

	/**
	 * 正回馈计数映射。
	 */
	private ConcurrentHashMap<Endpoint, AtomicInteger> positiveMap;

	/**
	 * 负回馈计数映射。
	 */
	private ConcurrentHashMap<Endpoint, AtomicInteger> negativeMap;

	/**
	 * 构造函数。
	 * 
	 * @param keyword 指定关联的关键字。
	 */
	public Feedback(String keyword) {
		this.keyword = keyword;
		this.positiveMap = new ConcurrentHashMap<Endpoint, AtomicInteger>();
		this.negativeMap = new ConcurrentHashMap<Endpoint, AtomicInteger>();
		this.timestamp = Clock.currentTimeMillis();
	}

	/**
	 * 获得其所关联的关键字。
	 * 
	 * @return 返回字符串形式的关键字。
	 */
	public String getKeyword() {
		return this.keyword;
	}

	/**
	 * 获得数据最近一次更新的时间戳。
	 * 
	 * @return 返回绝对时间形式的时间戳。
	 */
	public long getTimestamp() {
		return this.timestamp;
	}

	/**
	 * 更新指定终端的正回馈信息。
	 * 
	 * @param endpoint 指定需要更新计数的终端。
	 * @return 返回更新后的正回馈计数。
	 */
	public int updatePositive(Endpoint endpoint) {
		this.timestamp = Clock.currentTimeMillis();

		AtomicInteger value = this.positiveMap.get(endpoint);
		if (null != value) {
			value.incrementAndGet();
		}
		else {
			value = new AtomicInteger(1);
			this.positiveMap.put(endpoint, value);
		}
		return value.get();
	}

	/**
	 * 更新指定终端的负回馈信息。
	 * 
	 * @param endpoint 指定需要更新计数的终端。
	 * @return 返回更新后的负回馈计数。
	 */
	public int updateNegative(Endpoint endpoint) {
		this.timestamp = Clock.currentTimeMillis();

		AtomicInteger value = this.negativeMap.get(endpoint);
		if (null != value) {
			value.incrementAndGet();
		}
		else {
			value = new AtomicInteger(1);
			this.negativeMap.put(endpoint, value);
		}
		return value.get();
	}

	/**
	 * 移除指定终端的正回馈计数。
	 * 
	 * @param endpoint 指定需移除的终端。
	 */
	public void removePositive(Endpoint endpoint) {
		this.positiveMap.remove(endpoint);
	}

	/**
	 * 移除指定终端的负回馈计数。
	 * 
	 * @param endpoint 指定需移除的终端。
	 */
	public void removeNegative(Endpoint endpoint) {
		this.negativeMap.remove(endpoint);
	}

	/**
	 * 获得正回馈计数。
	 * 
	 * @param endpoint 指定终端。
	 * @return 返回指定终端的正回馈计数。
	 */
	public int countPositive(Endpoint endpoint) {
		AtomicInteger value = this.positiveMap.get(endpoint);
		if (null == value) {
			return 0;
		}

		return value.get();
	}

	/**
	 * 获得反回馈计数。
	 * 
	 * @param endpoint 指定终端。
	 * @return 返回指定终端的负回馈计数。
	 */
	public int countNegative(Endpoint endpoint) {
		AtomicInteger value = this.negativeMap.get(endpoint);
		if (null == value) {
			return 0;
		}

		return value.get();
	}

}
