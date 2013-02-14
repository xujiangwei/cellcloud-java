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

package net.cellcloud.core;

import java.util.Map;

import net.cellcloud.common.Cryptology;
import net.cellcloud.common.Session;
import net.cellcloud.util.Util;

/** 集群内数据块拉回协议。
 * 
 * @author Jiangwei Xu
 */
public class ClusterPullProtocol extends ClusterProtocol {

	public final static String NAME = "Pull";

	/// 块标签
	public final static String KEY_LABEL = "Label";
	/// 块数据
	public final static String KEY_CHUNK = "Chunk";
	/// 目标虚节点 Hash
	public final static String KEY_TARGET_HASH = "Target-Hash";

	private long targetHash = 0;
	private Chunk chunk = null;

	public ClusterPullProtocol(long targetHash, Chunk chunk) {
		super(ClusterPullProtocol.NAME);
		this.targetHash = targetHash;
		this.chunk = chunk;
	}

	public ClusterPullProtocol(Map<String, String> prop) {
		super(ClusterPullProtocol.NAME, prop);
	}

	/** 返回目标节点 Hash 。
	 */
	public long getTargetHash() {
		if (0 == this.targetHash) {
			String str = this.getProp(KEY_TARGET_HASH);
			if (null != str) {
				this.targetHash = Long.parseLong(str);
			}
		}

		return this.targetHash;
	}

	@Override
	public void launch(Session session) {
		StringBuilder buf = new StringBuilder();
		buf.append(KEY_PROTOCOL).append(": ").append(NAME).append("\n");
		buf.append(KEY_TAG).append(": ").append(Nucleus.getInstance().getTagAsString()).append("\n");
		buf.append(KEY_DATE).append(": ").append(super.getStandardDate()).append("\n");

		buf.append(KEY_TARGET_HASH).append(": ").append(this.targetHash).append("\n");
		buf.append(KEY_LABEL).append(": ").append(Cryptology.getInstance().encodeBase64(Util.string2Bytes(this.chunk.getLabel()))).append("\n");

		this.touch(session, buf);
		buf = null;
	}

	@Override
	public void respond(ClusterNode node, StateCode state) {
	}
}
