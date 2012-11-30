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

package net.cellcloud.cell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import net.cellcloud.cell.command.ConsoleCommand;
import net.cellcloud.cell.command.ExitCommand;
import net.cellcloud.cell.command.HelpCommand;
import net.cellcloud.cell.command.LogCommand;
import net.cellcloud.cell.command.UnknownCommand;

/** 控制台。
 * 
 * @author Ambrose Xu
 */
public final class Console {

	private Application app;
	private Scanner scanner;
	private boolean quitted;
	private String prompt;
	private ConsoleCommand currentCommand;
	private UnknownCommand unknownCommand;

	private HashMap<String, ConsoleCommand> commands;

	public Console(Application app) {
		this.app = app;
		this.scanner = new Scanner(System.in);
		this.quitted = false;
		this.prompt = "cell~> ";
		this.currentCommand = null;
		this.unknownCommand = new UnknownCommand();
		this.commands = new HashMap<String, ConsoleCommand>();

		StringBuilder buf = new StringBuilder();
		buf.append("Enter the 'help' for more information.\n\n");
		buf.append(this.prompt);
		System.out.print(buf.toString());
		buf = null;

		registerBuildInCommands();
	}

	public Application getApp() {
		return this.app;
	}

	public boolean processInput() {
		String input = null;
		try {
			input = this.scanner.nextLine();
		} catch (Exception e) {
			// Nothing
		}

		if (null == input) {
			return true;
		}

		if (null == this.currentCommand) {
			if (input.isEmpty()) {
				System.out.print(this.prompt);
			}
			else {
				String args = null;
				this.currentCommand = findCommand(args, input);
				if (null == this.currentCommand) {
					this.currentCommand = this.unknownCommand;
				}

				if (!this.currentCommand.execute(args)) {
					System.out.println();
				}

				if (ConsoleCommand.CCS_FINISHED == this.currentCommand.getState()) {
					System.out.print("\n" + this.prompt);
					this.currentCommand = null;
				}
			}
		}
		else {
			this.currentCommand.input(input);

			if (ConsoleCommand.CCS_FINISHED == this.currentCommand.getState()) {
				System.out.print("\n" + this.prompt);
				this.currentCommand = null;
			}
		}

		return !this.quitted;
	}

	/** 退出控制台。
	 */
	public void quit() {
		this.quitted = true;
	}

	protected void quitAndClose() {
		this.quitted = true;

		try {
			System.in.close();
		} catch (IOException e) {
			// Nothing
		}

		this.scanner.close();
	}

	/** 注册控制台命令。
	 */
	public boolean registerCommand(ConsoleCommand command) {
		if (this.commands.containsKey(command.getName())) {
			return false;
		}

		this.commands.put(command.getName(), command);
		return true;
	}

	/** 返回控制台里的所有命令。
	 */
	public List<ConsoleCommand> getCommands() {
		ArrayList<ConsoleCommand> list = new ArrayList<ConsoleCommand>();
		Iterator<ConsoleCommand> iter = this.commands.values().iterator();
		while (iter.hasNext()) {
			list.add(iter.next());
		}
		Collections.sort(list, ConsoleCommand.getComparator());
		return list;
	}

	/** 查找命令
	 */
	private ConsoleCommand findCommand(String outArgs, String cmd) {
		String word = cmd;
		int index = cmd.indexOf(" ");
		if (index > 0) {
			word = cmd.substring(0, index);
			outArgs = cmd.substring(index + 1, cmd.length());
		}

		return this.commands.get(word);
	}

	/** 注册内置命令。
	 */
	private void registerBuildInCommands() {
		ConsoleCommand cmd = new HelpCommand(this);
		this.registerCommand(cmd);

		cmd = new ExitCommand(this);
		this.registerCommand(cmd);

		cmd = new LogCommand();
		this.registerCommand(cmd);
	}
}
