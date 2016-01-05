/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2015 Cell Cloud Team (www.cellcloud.net)

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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Service;

public final class FlashSocketSecurity implements Service {

	private ServerSocket socket;
	private boolean stopped;

	private StringBuilder policy;

	public FlashSocketSecurity() {
		this.stopped = false;

		this.policy = new StringBuilder("<cross-domain-policy>");
		this.policy.append("<allow-access-from domain=\"*\" to-ports=\"*\" />");
		this.policy.append("</cross-domain-policy>\0");
	}

	@Override
	public boolean startup() {
		try {
			this.socket = new ServerSocket(8430);
			this.socket.setSoTimeout(5000);
		} catch (IOException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
			return false;
		}

		Thread thread = new Thread(){
			@Override
			public void run() {
				while (!stopped) {
					try {
						Socket client = socket.accept();

						if (null == client || client.isClosed()) {
							continue;
						}

						InputStreamReader input = new InputStreamReader(client.getInputStream(), "UTF-8");
						BufferedReader reader = new BufferedReader(input);

						OutputStreamWriter output = new OutputStreamWriter(client.getOutputStream(), "UTF-8");
						BufferedWriter writer = new BufferedWriter(output);

						StringBuilder data = new StringBuilder();
						int c = 0;
						while ((c = reader.read()) != -1) {
							if (c != '\0') {
								data.append((char) c);
							}
							else {
								break;
							}
						}

						String info = data.toString();
						Logger.d(FlashSocketSecurity.class, "Flash socket cross domain policy : " + client.getInetAddress().getHostAddress() + " -> " + info);

						if (info.indexOf("<policy-file-request/>") >= 0) {
							writer.write(policy.toString());
							writer.flush();
							Logger.d(FlashSocketSecurity.class, "Security policy response : " + client.getInetAddress().getHostAddress());
						}
						else {
							writer.write("Cube\0");
							Logger.w(FlashSocketSecurity.class, "Unknown request : " + client.getInetAddress().getHostAddress());
						}

						client.close();
						data = null;
					} catch (java.net.SocketTimeoutException e) {
						// Nothing
					} catch (IOException e) {
						Logger.log(FlashSocketSecurity.class, e, LogLevel.INFO);
					}
				}
			}
		};
		thread.start();

		return true;
	}

	@Override
	public void shutdown() {
		this.stopped = true;

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// Nothing
		}

		Thread.yield();

		if (null != this.socket) {
			try {
				this.socket.close();
			} catch (IOException e) {
				Logger.log(FlashSocketSecurity.class, e, LogLevel.DEBUG);
			}
		}
	}
}
