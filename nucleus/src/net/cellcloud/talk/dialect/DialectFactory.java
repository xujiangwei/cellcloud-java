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

package net.cellcloud.talk.dialect;

import net.cellcloud.core.Cellet;

/**
 * 方言工厂。
 * 
 * @author Ambrose Xu
 * 
 */
public abstract class DialectFactory {

	/**
	 * 获得方言的元数据描述。
	 * 
	 * @return 返回元数据。
	 */
	abstract public DialectMetaData getMetaData();

	/**
	 * 创建方言。
	 * 
	 * @param tracker 指定方言的 Tracker 。
	 * @return
	 */
	abstract public Dialect create(String tracker);

	/**
	 * 关闭工厂。
	 */
	abstract public void shutdown();

	/**
	 * 当发送方言时此方法被回调。
	 * 
	 * @param identifier 目标 Cellet 标识。
	 * @param dialect 被发送的方言。
	 * @return 返回 <code>false</code> 表示工厂截获该方言，将不被送入发送队列。
	 */
	abstract protected boolean onTalk(String identifier, Dialect dialect);

	/**
	 * 当收到对应的方言时此方法被回调。
	 * 
	 * @param identifier 来源 Cellet 标识。
	 * @param dialect 接收到的方言。
	 * @return 返回 <code>false</code> 表示工厂截获该方言，将不调用监听器通知 dialogue 事件发生。
	 */
	abstract protected boolean onDialogue(String identifier, Dialect dialect);

	/**
	 * 当发送方言时此方法被回调。
	 * 
	 * @param cellet 当前发送此方言的 Cellet 。
	 * @param targetTag 目标终端的内核标签。
	 * @param dialect 被发送的方言。
	 * @return 返回 <code>false</code> 表示工厂截获该方言，将不被送入发送队列。
	 */
	abstract protected boolean onTalk(Cellet cellet, String targetTag, Dialect dialect);

	/**
	 * 当收到对应的方言时此方法被回调。
	 * 
	 * @param cellet 接收此方言的 Cellet 。
	 * @param sourceTag 源终端的内核标签。
	 * @param dialect 接收到的方言。
	 * @return 返回 <code>false</code> 表示工厂截获该方言，将不调用监听器通知 dialogue 事件发生。
	 */
	abstract protected boolean onDialogue(Cellet cellet, String sourceTag, Dialect dialect);

}
