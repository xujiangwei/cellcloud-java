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

import net.cellcloud.core.Cellet;
import net.cellcloud.talk.dialect.Dialect;

/**
 * Cellet 数据事件回调监听器。
 * 
 * @author Ambrose Xu
 * 
 */
public interface CelletCallbackListener {

	/**
	 * 当 Cellet 向终端发送方言时此函数被回调。
	 * 
	 * @param cellet 发生此事件的 Cellet 。
	 * @param targetTag 发送目标的标签。
	 * @param dialect 发送的方言。
	 * @return 返回 <code>true</code> 表示截获该方言，将不被送入发送队列。
	 */
	public boolean doTalk(Cellet cellet, String targetTag, Dialect dialect);

	/**
	 * 当 Cellet 接收到来自终端的方言时此函数被回调。
	 * 
	 * @param cellet 发生此事件的 Cellet 。
	 * @param sourceTag 源终端的标签。
	 * @param dialect 接收到的方言。
	 * @return 返回 <code>true</code> 表示截获该方言，将不调用监听器通知 dialogue 事件发生。
	 */
	public boolean doDialogue(Cellet cellet, String sourceTag, Dialect dialect);

}
