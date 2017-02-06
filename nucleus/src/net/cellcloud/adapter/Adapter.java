/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2015 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.adapter;

import java.util.Map;

import net.cellcloud.core.Endpoint;

/** 适配器接口。
 * 
 * @author Jiangwei Xu
 */
public interface Adapter {

	/** 返回适配器名。 */
	public String getName();

	/** 返回实例名。 */
	public String getInstanceName();

	/** 配置适配器。 */
	public void config(Map<String, Object> parameters);

	/** 添加关联终端 */
	public boolean addEndpoint(Endpoint endpoint);

	/** 移除关联终端 */
	public void removeEndpoint(Endpoint endpoint);

	/** 配置适配器。该方法由适配器管理器调用。 */
	public void setup();

	/** 拆卸适配器。该方法由适配器管理器调用。 */
	public void teardown();

	/**
	 * 是否就绪。
	 * @return
	 */
	public boolean isReady();

}
