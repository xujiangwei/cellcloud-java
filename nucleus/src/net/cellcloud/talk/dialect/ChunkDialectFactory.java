/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2015 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.talk.dialect;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/** 块数据传输方言工厂。
 * 
 * @author Jinagwei Xu
 *
 */
public class ChunkDialectFactory extends DialectFactory {

	private DialectMetaData metaData;

	private ConcurrentHashMap<String, Vector<ChunkDialect>> cache;
	private ConcurrentHashMap<String, Long> timestampMap;

	public ChunkDialectFactory() {
		this.metaData = new DialectMetaData(ChunkDialect.DIALECT_NAME, "Chunk Dialect");
		this.cache = new ConcurrentHashMap<String, Vector<ChunkDialect>>();
		this.timestampMap = new ConcurrentHashMap<String, Long>();
	}

	@Override
	public DialectMetaData getMetaData() {
		return this.metaData;
	}

	@Override
	public Dialect create(String tracker) {
		return new ChunkDialect(tracker);
	}

	@Override
	public void shutdown() {
		this.cache.clear();
		this.timestampMap.clear();
	}

	protected void write(ChunkDialect chunk) {
		String sign = chunk.sign;
		if (this.cache.containsKey(sign)) {
			Vector<ChunkDialect> list = this.cache.get(sign);
			list.add(chunk);
		}
		else {
			Vector<ChunkDialect> list = new Vector<ChunkDialect>();
			list.add(chunk);
			this.cache.put(sign, list);
			this.timestampMap.put(sign, System.currentTimeMillis());
		}
	}

	protected int read(String sign, int index, byte[] out) {
		if (index < 0) {
			return -1;
		}

		Vector<ChunkDialect> list = this.cache.get(sign);
		if (null != list && index < list.size()) {
			ChunkDialect cd = list.get(index);
			byte[] buf = cd.data;
			int len = cd.length;
			System.arraycopy(buf, 0, out, 0, len);
			return len;
		}

		return -1;
	}

	protected boolean checkCompleted(String sign) {
		Vector<ChunkDialect> list = this.cache.get(sign);
		if (null != list) {
			ChunkDialect cd = list.get(0);
			if (cd.chunkNum == list.size()) {
				return true;
			}
		}

		return false;
	}
}
