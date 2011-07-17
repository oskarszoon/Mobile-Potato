/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oskarsson.mobilepotato;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;

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
	private String settingQuality = "";

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_movie);

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
			settingUseHTTPS = sp.getBoolean("UseHTTPS", MainActivity.defaultUseHTTPS);
			settingQuality = sp.getString("Quality", "");

			String fullText = (String) extras.get(Intent.EXTRA_TEXT);
			Log.d(debugTag, fullText);
			String[] IMDbDetails = getIMDbDetails(fullText);

			AddMovieToCPTask task = new AddMovieToCPTask(this);
			task.execute(IMDbDetails);
		}
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

	private class AddMovieToCPTask extends AsyncTask<String, Void, String> {

		private ProgressDialog dialog;
		private AddMovieActivity activity;

		public AddMovieToCPTask(AddMovieActivity addMovieActivity)
		{
			activity = addMovieActivity;
			dialog = new ProgressDialog(activity);
		}

		@Override
		protected void onPreExecute()
		{
			this.dialog.setMessage("Adding movie to CouchPotato");
			this.dialog.show();
		}

		@Override
		protected String doInBackground(String[] IMDbDetails)
		{
			Log.i(debugTag, "Doing background add for " + IMDbDetails[0]);

			String jobResponse = "Something went wrong, sorry :(";

			if (IMDbDetails[0].length() > 0 && IMDbDetails[1].length() > 0) {
				DefaultHttpClient httpClient = new DefaultHttpClient();

				try {
					//http://192.168.0.113:8083/movie/imdbAdd/?id=tt0068646&add=Add&quality=12
					String path = "/movie/imdbAdd/?id=" + IMDbDetails[1] + "&add=Add&quality=" + settingQuality;
					HttpResponse httpResponse = HTTPClientHelpers.getResponse(settingHost, Integer.parseInt(settingPort), path, settingUseHTTPS, settingUsername, settingPassword, httpClient);
					int responseCode = HTTPClientHelpers.getResponseCode(httpResponse);
					//String responseText = HTTPClientHelpers.getResponseContent(httpResponse);

					if (responseCode == 200) {
						jobResponse = "Added " + IMDbDetails[0] + " to CouchPotato";
					} else {
						Log.e(debugTag, "responseCode: " + responseCode);
					}

					httpClient.getConnectionManager().shutdown();
				} catch (Exception e) {
					httpClient.getConnectionManager().shutdown();
					Log.e(debugTag, "Exception: " + e.toString());
				}
			} else {
				Log.e(debugTag, "IMDbDetails[0] = " + IMDbDetails[0]);
				Log.e(debugTag, "IMDbDetails[1] = " + IMDbDetails[1]);
			}

			return jobResponse;
		}

		@Override
		protected void onPostExecute(String result)
		{
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}
			activity.finish();
			Toast toast = Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		}
	}
}
