/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2015 Cell Cloud Team (www.cellcloud.net)

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

import java.io.IOException;
import java.util.List;

import net.cellcloud.common.Base64;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.stuff.PredicateStuff;
import net.cellcloud.talk.stuff.SubjectStuff;

/*! 块数据方言。
 * 
 * \author Jiangwei Xu
 */
public class ChunkDialect extends Dialect {

	public final static String DIALECT_NAME = "ChunkDialect";
	public static int CHUNK_SIZE = 4096;

	protected boolean ack = false;

	protected String sign;
	protected int chunkIndex;
	protected int chunkNum;
	protected byte[] data;
	protected int length;
	protected long totalLength;

	// 用于标识该区块是否能写入缓存队列
	// 如果为 true ，表示已经“污染”，不能进入队列，必须直接发送
	protected boolean infectant = false;

	private ChunkListener listener;

	private int readIndex = 0;

	public ChunkDialect() {
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

	protected void setAck(String sign, int chunkIndex, int chunkNum) {
		this.sign = sign;
		this.chunkIndex = chunkIndex;
		this.chunkNum = chunkNum;
		this.ack = true;
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
			this.listener = null;
		}
	}

	@Override
	public Primitive translate() {
		Primitive primitive = new Primitive(this);
		primitive.commit(new PredicateStuff(this.ack));

		if (this.ack) {
			primitive.commit(new SubjectStuff(this.sign));
			primitive.commit(new SubjectStuff(this.chunkIndex));
			primitive.commit(new SubjectStuff(this.chunkNum));
		}
		else {
			primitive.commit(new SubjectStuff(this.sign));
			primitive.commit(new SubjectStuff(this.chunkIndex));
			primitive.commit(new SubjectStuff(this.chunkNum));
			primitive.commit(new SubjectStuff(Base64.encodeBytes(this.data)));
			primitive.commit(new SubjectStuff(this.length));
			primitive.commit(new SubjectStuff(this.totalLength));
		}

		return primitive;
	}

	@Override
	public void build(Primitive primitive) {
		this.ack = primitive.predicates().get(0).getValueAsBool();

		if (this.ack) {
			List<SubjectStuff> list = primitive.subjects();
			this.sign = list.get(0).getValueAsString();
			this.chunkIndex = list.get(1).getValueAsInt();
			this.chunkNum = list.get(2).getValueAsInt();
		}
		else {
			List<SubjectStuff> list = primitive.subjects();
			this.sign = list.get(0).getValueAsString();
			this.chunkIndex = list.get(1).getValueAsInt();
			this.chunkNum = list.get(2).getValueAsInt();
			try {
				this.data = Base64.decode(list.get(3).getValueAsString());
			} catch (IOException e) {
				Logger.log(ChunkDialect.class, e, LogLevel.ERROR);
			}
			this.length = list.get(4).getValueAsInt();
			this.totalLength = list.get(5).getValueAsLong();

			if (null != this.data) {
				// 数据直接写入
				ChunkDialectFactory fact = (ChunkDialectFactory) DialectEnumerator.getInstance().getFactory(ChunkDialect.DIALECT_NAME);
				fact.write(this);
			}
		}
	}

	public boolean hasCompleted() {
		ChunkDialectFactory fact = (ChunkDialectFactory) DialectEnumerator.getInstance().getFactory(ChunkDialect.DIALECT_NAME);
		return fact.checkCompleted(this.getOwnerTag(), this.sign);
	}

	public int read(int index, byte[] buffer) {
		ChunkDialectFactory fact = (ChunkDialectFactory) DialectEnumerator.getInstance().getFactory(ChunkDialect.DIALECT_NAME);
		return fact.read(this.getOwnerTag(), this.sign, index, buffer);
	}

	public int read(byte[] buffer) {
		if (this.readIndex >= this.chunkNum) {
			return -1;
		}

		ChunkDialectFactory fact = (ChunkDialectFactory) DialectEnumerator.getInstance().getFactory(ChunkDialect.DIALECT_NAME);
		int length = fact.read(this.getOwnerTag(), this.sign, this.readIndex, buffer);
		++this.readIndex;
		return length;
	}

	public void resetRead() {
		this.readIndex = 0;
	}

	public void clearAll() {
		ChunkDialectFactory fact = (ChunkDialectFactory) DialectEnumerator.getInstance().getFactory(ChunkDialect.DIALECT_NAME);
		fact.clear(this.getOwnerTag(), this.sign);
	}
}
