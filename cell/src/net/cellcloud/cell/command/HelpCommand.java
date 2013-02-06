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

import java.util.Iterator;
import java.util.List;

import net.cellcloud.cell.Console;

/** 控制台帮助命令。
 * 
 * @author Jiangwei Xu
 */
public final class HelpCommand extends ConsoleCommand {

	private Console console;

	public HelpCommand(Console console) {
		super("help", "Print cell console help document.", "");
		this.console = console;
	}

	@Override
	public byte getState() {
		return ConsoleCommand.CCS_FINISHED;
	}

	@Override
	public void execute(String arg) {
		StringBuilder buf = new StringBuilder();
		List<ConsoleCommand> list = this.console.getCommands();
		Iterator<ConsoleCommand> iter = list.iterator();
		while (iter.hasNext()) {
			ConsoleCommand cmd = iter.next();
			buf.append(cmd.getName());
			buf.append(blankSpace(cmd.getName()));
			buf.append(cmd.getDesc());
			buf.append("\n");
		}

		buf.deleteCharAt(buf.length() - 1);
		print(buf.toString());
		buf = null;
	}

	@Override
	public void input(String input) {
	}

	private String blankSpace(String cmdName) {
		final int space = 15;
		int num = space - cmdName.length();
		String bs = "";
		for (int i = 0; i < num; ++i) {
			bs += " ";
		}
		return bs;
	}
}
