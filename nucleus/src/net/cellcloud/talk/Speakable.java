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

package net.cellcloud.talk;

import java.net.InetSocketAddress;

/**
 * 通信会话器接口。
 * 
 * @author Jiangwei Xu
 */
public interface Speakable {

	/**
	 * 向指定地址发起请求 Cellet 服务。
	 * @param address
	 * @return
	 */
	public boolean call(InetSocketAddress address);

	/**
	 * 挂起服务。
	 * @param duration 指定挂起的有效时长。
	 */
	public void suspend(long duration);

	/**
	 * 恢复服务。
	 * @param startTime
	 */
	public void resume(long startTime);

	/**
	 * 挂断与 Cellet 的服务。
	 */
	public void hangUp();

	/**
	 * 向 Cellet 发送原语数据。
	 * @param primitive
	 * @return
	 */
	public boolean speak(Primitive primitive);

	/**
	 * 是否已经与 Cellet 建立服务。
	 * @return
	 */
	public boolean isCalled();

	/**
	 * Cellet 服务器是否已经被挂起。
	 * @return
	 */
	public boolean isSuspended();

	/**
	 * 返回与会话器对话的 Cellet 的识别符。
	 * @return
	 */
	public String getIdentifier();

	/**
	 * 返回远端的内核标签。
	 * @return
	 */
	public String getRemoteTag();
}
