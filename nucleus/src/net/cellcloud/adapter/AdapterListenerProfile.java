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

import java.lang.annotation.Annotation;

import net.cellcloud.adapter.annotation.NucleusAdapterListener;
import net.cellcloud.common.LogLevel;
import net.cellcloud.common.Logger;

public class AdapterListenerProfile {

	public AdapterListener listener;
	public String instanceName;

	public AdapterListenerProfile(AdapterListener listener, String instanceName) {
		this.listener = listener;
		this.instanceName = instanceName;
	}

	public static AdapterListenerProfile load(Class<?> clazz) {
		Annotation annotation = clazz.getAnnotation(NucleusAdapterListener.class);
		if (null == annotation) {
			return null;
		}

		NucleusAdapterListener listener = (NucleusAdapterListener) annotation;

		String instanceName = listener.instanceName();
		if (instanceName.length() <= 1) {
			return null;
		}

		AdapterListenerProfile ret = null;
		try {
			ret = new AdapterListenerProfile((AdapterListener) clazz.newInstance(), instanceName);
		} catch (InstantiationException e) {
			Logger.log(AdapterListenerProfile.class, e, LogLevel.WARNING);
		} catch (IllegalAccessException e) {
			Logger.log(AdapterListenerProfile.class, e, LogLevel.WARNING);
		}
		return ret;
	}

}
