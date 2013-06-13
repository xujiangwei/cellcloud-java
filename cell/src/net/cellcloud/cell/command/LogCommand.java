/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (cellcloudproject@gmail.com)

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

package net.cellcloud.cell.command;

import net.cellcloud.common.LogHandle;
import net.cellcloud.common.LogManager;

/** 日志操作命令。
 * 
 * @author Jiangwei Xu
 */
public class LogCommand extends ConsoleCommand {

	private LogHandle logHandle;
	private byte state;

	public LogCommand() {
		super("log", "Print log text on screen.", "");
		this.state = ConsoleCommand.CCS_FINISHED;
		this.logHandle = LogManager.getInstance().createSystemOutHandle();
	}

	@Override
	public byte getState() {
		return this.state;
	}

	@Override
	public void execute(String arg) {
		LogManager.getInstance().removeHandle(this.logHandle);
		LogManager.getInstance().addHandle(this.logHandle);

		this.state = ConsoleCommand.CCS_EXECUTING;
		println("Enter 'Q'/'q' to stop print log.");
		println("Start print log text:");
	}

	@Override
	public void input(String input) {
		if (input.equalsIgnoreCase("Q")) {			
			LogManager.getInstance().removeHandle(this.logHandle);

			this.state = ConsoleCommand.CCS_FINISHED;
			println("\nStop print log to console screen.");
		}
		else {
			println("[Tips] Command 'log' : enter 'Q'/'q' to stop print log.");
		}
	}
}
