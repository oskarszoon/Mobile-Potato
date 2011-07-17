package com.oskarsson.mobilepotato;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private String debugTag = "MP_Preferences";
	private String settingHost = "";
	private String settingPort = "";
	private String settingUsername = "";
	private String settingPassword = "";
	private Boolean settingUseHTTPS = false;
	private JSONObject settingQualities;
	private ListPreference qualityListPreference;
	private SharedPreferences sharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preferences);

		sharedPreferences = getPreferenceScreen().getSharedPreferences();

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

		// TODO: do something if the list is already populated
		qualityListPreference = (ListPreference) findPreference("Quality");
		qualityListPreference.setEnabled(false);
		qualityListPreference.setSummary(this.getString(R.string.quality_error));
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
			// TODO: retrieve the Name
			preference.setSummary(sp.getString("Quality", ""));
		}

		settingHost = sp.getString("Host", MainActivity.defaultHost);
		settingPort = sp.getString("Port", MainActivity.defaultPort);
		settingUsername = sp.getString("Username", MainActivity.defaultUsername);
		settingPassword = sp.getString("Password", MainActivity.defaultPassword);
		settingUseHTTPS = sp.getBoolean("UseHTTPS", MainActivity.defaultUseHTTPS);
		//String debugString = settingHost + "\n" + settingPort + "\n" + (settingUseHTTPS ? "on" : "off") + "\n" + settingUsername + "\n" + settingPassword;

		if (!key.equals("Quality")) {
			GetQualitiesTask task = new GetQualitiesTask(this);
			task.execute();
		}
	}

	private class GetQualitiesTask extends AsyncTask<Void, Void, String> {

		private PreferencesActivity activity;

		public GetQualitiesTask(PreferencesActivity activity)
		{
			this.activity = activity;
		}

		@Override
		protected String doInBackground(Void... params)
		{
			int responseCode = 500;
			if (settingHost.length() > 0 && settingPort.length() > 0) {
				DefaultHttpClient httpClient = new DefaultHttpClient();
				try {
					HttpResponse httpResponse = HTTPClientHelpers.getResponse(settingHost, Integer.parseInt(settingPort), "/movie/imdbAdd/?id=1", settingUseHTTPS, settingUsername, settingPassword, httpClient);
					responseCode = HTTPClientHelpers.getResponseCode(httpResponse);
					String responseText = HTTPClientHelpers.getResponseContent(httpResponse);

					settingQualities = getQualities(responseText);
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
			Log.d(debugTag, "Post execute");

			String responseText = "";
			if (responseCode.equals("200")) {
				responseText = "Connected to CouchPotato";

				String[] entryNames = {"Choose when adding"};
				String[] entryValues = {"0"};
				try {
					JSONArray qualityKeys = settingQualities.names();

					entryNames = new String[qualityKeys.length() + 1];
					entryValues = new String[qualityKeys.length() + 1];

					entryNames[0] = "Choose when adding";
					entryValues[0] = "0";
					for (int i = 0; i < qualityKeys.length(); i++) {
						entryNames[i + 1] = settingQualities.getString(qualityKeys.getString(i));
						entryValues[i + 1] = qualityKeys.getString(i);
					}
				} catch (Exception e) {
					// do nothing
				}
				activity.qualityListPreference.setEntries(entryNames);
				activity.qualityListPreference.setEntryValues(entryValues);
				// TODO: retrieve name
				activity.qualityListPreference.setSummary(sharedPreferences.getString("Quality", ""));
				activity.qualityListPreference.setEnabled(true);
			} else if (responseCode.equals("401")) {
				if (settingUsername.length() > 0 && settingPassword.length() > 0) {
					responseText = "Unauthorized, check your credentials";
				} else {
					responseText = "Unauthorized, please fill in your credentials";
				}
			} else {
				responseText = "Unable to connect to CouchPotato";
			}

			Toast toast = Toast.makeText(this.activity.getApplicationContext(), responseText, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		}
	}

	public static JSONObject getQualities(String responseText)
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

		return jsonObject;
	}
}
