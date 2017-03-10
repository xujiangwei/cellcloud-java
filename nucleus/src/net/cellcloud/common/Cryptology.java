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

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.cellcloud.util.Base64;
import net.cellcloud.util.RC4;

/**
 * 加解密库。
 * 
 * @author Ambrose Xu
 * 
 */
public final class Cryptology {

	/** 单例对象。 */
	private static final Cryptology instance = new Cryptology();

	/** 十六进制数字。 */
	private static final char HEX_DIGITS[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'a', 'b', 'c', 'd', 'e', 'f'};

	private Cryptology() {
	}

	/**
	 * 获得加解密库对象的实例。
	 * 
	 * @return 返回加解密库对象的实例。
	 */
	public static Cryptology getInstance() {
		return instance;
	}

	/**
	 * 简单加密操作。密钥长度为 8 位。
	 * 
	 * @param plaintext 指定待加密明文。
	 * @param key 指定密钥。
	 * @return 返回存储加密后密文的数组。
	 */
	public byte[] simpleEncrypt(byte[] plaintext, byte[] key) {
		if (key.length != 8)
			return null;

		// 运算密钥
		int keyCode = 11 + key[0];
		keyCode -= key[1];
		keyCode += key[2];
		keyCode -= key[3];
		keyCode += key[4];
		keyCode -= key[5];
		keyCode += key[6];
		keyCode -= key[7];

		// 评价
		byte cc = (byte) (keyCode % 8);
		byte parity = (byte) (((keyCode % 2) == 0) ? 2 : 1);

		int length = plaintext.length;
		byte[] out = new byte[length];

		for (int i = 0; i < length; ++i) {
			byte c = (byte) (plaintext[i] ^ parity);
			out[i] = (byte) (c ^ cc);
		}

		return out;
	}

	/**
	 * 简单解密操作。密钥长度为 8 位。
	 * 
	 * @param ciphertext 指定待解密密文。
	 * @param key 指定密钥。
	 * @return 返回存储解密后明文的数组。
	 */
	public byte[] simpleDecrypt(byte[] ciphertext, byte[] key) {
		if (key.length != 8)
			return null;

		// 运算密钥
		int keyCode = 11 + key[0];
		keyCode -= key[1];
		keyCode += key[2];
		keyCode -= key[3];
		keyCode += key[4];
		keyCode -= key[5];
		keyCode += key[6];
		keyCode -= key[7];

		// 评价
		byte cc = (byte) (keyCode % 8);
		byte parity = (byte) (((keyCode % 2) == 0) ? 2 : 1);

		int length = ciphertext.length;
		byte[] out = new byte[length];

		for (int i = 0; i < length; ++i) {
			byte c = (byte) (ciphertext[i] ^ cc);
			out[i] = (byte) (c ^ parity);
		}

		return out;
	}

	public byte[] encryptWithRC4(byte[] plaintext, byte[] key) {
		return RC4.encrypt(plaintext, key);
	}

	public byte[] decryptWithRC4(byte[] ciphertext, byte[] key) {
		return RC4.decrypt(ciphertext, key);
	}

	/**
	 * 快速生成散列码。
	 * 
	 * @param string 指定待操作的字符串。
	 * @return 返回散列码。
	 */
	public long fastHash(String string) {
		long h = 0;
		byte[] bytes = string.getBytes(Charset.forName("UTF-8"));
		for (byte b : bytes) {
			h = 31*h + b;
		}
		return h;
	}

	/**
	 * 快速生成散列码。
	 * 
	 * @param input 指定待操作的字节数组。
	 * @return 返回散列码。
	 */
	public long fastHash(byte[] input) {
		long h = 0;
		for (byte b : input) {
			h = 31*h + b;
		}
		return h;
	}

	/**
	 * 使用 MD5 算法生成数据摘要。
	 * 
	 * @param input 指定待操作的数据数组。
	 * @return 返回 MD5 算法生成的数据摘要。
	 */
	public byte[] hashWithMD5(byte[] input) {
		byte[] bytes = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(input);
			bytes = md.digest();
		} catch (NoSuchAlgorithmException e) {
			Logger.log(Cryptology.class, e, LogLevel.ERROR);
		}
		return bytes;
	}

	/** 生成 MD5 散列码。
	 */
	/**
	 * 使用 MD5 算法生成数据摘要。
	 * 
	 * @param input 指定待操作的数据数组。
	 * @return 返回 MD5 算法生成的数据摘要的字符串形式。
	 */
	public String hashWithMD5AsString(byte[] input) {
		byte[] md5 = this.hashWithMD5(input);
		char[] str = new char[md5.length * 2];
		int index = 0;
		for (int i = 0, size = md5.length; i < size; ++i) {
			byte b = md5[i];
			str[index++] = HEX_DIGITS[b >> 4 & 0xF];
			str[index++] = HEX_DIGITS[b & 0xF];
		}
		return new String(str);
	}

	/**
	 * 使用 Base64 编码数据。
	 * 
	 * @param source 指定待操作数据源。
	 * @return 返回编码后字符串形式的数据。
	 */
	public String encodeBase64(byte[] source) {
		return Base64.encodeBytes(source);
	}

	/**
	 * 使用 Base64 解码数据。
	 * 
	 * @param source 指定待操作数据源。
	 * @return 返回解码后的原文数据。
	 */
	public byte[] decodeBase64(String source) {
		byte[] result = null;
		try {
			result = Base64.decode(source);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	/*public static void main(String[] args) {
		String text = "这是一段测试文本，来自 shixinyun.com @ 2016.12.22";
		byte[] srcText = text.getBytes(Charset.forName("UTF-8"));
		byte[] key = "abc_091#".getBytes();

		long time = System.currentTimeMillis();
		for (int i = 0; i < 10; ++i) {
			byte[] result = Cryptology.getInstance().simpleEncrypt(srcText, key);
			Cryptology.getInstance().simpleDecrypt(result, key);

			result = Cryptology.getInstance().encryptWithRC4(srcText, key);
			Cryptology.getInstance().decryptWithRC4(result, key);
		}

		byte[] ciphertext = null;
		byte[] plaintext = null;

		long d = System.currentTimeMillis() - time;
		System.out.println("Warmup: " + d);

		// simple

		time = System.currentTimeMillis();
		for (int i = 0; i < 10000; ++i) {
			ciphertext = Cryptology.getInstance().simpleEncrypt(srcText, key);
		}
		d = System.currentTimeMillis() - time;
		System.out.println("simpleEncrypt: " + d + " ms");

		time = System.currentTimeMillis();
		for (int i = 0; i < 10000; ++i) {
			plaintext = Cryptology.getInstance().simpleDecrypt(ciphertext, key);
		}
		d = System.currentTimeMillis() - time;
		System.out.println("simpleDecrypt: " + d + " ms");
		System.out.println("content: " + new String(plaintext, Charset.forName("UTF-8")));

		// RC4

		time = System.currentTimeMillis();
		for (int i = 0; i < 10000; ++i) {
			ciphertext = Cryptology.getInstance().encryptWithRC4(srcText, key);
		}
		d = System.currentTimeMillis() - time;
		System.out.println("encryptWithRC4: " + d + " ms");

		time = System.currentTimeMillis();
		for (int i = 0; i < 10000; ++i) {
			plaintext = Cryptology.getInstance().decryptWithRC4(ciphertext, key);
		}
		d = System.currentTimeMillis() - time;
		System.out.println("decryptWithRC4: " + d + " ms");
		System.out.println("content: " + new String(plaintext, Charset.forName("UTF-8")));
	}*/
}
