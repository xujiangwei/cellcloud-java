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

package net.cellcloud.gateway;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethod;

import net.cellcloud.common.Logger;
import net.cellcloud.gateway.GatewayService.Slave;
import net.cellcloud.http.CapsuleHolder;
import net.cellcloud.http.HttpHandler;
import net.cellcloud.http.HttpRequest;
import net.cellcloud.http.HttpResponse;

/**
 * HTTP 协议代理。
 * 
 * @author Ambrose Xu
 *
 */
public class HttpProxy extends HttpHandler implements CapsuleHolder {

	public final static int TIMEOUT = 5000;

	public final static String CHARSET = "UTF-8";

	public final static int BUFF_SIZE = 4096;

	private GatewayService gateway;
	private String pathSpec;

	public HttpProxy(String pathSpec, GatewayService gateway) {
		super();
		this.pathSpec = pathSpec;
		this.gateway = gateway;
	}

	@Override
	public String getPathSpec() {
		return this.pathSpec;
	}

	@Override
	public HttpHandler getHttpHandler() {
		return this;
	}

	@Override
	protected void doGet(HttpRequest request, HttpResponse response) throws IOException {
		String remoteAddress = request.getRemoteAddr().getHostString();
		Slave slave = this.gateway.refreshHttpRouting(remoteAddress);
		if (null == slave) {
			Logger.w(this.getClass(), "Proxy '" + this.pathSpec + "' has no slave: " + remoteAddress);
			return;
		}

		this.proxy(HttpMethod.GET.asString(), slave.address.getHostString(), slave.httpPort, request, response);
	}

	@Override
	protected void doPost(HttpRequest request, HttpResponse response) throws IOException {
		String remoteAddress = request.getRemoteAddr().getHostString();
		Slave slave = this.gateway.refreshHttpRouting(remoteAddress);
		if (null == slave) {
			Logger.w(this.getClass(), "Proxy '" + this.pathSpec + "' has no slave: " + remoteAddress);
			return;
		}

		this.proxy(HttpMethod.POST.asString(), slave.address.getHostString(), slave.httpPort, request, response);
	}

	@Override
	protected void doOptions(HttpRequest request, HttpResponse response) throws IOException {
		String remoteAddress = request.getRemoteAddr().getHostString();
		Slave slave = this.gateway.refreshHttpRouting(remoteAddress);
		if (null == slave) {
			Logger.w(this.getClass(), "Proxy '" + this.pathSpec + "' has no slave: " + remoteAddress);
			return;
		}

		this.proxy(HttpMethod.OPTIONS.asString(), slave.address.getHostString(), slave.httpPort, request, response);
	}

	@Override
	protected void doPut(HttpRequest request, HttpResponse response) throws IOException {
		String remoteAddress = request.getRemoteAddr().getHostString();
		Slave slave = this.gateway.refreshHttpRouting(remoteAddress);
		if (null == slave) {
			Logger.w(this.getClass(), "Proxy '" + this.pathSpec + "' has no slave: " + remoteAddress);
			return;
		}

		this.proxy(HttpMethod.PUT.asString(), slave.address.getHostString(), slave.httpPort, request, response);
	}

	@Override
	protected void doDelete(HttpRequest request, HttpResponse response) throws IOException {
		String remoteAddress = request.getRemoteAddr().getHostString();
		Slave slave = this.gateway.refreshHttpRouting(remoteAddress);
		if (null == slave) {
			Logger.w(this.getClass(), "Proxy '" + this.pathSpec + "' has no slave: " + remoteAddress);
			return;
		}

		this.proxy(HttpMethod.DELETE.asString(), slave.address.getHostString(), slave.httpPort, request, response);
	}

	private void proxy(String method, String host, int port, HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
		HttpServletRequest request = httpRequest.getServletRequest();
		HttpServletResponse response = httpResponse.getServletResponse();

		// 创建头数据
		HashMap<String, String> headers = new HashMap<String, String>();
		Enumeration<String> iter = request.getHeaderNames();
		while (iter.hasMoreElements()) {
			String header = iter.nextElement();
			headers.put(header, request.getHeader(header));
		}

		// 生成 URL
		StringBuilder url = new StringBuilder("http://");
		url.append(host).append(":").append(port);
		url.append(this.pathSpec);

		// HTTP 请求
		HashMap<String, String> responseHeaders = new HashMap<String, String>();
		ByteArrayOutputStream responseStream = new ByteArrayOutputStream(1024);
		int status = 200;
		if (method.equalsIgnoreCase("POST") || request.getInputStream().available() > 1) {
			status = this.request(method, url.toString(), headers, request.getParameterMap(), request.getInputStream(),
				responseHeaders, responseStream);
		}
		else {
			status = this.request(method, url.toString(), headers, request.getParameterMap(),
				responseHeaders, responseStream);
		}

		// 设置状态码
		response.setStatus(status);
		// 设置响应头
		for (Entry<String, String> e : responseHeaders.entrySet()) {
			response.setHeader(e.getKey(), e.getValue());
		}
		response.getOutputStream().write(responseStream.toByteArray());

		responseStream.close();
		responseStream = null;
		responseHeaders = null;
	}

	private int request(String method, String URL, Map<String, String> headers, Map<String, String[]> parameters,
			Map<String, String> responseHeaders, OutputStream responseStream) throws IOException {
		// 生成 URL
		StringBuilder urlString = new StringBuilder(URL);
		if (null != parameters && !parameters.isEmpty()) {
			urlString.append("?");
			for (Map.Entry<String, String[]> e : parameters.entrySet()) {
				String key = e.getKey();
				String[] values = e.getValue();
				for (String value : values) {
					urlString.append(key).append("=").append(value).append("&");
				}
			}
			urlString.delete(urlString.length() - 1, urlString.length());
		}

		URL url = new URL(urlString.toString());
		URLConnection connection = url.openConnection();
		HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
		httpURLConnection.setConnectTimeout(TIMEOUT);
		httpURLConnection.setRequestMethod(method);
		for (Entry<String, String> e : headers.entrySet()) {
			httpURLConnection.setRequestProperty(e.getKey(), e.getValue());
		}

		// 连接
		httpURLConnection.connect();

		// 状态码
		int responseCode = httpURLConnection.getResponseCode();

		// 读取数据
		InputStream inputStream = null;
		try {
			inputStream = httpURLConnection.getInputStream();
			byte[] buf = new byte[BUFF_SIZE];
			int length = 0;
			while ((length = inputStream.read(buf)) > 0) {
				responseStream.write(buf, 0, length);
			}
			responseStream.flush();
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (null != inputStream) {
					inputStream.close();
				}
			} catch (IOException e) {
				// Nothing
			}
		}

		urlString = null;

		Map<String, List<String>> headersFields = httpURLConnection.getHeaderFields();
		for (Entry<String, List<String>> e : headersFields.entrySet()) {
			String key = e.getKey();
			List<String> values = e.getValue();
			if (null != key && null != values && !values.isEmpty()) {
				responseHeaders.put(key, values.get(0));
			}
		}

		return responseCode;
	}

	private int request(String method, String URL, Map<String, String> headers, Map<String, String[]> parameters, InputStream dataStream,
			Map<String, String> responseHeaders, OutputStream responseStream) throws IOException {
		// 生成 URL
		StringBuilder urlString = new StringBuilder(URL);
		if (null != parameters && !parameters.isEmpty()) {
			urlString.append("?");
			for (Map.Entry<String, String[]> e : parameters.entrySet()) {
				String key = e.getKey();
				String[] values = e.getValue();
				for (String value : values) {
					urlString.append(key).append("=").append(value).append("&");
				}
			}
			urlString.delete(urlString.length() - 1, urlString.length());
		}

		URL url = new URL(URL);
		URLConnection connection = url.openConnection();
		HttpURLConnection httpURLConnection = (HttpURLConnection) connection;

		httpURLConnection.setDoOutput(true);
		httpURLConnection.setDoInput(true);
		httpURLConnection.setConnectTimeout(TIMEOUT);
		httpURLConnection.setRequestMethod(method);
		for (Entry<String, String> e : headers.entrySet()) {
			httpURLConnection.setRequestProperty(e.getKey(), e.getValue());
		}

		// 写入数据
		OutputStream outputStream = null;
		try {
			outputStream = httpURLConnection.getOutputStream();
			byte[] buf = new byte[BUFF_SIZE];
			int length = 0;
			while ((length = dataStream.read(buf)) > 0) {
				outputStream.write(buf, 0, length);
			}
			outputStream.flush();
		} catch (IOException e) {
			throw e;
		} finally {
			if (null != outputStream) {
				try {
					outputStream.close();
				} catch (IOException e) {
					// Nothing
				}
			}
		}

		// 获得响应码
		int responseCode = httpURLConnection.getResponseCode();

		// 读取数据
		InputStream inputStream = null;
		try {
			inputStream = httpURLConnection.getInputStream();
			byte[] buf = new byte[BUFF_SIZE];
			int length = 0;
			while ((length = inputStream.read(buf)) > 0) {
				responseStream.write(buf, 0, length);
			}
			responseStream.flush();
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (null != inputStream) {
					inputStream.close();
				}
			} catch (IOException e) {
				// Nothing
			}
		}

		urlString = null;

		Map<String, List<String>> headersFields = httpURLConnection.getHeaderFields();
		for (Entry<String, List<String>> e : headersFields.entrySet()) {
			String key = e.getKey();
			List<String> values = e.getValue();
			if (null != key && null != values && !values.isEmpty()) {
				responseHeaders.put(key, values.get(0));
			}
		}

		return responseCode;
	}

}
