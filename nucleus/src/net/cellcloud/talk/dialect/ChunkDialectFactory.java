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
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import net.cellcloud.common.Logger;
import net.cellcloud.core.Cellet;

/** 块数据传输方言工厂。
 * 
 * @author Jiangwei Xu
 *
 */
public class ChunkDialectFactory extends DialectFactory {

	private DialectMetaData metaData;

	private ConcurrentHashMap<String, Cache> cacheMap;

	private ConcurrentHashMap<String, Queue> queueMap;

	private long cacheMemorySize = 0;
	private final long clearThreshold = 100 * 1024 * 1024;
	private Object mutex = new Object();
	private boolean clearRunning = false;

	public ChunkDialectFactory() {
		this.metaData = new DialectMetaData(ChunkDialect.DIALECT_NAME, "Chunk Dialect");
		this.cacheMap = new ConcurrentHashMap<String, Cache>();
		this.queueMap = new ConcurrentHashMap<String, Queue>();
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

	@Override
	protected synchronized boolean onTalk(String identifier, Dialect dialect) {
		return true;
	}

	@Override
	protected synchronized boolean onDialogue(String identifier, Dialect dialect) {
		return true;
	}

	@Override
	protected synchronized boolean onTalk(Cellet cellet, String targetTag, Dialect dialect) {
		ChunkDialect chunk = (ChunkDialect) dialect;
		if (chunk.getChunkIndex() == 0 || chunk.infectant || chunk.ack) {
			// 直接发送

			// 回调已处理
			chunk.fireProgress(targetTag);

			return true;
		}
		else {
			Queue queue = this.queueMap.get(chunk.getSign());
			if (null != queue) {
				// 写入队列
				queue.enqueue(chunk);
				// 劫持，由队列发送
				return false;
			}
			else {
				queue = new Queue(cellet.getFeature().getIdentifier(), chunk.getChunkNum());
				queue.enqueue(chunk);
				this.queueMap.put(chunk.getSign(), queue);
				// 劫持，由队列发送
				return false;
			}
		}
	}

	@Override
	protected synchronized boolean onDialogue(Cellet cellet, String sourceTag, Dialect dialect) {
		ChunkDialect chunk = (ChunkDialect) dialect;
		if (!chunk.ack) {
			// 回送确认
			String sign = chunk.getSign();

			ChunkDialect ack = new ChunkDialect();
			ack.setAck(sign, chunk.getChunkIndex(), chunk.getChunkNum());

			// 回送 ACK
			cellet.talk(dialect.getOwnerTag(), ack);

			// 不劫持
			return true;
		}
		else {
			// 收到 ACK ，发送下一个
			String sign = chunk.getSign();
			Queue queue = this.queueMap.get(sign);
			if (null != queue) {
				// 更新应答索引
				queue.ackIndex = chunk.getChunkIndex();
				// 发送下一条数据
				ChunkDialect response = queue.dequeue();
				if (null != response) {
					cellet.talk(queue.target, response);
				}

				// 检查
				if (queue.ackIndex == chunk.getChunkNum() - 1) {
					this.checkAndClearQueue();
				}
			}

			// 应答包，劫持
			return false;
		}
	}

	protected void write(ChunkDialect chunk) {
		String tag = chunk.getOwnerTag();
		if (this.cacheMap.containsKey(tag)) {
			Cache cache = this.cacheMap.get(tag);
			cache.offer(chunk);
		}
		else {
			Cache cache = new Cache(tag);
			cache.offer(chunk);
			this.cacheMap.put(tag, cache);
		}

		// 更新内存大小
		this.cacheMemorySize += chunk.length;

		if (this.cacheMemorySize > 1024) {
			Logger.i(ChunkDialectFactory.class, "Cache memory size: " + (long)(this.cacheMemorySize / 1024) + " KB");
		}
		else {
			Logger.i(ChunkDialectFactory.class, "Cache memory size: " + this.cacheMemorySize + " Bytes");
		}

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

	private void checkAndClearQueue() {
		LinkedList<String> deleteList = new LinkedList<String>();

		for (Map.Entry<String, Queue> entry : this.queueMap.entrySet()) {
			Queue queue = entry.getValue();
			if (queue.ackIndex >= 0 && queue.chunkNum - 1 == queue.ackIndex) {
				// 删除
				deleteList.add(entry.getKey());
			}
		}

		if (!deleteList.isEmpty()) {
			for (String sign : deleteList) {
				this.queueMap.remove(sign);

				if (Logger.isDebugLevel()) {
					Logger.i(this.getClass(), "Clear chunk factory queue: " + sign);
				}
			}

			deleteList.clear();
		}

		deleteList = null;
	}

	/**
	 * 内部缓存。
	 */
	private class Cache {
		protected String tag;
		private ConcurrentHashMap<String, Vector<ChunkDialect>> data;
		private Vector<String> signQueue;
		private Vector<Long> signTimeQueue;
		protected long dataSize;

		private Cache(String tag) {
			this.tag = tag;
			this.data = new ConcurrentHashMap<String, Vector<ChunkDialect>>();
			this.signQueue = new Vector<String>();
			this.signTimeQueue = new Vector<Long>();
			this.dataSize = 0;
		}

		public void offer(ChunkDialect dialect) {
			Vector<ChunkDialect> list = this.data.get(dialect.sign);
			if (null != list) {
				synchronized (list) {
					list.add(dialect);
					// 更新数据大小
					this.dataSize += dialect.length;
				}
			}
			else {
				list = new Vector<ChunkDialect>();
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
			Vector<ChunkDialect> list = this.data.get(sign);
			synchronized (list) {
				return list.get(index);
			}
		}

		public boolean checkCompleted(String sign) {
			Vector<ChunkDialect> list = this.data.get(sign);
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
			Vector<ChunkDialect> list = this.data.remove(sign);
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
				return this.signTimeQueue.get(0).longValue();
			}
		}

		public long clearFirst() {
			String sign = null;
			synchronized (this.signQueue) {
				sign = this.signQueue.get(0);
			}
			return this.clear(sign);
		}
	}

	private class Queue {
		private String target;
//		private long timestamp;
		private Vector<ChunkDialect> queue;

		protected int ackIndex = -1;

		protected int chunkNum = 0;

		private Queue(String target, int chunkNum) {
			this.target = target;
			this.chunkNum = chunkNum;
//			this.timestamp = System.currentTimeMillis();
			this.queue = new Vector<ChunkDialect>();
		}

		protected void enqueue(ChunkDialect chunk) {
			// 标识为已污染
			chunk.infectant = true;

			synchronized (this) {
				this.queue.add(chunk);
			}
		}

		protected ChunkDialect dequeue() {
			synchronized (this) {
				if (this.queue.isEmpty()) {
					return null;
				}

				ChunkDialect first = this.queue.get(0);
				if (null != first) {
					this.queue.remove(0);
				}
				return first;
			}
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
