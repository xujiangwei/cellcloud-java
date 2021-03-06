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

import java.io.ByteArrayInputStream;

import net.cellcloud.common.Logger;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.TalkServiceKernel;
import net.cellcloud.util.Utils;

/**
 * Talk Dialogue Command
 * 
 * @author Ambrose Xu
 * 
 */
public final class ServerDialogueCommand extends ServerCommand {

	/**
	 * 构造函数。
	 */
	public ServerDialogueCommand(TalkServiceKernel kernel) {
		super(kernel, null, null);
	}

	/**
	 * 构造函数。
	 */
	public ServerDialogueCommand(TalkServiceKernel kernel, Session session, Packet packet) {
		super(kernel, session, packet);
	}

	@Override
	public void execute() {
		// 包格式：序列化的原语|源标签|Cellet

		if (this.packet.numSegments() < 2) {
			Logger.e(ServerDialogueCommand.class, "Dialogue packet format error");
			return;
		}

		byte[] priData = this.packet.getSegment(0);
		ByteArrayInputStream stream = new ByteArrayInputStream(priData);

		byte[] tagData = this.packet.getSegment(1);
		String speakerTag = Utils.bytes2String(tagData);

		byte[] identifierData = this.packet.getSegment(2);

		// 反序列化原语
		Primitive primitive = new Primitive(speakerTag);
		primitive.read(stream);

		this.kernel.processDialogue(this.session, speakerTag, Utils.bytes2String(identifierData), primitive);
	}

}
