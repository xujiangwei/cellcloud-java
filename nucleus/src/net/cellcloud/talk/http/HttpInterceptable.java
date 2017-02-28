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

package net.cellcloud.talk.http;

import net.cellcloud.http.HttpSession;
import net.cellcloud.talk.Primitive;

/**
 * HTTP 消息拦截器接口。
 * 
 * @author Ambrose Xu
 *
 */
public interface HttpInterceptable {

	/**
	 * 当 HTTP 协议处理器接收到 HTTP 请求后，调用此方法判断是否要将此请求拦截，拦截后不再回调默认事件。
	 * 
	 * @param session 当前 HTTP Session 。
	 * @param speakerTag 源会话器的内核标签。
	 * @param celletIdentifier 目标 Cellet 标识。
	 * @param primitive 接收到的原语数据。
	 * @return 如果拦截此数据返回 <code>true</code> 。
	 */
	public boolean intercept(HttpSession session, String speakerTag, String celletIdentifier, Primitive primitive);

}
