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

package net.cellcloud.storage.mongodb;

import java.util.List;

import net.cellcloud.storage.ResultSet;
import net.cellcloud.storage.Schema;
import net.cellcloud.storage.Storage;
import net.cellcloud.util.property.Properties;

import com.mongodb.ServerAddress;

/** MongoDB 存储器。
 * 
 * @author Jiangwei Xu
 */
public final class MongoDBStorage implements Storage {

	public final static String TYPE_NAME = "MongoDBStorage";

	private String name;

	protected MongoDBStorage(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getTypeName() {
		return MongoDBStorage.TYPE_NAME;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean open(Properties properties) {
		if (properties.hasProperty(MongoDBProperties.SERVER_ADDRESS)) {
			List<ServerAddress> list = (List<ServerAddress>) properties.getProperty(MongoDBProperties.SERVER_ADDRESS).getValue();
			for (ServerAddress addr : list) {
				addr.getHost();
			}
		}
		return false;
	}

	@Override
	public void close() {

	}

	@Override
	public ResultSet store(String statement) {
		return null;
	}

	@Override
	public ResultSet store(Schema schema) {
		return null;
	}
}
