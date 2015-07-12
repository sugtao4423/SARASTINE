package com.tao.sarastine;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ListViewAdapter extends ArrayAdapter<Utt> {
	private LayoutInflater mInflater;
	private String who;
	public ListViewAdapter(Context context, String who){
		super(context, android.R.layout.simple_list_item_1);
		this.who = who;
		mInflater = (LayoutInflater)context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent){
		final Utt item = (Utt)getItem(position);
		boolean me = item.getMe();
		
		if(me){
			convertView = mInflater.inflate(R.layout.dialogue_me, null);
			TextView utt = (TextView)convertView.findViewById(R.id.dialogueMe);
				
			utt.setText(item.getMeUtt());
		}else{
			convertView = mInflater.inflate(R.layout.dialogue_sisters, null);
			ImageView icon = (ImageView)convertView.findViewById(R.id.sisterIcon);
			TextView utt = (TextView)convertView.findViewById(R.id.sister_utt);
			
			switch(who){
			case "noah":
				icon.setImageResource(R.drawable.noah);
				break;
			case "yua":
				icon.setImageResource(R.drawable.yua);
				break;
			}
			utt.setText(item.getSisterUtt());
		}
		return convertView;
	}
}