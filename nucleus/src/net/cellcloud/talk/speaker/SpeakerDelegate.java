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
import net.cellcloud.talk.Speakable;
import net.cellcloud.talk.TalkServiceFailure;

/**
 * 对话者的事件委派。
 * 
 * @author Ambrose Xu
 * 
 */
public interface SpeakerDelegate {

	/**
	 * 当收到来自服务器的原语时此函数被调用。
	 * 
	 * @param speaker 接收到数据的对话者实例。
	 * @param celletIdentifier 数据源的 Cellet 标识。
	 * @param primitive 接收到的原语。
	 */
	public void onDialogue(Speakable speaker, String celletIdentifier, Primitive primitive);

	/**
	 * 当对话者与 Cellet 建立起连接时此函数被调用。
	 * 
	 * @param speaker 建立服务连接的对话者实例。
	 * @param celletIdentifier Cellet 标识。
	 */
	public void onContacted(Speakable speaker, String celletIdentifier);

	/**
	 * 当对话者断开与 Cellet 的服务连接时此函数被调用。
	 * 
	 * @param speaker 断开服务连接的对话者实例。
	 * @param celletIdentifier Cellet 标识。
	 */
	public void onQuitted(Speakable speaker, String celletIdentifier);

	/**
	 * 当对话者在服务时发生错误时此函数被调用。
	 * 
	 * @param speaker 发生错误的对话者实例。
	 * @param failure 发生的错误信息。
	 */
	public void onFailed(Speakable speaker, TalkServiceFailure failure);

}
