package com.tao.sarastine;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	private Pattern jsonPattern;
	
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
		jsonPattern = Pattern.compile(
				"^\\{\"utt\":(.+),\"mode\":\"(markov|api)\",\"context\":\"(\\w+)\",\"sister\":\"(noah|yua)\",\"user_id\":\"[a-zA-Z0-9]+\"\\}$", Pattern.DOTALL);
		
		String who = getIntent().getStringExtra("who");
		this.who = who;
		switch (who) {
		case "noah":
			noahAdapter = new ListViewAdapter(this, "noah");
			context = pref.getString("noah_context", null);
			getActionBar().setTitle("のあちゃん");
			list.setAdapter(noahAdapter);
			loadDialogue("noah");
			break;
		case "yua":
			yuaAdapter = new ListViewAdapter(this, "yua");
			context = pref.getString("yua_context", null);
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
		String call = null;
		
		if(context == null){
			call = "http://api.flum.pw/apis/dialogue?api_key=" + getString(R.string.sarastyAPI) +
					"&sister=" + who + "&user_id=" + pref.getString("user_id", null) +
					"&mode=markov&utt=" + URLEncoder.encode(text, "utf-8");
		}else{
			call = "http://api.flum.pw/apis/dialogue?api_key=" + getString(R.string.sarastyAPI) +
					"&context=" + context + "&sister=" + who + "&user_id=" + pref.getString("user_id", null) +
					"&mode=markov&utt=" + URLEncoder.encode(text, "utf-8");
		}
		
		AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>(){
			@Override
			protected String doInBackground(String... params) {
				try{
					InputStream in = new URL(params[0]).openStream();
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
					Matcher m = jsonPattern.matcher(result);
					if(m.find()){
						String utt = m.group(1);
						if(utt.equals("null")){
							Toast.makeText(Dialogue.this, "null", Toast.LENGTH_SHORT).show();
						}else{
							utt = utt.substring(1, utt.length() - 1);
							context = m.group(3);
							Utt u = new Utt();
							u.setSisterUtt(utt);
							u.setMe(false);
							if(who.equals("noah"))
								noahAdapter.add(u);
							else
								yuaAdapter.add(u);
							list.setSelection(list.getBottom());
							db.execSQL("insert into " + who + " values('" + utt + "', 'false')");
						}
					}else{
						Toast.makeText(Dialogue.this, "エラー", Toast.LENGTH_SHORT).show();
					}
				}else{
					Toast.makeText(Dialogue.this, "エラー", Toast.LENGTH_SHORT).show();
				}
			}
		};
		task.execute(new String[]{call});
	}
	
	@Override
	public void onStop(){
		super.onStop();
		pref.edit().putString(who + "_context", context).commit();
	}
}