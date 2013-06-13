/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (www.cellcloud.net)

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

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;
import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.dialect.Dialect;
import net.cellcloud.talk.dialect.DialectEnumerator;

/** 原语序列化器。
 * 
 * @author Jiangwei Xu
 */
public final class PrimitiveSerializer {

	private static final byte TOKEN_OPEN_BRACKET = '[';
	private static final byte TOKEN_CLOSE_BRACKET = ']';
	private static final byte TOKEN_OPEN_BRACE = '{';
	private static final byte TOKEN_CLOSE_BRACE = '}';
	private static final byte TOKEN_POINT = '.';
	private static final byte TOKEN_OPERATE_ASSIGN = '=';
	private static final byte TOKEN_OPERATE_DECLARE = ':';
	private static final byte TOKEN_AT = '@';
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
	private static final String LITERALBASE_BOOL = "bool";

	private static final byte[] LITERALBASE_STRING_BYTES = LITERALBASE_STRING.getBytes();
	private static final byte[] LITERALBASE_INT_BYTES = LITERALBASE_INT.getBytes();
	private static final byte[] LITERALBASE_UINT_BYTES = LITERALBASE_UINT.getBytes();
	private static final byte[] LITERALBASE_LONG_BYTES = LITERALBASE_LONG.getBytes();
	private static final byte[] LITERALBASE_ULONG_BYTES = LITERALBASE_ULONG.getBytes();
	private static final byte[] LITERALBASE_BOOL_BYTES = LITERALBASE_BOOL.getBytes();

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

	private static final int BLOCK = 2048;

	private PrimitiveSerializer() {
	}

	/** 将原语写入数据流。
	 */
	public static void write(OutputStream stream, Primitive primitive) {
		/*
		原语序列化格式：
		[version]{sutff}...{stuff}[dialect@tracker]
		示例：
		[01.00]{sub=cloud:string}{pre=add:string}[FileReader@Lynx]
		*/

		try {
			// 版本
			stream.write((int)TOKEN_OPEN_BRACKET);
			byte[] version = {'0', '1', TOKEN_POINT, '0', '0'};
			stream.write(version);
			stream.write((int)TOKEN_CLOSE_BRACKET);

			ByteBuffer buf = ByteBuffer.allocate(BLOCK);
			int bufLength = 0;

			// 语素
			if (null != primitive.subjects()) {
				Iterator<SubjectStuff> iter = primitive.subjects().iterator();
				while (iter.hasNext()) {
					SubjectStuff stuff = iter.next();
					stream.write((int)TOKEN_OPEN_BRACE);
					stream.write(STUFFTYPE_SUBJECT_BYTES);
					stream.write((int)TOKEN_OPERATE_ASSIGN);

					bufLength = reviseValue(buf, stuff.value.getBytes(Charset.forName("UTF8")));
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
			if (null != primitive.predicates()) {
				Iterator<PredicateStuff> iter = primitive.predicates().iterator();
				while (iter.hasNext()) {
					PredicateStuff stuff = iter.next();
					stream.write((int)TOKEN_OPEN_BRACE);
					stream.write(STUFFTYPE_PREDICATE_BYTES);
					stream.write((int)TOKEN_OPERATE_ASSIGN);

					bufLength = reviseValue(buf, stuff.value.getBytes(Charset.forName("UTF8")));
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
			if (null != primitive.objectives()) {
				Iterator<ObjectiveStuff> iter = primitive.objectives().iterator();
				while (iter.hasNext()) {
					ObjectiveStuff stuff = iter.next();
					stream.write((int)TOKEN_OPEN_BRACE);
					stream.write(STUFFTYPE_OBJECTIVE_BYTES);
					stream.write((int)TOKEN_OPERATE_ASSIGN);

					bufLength = reviseValue(buf, stuff.value.getBytes(Charset.forName("UTF8")));
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
			if (null != primitive.adverbials()) {
				Iterator<AdverbialStuff> iter = primitive.adverbials().iterator();
				while (iter.hasNext()) {
					AdverbialStuff stuff = iter.next();
					stream.write((int)TOKEN_OPEN_BRACE);
					stream.write(STUFFTYPE_ADVERBIAL_BYTES);
					stream.write((int)TOKEN_OPERATE_ASSIGN);

					bufLength = reviseValue(buf, stuff.value.getBytes(Charset.forName("UTF8")));
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
			if (null != primitive.attributives()) {
				Iterator<AttributiveStuff> iter = primitive.attributives().iterator();
				while (iter.hasNext()) {
					AttributiveStuff stuff = iter.next();
					stream.write((int)TOKEN_OPEN_BRACE);
					stream.write(STUFFTYPE_ATTRIBUTIVE_BYTES);
					stream.write((int)TOKEN_OPERATE_ASSIGN);

					bufLength = reviseValue(buf, stuff.value.getBytes(Charset.forName("UTF8")));
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
			if (null != primitive.complements()) {
				Iterator<ComplementStuff> iter = primitive.complements().iterator();
				while (iter.hasNext()) {
					ComplementStuff stuff = iter.next();
					stream.write((int)TOKEN_OPEN_BRACE);
					stream.write(STUFFTYPE_COMPLEMENT_BYTES);
					stream.write((int)TOKEN_OPERATE_ASSIGN);

					bufLength = reviseValue(buf, stuff.value.getBytes(Charset.forName("UTF8")));
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
			if (null != primitive.getDialect()) {
				stream.write(TOKEN_OPEN_BRACKET);
				stream.write(primitive.getDialect().getName().getBytes(Charset.forName("UTF8")));
				stream.write(TOKEN_AT);
				stream.write(primitive.getDialect().getTracker().getBytes(Charset.forName("UTF8")));
				stream.write(TOKEN_CLOSE_BRACKET);
			}

			stream.flush();
		} catch (IOException e) {
			Logger.log(e, LogLevel.ERROR);
		}
	}

	/** 从数据流中读取原语。
	 */
	public static void read(Primitive primitive, InputStream stream) {
		/*
		原语序列化格式：
		[version]{sutff}...{stuff}[dialect@tracker]
		示例：
		[01.00]{sub=cloud:string}{pre=add:string}[FileReader@Lynx]
		*/

		try {
			byte phase = PARSE_PHASE_UNKNOWN;
			int read = 0;

			ByteBuffer buf = ByteBuffer.allocate(BLOCK);
			byte[] type = new byte[3];
			byte[] value = null;
			byte[] literal = null;
			int length = 0;

			while ((read = stream.read()) >= 0) {

				// 判断处理阶段
				switch (phase) {

				case PARSE_PHASE_VALUE:
					// 判断转义
					if (read == '\\') {
						// 读取下一个字符
						int next = stream.read();
						if (next == TOKEN_OPEN_BRACE
							|| next == TOKEN_CLOSE_BRACE
							|| next == TOKEN_OPERATE_ASSIGN
							|| next == TOKEN_OPERATE_DECLARE) {
							buf.put((byte)next);
							++length;
						}
						else
						{
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

						// 添加语素
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
						deserializeDialect(primitive, new String(buf.array(), 0, length, Charset.forName("UTF-8")));
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
			Logger.log(e, LogLevel.ERROR);
		}
	}

	/** 将数据数组解析为语素，并注入原语。
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
			SubjectStuff subject = new SubjectStuff(new String(value, Charset.forName("UTF-8")));
			subject.literalBase = lb;
			primitive.commit(subject);
		}
		else if (typeString.equals(STUFFTYPE_PREDICATE)) {
			PredicateStuff predicate = new PredicateStuff(new String(value, Charset.forName("UTF-8")));
			predicate.literalBase = lb;
			primitive.commit(predicate);
		}
		else if (typeString.equals(STUFFTYPE_OBJECTIVE)) {
			ObjectiveStuff objective = new ObjectiveStuff(new String(value, Charset.forName("UTF-8")));
			objective.literalBase = lb;
			primitive.commit(objective);
		}
		else if (typeString.equals(STUFFTYPE_ADVERBIAL)) {
			AdverbialStuff adverbial = new AdverbialStuff(new String(value, Charset.forName("UTF-8")));
			adverbial.literalBase = lb;
			primitive.commit(adverbial);
		}
		else if (typeString.equals(STUFFTYPE_ATTRIBUTIVE)) {
			AttributiveStuff attributive = new AttributiveStuff(new String(value, Charset.forName("UTF-8")));
			attributive.literalBase = lb;
			primitive.commit(attributive);
		}
		else if (typeString.equals(STUFFTYPE_COMPLEMENT)) {
			ComplementStuff complement = new ComplementStuff(new String(value, Charset.forName("UTF-8")));
			complement.literalBase = lb;
			primitive.commit(complement);
		}
	}

	/** 进行数据内容转义。
	 */
	private static int reviseValue(ByteBuffer buf, byte[] input) {
		int length = 0;
		int inputLength = input.length;

		for (int i = 0; i < inputLength; ++i) {
			byte b = input[i];
			if (b == TOKEN_OPEN_BRACE
				|| b == TOKEN_CLOSE_BRACE
				|| b == TOKEN_OPERATE_ASSIGN
				|| b == TOKEN_OPERATE_DECLARE) {
				buf.put((byte) '\\');
				++length;
			}

			buf.put(b);
			++length;
		}

		return length;
	}

	/** 解析字面义。
	 */
	private static byte[] parseLiteralBase(LiteralBase literal) {
		if (literal == LiteralBase.STRING) {
			return LITERALBASE_STRING_BYTES;
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
		else {
			return null;
		}
	}

	/** 解析字面义。
	 */
	private static LiteralBase parseLiteralBase(byte[] literal) {
		if (literal[0] == LITERALBASE_STRING_BYTES[0] && literal[1] == LITERALBASE_STRING_BYTES[1]) {
			return LiteralBase.STRING;
		}
		else if ((literal[0] == LITERALBASE_INT_BYTES[0] && literal[1] == LITERALBASE_INT_BYTES[1])
				|| (literal[0] == LITERALBASE_UINT_BYTES[0] && literal[1] == LITERALBASE_UINT_BYTES[1])) {
			return LiteralBase.INT;
		}
		else if ((literal[0] == LITERALBASE_LONG_BYTES[0] && literal[1] == LITERALBASE_LONG_BYTES[1])
				|| (literal[0] == LITERALBASE_ULONG_BYTES[0] && literal[1] == LITERALBASE_ULONG_BYTES[1])) {
			return LiteralBase.LONG;
		}
		else if (literal[0] == LITERALBASE_BOOL_BYTES[0] && literal[1] == LITERALBASE_BOOL_BYTES[1]) {
			return LiteralBase.BOOL;
		}
		else {
			return null;
		}
	}

	/** 反序列化方言
	 */
	private static void deserializeDialect(Primitive primitive, final String dialectStr) {
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
		dialect.build(primitive);
	}
}
