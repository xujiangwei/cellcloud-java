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

package net.cellcloud.core;

/** 系统通用日志接口。
 * 
 * @author Jiangwei Xu
 */
public final class Logger {

	/** 打印 DEBUG 级别日志。
	 */
	public static void d(Class<?> clazz, String log) {
		LoggerManager.getInstance().log(LoggerManager.DEBUG, clazz.getName(), log);
	}

	/** 打印 INFO 级别日志。
	 */
	public static void i(Class<?> clazz, String log) {
		LoggerManager.getInstance().log(LoggerManager.INFO, clazz.getName(), log);
	}

	/** 打印 WARNING 级别日志。
	 */
	public static void w(Class<?> clazz, String log) {
		LoggerManager.getInstance().log(LoggerManager.WARNING, clazz.getName(), log);
	}

	/** 打印 ERROR 级别日志。
	 */
	public static void e(Class<?> clazz, String log) {
		LoggerManager.getInstance().log(LoggerManager.ERROR, clazz.getName(), log);
	}
}
