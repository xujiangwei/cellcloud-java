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

package net.cellcloud.util;

import java.nio.charset.Charset;

public final class ByteUtils {

	private ByteUtils() {
	}

	public static byte[] toBytes(short data) {
		byte[] bytes = new byte[2];
		bytes[0] = (byte) (data & 0xff);
		bytes[1] = (byte) ((data & 0xff00) >> 8);
		return bytes;
	}

	public static byte[] toBytes(char data) {
		byte[] bytes = new byte[2];
		bytes[0] = (byte) (data);
		bytes[1] = (byte) (data >> 8);
		return bytes;
	}

	public static byte[] toBytes(int data) {
		byte[] bytes = new byte[4];
		bytes[0] = (byte) (data & 0xff);
		bytes[1] = (byte) ((data & 0xff00) >> 8);
		bytes[2] = (byte) ((data & 0xff0000) >> 16);
		bytes[3] = (byte) ((data & 0xff000000) >> 24);
		return bytes;
	}

	public static byte[] toBytes(long data) {
		byte[] bytes = new byte[8];
		bytes[0] = (byte) (data & 0xff);
		bytes[1] = (byte) ((data >> 8) & 0xff);
		bytes[2] = (byte) ((data >> 16) & 0xff);
		bytes[3] = (byte) ((data >> 24) & 0xff);
		bytes[4] = (byte) ((data >> 32) & 0xff);
		bytes[5] = (byte) ((data >> 40) & 0xff);
		bytes[6] = (byte) ((data >> 48) & 0xff);
		bytes[7] = (byte) ((data >> 56) & 0xff);
		return bytes;
	}

	public static byte[] toBytes(float data) {
		int intBits = Float.floatToIntBits(data);
		return toBytes(intBits);
	}

	public static byte[] toBytes(double data) {
		long longBits = Double.doubleToLongBits(data);
		return toBytes(longBits);
	}

	public static byte[] toBytes(boolean data) {
		return new byte[]{ data ? (byte) 1 : (byte) 0 };
	}

	public static byte[] toBytes(String data, String charsetName) {
		return data.getBytes(Charset.forName(charsetName));
	}

	public static byte[] toBytes(String data) {
		return toBytes(data, "UTF-8");
	}

	public static short toShort(byte[] bytes) {
		return (short) ((0xff & bytes[0]) | (0xff00 & (bytes[1] << 8)));
	}

	public static char toChar(byte[] bytes) {
		return (char) ((0xff & bytes[0]) | (0xff00 & (bytes[1] << 8)));
	}

	public static int toInt(byte[] bytes) {
		return (0xff & bytes[0])
				| (0xff00 & (bytes[1] << 8))
				| (0xff0000 & (bytes[2] << 16))
				| (0xff000000 & (bytes[3] << 24));
	}

	public static long toLong(byte[] bytes) {
		return (0xffL & (long) bytes[0])
				| (0xff00L & ((long) bytes[1] << 8))
				| (0xff0000L & ((long) bytes[2] << 16))
				| (0xff000000L & ((long) bytes[3] << 24))
				| (0xff00000000L & ((long) bytes[4] << 32))
				| (0xff0000000000L & ((long) bytes[5] << 40))
				| (0xff000000000000L & ((long) bytes[6] << 48))
				| (0xff00000000000000L & ((long) bytes[7] << 56));
	}

	public static float toFloat(byte[] bytes) {
		return Float.intBitsToFloat(toInt(bytes));
	}

	public static double toDouble(byte[] bytes) {
		return Double.longBitsToDouble(toLong(bytes));
	}

	public static boolean toBoolean(byte[] bytes) {
		return (bytes[0] == (byte)1) ? true : false;
	}

	public static String toString(byte[] bytes, String charsetName) {
		return new String(bytes, Charset.forName(charsetName));
	}

	public static String toString(byte[] bytes) {
		return toString(bytes, "UTF-8");
	}

	public static void main(String[] args) {
		double a = -23.89d;
		byte[] bytes = toBytes(a);
		for (byte b : bytes) {
			System.out.println(b);
		}

//		short s = -12;
//		int i = -1234;
//		long l = -123456789L;
//
//		char c = 'a';
//
//		float f = -123.45f;
//		double d = -12345.67d;
//		
//		boolean b = false;
//
//		String string = "我是好孩子";
//
//		System.out.println(s);
//		System.out.println(i);
//		System.out.println(l);
//		System.out.println(c);
//		System.out.println(f);
//		System.out.println(d);
//		System.out.println(b);
//		System.out.println(string);
//
//		System.out.println("**************");
//
//		System.out.println(toShort(toBytes(s)));
//		System.out.println(toInt(toBytes(i)));
//		System.out.println(toLong(toBytes(l)));
//		System.out.println(toChar(toBytes(c)));
//		System.out.println(toFloat(toBytes(f)));
//		System.out.println(toDouble(toBytes(d)));
//		System.out.println(toBoolean(toBytes(b)));
//		System.out.println(toString(toBytes(string)));
	}
}
