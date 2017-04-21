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

package net.cellcloud.talk;

/**
 * Talk 服务器网络包定义。
 * 
 * @author Ambrose Xu
 * 
 */
public final class TalkDefinition {

	// TPT - Talk Packet Tag

	/** 连接询问验证。 */
	public static final byte[] TPT_INTERROGATE = {'C', 'T', 'I', 'T'};

	/** 请求验证密文结果。 */
	public static final byte[] TPT_CHECK = {'C', 'T', 'C', 'K'};

	/** 协商服务能力。 */
	public static final byte[] TPT_CONSULT = {'C', 'T', 'C', 'O'};

	/** 请求 Cellet 服务。 */
	public static final byte[] TPT_REQUEST = {'C', 'T', 'R', 'Q'};

	/** Cellet 对话。 */
	public static final byte[] TPT_DIALOGUE = {'C', 'T', 'D', 'L'};

	/** 网络心跳。 */
	public static final byte[] TPT_HEARTBEAT = {'C', 'T', 'H', 'B'};

	/** 快速握手。 */
	public static final byte[] TPT_QUICK = {'C', 'T', 'Q', 'K'};

	/** 代理访问。 */
	public static final byte[] TPT_PROXY = {'C', 'T', 'P', 'X'};

	/** 代理对话应答。 */
	public static final byte[] TPT_PROXY_DR = {'C', 'T', 'P', 'R'};


	/** 成功状态码。 */
	public static final byte[] SC_SUCCESS = {'0', '0', '0', '0'};
	/** 失败状态码。 */
	public static final byte[] SC_FAILURE = {'0', '0', '0', '1'};
	/** 未找到 Cellet 状态码。 */
	public static final byte[] SC_FAILURE_NOCELLET = {'0', '0', '1', '0'};


	/**
	 * 判断是否是 INTERROGATE 包。
	 * 
	 * @param ptg 指定需验证的包标签。
	 * @return 如果是 INTERROGATE 包返回 <code>true</code> 。
	 */
	public static boolean isInterrogate(byte[] ptg) {
		return (ptg[2] == TPT_INTERROGATE[2] && ptg[3] == TPT_INTERROGATE[3]);
	}

	/**
	 * 判断是否是 CHECK 包。
	 * 
	 * @param ptg 指定需验证的包标签。
	 * @return 如果是 CHECK 包返回 <code>true</code> 。
	 */
	public static boolean isCheck(byte[] ptg) {
		return (ptg[2] == TPT_CHECK[2] && ptg[3] == TPT_CHECK[3]);
	}

	/**
	 * 判断是否是 REQUEST 包。
	 * 
	 * @param ptg 指定需验证的包标签。
	 * @return 如果是 REQUEST 包返回 <code>true</code> 。
	 */
	public static boolean isRequest(byte[] ptg) {
		return (ptg[2] == TPT_REQUEST[2] && ptg[3] == TPT_REQUEST[3]);
	}

	/**
	 * 判断是否是 CONSULT 包。
	 * 
	 * @param ptg 指定需验证的包标签。
	 * @return 如果是 CONSULT 包返回 <code>true</code> 。
	 */
	public static boolean isConsult(byte[] ptg) {
		return (ptg[2] == TPT_CONSULT[2] && ptg[3] == TPT_CONSULT[3]);
	}

	/**
	 * 判断是否是 QUICK 包。
	 * 
	 * @param ptg 指定需验证的包标签。
	 * @return 如果是 QUICK 包返回 <code>true</code> 。
	 */
	public static boolean isQuick(byte[] ptg) {
		return (ptg[2] == TPT_QUICK[2] && ptg[3] == TPT_QUICK[3]);
	}

	/**
	 * 判断是否是 DIALOGUE 包。
	 * 
	 * @param ptg 指定需验证的包标签。
	 * @return 如果是 DIALOGUE 包返回 <code>true</code> 。
	 */
	public static boolean isDialogue(byte[] ptg) {
		return (ptg[2] == TPT_DIALOGUE[2] && ptg[3] == TPT_DIALOGUE[3]);
	}

	/**
	 * 判断是否是 HEARTBEAT 包。
	 * 
	 * @param ptg 指定需验证的包标签。
	 * @return 如果是 HEARTBEAT 包返回 <code>true</code> 。
	 */
	public static boolean isHeartbeat(byte[] ptg) {
		return (ptg[2] == TPT_HEARTBEAT[2] && ptg[3] == TPT_HEARTBEAT[3]);
	}

	/**
	 * 判断是否是 PROXY 包。
	 * 
	 * @param ptg 指定需验证的包标签。
	 * @return 如果是 PROXY 包返回 <code>true</code> 。
	 */
	public static boolean isProxy(byte[] ptg) {
		return (ptg[2] == TPT_PROXY[2] && ptg[3] == TPT_PROXY[3]);
	}

	/**
	 * 判断是否是 PROXY DIALOGUE RESPONSE 包。
	 * 
	 * @param ptg 指定需验证的包标签。
	 * @return 如果是 PROXY DIALOGUE RESPONSE 包返回 <code>true</code> 。
	 */
	public static boolean isProxyDialogueResponse(byte[] ptg) {
		return (ptg[2] == TPT_PROXY_DR[2] && ptg[3] == TPT_PROXY_DR[3]);
	}

}
