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

package net.cellcloud.talk.http;

import net.cellcloud.http.HttpSession;
import net.cellcloud.http.SessionListener;
import net.cellcloud.talk.TalkServiceKernel;

/**
 * HTTP 会话监听器。
 * 
 * @author Ambrose Xu
 *
 */
public class HttpSessionListener implements SessionListener {

	/** Talk 服务核心。 */
	private TalkServiceKernel talkServiceKernel;

	/**
	 * 构造函数。
	 * 
	 * @param talkServiceKernel 指定 Talk 服务核心。
	 */
	public HttpSessionListener(TalkServiceKernel talkServiceKernel) {
		this.talkServiceKernel = talkServiceKernel;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(HttpSession session) {
		// Nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDestroy(HttpSession session) {
		this.talkServiceKernel.closeSession(session);
	}

}
