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

public class ListViewAdapter extends ArrayAdapter<Utt>{
	private LayoutInflater mInflater;
	private String who;

	public ListViewAdapter(Context context, String who){
		super(context, android.R.layout.simple_list_item_1);
		this.who = who;
		mInflater = (LayoutInflater)context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
	}

	@SuppressLint("InflateParams")
	@Override
	public View getView(int position, View convertView, ViewGroup parent){
		final Utt item = (Utt)getItem(position);

		if(item.getIsMe()) {
			convertView = mInflater.inflate(R.layout.list_item_me, null);
			TextView utt = (TextView)convertView.findViewById(R.id.me_utt);
			TextView date = (TextView)convertView.findViewById(R.id.me_date);

			utt.setText(item.getUtt());
			date.setText(item.getDate());
		}else{
			convertView = mInflater.inflate(R.layout.list_item_sister, null);
			ImageView icon = (ImageView)convertView.findViewById(R.id.sister_icon);
			TextView utt = (TextView)convertView.findViewById(R.id.sister_utt);
			TextView date = (TextView)convertView.findViewById(R.id.sister_date);

			switch(who){
			case "noah":
				icon.setImageResource(R.drawable.noah);
				break;
			case "yua":
				icon.setImageResource(R.drawable.yua);
				break;
			case "momoka":
				icon.setImageResource(R.drawable.momoka);
				break;
			}
			utt.setText(item.getUtt());
			date.setText(item.getDate());
		}
		return convertView;
	}
}