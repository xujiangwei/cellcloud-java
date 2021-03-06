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

import java.io.IOException;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.talk.TalkDefinition;
import net.cellcloud.talk.TalkServiceKernel;

/**
 * Talk heartbeat command
 * 
 * @author Ambrose Xu
 * 
 */
public final class ServerHeartbeatCommand extends ServerCommand {

	/**
	 * 构造函数。
	 */
	public ServerHeartbeatCommand(TalkServiceKernel service) {
		super(service, null, null);
	}

	/**
	 * 构造函数。
	 */
	public ServerHeartbeatCommand(TalkServiceKernel service, Session session, Packet packet) {
		super(service, session, packet);
	}

	@Override
	public void execute() {
		// 更新时间戳成功，回送心跳包
		if (this.kernel.updateSessionHeartbeat(this.session)) {
			Packet packet = new Packet(TalkDefinition.TPT_HEARTBEAT, 9, this.session.major, this.session.minor);
			byte[] data = Packet.pack(packet);
			Message message = new Message(data);
			try {
				this.session.write(message);
			} catch (IOException e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			}
		}
	}

}
