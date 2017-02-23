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

package net.cellcloud.gateway;

import net.cellcloud.common.Logger;
import net.cellcloud.core.Cellet;
import net.cellcloud.gateway.GatewayService.Slave;
import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.TalkFailureCode;
import net.cellcloud.talk.TalkListener;
import net.cellcloud.talk.TalkServiceFailure;
import net.cellcloud.talk.speaker.Speaker;
import net.cellcloud.talk.speaker.SpeakerProxyListener;

import org.json.JSONObject;

public class ProxyTalkListener implements TalkListener, SpeakerProxyListener {

	private GatewayService gateway;
	private Slave slave;

	public ProxyTalkListener(GatewayService gateway, Slave slave) {
		this.gateway = gateway;
		this.slave = slave;
	}

	@Override
	public void dialogue(String identifier, Primitive primitive) {
		// 代理模式下该方法不会被调用
		// Nothing
	}

	@Override
	public void contacted(String identifier, String tag) {
		Logger.d(this.getClass(), "contacted: " + identifier);

		this.gateway.addOnlineSlave(this.slave);
	}

	@Override
	public void quitted(String identifier, String tag) {
		Logger.d(this.getClass(), "quitted: " + identifier);

		this.gateway.removeOnlineSlave(this.slave);
	}

	@Override
	public void failed(String tag, TalkServiceFailure failure) {
		Logger.d(this.getClass(), "failed: " + failure.getHost() + ":" + failure.getPort());

		if (failure.getCode() == TalkFailureCode.PROXY_FAILED) {
			synchronized (this.slave.kernel) {
				this.slave.kernel.notifyAll();
			}
			return;
		}

		this.gateway.removeOnlineSlave(this.slave);
	}

	@Override
	public void onProxy(Speaker speaker, JSONObject data) {
		Logger.d(this.getClass(), "onProxy: " + speaker.getRemoteTag());

		synchronized (this.slave.kernel) {
			this.slave.kernel.notifyAll();
		}
	}

	@Override
	public void onProxyDialogue(String targetTag, String celletIdentifier, Primitive primitive) {
//		Logger.d(this.getClass(), "onProxyDialogue: " + targetTag);

		// 查找指定的 Cellet
		Cellet cellet = this.gateway.getCellet(celletIdentifier);
		if (null == cellet) {
			Logger.e(this.getClass(), "Can NOT find cellet: " + celletIdentifier);
			return;
		}

		// 操控 Cellet 发送数据
		boolean ret = this.gateway.serverKernel.notice(targetTag, primitive, cellet, this.gateway.getSandbox(celletIdentifier));
		if (!ret) {
			Logger.w(this.getClass(), "Talk kernel notici failed, target:'" + targetTag + "', cellet:'" + celletIdentifier + "'");
		}
	}

}
