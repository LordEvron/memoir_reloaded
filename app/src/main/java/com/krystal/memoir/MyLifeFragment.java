package com.krystal.memoir;

import static androidx.core.content.FileProvider.getUriForFile;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.LightingColorFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.krystal.memoir.database.Video;
import com.krystal.memoir.services.TranscodingService;

public class MyLifeFragment extends Fragment {

	private MyLifeDateListArrayAdapter mDateAdapter = null;
	private TranscodingServiceBroadcastReceiver mDataBroadcastReceiver = null;
	private SharedPreferences mPrefs = null;
	private ImageView mMyLifeIV = null, mMyLifeFullscreenIV = null;
	private ProgressBar mMyLifePB = null;
	private TextView mMyLifeTV = null;
	private FrameLayout mMyLifeFL = null;
	protected VideoView mVideoView = null;
	public List<List<Video>> mVideos = null;
	public int mTransparent = 0, mWidth = 0, mHeight = 0, mCWidth = 0,
			mCHeight = 0;
	public Video mMyLifeVideo = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_my_life, container,
				false);
		mPrefs = this.getActivity().getSharedPreferences("com.krystal.memoir",
				Context.MODE_PRIVATE);

		return rootView;
	}

	@SuppressLint("NewApi")
	public void setDisplayMatrix() {
		/** Note : For getting the height and width of the screen */
		Display display = getActivity().getWindowManager().getDefaultDisplay();

		if (android.os.Build.VERSION.SDK_INT >= 14
				&& android.os.Build.VERSION.SDK_INT <= 16) {
			try {
				Method mGetRawH = Display.class.getMethod("getRawHeight");
				Method mGetRawW = Display.class.getMethod("getRawWidth");
				mWidth = (Integer) mGetRawW.invoke(display);
				mHeight = (Integer) mGetRawH.invoke(display);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		} else {
			DisplayMetrics outMetrics = new DisplayMetrics();
			display.getRealMetrics(outMetrics);
			mHeight = outMetrics.heightPixels;
			mWidth = outMetrics.widthPixels;
		}

		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		mCWidth = outMetrics.widthPixels;
		mCHeight = outMetrics.heightPixels;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Activity activity = this.getActivity();
		mMyLifeFL = (FrameLayout) activity.findViewById(R.id.MyLifeFL);
		mMyLifeIV = (ImageView) activity.findViewById(R.id.MyLifeIV);

		mMyLifeFullscreenIV = (ImageView) activity
				.findViewById(R.id.MyLifeFullscreenIV);
		mMyLifePB = (ProgressBar) activity.findViewById(R.id.MyLifePB);
		mMyLifeTV = (TextView) activity.findViewById(R.id.MyLifeTV);
		mTransparent = getResources().getColor(android.R.color.black);

		mVideoView = (VideoView) activity.findViewById(R.id.MyLifeVV);
		new MemoirMediaController(getActivity(), mMyLifeFL, mVideoView);
		mVideoView.requestFocus();

		mVideoView
				.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

					@Override
					public void onCompletion(MediaPlayer vmp) {
						mMyLifeVideo = ((MemoirApplication) getActivity()
								.getApplication()).getMyLifeFile(getActivity()
								.getApplicationContext());
						if (mMyLifeVideo != null) {
							updateMyLifeViews(R.drawable.play,
									mMyLifeVideo.thumbnailPath, View.VISIBLE,
									View.INVISIBLE, View.VISIBLE);
						} else {
							updateMyLifeViews(R.drawable.no_video, null,
									View.VISIBLE, View.INVISIBLE,
									View.INVISIBLE);
						}
					}
				});

		mVideoView.setOnErrorListener(new OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
				Log.e("asd", "Cant Play This Video :(");
				return true;
			}
		});

		mMyLifeIV.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (view.getTag() != null) {
					updateMyLifeViews(R.drawable.play, null, View.INVISIBLE,
							View.INVISIBLE, View.INVISIBLE);
					mVideoView.start();
				}
			}
		});

		mMyLifeFullscreenIV.setOnClickListener(new OnClickListener() {

			@RequiresApi(api = Build.VERSION_CODES.M)
			@Override
			public void onClick(View view) {
				if (mMyLifeVideo != null) {
					Intent playIntent = new Intent();
					playIntent.setAction(Intent.ACTION_VIEW);
					playIntent.setDataAndType(
							getUriForFile(getContext().getApplicationContext(),"com.krystal.fileprovider",new File(mMyLifeVideo.path)),
							"video/*");

					getActivity().startActivity(playIntent);
				}
			}
		});
	}

	public void onVideoViewPrepared() {
		if (mMyLifeVideo != null) {

			updateMyLifeViews(R.drawable.play, mMyLifeVideo.thumbnailPath,
					View.VISIBLE, View.INVISIBLE, View.VISIBLE);

		}
	}

	@Override
	public void onStart() {
		super.onStart();

		((MemoirApplication) getActivity().getApplication()).getDBA()
				.setStartEndDates();

		mVideos = ((MemoirApplication) getActivity().getApplication()).getDBA()
				.getVideos(
						0,
						-1,
						false,
						mPrefs.getBoolean("com.krystal.memoir.showonlymultiple",
								false));
		mDateAdapter = new MyLifeDateListArrayAdapter(getActivity(), mVideos);
		ListView mDateList = (ListView) getActivity().findViewById(
				R.id.MyLifeDateLV);
		mDateList.setAdapter(mDateAdapter);
	}

	public void refreshLifeTimeVideo() {

		updateMyLifeViews(R.drawable.no_video, null, View.INVISIBLE,
				View.VISIBLE, View.INVISIBLE);

		Intent intent = new Intent(getActivity(), TranscodingService.class);
		intent.setAction(TranscodingService.ActionCreateMyLife);
		intent.putExtra("startDate",
				mPrefs.getLong("com.krystal.memoir.startselected", 0));
		intent.putExtra("endDate",
				mPrefs.getLong("com.krystal.memoir.endselected", 0));
		getActivity().startService(intent);
		mMyLifeVideo = null;
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mDataBroadcastReceiver == null)
			mDataBroadcastReceiver = new TranscodingServiceBroadcastReceiver();

		IntentFilter intentFilter = new IntentFilter(
				TranscodingService.ActionCreateMyLife);
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
				mDataBroadcastReceiver, intentFilter);

		if (mPrefs.getBoolean("com.krystal.memoir.datachanged", true) == true) {
			mPrefs.edit().putBoolean("com.krystal.memoir.datachanged", false)
					.commit();
			refreshLifeTimeVideo();
		} else {
			mMyLifeVideo = ((MemoirApplication) getActivity().getApplication())
					.getMyLifeFile(getActivity().getApplicationContext());
			if (mMyLifeVideo != null) {
				((VideoView) getActivity().findViewById(R.id.MyLifeVV))
						.setVideoPath(mMyLifeVideo.path);
			} else {
				updateMyLifeViews(R.drawable.no_video, null, View.VISIBLE,
						View.INVISIBLE, View.INVISIBLE);
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mDataBroadcastReceiver != null)
			LocalBroadcastManager.getInstance(getActivity())
					.unregisterReceiver(mDataBroadcastReceiver);
	}

	public void updateMyLifeViews(int IVRes, String IVPath, int IVVis,
			int PBVis, int TVVis) {

		mMyLifeIV.setImageResource(IVRes);
		if (android.os.Build.VERSION.SDK_INT == 14
				|| android.os.Build.VERSION.SDK_INT == 15) {
			mMyLifeIV.setAlpha(200);
		} else if (android.os.Build.VERSION.SDK_INT >= 16) {
			mMyLifeIV.setImageAlpha(200);
		}
		if (IVPath != null) {
			mMyLifeIV.setBackgroundResource(R.drawable.videothumbnail);
			mMyLifeIV.setTag(IVPath);
			mMyLifeFullscreenIV.setVisibility(View.VISIBLE);
		} else {
			mMyLifeIV.setBackgroundColor(mTransparent);
			mMyLifeIV.setTag(null);
			mMyLifeFullscreenIV.setVisibility(View.INVISIBLE);
		}
		mMyLifeIV.requestLayout();
		mMyLifeIV.setVisibility(IVVis);

		mMyLifePB.setVisibility(PBVis);

		String text = MemoirApplication.convertDate(
				mPrefs.getLong("com.krystal.memoir.startselected", 0),
				"Long Time Ago")
				+ " - "
				+ MemoirApplication.convertDate(
						mPrefs.getLong("com.krystal.memoir.endselected", 0),
						"Now");
		mMyLifeTV.setText(text);
		mMyLifeTV.setVisibility(TVVis);
	}

	public class TranscodingServiceBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra("OutputFileName") && getActivity() != null) {
				String outputFile = intent.getStringExtra("OutputFileName");

				if (!outputFile.isEmpty()) {

					mMyLifeVideo = ((MemoirApplication) getActivity()
							.getApplication()).getMyLifeFile(getActivity()
							.getApplicationContext());

					if (mMyLifeVideo != null) {
						((VideoView) getActivity().findViewById(R.id.MyLifeVV))
								.setVideoPath(mMyLifeVideo.path);
					}
				} else {
					if (intent.hasExtra("Error")) {
						Toast.makeText(getActivity(),
								intent.getStringExtra("Error"),
								Toast.LENGTH_LONG).show();

					}
					updateMyLifeViews(R.drawable.no_video, null, View.VISIBLE,
							View.INVISIBLE, View.INVISIBLE);
				}
			}
		}
	}

	public class MyLifeDateListArrayAdapter extends ArrayAdapter<List<Video>>
			implements View.OnClickListener, View.OnLongClickListener {

		private List<List<Video>> mList;
		private LayoutInflater mInflater;
		private LinearLayout mLinearLayout;
		private Object mActionMode;
		private Video mSelectedVideo = null;
		private ImageView mSelectedVideoIV = null;
		private ViewGroup.MarginLayoutParams params = null;

		public MyLifeDateListArrayAdapter(Context context,
				List<List<Video>> List) {
			super(context, R.layout.fragment_my_life_list_item);

			this.mList = List;
			this.mInflater = LayoutInflater.from(context);
			this.params = new ViewGroup.MarginLayoutParams(
					(int) TypedValue.applyDimension(
							TypedValue.COMPLEX_UNIT_DIP, 90, getResources()
									.getDisplayMetrics()),
					(int) TypedValue.applyDimension(
							TypedValue.COMPLEX_UNIT_DIP, 68, getResources()
									.getDisplayMetrics()));
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			List<Video> VideoList = this.mList.get(position);
			long date = VideoList.get(0).date;

			if (convertView == null) {
				convertView = mInflater.inflate(
						R.layout.fragment_my_life_list_item, null);
			} else {
				if (Long.valueOf((Long) ((TextView) convertView
						.findViewById(R.id.MyLifeListItemTV)).getTag()) != date) {
					/**
					 * NOTE: Comparing dates of this list item and the item from
					 * position if they are same then continue otherwise needs
					 * to flush the view
					 */

					((LinearLayout) convertView
							.findViewById(R.id.MyLifeListItemInnerLL))
							.removeAllViews();
				} else if (convertView.getTag() != null
						&& convertView.getTag().equals("dirty")) {
					/**
					 * NOTE: Comparing even if the dates are same but if its
					 * marked dirty then redraw
					 */

					((LinearLayout) convertView
							.findViewById(R.id.MyLifeListItemInnerLL))
							.removeAllViews();
				} else {
					return convertView;
				}
			}
			/** NOTE: Setting the background color of tiles */
			if (position % 2 == 0) {
				convertView.setBackgroundResource(R.drawable.alterselector1);
			} else {
				convertView.setBackgroundResource(R.drawable.alterselector2);
			}

			((TextView) convertView.findViewById(R.id.MyLifeListItemTV))
					.setText(MemoirApplication.getDateStringWRTToday(date));
			((TextView) convertView.findViewById(R.id.MyLifeListItemTV))
					.setTag(new Long(date));

			mLinearLayout = (LinearLayout) convertView
					.findViewById(R.id.MyLifeListItemInnerLL);

			for (Video v : VideoList) {

				ImageView iv = (ImageView) mInflater.inflate(
						R.layout.fragment_my_life_video_item, null);
				mLinearLayout.addView(iv, this.params);

				MemoirApplication.mTL.loadImage(v.path, iv);
				iv.setTag(v);

				if (v.selected) {
					iv.setBackgroundColor(getResources().getColor(
							R.color.selectBlue));
				}

				iv.setOnClickListener(this);
				iv.setOnLongClickListener(this);
			}

			return convertView;
		}

		@Override
		public boolean onLongClick(View view) {
			if (mActionMode != null) {
				return false;
			}
			((ImageView) view).setColorFilter(new LightingColorFilter(0xFFFFFF,
					0x005050));

			mActionMode = getActivity().startActionMode(mActionModeCallback);
			mSelectedVideoIV = (ImageView) view;
			mSelectedVideo = (Video) mSelectedVideoIV.getTag();
			return true;
		}

		@Override
		public void onClick(View view) {
			Video v = (Video) view.getTag();
			Intent playIntent = new Intent();
			playIntent.setAction(Intent.ACTION_VIEW);
			File myfile = new File(v.path);
			boolean exs= myfile.exists();
			Uri videouri = getUriForFile(getContext().getApplicationContext(),"com.krystal.fileprovider",myfile);

			playIntent.setDataAndType(videouri, "video/*");

			getActivity().startActivity(playIntent);
		}

		private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.my_life_fragment_contextual_menu, menu);
				return true;
			}

			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

				switch (item.getItemId()) {
				case R.id.select_button:
					mode.finish(); // Action picked, so close the CAB
					if (mSelectedVideo != null) {
						((MemoirApplication) getActivity().getApplication())
								.getDBA().selectVideo(mSelectedVideo);
						mSelectedVideoIV.setBackgroundColor(getResources()
								.getColor(R.color.selectBlue));

						for (List<Video> videoList : mList) {
							if (videoList.get(0).date == mSelectedVideo.date) {
								for (Video v : videoList) {
									if (!v.path.equals(mSelectedVideo.path)) {
										v.selected = false;
									}
								}
							}
						}
						mSelectedVideo.selected = true;
						LinearLayout container = (LinearLayout) mSelectedVideoIV
								.getParent().getParent().getParent();
						container.setTag("dirty");
						mDateAdapter.notifyDataSetChanged();
						mSelectedVideo = null;

						/** NOTE: Function call to refresh the LifeTime Video */
						refreshLifeTimeVideo();
					}
					return true;
				case R.id.delete_button:
					mode.finish(); // Action picked, so close the CAB
					if (mSelectedVideo != null) {
						((MemoirApplication) getActivity().getApplication())
								.getDBA().deleteVideo(mSelectedVideo);

						List<Video> videoListRef = null;
						Video videoRef = null;
						for (List<Video> videoList : mList) {
							if (videoList.get(0).date == mSelectedVideo.date) {
								videoListRef = videoList;
								videoRef = null;
								for (Video v : videoList) {
									if (v.path.equals(mSelectedVideo.path)) {
										videoRef = v;
										break;
									}
								}
								if (videoRef != null)
									break;
							}
						}

						if (videoRef != null) {
							videoListRef.remove(videoRef);
							if (mSelectedVideo.selected
									&& !videoListRef.isEmpty()) {
								Video tmpV = videoListRef.get(0);
								tmpV.selected = true;
								((MemoirApplication) getActivity()
										.getApplication()).getDBA()
										.selectVideo(tmpV);

								/**
								 * NOTE: Function call to refresh the LifeTime
								 * Video
								 */
								refreshLifeTimeVideo();
							} else if (videoListRef.isEmpty()) {
								mList.remove(videoListRef);

								/**
								 * NOTE: Function call to refresh the LifeTime
								 * Video
								 */
								refreshLifeTimeVideo();
							}
						}

						LinearLayout container = (LinearLayout) mSelectedVideoIV
								.getParent().getParent().getParent();
						container.setTag("dirty");
						mDateAdapter.notifyDataSetChanged();
						mSelectedVideo = null;
					}
					return true;
				default:
					return false;
				}
			}

			public void onDestroyActionMode(ActionMode mode) {
				mActionMode = null;
				mSelectedVideoIV.setColorFilter(null);
			}
		};

		@Override
		public int getCount() {
			if (mList == null)
				return 0;
			return mList.size();
		}

		@Override
		public List<Video> getItem(int position) {
			return mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
	}
}
