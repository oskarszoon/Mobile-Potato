package com.oskarsson.mobilepotato;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preferences);

		SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
		
		Preference p;
		String[] textPreferences = {"Host", "Port", "Username", "Password"};
		for (String preference : textPreferences) {
			p = (Preference) findPreference(preference);
			if (preference.equals("Password")) {
				p.setSummary(sp.getString(preference, "").replaceAll(".", "*"));
			} else {
				p.setSummary(sp.getString(preference, ""));
			}
		}

		CheckBoxPreference autostart = (CheckBoxPreference) findPreference("HTTPS");
		if (autostart.isChecked()) {
			autostart.setSummary("On");
		} else {
			autostart.setSummary("Off");
		}
	}

	@Override
	protected void onPause()
	{
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		finish();
		super.onPause();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		Preference pref = findPreference(key);
		if (pref instanceof EditTextPreference) {
			EditTextPreference etp = (EditTextPreference) pref;
			if (pref.getKey().equals("Password")) {
				pref.setSummary(etp.getText().replaceAll(".", "*"));
			} else {
				pref.setSummary(etp.getText());
			}
		} else if (pref instanceof CheckBoxPreference) {
			if (((CheckBoxPreference) pref).isChecked()) {
				pref.setSummary("On");
			} else {
				pref.setSummary("Off");
			}
		}
	}
}
