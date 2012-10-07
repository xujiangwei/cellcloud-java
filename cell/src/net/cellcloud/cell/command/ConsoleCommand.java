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

import java.util.Comparator;

/** 抽象控制台命令类。
 * 
 * @author Jiangwei Xu
 */
public abstract class ConsoleCommand {

	public static final byte CCS_EXECUTING = 1;
	public static final byte CCS_FINISHED = 2;

	private static final CommandComparator cmdComparator = new CommandComparator();

	/// 命令名
	protected String name;
	/// 命令描述
	protected String desc;
	/// 命令用法
	protected String usage;

	public ConsoleCommand(String name, String desc, String usage) {
		this.name = name;
		this.desc = desc;
		this.usage = usage;
	}

	/** 返回命令名。
	*/
	public String getName() {
		return this.name;
	}

	/** 返回命令描述信息。
	*/
	public String getDesc() {
		return this.desc;
	}

	/** 返回命令使用说明。
	*/
	public String getUsage() {
		return this.usage;
	}

	/** 向终端打印指定信息。
	*/
	public void print(String text) {
		System.out.print(text);
	}

	/** 向终端打印指定信息，并将游标移动到下一行。
	*/
	public void println(String text) {
		System.out.println(text);
	}

	/** 返回命令状态。
	 */
	public abstract byte getState();

	/** 执行命令。
	 */
	public abstract boolean execute(String arg);

	/** 处理交互输入信息。
	 */
	public abstract void input(String input);

	/** 返回命令比较器。
	 */
	public static CommandComparator getComparator() {
		return cmdComparator;
	}

	/** 命令比较器。
	 */
	public static class CommandComparator implements Comparator<ConsoleCommand> {
		public CommandComparator() {
		}

		@Override
		public int compare(ConsoleCommand o1, ConsoleCommand o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}
}
