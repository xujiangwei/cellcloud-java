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

package net.cellcloud.common;

import java.util.ArrayList;

import net.cellcloud.util.ByteUtils;


/**
 * 数据包类。描述在网络上进行投递的数据包格式。
 * 
 * @author Ambrose Xu
 * 
 * @remarks
 * ---- 2.X 版本数据格式定义如下 ----
 * 数据包字段，单位 byte：
 * <code>
 * +--|-00-|-01-|-02-|-03-|-04-|-05-|-06-|-07-+
 * |--+---------------------------------------+
 * |01| VER| RES|        TAG        |    SN   |
 * |--+---------------------------------------+
 * |02|   SMN   |       SML{1}      |   ...   |
 * |--+---------------------------------------+
 * |03|   ...   |       SML{n}      |
 * |--+---------------------------------------+
 * |04|               SMD{1}                  |
 * |--+---------------------------------------+
 * |05|                ... ...                |
 * |--+---------------------------------------+
 * |06|               SMD{n}                  |
 * |--+---------------------------------------+
 * </code>
 * 说明：
 * VER - 版本描述
 * RES - 保留位
 * TAG - 包标签
 * SN - 包序号
 * SMN - 数据段数量
 * SML - 每段数据段的长度
 * SMD - 每段数据段的负载数据
 * 动态包格式，从 SML 开始为动态长度
 * 
 * <p>
 * 
 * ---- 1.X 版本数据格式定义如下 ----
 * 数据包划分为 Tag（标签）、Version（版本）、Sequence Number（序号）、
 * Body Length（包体长度） 和 Body Data（包数据） 等 5 个主要数据段，
 * 依次简记为：TAG、VER、SEN、LEN、DAT。
 * 格式如下：<br />
 * TAG | VER | SEN | LEN | DAT <br />
 * 各字段说明如下：<br />
 * 包标签 | 版本描述 | 包序号 | 包体长度 | 包体数据 <br />
 * 以上数据格式中，符号'|'表示逻辑分割，不是数据包实体数据的一部分。
 * 各数据域长度如下（单位：byte）：<br />
 * [TAG] - 4 <br />
 * [VER] - 4 <br />
 * [SEN] - 4 <br />
 * [LEN] - 8 <br />
 * [DAT] - {!} 由 LEN 决定，最大 262144 bytes。<br />
 * Packet 提供了对 DAT 段的动态定义能力。
 * 依次 DAT 段数据可以进行二次分解，分解为任意长度的子数据段。
 * DAT 段数据格式如下：<br />
 * SMN | SML{1} | ... | SML{n} | SMD{1} | ... | SMD{n} <br />
 * 个数据字段说明如下： <br />
 * 段数量 | 段1长度 | ... | 段N长度 | 段1数据 | ... | 段 N 数据 <br />
 * 各数据域长度如下（单位：byte）：<br />
 * [SMN] - 4 <br />
 * [SML] - 8 <br />
 * [SMD] - {!} 由 SML 决定 <br />
 * SML 和 SMD 数据一致，且由 SMN 决定。
 * 
 */
public final class Packet {
	
	protected static final int PSL_TAG = 4;
	protected static final int PSL_VERSION = 4;
	protected static final int PSL_SN = 4;
	protected static final int PSL_PAYLOAD_LENGTH = 8;
	protected static final int PSL_SEGMENT_NUM = 4;
	protected static final int PSL_SEGMENT_LENGTH = 8;

	/** 版本字段占用字节数。 */
	protected static final int PFB_VERSION = 1;
	/** 保留字段占用字节数。 */
	protected static final int PFB_RES = 1;
	/** 标签字段占用字节数。 */
	protected static final int PFB_TAG = 4;
	/** 序号字段占用字节数。 */
	protected static final int PFB_SN = 2;
	/** 数据段数量字段占用字节数。 */
	protected static final int PFB_SEGMENT_NUM = 2;
	/** 数据段数据长度描述字段占用字节数。 */
	protected static final int PFB_SEGMENT_LENGTH = 4;

	/** 包标签。 */
	private byte[] tag;
	/** 包序号。 */
	private int sn;
	/** 包主版本号。 */
	private int major;
	/** 包副版本号。 */
	private int minor;

	/** 包数据段数据。 */
	private ArrayList<byte[]> segments;

	/**
	 * 构造函数。
	 * 
	 * @param tag
	 * @param sn
	 */
	public Packet(byte[] tag, int sn) {
		this.tag = tag;
		this.sn = sn;
		this.major = 2;
		this.minor = 0;
		this.segments = new ArrayList<byte[]>();
	}

	/**
	 * 构造函数。
	 * 
	 * @param tag
	 * @param sn
	 * @param major
	 * @param minor
	 */
	public Packet(byte[] tag, int sn, int major, int minor) {
		this.tag = tag;
		this.sn = sn;
		this.major = major;
		this.minor = minor;
		this.segments = new ArrayList<byte[]>();
	}

	/**
	 * 获得包标签。
	 * 
	 * @return 返回包标签。
	 */
	public byte[] getTag() {
		return this.tag;
	}

	/**
	 * 获得主版本号。
	 * 
	 * @return 返回主版本号。
	 */
	public int getMajorVersion() {
		return this.major;
	}

	/**
	 * 获得副版本号。
	 * 
	 * @return 返回副版本号。
	 */
	public int getMinorVersion() {
		return this.minor;
	}

	/**
	 * 获得包序号。
	 * 
	 * @return 返回包序号。
	 */
	public int getSequenceNumber() {
		return this.sn;
	}

	/**
	 * 添加数据段。
	 * 
	 * @param segment
	 */
	public void appendSegment(byte[] segment) {
		this.segments.add(segment);
	}

	/**
	 * 获得指定索引的数据段。
	 * 
	 * @param index
	 * @return
	 */
	public byte[] getSegment(int index) {
		if (index < 0 || index >= this.segments.size())
			return null;

		return this.segments.get(index);
	}

	/**
	 * 获得数据段数量。
	 * 
	 * @return 返回数据段数量。
	 */
	public int numSegments() {
		return this.segments.size();
	}

	/**
	 * 获得包的数据负载长度。
	 * 
	 * @return 返回包的数据负载长度。
	 */
	public int getPayloadLength() {
		if (this.segments.isEmpty()) {
			return 0;
		}

		int len = 0;

		if (this.major == 2) {
			for (int i = 0, size = this.segments.size(); i < size; ++i) {
				len += PFB_SEGMENT_LENGTH;
				len += this.segments.get(i).length;
			}
		}
		else {
			len = PSL_SEGMENT_NUM;

			for (int i = 0, size = this.segments.size(); i < size; ++i) {
				len += PSL_SEGMENT_LENGTH;
				len += this.segments.get(i).length;
			}
		}

		return len;
	}

	/**
	 * 将指定的包序列化为字节数组。
	 * 
	 * @param packet
	 * @return
	 */
	public static byte[] pack(Packet packet) {
		if (packet.major == 2) {
			// 计算数据长度
			int payloadSize = packet.getPayloadLength();
			int totalSize = PFB_VERSION + PFB_RES + PFB_TAG + PFB_SN + PFB_SEGMENT_NUM + payloadSize;
			byte[] data = new byte[totalSize];
			int dataCursor = 0;

			// 填写 VER 和 RES
			byte major = (byte) packet.major;
			byte minor = (byte) packet.minor;
			data[0] = major;
			data[1] = minor;
			// 更新游标
			dataCursor = PFB_VERSION + PFB_RES;

			// 填写 TAG
			System.arraycopy(packet.tag, 0, data, dataCursor, PFB_TAG);
			// 更新游标
			dataCursor += PFB_TAG;

			// 填写 SN
			short pSN = (short) packet.sn;
			byte[] snBytes = ByteUtils.toBytes(pSN);
			System.arraycopy(snBytes, 0, data, dataCursor, PFB_SN);
			// 更新游标
			dataCursor += PFB_SN;

			// 填写 SMN
			short smn = (short) packet.segments.size();
			byte[] smnBytes = ByteUtils.toBytes(smn);
			System.arraycopy(smnBytes, 0, data, dataCursor, PFB_SEGMENT_NUM);
			// 更新游标
			dataCursor += PFB_SEGMENT_NUM;

			if (smn > 0) {
				// 填写动态的数据段长度
				for (short i = 0; i < smn; ++i) {
					int length = packet.segments.get(i).length;
					byte[] lenBytes = ByteUtils.toBytes(length);
					System.arraycopy(lenBytes, 0, data, dataCursor, PFB_SEGMENT_LENGTH);
					// 更新游标
					dataCursor += PFB_SEGMENT_LENGTH;
				}

				// 填写动态的数据段数据
				for (short i = 0; i < smn; ++i) {
					byte[] sd = packet.segments.get(i);
					System.arraycopy(sd, 0, data, dataCursor, sd.length);
					// 更新游标
					dataCursor += sd.length;
				}
			}

			return data;
		}
		else {
			int ssNum = packet.numSegments();
			if (0 == ssNum) {
				return null;
			}

			// 计算总长度
			int bodyLength = PSL_SEGMENT_NUM + (ssNum * PSL_SEGMENT_LENGTH);
			int totalLength = PSL_TAG + PSL_VERSION + PSL_SN + PSL_PAYLOAD_LENGTH + bodyLength;
			for (int i = 0; i < ssNum; ++i) {
				totalLength += packet.getSegment(i).length;
				bodyLength += packet.getSegment(i).length;
			}

			byte[] data = new byte[totalLength];

			// 填写 Tag
			System.arraycopy(packet.getTag(), 0, data, 0, PSL_TAG);

			// 填写 Version
			String sMinor = fastFormatNumber(packet.getMinorVersion(), 2);
			String sMajor = fastFormatNumber(packet.getMajorVersion(), 2);
			System.arraycopy(sMinor.getBytes(), 0, data, PSL_TAG, 2);
			System.arraycopy(sMajor.getBytes(), 0, data, PSL_TAG + 2, 2);

			// 填写 SN
			String sVersion = fastFormatNumber(packet.getSequenceNumber(), PSL_SN);
			System.arraycopy(sVersion.getBytes(), 0, data, PSL_TAG + PSL_VERSION, PSL_SN);

			// 填写 Body 段长度
			String sLength = fastFormatNumber(bodyLength, PSL_PAYLOAD_LENGTH);
			System.arraycopy(sLength.getBytes(), 0, data, PSL_TAG + PSL_VERSION + PSL_SN, PSL_PAYLOAD_LENGTH);

			// 填写 Body 子段
			// 子段格式打包
			String sSubNum = fastFormatNumber(ssNum, PSL_SEGMENT_NUM);
			System.arraycopy(sSubNum.getBytes(), 0, data, PSL_TAG + PSL_VERSION + PSL_SN + PSL_PAYLOAD_LENGTH, PSL_SEGMENT_NUM);

			int begin = PSL_TAG + PSL_VERSION + PSL_SN + PSL_PAYLOAD_LENGTH + PSL_SEGMENT_NUM;

			// 填充各子段长度
			for (int i = 0; i < ssNum; ++i) {
				int length = packet.getSegment(i).length;
				String sLen = fastFormatNumber(length, PSL_SEGMENT_LENGTH);
				System.arraycopy(sLen.getBytes(), 0, data, begin, PSL_SEGMENT_LENGTH);
				begin += PSL_SEGMENT_LENGTH;
			}

			// 填充各子段数据
			for (int i = 0; i < ssNum; ++i) {
				byte[] subData = packet.getSegment(i);
				System.arraycopy(subData, 0, data, begin, subData.length);
				begin += subData.length;
			}

			/* 2.X 版本开始不使用 BODY
			System.arraycopy(packet.body, 0, data, PSL_TAG + PSL_VERSION + PSL_SN + PSL_BODY_LENGTH, packet.body.length);
			*/

			return data;
		}
	}

	/**
	 * 将指定的数据反序列化为包对象。
	 * 
	 * @param data
	 * @return
	 * @throws NumberFormatException
	 */
	public static Packet unpack(byte[] data) throws NumberFormatException {
		byte flag = data[0];
		if (flag == 2) {
			int totalSize = data.length;
			if (totalSize < PFB_VERSION + PFB_RES + PFB_TAG + PFB_SN + PFB_SEGMENT_NUM) {
				// 数据不完整
				return null;
			}

			int dataCursor = 0;

			// 解析版本号
			int major = data[0];
			int minor = data[1];
			// 更新游标
			dataCursor = 2;

			// 解析 TAG
			byte[] tag = new byte[PFB_TAG];
			System.arraycopy(data, dataCursor, tag, 0, PFB_TAG);
			// 更新游标
			dataCursor += PFB_TAG;

			// 解析 SN
			byte[] snBytes = new byte[PFB_SN];
			System.arraycopy(data, dataCursor, snBytes, 0, PFB_SN);
			int sn = ByteUtils.toShort(snBytes);
			// 更新游标
			dataCursor += PFB_SN;

			// 解析 SMN
			byte[] smnBytes = new byte[PFB_SEGMENT_NUM];
			System.arraycopy(data, dataCursor, smnBytes, 0, PFB_SEGMENT_NUM);
			int smn = ByteUtils.toShort(smnBytes);
			// 更新游标
			dataCursor += PFB_SEGMENT_NUM;

			// 创建数据包
			Packet packet = new Packet(tag, sn, major, minor);

			if (smn > 0) {
				// 解析动态数据段长度
				int[] segmentLengths = new int[smn];
				for (int i = 0; i < smn; ++i) {
					byte[] lenBytes = new byte[PFB_SEGMENT_LENGTH];
					System.arraycopy(data, dataCursor, lenBytes, 0, PFB_SEGMENT_LENGTH);
					segmentLengths[i] = ByteUtils.toInt(lenBytes);
					// 更新游标
					dataCursor += PFB_SEGMENT_LENGTH;
				}

				// 解析动态数据段数据
				for (int i = 0; i < smn; ++i) {
					int length = segmentLengths[i];
					byte[] segment = new byte[length];
					System.arraycopy(data, dataCursor, segment, 0, length);
					packet.appendSegment(segment);
					// 更新游标
					dataCursor += length;
				}

				segmentLengths = null;
			}

			return packet;
		}
		else {
			int datalen = data.length;
			if (datalen < PSL_TAG + PSL_VERSION + PSL_SN + PSL_PAYLOAD_LENGTH) {
				return null;
			}

			// 解析 Tag
			byte[] bTag = new byte[PSL_TAG];
			System.arraycopy(data, 0, bTag, 0, PSL_TAG);

			// 解析 Version
			byte[] bMinor = new byte[2];
			byte[] bMajor = new byte[2];
			System.arraycopy(data, PSL_TAG, bMinor, 0, 2);
			System.arraycopy(data, PSL_TAG + 2, bMajor, 0, 2);
			int minor = 0;
			int major = 0;
			try {
				minor = Integer.parseInt(new String(bMinor));
				major = Integer.parseInt(new String(bMajor));
			} catch (NumberFormatException e) {
				Logger.log(Packet.class, e, LogLevel.ERROR);
				return null;
			}

			// 解析 SN
			byte[] bSN = new byte[PSL_SN];
			System.arraycopy(data, PSL_TAG + PSL_VERSION, bSN, 0, PSL_SN);
			int sn = Integer.parseInt(new String(bSN));

			// 解析 Body 段长度
			byte[] bBodyLength = new byte[PSL_PAYLOAD_LENGTH];
			System.arraycopy(data, PSL_TAG + PSL_VERSION + PSL_SN,
					bBodyLength, 0, PSL_PAYLOAD_LENGTH);
			int bodyLength = Integer.parseInt(new String(bBodyLength));

			// 创建实例
			Packet packet = new Packet(bTag, sn, major, minor);

			if (datalen > PSL_TAG + PSL_VERSION + PSL_SN + PSL_PAYLOAD_LENGTH) {
				// 确认有 BODY 段，校验 BODY 段长度
				if ((datalen - (PSL_TAG + PSL_VERSION + PSL_SN + PSL_PAYLOAD_LENGTH)) != bodyLength) {
					Logger.w(Packet.class, "Packet length exception : bytes-length=" + datalen + " body-length=" + bodyLength);
				}

				int begin = PSL_TAG + PSL_VERSION + PSL_SN + PSL_PAYLOAD_LENGTH;

				// 判断是否符合子段分割形式
				byte[] bSubNum = new byte[PSL_SEGMENT_NUM];
				System.arraycopy(data, begin, bSubNum, 0, PSL_SEGMENT_NUM);
				// 判断是否是数字
				for (int i = 0; i < PSL_SEGMENT_NUM; ++i) {
					if (false == Character.isDigit(bSubNum[i])) {
						// 不是数字，直接使用 Body
						byte[] body = new byte[bodyLength];
						System.arraycopy(data, begin, body, 0, bodyLength);
						/* 2.x 版本开始不再提供直接设置负载数据的方式
						 * packet.setBody(body);
						 */
						return packet;
					}
				}

				// 解析子段数量
				int subNum = Integer.parseInt(new String(bSubNum));
				bSubNum = null;

				int[] subsegmentLengthArray = new int[subNum];
				begin += PSL_SEGMENT_NUM;

				// 解析子段长度
				for (int i = 0; i < subNum; ++i) {
					byte[] bSubLength = new byte[PSL_SEGMENT_LENGTH];
					System.arraycopy(data, begin, bSubLength, 0, PSL_SEGMENT_LENGTH);
					subsegmentLengthArray[i] = Integer.parseInt(new String(bSubLength));
					begin += PSL_SEGMENT_LENGTH;
					bSubLength = null;
				}

				// 解析子段数据
				for (int i = 0; i < subNum; ++i) {
					int length = subsegmentLengthArray[i];
					byte[] subsegment = new byte[length];
					System.arraycopy(data, begin, subsegment, 0, length);
					begin += length;

					// 添加子段
					packet.appendSegment(subsegment);
				}
			}

			return packet;
		}
	}

	private static String fastFormatNumber(int number, int limit) {
		switch (limit) {
		case 2:
			if (number < 10) {
				return "0" + number;
			}
			else {
				return Integer.toString(number);
			}
		case 4:
			if (number < 10) {
				return "000" + number;
			}
			else if (number >= 10 && number < 100) {
				return "00" + number;
			}
			else if (number >= 100 && number < 1000) {
				return "0" + number;
			}
			else {
				return Integer.toString(number);
			}
		case 8:
			if (number < 10) {
				return "0000000" + number;
			}
			else if (number >= 10 && number < 100) {
				return "000000" + number;
			}
			else if (number >= 100 && number < 1000) {
				return "00000" + number;
			}
			else if (number >= 1000 && number < 10000) {
				return "0000" + number;
			}
			else if (number >= 10000 && number < 100000) {
				return "000" + number;
			}
			else if (number >= 100000 && number < 1000000) {
				return "00" + number;
			}
			else if (number >= 1000000 && number < 10000000) {
				return "0" + number;
			}
			else {
				return Integer.toString(number);
			}
		default:
			return Integer.toString(number);
		}
	}

	/* Just for test
	public static void main(String[] args) {
		byte[] tag = { 'X', 'I', 'A', 'O' };
		Packet packet = new Packet(tag, Utils.randomInt(1, 99), 2, 0);

		String s1 = Utils.randomString(Utils.randomInt(8, 64));
		packet.appendSegment(s1.getBytes());

		byte[] data = Packet.pack(packet);
		System.out.println("Data length: " + data.length);

		Packet sp = Packet.unpack(data);
		System.out.println("Tag: " + (sp.getTag()[0] == tag[0] && sp.getTag()[1] == tag[1]
				&& sp.getTag()[2] == tag[2] && sp.getTag()[3] == tag[3]));
		System.out.println("S1: " + (s1.equals(new String(sp.getSegment(0)))));
	}*/

}
