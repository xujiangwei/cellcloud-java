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

import java.util.concurrent.ExecutorService;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Message;
import net.cellcloud.common.MessageInterceptor;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.core.Nucleus;
import net.cellcloud.gateway.GatewayService.Slave;
import net.cellcloud.gateway.RoutingTable.Record;
import net.cellcloud.talk.TalkDefinition;

import org.json.JSONException;
import org.json.JSONObject;

public class ProxyForwarder implements MessageInterceptor {

	private RoutingTable routingTable;

	private ExecutorService executor;

	public ProxyForwarder(RoutingTable routingTable, ExecutorService executor) {
		this.routingTable = routingTable;
		this.executor = executor;
	}

	@Override
	public boolean interceptCreating(Session session) {
		// Nothing
		return false;
	}

	@Override
	public boolean interceptDestroying(Session session) {
		// Nothing
		return false;
	}

	@Override
	public boolean interceptOpening(Session session) {
		// Nothing
		return false;
	}

	@Override
	public boolean interceptClosing(final Session session) {
		this.executor.execute(new Runnable() {
			@Override
			public void run() {
				// 会话被销毁
				Slave slave = routingTable.querySlave(session);
				if (null != slave) {
					// 移除路由信息
					Record record = routingTable.remove(session);

					for (int i = 0; i < record.runtimeIdentifiers.size(); ++i) {
						JSONObject proxy = new JSONObject();
						try {
							proxy.put("proxy", Nucleus.getInstance().getTagAsString());
							proxy.put("sid", session.getId().longValue());
							proxy.put("tag", record.tag);
							proxy.put("identifier", record.runtimeIdentifiers.get(i));
							proxy.put("active", false);
						} catch (JSONException e) {
							Logger.log(ProxyForwarder.class, e, LogLevel.WARNING);
						}

						// 通过代理协议关闭路由
						slave.kernel.proxy(slave.celletIdentifiers.get(0), proxy);
					}

					// 从下位机移除
					slave.removeSession(record.tag);
				}
			}
		});

		return false;
	}

	@Override
	public boolean interceptMessage(final Session session, Message message) {
		final byte[] data = message.get();
		try {
			Packet packet = Packet.unpack(data);
			if (null != packet) {
				byte[] ptag = packet.getTag();

				// 拦截对话包
				if (TalkDefinition.isDialogue(ptag)) {
					// 执行发送任务
					this.executor.execute(new Runnable() {
						@Override
						public void run() {
							Slave slave = routingTable.querySlave(session);
							if (null != slave) {
								if (!slave.kernel.pass(slave.celletIdentifiers.get(0), data)) {
									Logger.w(ProxyForwarder.class,
											"Pass dialogue data failed, cellet identifier: " + slave.celletIdentifiers.get(0));
								}
							}
							else {
								Logger.w(ProxyForwarder.class,
										"Can NOT find routing info for session '" + session.getAddress().getHostString());
							}
						}
					});

					return true;
				}
			}
		} catch (NumberFormatException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		} catch (ArrayIndexOutOfBoundsException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		}

		return false;
	}

	@Override
	public boolean interceptError(Session session, int errorCode) {
		// Nothing
		return false;
	}

}
