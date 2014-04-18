/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2013 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.storage;

import net.cellcloud.exception.StorageException;

/** 结果集。
 * 
 * @author Jiangwei Xu
 */
public interface ResultSet {

	/** 游标绝对定位。
	*/
	boolean absolute(int cursor);

	/** 游标相对定位。
	*/
	boolean relative(int cursor);

	/** 游标移动到数据头。
	*/
	boolean first();

	/** 游标移动到数据尾。
	*/
	boolean last();

	/** 游标下移一个数据位。
	*/
	boolean next();

	/** 游标上移一个数据位。
	*/
	boolean previous();

	/** 游标是否在第一个数据位。
	*/
	boolean isFirst();

	/** 游标是否在最后一个数据位。
	*/
	boolean isLast();

	/** 返回指定游标处字符型数据。
	*/
	char getChar(int index);

	/** 返回指定游标标签处字符型数据。
	*/
	char getChar(final String label);

	/** 返回指定游标处整数型数据。
	*/
	int getInt(int index);

	/** 返回指定游标标签处整数型数据。
	*/
	int getInt(final String label);

	/** 返回指定游标处长整数型数据。
	*/
	long getLong(int index);

	/** 返回指定游标标签处长整数型数据。
	*/
	long getLong(final String label);

	/** 返回指定游标处字符串型数据。
	*/
	String getString(int index);

	/** 返回指定游标标签处字符串型数据。
	*/
	String getString(final String label);

	/** 返回指定游标处布尔型数据。
	*/
	boolean getBool(int index);

	/** 返回指定游标标签处布尔型数据。
	*/
	boolean getBool(final String label);

	/** 获取原始数据。
	@return 返回数据长度。
	*/
	byte[] getRaw(final String label, long offset, long length);

	/** 更新指定索引处字符值。
	*/
	void updateChar(int index, char value);

	/** 更新指定标签处字符值。
	*/
	void updateChar(final String label, char value);

	/** 更新指定索引处整数值。
	*/
	void updateInt(int index, int value);

	/** 更新指定标签处整数值。
	*/
	void updateInt(final String label, int value);

	/** 更新指定索引处的长整型值。
	*/
	void updateLong(int index, long value);

	/** 更新指定标签处的长整型值。
	*/
	void updateLong(final String label, long value);

	/** 更新指定索引处的字符串型值。
	*/
	void updateString(int index, final String value);

	/** 更新指定标签处的字符串型值。
	*/
	void updateString(final String label, final String value);

	/** 更新指定索引处的布尔型值。
	*/
	void updateBool(int index, boolean value);

	/** 更新指定标签处的布尔型值。
	*/
	void updateBool(final String label, boolean value);

	/** 更新指定标签处的原始数据值。
	*/
	void updateRaw(final String label, byte[] src, int offset, int length)
			throws StorageException;

	/** 更新指定标签处的原始数据值。
	*/
	void updateRaw(final String label, byte[] src, long offset, long length)
			throws StorageException;

	/** 关闭结果集。
	 */
	void close();
}
