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

import java.util.LinkedList;
import java.util.List;

import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.stuff.LiteralBase;
import net.cellcloud.talk.stuff.ObjectiveStuff;
import net.cellcloud.talk.stuff.PredicateStuff;
import net.cellcloud.talk.stuff.SubjectStuff;

import org.json.JSONException;
import org.json.JSONObject;

/** 动作方言。
 * 
 * @author Jiangwei Xu
 */
public class ActionDialect extends Dialect {

	public final static String DIALECT_NAME = "ActionDialect";

	private String action;
	private LinkedList<String> nameList;
	private LinkedList<ObjectiveStuff> valueList;

	private Object customContext;

	/**
	 * 构造函数。
	 */
	public ActionDialect() {
		super(ActionDialect.DIALECT_NAME);
		this.nameList = new LinkedList<String>();
		this.valueList = new LinkedList<ObjectiveStuff>();
	}

	/**
	 * 构造函数。
	 * @param tracker
	 */
	public ActionDialect(String tracker) {
		super(ActionDialect.DIALECT_NAME, tracker);
		this.nameList = new LinkedList<String>();
		this.valueList = new LinkedList<ObjectiveStuff>();
	}

	/**
	 * 构造函数。
	 * @param tracker
	 * @param action
	 */
	public ActionDialect(String tracker, String action) {
		super(ActionDialect.DIALECT_NAME, tracker);
		this.action = action;
		this.nameList = new LinkedList<String>();
		this.valueList = new LinkedList<ObjectiveStuff>();
	}

	/** 设置自定义上下文。
	 */
	public void setCustomContext(Object obj) {
		this.customContext = obj;
	}

	/** 返回自定义上下文。
	 */
	public Object getCustomContext() {
		return this.customContext;
	}

	@Override
	public Primitive translate() {
		if (null == this.action || this.action.isEmpty()) {
			return null;
		}

		Primitive primitive = new Primitive(this);

		synchronized (this) {
			for (int i = 0, size = this.nameList.size(); i < size; ++i) {
				SubjectStuff nameStuff = new SubjectStuff(this.nameList.get(i));
				ObjectiveStuff valueStuff = this.valueList.get(i);

				primitive.commit(nameStuff);
				primitive.commit(valueStuff);
			}
		}

		PredicateStuff actionStuff = new PredicateStuff(this.action);
		primitive.commit(actionStuff);

		return primitive;
	}

	@Override
	public void build(Primitive primitive) {
		this.action = primitive.predicates().get(0).getValueAsString();

		if (null != primitive.subjects()) {
			List<SubjectStuff> names = primitive.subjects();
			List<ObjectiveStuff> values = primitive.objectives();
			synchronized (this) {
				for (int i = 0, size = names.size(); i < size; ++i) {
					this.nameList.add(names.get(i).getValueAsString());
					this.valueList.add(values.get(i));
				}
			}
		}
	}

	/** 设置动作名。
	 */
	public void setAction(final String action) {
		this.action = action;
	}

	/** 返回动作名。
	 */
	public String getAction() {
		return this.action;
	}

	/** 添加动作参数键值对。
	 */
	public void appendParam(final String name, final String value) {
		synchronized (this) {
			this.nameList.add(name);
			this.valueList.add(new ObjectiveStuff(value));
		}
	}
	/** 添加动作参数键值对。
	 */
	public void appendParam(final String name, final int value) {
		synchronized (this) {
			this.nameList.add(name);
			this.valueList.add(new ObjectiveStuff(value));
		}
	}
	/** 添加动作参数键值对。
	 */
	public void appendParam(final String name, final long value) {
		synchronized (this) {
			this.nameList.add(name);
			this.valueList.add(new ObjectiveStuff(value));
		}
	}
	/** 添加动作参数键值对。
	 */
	public void appendParam(final String name, final float value) {
		synchronized (this) {
			this.nameList.add(name);
			this.valueList.add(new ObjectiveStuff(value));
		}
	}
	/** 添加动作参数键值对。
	 */
	public void appendParam(final String name, final double value) {
		synchronized (this) {
			this.nameList.add(name);
			this.valueList.add(new ObjectiveStuff(value));
		}
	}
	/** 添加动作参数键值对。
	 */
	public void appendParam(final String name, final boolean value) {
		synchronized (this) {
			this.nameList.add(name);
			this.valueList.add(new ObjectiveStuff(value));
		}
	}
	/** 添加动作参数键值对。
	 */
	public void appendParam(final String name, final JSONObject value) {
		synchronized (this) {
			this.nameList.add(name);
			this.valueList.add(new ObjectiveStuff(value));
		}
	}

	/** 返回指定名称的参数值。
	 */
	public String getParamAsString(final String name) {
		synchronized (this) {
			int index = this.nameList.indexOf(name);
			if (index >= 0)
				return this.valueList.get(index).getValueAsString();
		}

		return null;
	}
	/** 返回指定名称的参数值。
	 */
	public int getParamAsInt(final String name) {
		synchronized (this) {
			int index = this.nameList.indexOf(name);
			if (index >= 0)
				return this.valueList.get(index).getValueAsInt();
		}

		return 0;
	}
	/** 返回指定名称的参数值。
	 */
	public long getParamAsLong(final String name) {
		synchronized (this) {
			int index = this.nameList.indexOf(name);
			if (index >= 0)
				return this.valueList.get(index).getValueAsLong();
		}

		return 0;
	}
	/** 返回指定名称的参数值。
	 */
	public float getParamAsFloat(final String name) {
		synchronized (this) {
			int index = this.nameList.indexOf(name);
			if (index >= 0)
				return this.valueList.get(index).getValueAsFloat();
		}

		return 0;
	}
	/** 返回指定名称的参数值。
	 */
	public double getParamAsDouble(final String name) {
		synchronized (this) {
			int index = this.nameList.indexOf(name);
			if (index >= 0)
				return this.valueList.get(index).getValueAsDouble();
		}

		return 0;
	}
	/** 返回指定名称的参数值。
	 */
	public boolean getParamAsBoolean(final String name) {
		synchronized (this) {
			int index = this.nameList.indexOf(name);
			if (index >= 0)
				return this.valueList.get(index).getValueAsBool();
		}

		return false;
	}
	/** 返回指定名称的参数值。
	 * @throws JSONException 
	 */
	public JSONObject getParamAsJSON(final String name) throws JSONException {
		synchronized (this) {
			int index = this.nameList.indexOf(name);
			if (index >= 0)
				return this.valueList.get(index).getValueAsJSON();
		}

		return null;
	}

	/** 判断指定名称的参数是否存在。
	 */
	public boolean existParam(final String name) {
		synchronized (this) {
			return this.nameList.contains(name);
		}
	}

	/** 返回所有参数名。
	 */
	public List<String> getParamNames() {
		return this.nameList;
	}

	/**
	 * 返回指定参数的数值字面义。
	 * @param name
	 * @return
	 */
	public LiteralBase getParamLiteralBase(String name) {
		synchronized (this) {
			int index = this.nameList.indexOf(name);
			if (index >= 0) {
				return this.valueList.get(index).getLiteralBase();
			}
		}

		return null;
	}

	/**
	 * 创建数据副本。
	 * @return
	 */
	public ActionDialect copy() {
		ActionDialect copy = new ActionDialect();
		synchronized (this) {
			copy.tracker = this.tracker;
			copy.tag = this.tag;
			copy.celletIdentifier = this.celletIdentifier;
			copy.cellet = this.cellet;

			copy.action = this.action;

			for (String name : this.nameList) {
				copy.nameList.add(name);
			}

			for (ObjectiveStuff value : this.valueList) {
				copy.valueList.add(value);
			}

			copy.customContext = this.customContext;
		}
		return copy;
	}

	/** 执行动作委派（异步）。
	 */
	public void act(ActionDelegate delegate) {
		ActionDialectFactory factory = (ActionDialectFactory) DialectEnumerator.getInstance().getFactory(ActionDialect.DIALECT_NAME);
		if (null != factory) {
			factory.doAction(this, delegate);
		}
	}
}
