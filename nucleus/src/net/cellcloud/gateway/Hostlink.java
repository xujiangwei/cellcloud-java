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

package net.cellcloud.gateway;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于记录上位主机与终端之间的连接关系的上位链路。
 * 
 * @author Ambrose Xu
 *
 */
public class Hostlink {

	/**
	 * 上位机（网关机）代理的会话标签对应的上位机标签。
	 * 键是被代理的终端标签，值是上位网关标签。
	 */
	private ConcurrentHashMap<String, String> targetMapProxyTag;

	/**
	 * 构造函数。
	 */
	public Hostlink() {
		this.targetMapProxyTag = new ConcurrentHashMap<String, String>();
	}

	/**
	 * 添加上位链路信息。
	 * 
	 * @param targetTag 指定被上位机代理的标签。
	 * @param proxyTag 指定上位机的标签。
	 */
	public void addLink(String targetTag, String proxyTag) {
		this.targetMapProxyTag.put(targetTag, proxyTag);
	}

	/**
	 * 删除上位链路信息。
	 * 
	 * @param targetTag 指定被上位机代理的标签。
	 */
	public void removeLink(String targetTag) {
		this.targetMapProxyTag.remove(targetTag);
	}

	/**
	 * 用目标终端检索上位主机的标签，即查询对应的代理的标签。
	 * 
	 * @param targetTag 指定被上位机代理的标签。
	 * @return 返回上位机标签。
	 */
	public String searchHost(String targetTag) {
		return this.targetMapProxyTag.get(targetTag);
	}

	/**
	 * 列举出指定上位机里被代理的所有标签。
	 * 
	 * @param proxyTag 指定上位机标签。
	 * @return 返回存储被代理终端标签的数组。
	 */
	public List<String> listLinkedTag(String proxyTag) {
		ArrayList<String> list = new ArrayList<String>();
		Iterator<Entry<String, String>> iter = this.targetMapProxyTag.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, String> e = iter.next();
			if (e.getValue().equals(proxyTag)) {
				list.add(e.getKey());
			}
		}
		return list;
	}

}
