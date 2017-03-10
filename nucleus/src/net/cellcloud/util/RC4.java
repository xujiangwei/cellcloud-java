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


/**
 * RC4 算法实现类。
 * 
 * @author Ambrose Xu
 * 
 */
public class RC4 {

	private final byte[] s = new byte[256];

	private int x = 0;
	private int y = 0;

	/**
	 * 构造函数。
	 * 
	 * @param key 指定密钥。
	 */
	public RC4(byte[] key) {
		for (int i = 0; i < 256; i++) {
			s[i] = (byte) i;
		}

		for (int i = 0, j = 0; i < 256; i++) {
			j = (j + (s[i] & 0xff) + (key[i % key.length] & 0xff)) & 0xff;
			byte tmp = s[i];
			s[i] = s[j];
			s[j] = tmp;
		}
	}

	/**
	 * 加密数据。
	 * 
	 * @param out
	 * @param outOffset
	 * @param in
	 * @param inOffset
	 * @param len
	 * @return
	 */
	public int encrypt(byte[] out, int outOffset, byte[] in, int inOffset, int len) {
		int x = this.x;
		int y = this.y;
		byte[] s = this.s;
		for (int i = 0; i < len; i++) {
			x = (x + 1) & 0xff;
			y = (y + (s[x] & 0xff)) & 0xff;
			byte tmp = s[x];
			s[x] = s[y];
			s[y] = tmp;
			int t = ((s[x] & 0xff) + (s[y] & 0xff)) & 0xff;
			int k = s[t];
			out[outOffset + i] = (byte) ((in[inOffset + i] & 0xff) ^ k);
		}
		this.x = x;
		this.y = y;
		return len;
	}

	/**
	 * 解密数据。
	 * 
	 * @param out
	 * @param outOffset
	 * @param in
	 * @param inOffset
	 * @param len
	 * @return
	 */
	public int decrypt(byte[] out, int outOffset, byte[] in, int inOffset, int len) {
		return this.encrypt(out, outOffset, in, inOffset, len);
	}

	/**
	 * 加密数据。
	 * 
	 * @param in
	 * @return
	 */
	public byte[] encrypt(byte[] in) {
		byte[] results = new byte[in.length];
		this.encrypt(results, 0, in, 0, in.length);
		return results;
	}

	/**
	 * 解密数据。
	 * 
	 * @param in
	 * @return
	 */
	public byte[] decrypt(byte[] in) {
		byte[] results = new byte[in.length];
		this.decrypt(results, 0, in, 0, in.length);
		return results;
	}

	/**
	 * 加密数据。
	 * 
	 * @param in
	 * @param key
	 * @return
	 */
	public static byte[] encrypt(byte[] in, byte[] key) {
		byte[] s = new byte[256];
		for (int i = 0; i < 256; i++) {
			s[i] = (byte) i;
		}

		for (int i = 0, j = 0; i < 256; i++) {
			j = (j + (s[i] & 0xff) + (key[i % key.length] & 0xff)) & 0xff;
			byte tmp = s[i];
			s[i] = s[j];
			s[j] = tmp;
		}

		int len = in.length;
		byte[] out = new byte[len];
		int inOffset = 0;
		int outOffset = 0;

		int x = 0;
		int y = 0;
		for (int i = 0; i < len; i++) {
			x = (x + 1) & 0xff;
			y = (y + (s[x] & 0xff)) & 0xff;
			byte tmp = s[x];
			s[x] = s[y];
			s[y] = tmp;
			int t = ((s[x] & 0xff) + (s[y] & 0xff)) & 0xff;
			int k = s[t];
			out[outOffset + i] = (byte) ((in[inOffset + i] & 0xff) ^ k);
		}

		return out;
	}

	/**
	 * 解密数据。
	 * 
	 * @param in
	 * @param key
	 * @return
	 */
	public static byte[] decrypt(byte[] in, byte[] key) {
		return RC4.encrypt(in, key);
	}

}
