/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (www.cellcloud.net)

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

import java.util.Iterator;
import java.util.LinkedList;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.talk.dialect.DialectEnumerator;
import net.cellcloud.util.Clock;

/** Talk Service 守护线程。
 * 
 * @author Jiangwei Xu
 */
public final class TalkServiceDaemon extends Thread {

	private boolean spinning = false;
	protected boolean running = false;
	private long tickTime = 0;

	public TalkServiceDaemon() {
		super("TalkServiceDaemon");
	}

	/** 返回周期时间点。
	 */
	protected long getTickTime() {
		return this.tickTime;
	}

	@Override
	public void run() {
		this.running = true;
		this.spinning = true;

		LinkedList<Speaker> speakerList = new LinkedList<Speaker>();

		TalkService service = TalkService.getInstance();

		int heartbeatCount = 0;

		do {
			// 当前时间
			this.tickTime = Clock.currentTimeMillis();

			// 心跳计数
			++heartbeatCount;
			if (heartbeatCount >= 6000) {
				heartbeatCount = 0;
			}

			// 60 秒周期处理
			if (heartbeatCount % 60 == 0) {
				try {
					// HTTP 客户端管理，每 60 秒一次计数
					if (null != service.httpSpeakers) {
						for (HttpSpeaker speaker : service.httpSpeakers) {
							speaker.tick();
						}
					}
				} catch (Exception e) {
					Logger.log(this.getClass(), e, LogLevel.ERROR);
				}
			}

			// 1 分钟周期处理
			if (heartbeatCount % 60 == 0) {
				try {
					// 检查 HTTP Session
					service.checkHttpSessionHeartbeat();

					// 检查 Session
					service.checkSessionHeartbeat();
				} catch (Exception e) {
					Logger.log(this.getClass(), e, LogLevel.ERROR);
				}
			}

			// 5 分钟周期处理
			if (heartbeatCount % 300 == 0) {
				try {
					// 300 秒一次心跳
					if (null != service.speakers) {
						synchronized (service.speakers) {
							for (Speaker speaker : service.speakers) {
								if (speaker.heartbeat()) {
									Logger.i(TalkServiceDaemon.class, "Speaker heartbeat to " + speaker.getAddress().getHostString()
											+ ":" + speaker.getAddress().getPort());
								}
							}
						}
					}
				} catch (Exception e) {
					Logger.log(this.getClass(), e, LogLevel.ERROR);
				}
			}

			// 检查丢失连接的 Speaker
			if (null != service.speakers) {
				try {
					synchronized (service.speakers) {
						for (Speaker speaker : service.speakers) {
							if (speaker.lost
								&& null != speaker.capacity
								&& speaker.capacity.retryAttempts > 0) {
								if (speaker.retryTimestamp == 0) {
									// 建立时间戳
									speaker.retryTimestamp = this.tickTime;
									continue;
								}

								// 判断是否达到最大重试次数
								if (speaker.retryCounts >= speaker.capacity.retryAttempts) {
									if (!speaker.retryEnd) {
										speaker.retryEnd = true;
										speaker.fireRetryEnd();
									}
									continue;
								}

								// 可以进行重连尝试
								if (this.tickTime - speaker.retryTimestamp >= speaker.capacity.retryDelay) {
									speakerList.add(speaker);
								}
							}
						}
					} //#synchronized

					if (!speakerList.isEmpty()) {
						for (Speaker speaker : speakerList) {
							// 重置 identifiers
							LinkedList<String> identifiers = new LinkedList<String>();
							identifiers.addAll(speaker.getIdentifiers());

							// 挂断
							speaker.hangUp();

							// 执行 call
							if (speaker.call(identifiers)) {
								StringBuilder buf = new StringBuilder();
								buf.append("Retry call cellet '");
								buf.append(speaker.getIdentifiers().get(0));
								buf.append("' at ");
								buf.append(speaker.getAddress().getAddress().getHostAddress());
								buf.append(":");
								buf.append(speaker.getAddress().getPort());
								Logger.i(TalkServiceDaemon.class, buf.toString());
								buf = null;
							}
							else {
								StringBuilder buf = new StringBuilder();
								buf.append("Failed retry call cellet '");
								buf.append(speaker.getIdentifiers().get(0));
								buf.append("' at ");
								buf.append(speaker.getAddress().getAddress().getHostAddress());
								buf.append(":");
								buf.append(speaker.getAddress().getPort());
								Logger.w(TalkServiceDaemon.class, buf.toString());
								buf = null;
							}

							// 重连计数
							speaker.retryCounts++;
						}

						// 清空列表
						speakerList.clear();
					}
				} catch (Exception e) {
					Logger.log(this.getClass(), e, LogLevel.ERROR);
				}
			}

			try {
				// 处理未识别 Session
				service.processUnidentifiedSessions(this.tickTime);
			} catch (Exception e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			}

			// 休眠 1 秒
			try {
				long dt = Clock.currentTimeMillis() - this.tickTime;
				if (dt <= 1000) {
					dt = 1000 - dt;
				}
				else {
					dt = dt % 1000;
				}

				Thread.sleep(dt);
			} catch (InterruptedException e) {
				Logger.log(TalkServiceDaemon.class, e, LogLevel.ERROR);
			}

			Thread.yield();

		} while (this.spinning);

		// 关闭所有 Speaker
		if (null != service.speakers) {
			synchronized (service.speakers) {
				for (Speaker speaker : service.speakers) {
					speaker.hangUp();
				}
				service.speakers.clear();
			}
		}
		if (null != service.httpSpeakers) {
			Iterator<HttpSpeaker> iter = service.httpSpeakers.iterator();
			while (iter.hasNext()) {
				HttpSpeaker speaker = iter.next();
				speaker.hangUp();
			}
			service.httpSpeakers.clear();
		}

		// 关闭所有工厂
		DialectEnumerator.getInstance().shutdownAll();

		Logger.i(this.getClass(), "Talk service daemon quit.");
		this.running = false;
	}

	public void stopSpinning() {
		this.spinning = false;
	}
}
