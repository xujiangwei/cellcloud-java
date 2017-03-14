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

package net.cellcloud.util.nio.secure;

import net.cellcloud.util.nio.Socket;

/**
 * Task listeners listen for {@link javax.net.ssl.SSLEngine} task completions
 * and act upon them.
 * As such, task listeners are only useful for {@link SecureSocket} implementations.
 * Only one task listener is associated with each {@link SecureSocket} instance.
 */
public interface TaskListener {

	/**
	 * This method is called from the {@link TaskWorker} thread once it has
	 * finished processing all {@link javax.net.ssl.SSLEngine} tasks associated
	 * with a {@link SecureSocket} instance.
	 * 
	 * @param socket
	 *            The Socket that just had one (or more) SSLEngine task(s)
	 *            completed
	 * @see SecureSocket
	 */
	public void taskComplete(Socket socket);

}
