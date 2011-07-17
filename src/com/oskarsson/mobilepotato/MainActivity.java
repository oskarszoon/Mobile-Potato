package com.oskarsson.mobilepotato;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;

public class MainActivity extends Activity implements OnSharedPreferenceChangeListener {
	
	private String debugTag = "MP_Main";

	private static final int MENU_SETTINGS = 0;
	private String settingHost = "";
	private String settingPort = "";
	private String settingUsername = "";
	private String settingPassword = "";
	private Boolean settingUseHTTPS = false;
	
	public static String defaultHost = "127.0.0.1";
	public static String defaultPort = "5000";
	public static String defaultUsername = "";
	public static String defaultPassword = "";
	public static Boolean defaultUseHTTPS = false;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// TODO: check for IMDB app
		// TODO: check for configuration set
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		sp.registerOnSharedPreferenceChangeListener(this);

		settingHost = sp.getString("Host", defaultHost);
		settingPort = sp.getString("Port", defaultPort);
		settingUsername = sp.getString("Username", defaultUsername);
		settingPassword = sp.getString("Password", defaultPassword);
		settingUseHTTPS = sp.getBoolean("HTTPS", defaultUseHTTPS);

		final EditText searchInput = (EditText) findViewById(R.id.search_input);
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
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, MENU_SETTINGS, 0, "Preferences").setIcon(android.R.drawable.ic_menu_preferences);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
			case MENU_SETTINGS:
				startActivity(new Intent(this, PreferencesActivity.class));
				return true;
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
		settingUseHTTPS = sp.getBoolean("HTTPS", defaultUseHTTPS);
	}
}
