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

import net.cellcloud.common.Logger;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.core.Endpoint;
import net.cellcloud.core.Role;
import net.cellcloud.gateway.GatewayService;
import net.cellcloud.gateway.Hostlink;
import net.cellcloud.talk.TalkServiceKernel;
import net.cellcloud.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public class ServerProxyInfoCommand extends ServerCommand {

	public ServerProxyInfoCommand(TalkServiceKernel service) {
		super(service, null, null);
	}

	public ServerProxyInfoCommand(TalkServiceKernel service, Session session, Packet packet) {
		super(service, session, packet);
	}

	@Override
	public void execute() {
		// 包格式：JSON数据
		
		byte[] data = this.packet.getSegment(0);

		String jsonString = Utils.bytes2String(data);

		String proxyTag = null;
		String info = null;

		String targetTag = null;
		String address = null;
		int port = 0;

		try {
			JSONObject json = new JSONObject(jsonString);
			proxyTag = json.getString("proxy");
			info = json.getString("info");
			if (info.equals(GatewayService.PROXY_INFO_ENDPOINT)) {
				targetTag = json.getString("tag");
				address = json.getString("address");
				port = json.getInt("port");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (null == proxyTag) {
			Logger.e(this.getClass(), "Packet JSON format error");
			return;
		}

		if (info.equals(GatewayService.PROXY_INFO_ENDPOINT)) {
			Hostlink hostlink = this.kernel.getHostlink();
			if (null != hostlink) {
				String tag = targetTag.toString();
				hostlink.addEnpoint(tag, new Endpoint(tag, Role.CONSUMER, address, port));
			}
			else {
				Logger.w(this.getClass(), "Hostlink is null.");
			}
		}
	}

}
