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

import java.nio.charset.Charset;

import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.core.Nucleus;
import net.cellcloud.talk.TalkCapacity;
import net.cellcloud.talk.TalkDefinition;
import net.cellcloud.talk.TalkServiceKernel;
import net.cellcloud.talk.TalkServiceKernel.Certificate;
import net.cellcloud.talk.TalkTracker;
import net.cellcloud.util.Utils;

/**
 * Talk quick command
 * 
 * @author Ambrose Xu
 * 
 */
public final class ServerQuickCommand extends ServerCommand {

	/**
	 * 构造函数。
	 */
	public ServerQuickCommand(TalkServiceKernel service) {
		super(service, null, null);
	}

	/**
	 * 构造函数。
	 */
	public ServerQuickCommand(TalkServiceKernel service, Session session, Packet packet) {
		super(service, session, packet);
	}

	@Override
	public void execute() {
		// 包格式：明文|源标签|能力描述序列化数据|CelletIdentifiers

		// 获得客户端包版本
		this.session.major = this.packet.getMajorVersion();
		this.session.minor = this.packet.getMinorVersion();

		Certificate cert = this.service.getCertificate(this.session);
		if (null == cert) {
			return;
		}

		byte[] plaintext = this.packet.getSegment(0);
		if (null == plaintext) {
			return;
		}

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

			byte[] tagBytes = this.packet.getSegment(1);
			String tag = Utils.bytes2String(tagBytes);

			// 接受 Session 连接
			this.service.acceptSession(this.session, tag);

			// 能力描述
			TalkCapacity capacity = TalkCapacity.deserialize(this.packet.getSegment(2));
			if (null == capacity) {
				Logger.w(ServerQuickCommand.class, "Error talk capacity data format: tag=" + tag);
				capacity = new TalkCapacity();
			}

			// 进行协商
			TalkCapacity ret = this.service.processConsult(this.session, tag, capacity);

			// 请求 Cellet
			boolean request = false;
			byte[][] identifiers = new byte[this.packet.numSegments() - 3][];
			for (int i = 3, size = this.packet.numSegments(); i < size; ++i) {
				byte[] identifier = this.packet.getSegment(i);

				// 请求 Cellet
				TalkTracker tracker = this.service.processRequest(this.session,
						tag, Utils.bytes2String(identifier));

				if (null != tracker) {
					request = true;
					// 设置 Cellet 的 identifier
					identifiers[i - 3] = identifier;
				}
				else {
					request = false;
					break;
				}
			}

			// 包格式：成功码|内核标签|能力描述序列化数据|CelletIdentifiers

			byte[] capdata = TalkCapacity.serialize(ret);

			// 数据打包
			Packet packet = new Packet(TalkDefinition.TPT_QUICK, 2, this.session.major, this.session.minor);
			if (request) {
				packet.appendSegment(TalkDefinition.SC_SUCCESS);
				packet.appendSegment(Nucleus.getInstance().getTagAsString().getBytes());
				packet.appendSegment(capdata);
				for (int i = 0; i < identifiers.length; ++i) {
					byte[] identifier = identifiers[i];
					if (null == identifier) {
						break;
					}

					packet.appendSegment(identifier);
				}
			}
			else {
				packet.appendSegment(TalkDefinition.SC_FAILURE_NOCELLET);
				packet.appendSegment(Nucleus.getInstance().getTagAsString().getBytes());
				packet.appendSegment(capdata);
			}

			byte[] data = Packet.pack(packet);
			if (null != data) {
				Message message = new Message(data);
				this.session.write(message);
			}
		}
		else {
			log.append(" checkout.");
			this.service.rejectSession(this.session);
		}

		Logger.d(ServerQuickCommand.class, log.toString());
		log = null;
	}

}
