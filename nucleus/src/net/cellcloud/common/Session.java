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

package net.cellcloud.common;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import net.cellcloud.util.Utils;

/**
 * 消息会话描述类。
 * 
 * @author Ambrose Xu
 * 
 */
public class Session implements Comparable<Object> {

	/** 会话的 ID 。 */
	private Long id;
	/** 会话创建时的时间戳。 */
	private long timestamp;
	/** 会话对应消息服务。 */
	private MessageService service;
	/** 会话对应的终端地址。 */
	private InetSocketAddress address;

	/** 会话的密钥。 */
	private byte[] secretKey;

	/** 数据缓存。 */
	protected byte[] cache;
	/** 当前数据缓存的游标。 */
	protected int cacheCursor;

	/** 属性映射，用于存储会话的属性。 */
	private ConcurrentHashMap<String, Object> attributes;

	/** 终端使用的数据包主版本号。 */
	public int major = 2;
	/** 终端使用的数据包副版本号。 */
	public int minor = 0;

	/**
	 * 构造函数。
	 * 
	 * @param service 指定消息服务。
	 * @param address 指定终端的连接地址。
	 */
	public Session(MessageService service, InetSocketAddress address) {
		this.id = Utils.generateSerialNumber();
		this.timestamp = System.currentTimeMillis();
		this.service = service;
		this.address = address;
		this.secretKey = null;

		this.cache = new byte[4096];
		this.cacheCursor = 0;
	}

	/**
	 * 构造函数。
	 * 
	 * @param id 指定 ID 。
	 * @param service 指定消息服务。
	 * @param address 指定终端的连接地址。
	 */
	public Session(long id, MessageService service, InetSocketAddress address) {
		this.id = id;
		this.timestamp = System.currentTimeMillis();
		this.service = service;
		this.address = address;
		this.secretKey = null;

		this.cache = new byte[4096];
		this.cacheCursor = 0;
	}

	/**
	 * 获得会话 ID 。
	 * 
	 * @return 返回会话 ID 。
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * 获得会话创建的时间戳。
	 * 
	 * @return 返回会话创建的时间戳。
	 */
	public long getTimestamp() {
		return this.timestamp;
	}

	/**
	 * 获得消息服务实例。
	 * 
	 * @return 返回消息服务实例。
	 */
	public MessageService getService() {
		return this.service;
	}

	/**
	 * 获得会话的网络地址。
	 * 
	 * @return 返回会话的网络地址。
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/**
	 * 添加属性。
	 * 
	 * @param name 指定属性名。
	 * @param value 指定属性值。
	 */
	public void addAttribute(String name, Object value) {
		if (null == this.attributes) {
			this.attributes = new ConcurrentHashMap<String, Object>();
		}

		this.attributes.put(name, value);
	}

	/**
	 * 移除属性。
	 * 
	 * @param name 指定需删除属性的属性名。
	 */
	public Object removeAttribute(String name) {
		if (null == this.attributes) {
			return null;
		}

		return this.attributes.remove(name);
	}

	/**
	 * 获取指定的属性值。
	 * 
	 * @param name 指定属性名。
	 * @return 返回查找到的属性值。如果没有找到返回 <code>null</code> 。
	 */
	public Object getAttribute(String name) {
		if (null == this.attributes) {
			return null;
		}

		return this.attributes.get(name);
	}

	/**
	 * 判断是否有指定名称的属性。
	 * 
	 * @param name 指定待查找的属性名。
	 * @return 如果当前会话中有该属性返回 <code>true</code> ，否则返回 <code>false</code> 。
	 */
	public boolean hasAttribute(String name) {
		if (null == this.attributes) {
			return false;
		}

		return this.attributes.containsKey(name);
	}

	/**
	 * 当前会话是否使用了安全连接。
	 * 
	 * @return 返回是否是加密连接。
	 */
	public boolean isSecure() {
		return (null != this.secretKey);
	}

	/**
	 * 激活会话使用密钥。
	 * 
	 * @param key 指定密钥。
	 * @return 激活成功返回 <code>true</code> 。
	 */
	public boolean activeSecretKey(byte[] key) {
		if (null == key) {
			this.secretKey = null;
			return false;
		}

		if (key.length < 8) {
			return false;
		}

		this.secretKey = new byte[8];
		System.arraycopy(key, 0, this.secretKey, 0, 8);
		return true;
	}

	/**
	 * 吊销密钥。
	 */
	public void deactiveSecretKey() {
		this.secretKey = null;
	}

	/**
	 * 获得安全密钥。
	 * 
	 * @return 返回存储了安全密钥的数组。
	 */
	public byte[] getSecretKey() {
		return this.secretKey;
	}

	/**
	 * 向该会话写消息。
	 * 
	 * @param message 指定待写入的消息。
	 */
	public void write(Message message) {
		this.service.write(this, message);
	}

	@Override
	public boolean equals(Object obj) {
		if (null != obj && obj instanceof Session) {
			Session other = (Session) obj;
			return this.id.longValue() == other.id.longValue();
		}

		return false;
	}

	@Override
	public int hashCode() {
		return this.id.intValue();
	}

	@Override
	public int compareTo(Object o) {
		if (o instanceof Session) {
			return (int)(this.timestamp - ((Session) o).timestamp);
		}

		return 0;
	}

	/**
	 * 获得数据缓存的大小。
	 * 
	 * @return 返回数据缓存的大小。
	 */
	protected int getCacheSize() {
		synchronized (this) {
			return this.cache.length;
		}
	}

	/**
	 * 重置缓存的数据和游标。
	 */
	protected void resetCache() {
		synchronized (this) {
			if (this.cache.length > 4096) {
				this.cache = null;
				this.cache = new byte[4096];
			}

			this.cacheCursor = 0;
		}
	}

	/**
	 * 重置缓存大小。重置缓存大小不会清空当前的数据。
	 * 
	 * @param newSize 指定新的缓存大小。
	 */
	protected void resetCacheSize(int newSize) {
		synchronized (this) {
			if (newSize <= this.cache.length) {
				return;
			}

			if (this.cacheCursor > 0) {
				byte[] cur = new byte[this.cacheCursor];
				System.arraycopy(this.cache, 0, cur, 0, this.cacheCursor);
				this.cache = new byte[newSize];
				System.arraycopy(cur, 0, this.cache, 0, this.cacheCursor);
				cur = null;
			}
			else {
				this.cache = new byte[newSize];
			}
		}
	}

}
