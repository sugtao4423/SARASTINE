package com.tao.sarastine;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity{

	private SharedPreferences pref;

	private Twitter twitter;
	private RequestToken rt;

	private ListView userList;
	private TalkUserListAdapter userListAdapter;
	private SQLiteDatabase db;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		db = new SQLHelper(this).getWritableDatabase();

		userList = (ListView)findViewById(R.id.talkUserList);
		userListAdapter = new TalkUserListAdapter(this);
		userList.setAdapter(userListAdapter);

		userList.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id){
				User user = (User)parent.getItemAtPosition(position);
				Intent intent = new Intent(MainActivity.this, Dialogue.class);
				switch(user.getName()){
				case "のあちゃん":
					intent.putExtra("who", "noah");
					break;
				case "ゆあちゃん":
					intent.putExtra("who", "yua");
					break;
				case "ももかちゃん":
					intent.putExtra("who", "momoka");
					break;
				}
				startActivity(intent);
			}
		});

		pref = PreferenceManager.getDefaultSharedPreferences(this);

		if(pref.getString("user_id", null) == null) {
			ninsyo();
		}
	}

	@Override
	public void onResume(){
		super.onResume();
		userListAdapter.clear();
		Cursor n = db.query("noah", new String[]{"utt", "date"}, null, null, null, null, null);
		User noah = new User("のあちゃん", R.drawable.noah);
		if(n.moveToLast()) {
			noah.setLastTalk(n.getString(0));
			noah.setLastDate(n.getString(1));
		}

		Cursor y = db.query("yua", new String[]{"utt", "date"}, null, null, null, null, null);
		User yua = new User("ゆあちゃん", R.drawable.yua);
		if(y.moveToLast()) {
			yua.setLastTalk(y.getString(0));
			yua.setLastDate(y.getString(1));
		}

		Cursor m = db.query("momoka", new String[]{"utt", "date"}, null, null, null, null, null);
		User momoka = new User("ももかちゃん", R.drawable.momoka);
		if(m.moveToLast()) {
			momoka.setLastTalk(m.getString(0));
			momoka.setLastDate(m.getString(1));
		}

		userListAdapter.add(noah);
		userListAdapter.add(yua);
		userListAdapter.add(momoka);
	}

	public void ninsyo(){
		String CK = getString(R.string.CK);
		String CS = getString(R.string.CS);

		Configuration conf = new ConfigurationBuilder().setOAuthConsumerKey(CK).setOAuthConsumerSecret(CS).build();

		twitter = new TwitterFactory(conf).getInstance();

		AsyncTask<Void, Void, RequestToken> task = new AsyncTask<Void, Void, RequestToken>(){
			private ProgressDialog progDailog;

			@Override
			protected void onPreExecute(){
				progDailog = new ProgressDialog(MainActivity.this);
				progDailog.setMessage("Loading...");
				progDailog.setIndeterminate(false);
				progDailog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progDailog.setCancelable(true);
				progDailog.show();
			}

			@Override
			protected RequestToken doInBackground(Void... params){
				try{
					rt = twitter.getOAuthRequestToken("sarastine://twitter");
					return rt;
				}catch(TwitterException e){
					return null;
				}
			}

			@Override
			protected void onPostExecute(RequestToken result){
				progDailog.dismiss();
				if(result != null)
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(rt.getAuthenticationURL())));
				else
					Toast.makeText(MainActivity.this, "リクエストトークン取得エラー", Toast.LENGTH_SHORT).show();
			}
		};
		task.execute();
	}

	@Override
	protected void onNewIntent(Intent intent){
		super.onNewIntent(intent);
		if(intent == null || intent.getData() == null
				|| !intent.getData().toString().startsWith("sarastine://twitter")) {
			return;
		}
		final String verifier = intent.getData().getQueryParameter("oauth_verifier");

		AsyncTask<Void, Void, AccessToken> task = new AsyncTask<Void, Void, AccessToken>(){
			private ProgressDialog progDailog;

			@Override
			protected void onPreExecute(){
				progDailog = new ProgressDialog(MainActivity.this);
				progDailog.setMessage("Loading...");
				progDailog.setIndeterminate(false);
				progDailog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progDailog.setCancelable(true);
				progDailog.show();
			}

			@Override
			protected AccessToken doInBackground(Void... params){
				try{
					return twitter.getOAuthAccessToken(rt, verifier);
				}catch(Exception e){
					return null;
				}
			}

			@Override
			protected void onPostExecute(AccessToken result){
				progDailog.dismiss();
				if(result != null) {
					pref.edit().putString("user_id", result.getScreenName()).commit();
					Toast.makeText(MainActivity.this, "認証しました", Toast.LENGTH_SHORT).show();
				}else
					Toast.makeText(MainActivity.this, "認証できませんでした", Toast.LENGTH_SHORT).show();
			}
		};
		task.execute();
	}
}