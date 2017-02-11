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

package net.cellcloud.adapter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.cellcloud.core.Endpoint;

public class Feedback {

	private String keyword;

	private ConcurrentHashMap<Endpoint, AtomicInteger> positiveMap;

	private ConcurrentHashMap<Endpoint, AtomicInteger> negativeMap;

	public Feedback(String keyword) {
		this.keyword = keyword;
	}

	public String getKeyword() {
		return this.keyword;
	}

	public int updatePositive(Endpoint endpoint) {
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

	public int updateNegative(Endpoint endpoint) {
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

	public int countPositive(Endpoint endpoint) {
		AtomicInteger value = this.positiveMap.get(endpoint);
		if (null == value) {
			return 0;
		}

		return value.get();
	}

	public int countNegative(Endpoint endpoint) {
		AtomicInteger value = this.negativeMap.get(endpoint);
		if (null == value) {
			return 0;
		}

		return value.get();
	}

}
