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

package net.cellcloud.util.nio.timeout;

import net.cellcloud.util.nio.Socket;

/**
 * Timeout listeners listen for expired
 * {@link net.cellcloud.util.nio.timeout.Timeout}s and act upon them.
 * Only one timeout listener is associated with each
 * {@link net.cellcloud.util.nio.timeout.Timeout} instance.
 */
public interface TimeoutListener {

	/**
	 * This method is called from the
	 * {@link net.cellcloud.util.nio.timeout.Timeout#expired()}
	 * method, notifying any Timeout Listeners that the Timeout has expired and
	 * they should act upon it.
	 * 
	 * @param socket
	 *            The Socket that just had its Timeout expired
	 */
	public void timeoutExpired(Socket socket);

}
