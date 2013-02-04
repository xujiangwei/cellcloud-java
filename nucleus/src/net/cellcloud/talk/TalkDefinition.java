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

package net.cellcloud.talk;

/** Talk 服务器网络包定义。
 * 
 * @author Jiangwei Xu
 */
public final class TalkDefinition {
	
	// TPT - Talk Packet Tag

	// 连接询问验证
	public static final byte[] TPT_INTERROGATE = {'C', 'T', 'I', 'T'};

	// 请求验证密文结果
	public static final byte[] TPT_CHECK = {'C', 'T', 'C', 'K'};

	// 代理访问
	public static final byte[] TPT_PROXY = {'C', 'T', 'P', 'X'};

	// 协商服务能力
	public static final byte[] TPT_CONSULT = {'C', 'T', 'C', 'O'};

	// 请求 Cellet 服务
	public static final byte[] TPT_REQUEST = {'C', 'T', 'R', 'Q'};

	// Cellet 对话
	public static final byte[] TPT_DIALOGUE = {'C', 'T', 'D', 'L'};

	// 网络心跳
	public static final byte[] TPT_HEARTBEAT = {'C', 'T', 'H', 'B'};

	// 挂起指定的 Cellet 服务
	public static final byte[] TPT_SUSPEND = {'C', 'T', 'S', 'U'};

	// 恢复指定的 Cellet 服务
	public static final byte[] TPT_RESUME = {'C', 'T', 'R', 'E'};


	// 状态码
	protected static final byte[] SC_SUCCESS = {'0', '0', '0', '0'};
	protected static final byte[] SC_FAILURE = {'0', '0', '0', '1'};
	protected static final byte[] SC_FAILURE_NOCELLET = {'0', '0', '1', '0'};


	/** 判断是否是 INTERROGATE 包。
	 */
	public static boolean isInterrogate(final byte[] ptg) {
		if (ptg[2] == TPT_INTERROGATE[2] && ptg[3] == TPT_INTERROGATE[3]) {
			return true;
		}
		else {
			return false;
		}
	}

	/** 判断是否是 CHECK 包。
	 */
	public static boolean isCheck(final byte[] ptg) {
		if (ptg[2] == TPT_CHECK[2] && ptg[3] == TPT_CHECK[3]) {
			return true;
		}
		else {
			return false;
		}
	}

	/** 判断是否是 REQUEST 包。
	 */
	public static boolean isRequest(final byte[] ptg) {
		if (ptg[2] == TPT_REQUEST[2] && ptg[3] == TPT_REQUEST[3]) {
			return true;
		}
		else {
			return false;
		}
	}

	/** 判断是否是 CONSULT 包。
	 */
	public static boolean isConsult(final byte[] ptg) {
		if (ptg[2] == TPT_CONSULT[2] && ptg[3] == TPT_CONSULT[3]) {
			return true;
		}
		else {
			return false;
		}
	}

	/** 判断是否是 SUSPEND 包。
	 */
	public static boolean isSuspend(final byte[] ptg) {
		if (ptg[2] == TPT_SUSPEND[2] && ptg[3] == TPT_SUSPEND[3]) {
			return true;
		}
		else {
			return false;
		}
	}

	/** 判断是否是 RESUME 包。
	 */
	public static boolean isResume(final byte[] ptg) {
		if (ptg[2] == TPT_RESUME[2] && ptg[3] == TPT_RESUME[3]) {
			return true;
		}
		else {
			return false;
		}
	}

	/** 判断是否是 DIALOGUE 包。
	 */
	public static boolean isDialogue(final byte[] ptg) {
		if (ptg[2] == TPT_DIALOGUE[2] && ptg[3] == TPT_DIALOGUE[3]) {
			return true;
		}
		else {
			return false;
		}
	}

	/** 判断是否是 HEARTBEAT 包。
	 */
	public static boolean isHeartbeat(final byte[] ptg) {
		if (ptg[2] == TPT_HEARTBEAT[2] && ptg[3] == TPT_HEARTBEAT[3]) {
			return true;
		}
		else {
			return false;
		}
	}
}
