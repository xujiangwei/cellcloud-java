/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.storage.file;

import net.cellcloud.storage.Storage;

/** 文件存储器接口。
 * 
 * @author Jiangwei Xu
 */
public interface FileStorage extends Storage {

	/// 文件名标签
	public final static String LABEL_STRING_FILENAME = "filename";
	/// 文件是否存在标签
	public final static String LABEL_BOOL_EXIST = "exist";
	/// 文件大小标签
	public final static String LABEL_LONG_SIZE = "size";
	/// 文件最后修改时间
	public final static String LABEL_LONG_LASTMODIFIED = "lastModified";
	/// 文件数据标签
	public final static String LABEL_RAW_DATA = "data";

	/** 创建读语句。
	 */
	public String createReadStatement(final String file);

	/** 创建写语句。
	 */
	public String createWriteStatement(final String file);
}
