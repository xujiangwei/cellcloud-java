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

package net.cellcloud.common;

/**
 * 消息拦截器。
 * 
 * @author Ambrose Xu
 * 
 */
public interface MessageInterceptor {

	/**
	 * 是否拦截会话创建。
	 * 
	 * @param session 当前有效的会话对象。
	 * @return 如果返回 <code>true</code> 则表示创建被拦截，不调用会话创建回调，并结束处理流程。
	 */
	public boolean interceptCreating(Session session);

	/**
	 * 是否拦截会话启用。
	 * 
	 * @param session 当前有效的会话对象。
	 * @return 如果返回 <code>true</code> 则表示启用被拦截，不调用会话启用回调，并结束处理流程。
	 */
	public boolean interceptOpening(Session session);

	/**
	 * 是否拦截会话停用。
	 * 
	 * @param session 当前有效的会话对象。
	 * @return 如果返回 <code>true</code> 则表示停用被拦截，不调用会话停用回调，并结束处理流程。
	 */
	public boolean interceptClosing(Session session);

	/**
	 * 是否拦截会话销毁。
	 * 
	 * @param session 当前有效的会话对象。
	 * @return 如果返回 <code>true</code> 则表示销毁被拦截，不调用会话销毁回调，并结束处理流程。
	 */
	public boolean interceptDestroying(Session session);

	/**
	 * 是否拦截消息数据处理。
	 * 
	 * @param session 本次事件的会话。
	 * @param message 本次事件的消息数据。
	 * @return 如果返回 <code>true</code> 则表示消息被拦截，不调用消息解析和处理器回调，否则不进行拦截，继续后续数据处理。
	 */
	public boolean interceptMessage(Session session, Message message);

	/**
	 * 是否拦截错误处理。
	 * 
	 * @param session 发送本次错误的会话。
	 * @param errorCode 本次错误的错误码。
	 * @return 如果返回 <code>true</code> 则表示错误被拦截，不调用会话错误回调，并结束处理流程。
	 */
	public boolean interceptError(Session session, int errorCode);

}
