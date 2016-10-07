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

package net.cellcloud.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * @author Jiangwei Xu
 */
public final class Clock {

	private static final Clock instance = new Clock();

	private long start;

	private Timer timer;

	private AtomicLong time;

	private Clock() {
		this.start = System.currentTimeMillis();
		this.time = new AtomicLong(this.start);
	}

	private void startTimer() {
		this.timer = new Timer("ClockTimer");
		this.timer.scheduleAtFixedRate(new ClockTask(), 1000, 200);
	}

	private void stopTimer() {
		if (null != this.timer) {
			this.timer.cancel();
			this.timer.purge();
			this.timer = null;
		}
	}

	public static long startTime() {
		return Clock.instance.start;
	}

	public static void start() {
		Clock.instance.startTimer();
	}

	public static void stop() {
		Clock.instance.stopTimer();
	}

	public static long currentTimeMillis() {
		return Clock.instance.time.get();
	}

	private class ClockTask extends TimerTask {
		private ClockTask() {
		}

		@Override
		public void run() {
			time.set(System.currentTimeMillis());
		}
	}
}
