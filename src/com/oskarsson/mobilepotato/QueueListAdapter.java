/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oskarsson.mobilepotato;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class QueueListAdapter extends BaseAdapter {

	Activity context;
	String movieName[];
	String movieID[];

	public QueueListAdapter(Activity context, String[] title, String[] description)
	{
		this.context = context;
		this.movieName = title;
		this.movieID = description;
	}

	public int getCount()
	{
		return movieName.length;
	}

	public Object getItem(int position)
	{
		return null;
	}

	public long getItemId(int position)
	{
		return 0;
	}

	private class ViewHolder {

		TextView textViewName;
		TextView textViewID;
	}

	public View getView(int position, View convertView, ViewGroup parent)
	{
		// TODO Auto-generated method stub
		ViewHolder holder;
		LayoutInflater inflater = context.getLayoutInflater();

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.queue_list_item, null);
			holder = new ViewHolder();
			holder.textViewName = (TextView) convertView.findViewById(R.id.queue_list_title);
			holder.textViewID = (TextView) convertView.findViewById(R.id.queue_list_id);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.textViewName.setText(movieName[position]);
		holder.textViewID.setText(movieID[position]);

		return convertView;
	}
}