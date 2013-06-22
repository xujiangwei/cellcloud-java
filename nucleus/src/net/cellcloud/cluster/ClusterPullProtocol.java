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

package net.cellcloud.cluster;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import net.cellcloud.common.Cryptology;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.common.Session;
import net.cellcloud.core.Nucleus;
import net.cellcloud.util.Utils;

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
	private String chunkLabel = null;
	private Chunk chunk = null;

	public ClusterPullProtocol(long targetHash, String chunkLabel) {
		super(ClusterPullProtocol.NAME);
		this.targetHash = targetHash;
		this.chunkLabel = chunkLabel;
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

	/** 返回 Chunk 标签。
	 */
	public String getChunkLabel() {
		if (null == this.chunkLabel) {
			this.chunkLabel = this.getProp(KEY_LABEL);
		}

		return this.chunkLabel;
	}

	/** 返回数据块。
	 */
	public void setChunk(Chunk chunk) {
		this.chunk = chunk;
	}

	/** 返回数据块。
	 */
	public Chunk getChunk() {
		if (null == this.chunk) {
			String strLabel = this.getProp(KEY_LABEL);
			String strChunk = this.getProp(KEY_CHUNK);
			if (null != strLabel && null != strChunk) {
				String label = Utils.bytes2String(Cryptology.getInstance().decodeBase64(strLabel));
				String jsChunk = Utils.bytes2String(Cryptology.getInstance().decodeBase64(strChunk));
				JSONObject json = null;
				try {
					json = new JSONObject(jsChunk);
				} catch (JSONException e) {
					Logger.log(ClusterPullProtocol.class, e, LogLevel.ERROR);
				}
				if (null != json) {
					this.chunk = new Chunk(label, json);
				}
			}
		}

		return this.chunk;
	}

	@Override
	public void launch(Session session) {
		StringBuilder buf = new StringBuilder();
		buf.append(KEY_PROTOCOL).append(": ").append(NAME).append("\n");
		buf.append(KEY_TAG).append(": ").append(Nucleus.getInstance().getTagAsString()).append("\n");
		buf.append(KEY_DATE).append(": ").append(super.getStandardDate()).append("\n");

		buf.append(KEY_TARGET_HASH).append(": ").append(this.targetHash).append("\n");
		buf.append(KEY_LABEL).append(": ").append(Cryptology.getInstance().encodeBase64(Utils.string2Bytes(this.chunkLabel))).append("\n");

		this.touch(session, buf);
		buf = null;
	}

	@Override
	public void respond(ClusterNode node, StateCode state) {
		StringBuilder buf = new StringBuilder();
		buf.append(KEY_PROTOCOL).append(": ").append(NAME).append("\n");
		buf.append(KEY_TAG).append(": ").append(Nucleus.getInstance().getTagAsString()).append("\n");
		buf.append(KEY_DATE).append(": ").append(super.getStandardDate()).append("\n");
		buf.append(KEY_STATE).append(": ").append(state.getCode()).append("\n");

		if (null != this.chunk) {
			buf.append(KEY_LABEL).append(": ").append(Cryptology.getInstance().encodeBase64(Utils.string2Bytes(this.chunk.getLabel()))).append("\n");
			buf.append(KEY_CHUNK).append(": ").append(Cryptology.getInstance().encodeBase64(Utils.string2Bytes(this.chunk.getData().toString()))).append("\n");
		}
		else {
			buf.append(KEY_LABEL).append(": ").append(Cryptology.getInstance().encodeBase64(Utils.string2Bytes(this.chunkLabel))).append("\n");
		}

		this.touch(this.contextSession, buf);
		buf = null;
	}
}
