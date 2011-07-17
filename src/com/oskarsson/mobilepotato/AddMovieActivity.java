/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oskarsson.mobilepotato;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

/**
 *
 * @author oli
 */
public class AddMovieActivity extends Activity {

	private String debugTag = "MP_AddMovie";
	private String settingHost = "";
	private String settingPort = "";
	private String settingUsername = "";
	private String settingPassword = "";
	private Boolean settingUseHTTPS = false;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: something still shows up, not fully invisible
		super.onCreate(savedInstanceState);
		setVisible(false);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		String action = intent.getAction();

		// if this is from the share menu
		if (Intent.ACTION_SEND.equals(action)) {
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
			settingHost = sp.getString("Host", MainActivity.defaultHost);
			settingPort = sp.getString("Port", MainActivity.defaultPort);
			settingUsername = sp.getString("Username", MainActivity.defaultUsername);
			settingPassword = sp.getString("Password", MainActivity.defaultPassword);
			settingUseHTTPS = sp.getBoolean("HTTPS", MainActivity.defaultUseHTTPS);

			String fullText = (String) extras.get(Intent.EXTRA_TEXT);
			Log.d(debugTag, fullText);
			String[] IMDbDetails = getIMDbDetails(fullText);

			AddMovieToCP task = new AddMovieToCP();
			task.execute(IMDbDetails);
		}

		finish();
	}

	public String[] getIMDbDetails(String fullText)
	{
		String[] IMDbDetails = {"", ""};

		Pattern p = Pattern.compile("^(.+)\\shttp://[a-z\\.]*imdb\\.com/title/(tt[0-9]+)[\\s\\w\\W]*$");
		Matcher m = p.matcher(fullText);

		if (m.matches()) {
			IMDbDetails[0] = m.group(1);
			IMDbDetails[1] = m.group(2);
		}

		return IMDbDetails;
	}

	private class AddMovieToCP extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String[] IMDbDetails)
		{
			Log.i(debugTag, "Doing background add for " + IMDbDetails[0]);

			String jobResponse = "Something went wrong, sorry :(";

			if (IMDbDetails[0].length() > 0 && IMDbDetails[1].length() > 0) {
				DefaultHttpClient httpclient = new DefaultHttpClient();

				try {
					httpclient.getCredentialsProvider().setCredentials(
									new AuthScope(settingHost, Integer.parseInt(settingPort)),
									new UsernamePasswordCredentials(settingUsername, settingPassword));
					BasicHttpContext localcontext = new BasicHttpContext();
					localcontext.setAttribute("preemptive-auth", new BasicScheme());
					httpclient.addRequestInterceptor(new PreemptiveAuth(), 0);

					HttpHost targetHost = new HttpHost(settingHost, Integer.parseInt(settingPort), "http" + (settingUseHTTPS ? "s" : ""));
					//http://192.168.0.113:8083/movie/imdbAdd/?id=tt0068646&add=Add&quality=12
					HttpGet httpGet = new HttpGet("/movie/imdbAdd/?id=" + IMDbDetails[1] + "&add=Add&quality=12");

					HttpResponse response = httpclient.execute(targetHost, httpGet, localcontext);
					int responseCode = response.getStatusLine().getStatusCode();
					
					if (responseCode == 200) {
						jobResponse = "Added " + IMDbDetails[0] + " to CouchPotato";
					} else {
						Log.e(debugTag, "responseCode: " + responseCode);
					}

					/* Retrieve the response, in case of debugging
					HttpEntity entity = response.getEntity();
					InputStream inputStream = entity.getContent();
					// Read response into a buffered stream
					ByteArrayOutputStream content = new ByteArrayOutputStream();
					int readBytes = 0;
					byte[] sBuffer = new byte[512];
					while ((readBytes = inputStream.read(sBuffer)) != -1) {
						content.write(sBuffer, 0, readBytes);
					}
					// Return result from buffered stream
					String dataAsString = new String(content.toByteArray());*/

					httpclient.getConnectionManager().shutdown();
				} catch (Exception e) {
					httpclient.getConnectionManager().shutdown();
					Log.e(debugTag, "Exception: " + e.toString());
				}
			} else {
				Log.e(debugTag, "IMDbDetails[0] = " + IMDbDetails[0]);
				Log.e(debugTag, "IMDbDetails[1] = " + IMDbDetails[1]);
			}

			return jobResponse;
		}

		public class PreemptiveAuth implements HttpRequestInterceptor {

			public void process(
							final HttpRequest request,
							final HttpContext context) throws HttpException, IOException
			{

				AuthState authState = (AuthState) context.getAttribute(
								ClientContext.TARGET_AUTH_STATE);

				// If no auth scheme avaialble yet, try to initialize it preemptively
				if (authState.getAuthScheme() == null) {
					AuthScheme authScheme = (AuthScheme) context.getAttribute(
									"preemptive-auth");
					CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(
									ClientContext.CREDS_PROVIDER);
					HttpHost targetHost = (HttpHost) context.getAttribute(
									ExecutionContext.HTTP_TARGET_HOST);
					if (authScheme != null) {
						Credentials creds = credsProvider.getCredentials(
										new AuthScope(
										targetHost.getHostName(),
										targetHost.getPort()));
						if (creds == null) {
							throw new HttpException("No credentials for preemptive authentication");
						}
						authState.setAuthScheme(authScheme);
						authState.setCredentials(creds);
					}
				}
			}
		}

		@Override
		protected void onPostExecute(String result)
		{
			Toast toast = Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		}
	}
}
