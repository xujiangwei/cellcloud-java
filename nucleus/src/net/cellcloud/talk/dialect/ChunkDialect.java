/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2016 Cell Cloud Team (www.cellcloud.net)

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

package net.cellcloud.talk.dialect;

import java.util.List;

import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.stuff.SubjectStuff;

/** 块数据方言。
 * 
 * @author Jiangwei Xu
 */
public class ChunkDialect extends Dialect {

	public final static String DIALECT_NAME = "ChunkDialect";
	public final static int CHUNK_SIZE = 2048;

	protected String sign = null;
	protected int chunkIndex = 0;
	protected int chunkNum = 0;
	protected byte[] data = null;
	protected int length = 0;
	protected long totalLength = 0;

	// 用于标识该区块是否能写入缓存队列
	// 如果为 true ，表示已经“污染”，不能进入队列，必须直接发送
	protected boolean infectant = false;

	private ChunkListener listener;

	private int readIndex = 0;

	protected ChunkDialect() {
		super(ChunkDialect.DIALECT_NAME);
	}

	public ChunkDialect(String tracker) {
		super(ChunkDialect.DIALECT_NAME, tracker);
	}

	public ChunkDialect(String sign, long totalLength, int chunkIndex, int chunkNum, byte[] data, int length) {
		super(ChunkDialect.DIALECT_NAME);
		this.sign = sign;
		this.totalLength = totalLength;
		this.chunkIndex = chunkIndex;
		this.chunkNum = chunkNum;
		this.data = new byte[length];
		System.arraycopy(data, 0, this.data, 0, length);
		this.length = length;
	}

	public ChunkDialect(String tracker, String sign, long totalLength, int chunkIndex, int chunkNum, byte[] data, int length) {
		super(ChunkDialect.DIALECT_NAME, tracker);
		this.sign = sign;
		this.totalLength = totalLength;
		this.chunkIndex = chunkIndex;
		this.chunkNum = chunkNum;
		this.data = new byte[length];
		System.arraycopy(data, 0, this.data, 0, length);
		this.length = length;
	}

	public String getSign() {
		return this.sign;
	}

	public long getTotalLength() {
		return this.totalLength;
	}

	public int getChunkIndex() {
		return this.chunkIndex;
	}

	public int getChunkNum() {
		return this.chunkNum;
	}

	public int getLength() {
		return this.length;
	}

	public void setListener(ChunkListener listener) {
		this.listener = listener;
	}

	protected void fireProgress(String target) {
		if (null != this.listener) {
			this.listener.onProgress(target, this);
			if (this.chunkIndex + 1 < this.chunkNum) {
				this.listener = null;
			}
		}
	}

	protected void fireCompleted(String target) {
		if (null != this.listener) {
			this.listener.onCompleted(target, this);
			this.listener = null;
		}
	}

	protected void fireFailed(String target) {
		if (null != this.listener) {
			this.listener.onFailed(target, this);
			this.listener = null;
		}
	}

	@Override
	public Primitive translate() {
		Primitive primitive = new Primitive(this);

		primitive.commit(new SubjectStuff(this.sign));
		primitive.commit(new SubjectStuff(this.chunkIndex));
		primitive.commit(new SubjectStuff(this.chunkNum));
		primitive.commit(new SubjectStuff(this.data));
		primitive.commit(new SubjectStuff(this.length));
		primitive.commit(new SubjectStuff(this.totalLength));

		return primitive;
	}

	@Override
	public void build(Primitive primitive) {
		List<SubjectStuff> list = primitive.subjects();
		this.sign = list.get(0).getValueAsString();
		this.chunkIndex = list.get(1).getValueAsInt();
		this.chunkNum = list.get(2).getValueAsInt();
		this.data = list.get(3).getValue();
		this.length = list.get(4).getValueAsInt();
		this.totalLength = list.get(5).getValueAsLong();
	}

	@Override
	public boolean equals(Object obj) {
		if (null != obj && obj instanceof ChunkDialect) {
			ChunkDialect cd = (ChunkDialect) obj;
			if (null != this.sign && null != cd.sign && cd.sign.equals(this.sign)
				&& cd.chunkIndex == this.chunkIndex) {
				return true;
			}
		}

		return false;
	}

	public boolean hasCompleted() {
		ChunkDialectFactory fact = (ChunkDialectFactory) DialectEnumerator.getInstance().getFactory(ChunkDialect.DIALECT_NAME);
		return fact.checkCompleted(this.sign);
	}

	protected boolean isLast() {
		return (this.chunkIndex + 1 == this.chunkNum);
	}

	public int read(int index, byte[] buffer) {
		ChunkDialectFactory fact = (ChunkDialectFactory) DialectEnumerator.getInstance().getFactory(ChunkDialect.DIALECT_NAME);
		return fact.read(this.sign, index, buffer);
	}

	public int read(byte[] buffer) {
		if (this.readIndex >= this.chunkNum) {
			return -1;
		}

		ChunkDialectFactory fact = (ChunkDialectFactory) DialectEnumerator.getInstance().getFactory(ChunkDialect.DIALECT_NAME);
		int length = fact.read(this.sign, this.readIndex, buffer);
		++this.readIndex;
		return length;
	}

	public void resetRead() {
		this.readIndex = 0;
	}

	public void clearAll() {
		ChunkDialectFactory fact = (ChunkDialectFactory) DialectEnumerator.getInstance().getFactory(ChunkDialect.DIALECT_NAME);
		fact.clear(this.sign);
	}
}
