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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import net.cellcloud.core.LogLevel;
import net.cellcloud.core.Logger;
import net.cellcloud.core.Nucleus;

/** Cell Cloud 容器。
 * 
 * @author Ambrose Xu
 */
public final class Cell {

	private static Application app = null;
	private static Thread daemon = null;
	private static boolean spinning = true;

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

	private static void markStart() {
		try {
			// 处理文件
			File file = new File("bin/tag");
			if (file.exists()) {
				file.delete();
			}
			file.createNewFile();

			FileOutputStream fos = new FileOutputStream(file);
			fos.write(Nucleus.getInstance().getTagAsString().getBytes());
			fos.flush();
			fos.close();
		} catch (FileNotFoundException e) {
			Logger.logException(e, LogLevel.ERROR);
		} catch (IOException e) {
			Logger.logException(e, LogLevel.ERROR);
		}

		Thread daemon = new Thread() {
			@Override
			public void run() {

				while (spinning) {

					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						Logger.logException(e, LogLevel.WARNING);
					}

					Cell.tick();
				}
			}
		};
		daemon.start();
	}

	private static void markStop() {
		File file = new File("bin/tag");
		try {
			file.createNewFile();

			FileOutputStream fos = new FileOutputStream(file);
			fos.write(Nucleus.getInstance().getTagAsString().getBytes());
			fos.flush();
			fos.close();
		} catch (IOException e) {
			Logger.logException(e, LogLevel.ERROR);
		}
	}

	private static void tick() {
		// 文件不存在则退出
		File file = new File("bin/tag");
		if (!file.exists()) {
			Cell.spinning = false; 
			Cell.app.stop();
		}
	}

	/** 默认主函数。
	 */
	public static void main(String[] args) {
		if (null != args && args.length > 0) {
			if (args[0].equals("start")) {
				Cell.app = new Application(true);

				if (Cell.app.startup()) {

					Cell.markStart();

					Cell.app.run();
				}

				Cell.spinning = false;

				Cell.app.shutdown();

				Cell.markStop();

				Cell.app = null;

				System.out.println("\nProcess exit.");
			}
			else if (args[0].equals("stop")) {
				File file = new File("bin/tag");
				if (file.exists()) {
					file.delete();

					System.out.println("\nStopping Cell Cloud application, please waiting...");

					long startTime = System.currentTimeMillis();

					while (true) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							Logger.logException(e, LogLevel.INFO);
						}

						File testFile = new File("bin/tag");
						if (testFile.exists()) {
							break;
						}
						else {
							if (System.currentTimeMillis() - startTime >= 30000) {
								System.out.println("Shutdown program fail!");
								System.exit(0);
							}
						}
					}

					System.out.println("\nCell Cloud process exit, progress elapsed time " +
							(int)((System.currentTimeMillis() - startTime)/1000) + " seconds.\n");
				}
			}
			else {
				VersionInfo.main(args);
			}
		}
	}
}