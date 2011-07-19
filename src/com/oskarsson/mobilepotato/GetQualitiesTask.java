/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oskarsson.mobilepotato;

import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;

public class GetQualitiesTask extends AsyncTask<Void, Void, String> {

	private String debugTag = "MP_GetQualities";
	private PreferencesActivity activity;

	public GetQualitiesTask(PreferencesActivity activity)
	{
		this.activity = activity;
	}

	@Override
	protected String doInBackground(Void... params)
	{
		String debugString = activity.settingUsername + ":" + activity.settingPassword + "@" + activity.settingHost + ":" + activity.settingPort + " - HTTPS: " + (activity.settingUseHTTPS ? "on" : "off");
		Log.d(debugTag, "Connecting to " + debugString);
		
		int responseCode = 500;
		if (activity.settingHost.length() > 0 && activity.settingPort.length() > 0) {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			try {
				HttpResponse httpResponse = HTTPClientHelpers.getResponse(
								activity.settingHost,
								Integer.parseInt(activity.settingPort),
								"/movie/imdbAdd/?id=1",
								activity.settingUseHTTPS,
								activity.settingUsername,
								activity.settingPassword,
								httpClient);
				responseCode = HTTPClientHelpers.getResponseCode(httpResponse);
				String responseText = HTTPClientHelpers.getResponseContent(httpResponse);

				activity.settingQualities = PreferencesActivity.getQualities(responseText);
				httpClient.getConnectionManager().shutdown();
			} catch (Exception e) {
				httpClient.getConnectionManager().shutdown();
				Log.e(debugTag, "Exception: " + e.toString());
			}
		}

		return Integer.toString(responseCode);
	}

	@Override
	protected void onPostExecute(String responseCode)
	{
		Log.d(debugTag, "Post execute with code: " + responseCode);

		String responseText = "";
		if (responseCode.equals("200")) {
			responseText = "Connected to CouchPotato";

			String[] entryNames = {"Choose when adding"};
			String[] entryValues = {"0"};
			try {
				JSONArray qualityKeys = activity.settingQualities.names();

				entryNames = new String[qualityKeys.length()];
				entryValues = new String[qualityKeys.length()];

				for (int i = 0; i < qualityKeys.length(); i++) {
					entryNames[i] = activity.settingQualities.getString(qualityKeys.getString(i));
					entryValues[i] = qualityKeys.getString(i);
				}
			} catch (Exception e) {
				// do nothing
			}
			activity.qualityListPreference.setEntries(entryNames);
			activity.qualityListPreference.setEntryValues(entryValues);
			activity.setConnected(true);
		} else {
			if (responseCode.equals("401")) {
				if (activity.settingUsername.length() > 0 && activity.settingPassword.length() > 0) {
					responseText = "Unauthorized, check your credentials";
				} else {
					responseText = "Unauthorized, please fill in your credentials";
				}
			} else {
				responseText = "Unable to connect to CouchPotato";
			}
			activity.setConnected(false);
		}

		Toast toast = Toast.makeText(this.activity.getApplicationContext(), responseText, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
}