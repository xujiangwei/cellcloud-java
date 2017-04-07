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

package net.cellcloud.talk;

import java.util.List;

/**
 * 通信会话器接口。
 * 
 * @author Ambrose Xu
 * 
 */
public interface Speakable {

	/**
	 * 向指定地址发起请求 Cellet 服务。
	 * 
	 * @param identifiers 指定 Cellet 标识清单。
	 * @return 如果请求发出返回 <code>true</code> 。
	 */
	public boolean call(List<String> identifiers);

	/**
	 * 挂断与 Cellet 的服务。
	 */
	public void hangUp();

	/**
	 * 向 Cellet 发送原语数据。
	 * 
	 * @param identifier 指定需接收原语的 Cellet 标识。
	 * @param primitive 指定原语数据。
	 * @return 如果数据成功送入发送队列返回 <code>true</code> 。
	 */
	public boolean speak(String celletIdentifier, Primitive primitive);

	/**
	 * 是否已经与 Cellet 建立服务。
	 * 
	 * @return 如果已经建立服务返回 <code>true</code> 。
	 */
	public boolean isCalled();

	/**
	 * 获得会话器请求的所有 Cellet 标识符列表。
	 * 
	 * @return 返回 Cellet 标识符列表。
	 */
	public List<String> getIdentifiers();

	/**
	 * 获得远端的内核标签。
	 * 
	 * @return 返回远端的内核标签。
	 */
	public String getRemoteTag();

}
