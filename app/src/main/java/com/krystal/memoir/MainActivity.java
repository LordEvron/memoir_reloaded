package com.krystal.memoir;

import static androidx.core.content.FileProvider.getUriForFile;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.krystal.memoir.database.Video;
import com.krystal.memoir.services.TranscodingService;

public class MainActivity extends Activity {
	private ShareActionProvider mShareActionProvider;
	public static int VIDEO_CAPTURE = 0;
	public static int VIDEO_IMPORT = 1;
	public static Video mVideo = null;
	public SharedPreferences mPrefs = null;
	TranscodingServiceBroadcastReceiver mDataBroadcastReceiver = null;
	public Fragment mFragment = null;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.main, menu);
		mShareActionProvider = (ShareActionProvider) menu.findItem(
				R.id.action_share_video).getActionProvider();

		shareActionProviderTask();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case android.R.id.home:
			/*
			 * Intent intent = new Intent(this, HomeActivity.class);
			 * intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			 * startActivity(intent);
			 */
			return true;
		case R.id.action_shoot_video:
			if (!mPrefs.getBoolean("com.krystal.memoir.firsttimeshootvideo",
					false)) {
				mPrefs.edit()
						.putBoolean("com.krystal.memoir.firsttimeshootvideo",
								true).commit();
				Toast.makeText(
						MainActivity.this,
						"Shoot videos in landscape mode only. Portrait videos will not be added.",
						Toast.LENGTH_LONG).show();
			}

			if (((MemoirApplication) getApplication()).getDBA()
					.checkVideoInLimit()) {

				SimpleDateFormat ft = new SimpleDateFormat("yyyyMMdd");
				long d = Long.parseLong(ft.format(new Date()));
				mVideo = new Video(0, d,
						MemoirApplication.getOutputMediaFile(this), false, 2,
						true);

				Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

				takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT,
						mPrefs.getInt("com.krystal.memoir.noofseconds", 2));
				takeVideoIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION,
						ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				File videoFile = new File(mVideo.path);


				Uri videoUri= getUriForFile(getBaseContext().getApplicationContext(),"com.krystal.fileprovider",videoFile);

				takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT,
						videoUri);

				takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
				startActivityForResult(takeVideoIntent, VIDEO_CAPTURE);
			} else {
				Toast.makeText(
						MainActivity.this,
						"Max videos reached for the day. Please delete some videos.",
						Toast.LENGTH_LONG).show();
			}
			return true;
		case R.id.action_import_video:
			intent = new Intent(this, ImportVideoActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivityForResult(intent, VIDEO_IMPORT);
			return true;
		case R.id.action_settings:
			intent = new Intent(this, SettingsActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		case R.id.action_help:
			intent = new Intent(this, HelpActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		/**
		 * NOTE: for some reason my activity was not able to pick up correct xml
		 * file based on orientation hence I have done this
		 */
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			setContentView(R.layout.activity_main_landscape);
		} else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			setContentView(R.layout.activity_main_portrait);
		}
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(false);

		mPrefs = this.getSharedPreferences("com.krystal.memoir",
				Context.MODE_PRIVATE);
	}

	public class TranscodingServiceBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra("OutputFileName")) {
				String outputFile = intent.getStringExtra("OutputFileName");

				if (!outputFile.isEmpty()) {
					if (mShareActionProvider != null) {
						shareActionProviderTask();
					}
				}
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mDataBroadcastReceiver == null)
			mDataBroadcastReceiver = new TranscodingServiceBroadcastReceiver();

		IntentFilter intentFilter = new IntentFilter(
				TranscodingService.ActionCreateMyLife);
		LocalBroadcastManager.getInstance(this).registerReceiver(
				mDataBroadcastReceiver, intentFilter);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mDataBroadcastReceiver != null)
			LocalBroadcastManager.getInstance(this).unregisterReceiver(
					mDataBroadcastReceiver);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == VIDEO_CAPTURE && resultCode == RESULT_OK && data != null) {

			if ( new File(mVideo.path).exists()) {

				MediaMetadataRetriever mMediaRetriever = new MediaMetadataRetriever();
				mMediaRetriever.setDataSource(mVideo.path);

				int rotationAngle = Integer
						.parseInt(mMediaRetriever
								.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));

				if (rotationAngle == 90 || rotationAngle == 270) {
					Toast.makeText(
							this,
							"This video is in portrait mode and cannot be imported",
							Toast.LENGTH_LONG).show();
					findViewById(R.id.action_shoot_video).callOnClick();
					return;
				}

				mVideo.length = Long
						.parseLong(mMediaRetriever
								.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
				((MemoirApplication) getApplication()).getDBA()
						.addVideo(mVideo);
				((MemoirApplication) getApplication()).getDBA().selectVideo(
						mVideo);

				SimpleDateFormat ft = new SimpleDateFormat("yyyyMMdd");
				long date = Long.parseLong(ft.format(new Date()));
				mPrefs.edit()
						.putBoolean("com.krystal.memoir.datachanged", true)
						.putLong("com.krystal.memoir.endall", date)
						.putLong("com.krystal.memoir.endselected", date)
						.commit();

				if (mFragment != null) {
					List<List<Video>> videos1 = ((MemoirApplication) this
							.getApplication()).getDBA().getVideos(
							0,
							-1,
							false,
							mPrefs.getBoolean(
									"com.krystal.memoir.showonlymultiple",
									false));

					List<List<Video>> videos2 = ((MyLifeFragment) mFragment).mVideos;

					if (videos1 != null) {
						if (videos2 == null || videos1.size() != videos2.size()) {
							((MyLifeFragment) mFragment).onStart();
						} else {
							int len = videos1.size();
							for (int i = 0; i < len; i++) {
								if (videos1.get(i).size() != videos2.get(i)
										.size()) {
									((MyLifeFragment) mFragment).onStart();
								}
							}
						}
					}
				}
			}
		} else if (requestCode == VIDEO_IMPORT && resultCode == RESULT_OK) {
			long d = Long.parseLong(data.getStringExtra("videoDate"));
			long length = data.getLongExtra("videoLength", 0);
			mVideo = new Video(0, d, data.getStringExtra("OutputFileName"),
					false, length, true);
			((MemoirApplication) getApplication()).getDBA().addVideo(mVideo);
			((MemoirApplication) getApplication()).getDBA().selectVideo(mVideo);

			mPrefs.edit().putBoolean("com.krystal.memoir.datachanged", true)
					.commit();
			if (mPrefs.getLong("com.krystal.memoir.startall", 0) > d) {
				mPrefs.edit().putLong("com.krystal.memoir.startall", d)
						.commit();
			}
			if (mPrefs.getLong("com.krystal.memoir.startselected", 0) > d) {
				mPrefs.edit().putLong("com.krystal.memoir.startselected", d)
						.commit();
			}
			if (mPrefs.getLong("com.krystal.memoir.endall", 0) < d) {
				mPrefs.edit().putLong("com.krystal.memoir.endall", d).commit();
			}
			if (mPrefs.getLong("com.krystal.memoir.endselected", 0) < d) {
				mPrefs.edit().putLong("com.krystal.memoir.endselected", d)
						.commit();
			}

			if (mFragment != null) {
				List<List<Video>> videos1 = ((MemoirApplication) this
						.getApplication()).getDBA().getVideos(
						0,
						-1,
						false,
						mPrefs.getBoolean(
								"com.krystal.memoir.showonlymultiple", false));

				List<List<Video>> videos2 = ((MyLifeFragment) mFragment).mVideos;

				if (videos1 != null) {
					if (videos2 == null || videos1.size() != videos2.size()) {
						((MyLifeFragment) mFragment).onStart();
					} else {
						int len = videos1.size();
						for (int i = 0; i < len; i++) {
							if (videos1.get(i).size() != videos2.get(i).size()) {
								((MyLifeFragment) mFragment).onStart();
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		mFragment = fragment;
		super.onAttachFragment(fragment);
	}

	public void shareActionProviderTask() {
		Video v = ((MemoirApplication) getApplication())
				.getMyLifeFile(getApplicationContext());

		if (v != null && mShareActionProvider != null) {
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType("video/mp4");
			shareIntent.putExtra(Intent.EXTRA_STREAM,
					Uri.fromFile(new File(v.path)));
			mShareActionProvider.setShareIntent(shareIntent);
		}
	}
}
