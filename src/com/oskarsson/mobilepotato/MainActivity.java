package com.oskarsson.mobilepotato;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

	private String debugTag = "MP_Main";
	public static String defaultHost = "";
	public static String defaultPort = "";
	public static Boolean defaultUseHTTPS = false;
	public static String defaultUsername = "";
	public static String defaultPassword = "";
	GoogleAnalyticsTracker tracker;
	private AdView adView;
	// TODO: enable tracking before releasing to market
	public static Boolean trackAnalytics = true;
	public static Boolean showAds = true;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// TODO: better google analytics tracking, see http://code.google.com/mobile/analytics/docs/android/ (UA-24637776-1)
		if (trackAnalytics) {
			tracker = GoogleAnalyticsTracker.getInstance();
			tracker.start("UA-24637776-1", getApplication());
			tracker.trackPageView("/Main");
			tracker.dispatch();
		}

		if (showAds) {
			adView = new AdView(this, AdSize.BANNER, "a14e273f3a516b4");
			LinearLayout layout = (LinearLayout) findViewById(R.id.main_ad);
			layout.addView(adView);
			adView.loadAd(new AdRequest());
		}

		// don in onReusme?
		//checkForIMDbApp();
		//setQueueActions();
	}

	private void checkForIMDbApp()
	{
		final EditText searchInput = (EditText) findViewById(R.id.search_input);
		try {
			// Check for IMDb existance
			getPackageManager().getApplicationInfo("com.imdb.mobile", 0);

			searchInput.setOnKeyListener(new OnKeyListener() {

				public boolean onKey(View v, int keyCode, KeyEvent event)
				{
					// If the event is a key-down event on the "enter" button
					if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
						Intent imdbIntent = new Intent();
						imdbIntent.setAction(Intent.ACTION_VIEW);
						imdbIntent.setData(Uri.parse("imdb:///find?q=" + searchInput.getText()));
						startActivity(imdbIntent);
						return true;
					}
					return false;
				}
			});
		} catch (NameNotFoundException e) {
			Log.e(debugTag, "IMDb app not found: " + e.toString());
			searchInput.setHint("Please install the IMDb app");
			searchInput.setEnabled(false);
		}
	}

	private void setQueueActions()
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		String queueString = sharedPreferences.getString("Queue", "");

		Boolean queueFilled = false;
		ListView lv = (ListView) findViewById(R.id.queue_action);
		if (!queueString.equals("")) {
			JSONObject queue;
			try {
				queue = new JSONObject(queueString);
			} catch (JSONException e) {
				queue = new JSONObject();
			}

			if (queue.length() > 0) {
				queueFilled = true;
				lv.setVisibility(View.VISIBLE);
				String[] myList = new String[]{"Movies in offline queue: " + queue.length()};
				lv.setAdapter(new ArrayAdapter<String>(this, R.layout.main_list_item, myList));

				lv.setOnItemClickListener(new OnItemClickListener() {

					public void onItemClick(AdapterView<?> parent, View view, int position, long id)
					{
						startActivity(new Intent(view.getContext(), QueueActivity.class));
					}
				});
			}
		}
		
		if (!queueFilled) {
			lv.setVisibility(View.GONE);
		}
	}
	private static final int MENU_SETTINGS = 1;
	private static final int MENU_ABOUT = 2;

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, MENU_SETTINGS, 0, "Preferences").setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, MENU_ABOUT, 0, "About").setIcon(android.R.drawable.ic_menu_info_details);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
			case MENU_SETTINGS:
				startActivity(new Intent(this, PreferencesActivity.class));
				break;
			case MENU_ABOUT:
				showDialog(DIALOG_ABOUT);
				break;
		}
		return true;
	}

	@Override
	public void onConfigurationChanged(final Configuration newConfig)
	{
		// Ignore orientation change to keep activity from restarting
		super.onConfigurationChanged(newConfig);
	}
	static final int DIALOG_ABOUT = 1;

	@Override
	protected Dialog onCreateDialog(int id)
	{
		Dialog dialog = null;
		switch (id) {
			case DIALOG_ABOUT:
				dialog = createAboutDialog();
				break;
			default:
				dialog = null;
		}
		return dialog;
	}

	private AlertDialog createAboutDialog()
	{
		AlertDialog dialog = null;
		try {
			dialog = AboutDialog.create(this);
			dialog.show();
		} catch (NameNotFoundException e) {
			Log.e(debugTag, "Exception: " + e.toString());
		}

		return dialog;
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		checkForIMDbApp();
		setQueueActions();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if (trackAnalytics) {
			tracker.stop();
		}
		if (showAds) {
			adView.destroy();
		}
	}
}
