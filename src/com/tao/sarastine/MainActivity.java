package com.tao.sarastine;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private SharedPreferences pref;
	
	private Twitter twitter;
	private RequestToken rt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		
		if(pref.getString("user_id", null) == null){
			ninsyo();
		}
	}
	
	public void noah(View v){
		Intent i = new Intent(this, Dialogue.class);
		i.putExtra("who", "noah");
		startActivity(i);
	}
	public void yua(View v){
		Intent i = new Intent(this, Dialogue.class);
		i.putExtra("who", "yua");
		startActivity(i);
	}
	
	public void ninsyo(){
		String CK = getString(R.string.CK);
		String CS = getString(R.string.CS);
		
		Configuration conf = new ConfigurationBuilder()
		.setOAuthConsumerKey(CK).setOAuthConsumerSecret(CS).build();
		
		twitter = new TwitterFactory(conf).getInstance();
		
		AsyncTask<Void, Void, RequestToken> task = new AsyncTask<Void, Void, RequestToken>(){
			@Override
			protected RequestToken doInBackground(Void... params) {
				try {
					rt = twitter.getOAuthRequestToken("sarastine://twitter");
					return rt;
				} catch (TwitterException e) {
					return null;
				}
			}
			@Override
			protected void onPostExecute(RequestToken result){
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
		if (intent == null
                || intent.getData() == null
                || !intent.getData().toString().startsWith("sarastine://twitter")) {
            return;
        }
		final String verifier = intent.getData().getQueryParameter("oauth_verifier");
		
		AsyncTask<Void, Void, AccessToken> task = new AsyncTask<Void, Void, AccessToken>(){
			@Override
			protected AccessToken doInBackground(Void... params) {
				try{
					return twitter.getOAuthAccessToken(rt, verifier);
				}catch(Exception e){
					return null;
				}
			}
			@Override
			protected void onPostExecute(AccessToken result){
				if(result != null){
					pref.edit().putString("user_id", result.getScreenName()).commit();
					Toast.makeText(MainActivity.this, "認証しました", Toast.LENGTH_SHORT).show();
				}else
					Toast.makeText(MainActivity.this, "認証できませんでした", Toast.LENGTH_SHORT).show();
			}
		};
		task.execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}
}