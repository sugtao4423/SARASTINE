package com.tao.sarastine;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import com.fasterxml.jackson.core.JsonFactory;
import com.tao.sarastine.CustomListView.OnSoftKeyShownListener;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.Toast;

public class Dialogue extends Activity {
	
	private CustomListView list;
	private ListViewAdapter noahAdapter;
	private ListViewAdapter yuaAdapter;
	
	private SharedPreferences pref;
	private String who, context;
	
	private EditText talkText;
	
	private SQLiteDatabase db;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		this.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		setContentView(R.layout.dialogue);
		list = (CustomListView)findViewById(R.id.listView1);
		list.setListener(new OnSoftKeyShownListener(){
			@Override
			public void onSoftKeyShown(boolean isShown){
				if(isShown)
					list.setSelection(list.getCount() - 1);
			}
		});
		talkText = (EditText)findViewById(R.id.editText1);
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		db = new SQLHelper(this).getWritableDatabase();
		
		String who = getIntent().getStringExtra("who");
		this.who = who;
		switch (who) {
		case "noah":
			noahAdapter = new ListViewAdapter(this, "noah");
			context = pref.getString("noah_context", "");
			getActionBar().setTitle("のあちゃん");
			list.setAdapter(noahAdapter);
			loadDialogue("noah");
			break;
		case "yua":
			yuaAdapter = new ListViewAdapter(this, "yua");
			context = pref.getString("yua_context", "");
			getActionBar().setTitle("ゆあちゃん");
			list.setAdapter(yuaAdapter);
			loadDialogue("yua");
			break;
		}
	}
	
	public void loadDialogue(String who){
		Cursor result = db.query(who, new String[]{"utt", "me"}, null, null, null, null, null);
		boolean mov = result.moveToFirst();
		while(mov){
			Utt u = new Utt();
			boolean itsMe = Boolean.parseBoolean(result.getString(1));
			if(itsMe)
				u.setMeUtt(result.getString(0));
			else
				u.setSisterUtt(result.getString(0));
			u.setMe(itsMe);
			if(who.equals("noah"))
				noahAdapter.add(u);
			else
				yuaAdapter.add(u);
			
			mov = result.moveToNext();
		}
	}
	
	public void send(View v) throws UnsupportedEncodingException{
		list.setSelection(list.getBottom());
		String text = talkText.getText().toString();
		talkText.setText("");
		Utt u = new Utt();
		u.setMeUtt(text);
		u.setMe(true);
		if(who.equals("noah"))
			noahAdapter.add(u);
		else
			yuaAdapter.add(u);
		db.execSQL("insert into " + who + " values('" + text + "', 'true')");
		final String call = "http://api.flum.pw/apis/dialogue?api_key=" + getString(R.string.sarastyAPI) +
				"&sister=" + who + "&user_id=" + pref.getString("user_id", null) +
				"&mode=markov&utt=" + URLEncoder.encode(text, "utf-8");
		
		
		AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>(){
			@Override
			protected String doInBackground(Void... params) {
				try{
					InputStream in = new URL(call).openStream();
					StringBuilder sb = new StringBuilder();
					try {
						BufferedReader bf = new BufferedReader(new InputStreamReader(in));
						String s;
						while((s=bf.readLine())!=null)
							sb.append(s);
					}finally{
						in.close();
					}
					return sb.toString();
				}catch(Exception e){
					return null;
				}
			}
			@Override
			protected void onPostExecute(String result){
				if(result != null){
					String utt = null;
					try{
						com.fasterxml.jackson.core.JsonParser parser = new JsonFactory().createParser(result);
						for(int i = 0; i < 7; i++){
							String name = parser.getCurrentName();
							parser.nextToken();
				        	if("utt".equals(name)){
				        		utt = parser.getText();
				        	}
				        	else if ("context".equals(name)){
				        		context = parser.getText();
				        	}
				        	parser.nextToken();
						}
					}catch(Exception e){
						Toast.makeText(Dialogue.this, "エラー", Toast.LENGTH_SHORT).show();
					}
					Utt u = new Utt();
					u.setSisterUtt(utt);
					u.setMe(false);
					if(who.equals("noah"))
						noahAdapter.add(u);
					else
						yuaAdapter.add(u);
					list.setSelection(list.getBottom());
					db.execSQL("insert into " + who + " values('" + utt + "', 'false')");
				}else{
					Toast.makeText(Dialogue.this, "エラー", Toast.LENGTH_SHORT).show();
				}
			}
		};
		task.execute();
	}
	
	@Override
	public void onStop(){
		super.onStop();
		pref.edit().putString(who + "_context", context).commit();
	}
}