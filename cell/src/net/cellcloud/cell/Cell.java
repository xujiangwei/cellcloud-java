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

package net.cellcloud.cell;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.core.Nucleus;
import net.cellcloud.util.Utils;

/** Cell Cloud 容器。
 * 
 * @author Ambrose Xu
 */
public final class Cell {

	private Application app = null;

	private Thread daemon = null;

	private Timer timer = null;

	private Object signal = null;

	private String tagFilename = null;

	protected Cell(Application app) {
		this.app = app;
	}

	public Cell() {
	}

	/** 启动 Cell 容器。
	 */
	public boolean start() {
		return this.start(false, "cell.log");
	}

	/** 启动 Cell 容器。
	 */
	public boolean start(final boolean console, final String logFile) {
		if (null != this.daemon) {
			return false;
		}

		Arguments arguments = new Arguments();
		arguments.console = console;
		arguments.logFile = logFile;

		// 实例化 App
		this.app = new Application(arguments);

		this.daemon = new Thread() {
			@Override
			public void run() {
				if (app.startup()) {
					app.run();
				}

				app.shutdown();

				if (null != signal) {
					synchronized (signal) {
						signal.notifyAll();
					}
				}
			}
		};
		this.daemon.setName("CellMain");
		this.daemon.start();

		return true;
	}

	/** 关闭 Cell 容器。
	 */
	public void stop() {
		if (null != this.app) {
			if (null != this.app.getConsole())
				this.app.getConsole().quit();

			this.app.stop();
		}

		if (null != this.signal) {
			synchronized (this.signal) {
				this.signal.notifyAll();
			}
		}

		this.daemon = null;
	}

	/** 阻塞当前线程直到 Cell 停止。
	 */
	public void waitForCellStopped() {
		if (null == this.signal) {
			this.signal = new Object();
		}

		synchronized (this.signal) {
			try {
				this.signal.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/** 返回控制台。
	 */
	public Console getConsole() {
		return this.app.getConsole();
	}

	private void markStart() {
		try {
			// 处理文件
			File file = new File("bin");
			if (!file.exists()) {
				file.mkdir();
			}

			this.tagFilename = "bin/tag_" + Nucleus.getInstance().getTalkService().getPort();

			file = new File(this.tagFilename);
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

		this.timer = new Timer("CellTimer");
		this.timer.schedule(new TimerTask() {
			@Override
			public void run() {
				tick();
			}
		}, 5000L, 5000L);
	}

	private void markStop() {
		if (null == this.tagFilename) {
			return;
		}

		File file = new File(this.tagFilename);
		try {
			file.createNewFile();

			FileOutputStream fos = new FileOutputStream(file);
			fos.write(Nucleus.getInstance().getTagAsString().getBytes());
			fos.write('\n');
			fos.write(Utils.string2Bytes(Utils.convertDateToSimpleString(new Date())));
			fos.flush();
			fos.close();
		} catch (IOException e) {
			Logger.log(Cell.class, e, LogLevel.ERROR);
		}
	}

	private void tick() {
		// 文件不存在则退出
		if (null != this.tagFilename) {
			File file = new File(this.tagFilename);
			if (!file.exists()) {
				if (null != this.timer) {
					this.timer.cancel();
					this.timer.purge();
					this.timer = null;
				}

				this.app.stop();
			}
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

				Cell cell = new Cell(new Application(arguments));

				if (cell.app.startup()) {

					cell.markStart();

					cell.app.run();
				}

				cell.app.shutdown();

				cell.markStop();

				System.out.println("\nProcess exit.");

				// 执行 exit
				System.exit(0);
			}
			else if (args[0].equals("stop")) {
				String tagFilename = null;
				if (args.length > 1) {
					tagFilename = "bin/tag_" + args[1];
				}

				ArrayList<File> fileList = new ArrayList<File>();
				// tagFilename 为 null 则表示停止所有
				if (null == tagFilename) {
					File dir = new File("bin");
					if (dir.isDirectory()) {
						File[] files = dir.listFiles();
						for (File f : files) {
							if (f.getName().startsWith("tag_") && f.length() <= 36) {
								fileList.add(f);
							}
						}
					}
				}
				else {
					File file = new File(tagFilename);
					fileList.add(file);
				}

				for (int i = 0; i < fileList.size(); ++i) {
					File file = fileList.get(i);
					if (file.exists() && file.length() <= 36) {
						// 删除文件
						file.delete();

						System.out.println("\nStopping Cell Cloud (" + file.getName() + ") process, please wait...");

						long startTime = System.currentTimeMillis();

						while (true) {
							try {
								Thread.sleep(500L);
							} catch (InterruptedException e) {
								Logger.log(Cell.class, e, LogLevel.INFO);
							}

							File testFile = new File(file.getAbsolutePath());
							if (testFile.exists()) {
								break;
							}
							else {
								if (System.currentTimeMillis() - startTime >= 20000L) {
									System.out.println("Shutdown program fail!");
									System.exit(0);
								}
							}
						}

						System.out.println("\nCell Cloud (" + file.getName() + ") process exit, progress elapsed time: " +
								Math.round((System.currentTimeMillis() - startTime)/1000.0d) + " seconds.\n");
					}
					else {
						System.out.println("Can not find Cell Cloud (" + file.getName() + ") process.");
					}
				}

				if (fileList.isEmpty()) {
					System.out.println("Can not find Cell Cloud process.");
				}
			}
			else {
				CellVersion.main(args);
			}
		}
		else {
			System.out.print("cell> No argument");
			CellVersion.main(args);
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
		// -config=<filename|none>

		if (args.length > 1) {
			HashMap<String, String> map = new HashMap<String, String>(2);
			for (String arg : args) {
				String[] array = arg.split("=");
				if (array.length == 2) {
					map.put(array[0], array[1]);
				}
			}

			for (Map.Entry<String, String> entry : map.entrySet()) {
				String name = entry.getKey();
				String value = entry.getValue();
				if (name.equals("-console")) {
					// 交互式控制台
					try {
						ret.console = Boolean.parseBoolean(value);
					} catch (Exception e) {
						Logger.w(Cell.class, "Cellet arguments error: -console");
					}
				}
				else if (name.equals("-log")) {
					// 日志文件
					ret.logFile = value;
				}
				else if (name.equals("-config")) {
					// 配置文件
					if (value.equals("none") || value.equals("no"))
						ret.confileFile = null;
					else
						ret.confileFile = value;
				}
			}
		}

		return ret;
	}
}
