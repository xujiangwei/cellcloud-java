/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (cellcloudproject@gmail.com)

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

package net.cellcloud.core;

/** 加解密库。
 * 
 * @author Jiangwei Xu
 */
public final class Cryptology {

	private static final Cryptology instance = new Cryptology();

	private Cryptology() {
	}

	/** 返回加解密库对象的实例。
	 */
	public synchronized static Cryptology getInstance() {
		return instance;
	}

	/** 简单加密操作。密钥长度为 8 位。
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

		for (int i = 0; i < length; ++i)
		{
			byte c = (byte) (plaintext[i] ^ parity);
			out[i] = (byte) (c ^ cc);
		}

		return out;
	}

	/** 简单解密操作。密钥长度为 8 位。
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

		for (int i = 0; i < length; ++i)
		{
			byte c = (byte) (ciphertext[i] ^ cc);
			out[i] = (byte) (c ^ parity);
		}

		return out;
	}
}
