/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2016 Cell Cloud Team (www.cellcloud.net)

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

import java.io.IOException;
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

import net.cellcloud.common.Base64;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.core.Cellet;
import net.cellcloud.talk.TalkService;
import net.cellcloud.util.Clock;

/** 块数据传输方言工厂。
 * 
 * @author Jiangwei Xu
 */
public class ChunkDialectFactory extends DialectFactory {

	private DialectMetaData metaData;

	private ExecutorService executor;

	// 配额定时器
	private Timer quotaTimer;
	private long defaultQuotaPerList = 4L * 1024L;

	// 数据接收缓存，Key: Sign
	private ConcurrentHashMap<String, Cache> cacheMap;

	// 用于服务器模式下的队列
	private ConcurrentHashMap<String, ChunkList> sListMap;

	// 用于客户端模式下的队列
	private ConcurrentHashMap<String, ChunkList> cListMap;

	// 数据在发送缓存里的超时时间
	private long listTimeout = 10L * 60L * 1000L;

	private AtomicLong cacheMemorySize = new AtomicLong(0);
	private final long clearThreshold = 100L * 1024L * 1024L;
	private AtomicBoolean clearRunning = new AtomicBoolean(false);
	private Object mutex = new Object();

	private int logCounts = 0;

	public ChunkDialectFactory(ExecutorService executor) {
		this.metaData = new DialectMetaData(ChunkDialect.DIALECT_NAME, "Chunk Dialect");
		this.executor = executor;
		this.cacheMap = new ConcurrentHashMap<String, Cache>();
		this.quotaTimer = new Timer("ChunkQuotaTimer");
		// 每 100ms 一次任务
		this.quotaTimer.schedule(new QuotaTask(), 3000L, 100L);
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

		if (null != this.quotaTimer) {
			this.quotaTimer.cancel();
			this.quotaTimer.purge();
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
		if (null == this.sListMap) {
			return 0;
		}

		return this.sListMap.size();
	}

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

			this.updateListMap(this.cListMap, identifier, chunk, null);
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

			this.updateListMap(this.sListMap, targetTag, chunk, cellet);
		}

		return false;
	}

	@Override
	protected boolean onDialogue(final Cellet cellet, final String sourceTag, Dialect dialect) {
		ChunkDialect chunk = (ChunkDialect) dialect;

		this.write(chunk);

		return true;
	}

	private synchronized boolean write(ChunkDialect chunk) {
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
		this.cacheMemorySize.addAndGet(chunk.getDataSize());

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

	protected int read(String sign, int index, byte[] out) {
		if (index < 0) {
			return -1;
		}

		Cache cache = this.cacheMap.get(sign);
		if (null != cache) {
			ChunkDialect cd = cache.get(index);
			byte[] buf = null;
			try {
				buf = Base64.decode(cd.data);
			} catch (IOException e) {
				Logger.log(this.getClass(), e, LogLevel.WARNING);
			}
			int len = cd.length;

			if (null == buf) {
				return -1;
			}

			System.arraycopy(buf, 0, out, 0, len);
			return len;
		}

		return -1;
	}

	protected boolean checkCompleted(String sign) {
		if (null == sign) {
			return false;
		}

		Cache cache = this.cacheMap.get(sign);
		if (null != cache) {
			return cache.checkCompleted();
		}

		return false;
	}

	protected void clear(String sign) {
		Cache cache = this.cacheMap.remove(sign);
		if (null != cache) {
			// 计算缓存大小变化差值，进行缓存清理
			long size = cache.clear();

			// 更新内存大小
			this.cacheMemorySize.set(this.cacheMemorySize.get() - size);
		}
	}

	private void updateListMap(ConcurrentHashMap<String, ChunkList> listMap, String target, ChunkDialect chunk, Cellet cellet) {
		ChunkList list = listMap.get(chunk.getSign());
		if (null != list) {
			if (chunk.getChunkIndex() == 0) {
				list.reset(chunk.chunkNum);
			}

			// 写入列表
			list.append(chunk);
		}
		else {
			list = new ChunkList(target.toString(), chunk.getChunkNum(), this.defaultQuotaPerList, cellet);
			list.append(chunk);
			listMap.put(chunk.getSign().toString(), list);
		}

		if (!list.running.get()) {
			list.running.set(true);
			this.executor.execute(list);
		}
	}

	private void checkAndClearList(final ConcurrentHashMap<String, ChunkList> listMap, final List<String> deleteList, long timeout) {
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
			for (String sign : deleteList) {
				listMap.remove(sign);

				Logger.i(ChunkDialectFactory.class, "Clear chunk list - sign: " + sign);
			}

			deleteList.clear();
		}
	}

	/**
	 * 内部缓存。
	 */
	private class Cache {
		private String sign;
		private ArrayList<ChunkDialect> dataQueue;
		private long timestamp;
		private long dataSize;

		private Cache(String sign, int capacity) {
			this.sign = sign;
			this.dataQueue = new ArrayList<ChunkDialect>(capacity);
			for (int i = 0; i < capacity; ++i) {
				this.dataQueue.add(new ChunkDialect());
			}
			this.dataSize = 0;
		}

		public void offer(ChunkDialect dialect) {
			synchronized (this.dataQueue) {
				if (this.dataQueue.contains(dialect)) {
					int index = this.dataQueue.indexOf(dialect);
					this.dataQueue.set(index, dialect);
				}
				else {
					this.dataQueue.set(dialect.getChunkIndex(), dialect);
					this.dataSize += dialect.getDataSize();
				}
			}

			this.timestamp = Clock.currentTimeMillis();
		}

		public ChunkDialect get(int index) {
			synchronized (this.dataQueue) {
				if (index >= this.dataQueue.size()) {
					return null;
				}

				return this.dataQueue.get(index);
			}
		}

		public boolean checkCompleted() {
			synchronized (this.dataQueue) {
				if (this.dataQueue.isEmpty()) {
					return false;
				}

				for (ChunkDialect cd : this.dataQueue) {
					if (cd.getDataSize() < 0) {
						return false;
					}
				}
			}

			return true;
		}

		public long clear() {
			long size = this.dataSize;
			synchronized (this.dataQueue) {
				this.dataQueue.clear();
				this.dataSize = 0;
			}
			return size;
		}

		public long getTimestamp() {
			return this.timestamp;
		}
	}

	private class ChunkList implements Runnable {
		private Cellet cellet = null;
		private long timestamp;
		private String target;
		private int chunkNum = 0;
		private ArrayList<ChunkDialect> list;
		private AtomicInteger index;

		private AtomicBoolean running;

		private long quota;
		private AtomicLong remaining;

		private ChunkList(String target, int chunkNum, long quota, Cellet cellet) {
			this.timestamp = Clock.currentTimeMillis();
			this.target = target;
			this.chunkNum = chunkNum;
			this.quota = quota;
			this.cellet = cellet;
			this.list = new ArrayList<ChunkDialect>();
			this.index = new AtomicInteger(-1);
			this.running = new AtomicBoolean(false);
			this.remaining = new AtomicLong(this.quota);
		}

		protected void append(ChunkDialect chunk) {
			// 标识为已污染
			chunk.infectant = true;

			synchronized (this) {
				if (!this.list.contains(chunk)) {
					this.list.add(chunk);
				}
			}
		}

		protected boolean isComplete() {
			return (this.index.get() + 1 == this.chunkNum);
		}

		protected void reset(int chunkNum) {
			this.timestamp = Clock.currentTimeMillis();
			this.chunkNum = chunkNum;
			this.index.set(-1);
			this.remaining.set(this.quota);

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
					qr = qr - dialect.getDataSize();
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
						this.remaining.addAndGet(dialect.getDataSize());
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
							Thread.sleep(1);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						this.run();
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

	private class QuotaTask extends TimerTask {

		private int counts = 0;

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
	 *
	 */
	private class ClearCacheTask implements Runnable {
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
