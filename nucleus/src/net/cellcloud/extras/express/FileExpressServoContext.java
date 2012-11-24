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

package net.cellcloud.extras.express;

import java.util.concurrent.ConcurrentHashMap;

import net.cellcloud.exception.StorageException;
import net.cellcloud.storage.ResultSet;
import net.cellcloud.storage.Storage;

/** 文件快递上下文。
 * 
 * @author Jiangwei Xu
 */
public final class FileExpressServoContext extends FileExpressContext {

	private Storage storage;
	private long timestamp;

	private ConcurrentHashMap<String, FileAttribute> attributes;

	public FileExpressServoContext(ExpressAuthCode authCode, Storage storage) {
		super(authCode);
		this.storage = storage;
		this.timestamp = System.currentTimeMillis();
		this.attributes = new ConcurrentHashMap<String, FileAttribute>();
	}

	public long getRemainingTime() {
		if (ExpressAuthCode.DURATION_NONE == this.authCode.getDuration()) {
			// 返回大于 0 的数表示该上下文继续有效
			return 1;
		}

		return this.authCode.getDuration() - (System.currentTimeMillis() - this.timestamp);
	}

	/** 返回指定文件属性。
	 */
	public FileAttribute getAttribute(final String filename) {
		FileAttribute attr = this.attributes.get(filename);
		if (null == attr) {
			ResultSet rs = null;
			try {
				rs = this.storage.store(this.getAuthCode().getContextPath() + filename);
			} catch (StorageException e) {
				e.printStackTrace();
			}
			rs.close();
		}

		return attr;
	}

	/** 返回指定文件的上下文。
	 */
	public FileExpressContext getContext(final String filename) {
		// TODO
		return null;
	}

	/** 关闭指定文件。
	 */
	public void closeFile(final String filename) {
		// TODO
	}
}
