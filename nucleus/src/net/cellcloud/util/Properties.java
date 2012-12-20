/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (cellcloudproject@gmail.com)

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

package net.cellcloud.util;

import java.util.Collection;
import java.util.HashMap;

/** 属性集。
 * 
 * @author Jiangwei Xu
 */
public class Properties {

	private HashMap<String, PropertyReference> properties;

	public Properties() {
		this.properties = new HashMap<String, PropertyReference>();
	}

	/** 添加属性。
	 */
	public void addProperty(PropertyReference property) {
		this.properties.put(property.getKey(), property);
	}

	/** 移除属性。
	 */
	public void removeProperty(String key) {
		this.properties.remove(key);
	}

	/** 返回属性。
	 */
	public PropertyReference getProperty(String key) {
		return this.properties.get(key);
	}

	/** 更新属性。
	 */
	public void updateProperty(PropertyReference property) {
		this.properties.remove(property.getKey());
		this.properties.put(property.getKey(), property);
	}

	/** 是否包含指定主键的属性。
	 */
	public boolean hasProperty(String key) {
		return this.properties.containsKey(key);
	}

	/** 返回属性集合。
	 */
	public Collection<PropertyReference> getPropertyCollection() {
		return this.properties.values();
	}
}
