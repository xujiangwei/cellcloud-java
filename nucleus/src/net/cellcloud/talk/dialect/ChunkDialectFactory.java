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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import net.cellcloud.common.Logger;
import net.cellcloud.core.Cellet;
import net.cellcloud.talk.TalkService;
import net.cellcloud.util.Clock;

/** 块数据传输方言工厂。
 * 
 * @author Jiangwei Xu
 *
 */
public class ChunkDialectFactory extends DialectFactory {

	private DialectMetaData metaData;

	private ExecutorService executor;

	private ConcurrentHashMap<String, Cache> cacheMap;

	// 用于服务器模式下的队列
	private ConcurrentHashMap<String, ChunkList> sListMap;

	// 用于客户端模式下的队列
	private ConcurrentHashMap<String, ChunkList> cListMap;

	private AtomicLong cacheMemorySize = new AtomicLong(0);
	private final long clearThreshold = 500 * 1024 * 1024;
	private AtomicBoolean clearRunning = new AtomicBoolean(false);
	private Object mutex = new Object();

	// 数据在缓存里的超时时间，仅客户端使用
	private long clientTimeout = 10 * 60 * 1000;

	private int logCounts = 0;

	public ChunkDialectFactory(ExecutorService executor) {
		this.metaData = new DialectMetaData(ChunkDialect.DIALECT_NAME, "Chunk Dialect");
		this.executor = executor;
		this.cacheMap = new ConcurrentHashMap<String, Cache>();
		this.sListMap = new ConcurrentHashMap<String, ChunkList>();
		this.cListMap = new ConcurrentHashMap<String, ChunkList>();
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
		this.sListMap.clear();
		this.cListMap.clear();
		this.cacheMemorySize.set(0);
	}

	public long getCacheMemorySize() {
		return this.cacheMemorySize.get();
	}

	public long getMaxCacheMemorySize() {
		return this.clearThreshold;
	}

	public int getCacheNum() {
		return this.cacheMap.size();
	}

	public int getSListSize() {
		return this.sListMap.size();
	}

	public int getCListSize() {
		return this.cListMap.size();
	}

	@Override
	protected boolean onTalk(String identifier, Dialect dialect) {
		ChunkDialect chunk = (ChunkDialect) dialect;

		if (!chunk.ack && !chunk.infectant) {
			ChunkList list = this.cListMap.get(chunk.getSign());
			if (null != list) {
				if (chunk.getChunkIndex() == 0) {
					list.clear();
				}

				// 写入列表
				list.append(chunk);
			}
			else {
				list = new ChunkList(identifier.toString(), chunk.getChunkNum());
				list.append(chunk);
				this.cListMap.put(chunk.getSign().toString(), list);
			}
		}

		if (chunk.getChunkIndex() == 0 || chunk.infectant || chunk.ack) {
			// 直接发送

			// 回调已处理
			chunk.fireProgress(identifier);

			return true;
		}
		else {
			// 劫持，由队列发送
			return false;
		}
	}

	@Override
	protected boolean onDialogue(final String identifier, Dialect dialect) {
		ChunkDialect chunk = (ChunkDialect) dialect;

		if (chunk.ack) {
			// 收到 ACK ，发送下一个
			String sign = chunk.getSign();
			ChunkList list = this.cListMap.get(sign);
			if (null != list) {
				// 更新应答索引
				list.ackIndex = chunk.getChunkIndex();
				// 发送下一条数据
				ChunkDialect response = list.get(list.ackIndex + 1);
				if (null != response) {
					TalkService.getInstance().talk(list.target, response);
				}

				// 检查
				if (list.ackIndex == chunk.getChunkNum() - 1) {
					this.checkAndClearList(this.cListMap, this.clientTimeout);
				}
			}
			else {
				Logger.w(this.getClass(), "Can NOT find chunk: " + sign);
			}

			// 应答包，劫持
			return false;
		}
		else {
			// 回送确认
			String sign = chunk.getSign();

			final ChunkDialect ack = new ChunkDialect(chunk.getTracker().toString());
			ack.setAck(sign, chunk.getChunkIndex(), chunk.getChunkNum());

			this.executor.execute(new Runnable() {
				@Override
				public void run() {
					// 回送 ACK
					TalkService.getInstance().talk(identifier, ack);
				}
			});

			// 不劫持
			return true;
		}
	}

	@Override
	protected boolean onTalk(Cellet cellet, String targetTag, Dialect dialect) {
		ChunkDialect chunk = (ChunkDialect) dialect;

		if (!chunk.ack && !chunk.infectant) {
			ChunkList list = this.sListMap.get(chunk.getSign());
			if (null != list) {
				if (chunk.getChunkIndex() == 0) {
					list.clear();
				}

				// 写入列表
				list.append(chunk);
			}
			else {
				list = new ChunkList(targetTag.toString(), chunk.getChunkNum());
				list.append(chunk);
				this.sListMap.put(chunk.getSign(), list);
			}
		}

		if (chunk.getChunkIndex() == 0 || chunk.infectant || chunk.ack) {
			// 直接发送

			// 回调已处理
			chunk.fireProgress(targetTag);

			return true;
		}
		else {
			// 劫持，由队列发送
			return false;
		}
	}

	@Override
	protected boolean onDialogue(final Cellet cellet, final String sourceTag, Dialect dialect) {
		ChunkDialect chunk = (ChunkDialect) dialect;

		if (!chunk.ack) {
			// 回送确认
			String sign = chunk.getSign();

			final ChunkDialect ack = new ChunkDialect(chunk.getTracker().toString());
			ack.setAck(sign, chunk.getChunkIndex(), chunk.getChunkNum());

			this.executor.execute(new Runnable() {
				@Override
				public void run() {
					// 回送 ACK
					cellet.talk(sourceTag, ack);
				}
			});

			// 不劫持
			return true;
		}
		else {
			// 收到 ACK ，发送下一个
			String sign = chunk.getSign();
			ChunkList list = this.sListMap.get(sign);
			if (null != list) {
				// 更新应答索引
				list.ackIndex = chunk.getChunkIndex();
				// 发送下一条数据
				ChunkDialect response = list.get(list.ackIndex + 1);
				if (null != response) {
					cellet.talk(list.target, response);
				}

				// 检查
				if (list.ackIndex == chunk.getChunkNum() - 1) {
					this.checkAndClearList(this.sListMap, 0);
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
		this.cacheMemorySize.set(this.cacheMemorySize.get() + chunk.length);

		if ((this.logCounts % 100) == 0) {
			long mem = this.cacheMemorySize.get();
			if (mem > 1024 && mem <= 1048576) {
				Logger.i(ChunkDialectFactory.class, "Cache memory size: " + (long)(mem / 1024) + " KB");
			}
			else if (mem > 1048576) {
				Logger.i(ChunkDialectFactory.class, "Cache memory size: " + (long)(mem / 1048576) + " MB");
			}
			else {
				Logger.i(ChunkDialectFactory.class, "Cache memory size: " + mem + " Bytes");
			}
		}
		++this.logCounts;
		if (this.logCounts > 9999) {
			this.logCounts = 0;
		}

		if (this.cacheMemorySize.get() > this.clearThreshold) {
			synchronized (this.mutex) {
				if (!this.clearRunning.get()) {
					this.clearRunning.set(true);
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
		if (null == tag || null == sign) {
			return false;
		}

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

			// 差值
			long ds = size - cache.dataSize;

			// 更新内存大小
			this.cacheMemorySize.set(this.cacheMemorySize.get() - ds);

			// 移除空缓存
			if (cache.isEmpty()) {
				this.cacheMap.remove(tag);
			}
		}
	}

	private synchronized void checkAndClearList(ConcurrentHashMap<String, ChunkList> listMap, long timeout) {
		long time = Clock.currentTimeMillis();
		LinkedList<String> deleteList = new LinkedList<String>();

		for (Map.Entry<String, ChunkList> entry : listMap.entrySet()) {
			ChunkList list = entry.getValue();
			if (list.ackIndex >= 0 && list.chunkNum - 1 == list.ackIndex) {
				// 删除
				deleteList.add(entry.getKey());
			}
			else if (timeout > 0 && time - list.timestamp > timeout) {
				// 超时
				deleteList.add(entry.getKey());
			}
		}

		if (!deleteList.isEmpty()) {
			for (String sign : deleteList) {
				listMap.remove(sign);

				Logger.i(this.getClass(), "Clear chunk list - sign: " + sign);
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

		public void offer(ChunkDialect dialect) {
			LinkedList<ChunkDialect> list = this.data.get(dialect.sign);
			if (null != list) {
				boolean rebuildTime = false;
				synchronized (list) {
					// 如果发现当前清单数量和 chunkNum 一致，说明 sign 重复，清理之前的
					if (list.size() == dialect.chunkNum) {
						list.clear();

						// 重建时间戳
						rebuildTime = true;
					}

					list.add(dialect);
					// 更新数据大小
					this.dataSize += dialect.length;
				}

				if (rebuildTime) {
					synchronized (this.signQueue) {
						int index = this.signQueue.indexOf(dialect.sign);
						if (index >= 0) {
							this.signQueue.remove(index);
							this.signTimeQueue.remove(index);
						}

						this.signQueue.add(dialect.sign);
						this.signTimeQueue.add(System.currentTimeMillis());
					}
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
				synchronized (list) {
					ChunkDialect cd = list.get(0);
					if (cd.chunkNum == list.size()) {
						return true;
					}
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

	private class ChunkList {
		private long timestamp;
		private String target;
		private LinkedList<ChunkDialect> list;

		protected int ackIndex = -1;

		protected int chunkNum = 0;

		private ChunkList(String target, int chunkNum) {
			this.timestamp = System.currentTimeMillis();
			this.target = target;
			this.chunkNum = chunkNum;
			this.list = new LinkedList<ChunkDialect>();
		}

		protected void append(ChunkDialect chunk) {
			// 标识为已污染
			chunk.infectant = true;

			synchronized (this) {
				this.list.add(chunk);
			}
		}

		protected ChunkDialect get(int index) {
			synchronized (this) {
				return this.list.get(index);
			}
		}

		protected void clear() {
			synchronized (this) {
				this.list.clear();
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

				// 找到最旧的 cache
				long ft = cache.getFirstTime();
				if (ft < time) {
					time = ft;
					selected = cache;
				}
			}

			if (null != selected) {
				long size = selected.clearFirst();

				// 更新内存大小记录
				cacheMemorySize.set(cacheMemorySize.get() - size);
			}

			if (!emptyList.isEmpty()) {
				for (Cache cache : emptyList) {
					cacheMap.remove(cache.tag);
				}
			}

			clearRunning.set(false);
		}
	}
}
