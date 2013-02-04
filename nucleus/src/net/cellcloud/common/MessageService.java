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

package net.cellcloud.common;

/** 消息服务。
 * 
 * @author Jiangwei Xu
 */
public abstract class MessageService {

	protected MessageHandler handler;
	protected MessageInterceptor interceptor;
	private byte[] headMark;
	private byte[] tailMark;
	private int maxConnectNum;

	public MessageService() {
		this.handler = null;
		this.interceptor = null;
		this.headMark = null;
		this.tailMark = null;
		this.maxConnectNum = 32;
	}

	/** 返回消息句柄。
	 */
	public MessageHandler getHandler() {
		return this.handler;
	}

	/** 设置消息句柄。
	 */
	public void setHandler(MessageHandler handler) {
		this.handler = handler;
	}

	/** 返回消息拦截器。
	 */
	public MessageInterceptor getInterceptor() {
		return this.interceptor;
	}

	/** 设置消息拦截器。
	 */
	public void setInterceptor(MessageInterceptor interceptor) {
		this.interceptor = interceptor;
	}

	/** 定义消息传输时使用的数据掩码。
	 * 默认无掩码。
	 */
	public void defineDataMark(byte[] headMark, byte[] tailMark) {
		this.headMark = headMark;
		this.tailMark = tailMark;
	}

	/** 返回该服务使用使用了数据掩码。
	 */
	public boolean existDataMark() {
		return (null != this.headMark && null != this.tailMark);
	}

	/** 返回数据头掩码。
	 */
	public byte[] getHeadMark() {
		return this.headMark;
	}

	/** 返回数据尾掩码。
	 */
	public byte[] getTailMark() {
		return this.tailMark;
	}

	/** 设置最大连接数。
	 */
	public void setMaxConnectNum(int num) {
		this.maxConnectNum = num;
	}

	/** 返回最大连接数。
	 */
	public int getMaxConnectNum() {
		return this.maxConnectNum;
	}

	/** 写入消息数据。 */
	public abstract void write(Session session, Message message);

	/** 读取消息数据。 */
	public abstract void read(Message message, Session session);
}
