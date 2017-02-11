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

package net.cellcloud.adapter;

import java.util.concurrent.ConcurrentHashMap;

import net.cellcloud.core.Endpoint;

/**
 */
public class FeedbackController {

	private ConcurrentHashMap<String, Feedback> feedbackMap;

	public FeedbackController() {
		this.feedbackMap = new ConcurrentHashMap<String, Feedback>();
	}

	public int getPositiveCounts(String keyword, Endpoint endpoint) {
		Feedback feedback = this.feedbackMap.get(keyword);
		if (null == feedback) {
			return 0;
		}

		return feedback.countPositive(endpoint);
	}

	public int getNegativeCounts(String keyword, Endpoint endpoint) {
		Feedback feedback = this.feedbackMap.get(keyword);
		if (null == feedback) {
			return 0;
		}

		return feedback.countNegative(endpoint);
	}

	public void updateEncourage(String keyword, Endpoint endpoint) {
		Feedback feedback = this.feedbackMap.get(keyword);
		if (null != feedback) {
			feedback.updatePositive(endpoint);
		}
		else {
			feedback = new Feedback(keyword);
			feedback.updatePositive(endpoint);
			this.feedbackMap.put(keyword, feedback);
		}
	}

	public void updateDiscourage(String keyword, Endpoint endpoint) {
		Feedback feedback = this.feedbackMap.get(keyword);
		if (null != feedback) {
			feedback.updateNegative(endpoint);
		}
		else {
			feedback = new Feedback(keyword);
			feedback.updateNegative(endpoint);
			this.feedbackMap.put(keyword, feedback);
		}
	}

}
