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

import java.util.regex.Pattern;

/** 子命令。
 * 
 * @author Jiangwei Xu
 */
public final class Subcommand {

	private String cmdWord;
	private String[] args;

	public Subcommand(String cmdWord, String[] args) {
		this.cmdWord = cmdWord;
		this.args = args;
	}

	/** 返回命令字。
	 */
	public String getWord() {
		return this.cmdWord;
	}

	/** 返回参数个数。
	 */
	public int numOfArgs() {
		return (null != this.args) ? this.args.length : 0;
	}

	/** 返回字符串参数。
	 */
	public String getStringArg(int index) {
		return this.args[index];
	}

	/** 指定索引处是否是整数型参数。
	 */
	public boolean isIntArg(int index) {
		if (null == this.args) {
			return false;
		}

		Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
		return pattern.matcher(this.args[index]).matches();
	}

	/** 返回整数型参数。
	 */
	public int getIntArg(int index) {
		return Integer.parseInt(this.args[index]);
	}
}
