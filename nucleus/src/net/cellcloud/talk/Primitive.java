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

package net.cellcloud.talk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import net.cellcloud.talk.dialect.Dialect;
import net.cellcloud.talk.stuff.AdverbialStuff;
import net.cellcloud.talk.stuff.AttributiveStuff;
import net.cellcloud.talk.stuff.ComplementStuff;
import net.cellcloud.talk.stuff.ObjectiveStuff;
import net.cellcloud.talk.stuff.PredicateStuff;
import net.cellcloud.talk.stuff.PrimitiveSerializer;
import net.cellcloud.talk.stuff.SubjectStuff;

/** 原语描述类。
 * 
 * @author Jiangwei Xu
 */
public class Primitive {

	private String ownerTag;
	private String celletIdentifier;
	private ArrayList<SubjectStuff> subjectList;
	private ArrayList<PredicateStuff> predicateList;
	private ArrayList<ObjectiveStuff> objectiveList;
	private ArrayList<AttributiveStuff> attributiveList;
	private ArrayList<AdverbialStuff> adverbialList;
	private ArrayList<ComplementStuff> complementList;

	private Dialect dialect;

	/** 构造函数。 */
	public Primitive() {
		this.ownerTag = null;
		this.celletIdentifier = null;
		this.dialect = null;
	}

	/** 构造函数。 */
	public Primitive(String ownerTag) {
		this.ownerTag = ownerTag;
		this.celletIdentifier = null;
		this.dialect = null;
	}

	/** 构造函数。 */
	public Primitive(String ownerTag, Dialect dialect) {
		this.ownerTag = ownerTag;
		this.celletIdentifier = null;
		this.dialect = dialect;
	}

	/** 返回原语所属端的标签。
	*/
	public String getOwnerTag() {
		return this.ownerTag;
	}

	/** 设置 Cellet 标识。
	 */
	protected void setCelletIdentifier(String celletIdentifier) {
		this.celletIdentifier = celletIdentifier;
	}
	/** 返回 Cellet 标识。
	 */
	public String getCelletIdentifier() {
		return this.celletIdentifier;
	}

	/** 判断是否具有方言属性。
	 */
	public boolean isDialectal() {
		return (null != this.dialect);
	}

	/** 返回方言。
	 */
	public Dialect getDialect() {
		return this.dialect;
	}

	/** 设置关联方言。
	 */
	public void capture(Dialect dialect) {
		this.dialect = dialect;
	}

	/** 提交主语。
	*/
	public void commit(SubjectStuff subject) {
		if (null == this.subjectList)
			this.subjectList = new ArrayList<SubjectStuff>();
		this.subjectList.add(subject);
	}

	/** 提交谓语。
	 */
	public void commit(PredicateStuff predicate) {
		if (null == this.predicateList)
			this.predicateList = new ArrayList<PredicateStuff>();
		this.predicateList.add(predicate);
	}

	/** 提交宾语。
	 */
	public void commit(ObjectiveStuff objective) {
		if (null == this.objectiveList)
			this.objectiveList = new ArrayList<ObjectiveStuff>();
		this.objectiveList.add(objective);
	}

	/** 提交定语。
	 */
	public void commit(AttributiveStuff attributive) {
		if (null == this.attributiveList)
			this.attributiveList = new ArrayList<AttributiveStuff>();
		this.attributiveList.add(attributive);
	}

	/** 提交状语。
	 */
	public void commit(AdverbialStuff adverbial) {
		if (null == this.adverbialList)
			this.adverbialList = new ArrayList<AdverbialStuff>();
		this.adverbialList.add(adverbial);
	}

	/** 提交补语。
	 */
	public void commit(ComplementStuff complement) {
		if (null == this.complementList)
			this.complementList = new ArrayList<ComplementStuff>();
		this.complementList.add(complement);
	}

	/** 返回主语列表。
	*/
	public List<SubjectStuff> subjects() {
		return this.subjectList;
	}

	/** 返回谓语列表
	*/
	public List<PredicateStuff> predicates() {
		return this.predicateList;
	}

	/** 返回宾语列表
	*/
	public List<ObjectiveStuff> objectives() {
		return this.objectiveList;
	}

	/** 返回定语列表。
	*/
	public List<AttributiveStuff> attributives() {
		return this.attributiveList;
	}

	/** 返回状语列表。
	*/
	public List<AdverbialStuff> adverbials() {
		return this.adverbialList;
	}

	/** 返回补语列表。
	*/
	public List<ComplementStuff> complements() {
		return this.complementList;
	}

	/** 复制语素到指定原语。
	 */
	public void copyStuff(Primitive dest) {
		if (null != this.subjectList) {
			for (int i = 0, size = this.subjectList.size(); i < size; ++i) {
				dest.commit(this.subjectList.get(i));
			}
		}

		if (null != this.predicateList) {
			for (int i = 0, size = this.predicateList.size(); i < size; ++i) {
				dest.commit(this.predicateList.get(i));
			}
		}

		if (null != this.objectiveList) {
			for (int i = 0, size = this.objectiveList.size(); i < size; ++i) {
				dest.commit(this.objectiveList.get(i));
			}
		}

		if (null != this.attributiveList) {
			for (int i = 0, size = this.attributiveList.size(); i < size; ++i) {
				dest.commit(this.attributiveList.get(i));
			}
		}

		if (null != this.adverbialList) {
			for (int i = 0, size = this.adverbialList.size(); i < size; ++i) {
				dest.commit(this.adverbialList.get(i));
			}
		}

		if (null != this.complementList) {
			for (int i = 0, size = this.complementList.size(); i < size; ++i) {
				dest.commit(this.complementList.get(i));
			}
		}
	}

	/** 清空所有语素。
	 */
	public void clearStuffs() {
		if (null != this.subjectList)
			this.subjectList.clear();

		if (null != this.predicateList)
			this.predicateList.clear();

		if (null != this.objectiveList)
			this.objectiveList.clear();

		if (null != this.attributiveList)
			this.attributiveList.clear();

		if (null != this.adverbialList)
			this.adverbialList.clear();

		if (null != this.complementList)
			this.complementList.clear();
	}

	/** 将原语数据写入序列化流。
	*/
	public ByteArrayOutputStream write() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		PrimitiveSerializer.write(stream, this);
		return stream;
	}

	/** 从序列化流读取原语数据。
	*/
	public void read(ByteArrayInputStream stream) {
		PrimitiveSerializer.read(this, stream);
	}
}
