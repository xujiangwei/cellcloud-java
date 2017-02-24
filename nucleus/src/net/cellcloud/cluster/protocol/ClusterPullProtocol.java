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

package net.cellcloud.cluster.protocol;

import java.util.Map;

import net.cellcloud.cluster.Chunk;
import net.cellcloud.cluster.ClusterNode;
import net.cellcloud.common.Cryptology;
import net.cellcloud.common.Session;
import net.cellcloud.core.Nucleus;
import net.cellcloud.util.Utils;

/**
 * 集群内数据块拉取协议。
 * 
 * @author Ambrose Xu
 * 
 */
public class ClusterPullProtocol extends ClusterProtocol {

	/**
	 * 协议名。
	 */
	public final static String NAME = "Pull";

	/**
	 * 数据键：块标签。
	 */
	public final static String KEY_LABEL = "Label";

	/**
	 * 数据键：目标虚节点 Hash 值。
	 */
	public final static String KEY_TARGET_HASH = "Target-Hash";

	/** 目标虚拟节点 Hash 值。 */
	private long targetHash = 0;
	/** 数据块标签。 */
	private String chunkLabel = null;
	/** 数据块。 */
	private Chunk chunk = null;

	/**
	 * 构造函数。
	 * 
	 * @param targetHash 指定目标虚拟节点 Hash 值。
	 * @param chunkLabel 指定拉取数据的标签。
	 */
	public ClusterPullProtocol(long targetHash, String chunkLabel) {
		super(ClusterPullProtocol.NAME);
		this.targetHash = targetHash;
		this.chunkLabel = chunkLabel;
	}

	/**
	 * 构造函数。
	 * 
	 * @param prop 指定协议的参数映射。
	 */
	public ClusterPullProtocol(Map<String, Object> prop) {
		super(ClusterPullProtocol.NAME, prop);
	}

	/**
	 * 获得拉取目标节点的 Hash 值。
	 * 
	 * @return 返回目标节点 Hash 值。
	 */
	public long getTargetHash() {
		if (0 == this.targetHash) {
			Object value = this.getProp(KEY_TARGET_HASH);
			if (null != value) {
				this.targetHash = Long.parseLong(value.toString());
			}
		}

		return this.targetHash;
	}

	/**
	 * 获得拉取区块的标签值。
	 * 
	 * @return 返回 Chunk 标签。
	 */
	public String getChunkLabel() {
		if (null == this.chunkLabel) {
			Object value = this.getProp(KEY_LABEL);
			if (null != value) {
				this.chunkLabel = Utils.bytes2String(Cryptology.getInstance().decodeBase64(value.toString()));
			}
		}

		if (null == this.chunkLabel && null != this.chunk) {
			this.chunkLabel = this.chunk.getLabel();
		}

		return this.chunkLabel;
	}

	/**
	 * 设置数据块。
	 * 
	 * @param chunk 指定待设置区块。
	 */
	public void setChunk(Chunk chunk) {
		this.chunk = chunk;
	}

	/**
	 * 获得数据块对象实例。
	 * 
	 * @return 返回数据块。
	 */
	public synchronized Chunk getChunk() {
		if (null == this.chunk) {
			Object vLabel = this.getProp(KEY_LABEL);
			Object vData = this.getProp(KEY_PAYLOAD);
			if (null != vLabel && null != vData) {
				String label = Utils.bytes2String(Cryptology.getInstance().decodeBase64(vLabel.toString()));
				byte[] data = (byte[]) vData;
				this.chunk = new Chunk(label, data);
			}
		}

		return this.chunk;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void launch(Session session) {
		StringBuilder buf = new StringBuilder();
		buf.append(KEY_PROTOCOL).append(":").append(NAME).append("\n");
		buf.append(KEY_TAG).append(":").append(Nucleus.getInstance().getTagAsString()).append("\n");
		buf.append(KEY_DATE).append(":").append(super.getStandardDate()).append("\n");

		buf.append(KEY_TARGET_HASH).append(":").append(this.targetHash).append("\n");
		buf.append(KEY_LABEL).append(":").append(Cryptology.getInstance().encodeBase64(Utils.string2Bytes(this.chunkLabel))).append("\n");

		this.touch(session, buf, null);
		buf = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void respond(ClusterNode node, StateCode state, Object custom) {
		StringBuilder buf = new StringBuilder();
		buf.append(KEY_PROTOCOL).append(":").append(NAME).append("\n");
		buf.append(KEY_TAG).append(":").append(Nucleus.getInstance().getTagAsString()).append("\n");
		buf.append(KEY_DATE).append(":").append(super.getStandardDate()).append("\n");
		buf.append(KEY_STATE).append(":").append(state.getCode()).append("\n");

		byte[] payload = null;
		if (null != this.chunk) {
			buf.append(KEY_LABEL).append(":").append(Cryptology.getInstance().encodeBase64(Utils.string2Bytes(this.chunk.getLabel()))).append("\n");
			payload = this.chunk.getData();
		}
		else if (null != custom) {
			buf.append(KEY_LABEL).append(":").append(Cryptology.getInstance().encodeBase64(Utils.string2Bytes(custom.toString()))).append("\n");
		}

		this.touch(this.contextSession, buf, payload);
		buf = null;
	}

}
