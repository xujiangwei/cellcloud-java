/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.core;

import net.cellcloud.talk.Primitive;

/** 抽象 Cellet 单元。
 * 
 * @author Jiangwei Xu
 */
public abstract class AbstractCellet {

	public AbstractCellet() {
	}

	/** Cellet 激活时调用该方法。
	 */
	public abstract void activate();

	/** Cellet 销毁时调用该方法。
	 */
	public abstract void deactivate();

	/** Talk 会话回调。
	 * 
	 * @param tag 对端的内核标签。
	 */
	public abstract void dialogue(final String tag, final Primitive primitive);

	/** 当消费者连接服务时回调此方法。
	 * 
	 * @param tag 对端的内核标签。
	 */
	public abstract void contacted(final String tag);

	/** 当消费者退出服务时回调此方法。
	 * 
	 * @param tag 对端的内核标签。
	 */
	public abstract void quitted(final String tag);
}
