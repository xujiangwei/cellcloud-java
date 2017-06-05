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
import net.cellcloud.talk.TalkCapacity;
import net.cellcloud.talk.TalkDefinition;
import net.cellcloud.talk.TalkServiceKernel;
import net.cellcloud.util.Utils;

/**
 * Talk consult Command
 * 
 * @author Ambrose Xu
 * 
 */
public final class ServerConsultCommand extends ServerCommand {

	/**
	 * 构造函数。
	 */
	public ServerConsultCommand(TalkServiceKernel kernel, Session session, Packet packet) {
		super(kernel, session, packet);
	}

	@Override
	public void execute() {
		// 包格式：源标签|能力描述序列化数据

		// 标签
		String tag = Utils.bytes2String(this.packet.getSegment(0));

		// 能力描述
		TalkCapacity capacity = TalkCapacity.deserialize(this.packet.getSegment(1));

		if (null == capacity) {
			Logger.w(ServerConsultCommand.class, "Error talk capacity data format: tag=" + tag);
			capacity = new TalkCapacity();
		}

		TalkCapacity ret = this.kernel.processConsult(this.session, tag, capacity);

		// 应答		
		// 包格式：源标签|能力描述序列化数据

		byte[] capdata = TalkCapacity.serialize(ret);

		Packet response = new Packet(TalkDefinition.TPT_CONSULT, 4, this.session.major, this.session.minor);
		response.appendSegment(this.packet.getSegment(0));
		response.appendSegment(capdata);

		byte[] data = Packet.pack(response);
		if (null != data) {
			Message message = new Message(data);
			try {
				this.session.write(message);
			} catch (IOException e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			}
		}
	}

}
