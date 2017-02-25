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

package net.cellcloud.talk.dialect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.cellcloud.common.Logger;
import net.cellcloud.core.Cellet;
import net.cellcloud.talk.TalkService;
import net.cellcloud.util.Clock;

/**
 * 块数据传输方言工厂。
 * 
 * @author Ambrose Xu
 * 
 */
public class ChunkDialectFactory extends DialectFactory {

	/**
	 * 方言元数据。
	 */
	private DialectMetaData metaData;

	/**
	 * 线程池执行器。
	 */
	private ExecutorService executor;

	/** 配额定时器。 */
	private Timer quotaTimer;
	/** 每个区块列表列表的默认配额。 */
	private long defaultQuotaPerList = 48L * 1024L;

	/** 数据接收缓存。键是区块记号 */
	private ConcurrentHashMap<String, Cache> cacheMap;

	/** 用于服务器模式下的队列。 */
	private ConcurrentHashMap<String, ChunkList> sListMap;

	/** 用于客户端模式下的队列。 */
	private ConcurrentHashMap<String, ChunkList> cListMap;

	/** 数据在发送缓存里的超时时间。 */
	private long listTimeout = 10L * 60L * 1000L;

	/** 当前缓存占用的内存大小。 */
	private AtomicLong cacheMemorySize = new AtomicLong(0);
	/** 内存清理门限，当内存缓存大小大于此值时，将执行清理算法对内存进行清理。 */
	private final long clearThreshold = 100L * 1024L * 1024L;
	/** 是否正在执行清理任务。 */
	private AtomicBoolean clearRunning = new AtomicBoolean(false);
	/** 线程互斥体。 */
	private Object mutex = new Object();

	/** 用于辅助打印日志的计数器。 */
	private int logCounts = 0;

	/**
	 * 构造函数。
	 * 
	 * @param executor 指定线程池执行器。
	 */
	public ChunkDialectFactory(ExecutorService executor) {
		this.metaData = new DialectMetaData(ChunkDialect.DIALECT_NAME, "Chunk Dialect");
		this.executor = executor;
		this.cacheMap = new ConcurrentHashMap<String, Cache>();
		this.quotaTimer = new Timer("ChunkQuotaTimer");
		// 每 500ms 一次任务
		this.quotaTimer.schedule(new QuotaTask(), 3000L, 500L);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DialectMetaData getMetaData() {
		return this.metaData;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Dialect create(String tracker) {
		return new ChunkDialect(tracker);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdown() {
		this.cacheMap.clear();

		if (null != this.quotaTimer) {
			this.quotaTimer.cancel();
			this.quotaTimer = null;
		}

		if (null != this.sListMap) {
			this.sListMap.clear();
		}

		if (null != this.cListMap) {
			this.cListMap.clear();
		}

		this.cacheMemorySize.set(0);
	}

	/**
	 * 获得当前内存缓存的大小。
	 * 
	 * @return 返回缓存占用的内存大小。
	 */
	public long getCacheMemorySize() {
		return this.cacheMemorySize.get();
	}

	/**
	 * 获得允许的最大缓存大小。
	 * 
	 * @return 返回缓存最大值。
	 */
	public long getMaxCacheMemorySize() {
		return this.clearThreshold;
	}

	/**
	 * 获得接收区的缓存数量。
	 * 
	 * @return 返回接收用的缓存数量。
	 */
	public int getCacheNum() {
		return this.cacheMap.size();
	}

	/**
	 * 获得服务器模式下的发送区块列表大小。
	 * 
	 * @return 返回服务器模式下的发送区块列表大小。
	 */
	public int getSListSize() {
		if (null == this.sListMap) {
			return 0;
		}

		return this.sListMap.size();
	}

	/**
	 * 获得客户机模式下的发送区块列表大小。
	 * 
	 * @return 返回客户机模式下的发送区块列表大小。
	 */
	public int getCListSize() {
		if (null == this.cListMap) {
			return 0;
		}

		return this.cListMap.size();
	}

	@Override
	protected boolean onTalk(String identifier, Dialect dialect) {
		ChunkDialect chunk = (ChunkDialect) dialect;

		if (chunk.infectant) {
			// 回调已处理
			chunk.fireProgress(identifier);

			// 直接发送
			return true;
		}

		synchronized (this) {
			if (null == this.cListMap) {
				this.cListMap = new ConcurrentHashMap<String, ChunkList>();
			}

			this.updateListMap(this.cListMap, chunk.getSign(), identifier, chunk, null);
		}

		return false;
	}

	@Override
	protected boolean onDialogue(final String identifier, Dialect dialect) {
		ChunkDialect chunk = (ChunkDialect) dialect;

		this.write(chunk);

		return true;
	}

	@Override
	protected boolean onTalk(Cellet cellet, String targetTag, Dialect dialect) {
		ChunkDialect chunk = (ChunkDialect) dialect;

		if (chunk.infectant) {
			// 回调已处理
			chunk.fireProgress(targetTag);

			// 直接发送
			return true;
		}

		synchronized (this) {
			if (null == this.sListMap) {
				this.sListMap = new ConcurrentHashMap<String, ChunkList>();
			}

			this.updateListMap(this.sListMap, targetTag + chunk.getSign(), targetTag, chunk, cellet);
		}

		return false;
	}

	@Override
	protected boolean onDialogue(final Cellet cellet, final String sourceTag, Dialect dialect) {
		ChunkDialect chunk = (ChunkDialect) dialect;

		this.write(chunk);

		return true;
	}

	/**
	 * 将指定的区块方言写入缓存。
	 * 
	 * @param chunk 指定的区块方言。
	 * @return 返回是否写入成功。
	 */
	private synchronized boolean write(ChunkDialect chunk) {
		if (chunk.getChunkIndex() == 0) {
			this.clear(chunk.getSign());
		}

		if (this.cacheMap.containsKey(chunk.getSign())) {
			Cache cache = this.cacheMap.get(chunk.getSign());
			cache.offer(chunk);
		}
		else {
			Cache cache = new Cache(chunk.getSign(), chunk.chunkNum);
			cache.offer(chunk);
			this.cacheMap.put(chunk.getSign(), cache);
		}

		// 更新内存大小
		this.cacheMemorySize.addAndGet(chunk.getLength());

		if ((this.logCounts % 100) == 0) {
			long mem = this.cacheMemorySize.get();
			if (mem > 1024L && mem <= 1048576L) {
				Logger.i(ChunkDialectFactory.class, "Cache memory size: " + (long)(mem / 1024L) + " KB");
			}
			else if (mem > 1048576L) {
				Logger.i(ChunkDialectFactory.class, "Cache memory size: " + (long)(mem / 1048576L) + " MB");
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
					(new Thread(new ClearCacheTask())).start();
				}
			}
		}

		if (chunk.isLast()) {
			// 最后一个
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * 读取接收缓存区内的区块数据。
	 * 
	 * @param sign 指定区块记号。
	 * @param index 指定读取的索引。
	 * @param out 指定输出数组。
	 * @return 返回读取数据的长度，如果读取失败返回值为 <code>-1</code> 。
	 */
	protected int read(String sign, int index, byte[] out) {
		if (index < 0) {
			return -1;
		}

		Cache cache = this.cacheMap.get(sign);
		if (null != cache) {
			ChunkDialect cd = cache.get(index);
			byte[] buf = cd.data;
			int len = cd.length;

			if (null == buf) {
				return -1;
			}

			System.arraycopy(buf, 0, out, 0, len);
			return len;
		}

		return -1;
	}

	/**
	 * 判断指定记号的整块是否接收完成。
	 * 
	 * @param sign 指定区块的记号。
	 * @return 如果已经完成返回 <code>true</code> 。
	 */
	protected boolean hasCompleted(String sign) {
		if (null == sign) {
			return false;
		}

		Cache cache = this.cacheMap.get(sign);
		if (null != cache) {
			return cache.hasCompleted();
		}

		return false;
	}

	/**
	 * 从接收缓存区中删除掉指定的整个区块。
	 * 
	 * @param sign 指定区块的记号。
	 */
	protected void clear(String sign) {
		Cache cache = this.cacheMap.remove(sign);
		if (null != cache) {
			// 计算缓存大小变化差值，进行缓存清理
			long size = cache.clear();

			// 更新内存大小
			this.cacheMemorySize.set(this.cacheMemorySize.get() - size);
		}
	}

	/**
	 * 更新指定的发送列表。
	 * 将指定区块方言正确的写入到区块列表。
	 * 
	 * @param listMap 指定发送列表。
	 * @param mapKey 指定更新的映射键。
	 * @param target 指定识别串标识的目标。
	 * @param chunk 指定区块方言。
	 * @param cellet 指定操作的 Cellet 。
	 */
	private void updateListMap(ConcurrentHashMap<String, ChunkList> listMap, String mapKey, String target, ChunkDialect chunk, Cellet cellet) {
		ChunkList list = listMap.get(mapKey);
		if (null != list) {
			if (chunk.getChunkIndex() == 0) {
				list.reset(target.toString(), chunk.chunkNum);
			}

			// 写入列表
			list.append(chunk);
		}
		else {
			list = new ChunkList(target.toString(), chunk.getChunkNum(), this.defaultQuotaPerList, cellet);
			list.append(chunk);
			listMap.put(mapKey, list);
		}

		if (!list.running.get()) {
			list.running.set(true);
			this.executor.execute(list);
		}
	}

	/**
	 * 检查并尝试清理指定的接收列表。
	 * 
	 * @param listMap 指定需检测的列表。
	 * @param deleteList 用于优化删除的列表。
	 * @param timeout 指定强制删除的超时时间。
	 */
	private void checkAndClearList(ConcurrentHashMap<String, ChunkList> listMap, List<String> deleteList, long timeout) {
		long time = Clock.currentTimeMillis();

		Iterator<Map.Entry<String, ChunkList>> iter = listMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, ChunkList> entry = iter.next();
			ChunkList list = entry.getValue();
			if (time - list.timestamp > timeout) {
				// 超时
				deleteList.add(entry.getKey());
			}
		}

		if (!deleteList.isEmpty()) {
			for (String key : deleteList) {
				listMap.remove(key);

				Logger.i(ChunkDialectFactory.class, "Clear chunk list - key: " + key);
			}

			deleteList.clear();
		}
	}

	/**
	 * 用于接收数据的缓存。每个 Cache 存储一个整块。
	 */
	private class Cache {
		/** 块记号。 */
		private String sign;
		/** 区块方言列表。 */
		private ArrayList<ChunkDialect> dataQueue;
		/** 更新时间戳。 */
		private long timestamp;
		/** 总数据大小。 */
		private long dataSize;

		/**
		 * 构造函数。
		 * 
		 * @param sign 指定记号。
		 * @param capacity 指定总块数量。
		 */
		private Cache(String sign, int capacity) {
			this.sign = sign;
			this.dataQueue = new ArrayList<ChunkDialect>(capacity);
			for (int i = 0; i < capacity; ++i) {
				this.dataQueue.add(new ChunkDialect());
			}
			this.dataSize = 0;
		}

		/**
		 * 追加数据到缓存。
		 * 
		 * @param dialect 指定待追加区块方言。
		 */
		public void offer(ChunkDialect dialect) {
			synchronized (this.dataQueue) {
				if (this.dataQueue.contains(dialect)) {
					int index = this.dataQueue.indexOf(dialect);
					// 删除旧长度
					ChunkDialect old = this.dataQueue.get(index);
					this.dataSize -= old.getLength();
					// 设置新值
					this.dataQueue.set(index, dialect);
					this.dataSize += dialect.getLength();
				}
				else {
					this.dataQueue.set(dialect.getChunkIndex(), dialect);
					this.dataSize += dialect.getLength();
				}
			}

			this.timestamp = Clock.currentTimeMillis();
		}

		/**
		 * 获得指定索引位置的区块方言。
		 * 
		 * @param index 指定索引。
		 * @return 返回对应的区块方言。
		 */
		public ChunkDialect get(int index) {
			synchronized (this.dataQueue) {
				if (index >= this.dataQueue.size()) {
					return null;
				}

				return this.dataQueue.get(index);
			}
		}

		/**
		 * 是否接收到所有的块。
		 * 
		 * @return 如果已经接收到所有的块返回 <code>true</code> 。
		 */
		public boolean hasCompleted() {
			synchronized (this.dataQueue) {
				if (this.dataQueue.isEmpty()) {
					return false;
				}

				for (ChunkDialect cd : this.dataQueue) {
					if (cd.getLength() <= 0) {
						return false;
					}
				}
			}

			return true;
		}

		/**
		 * 清理缓存。
		 * 
		 * @return 返回清理前的数据总长度。
		 */
		public long clear() {
			long size = this.dataSize;
			synchronized (this.dataQueue) {
				this.dataQueue.clear();
				this.dataSize = 0;
			}
			return size;
		}

		/**
		 * 返回时间戳。
		 * 
		 * @return 返回时间戳。
		 */
		public long getTimestamp() {
			return this.timestamp;
		}

	}

	/**
	 * 区块发送列表。
	 */
	private class ChunkList implements Runnable {
		/** Cellet */
		private Cellet cellet = null;
		/** 时间戳。 */
		private long timestamp;
		/** 源标签或 Cellet 标识。 */
		private String target;
		/** 总块数量。 */
		private int chunkNum = 0;
		/** 存储区块方言的列表。 */
		private ArrayList<ChunkDialect> list;
		/** 当前有效存储的区块索引。 */
		private AtomicInteger index;

		/** 该列表任务是否正在被执行。 */
		private AtomicBoolean running;

		/** 带宽配额。 */
		private long quota;
		/** 剩余配额。 */
		private AtomicLong remaining;

		/** 执行间隔，用于控制数据传输速率。 */
		private long interval = 100L;

		/**
		 * 构造函数。
		 */
		private ChunkList(String target, int chunkNum, long quota, Cellet cellet) {
			this.timestamp = Clock.currentTimeMillis();
			this.target = target;
			this.chunkNum = chunkNum;
			this.quota = quota;
			this.cellet = cellet;
			this.list = new ArrayList<ChunkDialect>(chunkNum);
			this.index = new AtomicInteger(-1);
			this.running = new AtomicBoolean(false);
			this.remaining = new AtomicLong(this.quota);
		}

		/**
		 * 追加数据到列表。
		 * 
		 * @param chunk 指定追加的区块方言。
		 */
		protected void append(ChunkDialect chunk) {
			// 标识为已污染
			chunk.infectant = true;

			synchronized (this) {
				if (!this.list.contains(chunk)) {
					this.list.add(chunk);
				}
			}

			if (chunk.getChunkIndex() == 0) {
				double t = (ChunkDialect.CHUNK_SIZE_KB + 0.0d) / (chunk.speedInKB + 0.0d) * 1000.0d;
				if (t >= 10.0d) {
					this.interval = (long) t;
				}
			}
		}

		/**
		 * 是否发送完成整个块。
		 * 
		 * @return 如果已经全部发送返回 <code>true</code> 。
		 */
		protected boolean isComplete() {
			return (this.index.get() + 1 == this.chunkNum);
		}

		/**
		 * 重置列表。
		 * 
		 * @param target 指定源标签或 Cellet 标识。
		 * @param chunkNum 指定总区块数量。
		 */
		protected void reset(String target, int chunkNum) {
			this.timestamp = Clock.currentTimeMillis();
			this.target = target;
			this.chunkNum = chunkNum;
			this.index.set(-1);
			this.remaining.set(this.quota);
			this.interval = 100L;

			synchronized (this) {
				this.list.clear();
			}
		}

		@Override
		public void run() {
			// 判断剩余配额
			long qr = this.remaining.get();
			if (qr > 0) {
				// 有配额
				ChunkDialect dialect = null;
				synchronized (this) {
					// 更新索引
					this.index.incrementAndGet();

					if (this.index.get() < this.list.size()) {
						dialect = this.list.get(this.index.get());
					}
				}

				if (null != dialect) {
					// 更新剩余配额
					qr = qr - dialect.getLength();
					this.remaining.set(qr);

					// 发送
					boolean ret = false;
					if (null == this.cellet) {
						ret = TalkService.getInstance().talk(this.target, dialect);
					}
					else {
						ret = this.cellet.talk(this.target, dialect);
					}

					if (!ret) {
						// 错误处理
						// 修正配额
						this.remaining.addAndGet(dialect.getLength());
						// 修正索引
						this.index.decrementAndGet();

						// 进行回调
						dialect.fireFailed(this.target);

						// 结束发送
						this.running.set(false);
						return;
					}

					if (this.index.get() + 1 == this.chunkNum) {
						dialect.fireCompleted(this.target);
						this.running.set(false);
					}
					else {
						try {
							Thread.sleep(this.interval);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						// 继续执行
						executor.execute(this);
					}
				}
				else {
					// 修正索引
					this.index.decrementAndGet();
					this.running.set(false);
				}
			}
			else {
				// 没有配额
				this.running.set(false);
			}
		}
	}


	/**
	 * 配额管理定时任务。
	 */
	private class QuotaTask extends TimerTask {

		/** 执行次数计数。 */
		private int counts = 0;

		/**
		 * 构造函数。
		 */
		private QuotaTask() {
			super();
		}

		@Override
		public void run() {
			if (null != cListMap) {
				Iterator<ChunkList> iter = cListMap.values().iterator();
				while (iter.hasNext()) {
					ChunkList list = iter.next();
					list.remaining.set(list.quota);

					if (!list.isComplete() && !list.running.get()) {
						// 列表没有发送完成
						list.running.set(true);
						executor.execute(list);
					}
				}
			}

			if (null != sListMap) {
				Iterator<ChunkList> iter = sListMap.values().iterator();
				while (iter.hasNext()) {
					ChunkList list = iter.next();
					list.remaining.set(list.quota);

					if (!list.isComplete() && !list.running.get()) {
						// 列表没有发送完成
						list.running.set(true);
						executor.execute(list);
					}
				}
			}

			++this.counts;

			if (this.counts >= 1000) {
				this.counts = 0;

				if (null != cListMap) {
					executor.execute(new Runnable() {
						@Override
						public void run() {
							ArrayList<String> deleteList = new ArrayList<String>();
							checkAndClearList(cListMap, deleteList, listTimeout);
							deleteList = null;
						}
					});
				}

				if (null != sListMap) {
					executor.execute(new Runnable() {
						@Override
						public void run() {
							ArrayList<String> deleteList = new ArrayList<String>();
							checkAndClearList(sListMap, deleteList, listTimeout);
							deleteList = null;
						}
					});
				}
			}
		}

	}


	/**
	 * 清理缓存任务。
	 */
	private class ClearCacheTask implements Runnable {
		/**
		 * 构造函数。
		 */
		private ClearCacheTask() {
		}

		@Override
		public void run() {
			long time = Long.MAX_VALUE;
			Cache selected = null;

			for (Cache cache : cacheMap.values()) {
				// 找到最旧的 cache
				long ft = cache.getTimestamp();
				if (ft < time) {
					time = ft;
					selected = cache;
				}
			}

			if (null != selected) {
				long size = selected.clear();

				// 更新内存大小记录
				cacheMemorySize.set(cacheMemorySize.get() - size);

				cacheMap.remove(selected.sign);
			}

			clearRunning.set(false);
		}
	}

}
