/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oskarsson.mobilepotato;

import android.content.SharedPreferences;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;

public class Helpers {
	
	public static int addMovieToCP(SharedPreferences sharedPreferences, String IMDbID)
	{
		String debugTag = "MP_AddMovieToCP";
		
		String host = sharedPreferences.getString("Host", "");
		String port = sharedPreferences.getString("Port", "");
		String username = sharedPreferences.getString("Username", "");
		String password = sharedPreferences.getString("Password", "");
		Boolean useHTTPS = sharedPreferences.getBoolean("UseHTTPS", false);
		String quality = sharedPreferences.getString("Quality", "");

		int responseCode = 500;

		if (IMDbID.length() > 0) {
			DefaultHttpClient httpClient = new DefaultHttpClient();

			try {
				//http://192.168.0.113:8083/movie/imdbAdd/?id=tt0068646&add=Add&quality=12
				String path = "/movie/imdbAdd/?id=" + IMDbID + "&add=Add&quality=" + quality;
				HttpResponse httpResponse = HTTPClientHelpers.getResponse(host, Integer.parseInt(port), path, useHTTPS, username, password, httpClient);
				responseCode = HTTPClientHelpers.getResponseCode(httpResponse);
				httpClient.getConnectionManager().shutdown();
			} catch (Exception e) {
				httpClient.getConnectionManager().shutdown();
				Log.e(debugTag, "Exception: " + e.toString());
			}
		}
		Log.i(debugTag, "Adding " + IMDbID + ": " + Integer.toString(responseCode));
		
		return responseCode;
	}
}