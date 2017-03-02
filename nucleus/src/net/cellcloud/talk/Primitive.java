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

package net.cellcloud.talk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import net.cellcloud.core.Cellet;
import net.cellcloud.talk.dialect.Dialect;
import net.cellcloud.talk.stuff.AdverbialStuff;
import net.cellcloud.talk.stuff.AttributiveStuff;
import net.cellcloud.talk.stuff.ComplementStuff;
import net.cellcloud.talk.stuff.ObjectiveStuff;
import net.cellcloud.talk.stuff.PredicateStuff;
import net.cellcloud.talk.stuff.PrimitiveSerializer;
import net.cellcloud.talk.stuff.StuffVersion;
import net.cellcloud.talk.stuff.SubjectStuff;

/**
 * 原语描述类。
 * 
 * @author Ambrose Xu
 * 
 */
public class Primitive {

	/** 生成该原语的内核节点标签。 */
	private String ownerTag;
	/** 此原语关联的 Cellet 标识。 */
	private String celletIdentifier;

	/** 语素版本。 */
	private StuffVersion version;
	/** 主语语素清单。 */
	private ArrayList<SubjectStuff> subjectList;
	/** 谓语语素清单。 */
	private ArrayList<PredicateStuff> predicateList;
	/** 宾语语素清单。 */
	private ArrayList<ObjectiveStuff> objectiveList;
	/** 定语语素清单。 */
	private ArrayList<AttributiveStuff> attributiveList;
	/** 状语语素清单。 */
	private ArrayList<AdverbialStuff> adverbialList;
	/** 补语语素清单。 */
	private ArrayList<ComplementStuff> complementList;

	/** 方言。 */
	private Dialect dialect;

	/** 关联的 Cellet 。 */
	private Cellet cellet;

	/**
	 * 构造函数。
	 */
	public Primitive() {
		this.ownerTag = null;
		this.celletIdentifier = null;
		this.version = StuffVersion.V2;
		this.dialect = null;
		this.cellet = null;
	}

	/**
	 * 构造函数。
	 * 
	 * @param ownerTag 指定源的内核标签。
	 */
	public Primitive(String ownerTag) {
		this.ownerTag = ownerTag;
		this.celletIdentifier = null;
		this.version = StuffVersion.V2;
		this.dialect = null;
		this.cellet = null;
	}

	/**
	 * 构造函数。
	 * 
	 * @param dialect 指定关联的方言对象。
	 */
	public Primitive(Dialect dialect) {
		this.ownerTag = null;
		this.celletIdentifier = null;
		this.version = StuffVersion.V2;
		this.dialect = dialect;
		this.cellet = null;
	}

	/**
	 * 获得原语所属端的标签。
	 * 
	 * @return 返回原语所属端的标签。
	 */
	public String getOwnerTag() {
		return this.ownerTag;
	}

	/**
	 * 设置语素版本。
	 * 
	 * @param version 指定新的语素版本。
	 */
	public void setVersion(StuffVersion version) {
		this.version = version;

		if (null != this.subjectList) {
			for (SubjectStuff s : this.subjectList) {
				s.changeVersion(version);
			}
		}
		if (null != this.predicateList) {
			for (PredicateStuff p : this.predicateList) {
				p.changeVersion(version);
			}
		}
		if (null != this.objectiveList) {
			for (ObjectiveStuff o : this.objectiveList) {
				o.changeVersion(version);
			}
		}
		if (null != this.attributiveList) {
			for (AttributiveStuff a : this.attributiveList) {
				a.changeVersion(version);
			}
		}
		if (null != this.adverbialList) {
			for (AdverbialStuff a : this.adverbialList) {
				a.changeVersion(version);
			}
		}
		if (null != this.complementList) {
			for (ComplementStuff c : this.complementList) {
				c.changeVersion(version);
			}
		}
	}

	/**
	 * 获得语素版本。
	 * 
	 * @return 返回语素版本。
	 */
	public StuffVersion getVersion() {
		return this.version;
	}

	/**
	 * 设置 Cellet 标识。
	 * 
	 * @param celletIdentifier 指定 Cellet 标识。
	 */
	public void setCelletIdentifier(String celletIdentifier) {
		this.celletIdentifier = celletIdentifier;
		if (null != this.dialect) {
			this.dialect.setCelletIdentifier(celletIdentifier);
		}
	}

	/**
	 * 获得 Cellet 标识。
	 * 
	 * @return 返回 Cellet 标识。
	 */
	public String getCelletIdentifier() {
		return this.celletIdentifier;
	}

	/**
	 * 设置关联的 Cellet 。
	 * 
	 * @param cellet 指定 Cellet 实例。
	 */
	protected void setCellet(Cellet cellet) {
		this.cellet = cellet;
		if (null != this.dialect) {
			this.dialect.setCellet(cellet);
		}
	}

	/**
	 * 获得关联的 Cellet 。
	 * 
	 * @return 返回关联的 Cellet 。
	 */
	public Cellet getCellet() {
		return this.cellet;
	}

	/**
	 * 判断是否具有方言特征。
	 * 
	 * @return 如果原语具有方言特征返回 <code>true</code> 。
	 */
	public boolean isDialectal() {
		return (null != this.dialect);
	}

	/**
	 * 获得翻译后的方言。
	 * 
	 * @return 返回方言实例。
	 */
	public Dialect getDialect() {
		return this.dialect;
	}

	/**
	 * 设置关联方言。
	 * 
	 * @param dialect 指定需设置的方言。
	 */
	public void capture(Dialect dialect) {
		this.dialect = dialect;
		this.dialect.setOwnerTag(this.ownerTag);
		this.dialect.setCelletIdentifier(this.celletIdentifier);
	}

	/**
	 * 提交主语数据。
	 * 
	 * @param subject 指定需提交的主语。
	 */
	public void commit(SubjectStuff subject) {
		subject.changeVersion(this.version);

		if (null == this.subjectList)
			this.subjectList = new ArrayList<SubjectStuff>();
		this.subjectList.add(subject);
	}

	/**
	 * 提交谓语数据。
	 * 
	 * @param predicate 指定需提交的谓语。
	 */
	public void commit(PredicateStuff predicate) {
		predicate.changeVersion(this.version);

		if (null == this.predicateList)
			this.predicateList = new ArrayList<PredicateStuff>();
		this.predicateList.add(predicate);
	}

	/**
	 * 提交宾语数据。
	 * 
	 * @param objective 指定需提交的宾语。
	 */
	public void commit(ObjectiveStuff objective) {
		objective.changeVersion(this.version);

		if (null == this.objectiveList)
			this.objectiveList = new ArrayList<ObjectiveStuff>();
		this.objectiveList.add(objective);
	}

	/**
	 * 提交定语数据。
	 * 
	 * @param attributive 指定需提交的定语。
	 */
	public void commit(AttributiveStuff attributive) {
		attributive.changeVersion(this.version);

		if (null == this.attributiveList)
			this.attributiveList = new ArrayList<AttributiveStuff>();
		this.attributiveList.add(attributive);
	}

	/**
	 * 提交状语数据。
	 * 
	 * @param adverbial 指定需提交的状语。
	 */
	public void commit(AdverbialStuff adverbial) {
		adverbial.changeVersion(this.version);

		if (null == this.adverbialList)
			this.adverbialList = new ArrayList<AdverbialStuff>();
		this.adverbialList.add(adverbial);
	}

	/**
	 * 提交补语数据。
	 *
	 * @param complement 指定需提交的补语。
	 */
	public void commit(ComplementStuff complement) {
		complement.changeVersion(this.version);

		if (null == this.complementList)
			this.complementList = new ArrayList<ComplementStuff>();
		this.complementList.add(complement);
	}

	/**
	 * 返回主语列表。
	 * 
	 * @return 返回主语列表。
	 */
	public List<SubjectStuff> subjects() {
		return this.subjectList;
	}

	/**
	 * 返回谓语列表。
	 * 
	 * @return 返回谓语列表。
	 */
	public List<PredicateStuff> predicates() {
		return this.predicateList;
	}

	/**
	 * 返回宾语列表。
	 * 
	 * @return 返回宾语列表。
	 */
	public List<ObjectiveStuff> objectives() {
		return this.objectiveList;
	}

	/**
	 * 返回定语列表。
	 * 
	 * @return 返回定语列表。
	 */
	public List<AttributiveStuff> attributives() {
		return this.attributiveList;
	}

	/**
	 * 返回状语列表。
	 * 
	 * @return 返回状语列表。
	 */
	public List<AdverbialStuff> adverbials() {
		return this.adverbialList;
	}

	/**
	 * 返回补语列表。
	 * 
	 * @return 返回补语列表。
	 */
	public List<ComplementStuff> complements() {
		return this.complementList;
	}

	/**
	 * 复制当前实例的语素到指定原语。
	 * 
	 * @param dest 指定复制目标原语。
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

	/**
	 * 清空所有语素。
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

	/**
	 * 将原语数据写入序列化流。
	 * 
	 * @return 返回存储序列化数据的流。
	 */
	public ByteArrayOutputStream write() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream(256);
		PrimitiveSerializer.write(stream, this);
		return stream;
	}

	/**
	 * 从序列化流读取原语数据。
	 * 
	 * @param stream 指定数据源的流。
	 */
	public void read(ByteArrayInputStream stream) {
		PrimitiveSerializer.read(this, stream);
	}

}
