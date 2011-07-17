/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oskarsson.mobilepotato;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 *
 * @author oli
 */
public class AddMovieService extends IntentService {

	public AddMovieService()
	{
		super("AddMovieService");
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		Context context = getApplicationContext();
		CharSequence text = "Hello toast!";
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}
}