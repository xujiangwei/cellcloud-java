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

package net.cellcloud.common;

/**
 * 日志操作器。
 * 
 * @author Ambrose Xu
 * 
 */
public interface LogHandle {

	/**
	 * 获得日志句柄名称。
	 * 
	 * @return 返回日志句柄名称。
	 */
	public String getName();

	/**
	 * 记录 DEBUG 记录。
	 * 
	 * @param tag 日志标签。
	 * @param log 日志内容。
	 */
	public void logDebug(String tag, String log);

	/**
	 * 记录 INFO 记录。
	 * 
	 * @param tag 日志标签。
	 * @param log 日志内容。
	 */
	public void logInfo(String tag, String log);

	/**
	 * 记录 WARNING 记录。
	 * 
	 * @param tag 日志标签。
	 * @param log 日志内容。
	 */
	public void logWarning(String tag, String log);

	/**
	 * 记录 ERROR 记录。
	 * 
	 * @param tag 日志标签。
	 * @param log 日志内容。
	 */
	public void logError(String tag, String log);

}
