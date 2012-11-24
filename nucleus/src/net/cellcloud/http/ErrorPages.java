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

package net.cellcloud.http;

/** 错误页内容。
 * 
 * @author Jiangwei Xu
 */
public final class ErrorPages {

	private static String PAGE_404;

	protected ErrorPages() {
	}

	public final static String ERROR_404() {
		return ErrorPages.PAGE_404;
	}

	protected static void build() {
		StringBuilder buf = new StringBuilder();
		buf.append("<!doctype html>\r\n");
		buf.append("<html><head>");
		buf.append("<meta charset=\"utf-8\">");
		buf.append("<title>404 - File or directory not found.</title>");
		buf.append("<style type=\"text/css\">");
		buf.append("body{margin:0;font-size:.7em;font-family:Verdana, Arial, Helvetica, sans-serif;background:#EEEEEE;}");
		buf.append("h1{font-size:2.4em;margin:0;color:#FFF;}");
		buf.append("h2{font-size:1.7em;margin:0;color:#CC0000;}"); 
		buf.append("h3{font-size:1.2em;margin:10px 0 0 0;color:#000000;}"); 
		buf.append("#fieldset{padding:0 15px 10px 15px;}");
		buf.append("#header{width:96%;margin:0 0 0 0;padding:6px 2% 6px 2%;font-family:Verdana, sans-serif;color:#FFF;background-color:#555555;}");
		buf.append("#content{margin:0 0 0 2%;position:relative;}");
		buf.append(".content-container{background:#FFF;width:96%;margin-top:8px;padding:10px;position:relative;}");
		buf.append("#footer{margin:0 0 0 0;padding:20px 0px 0px 20px;font-size:1.0em;font-style:italic;}");
		buf.append("</style></head>\r\n<body>");
		buf.append("<div id=\"header\"><h1>Server Error</h1></div>");
		buf.append("<div id=\"content\">");
		buf.append("<div class=\"content-container\">");
		buf.append("<dvi id=\"fieldset\">");
		buf.append("<h2>404 - File or directory not found.</h2>");
		buf.append("<h3>The resource you are looking for might have been removed, had its name changed, or is temporarily unavailable.</h3>");
		buf.append("</div></div></div>");
		buf.append("<div id=\"footer\">Powered by Cell Cloud</div>");
		buf.append("</body></html>\r\n");

		PAGE_404 = buf.toString();

		buf.delete(0, buf.length());
		buf = null;
	}
}
