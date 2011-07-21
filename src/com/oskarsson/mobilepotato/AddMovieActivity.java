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
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

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
	private Boolean settingConnected = false;
	private String settingQuality = "";
	private SharedPreferences sharedPreferences;

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
			sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
			settingHost = sharedPreferences.getString("Host", MainActivity.defaultHost);
			settingPort = sharedPreferences.getString("Port", MainActivity.defaultPort);
			settingUsername = sharedPreferences.getString("Username", MainActivity.defaultUsername);
			settingPassword = sharedPreferences.getString("Password", MainActivity.defaultPassword);
			settingUseHTTPS = sharedPreferences.getBoolean("UseHTTPS", MainActivity.defaultUseHTTPS);
			settingQuality = sharedPreferences.getString("Quality", "");
			settingConnected = sharedPreferences.getBoolean("Connected", false);
			
			String fullText = (String) extras.get(Intent.EXTRA_TEXT);
			Log.d(debugTag, fullText);
			String[] IMDbDetails = getIMDbDetails(fullText);
			if (settingConnected) {
				AddMovieToCPTask task = new AddMovieToCPTask(this);
				task.execute(IMDbDetails);
			} else {
				AddMovieToQueue(IMDbDetails);
				showMessage("Offline: added " +  IMDbDetails[0] + " to queue");
				finish();
			}
		} else {
			finish();
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
	
	private void showMessage(String message)
	{
		Toast toast = Toast.makeText(AddMovieActivity.this, message, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
	
	private void AddMovieToQueue(String[] IMDbDetails)
	{
		SharedPreferences.Editor editor = this.sharedPreferences.edit();
		
		JSONObject queue;
		try {
			queue = new JSONObject(this.sharedPreferences.getString("Queue", ""));
		} catch (JSONException e) {
			queue = new JSONObject();
		}
		try {
			queue.put(IMDbDetails[1], IMDbDetails[0]);
		} catch (JSONException e) {
			Log.e(debugTag, "Could not add movie to queue: " + e.toString());
		}
		
		if (settingConnected) {
			// We are connected, but the movie was queued, so something went wrong, lets go offline
			editor.putBoolean("Connected", false);
			settingConnected = false;
		}
		
		editor.putString("Queue", queue.toString());
		editor.commit();
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

			String jobResponse = "";

			if (IMDbDetails[0].length() > 0 && IMDbDetails[1].length() > 0) {
				
				int responseCode = Helpers.addMovieToCP(sharedPreferences, IMDbDetails[1]);
				if (responseCode == 200) {
					if (MainActivity.trackAnalytics) {
						// TODO: group analytics calls
						GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();
						tracker.start("UA-24637776-1", getApplication());
						tracker.trackPageView("/AddMovie/" + IMDbDetails[0]);
						tracker.dispatch();
						tracker.stop();
					}
					jobResponse = "Added " + IMDbDetails[0] + " to CouchPotato";
				} else {
					Log.e(debugTag, "responseCode: " + responseCode);
					AddMovieToQueue(IMDbDetails);
					jobResponse = "Unable to connect to CouchPotato, added " + IMDbDetails[0] + " to offline queue";
				}
			} else {
				showMessage("Could not add movie, did not receive the IMDb information");
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
			if (!result.equals("")) {
				showMessage(result);
			}
			activity.finish();
		}
	}
}
