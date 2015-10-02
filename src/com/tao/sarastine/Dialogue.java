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
import org.json.JSONException;
import org.json.JSONObject;

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

public class Dialogue extends Activity{

	private CustomListView list;
	private ListViewAdapter adapter;

	private SharedPreferences pref;
	private String who, context;

	private EditText talkText;

	private SQLiteDatabase db;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		this.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		setContentView(R.layout.dialogue);
		list = (CustomListView)findViewById(R.id.talkList);
		listViewListener();

		talkText = (EditText)findViewById(R.id.editText1);
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		db = new SQLHelper(this).getWritableDatabase();

		who = getIntent().getStringExtra("who");
		context = pref.getString(who + "_context", null);
		list.setBackgroundColor(pref.getInt(who + "_backgroundColor", Color.parseColor("#cccccc")));
		switch(who){
		case "noah":
			adapter = new ListViewAdapter(this, "noah");
			getActionBar().setTitle("のあちゃん");
			getActionBar().setIcon(R.drawable.noah);
			list.setAdapter(adapter);
			loadDialogue("noah");
			break;
		case "yua":
			adapter = new ListViewAdapter(this, "yua");
			getActionBar().setTitle("ゆあちゃん");
			getActionBar().setIcon(R.drawable.yua);
			list.setAdapter(adapter);
			loadDialogue("yua");
			break;
		case "momoka":
			adapter = new ListViewAdapter(this, "momoka");
			getActionBar().setTitle("ももかちゃん");
			getActionBar().setIcon(R.drawable.momoka);
			list.setAdapter(adapter);
			loadDialogue("momoka");
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
		list.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id){
				final String utt = ((Utt)parent.getItemAtPosition(position)).getUtt();
				AlertDialog.Builder builder = new AlertDialog.Builder(Dialogue.this);
				builder.setTitle("クリップボードにコピーしますか？").setMessage(utt).setPositiveButton("OK", new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which){
						ClipboardManager clipboardManager = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
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
			boolean isMe = Boolean.parseBoolean(result.getString(1));
			Utt u = new Utt(result.getString(0), isMe, result.getString(2));
			adapter.add(u);

			mov = result.moveToNext();
		}
	}

	public void send(View v){
		String text = talkText.getText().toString();
		talkText.setText("");
		String date = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.JAPANESE).format(new Date());
		Utt u = new Utt(text, true, date);
		adapter.add(u);
		db.execSQL("insert into " + who + " values('" + text + "', 'true', '" + date + "')");
		list.setSelection(list.getBottom());

		if(who.equals("noah") || who.equals("yua"))
			sarastySisters(text);
		else if(who.equals("momoka"))
			momoka();
	}

	public void sarastySisters(String utt){
		String call = null;

		try{
			if(context == null) {
				call = "http://api.flum.pw/apis/dialogue?api_key=" + getString(R.string.sarastyAPI) + "&sister=sarasty_"
						+ who + "&user_id=" + pref.getString("user_id", null) + "&mode=markov&utt="
						+ URLEncoder.encode(utt, "utf-8");
			}else{
				call = "http://api.flum.pw/apis/dialogue?api_key=" + getString(R.string.sarastyAPI) + "&context="
						+ context + "&sister=sarasty_" + who + "&user_id=" + pref.getString("user_id", null)
						+ "&mode=markov&utt=" + URLEncoder.encode(utt, "utf-8");
			}
		}catch(UnsupportedEncodingException e){
			Toast.makeText(this, "utf-8エンコードエラー", Toast.LENGTH_SHORT).show();
			return;
		}

		AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>(){
			@Override
			protected String doInBackground(String... params){
				try{
					InputStream in = new URL(params[0]).openStream();
					StringBuilder sb = new StringBuilder();
					try{
						BufferedReader bf = new BufferedReader(new InputStreamReader(in));
						String s;
						while((s = bf.readLine()) != null)
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
				if(result != null) {
					JSONObject jsonObj;
					String utt = null, smile = null, context = null;
					try{
						jsonObj = new JSONObject(result);
						utt = jsonObj.getString("utt");
						smile = jsonObj.getString("smile");
						context = jsonObj.getString("context");
					}catch(JSONException e){
						Toast.makeText(Dialogue.this, "JsonParseError", Toast.LENGTH_SHORT).show();
						return;
					}
					if(utt.equals("null")) {
						Toast.makeText(Dialogue.this, "null", Toast.LENGTH_SHORT).show();
					}else{
						Dialogue.this.context = context;
						String date = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.JAPANESE).format(new Date());
						utt = smile + utt;
						Utt u = new Utt(utt, false, date);
						adapter.add(u);
						list.setSelection(list.getBottom());
						db.execSQL("insert into " + who + " values('" + utt + "', 'false', '" + date + "')");
					}
				}else{
					Toast.makeText(Dialogue.this, "result == null", Toast.LENGTH_SHORT).show();
				}
			}
		};
		task.execute(new String[]{call});
	}

	public void momoka(){
		AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>(){
			@Override
			protected String doInBackground(Void... params){
				try{
					InputStream in = new URL(getString(R.string.momokaAddress)).openStream();
					StringBuilder sb = new StringBuilder();
					try{
						BufferedReader bf = new BufferedReader(new InputStreamReader(in));
						String s;
						while((s = bf.readLine()) != null)
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
				if(result != null) {
					Dialogue.this.context = context;
					String date = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.JAPANESE).format(new Date());
					Utt u = new Utt(result, false, date);
					adapter.add(u);
					list.setSelection(list.getBottom());
					db.execSQL("insert into " + who + " values('" + result + "', 'false', '" + date + "')");
				}else{
					Toast.makeText(Dialogue.this, "result == null", Toast.LENGTH_SHORT).show();
				}
			}
		};
		task.execute();
	}

	@Override
	public void onStop(){
		super.onStop();
		if(who.equals("noah") || who.equals("yua"))
			pref.edit().putString(who + "_context", context).commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		menu.add("背景色");
		menu.add("履歴を削除");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if(item.getTitle().equals("背景色")) {
			if(item.getTitle().equals("背景色")) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setItems(new String[]{"カラーピッカー", "hexを直接"}, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which){
						if(which == 0) {
							ColorPickerDialog cpd = new ColorPickerDialog(Dialogue.this, new OnColorChangedListener(){
								@Override
								public void colorChanged(int color){
									pref.edit().putInt(who + "_backgroundColor", color).commit();
									Toast.makeText(Dialogue.this, "選択しました", Toast.LENGTH_SHORT).show();
									list.setBackgroundColor(color);
								}
							}, color.darker_gray);
							cpd.show();
						}
						if(which == 1) {
							AlertDialog.Builder builder = new AlertDialog.Builder(Dialogue.this);
							final EditText hex = new EditText(Dialogue.this);
							builder.setTitle("hex値を#から入力してください").setView(hex).setPositiveButton("OK",
									new DialogInterface.OnClickListener(){
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
		if(item.getTitle().equals("履歴を削除")) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("本当に削除しますか？").setPositiveButton("削除", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which){
					db.execSQL("drop table " + who);
					pref.edit().putString(who + "_context", null).commit();
					adapter.clear();
					switch(who){
					case "noah":
						db.execSQL("create table noah(utt text, me text, date text)");
						break;
					case "yua":
						db.execSQL("create table yua(utt text, me text, date text)");
						break;
					case "momoka":
						db.execSQL("create table momoka(utt text, me text, date text)");
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