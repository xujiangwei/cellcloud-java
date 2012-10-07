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

/** Cell Cloud 容器。
 * 
 * @author Ambrose Xu
 */
public final class Cell {

	private static Application app = null;
	private static Thread daemon = null;

	private Cell() {
	}

	/** 启动 Cell 容器。
	 */
	public static boolean start() {
		if (null != Cell.daemon) {
			return false;
		}

		Cell.daemon = new Thread() {
			@Override
			public void run() {
				Cell.app = new Application(false);

				if (Cell.app.startup()) {
					Cell.app.run();
				}

				Cell.app.shutdown();
				Cell.app = null;
			}
		};
		Cell.daemon.setName("CellAppMain");
		Cell.daemon.start();

		return true;
	}

	/** 关闭 Cell 容器。
	 */
	public static void stop() {
		if (null != Cell.app) {
			if (null != Cell.app.getConsole())
				Cell.app.getConsole().quit();

			Cell.app.stop();
		}

		Cell.daemon = null;
	}

	/** 默认主函数。
	 */
	public static void main(String[] args) {
		Cell.app = new Application(true);

		if (Cell.app.startup()) {
			Cell.app.run();
		}

		Cell.app.shutdown();
		Cell.app = null;
	}
}
