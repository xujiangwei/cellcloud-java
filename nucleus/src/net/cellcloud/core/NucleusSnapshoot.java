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

package net.cellcloud.core;

import java.util.List;

import net.cellcloud.talk.TalkSnapshoot;

/**
 * 内核快照。
 * 
 * @author Ambrose Xu
 * 
 */
public final class NucleusSnapshoot {

	/** 内核标签。 */
	public String tag;

	/** 本次快照的时间戳。 */
	public long timestamp;

	/** JVM 最大可用内存。 */
	public long maxMemory;

	/** JVM 已使用的系统内存。 */
	public long totalMemory;

	/** JVM 空闲的系统内存。 */
	public long freeMemory;

	/** 内核启动时间。 */
	public long systemStartTime;

	/** 截止本次快照生成时内核运行的时长，单位：毫秒。 */
	public long systemDuration;

	/** 内核的 Cellet 列表。 */
	public List<String> celletList;

	/** Talk 服务快照。 */
	public TalkSnapshoot talk;

	/**
	 * 构造函数。
	 */
	protected NucleusSnapshoot() {
		this.timestamp = System.currentTimeMillis();
		this.maxMemory = Runtime.getRuntime().maxMemory();
		this.totalMemory = Runtime.getRuntime().totalMemory();
		this.freeMemory = Runtime.getRuntime().freeMemory();
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("Talk port: ").append(talk.port).append("\n");
		return buf.toString();
	}

}
