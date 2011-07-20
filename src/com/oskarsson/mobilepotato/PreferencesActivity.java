package com.oskarsson.mobilepotato;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private String debugTag = "MP_Preferences";
	public String settingHost = "";
	public String settingPort = "";
	public String settingUsername = "";
	public String settingPassword = "";
	public Boolean settingUseHTTPS = false;
	public Boolean settingConnected = false;
	public JSONObject settingQualities;
	public ListPreference qualityListPreference;
	public SharedPreferences sharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preferences);

		sharedPreferences = getPreferenceScreen().getSharedPreferences();
		qualityListPreference = (ListPreference) findPreference("Quality");
		
		// Populate defaults
		setServerSettings();
		try {
			settingQualities = new JSONObject(sharedPreferences.getString("Qualities", ""));
		} catch (JSONException ex) {
			settingQualities = new JSONObject();
		}
		setConnected(settingConnected);

		Preference p;
		String[] textPreferences = {"Host", "Port", "Username", "Password"};
		for (String preference : textPreferences) {
			p = (Preference) findPreference(preference);
			if (preference.equals("Password")) {
				p.setSummary(sharedPreferences.getString(preference, "").replaceAll(".", "*"));
			} else {
				p.setSummary(sharedPreferences.getString(preference, ""));
			}
		}

		CheckBoxPreference autostart = (CheckBoxPreference) findPreference("UseHTTPS");
		if (autostart.isChecked()) {
			autostart.setSummary("On");
		} else {
			autostart.setSummary("Off");
		}

		p = (Preference) findPreference("Connect");
		p.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			public boolean onPreferenceClick(Preference preference)
			{
				GetQualitiesTask task = new GetQualitiesTask(PreferencesActivity.this);
				task.execute();
				return true;
			}
		});
	}

	@Override
	protected void onPause()
	{
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
		finish();
		super.onPause();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sp, String key)
	{
		Preference preference = findPreference(key);
		if (preference instanceof EditTextPreference) {
			EditTextPreference etp = (EditTextPreference) preference;
			if (preference.getKey().equals("Password")) {
				preference.setSummary(etp.getText().replaceAll(".", "*"));
			} else {
				preference.setSummary(etp.getText());
			}
		} else if (preference instanceof CheckBoxPreference) {
			if (((CheckBoxPreference) preference).isChecked()) {
				preference.setSummary("On");
			} else {
				preference.setSummary("Off");
			}
		} else if (key.equals("Quality")) {
			setConnected(true);
		}

		setServerSettings();
	}

	public void setServerSettings()
	{
		settingHost = sharedPreferences.getString("Host", MainActivity.defaultHost);
		settingPort = sharedPreferences.getString("Port", MainActivity.defaultPort);
		settingUsername = sharedPreferences.getString("Username", MainActivity.defaultUsername);
		settingPassword = sharedPreferences.getString("Password", MainActivity.defaultPassword);
		settingConnected = sharedPreferences.getBoolean("Connected", false);
		settingUseHTTPS = sharedPreferences.getBoolean("UseHTTPS", MainActivity.defaultUseHTTPS);
		//String debugString = settingHost + "\n" + settingPort + "\n" + (settingUseHTTPS ? "on" : "off") + "\n" + settingUsername + "\n" + settingPassword;
	}

	public void setConnected(Boolean connected)
	{
		SharedPreferences.Editor editor = sharedPreferences.edit();
		
		String quality = sharedPreferences.getString("Quality", "");
		String qualitySummary = "";
		if (connected) {
			editor.putString("Qualities", settingQualities.toString());
			
			try {
				if (quality.equals("")) {
					JSONArray qualityIDs = settingQualities.names();
					// TODO: better default quality
					quality = qualityIDs.getString(0);
				}
				qualitySummary = settingQualities.getString(quality);
			} catch (JSONException e) {
				Log.e(debugTag, "Unable to find key " + quality + " in JSON Object: " + e.toString());
			}
			
			populateQualities();
		}
		qualityListPreference.setSummary(qualitySummary);
		qualityListPreference.setEnabled(connected);
		
		editor.putBoolean("Connected", connected);
		editor.commit();
	}

	public void setQualities(String responseText)
	{
		JSONObject jsonObject = new JSONObject();
		try {
			Pattern p = Pattern.compile(".*?<option value=\"([0-9]+)\".*?>(.*?)<.*?", Pattern.DOTALL);
			Matcher m = p.matcher(responseText);
			while (m.find() == true) {
				jsonObject.accumulate(m.group(1), m.group(2));
			}
		} catch (Exception e) {
			// do nothing
		}
		
		settingQualities = jsonObject;
		
		populateQualities();
	}
	
	private void populateQualities()
	{
		// TODO: add support for quality choosing when adding movie
		String[] entryNames = {"Unable to retrieve qualities"};
		String[] entryValues = {"0"};
		try {
			JSONArray qualityKeys = settingQualities.names();

			entryNames = new String[qualityKeys.length()];
			entryValues = new String[qualityKeys.length()];

			for (int i = 0; i < qualityKeys.length(); i++) {
				entryNames[i] = settingQualities.getString(qualityKeys.getString(i));
				entryValues[i] = qualityKeys.getString(i);
			}
		} catch (Exception e) {
			// do nothing
		}
		qualityListPreference.setEntries(entryNames);
		qualityListPreference.setEntryValues(entryValues);
	}
}
