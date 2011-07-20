/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oskarsson.mobilepotato;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author oli
 */
public class QueueActivity extends Activity {
	
	private String debugTag = "MP_Queue";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.queue);
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		String queueString = sharedPreferences.getString("Queue", "");

		ListView lv = (ListView) findViewById(R.id.queue_list);
		if (!queueString.equals("")) {
			JSONObject queue;
			try {
				queue = new JSONObject(queueString);
			} catch (JSONException e) {
				queue = new JSONObject();
			}

			if (queue.length() > 0) {
				String[] queueList = new String[queue.length()];
				
				try {
					JSONArray queueKeys = queue.names();
					for (int i = 0; i < queueKeys.length(); i++) {
						queueList[i] = queue.getString(queueKeys.getString(i));
					}
				} catch (JSONException e) {
					queueList = new String[0];
					Log.e(debugTag, "Unable to add queue items from JSONObject: " + e.toString());
				}
				java.util.Arrays.sort(queueList);
				ArrayAdapter lvAdapter = new ArrayAdapter<String>(this, R.layout.list_item, queueList);
				lv.setAdapter(lvAdapter);

				lv.setOnItemClickListener(new OnItemClickListener() {

					public void onItemClick(AdapterView<?> parent, View view, int position, long id)
					{
						//startActivity(new Intent(view.getContext(), QueueActivity.class));
					}
				});
			}
		}
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		finish();
	}
}
