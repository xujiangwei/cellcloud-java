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
import net.cellcloud.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Talk proxy command
 * 
 * @author Ambrose Xu
 * 
 */
public final class ServerProxyCommand extends ServerCommand {

	/**
	 * 构造函数。
	 */
	public ServerProxyCommand(TalkServiceKernel service) {
		super(service, null, null);
	}

	/**
	 * 构造函数。
	 */
	public ServerProxyCommand(TalkServiceKernel service, Session session, Packet packet) {
		super(service, session, packet);
	}

	@Override
	public void execute() {
		// 包格式：JSON数据

		byte[] data = this.packet.getSegment(0);

		String jsonString = Utils.bytes2String(data);

		String proxyTag = null;
		String targetTag = null;
		String identifier = null;
		boolean active = false;
		try {
			JSONObject json = new JSONObject(jsonString);
			proxyTag = json.getString("proxy");
			targetTag = json.getString("tag");
			identifier = json.getString("identifier");
			active = json.getBoolean("active");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		// 处理代理
		boolean ret = this.service.processProxy(proxyTag, targetTag, identifier, active);

		// 包格式：状态码|JSON数据
		Packet packet = new Packet(TalkDefinition.TPT_PROXY, 20, this.session.major, this.session.minor);
		packet.appendSegment(ret ? TalkDefinition.SC_SUCCESS : TalkDefinition.SC_FAILURE_NOCELLET);
		packet.appendSegment(data);

		// 打包数据
		byte[] response = Packet.pack(packet);
		if (null != response) {
			Message message = new Message(response);
			try {
				this.session.write(message);
			} catch (IOException e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			}
		}
	}

}
