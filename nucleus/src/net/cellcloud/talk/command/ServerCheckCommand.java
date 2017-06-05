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
import java.nio.charset.Charset;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.core.Nucleus;
import net.cellcloud.talk.TalkDefinition;
import net.cellcloud.talk.TalkServiceKernel;
import net.cellcloud.talk.TalkServiceKernel.Certificate;
import net.cellcloud.util.Utils;

/**
 * Talk check command
 * 
 * @author Ambrose Xu
 * 
 */
public final class ServerCheckCommand extends ServerCommand {

	/**
	 * 构造函数。
	 */
	public ServerCheckCommand(TalkServiceKernel kernel, Session session, Packet packet) {
		super(kernel, session, packet);
	}

	@Override
	public void execute() {
		// 包格式：原文

		// 获得客户端包版本
		this.session.major = this.packet.getMajorVersion();
		this.session.minor = this.packet.getMinorVersion();

		Certificate cert = this.kernel.getCertificate(this.session);
		if (null == cert) {
			return;
		}

		byte[] plaintext = this.packet.getSegment(0);
		if (null == plaintext) {
			return;
		}

		byte[] tag = this.packet.getSegment(1);

		boolean checkin = false;
		String pt = new String(plaintext, Charset.forName("UTF-8"));
		if (pt.equals(cert.plaintext)) {
			checkin = true;
		}

		StringBuilder log = new StringBuilder();
		log.append("Session (");
		log.append(this.session.getId());
		log.append(") ");
		log.append(this.session.getAddress().getAddress().getHostAddress());
		log.append(":");
		log.append(this.session.getAddress().getPort());

		if (checkin) {
			log.append(" checkin.");
			this.kernel.acceptSession(this.session, Utils.bytes2String(tag));

			// 包格式：成功码|内核标签

			// 数据打包
			Packet packet = new Packet(TalkDefinition.TPT_CHECK, 2, this.session.major, this.session.minor);
			packet.appendSegment(TalkDefinition.SC_SUCCESS);
			packet.appendSegment(Nucleus.getInstance().getTagAsString().getBytes());

			byte[] data = Packet.pack(packet);
			if (null != data) {
				Message message = new Message(data);

				try {
					this.session.write(message);
				} catch (IOException e) {
					Logger.log(this.getClass(), e, LogLevel.ERROR);
				}
			}
		}
		else {
			log.append(" checkout.");
			this.kernel.rejectSession(this.session);
		}

		Logger.d(ServerCheckCommand.class, log.toString());
		log = null;
	}

}
