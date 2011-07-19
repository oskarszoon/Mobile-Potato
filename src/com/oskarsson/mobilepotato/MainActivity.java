package com.oskarsson.mobilepotato;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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
import android.widget.EditText;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import java.util.HashMap;

public class MainActivity extends Activity implements OnSharedPreferenceChangeListener {

	private String debugTag = "MP_Main";
	private String settingHost = "";
	private String settingPort = "";
	private String settingUsername = "";
	private String settingPassword = "";
	private Boolean settingUseHTTPS = false;
	public static String defaultHost = "";
	public static String defaultPort = "";
	public static Boolean defaultUseHTTPS = false;
	public static String defaultUsername = "";
	public static String defaultPassword = "";
	public static HashMap settingQualities;
	GoogleAnalyticsTracker tracker;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: test for other devices
		// TODO: handle orientation change in preferences screen

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// TODO: better google analytics tracking, see http://code.google.com/mobile/analytics/docs/android/ (UA-24637776-1)
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.start("UA-24637776-1", getApplication());
		tracker.trackPageView("/Main");
		tracker.dispatch();
		// Try to load the a package matching the name of our own package
		//PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_META_DATA);
		//tracker.setCustomVar(1, "Version", pInfo.versionName, 2);

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		sp.registerOnSharedPreferenceChangeListener(this);

		settingHost = sp.getString("Host", defaultHost);
		settingPort = sp.getString("Port", defaultPort);
		settingUsername = sp.getString("Username", defaultUsername);
		settingPassword = sp.getString("Password", defaultPassword);
		settingUseHTTPS = sp.getBoolean("UseHTTPS", defaultUseHTTPS);

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

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		settingHost = sp.getString("Host", defaultHost);
		settingPort = sp.getString("Port", defaultPort);
		settingUsername = sp.getString("Username", defaultUsername);
		settingPassword = sp.getString("Password", defaultPassword);
		settingUseHTTPS = sp.getBoolean("UseHTTPS", defaultUseHTTPS);
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
	protected void onDestroy()
	{
		super.onDestroy();
		tracker.stop();
	}
}
