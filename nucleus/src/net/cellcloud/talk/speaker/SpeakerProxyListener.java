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

package net.cellcloud.talk.speaker;

import net.cellcloud.talk.Primitive;

import org.json.JSONObject;

/**
 * 标准协议对话者代理监听器。
 * 
 * @author Ambrose Xu
 *
 */
public interface SpeakerProxyListener {

	/**
	 * 当收到来自被代理节点的对话原语时此函数被调用。
	 * 
	 * @param tag 代理节点指定的目标终端的内核标签。
	 * @param celletIdentifier 此原语的 Cellet 标签。
	 * @param primitive 原语数据。
	 */
	public void onProxyDialogue(String tag, String celletIdentifier, Primitive primitive);

	/**
	 * 当收到被代理节点发来的协商代理数据时此函数被调用。
	 * 
	 * @param speaker 与此次协商相关的对话者。
	 * @param data 代理协议的数据包。
	 */
	public void onProxy(Speaker speaker, JSONObject data);

}
