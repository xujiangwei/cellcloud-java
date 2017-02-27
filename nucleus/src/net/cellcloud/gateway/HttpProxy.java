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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.cellcloud.Version;
import net.cellcloud.gateway.GatewayService.Slave;
import net.cellcloud.http.CapsuleHolder;
import net.cellcloud.http.HttpHandler;
import net.cellcloud.http.HttpRequest;
import net.cellcloud.http.HttpResponse;

import org.json.JSONObject;

/**
 * 
 * @author Ambrose Xu
 *
 */
public class HttpProxy extends HttpHandler implements CapsuleHolder {

	public final static int TIMEOUT = 5000;

	public final static String CHARSET = "UTF-8";

	public final static int BUFF_SIZE = 4096;

	private RoutingTable routingTable;
	private String pathSpec;

	public HttpProxy(String pathSpec, RoutingTable routingTable) {
		super();
		this.pathSpec = pathSpec;
		this.routingTable = routingTable;
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
		Slave slave = this.routingTable.querySlaveByAddress(remoteAddress);
		if (null == slave) {
			return;
		}

		this.proxy("GET", slave.address.getHostString(), slave.httpPort, request, response);
	}

	@Override
	protected void doPost(HttpRequest request, HttpResponse response) throws IOException {
	}

	@Override
	protected void doOptions(HttpRequest request, HttpResponse response) throws IOException {
	}

	@Override
	protected void doPut(HttpRequest request, HttpResponse response) throws IOException {
	}

	@Override
	protected void doDelete(HttpRequest request, HttpResponse response) throws IOException {
	}

	private void proxy(String method, String host, int port, HttpRequest httpRequest, HttpResponse httpResponse)
			throws IOException {
		HttpServletRequest request = httpRequest.getServletRequest();
		HttpServletResponse response = httpResponse.getServletResponse();

		// 生成 URL
		StringBuilder url = new StringBuilder("http://");
		url.append(host).append(":").append(port);
		url.append(this.pathSpec);

		// 进行 HTTP 访问
		ByteArrayOutputStream result = new ByteArrayOutputStream(1024);
		int status = this.request(method, url.toString(), request.getParameterMap(), result);

		// 设置状态码
		response.setStatus(status);
		response.setCharacterEncoding(CHARSET);
		response.getOutputStream().write(result.toByteArray());

		result.close();
		result = null;
	}

	private int request(String method, String URL,
			Map<String, String[]> parameters, ByteArrayOutputStream result) throws IOException {
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
		httpURLConnection.setRequestProperty("Accept-Charset", CHARSET);
		httpURLConnection.setRequestProperty("User-Agent", "Nucleus/" + Version.getNumbers());
		httpURLConnection.setRequestProperty("Version", "1.1");

		// 连接
		httpURLConnection.connect();

		// 状态码
		int responseCode = httpURLConnection.getResponseCode();

		InputStream inputStream = null;

		try {
			inputStream = httpURLConnection.getInputStream();
			byte[] buf = new byte[BUFF_SIZE];
			int length = 0;
			while ((length = inputStream.read(buf)) > 0) {
				result.write(buf, 0, length);
			}
			result.flush();
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

		return responseCode;
	}

	/**
	 * POST 请求。
	 * 
	 * @param urlString
	 * @param data
	 * @param result
	 * @return
	 * @throws IOException
	 */
	private int requestWithPost(String urlString, JSONObject data, StringBuilder result) throws IOException {
		URL url = new URL(urlString);
		URLConnection connection = url.openConnection();
		HttpURLConnection httpURLConnection = (HttpURLConnection) connection;

		httpURLConnection.setDoOutput(true);
		httpURLConnection.setDoInput(true);
		httpURLConnection.setConnectTimeout(TIMEOUT);
		httpURLConnection.setRequestMethod("POST");
		httpURLConnection.setRequestProperty("Accept-Charset", CHARSET);
		httpURLConnection.setRequestProperty("Content-Type", "application/json");
		httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
		httpURLConnection.setRequestProperty("User-Agent", "Nucleus/" + Version.getNumbers());
		httpURLConnection.setRequestProperty("Version", "1.0");

		String dataString = data.toString();

		OutputStream outputStream = null;
		try {
			outputStream = httpURLConnection.getOutputStream();
			outputStream.write(dataString.getBytes(Charset.forName("UTF-8")));
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

		int responseCode = httpURLConnection.getResponseCode();

		InputStreamReader inputStreamReader = null;
		BufferedReader reader = null;

		try {
			inputStreamReader = new InputStreamReader(httpURLConnection.getInputStream());
			reader = new BufferedReader(inputStreamReader);
			String line = null;
			while ((line = reader.readLine()) != null) {
				result.append(line);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (null != reader) {
					reader.close();
				}
				if (null != inputStreamReader) {
					inputStreamReader.close();
				}
			} catch (IOException e) {
				// Nothing
			}
		}

		return responseCode;
	}
}
