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

package net.cellcloud.talk.command;

import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.talk.TalkServiceKernel;

/**
 * 会话命令。
 * 
 * @author Ambrose Xu
 *
 */
public abstract class ServerCommand {

	/** 命令调用的会话服务内核。 */
	protected TalkServiceKernel kernel;
	/** 此命令关联的 Session 。 */
	public Session session;
	/** 此命令需处理的包。 */
	public Packet packet;

	/**
	 * 构造函数。
	 * 
	 * @param service 指定会话服务核心。
	 * @param session 指定命令关联的 Session 。
	 * @param packet 指定命令处理的数据包。
	 */
	public ServerCommand(TalkServiceKernel kernel, Session session, Packet packet) {
		this.kernel = kernel;
		this.session = session;
		this.packet = packet;
	}

	/**
	 * 执行命令。
	 */
	public abstract void execute();

}
