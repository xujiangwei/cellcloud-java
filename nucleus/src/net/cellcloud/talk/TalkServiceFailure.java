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

import java.util.List;
import java.util.Vector;

/**
 * 故障描述类。
 * 
 * @author Ambrose Xu
 * 
 */
public final class TalkServiceFailure {

	/**
	 * 故障码。
	 */
	private TalkFailureCode code = null;

	/**
	 * 故障原因。
	 */
	private String reason = null;

	/**
	 * 故障描述。
	 */
	private String description = null;

	/**
	 * 故障发生源的描述内容。
	 */
	private String sourceDescription = "";

	/**
	 * 故障相关的 Cellet 标识。
	 */
	private Vector<String> sourceCelletIdentifiers = new Vector<String>(2);

	/**
	 * 故障相关连接的地址。
	 */
	private String host;

	/**
	 * 故障相关连接的端口。
	 */
	private int port;

	/**
	 * 构造函数。
	 * 
	 * @param code 故障代码。
	 * @param clazz 发生故障的类文件。
	 * @param host 发生故障时的连接地址，可以为 <code>null</code> 值。
	 * @param port 发成故障时的连接端口，可以为 <code>0</code> 值。
	 */
	public TalkServiceFailure(TalkFailureCode code, Class<?> clazz, String host, int port) {
		this.code = code;
		this.host = host;
		this.port = port;
		this.reason = "Error in " + clazz.getName();

		if (code == TalkFailureCode.NOT_FOUND)
			this.description = "Server can not find specified cellet";
		else if (code == TalkFailureCode.CALL_FAILED)
			this.description = "Network connecting timeout";
		else if (code == TalkFailureCode.TALK_LOST)
			this.description = "Lost talk connection";
		else if (code == TalkFailureCode.NETWORK_NOT_AVAILABLE)
			this.description = "Network not available";
		else if (code == TalkFailureCode.INCORRECT_DATA)
			this.description = "Incorrect data";
		else if (code == TalkFailureCode.RETRY_END)
			this.description = "Auto retry end";
		else
			this.description = "No failure description";
	}

	/**
	 * 获得故障码。故障码参看 {@link TalkFailureCode} 。
	 * 
	 * @return 返回故障码。
	 */
	public TalkFailureCode getCode() {
		return this.code;
	}

	/**
	 * 获得故障相关的主机地址。
	 * 
	 * @return 返回相关的主机地址。
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * 获得故障相关的主机端口。
	 * 
	 * @return 返回相关的主机端口。
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * 获得故障原因。
	 * 
	 * @return 返回字符串形式的故障原因。
	 */
	public String getReason() {
		return this.reason;
	}

	/**
	 * 获得故障的内容描述。
	 * 
	 * @return 返回字符串形式的故障内容描述。
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * 获得故障源描述。
	 * 
	 * @return 返回字符串形式的故障源描述。
	 */
	public String getSourceDescription() {
		return this.sourceDescription;
	}

	/**
	 * 填写故障源描述。
	 * 
	 * @param desc 故障源描述内容。
	 */
	public void setSourceDescription(String desc) {
		this.sourceDescription = desc;
	}

	/**
	 * 获得与故障相关的 Cellet 标识。
	 * 
	 * @return 返回与故障相关的 Cellet 标识。
	 */
	public List<String> getSourceCelletIdentifierList() {
		return this.sourceCelletIdentifiers;
	}

	/**
	 * 设置与故障相关的 Cellet 标识。
	 * 
	 * @param celletIdentifiers 指定 Cellet 标识列表。
	 */
	public void setSourceCelletIdentifiers(List<String> celletIdentifiers) {
		for (String identifier : celletIdentifiers) {
			if (this.sourceCelletIdentifiers.contains(identifier)) {
				continue;
			}

			this.sourceCelletIdentifiers.add(identifier);
		}
	}

}
