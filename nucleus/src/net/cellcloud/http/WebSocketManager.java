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

package net.cellcloud.http;

import net.cellcloud.common.Message;
import net.cellcloud.common.MessageInterceptor;

/**
 * WebSocket 管理器接口。
 * 
 * @author Ambrose Xu
 *
 */
public interface WebSocketManager {

	/**
	 * 是否存在指定的会话。
	 * 
	 * @param session 指定待判断的会话。
	 * @return 如果存在指定的会话返回 <code>true</code> 。
	 */
	public boolean hasSession(WebSocketSession session);

	/**
	 * 向指定的会话写入消息。
	 * 
	 * @param session 指定目标会话。
	 * @param message 指定待写入的消息。
	 */
	public void write(WebSocketSession session, Message message);

	/**
	 * 关闭指定的会话。
	 * 
	 * @param session 待关闭的会话。
	 */
	public void close(WebSocketSession session);

	/**
	 * 设置消息拦截器。
	 * 
	 * @param interceptor 指定消息拦截器实例。
	 */
	public void setInterceptor(MessageInterceptor interceptor);

}
