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

import net.cellcloud.Version;

/**
 * Cell 容器的版本信息。
 * 
 * @author Ambrose Xu
 * 
 */
public final class CellVersion {

	public final static int MAJOR = 1;
	public final static int MINOR = 6;
	public final static int REVISION = 6;

	public static void main(String[] args) {
		StringBuilder buf = new StringBuilder();
		buf.append("\n-----------------------------------------------------------------------\n");
		buf.append("Cell Application version ");
		buf.append(CellVersion.MAJOR);
		buf.append(".");
		buf.append(CellVersion.MINOR);
		buf.append(".");
		buf.append(CellVersion.REVISION);
		buf.append("\n");

		buf.append("Nucleus version ");
		buf.append(Version.MAJOR);
		buf.append(".");
		buf.append(Version.MINOR);
		buf.append(".");
		buf.append(Version.REVISION);
		buf.append(" (Build Java - ");
		buf.append(Version.NAME);
		buf.append(")\n");

		buf.append(" ___ ___ __  __     ___ __  ___ _ _ ___\n");
		buf.append("| __| __| | | |    | __| | |   | | | _ \\\n");
		buf.append("| |_| _|| |_| |_   | |_| |_| | | | | | |\n");
		buf.append("|___|___|___|___|  |___|___|___|___|___/\n\n");

		buf.append("Copyright (c) 2009,2017 Cell Cloud Team, www.cellcloud.net\n");
		buf.append("-----------------------------------------------------------------------");

		System.out.println(buf);

		buf = null;
	}
}
