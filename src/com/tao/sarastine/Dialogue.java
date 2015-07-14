package com.tao.sarastine;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tao.sarastine.CustomListView.OnSoftKeyShownListener;

import dronjo.products.dronjonail.ColorPickerDialog;
import dronjo.products.dronjonail.ColorPickerDialog.OnColorChangedListener;
import android.R.color;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
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
		listViewListener();
		
		talkText = (EditText)findViewById(R.id.editText1);
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		db = new SQLHelper(this).getWritableDatabase();
		jsonPattern = Pattern.compile(
				"^\\{\"utt\":(.+),\"mode\":\"(markov|api)\",\"context\":\"(\\w+)\",\"sister\":\"(noah|yua)\",\"user_id\":\"[a-zA-Z0-9]+\"\\}$", Pattern.DOTALL);
		
		String who = getIntent().getStringExtra("who");
		this.who = who;
		context = pref.getString(who + "_context", null);
		list.setBackgroundColor(pref.getInt(who + "_backgroundColor", Color.parseColor("#cccccc")));
		switch (who) {
		case "noah":
			noahAdapter = new ListViewAdapter(this, "noah");
			getActionBar().setTitle("のあちゃん");
			getActionBar().setIcon(R.drawable.noah);
			list.setAdapter(noahAdapter);
			loadDialogue("noah");
			break;
		case "yua":
			yuaAdapter = new ListViewAdapter(this, "yua");
			getActionBar().setTitle("ゆあちゃん");
			getActionBar().setIcon(R.drawable.yua);
			list.setAdapter(yuaAdapter);
			loadDialogue("yua");
			break;
		}
	}
	public void listViewListener(){
		list.setListener(new OnSoftKeyShownListener(){
			@Override
			public void onSoftKeyShown(boolean isShown){
				if(isShown)
					list.setSelection(list.getCount() - 1);
			}
		});
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final String utt = ((Utt)parent.getItemAtPosition(position)).getUtt();
				AlertDialog.Builder builder = new AlertDialog.Builder(Dialogue.this);
				builder.setTitle("クリップボードにコピーしますか？")
				.setMessage(utt)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ClipboardManager clipboardManager =(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
						ClipData clipData = ClipData.newPlainText("SARASTINE", utt);
						clipboardManager.setPrimaryClip(clipData);
						Toast.makeText(Dialogue.this, "コピーしました", Toast.LENGTH_SHORT).show();
					}
				});
				builder.setNegativeButton("キャンセル", null);
				builder.create().show();
			}
		});
	}
	
	public void loadDialogue(String who){
		Cursor result = db.query(who, new String[]{"utt", "me", "date"}, null, null, null, null, null);
		boolean mov = result.moveToFirst();
		while(mov){
			Utt u = new Utt();
			boolean me = Boolean.parseBoolean(result.getString(1));
			u.setUtt(result.getString(0));
			u.setMe(me);
			u.setDate(result.getString(2));
			if(who.equals("noah"))
				noahAdapter.add(u);
			else
				yuaAdapter.add(u);
			
			mov = result.moveToNext();
		}
	}
	
	public void send(View v) throws UnsupportedEncodingException{
		String text = talkText.getText().toString();
		talkText.setText("");
		Utt u = new Utt();
		u.setUtt(text);
		u.setMe(true);
		String date = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.JAPANESE).format(new Date());
		u.setDate(date);
		if(who.equals("noah"))
			noahAdapter.add(u);
		else
			yuaAdapter.add(u);
		db.execSQL("insert into " + who + " values('" + text + "', 'true', '" + date + "')");
		list.setSelection(list.getBottom());
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
							u.setUtt(utt);
							u.setMe(false);
							String date = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.JAPANESE).format(new Date());
							u.setDate(date);
							if(who.equals("noah"))
								noahAdapter.add(u);
							else
								yuaAdapter.add(u);
							list.setSelection(list.getBottom());
							db.execSQL("insert into " + who + " values('" + utt + "', 'false', '" + date + "')");
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("背景色");
		menu.add("履歴を削除");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if(item.getTitle().equals("背景色")){
			if(item.getTitle().equals("背景色")){
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setItems(new String[]{"カラーピッカー", "hexを直接"}, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(which == 0){
							ColorPickerDialog cpd = new ColorPickerDialog(Dialogue.this, new OnColorChangedListener() {
								@Override
								public void colorChanged(int color) {
									pref.edit().putInt(who + "_backgroundColor", color).commit();
									Toast.makeText(Dialogue.this, "選択しました", Toast.LENGTH_SHORT).show();
									list.setBackgroundColor(color);
								}
							}, color.darker_gray);
							cpd.show();
						}
						if(which == 1){
							AlertDialog.Builder builder = new AlertDialog.Builder(Dialogue.this);
							final EditText hex = new EditText(Dialogue.this);
							builder.setTitle("hex値を#から入力してください")
							.setView(hex)
							.setPositiveButton("OK", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which){
									int color = -1;
									try{
										String i = hex.getText().toString();
										color = Color.parseColor(i);
									}catch(Exception e){
										Toast.makeText(Dialogue.this, "エラー", Toast.LENGTH_SHORT).show();
										return;
									}
									pref.edit().putInt(who + "_backgroundColor", color).commit();
									Toast.makeText(Dialogue.this, "選択しました", Toast.LENGTH_SHORT).show();
									list.setBackgroundColor(color);
								}
							});
							builder.setNegativeButton("キャンセル", null);
							builder.create().show();
						}
					}
				});
				builder.create().show();
			}
		}
		if(item.getTitle().equals("履歴を削除")){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("本当に削除しますか？")
			.setPositiveButton("削除", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					db.execSQL("drop table " + who);
					pref.edit().putString(who + "_context", null).commit();
					switch(who){
					case "noah":
						noahAdapter.clear();
						db.execSQL("create table noah(utt text, me text, date text)");
						break;
					case "yua":
						yuaAdapter.clear();
						db.execSQL("create table yua(utt text, me text, date text)");
						break;
					}
					loadDialogue(who);
				}
			});
			builder.setNegativeButton("キャンセル", null);
			builder.create().show();
		}
		return super.onOptionsItemSelected(item);
	}
}