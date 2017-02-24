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

package net.cellcloud.cluster.protocol;

import net.cellcloud.cluster.ClusterFailure;
import net.cellcloud.cluster.ClusterNode;
import net.cellcloud.common.Session;

/**
 * 集群模块故障状态协议。
 * 
 * @author Ambrose Xu
 * 
 */
public class ClusterFailureProtocol extends ClusterProtocol {

	/**
	 * 故障描述。
	 */
	protected ClusterFailure failure;

	/**
	 * 描述故障的子协议。
	 */
	protected ClusterProtocol protocol;

	/**
	 * 构造函数。
	 * 
	 * @param failure
	 * @param protocol
	 */
	public ClusterFailureProtocol(ClusterFailure failure, ClusterProtocol protocol) {
		super("Failure");
		this.failure = failure;
		this.protocol = protocol;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void launch(Session session) {
		// TODO
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void respond(ClusterNode node, StateCode state, Object custom) {
		// TODO
	}

}
