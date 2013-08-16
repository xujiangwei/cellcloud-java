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

package net.cellcloud.cell.command;

import java.util.List;

import net.cellcloud.core.CelletFeature;
import net.cellcloud.core.Nucleus;

/**
 * Cellet 命令。
 * 
 * @author Jiangwei Xu
 */
public final class CelletCommand extends ConsoleCommand {

	public CelletCommand() {
		super("cellet", "Cellet command", "");
	}

	@Override
	public byte getState() {
		return ConsoleCommand.CCS_FINISHED;
	}

	@Override
	public void execute(String arg) {
		List<CelletFeature> list = Nucleus.getInstance().getCelletFeatures();
		this.println("Local cellets:");
		for (CelletFeature cf : list) {
			this.println(cf.getIdentifier() + " (v" + cf.getVersion().toString() + ")");
		}
		this.print("Total: " + list.size());
	}

	@Override
	public void input(String input) {
		// Nothing
	}
}
