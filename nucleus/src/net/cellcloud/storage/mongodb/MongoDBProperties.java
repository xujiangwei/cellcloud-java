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

package net.cellcloud.storage.mongodb;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import net.cellcloud.util.ListProperty;
import net.cellcloud.util.Properties;

import com.mongodb.ServerAddress;

/** MongoDB Storage 配置属性。
 * 
 * @author Jiangwei Xu
 */
public class MongoDBProperties extends Properties {

	public final static String SERVER_ADDRESS = "ServerAddress";

	public MongoDBProperties() {
	}

	/** 添加服务器地址信息。
	 */
	@SuppressWarnings("unchecked")
	public void addServerAddress(String host, int port) {
		List<ServerAddress> list = null;
		if (this.hasProperty(SERVER_ADDRESS)) {
			list = ((ListProperty<ServerAddress>)this.getProperty(SERVER_ADDRESS)).getValueAsList();
		}
		else {
			list = new ArrayList<ServerAddress>();
			ListProperty<ServerAddress> prop = new ListProperty<ServerAddress>(SERVER_ADDRESS, list);
			this.addProperty(prop);
		}

		try {
			list.add(new ServerAddress(host, port));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	/** 返回服务器地址列表。
	 */
	@SuppressWarnings("unchecked")
	public List<ServerAddress> getServerAddressList() {
		if (this.hasProperty(SERVER_ADDRESS)) {
			return ((ListProperty<ServerAddress>)this.getProperty(SERVER_ADDRESS)).getValueAsList();
		}
		else {
			return null;
		}
	}
}
