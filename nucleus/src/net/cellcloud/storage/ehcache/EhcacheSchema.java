package net.cellcloud.storage.ehcache;

import net.cellcloud.storage.Schema;

/**
 * Ehcache 数据方案。
 * 
 * @author Sun Dengling
 * 
 * @param <V>
 */
public class EhcacheSchema implements Schema{

	private String k;
	private Object v;
	private String cacheName;
	private EhcacheSchema.Operation operation;

	public enum Operation {
		PUT(1),		// 增加
		REMOVE(2),	// 删除
		UPDATE(3),	// 更新
		GET(4);		// 获取

		private int code;

		// 构造函数，枚举类型只能为私有
		private Operation(int value) {
			this.code = value;
		}

		@Override
		public String toString() {
			return String.valueOf(this.code);
		}
	}

	/**
	 * Get
	 * 
	 * @param key
	 */
	public EhcacheSchema(String key) {
		this(null, Operation.GET, key, null);
	}

	/**
	 * Put
	 * 
	 * @param key
	 * @param value
	 */
	public EhcacheSchema(String key, Object value) {
		this(null, Operation.PUT, key, value);
	}

	public EhcacheSchema(Operation operation, String key) {
		this(null, operation, key, null);
	}

	public EhcacheSchema(Operation operation, String key, Object value) {
		this(null, operation, key, value);
	}

	/**
	 * Get
	 * 
	 * @param cacheName
	 * @param key
	 */
	public EhcacheSchema(String cacheName, String key) {
		this(cacheName, Operation.GET, key, null);
	}

	/**
	 * Put
	 * 
	 * @param cacheName
	 * @param key
	 * @param value
	 */
	public EhcacheSchema(String cacheName, String key, Object value) {
		this(cacheName, Operation.PUT, key, value);
	}

	public EhcacheSchema(String cacheName, Operation operation, String key) {
		this(cacheName, operation, key, null);
	}

	public EhcacheSchema(String cacheName, Operation operation, String key, Object value) {
		this.cacheName = cacheName;
		this.operation = operation;
		this.k = key;
		this.v = value;
	}

	public String getKey() {
		return this.k;
	}

	public Object getValue() {
		return this.v;
	}

	public String getCacheName() {
		return this.cacheName;
	}

	public Operation getOperation() {
		return this.operation;
	}
}
