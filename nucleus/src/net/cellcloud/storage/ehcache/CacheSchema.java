package net.cellcloud.storage.ehcache;

import net.cellcloud.storage.Schema;

/**
 *  Ehcache数据方案
 * @author dengling
 *
 * @param <K>
 * @param <V>
 */
public class CacheSchema<K, V> implements Schema{

	private K k;
	private V v;
	private String cacheName;
	private CacheSchema.Action action;
	
	public enum Action{
		ADD(1), //增加
		REMOVE(2), //删除
		UPDATE(3), //更新，替换
		GET(4);
		
	    private int nCode ;
	    // 构造函数，枚举类型只能为私有
	    private Action(int _nCode) {
	    	this.nCode = _nCode;
	    }
	    @Override
	    public String toString() {
	    	return String.valueOf(this.nCode );
	    }
	}
	/**
	 * 
	 * @param k Key
	 * @param v Value
	 * @param cacheName 缓存名称
	 * @param action    动作名称
	 */
	public CacheSchema(K k, V v, String cacheName, Action action){
		this.k = k;
		this.v = v;
		this.cacheName = cacheName;
		this.action = action;
	}
	
	public K getKey() {
		return this.k;
	}
	
	public V getValue() {
		return this.v;
	}
	
	public String getCacheName() {
		return this.cacheName;
	}
	
	public Action getAction() {
		return this.action;
	}
	
}
