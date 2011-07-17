/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oskarsson.mobilepotato;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;

/**
 *
 * @author oli
 */
public class HTTPClientHelpers {

	static HttpResponse getResponse(String host, int port, String path, Boolean useHTTPS, String username, String password, DefaultHttpClient httpClient) throws Exception
	{
		HttpResponse httpResponse = null;
		try {
			BasicHttpContext localContext = new BasicHttpContext();
			if (username.length() > 0 && password.length() > 0) {
				httpClient.getCredentialsProvider().setCredentials(
								new AuthScope(host, port),
								new UsernamePasswordCredentials(username, password));
				localContext.setAttribute("preemptive-auth", new BasicScheme());
				httpClient.addRequestInterceptor(new PreemptiveAuth(), 0);
			}

			HttpHost targetHost = new HttpHost(host, port, "http" + (useHTTPS ? "s" : ""));
			HttpGet httpGet = new HttpGet(path);

			httpResponse = httpClient.execute(targetHost, httpGet, localContext);
		} catch (Exception e) {
			throw e;
		}
		
		return httpResponse;
	}
	
	static int getResponseCode(HttpResponse httpResponse)
	{
		return httpResponse.getStatusLine().getStatusCode();
	}
	
	static String getResponseContent(HttpResponse httpResponse) throws Exception
	{
		String responseContent = "";
		try {
			HttpEntity entity = httpResponse.getEntity();
			InputStream inputStream = entity.getContent();
			// Read response into a buffered stream
			ByteArrayOutputStream content = new ByteArrayOutputStream();
			int readBytes = 0;
			byte[] sBuffer = new byte[512];
			while ((readBytes = inputStream.read(sBuffer)) != -1) {
				content.write(sBuffer, 0, readBytes);
			}
			responseContent = new String(content.toByteArray());
		} catch (Exception e) {
			throw e;
		}
		
		return responseContent;
	}
}
