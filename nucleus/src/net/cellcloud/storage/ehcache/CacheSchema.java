package net.cellcloud.storage.ehcache;

import net.cellcloud.storage.Schema;

/**
 * Ehcache数据方案。
 * 
 * @author dengling
 * 
 * @param <V>
 */
public class CacheSchema implements Schema{

	private String k;
	private Object v;
	private String cacheName;
	private CacheSchema.Operation operation;

	public enum Operation {
		ADD(1),		//增加
		REMOVE(2),	//删除
		UPDATE(3),	//更新
		GET(4);

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
	 * 
	 * @param k Key
	 * @param v Value
	 * @param cacheName 缓存名称
	 * @param action    动作名称
	 */
	public CacheSchema(String k, Object v, String cacheName, Operation operation){
		this.k = k;
		this.v = v;
		this.cacheName = cacheName;
		this.operation = operation;
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
