/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oskarsson.mobilepotato;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author oli
 */
public class QueueActivity extends Activity {

	private String debugTag = "MP_Queue";
	private SharedPreferences sharedPreferences;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.queue);

		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		final String queueString = sharedPreferences.getString("Queue", "");

		ListView lv = (ListView) findViewById(R.id.queue_list);
		if (!queueString.equals("")) {
			JSONObject queue;
			try {
				queue = new JSONObject(queueString);
			} catch (JSONException e) {
				queue = new JSONObject();
			}

			if (queue.length() > 0) {
				final String[] queueList = new String[queue.length()];
				final String[] queueIDs = new String[queue.length()];

				try {
					JSONArray queueKeys = queue.names();
					for (int i = 0; i < queueKeys.length(); i++) {
						queueList[i] = queue.getString(queueKeys.getString(i));
						queueIDs[i] = queueKeys.getString(i);
					}
					//java.util.Arrays.sort(queueList);
					// TODO: sorting the queue list
					QueueListAdapter lvAdapter = new QueueListAdapter(this, queueList, queueIDs);
					lv.setAdapter(lvAdapter);

					lv.setOnItemClickListener(new OnItemClickListener() {

						public void onItemClick(AdapterView<?> parent, View view, int position, long id)
						{
							// TODO: Add quick actions: http://www.londatiga.net/it/how-to-create-quickaction-dialog-in-android/
							//Toast toast = Toast.makeText(QueueActivity.this, queueIDs[position], Toast.LENGTH_LONG);
							//toast.setGravity(Gravity.CENTER, 0, 0);
							//toast.show();
						}
					});

					// set the icon handlers
					LinearLayout queueAction;
					queueAction = (LinearLayout) findViewById(R.id.queue_action_upload);
					queueAction.setOnClickListener(new OnClickListener() {

						public void onClick(View v)
						{
							v.setBackgroundResource(android.R.drawable.list_selector_background);
							AddMoviesToCPTask task = new AddMoviesToCPTask(QueueActivity.this);
							String[] queueParam = { queueString };
							task.execute(queueParam);
						}
					});
					
					queueAction = (LinearLayout) findViewById(R.id.queue_action_clear);
					queueAction.setOnClickListener(new OnClickListener() {

						public void onClick(View v)
						{
							v.setBackgroundResource(android.R.drawable.list_selector_background);
							clearQueue();
							finish();
						}
					});
				} catch (JSONException e) {
					Log.e(debugTag, "Unable to add queue items from JSONObject: " + e.toString());
					unableToPopulateQueue();
				}
			} else {
				unableToPopulateQueue();
			}
		} else {
			unableToPopulateQueue();
		}
	}
	
	private class AddMoviesToCPTask extends AsyncTask<String, Void, String> {

		private ProgressDialog dialog;
		private QueueActivity activity;

		public AddMoviesToCPTask(QueueActivity queueActivity)
		{
			activity = queueActivity;
			dialog = new ProgressDialog(activity);
		}

		@Override
		protected void onPreExecute()
		{
			this.dialog.setMessage("Adding movies to CouchPotato");
			this.dialog.show();
		}

		@Override
		protected String doInBackground(String[] queueParam)
		{
			String jobResponse = "Unable to load queue";
			int responseCode = 500;
			Boolean connected = true;
			// TODO: if we can't connect, change status
			/*String path = "/movie/imdbAdd/?id=" + IMDbDetails[1] + "&add=Add&quality=" + settingQuality;
					HttpResponse httpResponse = HTTPClientHelpers.getResponse(settingHost, Integer.parseInt(settingPort), path, settingUseHTTPS, settingUsername, settingPassword, httpClient);
					int responseCode = HTTPClientHelpers.getResponseCode(httpResponse);*/
			
			JSONObject queue;
			try {
				queue = new JSONObject(queueParam[0]);
				
				if (queue.length() > 0) {
					jobResponse = "Added movies to CP";
					
					GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();
					if (MainActivity.trackAnalytics) {
						tracker.start("UA-24637776-1", getApplication());
						tracker.trackPageView("/AddMoviesQueued/" + Integer.toString(queue.length()));
					}
					
					JSONArray queueKeys = queue.names();
					for (int i = 0; i < queueKeys.length(); i++) {
						responseCode = Helpers.addMovieToCP(sharedPreferences, queueKeys.getString(i));
						if (responseCode == 200) {
							Log.i(debugTag, "Added " + queue.getString(queueKeys.getString(i)));
							if (MainActivity.trackAnalytics) {
								tracker.trackPageView("/AddMovie/" + queue.getString(queueKeys.getString(i)));
							}
							queue.remove(queueKeys.getString(i));
						} else {
							jobResponse = "Unable to add " + queue.getString(queueKeys.getString(i));
							break;
						}
					}
					
					if (MainActivity.trackAnalytics) {
						tracker.dispatch();
						tracker.stop();
					}
					
					saveQueue(queue);
				}
			} catch (Exception e) {
				jobResponse = "Unable to add movies from queue";
				Log.e(debugTag, "Unable to load queue: " + e.toString());
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
				Toast toast = Toast.makeText(activity, result, Toast.LENGTH_LONG);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();
			}
			activity.finish();
		}
	}
	
	private void clearQueue()
	{
		saveQueue(new JSONObject());
		
		Toast toast = Toast.makeText(QueueActivity.this, "Cleared the queue", Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
	
	private void saveQueue(JSONObject queue)
	{
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("Queue", queue.toString());
		editor.commit();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		finish();
	}

	private void unableToPopulateQueue()
	{
		Toast toast = Toast.makeText(this, "Unable to load queue", Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
		finish();
	}
}
