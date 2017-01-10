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


/** 数据包类。描述在网络上进行投递的数据包格式。
@author Jiangwei Xu
@remarks
	数据包划分为 Tag（标签）、Version（版本）、Sequence Number（序号）、
	Body Length（包体长度） 和 Body Data（包数据） 等 5 个主要数据段，
	依次简记为：TAG、VER、SEN、LEN、DAT。
	格式如下：<br />
	TAG | VER | SEN | LEN | DAT <br />
	各字段说明如下：<br />
	包标签 | 版本描述 | 包序号 | 包体长度 | 包体数据 <br />
	以上数据格式中，符号'|'表示逻辑分割，不是数据包实体数据的一部分。
	各数据域长度如下（单位：byte）：<br />
	[TAG] - 4 <br />
	[VER] - 4 <br />
	[SEN] - 4 <br />
	[LEN] - 8 <br />
	[DAT] - {!} 由 LEN 决定，最大 262144 bytes。<br />
	Packet 提供了对 DAT 段的动态定义能力。
	依次 DAT 段数据可以进行二次分解，分解为任意长度的子数据段。
	DAT 段数据格式如下：<br />
	SMN | SML{1} | ... | SML{n} | SMD{1} | ... | SMD{n} <br />
	个数据字段说明如下： <br />
	段数量 | 段1长度 | ... | 段N长度 | 段1数据 | ... | 段 N 数据 <br />
	各数据域长度如下（单位：byte）：<br />
	[SMN] - 4 <br />
	[SML] - 8 <br />
	[SMD] - {!} 由 SML 决定 <br />
	SML 和 SMD 数据一致，且由 SMN 决定。
*/
public final class Packet {
	
	protected static final int PSL_TAG = 4;
	protected static final int PSL_VERSION = 4;
	protected static final int PSL_SN = 4;
	protected static final int PSL_BODY_LENGTH = 8;
	protected static final int PSL_SUBSEGMENT_NUM = 4;
	protected static final int PSL_SUBSEGMENT_LENGTH = 8;

	private byte[] tag;
	private int sn;
	private int major;
	private int minor;

	private byte[] body;
	private ArrayList<byte[]> subsegments;

	/** 构造函数。
	 */
	public Packet(byte[] tag, int sn) {
		this.tag = tag;
		this.sn = sn;
		this.major = 1;
		this.minor = 0;
		this.body = null;
		this.subsegments = new ArrayList<byte[]>();
	}

	/** 构造函数。
	 */
	public Packet(byte[] tag, int sn, int major, int minor) {
		this.tag = tag;
		this.sn = sn;
		this.major = major;
		this.minor = minor;
		this.body = null;
		this.subsegments = new ArrayList<byte[]>();
	}

	/** 返回包标签。
	 */
	public byte[] getTag() {
		return this.tag;
	}

	/** 设置包标签。
	 */
	public void setTag(byte[] tag) {
		this.tag = tag;
	}

	/** 返回主版本号。
	 */
	public int getMajorVersion() {
		return this.major;
	}

	/** 返回副版本号。
	 */
	public int getMinorVersion() {
		return this.minor;
	}

	/** 设置版本号。
	 */
	public void setVersion(int major, int minor) {
		this.major = major;
		this.minor = minor;
	}

	/** 返回包序号。
	 */
	public int getSequenceNumber() {
		return this.sn;
	}

	/** 设置包序号。
	 */
	public void setSequenceNumber(int sn) {
		this.sn = sn;
	}

	/** 直接设置 Body 数据。
	 */
	public void setBody(byte[] body) {
		this.body = body;
	}
	/** 直接返回 Body 数据。
	 */
	public byte[] getBody() {
		return this.body;
	}

	/** 追加子段。
	 */
	public void appendSubsegment(byte[] subsegment) {
		this.subsegments.add(subsegment);
		this.body = null;
	}

	/** 获取子段。
	 */
	public byte[] getSubsegment(int index) {
		if (index < 0 || index >= this.subsegments.size())
			return null;

		return this.subsegments.get(index);
	}

	/** 返回子段数量。
	 */
	public int getSubsegmentCount() {
		return this.subsegments.size();
	}

	public int getBodyLength() {
		int len = 0;

		if (!this.subsegments.isEmpty()) {
			len += PSL_SUBSEGMENT_NUM;

			int size = this.subsegments.size();
			len += (size * PSL_SUBSEGMENT_LENGTH);

			for (int i = 0; i < size; ++i) {
				len += this.subsegments.get(i).length;
			}
		}
		else if (null != this.body) {
			len = this.body.length;
		}

		return len;
	}

	/** 打包。 */
	public static byte[] pack(Packet packet) {
		int ssNum = packet.getSubsegmentCount();

		// 计算总长度
		int bodyLength = (ssNum == 0 ? (null != packet.body ? packet.body.length : 0) : PSL_SUBSEGMENT_NUM + (ssNum * PSL_SUBSEGMENT_LENGTH));
		int totalLength = PSL_TAG + PSL_VERSION + PSL_SN + PSL_BODY_LENGTH + bodyLength;
		for (int i = 0; i < ssNum; ++i) {
			totalLength += packet.getSubsegment(i).length;
			bodyLength += packet.getSubsegment(i).length;
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
		String sLength = fastFormatNumber(bodyLength, PSL_BODY_LENGTH);
		System.arraycopy(sLength.getBytes(), 0, data, PSL_TAG + PSL_VERSION + PSL_SN, PSL_BODY_LENGTH);

		// 填写 Body 子段
		if (bodyLength > 0) {
			if (null == packet.body) {
				// 子段格式打包

				String sSubNum = fastFormatNumber(ssNum, PSL_SUBSEGMENT_NUM);
				System.arraycopy(sSubNum.getBytes(), 0, data, PSL_TAG + PSL_VERSION + PSL_SN + PSL_BODY_LENGTH, PSL_SUBSEGMENT_NUM);
	
				int begin = PSL_TAG + PSL_VERSION + PSL_SN + PSL_BODY_LENGTH + PSL_SUBSEGMENT_NUM;
	
				// 填充各子段长度
				for (int i = 0; i < ssNum; ++i) {
					int length = packet.getSubsegment(i).length;
					String sLen = fastFormatNumber(length, PSL_SUBSEGMENT_LENGTH);
					System.arraycopy(sLen.getBytes(), 0, data, begin, PSL_SUBSEGMENT_LENGTH);
					begin += PSL_SUBSEGMENT_LENGTH;
				}
	
				// 填充各子段数据
				for (int i = 0; i < ssNum; ++i) {
					byte[] subData = packet.getSubsegment(i);
					System.arraycopy(subData, 0, data, begin, subData.length);
					begin += subData.length;
				}
			}
			else {
				System.arraycopy(packet.body, 0, data, PSL_TAG + PSL_VERSION + PSL_SN + PSL_BODY_LENGTH, packet.body.length);
			}
		}

		return data;
	}

	/** 解包。 */
	public static Packet unpack(byte[] data) {
		int datalen = data.length;
		if (datalen < PSL_TAG + PSL_VERSION + PSL_SN + PSL_BODY_LENGTH) {
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
		byte[] bBodyLength = new byte[PSL_BODY_LENGTH];
		System.arraycopy(data, PSL_TAG + PSL_VERSION + PSL_SN,
				bBodyLength, 0, PSL_BODY_LENGTH);
		int bodyLength = Integer.parseInt(new String(bBodyLength));

		// 创建实例
		Packet packet = new Packet(bTag, sn, major, minor);

		if (datalen > PSL_TAG + PSL_VERSION + PSL_SN + PSL_BODY_LENGTH) {
			// 确认有 BODY 段，校验 BODY 段长度
			if ((datalen - (PSL_TAG + PSL_VERSION + PSL_SN + PSL_BODY_LENGTH)) != bodyLength) {
				Logger.w(Packet.class, "Packet length exception : bytes-length=" + datalen + " body-length=" + bodyLength);
			}

			int begin = PSL_TAG + PSL_VERSION + PSL_SN + PSL_BODY_LENGTH;

			// 判断是否符合子段分割形式
			byte[] bSubNum = new byte[PSL_SUBSEGMENT_NUM];
			System.arraycopy(data, begin, bSubNum, 0, PSL_SUBSEGMENT_NUM);
			// 判断是否是数字
			for (int i = 0; i < PSL_SUBSEGMENT_NUM; ++i) {
				if (false == Character.isDigit(bSubNum[i])) {
					// 不是数字，直接使用 Body
					byte[] body = new byte[bodyLength];
					System.arraycopy(data, begin, body, 0, bodyLength);
					packet.setBody(body);
					return packet;
				}
			}

			// 解析子段数量
			int subNum = Integer.parseInt(new String(bSubNum));
			bSubNum = null;

			int[] subsegmentLengthArray = new int[subNum];
			begin += PSL_SUBSEGMENT_NUM;

			// 解析子段长度
			for (int i = 0; i < subNum; ++i) {
				byte[] bSubLength = new byte[PSL_SUBSEGMENT_LENGTH];
				System.arraycopy(data, begin, bSubLength, 0, PSL_SUBSEGMENT_LENGTH);
				subsegmentLengthArray[i] = Integer.parseInt(new String(bSubLength));
				begin += PSL_SUBSEGMENT_LENGTH;
				bSubLength = null;
			}

			// 解析子段数据
			for (int i = 0; i < subNum; ++i) {
				int length = subsegmentLengthArray[i];
				byte[] subsegment = new byte[length];
				System.arraycopy(data, begin, subsegment, 0, length);
				begin += length;

				// 添加子段
				packet.appendSubsegment(subsegment);
			}
		}

		return packet;
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
}
