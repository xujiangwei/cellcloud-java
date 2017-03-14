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

package net.cellcloud.talk.stuff;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.dialect.Dialect;
import net.cellcloud.talk.dialect.DialectEnumerator;
import net.cellcloud.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 原语序列化器。
 * 
 * @author Ambrose Xu
 * 
 */
public final class PrimitiveSerializer {

	private static final byte TOKEN_OPEN_BRACKET = '[';
	private static final byte TOKEN_CLOSE_BRACKET = ']';
	private static final byte TOKEN_OPEN_BRACE = '{';
	private static final byte TOKEN_CLOSE_BRACE = '}';
	private static final byte TOKEN_OPERATE_ASSIGN = '=';
	private static final byte TOKEN_OPERATE_DECLARE = ':';
	private static final byte TOKEN_AT = '@';
	private static final byte TOKEN_ESCAPE = '\\';
	private static final String TOKEN_AT_STR = "@";

	private static final byte PARSE_PHASE_UNKNOWN = 0;
	private static final byte PARSE_PHASE_VERSION = 1;
	private static final byte PARSE_PHASE_TYPE = 2;
	private static final byte PARSE_PHASE_LITERAL = 3;
	private static final byte PARSE_PHASE_VALUE = 4;
	private static final byte PARSE_PHASE_STUFF = 5;
	private static final byte PARSE_PHASE_DIALECT = 6;

	private static final String LITERALBASE_STRING = "string";
	private static final String LITERALBASE_INT = "int";
	private static final String LITERALBASE_UINT = "uint";
	private static final String LITERALBASE_LONG = "long";
	private static final String LITERALBASE_ULONG = "ulong";
	private static final String LITERALBASE_FLOAT = "float";
	private static final String LITERALBASE_DOUBLE = "double";
	private static final String LITERALBASE_BOOL = "bool";
	private static final String LITERALBASE_JSON = "json";
	private static final String LITERALBASE_BIN = "bin";
	private static final String LITERALBASE_XML = "xml";

	private static final byte[] LITERALBASE_STRING_BYTES = LITERALBASE_STRING.getBytes();
	private static final byte[] LITERALBASE_INT_BYTES = LITERALBASE_INT.getBytes();
	private static final byte[] LITERALBASE_UINT_BYTES = LITERALBASE_UINT.getBytes();
	private static final byte[] LITERALBASE_LONG_BYTES = LITERALBASE_LONG.getBytes();
	private static final byte[] LITERALBASE_ULONG_BYTES = LITERALBASE_ULONG.getBytes();
	private static final byte[] LITERALBASE_FLOAT_BYTES = LITERALBASE_FLOAT.getBytes();
	private static final byte[] LITERALBASE_DOUBLE_BYTES = LITERALBASE_DOUBLE.getBytes();
	private static final byte[] LITERALBASE_BOOL_BYTES = LITERALBASE_BOOL.getBytes();
	private static final byte[] LITERALBASE_JSON_BYTES = LITERALBASE_JSON.getBytes();
	private static final byte[] LITERALBASE_BIN_BYTES = LITERALBASE_BIN.getBytes();
	private static final byte[] LITERALBASE_XML_BYTES = LITERALBASE_XML.getBytes();

	private static final String STUFFTYPE_SUBJECT = "sub";
	private static final String STUFFTYPE_PREDICATE = "pre";
	private static final String STUFFTYPE_OBJECTIVE = "obj";
	private static final String STUFFTYPE_ADVERBIAL = "adv";
	private static final String STUFFTYPE_ATTRIBUTIVE = "att";
	private static final String STUFFTYPE_COMPLEMENT = "com";

	private static final byte[] STUFFTYPE_SUBJECT_BYTES = STUFFTYPE_SUBJECT.getBytes();
	private static final byte[] STUFFTYPE_PREDICATE_BYTES = STUFFTYPE_PREDICATE.getBytes();
	private static final byte[] STUFFTYPE_OBJECTIVE_BYTES = STUFFTYPE_OBJECTIVE.getBytes();
	private static final byte[] STUFFTYPE_ADVERBIAL_BYTES = STUFFTYPE_ADVERBIAL.getBytes();
	private static final byte[] STUFFTYPE_ATTRIBUTIVE_BYTES = STUFFTYPE_ATTRIBUTIVE.getBytes();
	private static final byte[] STUFFTYPE_COMPLEMENT_BYTES = STUFFTYPE_COMPLEMENT.getBytes();

	private static final String JSONKEY_VERSION = "version";
	private static final String JSONKEY_STUFFS = "stuffs";
	private static final String JSONKEY_STUFFTYPE = "type";
	private static final String JSONKEY_STUFFVALUE = "value";
	private static final String JSONKEY_LITERALBASE = "literal";
	private static final String JSONKEY_DIALECT = "dialect";
	private static final String JSONKEY_NAME = "name";
	private static final String JSONKEY_TRACKER = "tracker";

	/** 缓存区大小。 */
	private static final int BLOCK = 65536;

	private PrimitiveSerializer() {
	}

	/**
	 * 将原语写入数据流。
	 * 
	 * @param stream 指定需写入数据的输出流。
	 * @param primitive 指定待操作源原语。
	 */
	public static void write(OutputStream stream, Primitive primitive) {
		/*
		原语序列化格式：
		[version]{sutff}...{stuff}[dialect@tracker]
		示例：
		[01000]{sub=cloud:string}{pre=add:string}[Action@Ambrose]
		*/

		try {
			// 版本
			stream.write((int)TOKEN_OPEN_BRACKET);
			byte[] version = null;
			if (StuffVersion.V2 == primitive.getVersion()) {
				version = new byte[]{'0', '0', '2', '0', '0'};
			}
			else {
				version = new byte[]{'0', '1', '0', '0', '0'};
			}
			stream.write(version);
			stream.write((int)TOKEN_CLOSE_BRACKET);

			ByteBuffer buf = ByteBuffer.allocate(BLOCK);
			int bufLength = 0;

			// 语素
			List<SubjectStuff> subjects = primitive.subjects();
			if (null != subjects) {
				Iterator<SubjectStuff> iter = subjects.iterator();
				while (iter.hasNext()) {
					SubjectStuff stuff = iter.next();
					stream.write((int)TOKEN_OPEN_BRACE);
					stream.write(STUFFTYPE_SUBJECT_BYTES);
					stream.write((int)TOKEN_OPERATE_ASSIGN);

					bufLength = reviseValue(buf, stuff.value);
					buf.flip();
					byte[] d = new byte[bufLength];
					buf.get(d, 0, bufLength);
					stream.write(d);
					buf.clear();

					stream.write((int)TOKEN_OPERATE_DECLARE);
					stream.write(parseLiteralBase(stuff.literalBase));
					stream.write((int)TOKEN_CLOSE_BRACE);
				}
			}
			List<PredicateStuff> predicates = primitive.predicates();
			if (null != predicates) {
				Iterator<PredicateStuff> iter = predicates.iterator();
				while (iter.hasNext()) {
					PredicateStuff stuff = iter.next();
					stream.write((int)TOKEN_OPEN_BRACE);
					stream.write(STUFFTYPE_PREDICATE_BYTES);
					stream.write((int)TOKEN_OPERATE_ASSIGN);

					bufLength = reviseValue(buf, stuff.value);
					buf.flip();
					byte[] d = new byte[bufLength];
					buf.get(d, 0, bufLength);
					stream.write(d);
					buf.clear();

					stream.write((int)TOKEN_OPERATE_DECLARE);
					stream.write(parseLiteralBase(stuff.literalBase));
					stream.write((int)TOKEN_CLOSE_BRACE);
				}
			}
			List<ObjectiveStuff> objectives = primitive.objectives();
			if (null != objectives) {
				Iterator<ObjectiveStuff> iter = objectives.iterator();
				while (iter.hasNext()) {
					ObjectiveStuff stuff = iter.next();
					stream.write((int)TOKEN_OPEN_BRACE);
					stream.write(STUFFTYPE_OBJECTIVE_BYTES);
					stream.write((int)TOKEN_OPERATE_ASSIGN);

					bufLength = reviseValue(buf, stuff.value);
					buf.flip();
					byte[] d = new byte[bufLength];
					buf.get(d, 0, bufLength);
					stream.write(d);
					buf.clear();

					stream.write((int)TOKEN_OPERATE_DECLARE);
					stream.write(parseLiteralBase(stuff.literalBase));
					stream.write((int)TOKEN_CLOSE_BRACE);
				}
			}
			List<AdverbialStuff> adverbials = primitive.adverbials();
			if (null != adverbials) {
				Iterator<AdverbialStuff> iter = adverbials.iterator();
				while (iter.hasNext()) {
					AdverbialStuff stuff = iter.next();
					stream.write((int)TOKEN_OPEN_BRACE);
					stream.write(STUFFTYPE_ADVERBIAL_BYTES);
					stream.write((int)TOKEN_OPERATE_ASSIGN);

					bufLength = reviseValue(buf, stuff.value);
					buf.flip();
					byte[] d = new byte[bufLength];
					buf.get(d, 0, bufLength);
					stream.write(d);
					buf.clear();

					stream.write((int)TOKEN_OPERATE_DECLARE);
					stream.write(parseLiteralBase(stuff.literalBase));
					stream.write((int)TOKEN_CLOSE_BRACE);
				}
			}
			List<AttributiveStuff> attributives = primitive.attributives();
			if (null != attributives) {
				Iterator<AttributiveStuff> iter = attributives.iterator();
				while (iter.hasNext()) {
					AttributiveStuff stuff = iter.next();
					stream.write((int)TOKEN_OPEN_BRACE);
					stream.write(STUFFTYPE_ATTRIBUTIVE_BYTES);
					stream.write((int)TOKEN_OPERATE_ASSIGN);

					bufLength = reviseValue(buf, stuff.value);
					buf.flip();
					byte[] d = new byte[bufLength];
					buf.get(d, 0, bufLength);
					stream.write(d);
					buf.clear();

					stream.write((int)TOKEN_OPERATE_DECLARE);
					stream.write(parseLiteralBase(stuff.literalBase));
					stream.write((int)TOKEN_CLOSE_BRACE);
				}
			}
			List<ComplementStuff> complements = primitive.complements();
			if (null != complements) {
				Iterator<ComplementStuff> iter = complements.iterator();
				while (iter.hasNext()) {
					ComplementStuff stuff = iter.next();
					stream.write((int)TOKEN_OPEN_BRACE);
					stream.write(STUFFTYPE_COMPLEMENT_BYTES);
					stream.write((int)TOKEN_OPERATE_ASSIGN);

					bufLength = reviseValue(buf, stuff.value);
					buf.flip();
					byte[] d = new byte[bufLength];
					buf.get(d, 0, bufLength);
					stream.write(d);
					buf.clear();

					stream.write((int)TOKEN_OPERATE_DECLARE);
					stream.write(parseLiteralBase(stuff.literalBase));
					stream.write((int)TOKEN_CLOSE_BRACE);
				}
			}

			// 方言
			Dialect dialect = primitive.getDialect();
			if (null != dialect) {
				stream.write(TOKEN_OPEN_BRACKET);
				stream.write(dialect.getName().getBytes(Charset.forName("UTF-8")));
				stream.write(TOKEN_AT);
				stream.write(dialect.getTracker().getBytes(Charset.forName("UTF-8")));
				stream.write(TOKEN_CLOSE_BRACKET);
			}

			stream.flush();
			buf = null;
		} catch (IOException e) {
			Logger.log(PrimitiveSerializer.class, e, LogLevel.ERROR);
		}
	}

	/**
	 * 从数据流中读取原语。
	 * 
	 * @param primitive 指定需写入数据的目标原语。
	 * @param stream 指定数据源的输入流。
	 */
	public static void read(Primitive primitive, InputStream stream) {
		/*
		原语序列化格式：
		[version]{sutff}...{stuff}[dialect@tracker]
		示例：
		[01000]{sub=cloud:string}{pre=add:string}[Action@Ambrose]
		*/

		try {
			byte phase = PARSE_PHASE_UNKNOWN;
			int read = 0;

			ByteBuffer buf = ByteBuffer.allocate(BLOCK);
			byte[] version = new byte[5];
			byte[] type = new byte[3];
			byte[] value = null;
			byte[] literal = null;
			int length = 0;

			while ((read = stream.read()) >= 0) {

				// 判断处理阶段
				switch (phase) {

				case PARSE_PHASE_VALUE:
					// 判断转义
					if (read == TOKEN_ESCAPE) {
						// 读取下一个字符
						int next = stream.read();
						if (next == TOKEN_OPEN_BRACE
							|| next == TOKEN_CLOSE_BRACE
							|| next == TOKEN_OPERATE_ASSIGN
							|| next == TOKEN_OPERATE_DECLARE
							|| next == TOKEN_ESCAPE) {
							buf.put((byte)next);
							++length;
						}
						else {
							buf.put((byte)read);
							buf.put((byte)next);
							length += 2;
						}

						// 继续下一个字节
						continue;
					}

					if (read == TOKEN_OPERATE_DECLARE) {
						// 数值结束
						buf.flip();
						value = new byte[length];
						buf.get(value, 0, length);
						buf.clear();

						phase = PARSE_PHASE_LITERAL;
						length = 0;
						continue;
					}

					buf.put((byte)read);
					++length;
					break;

				case PARSE_PHASE_TYPE:
					if (read == TOKEN_OPERATE_ASSIGN) {
						// 类型结束
						buf.flip();
						buf.get(type);
						buf.clear();

						phase = PARSE_PHASE_VALUE;
						length = 0;
						continue;
					}
					// 写入语素类型
					buf.put((byte)read);
					break;

				case PARSE_PHASE_LITERAL:
					if (read == TOKEN_CLOSE_BRACE) {
						// 字面义结束
						buf.flip();
						literal = new byte[length];
						buf.get(literal, 0, length);
						buf.clear();

						// 注入语素
						injectStuff(primitive, type, value, literal);

						phase = PARSE_PHASE_DIALECT;
						length = 0;
						continue;
					}
					buf.put((byte)read);
					++length;
					break;

				case PARSE_PHASE_STUFF:
					if (read == TOKEN_OPEN_BRACE) {
						// 进入解析语素阶段
						phase = PARSE_PHASE_TYPE;
						buf.clear();
					}
					break;

				case PARSE_PHASE_VERSION:
					if (read == TOKEN_CLOSE_BRACKET) {
						// 解析版本结束
						buf.flip();
						buf.get(version, 0, version.length);
						buf.clear();

						if (version[2] == '2') {
							primitive.setVersion(StuffVersion.V2);
						}
						else {
							primitive.setVersion(StuffVersion.V1);
						}

						phase = PARSE_PHASE_STUFF;
						continue;
					}
					buf.put((byte)read);
					break;

				case PARSE_PHASE_DIALECT:
					if (read == TOKEN_OPEN_BRACE) {
						phase = PARSE_PHASE_TYPE;
						buf.clear();
					}
					else if (read == TOKEN_OPEN_BRACKET) {
						// 解析方言开始
						buf.clear();
					}
					else if (read == TOKEN_CLOSE_BRACKET) {
						// 解析方言结束
						buf.flip();
						deserializeDialect(primitive, new String(buf.array(), 0, length, Charset.forName("UTF-8")));
						buf.clear();
					}
					else {
						// 记录数据
						buf.put((byte)read);
						++length;
					}
					break;

				default:
					if (read == TOKEN_OPEN_BRACE) {
						phase = PARSE_PHASE_TYPE;
						buf.clear();
					}
					else if (read == TOKEN_OPEN_BRACKET) {
						phase = PARSE_PHASE_VERSION;
						buf.clear();
					}
					break;
				}
			}

			buf.clear();

		} catch (IOException e) {
			Logger.log(PrimitiveSerializer.class, e, LogLevel.ERROR);
		}
	}

	/**
	 * 将数据数组解析为语素，并注入原语。
	 * 
	 * @param primitive 指定目标原语。
	 * @param type 指定语素类型描述。
	 * @param value 指定数值描述。
	 * @param literal 指定语素字面义描述。
	 */
	private static void injectStuff(Primitive primitive, byte[] type, byte[] value, byte[] literal) {
		// 字面义
		LiteralBase lb = parseLiteralBase(literal);
		if (lb == null) {
			return;
		}

		// 类型
		String typeString = new String(type);

		if (typeString.equals(STUFFTYPE_SUBJECT)) {
			SubjectStuff subject = new SubjectStuff(value);
			subject.literalBase = lb;
			primitive.commit(subject);
		}
		else if (typeString.equals(STUFFTYPE_PREDICATE)) {
			PredicateStuff predicate = new PredicateStuff(value);
			predicate.literalBase = lb;
			primitive.commit(predicate);
		}
		else if (typeString.equals(STUFFTYPE_OBJECTIVE)) {
			ObjectiveStuff objective = new ObjectiveStuff(value);
			objective.literalBase = lb;
			primitive.commit(objective);
		}
		else if (typeString.equals(STUFFTYPE_ADVERBIAL)) {
			AdverbialStuff adverbial = new AdverbialStuff(value);
			adverbial.literalBase = lb;
			primitive.commit(adverbial);
		}
		else if (typeString.equals(STUFFTYPE_ATTRIBUTIVE)) {
			AttributiveStuff attributive = new AttributiveStuff(value);
			attributive.literalBase = lb;
			primitive.commit(attributive);
		}
		else if (typeString.equals(STUFFTYPE_COMPLEMENT)) {
			ComplementStuff complement = new ComplementStuff(value);
			complement.literalBase = lb;
			primitive.commit(complement);
		}
	}

	/**
	 * 进行数据内容转义。
	 * 
	 * @param buf 指定完成转义处理的缓存。
	 * @param input 指定输入数据。
	 * @return
	 */
	private static int reviseValue(ByteBuffer buf, byte[] input) {
		int length = 0;
		int inputLength = input.length;

		for (int i = 0; i < inputLength; ++i) {
			byte b = input[i];
			if (b == TOKEN_OPEN_BRACE
				|| b == TOKEN_CLOSE_BRACE
				|| b == TOKEN_OPERATE_ASSIGN
				|| b == TOKEN_OPERATE_DECLARE
				|| b == TOKEN_ESCAPE) {
				buf.put(TOKEN_ESCAPE);
				++length;
			}

			buf.put(b);
			++length;
		}

		return length;
	}

	/**
	 * 解析字面义，将字面义序列化。
	 * 
	 * @param literal 指定字面义。
	 * @return 返回序列化的字面义。
	 */
	private static byte[] parseLiteralBase(LiteralBase literal) {
		if (literal == LiteralBase.STRING) {
			return LITERALBASE_STRING_BYTES;
		}
		else if (literal == LiteralBase.JSON) {
			return LITERALBASE_JSON_BYTES;
		}
		else if (literal == LiteralBase.INT) {
			return LITERALBASE_INT_BYTES;
		}
		else if (literal == LiteralBase.LONG) {
			return LITERALBASE_LONG_BYTES;
		}
		else if (literal == LiteralBase.BOOL) {
			return LITERALBASE_BOOL_BYTES;
		}
		else if (literal == LiteralBase.BIN) {
			return LITERALBASE_BIN_BYTES;
		}
		else if (literal == LiteralBase.FLOAT) {
			return LITERALBASE_FLOAT_BYTES;
		}
		else if (literal == LiteralBase.DOUBLE) {
			return LITERALBASE_DOUBLE_BYTES;
		}
		else if (literal == LiteralBase.UINT) {
			return LITERALBASE_UINT_BYTES;
		}
		else if (literal == LiteralBase.ULONG) {
			return LITERALBASE_ULONG_BYTES;
		}
		else if (literal == LiteralBase.XML) {
			return LITERALBASE_XML_BYTES;
		}
		else {
			return null;
		}
	}

	/**
	 * 解析字面义。将字面义反序列化。
	 * 
	 * @param literal 指定序列化形式的字面义。
	 * @return 返回转义后的字面义枚举对象。
	 */
	private static LiteralBase parseLiteralBase(byte[] literal) {
		if (literal[0] == LITERALBASE_STRING_BYTES[0] && literal[1] == LITERALBASE_STRING_BYTES[1]) {
			return LiteralBase.STRING;
		}
		else if (literal[0] == LITERALBASE_JSON_BYTES[0] && literal[1] == LITERALBASE_JSON_BYTES[1]) {
			return LiteralBase.JSON;
		}
		else if (literal[0] == LITERALBASE_INT_BYTES[0] && literal[1] == LITERALBASE_INT_BYTES[1]) {
			return LiteralBase.INT;
		}
		else if (literal[0] == LITERALBASE_LONG_BYTES[0] && literal[1] == LITERALBASE_LONG_BYTES[1]) {
			return LiteralBase.LONG;
		}
		else if (literal[0] == LITERALBASE_BOOL_BYTES[0] && literal[1] == LITERALBASE_BOOL_BYTES[1]) {
			return LiteralBase.BOOL;
		}
		else if (literal[0] == LITERALBASE_BIN_BYTES[0] && literal[1] == LITERALBASE_BIN_BYTES[1]) {
			return LiteralBase.BIN;
		}
		else if (literal[0] == LITERALBASE_FLOAT_BYTES[0] && literal[1] == LITERALBASE_FLOAT_BYTES[1]) {
			return LiteralBase.FLOAT;
		}
		else if (literal[0] == LITERALBASE_DOUBLE_BYTES[0] && literal[1] == LITERALBASE_DOUBLE_BYTES[1]) {
			return LiteralBase.DOUBLE;
		}
		else if ((literal[0] == LITERALBASE_UINT_BYTES[0] && literal[1] == LITERALBASE_UINT_BYTES[1])) {
			return LiteralBase.UINT;
		}
		else if (literal[0] == LITERALBASE_ULONG_BYTES[0] && literal[1] == LITERALBASE_ULONG_BYTES[1]) {
			return LiteralBase.ULONG;
		}
		else if (literal[0] == LITERALBASE_XML_BYTES[0] && literal[1] == LITERALBASE_XML_BYTES[1]) {
			return LiteralBase.XML;
		}
		else {
			return null;
		}
	}

	/**
	 * 反序列化方言。
	 * 
	 * @param primitive 指定结果原语。
	 * @param dialectStr 指定方言数据串。
	 */
	private static void deserializeDialect(Primitive primitive, String dialectStr) {
		String[] sections = dialectStr.split(TOKEN_AT_STR);
		if (sections.length != 2) {
			return;
		}

		String dialectName = sections[0];
		String tracker = sections[1];

		// 创建方言
		Dialect dialect = DialectEnumerator.getInstance().createDialect(dialectName, tracker);
		if (null == dialect) {
			Logger.w(PrimitiveSerializer.class, "Can't create '" +  dialectName + "' dialect.");
			return;
		}

		// 关联
		primitive.capture(dialect);

		// 分析数据
		dialect.construct(primitive);
	}

	/**
	 * 将原语序列化为 JSON 格式。
	 * 
	 * @param output 指定输出数据。
	 * @param primitive 指定源原语。
	 * @throws JSONException
	 */
	public static void write(JSONObject output, Primitive primitive) throws JSONException {
		// 版本
		output.put(JSONKEY_VERSION, "1.0");

		JSONArray stuffs = new JSONArray();
		List<SubjectStuff> subjects = primitive.subjects();
		if (null != subjects) {
			for (SubjectStuff stuff : subjects) {
				JSONObject stuffJSON = new JSONObject();
				// 解析类型
				stuffJSON.put(JSONKEY_STUFFTYPE, STUFFTYPE_SUBJECT);
				// 解析数值
				writeValue(stuffJSON, stuff);
				// 写入数组
				stuffs.put(stuffJSON);
			}
		}
		List<PredicateStuff> predicates = primitive.predicates();
		if (null != predicates) {
			for (PredicateStuff stuff : predicates) {
				JSONObject stuffJSON = new JSONObject();
				// 解析类型
				stuffJSON.put(JSONKEY_STUFFTYPE, STUFFTYPE_PREDICATE);
				// 解析数值
				writeValue(stuffJSON, stuff);
				// 写入数组
				stuffs.put(stuffJSON);
			}
		}
		List<ObjectiveStuff> objectives = primitive.objectives();
		if (null != objectives) {
			for (ObjectiveStuff stuff : objectives) {
				JSONObject stuffJSON = new JSONObject();
				// 解析类型
				stuffJSON.put(JSONKEY_STUFFTYPE, STUFFTYPE_OBJECTIVE);
				// 解析数值
				writeValue(stuffJSON, stuff);
				// 写入数组
				stuffs.put(stuffJSON);
			}
		}
		List<AdverbialStuff> adverbials = primitive.adverbials();
		if (null != adverbials) {
			for (AdverbialStuff stuff : adverbials) {
				JSONObject stuffJSON = new JSONObject();
				// 解析类型
				stuffJSON.put(JSONKEY_STUFFTYPE, STUFFTYPE_ADVERBIAL);
				// 解析数值
				writeValue(stuffJSON, stuff);
				// 写入数组
				stuffs.put(stuffJSON);
			}
		}
		List<AttributiveStuff> attributives = primitive.attributives();
		if (null != attributives) {
			for (AttributiveStuff stuff : attributives) {
				JSONObject stuffJSON = new JSONObject();
				// 解析类型
				stuffJSON.put(JSONKEY_STUFFTYPE, STUFFTYPE_ATTRIBUTIVE);
				// 解析数值
				writeValue(stuffJSON, stuff);
				// 写入数组
				stuffs.put(stuffJSON);
			}
		}
		List<ComplementStuff> complements = primitive.complements();
		if (null != complements) {
			for (ComplementStuff stuff : complements) {
				JSONObject stuffJSON = new JSONObject();
				// 解析类型
				stuffJSON.put(JSONKEY_STUFFTYPE, STUFFTYPE_COMPLEMENT);
				// 解析数值
				writeValue(stuffJSON, stuff);
				// 写入数组
				stuffs.put(stuffJSON);
			}
		}

		// 所有语素
		output.put(JSONKEY_STUFFS, stuffs);

		// 方言
		Dialect dialect = primitive.getDialect();
		if (null != dialect) {
			JSONObject dialectJSON = new JSONObject();
			dialectJSON.put(JSONKEY_NAME, dialect.getName());
			dialectJSON.put(JSONKEY_TRACKER, dialect.getTracker());
			// 添加方言数据
			output.put(JSONKEY_DIALECT, dialectJSON);
		}
	}

	/**
	 * 从 JSON 数据里反序列化原语。
	 * 
	 * @param output 指定目标原语。
	 * @param json 指定源 JSON 格式数据。
	 * @throws JSONException
	 */
	public static void read(Primitive output, JSONObject json) throws JSONException {
		// 解析语素
		JSONArray stuffs = json.getJSONArray(JSONKEY_STUFFS);
		for (int i = 0, size = stuffs.length(); i < size; ++i) {
			JSONObject stuffJSON = stuffs.getJSONObject(i);
			String type = stuffJSON.getString(JSONKEY_STUFFTYPE);
			if (type.equals(STUFFTYPE_SUBJECT)) {
				SubjectStuff subject = new SubjectStuff();
				readValue(subject, stuffJSON);
				output.commit(subject);
			}
			else if (type.equals(STUFFTYPE_PREDICATE)) {
				PredicateStuff predicate = new PredicateStuff();
				readValue(predicate, stuffJSON);
				output.commit(predicate);
			}
			else if (type.equals(STUFFTYPE_OBJECTIVE)) {
				ObjectiveStuff objective = new ObjectiveStuff();
				readValue(objective, stuffJSON);
				output.commit(objective);
			}
			else if (type.equals(STUFFTYPE_ATTRIBUTIVE)) {
				AttributiveStuff attributive = new AttributiveStuff();
				readValue(attributive, stuffJSON);
				output.commit(attributive);
			}
			else if (type.equals(STUFFTYPE_ADVERBIAL)) {
				AdverbialStuff adverbial = new AdverbialStuff();
				readValue(adverbial, stuffJSON);
				output.commit(adverbial);
			}
			else if (type.equals(STUFFTYPE_COMPLEMENT)) {
				ComplementStuff complement = new ComplementStuff();
				readValue(complement, stuffJSON);
				output.commit(complement);
			}
		}

		// 解析方言
		if (json.has(JSONKEY_DIALECT)) {
			JSONObject dialectJSON = json.getJSONObject(JSONKEY_DIALECT);
			String dialectName = dialectJSON.getString(JSONKEY_NAME);
			String tracker = dialectJSON.getString(JSONKEY_TRACKER);

			// 创建方言
			Dialect dialect = DialectEnumerator.getInstance().createDialect(dialectName, tracker);
			if (null != dialect) {
				// 关联
				output.capture(dialect);

				// 构建数据
				dialect.construct(output);
			}
			else {
				Logger.w(PrimitiveSerializer.class, "Can't create '" +  dialectName + "' dialect.");
			}
		}
	}

	/**
	 * 写入语素对应的数值。
	 * 
	 * @param output 指定接收数据的 JSON 对象。
	 * @param stuff 指定语素。
	 * @throws JSONException
	 */
	private static void writeValue(JSONObject output, Stuff stuff) throws JSONException {
		if (stuff.literalBase == LiteralBase.STRING) {
			output.put(JSONKEY_STUFFVALUE, stuff.getValueAsString());
			output.put(JSONKEY_LITERALBASE, LITERALBASE_STRING);
		}
		else if (stuff.literalBase == LiteralBase.JSON) {
			output.put(JSONKEY_STUFFVALUE, stuff.getValueAsJSON());
			output.put(JSONKEY_LITERALBASE, LITERALBASE_JSON);
		}
		else if (stuff.literalBase == LiteralBase.INT) {
			output.put(JSONKEY_STUFFVALUE, stuff.getValueAsInt());
			output.put(JSONKEY_LITERALBASE, LITERALBASE_INT);
		}
		else if (stuff.literalBase == LiteralBase.LONG) {
			output.put(JSONKEY_STUFFVALUE, stuff.getValueAsLong());
			output.put(JSONKEY_LITERALBASE, LITERALBASE_LONG);
		}
		else if (stuff.literalBase == LiteralBase.BOOL) {
			output.put(JSONKEY_STUFFVALUE, stuff.getValueAsBool());
			output.put(JSONKEY_LITERALBASE, LITERALBASE_BOOL);
		}
		else if (stuff.literalBase == LiteralBase.BIN) {
			String base64 = Base64.encodeBytes(stuff.getValue());
			output.put(JSONKEY_STUFFVALUE, base64);
			output.put(JSONKEY_LITERALBASE, LITERALBASE_BIN);
		}
		else if (stuff.literalBase == LiteralBase.FLOAT) {
			output.put(JSONKEY_STUFFVALUE, stuff.getValueAsFloat());
			output.put(JSONKEY_LITERALBASE, LITERALBASE_FLOAT);
		}
		else if (stuff.literalBase == LiteralBase.DOUBLE) {
			output.put(JSONKEY_STUFFVALUE, stuff.getValueAsDouble());
			output.put(JSONKEY_LITERALBASE, LITERALBASE_DOUBLE);
		}
		else if (stuff.literalBase == LiteralBase.UINT) {
			output.put(JSONKEY_STUFFVALUE, stuff.getValueAsInt());
			output.put(JSONKEY_LITERALBASE, LITERALBASE_UINT);
		}
		else if (stuff.literalBase == LiteralBase.ULONG) {
			output.put(JSONKEY_STUFFVALUE, stuff.getValueAsLong());
			output.put(JSONKEY_LITERALBASE, LITERALBASE_ULONG);
		}
		else if (stuff.literalBase == LiteralBase.XML) {
			Logger.e(PrimitiveSerializer.class, "Don't support XML literal in JSON format.");
		}
	}

	/**
	 * 读取语素对应的值。
	 * 
	 * @param output 指定接收数据的语素。
	 * @param json 指定源数据 JSON 对象。
	 * @throws JSONException
	 */
	private static void readValue(Stuff output, JSONObject json) throws JSONException {
		String literal = json.getString(JSONKEY_LITERALBASE);
		if (literal.equals(LITERALBASE_STRING)) {
			output.setValue(json.getString(JSONKEY_STUFFVALUE));
			output.setLiteralBase(LiteralBase.STRING);
		}
		else if (literal.equals(LITERALBASE_JSON)) {
			output.setValue(json.getJSONObject(JSONKEY_STUFFVALUE));
			output.setLiteralBase(LiteralBase.JSON);
		}
		else if (literal.equals(LITERALBASE_INT)) {
			output.setValue(json.getInt(JSONKEY_STUFFVALUE));
			output.setLiteralBase(LiteralBase.INT);
		}
		else if (literal.equals(LITERALBASE_LONG)) {
			output.setValue(json.getLong(JSONKEY_STUFFVALUE));
			output.setLiteralBase(LiteralBase.LONG);
		}
		else if (literal.equals(LITERALBASE_BOOL)) {
			output.setValue(json.getBoolean(JSONKEY_STUFFVALUE));
			output.setLiteralBase(LiteralBase.BOOL);
		}
		else if (literal.equals(LITERALBASE_BIN)) {
			byte[] base64 = null;
			try {
				base64 = Base64.decode(json.getString(JSONKEY_STUFFVALUE));
			} catch (IOException e) {
				// Nothing
			}
			if (null != base64) {
				output.setValue(base64);
				output.setLiteralBase(LiteralBase.BIN);
			}
		}
		else if (literal.equals(LITERALBASE_FLOAT)) {
			output.setValue(json.getDouble(JSONKEY_STUFFVALUE));
			output.setLiteralBase(LiteralBase.FLOAT);
		}
		else if (literal.equals(LITERALBASE_DOUBLE)) {
			output.setValue(json.getDouble(JSONKEY_STUFFVALUE));
			output.setLiteralBase(LiteralBase.DOUBLE);
		}
		else if (literal.equals(LITERALBASE_UINT)) {
			output.setValue(json.getInt(JSONKEY_STUFFVALUE));
			output.setLiteralBase(LiteralBase.UINT);
		}
		else if (literal.equals(LITERALBASE_ULONG)) {
			output.setValue(json.getLong(JSONKEY_STUFFVALUE));
			output.setLiteralBase(LiteralBase.ULONG);
		}
		else if (literal.equals(LITERALBASE_XML)) {
			Logger.e(PrimitiveSerializer.class, "Don't support XML literal in JSON format.");
		}
	}

}
