package com.tao.sarastine;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TalkUserListAdapter extends ArrayAdapter<User>{
	private LayoutInflater mInflater;

	public TalkUserListAdapter(Context context){
		super(context, android.R.layout.simple_list_item_1);
		mInflater = (LayoutInflater)context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
	}

	class ViewHolder{
		ImageView icon;
		TextView name, lastTalk, lastDate;
	}

	@SuppressLint("InflateParams")
	@Override
	public View getView(int position, View convertView, ViewGroup parent){
		final User user = (User)getItem(position);
		ViewHolder holder;

		if(convertView == null) {
			convertView = mInflater.inflate(R.layout.list_item_user, null);
			ImageView icon = (ImageView)convertView.findViewById(R.id.talkUserIcon);
			TextView name = (TextView)convertView.findViewById(R.id.talkUserName);
			TextView lastTalk = (TextView)convertView.findViewById(R.id.talkUserContent);
			TextView lastDate = (TextView)convertView.findViewById(R.id.talkUserDate);

			holder = new ViewHolder();
			holder.icon = icon;
			holder.name = name;
			holder.lastTalk = lastTalk;
			holder.lastDate = lastDate;

			convertView.setTag(holder);
		}else{
			holder = (ViewHolder)convertView.getTag();
		}

		holder.icon.setImageResource(user.getIcon());
		holder.name.setText(user.getName());
		holder.lastTalk.setText(user.getLastTalk());
		holder.lastDate.setText(user.getLastDate());

		return convertView;
	}
}