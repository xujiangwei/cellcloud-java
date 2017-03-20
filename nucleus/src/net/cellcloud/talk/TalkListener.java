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


/**
 * Talk 连接监听器。
 * 
 * @author Ambrose Xu
 * 
 */
public interface TalkListener {

	/**
	 * 当收到来自服务器的数据时该函数被调用。
	 * 
	 * @param identifier 该数据来源的 Cellet 的标识。
	 * @param primitive 接收到的原语数据。
	 */
	public void dialogue(String identifier, Primitive primitive);

	/**
	 * 当终端成功与指定的 Cellet 建立连接时该函数被调用。
	 * 
	 * @param identifier 建立连接的 Cellet 的标识。
	 * @param tag Cellet 的内核标签。
	 */
	public void contacted(String identifier, String tag);

	/**
	 * 当终端与 Cellet 的连接断开时该函数被调用。
	 * 
	 * @param identifier 断开连接的 Cellet 的标识。
	 * @param tag Cellet 的内核标签。
	 */
	public void quitted(String identifier, String tag);

	/**
	 * 当发生连接错误时该函数被调用。
	 * 
	 * @param tag 发生错误的内核标签。
	 * @param failure 错误描述。
	 */
	public void failed(String tag, TalkServiceFailure failure);

}
