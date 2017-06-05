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

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.core.Cellet;
import net.cellcloud.core.Nucleus;
import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.TalkServiceKernel;
import net.cellcloud.talk.stuff.PrimitiveSerializer;
import net.cellcloud.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Talk proxy dialogue response command.
 * 
 * @author Ambrose Xu
 * 
 */
public final class ServerProxyDialogueResponseCommand extends ServerCommand {

	/**
	 * 构造函数。
	 */
	public ServerProxyDialogueResponseCommand(TalkServiceKernel kernel) {
		super(kernel, null, null);
	}

	/**
	 * 构造函数。
	 */
	public ServerProxyDialogueResponseCommand(TalkServiceKernel kernel, Session session, Packet packet) {
		super(kernel, session, packet);
	}

	@Override
	public void execute() {
		// 包格式：JSON数据

		byte[] data = packet.getSegment(0);

		String jsonstr = Utils.bytes2String(data);

		String tag = null;
		String identifier = null;
		int failure = 0;
		Primitive primitive = new Primitive();
		try {
			JSONObject json = new JSONObject(jsonstr);
			tag = json.getString("tag");
			identifier = json.getString("identifier");
			failure = json.getInt("failure");
			JSONObject pjson = json.getJSONObject("primitive");
			PrimitiveSerializer.read(primitive, pjson);
		} catch (JSONException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
		}

		Cellet cellet = Nucleus.getInstance().getCellet(identifier);
		if (null != cellet) {
			cellet.failed(tag, failure, primitive);
		}
	}

}
