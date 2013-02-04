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

package net.cellcloud.util;

import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;

/** 实用函数库。
 * 
 * @author Jiangwei Xu
 */
public final class Util {

	// 常用日期格式
	public final static SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private final static Random sRandom = new Random(System.currentTimeMillis());

	// 字母表
	private static final char[] ALPHABET = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K',
		'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
		'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
		'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

	/** 生成随机长整数。
	 */
	public static long randomLong() {
		return sRandom.nextLong();
	}

	/** 生成随机整数。
	 */
	public static int randomInt() {
		return sRandom.nextInt();
	}

	/** 生成指定范围内的随机整数。
	 */
	public static int randomInt(int floor, int ceil) {
		if (floor > ceil) {
			return floor;
		}

		return sRandom.nextInt(ceil) % (ceil - floor + 1) + floor;
	}

	/** 生成随机字符串。
	 */
	public static String randomString(int length) {
		char[] buf = new char[length];
		int max = ALPHABET.length - 1;
		int min = 0;
		int index = 0;
		for (int i = 0; i < length; ++i) {
			index = sRandom.nextInt(max)%(max-min+1) + min;
			buf[i] = ALPHABET[index];
		}
		return new String(buf);
	}

	/** 转换日期为字符串形式。
	 */
	public static String convertDateToSimpleString(Date date) {
		return sDateFormat.format(date);
	}
	/** 转换字符串形式为日期。
	 */
	public static Date convertSimpleStringToDate(String string) {
		try {
			return sDateFormat.parse(string);
		} catch (ParseException e) {
			Logger.logException(e, LogLevel.ERROR);
		}

		return null;
	}

	/** Byte 数组转 UTF-8 字符串。
	 */
	public static String bytes2String(byte[] bytes) {
		return new String(bytes, Charset.forName("UTF-8"));
	}
	/** 字符串转 UTF-8 Byte 数组。 
	 */
	public static byte[] string2Bytes(String string) {
		return string.getBytes(Charset.forName("UTF-8"));
	}

	/** 操作系统是否是 Windows 系统。
	 */
	public static boolean isWindowsOS() {
		String os = System.getProperties().getProperty("os.name");
		return os.startsWith("Win") || os.startsWith("win");
	}

	/** 判断地址是否是 IPv4 格式。
	 */
	public static boolean isIPv4(String address) {
		if (address.replaceAll("\\d", "").length() == 3) {
			return true;
		}
		else {
			return false;
		}
	}

	/** 拆分 IPv4 地址。
	 */
	public static int[] splitIPv4Address(String ipAddress) {
		String[] ipSplit = ipAddress.split("\\.");
		int[] ip = new int[ipSplit.length];
		if (ipSplit.length == 4) {
			for (int i = 0; i < ipSplit.length; ++i) {
				ip[i] = Integer.parseInt(ipSplit[i]);
			}
		}
		return ip;  
    }

	/** 转换 IPv4 掩码为 IPv4 格式。
	 */
	public static int[] convertIPv4NetworkPrefixLength(short length) {
		switch (length) {
		case 8:
			return new int[]{255, 0, 0, 0};
		case 16:
			return new int[]{255, 255, 0, 0};
		case 24:
			return new int[]{255, 255, 255, 0};
		default:
			return new int[]{255, 255, 255, 255};
		}
	}
}
