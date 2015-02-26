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

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import net.cellcloud.common.Logger;

/** 块数据传输方言工厂。
 * 
 * @author Jiangwei Xu
 *
 */
public class ChunkDialectFactory extends DialectFactory {

	private DialectMetaData metaData;

	private ConcurrentHashMap<String, Cache> cacheMap;

	private long cacheMemorySize = 0;
	private final long clearThreshold = 100 * 1024 * 1024;
	private Object mutex = new Object();
	private boolean clearRunning = false;

	public ChunkDialectFactory() {
		this.metaData = new DialectMetaData(ChunkDialect.DIALECT_NAME, "Chunk Dialect");
		this.cacheMap = new ConcurrentHashMap<String, Cache>();
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
		this.cacheMap.clear();
		this.cacheMemorySize = 0;
	}

	protected void write(ChunkDialect chunk) {
		String tag = chunk.getOwnerTag();
		if (this.cacheMap.containsKey(tag)) {
			Cache cache = this.cacheMap.get(tag);
			cache.push(chunk);
		}
		else {
			Cache cache = new Cache(tag);
			cache.push(chunk);
			this.cacheMap.put(tag, cache);
		}

		// 更新内存大小
		this.cacheMemorySize += chunk.length;

		Logger.i(ChunkDialectFactory.class, "Cache memory size: " + (long)(cacheMemorySize / 1024) + " KB");

		if (this.cacheMemorySize > this.clearThreshold) {
			synchronized (this.mutex) {
				if (!this.clearRunning) {
					this.clearRunning = true;
					(new Thread(new ClearTask())).start();
				}
			}
		}
	}

	protected int read(String tag, String sign, int index, byte[] out) {
		if (index < 0) {
			return -1;
		}

		Cache cache = this.cacheMap.get(tag);
		if (null != cache) {
			ChunkDialect cd = cache.get(sign, index);
			byte[] buf = cd.data;
			int len = cd.length;
			System.arraycopy(buf, 0, out, 0, len);
			return len;
		}

		return -1;
	}

	protected boolean checkCompleted(String tag, String sign) {
		Cache cache = this.cacheMap.get(tag);
		if (null != cache) {
			return cache.checkCompleted(sign);
		}

		return false;
	}

	protected void clear(String tag, String sign) {
		Cache cache = this.cacheMap.get(tag);
		if (null != cache) {
			// 计算缓存大小变化差值
			long size = cache.dataSize;
			// 进行缓存清理
			cache.clear(sign);
			long ds = size - cache.dataSize;
			this.cacheMemorySize -= ds;

			// 移除空缓存
			if (cache.isEmpty()) {
				this.cacheMap.remove(tag);
			}
		}
	}

	/**
	 * 内部缓存。
	 */
	private class Cache {
		protected String tag;
		private ConcurrentHashMap<String, LinkedList<ChunkDialect>> data;
		private LinkedList<String> signQueue;
		private LinkedList<Long> signTimeQueue;
		protected long dataSize;

		private Cache(String tag) {
			this.tag = tag;
			this.data = new ConcurrentHashMap<String, LinkedList<ChunkDialect>>();
			this.signQueue = new LinkedList<String>();
			this.signTimeQueue = new LinkedList<Long>();
			this.dataSize = 0;
		}

		public void push(ChunkDialect dialect) {
			LinkedList<ChunkDialect> list = this.data.get(dialect.sign);
			if (null != list) {
				synchronized (list) {
					list.add(dialect);
					// 更新数据大小
					this.dataSize += dialect.length;
				}
			}
			else {
				list = new LinkedList<ChunkDialect>();
				list.add(dialect);
				// 更新数据大小
				this.dataSize += dialect.length;

				this.data.put(dialect.sign, list);

				synchronized (this.signQueue) {
					this.signQueue.add(dialect.sign);
					this.signTimeQueue.add(System.currentTimeMillis());
				}
			}
		}

		public ChunkDialect get(String sign, int index) {
			LinkedList<ChunkDialect> list = this.data.get(sign);
			synchronized (list) {
				return list.get(index);
			}
		}

		public boolean checkCompleted(String sign) {
			LinkedList<ChunkDialect> list = this.data.get(sign);
			if (null != list) {
				ChunkDialect cd = list.get(0);
				if (cd.chunkNum == list.size()) {
					return true;
				}
			}

			return false;
		}

		public long clear(String sign) {
			long size = 0;
			LinkedList<ChunkDialect> list = this.data.remove(sign);
			if (null != list) {
				synchronized (list) {
					for (ChunkDialect chunk : list) {
						this.dataSize -= chunk.length;
						size += chunk.length;
					}
				}
			}

			synchronized (this.signQueue) {
				int index = this.signQueue.indexOf(sign);
				if (index >= 0) {
					this.signQueue.remove(index);
					this.signTimeQueue.remove(index);
				}
			}

			return size;
		}

		public boolean isEmpty() {
			return this.data.isEmpty();
		}

		public long getFirstTime() {
			synchronized (this.signQueue) {
				return this.signTimeQueue.getFirst().longValue();
			}
		}

		public long clearFirst() {
			String sign = null;
			synchronized (this.signQueue) {
				sign = this.signQueue.getFirst();
			}
			return this.clear(sign);
		}
	}

	/**
	 *
	 */
	private class ClearTask implements Runnable {
		private ClearTask() {
		}

		@Override
		public void run() {
			long time = Long.MAX_VALUE;
			Cache selected = null;
			LinkedList<Cache> emptyList = new LinkedList<Cache>();

			for (Cache cache : cacheMap.values()) {
				if (cache.isEmpty()) {
					emptyList.add(cache);
					continue;
				}

				long ft = cache.getFirstTime();
				if (ft < time) {
					time = ft;
					selected = cache;
				}
			}

			if (null != selected) {
				long size = selected.clearFirst();
				cacheMemorySize -= size;

				Logger.i(ChunkDialectFactory.class, "Cache memory size: " + (long)(cacheMemorySize / 1024) + " KB");
			}

			if (!emptyList.isEmpty()) {
				for (Cache cache : emptyList) {
					cacheMap.remove(cache.tag);
				}
			}

			clearRunning = false;
		}
	}
}
