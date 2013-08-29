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

import net.cellcloud.cell.Console;

/** 退出命令。
 * 
 * @author Jiangwei Xu
 */
public final class ExitCommand extends ConsoleCommand {

	private Console console;
	private byte state;

	public ExitCommand(Console console) {
		super("exit", "Exit cell console and quit program.", "");
		this.state = ConsoleCommand.CCS_FINISHED;
		this.console = console;
	}

	@Override
	public byte getState() {
		return this.state;
	}

	@Override
	public void execute(String arg) {
		if (null != arg && arg.length() > 0) {
			print("This command does not support this argument.");
			this.state = ConsoleCommand.CCS_FINISHED;
			return;
		}

		this.state = ConsoleCommand.CCS_EXECUTING;
		print("Are you sure exit cell console and quit? [y/n] ");
	}

	@Override
	public void input(String input) {
		if (input.equalsIgnoreCase("Y")) {
			println("\nShutdown, please wait...");
			this.console.quit();
			this.console.getApp().stop();
		}
		else {
			this.state = ConsoleCommand.CCS_FINISHED;
		}
	}
}
