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

import java.io.ByteArrayInputStream;

import net.cellcloud.common.Logger;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.util.Utils;

/**
 * 
 * Dialogue Command
 * 
 * @author Jiangwei Xu
 */
public final class ServerDialogueCommand extends ServerCommand {

	protected ServerDialogueCommand(TalkService service) {
		super(service, null, null);
	}

	public ServerDialogueCommand(TalkService service, Session session, Packet packet) {
		super(service, session, packet);
	}

	@Override
	public void execute() {
		// 包格式：序列化的原语|源标签

		if (this.packet.getSubsegmentCount() < 2) {
			Logger.e(ServerDialogueCommand.class, "Dialogue packet format error");
			return;
		}

		byte[] priData = this.packet.getSubsegment(0);
		ByteArrayInputStream stream = new ByteArrayInputStream(priData);

		byte[] tagData = this.packet.getSubsegment(1);
		String speakerTag = Utils.bytes2String(tagData);

		byte[] identifierData = this.packet.getSubsegment(2);

		// 反序列化原语
		Primitive primitive = new Primitive(speakerTag);
		primitive.read(stream);

		this.service.processDialogue(this.session, speakerTag, Utils.bytes2String(identifierData), primitive);
	}

}
