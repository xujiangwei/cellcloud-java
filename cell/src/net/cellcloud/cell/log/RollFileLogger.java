/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2016 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.cell.log;

import java.util.concurrent.atomic.AtomicBoolean;

import net.cellcloud.util.Clock;


public class RollFileLogger extends FileLogger {

	// 文件大小门限，单位：字节
	private long thresholdSize = 12 * 1024 * 1024;

	// 文件记录时长门限，单位：毫秒
	private long thresholdExpired = 24 * 60 * 60 * 1000;

	private AtomicBoolean rolling = new AtomicBoolean(false);

	public RollFileLogger(String name) {
		super(name);
	}

	public void setMaxSize(long size) {
		this.thresholdSize = size;
	}

	/**
	 * 
	 * @param expired 单位：秒
	 */
	public void setExpired(long expired) {
		this.thresholdExpired = expired * 1000;
	}

	@Override
	public void logDebug(String tag, String log) {
		if (this.rolling.get()) {
			return;
		}

		super.logDebug(tag, log);
		this.rolling();
	}

	@Override
	public void logInfo(String tag, String log) {
		if (this.rolling.get()) {
			return;
		}

		super.logInfo(tag, log);
		this.rolling();
	}

	@Override
	public void logWarning(String tag, String log) {
		if (this.rolling.get()) {
			return;
		}

		super.logWarning(tag, log);
		this.rolling();
	}

	@Override
	public void logError(String tag, String log) {
		if (this.rolling.get()) {
			return;
		}

		super.logError(tag, log);
		this.rolling();
	}

	private void rolling() {
		if (this.rolling.get()) {
			return;
		}

		// 先达到条件者即处理
		if (this.byteSum >= this.thresholdSize
			|| Clock.currentTimeMillis() - this.created >= this.thresholdExpired) {

			this.rolling.set(true);

			// 关闭当前日志
			this.close();

			// 启动新日志文件
			this.open(this.filename);

			this.rolling.set(false);
		}
	}
}
