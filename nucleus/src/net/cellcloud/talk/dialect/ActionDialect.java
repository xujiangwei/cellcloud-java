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

/**
 * 动作方言。
 * 
 * @author Ambrose Xu
 * 
 */
public class ActionDialect extends Dialect {

	/**
	 * 方言类型名。
	 */
	public final static String DIALECT_NAME = "ActionDialect";

	/** 动作名。 */
	private String action;
	/** 参数名列表。 */
	private LinkedList<String> nameList;
	/** 参数值列表。 */
	private LinkedList<ObjectiveStuff> valueList;

	/** 动作自定义上下文数据。 */
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
	 * 
	 * @param tracker 指定方言的 Tracker 。
	 */
	public ActionDialect(String tracker) {
		super(ActionDialect.DIALECT_NAME, tracker);
		this.nameList = new LinkedList<String>();
		this.valueList = new LinkedList<ObjectiveStuff>();
	}

	/**
	 * 构造函数。
	 * 
	 * @param tracker 指定方言的 Tracker 。
	 * @param action 指定动作名。
	 */
	public ActionDialect(String tracker, String action) {
		super(ActionDialect.DIALECT_NAME, tracker);
		this.action = action;
		this.nameList = new LinkedList<String>();
		this.valueList = new LinkedList<ObjectiveStuff>();
	}

	/**
	 * 设置自定义上下文。
	 * 
	 * @param custom 指定自定义数据对象。
	 */
	public void setCustomContext(Object custom) {
		this.customContext = custom;
	}

	/**
	 * 获得自定义上下文。
	 * 
	 * @return 返回自定义上下文对象。
	 */
	public Object getCustomContext() {
		return this.customContext;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Primitive reconstruct() {
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void construct(Primitive primitive) {
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

	/**
	 * 设置动作名。
	 * 
	 * @param action 指定动作名。
	 */
	public void setAction(String action) {
		this.action = action;
	}

	/**
	 * 获得动作名。
	 * 
	 * @return 返回动作名。
	 */
	public String getAction() {
		return this.action;
	}

	/**
	 * 添加动作参数键值对。
	 * 
	 * @param name 指定参数名。
	 * @param value 指定字符串类型的参数值。
	 */
	public void appendParam(String name, String value) {
		synchronized (this) {
			this.nameList.add(name);
			this.valueList.add(new ObjectiveStuff(value));
		}
	}

	/**
	 * 添加动作参数键值对。
	 * 
	 * @param name 指定参数名。
	 * @param value 指定整数类型的参数值。
	 */
	public void appendParam(String name, int value) {
		synchronized (this) {
			this.nameList.add(name);
			this.valueList.add(new ObjectiveStuff(value));
		}
	}

	/**
	 * 添加动作参数键值对。
	 * 
	 * @param name 指定参数名。
	 * @param value 指定长整数类型的参数值。
	 */
	public void appendParam(String name, long value) {
		synchronized (this) {
			this.nameList.add(name);
			this.valueList.add(new ObjectiveStuff(value));
		}
	}

	/**
	 * 添加动作参数键值对。
	 * 
	 * @param name 指定参数名。
	 * @param value 指定浮点数类型的参数值。
	 */
	public void appendParam(String name, float value) {
		synchronized (this) {
			this.nameList.add(name);
			this.valueList.add(new ObjectiveStuff(value));
		}
	}

	/**
	 * 添加动作参数键值对。
	 * 
	 * @param name 指定参数名。
	 * @param value 指定双精浮点类型的参数值。
	 */
	public void appendParam(String name, double value) {
		synchronized (this) {
			this.nameList.add(name);
			this.valueList.add(new ObjectiveStuff(value));
		}
	}

	/**
	 * 添加动作参数键值对。
	 * 
	 * @param name 指定参数名。
	 * @param value 指定布尔类型的参数值。
	 */
	public void appendParam(String name, boolean value) {
		synchronized (this) {
			this.nameList.add(name);
			this.valueList.add(new ObjectiveStuff(value));
		}
	}

	/**
	 * 添加动作参数键值对。
	 * 
	 * @param name 指定参数名。
	 * @param value 指定 JSON 格式类型的参数值。
	 */
	public void appendParam(String name, JSONObject value) {
		synchronized (this) {
			this.nameList.add(name);
			this.valueList.add(new ObjectiveStuff(value));
		}
	}

	/**
	 * 获得指定参数名的字符串类型参数值。
	 * 
	 * @param name 指定参数名。
	 * @return 返回字符串类型的参数值。
	 */
	public String getParamAsString(String name) {
		synchronized (this) {
			int index = this.nameList.indexOf(name);
			if (index >= 0)
				return this.valueList.get(index).getValueAsString();
		}

		return null;
	}

	/**
	 * 获得指定参数名的整数类型参数值。
	 * 
	 * @param name 指定参数名。
	 * @return 返回整数类型的参数值。
	 */
	public int getParamAsInt(String name) {
		synchronized (this) {
			int index = this.nameList.indexOf(name);
			if (index >= 0)
				return this.valueList.get(index).getValueAsInt();
		}

		return 0;
	}

	/**
	 * 获得指定参数名的长整数类型参数值。
	 * 
	 * @param name 指定参数名。
	 * @return 返回长整数类型的参数值。
	 */
	public long getParamAsLong(String name) {
		synchronized (this) {
			int index = this.nameList.indexOf(name);
			if (index >= 0)
				return this.valueList.get(index).getValueAsLong();
		}

		return 0;
	}

	/**
	 * 获得指定参数名的浮点数类型参数值。
	 * 
	 * @param name 指定参数名。
	 * @return 返回浮点数类型的参数值。
	 */
	public float getParamAsFloat(String name) {
		synchronized (this) {
			int index = this.nameList.indexOf(name);
			if (index >= 0)
				return this.valueList.get(index).getValueAsFloat();
		}

		return 0;
	}

	/**
	 * 获得指定参数名的双精浮点类型参数值。
	 * 
	 * @param name 指定参数名。
	 * @return 返回双精浮点类型的参数值。
	 */
	public double getParamAsDouble(String name) {
		synchronized (this) {
			int index = this.nameList.indexOf(name);
			if (index >= 0)
				return this.valueList.get(index).getValueAsDouble();
		}

		return 0;
	}

	/**
	 * 获得指定参数名的布尔类型参数值。
	 * 
	 * @param name 指定参数名。
	 * @return 返回布尔类型的参数值。
	 */
	public boolean getParamAsBoolean(String name) {
		synchronized (this) {
			int index = this.nameList.indexOf(name);
			if (index >= 0)
				return this.valueList.get(index).getValueAsBool();
		}

		return false;
	}

	/**
	 * 获得指定参数名的 JSON 类型参数值。
	 * 
	 * @param name 指定参数名。
	 * @return 返回 JSON 类型的参数值。
	 * @throws JSONException
	 */
	public JSONObject getParamAsJSON(String name) throws JSONException {
		synchronized (this) {
			int index = this.nameList.indexOf(name);
			if (index >= 0)
				return this.valueList.get(index).getValueAsJSON();
		}

		return null;
	}

	/**
	 * 判断指定名称的参数是否存在。
	 * 
	 * @param name 指定需判断的参数名。
	 * @return 如果该参数存在返回 <code>true</code> ，否则返回 <code>false</code> 。
	 */
	public boolean hasParam(String name) {
		synchronized (this) {
			return this.nameList.contains(name);
		}
	}

	/**
	 * 获得动作方言里包含的所有参数名。
	 * 
	 * @return 返回包含所有参数名的列表。
	 */
	public List<String> getParamNames() {
		return this.nameList;
	}

	/**
	 * 获得指定参数名的字面义。
	 * 
	 * @param name 指定参数名。
	 * @return 返回指定参数名字面义。
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
	 * 异步方式执行动作委派。
	 */
	public void act(ActionDelegate delegate) {
		ActionDialectFactory factory = (ActionDialectFactory) DialectEnumerator.getInstance().getFactory(ActionDialect.DIALECT_NAME);
		if (null != factory) {
			factory.doAction(this, delegate);
		}
	}

}
