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

package net.cellcloud.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.regex.Pattern;

import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;

/**
 * 实用函数库。
 * 
 * @author Ambrose Xu
 * 
 */
public final class Utils {

	/**
	 * 常用日期格式。
	 */
	public final static SimpleDateFormat gsDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * 序列号头格式。
	 */
	private final static SimpleDateFormat sFormatter = new SimpleDateFormat("yyMMdd");

	/**
	 * 随机数发生器。
	 */
	private final static Random sRandom = new Random(System.currentTimeMillis());

	/**
	 * 字母表。
	 */
	private static final char[] ALPHABET = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K',
		'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
		'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
		'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

	/**
	 * 数字表。
	 */
	private static final char[] NUMERATION = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

	/**
	 * 生成随机长整数。
	 */
	public static long randomLong() {
		return sRandom.nextLong();
	}

	/**
	 * 生成随机整数。
	 */
	public static int randomInt() {
		return sRandom.nextInt();
	}

	/**
	 * 生成指定范围内的随机整数。
	 * 
	 * @param floor 指定起始值（包含）。
	 * @param ceil 指定结束值（包含）。
	 * @return
	 */
	public static int randomInt(final int floor, final int ceil) {
		if (floor > ceil) {
			return floor;
		}

		int realFloor = floor + 1;
		int realCeil = ceil + 1;

		return (sRandom.nextInt(realCeil) % (realCeil - realFloor + 1) + realFloor) - 1;
	}

	/**
	 * 生成指定长度的随机字符串。
	 * 
	 * @param length 指定需生成字符串的长度。
	 * @return
	 */
	public static String randomString(int length) {
		char[] buf = new char[length];
		int max = ALPHABET.length - 1;
		int min = 0;
		int index = 0;
		for (int i = 0; i < length; ++i) {
			index = sRandom.nextInt(max) % (max-min+1) + min;
			buf[i] = ALPHABET[index];
		}
		return new String(buf);
	}

	/**
	 * 生成指定长度的随机数字串。
	 * 
	 * @param length 指定需生成数字串的长度。
	 * @return
	 */
	public static String randomNumberString(int length) {
		char[] buf = new char[length];
		int max = NUMERATION.length - 1;
		int min = 0;
		int index = 0;

		// 第一个字符不能是 '0'
		index = sRandom.nextInt(max) % (max-min+1) + min;
		buf[0] = NUMERATION[(index == 0 ? 1 : index)];

		for (int i = 1; i < length; ++i) {
			index = sRandom.nextInt(max) % (max-min+1) + min;
			buf[i] = NUMERATION[index];
		}

		return new String(buf);
	}

	/**
	 * 生成唯一序列码。
	 * 
	 * @return
	 */
	public static Long generateSerialNumber() {
		StringBuilder buf = new StringBuilder();
		buf.append(sFormatter.format(new Date()));

		String rnd = Integer.toString(Math.abs(Utils.randomInt()));
		switch (rnd.length()) {
		case 1:
			buf.append("000000000");
			break;
		case 2:
			buf.append("00000000");
			break;
		case 3:
			buf.append("0000000");
			break;
		case 4:
			buf.append("000000");
			break;
		case 5:
			buf.append("00000");
			break;
		case 6:
			buf.append("0000");
			break;
		case 7:
			buf.append("000");
			break;
		case 8:
			buf.append("00");
			break;
		case 9:
			buf.append("0");
			break;
		default:
			break;
		}

		buf.append(rnd);
		return Long.valueOf(buf.toString());
	}

	/**
	 * 转换日期为字符串形式。
	 * 
	 * @param date 指定日期对象。
	 * @return
	 */
	public static String convertDateToSimpleString(Date date) {
		return gsDateFormat.format(date);
	}

	/**
	 * 转换字符串形式为日期。
	 * 
	 * @param string 指定字符串形式的日期。
	 * @return
	 */
	public static Date convertSimpleStringToDate(String string) {
		try {
			return gsDateFormat.parse(string);
		} catch (ParseException e) {
			Logger.log(Utils.class, e, LogLevel.ERROR);
		}

		return null;
	}

	/**
	 * Byte 数组转 UTF-8 字符串。
	 * 
	 * @param bytes
	 * @return
	 */
	public static String bytes2String(byte[] bytes) {
		return new String(bytes, Charset.forName("UTF-8"));
	}

	/**
	 * 字符串转 UTF-8 Byte 数组。 
	 * 
	 * @param string
	 * @return
	 */
	public static byte[] string2Bytes(String string) {
		return string.getBytes(Charset.forName("UTF-8"));
	}

	/**
	 * 判断字符串是否是数字形式。
	 * 
	 * @param string 指定待判断的字符串。
	 * @return
	 */
	public static boolean isNumeral(String string) {
		Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
		return pattern.matcher(string).matches();
	}

	/**
	 * 当前 JVM 运行时操作系统是否是 Windows 系统。
	 * 
	 * @return
	 */
	public static boolean isWindowsOS() {
		String os = System.getProperties().getProperty("os.name");
		return os.startsWith("Win") || os.startsWith("win");
	}

	/**
	 * 判断字符串形式的地址是否是 IPv4 格式。
	 * 
	 * @param address
	 * @return
	 */
	public static boolean isIPv4(String address) {
		if (address.replaceAll("\\d", "").length() == 3) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * 拆解 IPv4 地址。
	 * 
	 * @param ipAddress
	 * @return
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

	/**
	 * 转换 IPv4 掩码描述为 IPv4 格式。
	 * 
	 * @param length
	 * @return
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

	/**
	 * 拷贝文件。
	 * 
	 * @param srcFile 指定源文件。
	 * @param destFile 指定目标文件。
	 * @return 拷贝成功返回 <code>true</code> 。
	 * @throws IOException
	 */
	public static long copyFile(File srcFile, File destFile) throws IOException {
		long bytesum = 0;
		int byteread = 0;

		if (srcFile.exists()) {
			FileInputStream fis = null;
			FileOutputStream fos = null;
			try {
				fis = new FileInputStream(srcFile);
				fos = new FileOutputStream(destFile);
				byte[] buffer = new byte[4096];
				while ((byteread = fis.read(buffer)) > 0) {
					bytesum += byteread;
					fos.write(buffer, 0, byteread);
				}
				fos.flush();
			} catch (IOException e) {
				throw e;
			} finally {
				if (null != fis) {
					try { fis.close(); } catch (IOException e) { }
				}
				if (null != fos) {
					try { fos.close(); } catch (IOException e) { }
				}
			}
		}

		return bytesum;
	}

}
