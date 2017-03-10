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

package net.cellcloud.util.property;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 属性集。
 * 
 * @author Ambrose Xu
 * 
 */
public class Properties {

	private ConcurrentHashMap<String, PropertyReference> properties;

	/**
	 * 构造函数。
	 */
	public Properties() {
		this.properties = new ConcurrentHashMap<String, PropertyReference>();
	}

	/**
	 * 添加属性。
	 * 
	 * @param property 指定待添加属性。
	 */
	public void addProperty(PropertyReference property) {
		this.properties.put(property.getKey(), property);
	}

	/**
	 * 移除属性。
	 * 
	 * @param key 指定待移除的属性的键。
	 */
	public void removeProperty(String key) {
		this.properties.remove(key);
	}

	/**
	 * 返回属性。
	 * 
	 * @param key 指定属性键。
	 * @return 返回属性。
	 */
	public PropertyReference getProperty(String key) {
		return this.properties.get(key);
	}

	public boolean getPropertyValue(String key, boolean defaultValue) {
		PropertyReference ref = this.properties.get(key);
		if (null == ref || !(ref instanceof BooleanProperty)) {
			return defaultValue;
		}
		return ((BooleanProperty) ref).getValueAsBoolean();
	}

	/**
	 * 更新属性。
	 * 
	 * @param property 指定待更新的属性。
	 */
	public void updateProperty(PropertyReference property) {
		this.properties.remove(property.getKey());
		this.properties.put(property.getKey(), property);
	}

	/**
	 * 是否包含指定主键的属性。
	 * 
	 * @param key 指定键。
	 * @return 如果包含此键对应的属性返回 <code>true</code> 。
	 */
	public boolean hasProperty(String key) {
		return this.properties.containsKey(key);
	}

	/**
	 * 返回属性集合。
	 * 
	 * @return 返回属性集合。
	 */
	public Collection<PropertyReference> getPropertyCollection() {
		return this.properties.values();
	}

}
