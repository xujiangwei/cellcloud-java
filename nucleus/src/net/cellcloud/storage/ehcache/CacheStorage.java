/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2013 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.storage.ehcache;

import javax.swing.Action;

import net.cellcloud.storage.ResultSet;
import net.cellcloud.storage.Schema;
import net.cellcloud.storage.Storage;
import net.cellcloud.util.Properties;
import net.cellcloud.util.StringProperty;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/** 缓存存储器。
 * 
 * @author Jiangwei Xu
 */
public class CacheStorage implements Storage {

	public final static String TYPE_NAME = "CacheStorage";

	private String name;
	
	private static CacheManager manager = null;
	

	protected CacheStorage(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getTypeName() {
		return CacheStorage.TYPE_NAME;
	}

	@Override
	public boolean open(Properties properties) {
		if (properties.hasProperty(CacheProperties.CACHE_XML)){
			manager = CacheManager.create(((StringProperty)properties.getProperty(CacheProperties.CACHE_XML)).getValueAsString());
			return true;
		}
		return false;
	}

	@Override
	public void close() {
		if (null != manager){
			manager.shutdown();
		}
	}

	@Override
	public ResultSet store(String statement) {
		return null;
	}

	/**
	 * 重写ehcache返回的结果集
	 */
	@Override
	public ResultSet store(Schema schema) {
		if (null == schema)
			return null;
		CacheSchema cacheSchema = (CacheSchema)schema;
		CacheSchema.Action action = cacheSchema.getAction();
		
		CacheResultSet retSet = new CacheResultSet();
		retSet.setCacheName(cacheSchema.getCacheName());
		retSet.setKey(cacheSchema.getKey().toString());
		
		switch(action)
		{
		case ADD:
			{
				put(cacheSchema.getCacheName(), cacheSchema.getKey().toString(), cacheSchema.getValue());
				break;
			}
			
		case REMOVE:
			{
				remove(cacheSchema.getCacheName(),cacheSchema.getKey().toString());
				break;
			}
		case UPDATE:
			{
				replace(cacheSchema.getCacheName(),cacheSchema.getKey().toString(), cacheSchema.getValue());
				break;
			}
		case GET:
			{
				Object ret = get(cacheSchema.getCacheName(),cacheSchema.getKey().toString());
				retSet.setValue(ret);
				break;
			}
		}
		
		return retSet;
	}
	
	/**
	 * 存入缓存
	 * @param cacheName
	 * @param key
	 * @param value
	 */
	private void put(String cacheName, String key, Object value) {  
        Cache cache = manager.getCache(cacheName);  
        Element element = new Element(key, value);  
        //cache.put(element);
        cache.put(element);
    } 
	
	/**
	 * 从缓存中取元素
	 * @param cacheName
	 * @param key
	 * @return
	 */
	private Object get(String cacheName, String key) {  
        Cache cache = manager.getCache(cacheName);  
        Element element = cache.get(key);  
        return element == null ? null : element.getObjectValue();  
    }
	
	private void replace(String cacheName, String key, Object value)
	{
		 Cache cache = manager.getCache(cacheName); 
		 cache.replace(new Element(key, value));
	}
	
	private void remove(String cacheName, String key) {  
        Cache cache = manager.getCache(cacheName);  
        cache.remove(key);  
    }
}
