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

package net.cellcloud.extras.memory;

import java.util.Collection;

import net.cellcloud.extras.memory.attribute.Attribute;

/** 可外现化接口。
 * 
 * @author Jiangwei Xu
 */
public interface Externalizable {

	/**
	 * 返回 GUID 。
	 */
	public long getGUID();

	/**
	 * 设置属性。
	 * @param attribute
	 */
	public void setAttribute(Attribute attribute);

	/**
	 * 返回属性。
	 * @param name
	 * @return
	 */
	public Attribute getAttribute(String name);

	/**
	 * 返回所有属性的集合。
	 * @return
	 */
	public Collection<Attribute> getAttributes();
}
