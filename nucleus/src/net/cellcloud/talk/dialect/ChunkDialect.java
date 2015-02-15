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
import net.cellcloud.talk.stuff.SubjectStuff;

/** 块数据方言。
 * 
 * @author Jiangwei Xu
 *
 */
public class ChunkDialect extends Dialect {

	public final static String DIALECT_NAME = "ChunkDialect";

	protected String sign;
	protected int chunkIndex;
	protected int chunkNum;
	protected byte[] data;
	protected int length;

	public ChunkDialect() {
		super(ChunkDialect.DIALECT_NAME);
	}

	public ChunkDialect(String tracker) {
		super(ChunkDialect.DIALECT_NAME, tracker);
	}

	public ChunkDialect(String sign, int chunkIndex, int chunkNum, byte[] data, int length) {
		super(ChunkDialect.DIALECT_NAME);
		this.sign = sign;
		this.chunkIndex = chunkIndex;
		this.chunkNum = chunkNum;
		this.data = data;
		this.length = length;
	}

	public String getSign() {
		return this.sign;
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

	@Override
	public Primitive translate() {
		Primitive primitive = new Primitive(this);

		synchronized (this) {
			primitive.commit(new SubjectStuff(this.sign));
			primitive.commit(new SubjectStuff(this.chunkIndex));
			primitive.commit(new SubjectStuff(this.chunkNum));
			primitive.commit(new SubjectStuff(Base64.encodeBytes(this.data)));
			primitive.commit(new SubjectStuff(this.length));
		}

		return primitive;
	}

	@Override
	public void build(Primitive primitive) {
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

		if (null != this.data) {
			ChunkDialectFactory fact = (ChunkDialectFactory) DialectEnumerator.getInstance().getFactory(ChunkDialect.DIALECT_NAME);
			fact.write(this);
		}
	}

	public boolean hasCompleted() {
		ChunkDialectFactory fact = (ChunkDialectFactory) DialectEnumerator.getInstance().getFactory(ChunkDialect.DIALECT_NAME);
		return fact.checkCompleted(this.sign);
	}

	public int read(int index, byte[] buffer) {
		ChunkDialectFactory fact = (ChunkDialectFactory) DialectEnumerator.getInstance().getFactory(ChunkDialect.DIALECT_NAME);
		return fact.read(this.sign, index, buffer);
	}
}
