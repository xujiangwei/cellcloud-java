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

package net.cellcloud.cell.command;

import net.cellcloud.core.ClusterController;
import net.cellcloud.core.ClusterNode;
import net.cellcloud.core.ClusterVirtualNode;
import net.cellcloud.core.Nucleus;

/** Cluster 命令。
 * 
 * Subcommand:<br/>
 * vn <sn> 查看指定虚拟节点数据。
 * 
 * @author Jiangwei Xu
 */
public class ClusterCommand extends ConsoleCommand {

	public ClusterCommand() {
		super("clst", "Cluster help command", "");
	}

	@Override
	public byte getState() {
		return ConsoleCommand.CCS_FINISHED;
	}

	@Override
	public boolean execute(String arg) {
		if (null != arg && arg.length() > 0) {
			// 执行子命令
			Subcommand subcmd = this.parseSubcommand(arg);
			if (subcmd.getWord().equals("vn")) {
				SubcommandVN vn = new SubcommandVN(subcmd);
				vn.execute();
			}
			else {
				this.print("This command does not support this sub command.");
			}
		}
		else {
			ClusterController cltr = Nucleus.getInstance().getClusterController();
			ClusterNode node = cltr.getNode();
			Long[] hashes = node.getVirtualNodeHashList();

			StringBuilder info = new StringBuilder("Cluster nodes: ");
			info.append(hashes.length + " virtual nodes\n");
			info.append("VNode\n");
			info.append("----------------------------------------------------------------------\n");
			int sn = 1;
			for (Long h : hashes) {
				info.append(sn).append("\t").append(h).append("\t");

				// 判断是否是本地
				info.append(node.containsOwnVirtualNode(h) ? "L" : "R");

				info.append("\n");
				++sn;
			}
			info.deleteCharAt(info.length() - 1);
			this.print(info.toString());

			info = null;
		}

		return false;
	}

	@Override
	public void input(String input) {
		
	}

	/**
	 */
	private class SubcommandVN {
		private Subcommand subcmd;

		private SubcommandVN(Subcommand subcmd) {
			this.subcmd = subcmd;
		}

		protected void execute() {
			if (this.subcmd.numOfArgs() == 0) {
				print("Warning: Cluster 'vn' command not input argument.");
			}
			else {
				if (this.subcmd.isIntArg(0)) {
					ClusterController cltr = Nucleus.getInstance().getClusterController();
					ClusterNode node = cltr.getNode();
					Long[] hashes = node.getVirtualNodeHashList();

					int sn = this.subcmd.getIntArg(0);
					if (sn >= 1 && sn <= hashes.length) {
						Long hash = hashes[sn - 1];
						StringBuilder buf = new StringBuilder("Virtual node - ").append(hash).append("\n");
						if (node.containsOwnVirtualNode(hash)) {
							ClusterVirtualNode vnode = node.getOwnVirtualNode(hash);
							buf.append("Number of memory chunk: ").append(vnode.numOfChunk()).append("\n");
						}
						else {
							// TODO
						}

						print(buf.toString());
						buf = null;
					}
					else {
						print("Warning: Cluster 'vn' command argument <sn> error.");
					}
				}
				else {
					print("Warning: Cluster 'vn' command argument error.");
				}
			}
		}
	}
}
