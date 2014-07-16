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

package net.cellcloud.storage.ehcache;

import net.cellcloud.util.Properties;
import net.cellcloud.util.StringProperty;

/** 缓存属性。
 * 
 * @author Jiangwei Xu
 */
public final class CacheProperties extends Properties {

	public final static String CACHE_XML = "EhcacheXml";
	
	public CacheProperties() {
	}
	
	/** 添加ehcache配置文件路径，默认文件名为：ehcache.xml。
	 */
	public void setEhcacheXmlPath(String xmlPath){
		if (null == xmlPath)
		{
			xmlPath =  this.getClass().getResource("/").getPath() + "ehcache.xml";
		}
		StringProperty cachePro = new StringProperty(CACHE_XML, xmlPath);
		this.addProperty(cachePro);

	}
}
