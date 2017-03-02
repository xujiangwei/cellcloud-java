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

import net.cellcloud.talk.dialect.Dialect;

/**
 * Talk 对话者事件委派。
 * 
 * @author Ambrose Xu
 *
 */
public interface TalkDelegate {

	/**
	 * 当准备执行数据发送时调用此函数。
	 * 
	 * @param identifier 发送目标标识。
	 * @param dialect 发送的方言。
	 * @return 如果返回 <code>false</code> 则劫持事件，阻止事件回调发生。
	 */
	public boolean doTalk(String identifier, Dialect dialect);

	/**
	 * 当完成数据发送时调用此函数。
	 * 
	 * @param identifier 发送目标标识。
	 * @param dialect 发送的方言。
	 */
	public void didTalk(String identifier, Dialect dialect);

	/**
	 * 当准备执行对话数据送达时调用此函数。
	 * 
	 * @param identifier 目标标识。
	 * @param dialect 方言数据。
	 * @return 如果返回 <code>false</code> 则劫持事件，阻止事件回调发生。
	 */
	public boolean doDialogue(String identifier, Dialect dialect);

	/**
	 * 当完成对话数据送达时调用此函数。
	 * 
	 * @param identifier 目标标识。
	 * @param dialect 方言数据。
	 */
	public void didDialogue(String identifier, Dialect dialect);

}
