/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.storage;

import java.util.concurrent.ConcurrentHashMap;

/** 存储器枚举。
 * 
 * @author Jiangwei Xu
 */
public final class StorageEnumerator {

	private final static StorageEnumerator instance = new StorageEnumerator();

	private ConcurrentHashMap<String, StorageFactory> factories;

	private StorageEnumerator() {
		this.factories = new ConcurrentHashMap<String, StorageFactory>();

		// 加入默认内置的存取器。
		this.buildIn();
	}

	/** 返回枚举器实例。
	 */
	public synchronized static StorageEnumerator getInstance() {
		return StorageEnumerator.instance;
	}

	/** 添加工厂。
	 */
	public void addFactory(StorageFactory factory) {
		if (this.factories.containsKey(factory.getMetaData().getTypeName())) {
			return;
		}

		this.factories.put(factory.getMetaData().getTypeName(), factory);
	}

	/** 移除工厂。
	 */
	public void removeFactory(StorageFactory factory) {
		if (this.factories.containsKey(factory.getMetaData().getTypeName())) {
			this.factories.remove(factory.getMetaData().getTypeName());
		}
	}

	/** 移除工厂。
	 */
	public void removeFactory(final String typeName) {
		if (this.factories.containsKey(typeName)) {
			this.factories.remove(typeName);
		}
	}

	/** 创建存储器。
	 */
	public Storage createStorage(String typeName, String instanceName) {
		if (this.factories.containsKey(typeName)) {
			return this.factories.get(typeName).create(instanceName);
		}

		return null;
	}

	/** 销毁存储器。
	 */
	public void destroyStorage(Storage storage) {
		if (this.factories.containsKey(storage.getTypeName())) {
			this.factories.get(storage.getTypeName()).destroy(storage);
		}
	}

	private void buildIn() {
		this.addFactory(new LocalFileStorageFactory());
		this.addFactory(new MongoStorageFactory());
	}
}
