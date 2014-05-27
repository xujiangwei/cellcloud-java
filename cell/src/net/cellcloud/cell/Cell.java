/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2014 Cell Cloud Team (www.cellcloud.net)

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
import java.util.HashMap;
import java.util.Map;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.core.Nucleus;

/** Cell Cloud 容器。
 * 
 * @author Ambrose Xu
 */
public final class Cell {

	private static Application app = null;
	private static Thread daemon = null;
	private static boolean spinning = true;

	private static Object signal = null;

	private Cell() {
	}

	/** 启动 Cell 容器。
	 */
	public static boolean start() {
		return Cell.start(false, "cell.log");
	}

	/** 启动 Cell 容器。
	 */
	public static boolean start(final boolean console, final String logFile) {
		if (null != Cell.daemon) {
			return false;
		}

		// 实例化 App
		Cell.app = new Application(console, logFile);

		Cell.daemon = new Thread() {
			@Override
			public void run() {
				if (Cell.app.startup()) {
					Cell.app.run();
				}

				Cell.app.shutdown();
				Cell.app = null;

				if (null != Cell.signal) {
					synchronized (Cell.signal) {
						Cell.signal.notifyAll();
					}
				}
			}
		};
		Cell.daemon.setName("CellMain");
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

		if (null != Cell.signal) {
			synchronized (Cell.signal) {
				Cell.signal.notifyAll();
			}
		}

		Cell.daemon = null;
	}

	/** 阻塞当前线程直到 Cell 停止。
	 */
	public static void waitForCellStopped() {
		if (null == Cell.signal) {
			Cell.signal = new Object();
		}

		synchronized (Cell.signal) {
			try {
				Cell.signal.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/** 返回控制台。
	 */
	public static Console getConsole() {
		return Cell.app.getConsole();
	}

	private static void markStart() {
		try {
			// 处理文件
			File file = new File("bin");
			if (!file.exists()) {
				file.mkdir();
			}

			file = new File("bin/tag");
			if (file.exists()) {
				file.delete();
			}
			file.createNewFile();

			FileOutputStream fos = new FileOutputStream(file);
			fos.write(Nucleus.getInstance().getTagAsString().getBytes());
			fos.flush();
			fos.close();
		} catch (FileNotFoundException e) {
			Logger.log(Cell.class, e, LogLevel.ERROR);
		} catch (IOException e) {
			Logger.log(Cell.class, e, LogLevel.ERROR);
		}

		Thread daemon = new Thread() {
			@Override
			public void run() {

				while (spinning) {

					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						Logger.log(Cell.class, e, LogLevel.WARNING);
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
			Logger.log(Cell.class, e, LogLevel.ERROR);
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
			// 按照指定参数启停 Cell

			if (args[0].equals("start")) {
				// 解析参数
				Arguments arguments = Cell.parseArgs(args);

				Cell.app = new Application(arguments.console, arguments.logFile);

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

					System.out.println("\nStopping Cell Cloud process, please waiting...");

					long startTime = System.currentTimeMillis();

					while (true) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							Logger.log(Cell.class, e, LogLevel.INFO);
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
		else {
			System.out.print("cell> No argument");
			VersionInfo.main(args);
		}
	}

	/**
	 * 解析参数
	 * @param args
	 * @return
	 */
	private static Arguments parseArgs(String[] args) {
		Arguments ret = new Arguments();

		// -console=<true|false>
		// -log=<filename>

		if (args.length > 1) {
			HashMap<String, String> map = new HashMap<String, String>(2);
			for (String arg : args) {
				String[] array = arg.split("=");
				if (array.length == 2) {
					map.put(array[0], array[1]);
				}
			}

			// 判断是否使用交互式控制台
			for (Map.Entry<String, String> entry : map.entrySet()) {
				String name = entry.getKey();
				String value = entry.getValue();
				if (name.equals("-console")) {
					try {
						ret.console = Boolean.parseBoolean(value);
					} catch (Exception e) {
						Logger.w(Cell.class, "Cellet arguments error: -console");
					}
				}
				else if (name.equals("-log")) {
					ret.logFile = value;
				}
			}
		}

		return ret;
	}
}
