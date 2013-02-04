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

package net.cellcloud.talk;

import net.cellcloud.common.Message;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.util.Util;

/** Suspend Command
 * 
 * @author Jiangwei Xu
 */
public final class ServerSuspendCommand extends ServerCommand {

	public ServerSuspendCommand(TalkService service, Session session,
			Packet packet) {
		super(service, session, packet);
	}

	@Override
	public void execute() {
		// 包格式：源标签|有效时长

		String tag = Util.bytes2String(this.packet.getSubsegment(0));
		long duration = Long.parseLong(Util.bytes2String(this.packet.getSubsegment(1)));

		// 处理挂起
		boolean ret = this.service.processSuspend(this.session, tag, duration);

		Packet response = null;
		// 包格式：请求方标签|成功码|时间戳
		if (ret) {
			response = new Packet(TalkDefinition.TPT_SUSPEND, 5, 1, 0);
			response.appendSubsegment(this.packet.getSubsegment(0));
			response.appendSubsegment(TalkDefinition.SC_SUCCESS);
			response.appendSubsegment(Util.string2Bytes(Long.toString(System.currentTimeMillis())));
		}
		else {
			response = new Packet(TalkDefinition.TPT_SUSPEND, 5, 1, 0);
			response.appendSubsegment(this.packet.getSubsegment(0));
			response.appendSubsegment(TalkDefinition.SC_FAILURE);
			response.appendSubsegment(Util.string2Bytes(Long.toString(System.currentTimeMillis())));
		}

		byte[] data = Packet.pack(response);
		Message message = new Message(data);
		this.session.write(message);
	}
}
