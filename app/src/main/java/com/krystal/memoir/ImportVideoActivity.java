package com.krystal.memoir;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;
import android.widget.VideoView;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.krystal.memoir.services.TranscodingService;

public class ImportVideoActivity extends Activity implements OnPreparedListener {

	private FrameLayout mFrameLayoutVV = null;
	private RelativeLayout mRelativeLayoutScroll = null;
	private LinearLayout mLinearLayoutContainer = null;
	private VideoView mVideoView = null;
	private ImageView mImageViewPlay = null;
	private SeekBar mSeekBar = null;
	private float mdistanceToTimeRatio = 0, mDuration = 0;
	private int mVideoWidth = 0, mVideoHeight = 0;
	private int mWidth = 0, mHeight = 0;
	private static String mPath = null;
	private String mVideoDate = null;
	private static int VIDEO_IMPORT_FROM_GALLERY = 0;
	private TranscodingServiceBroadcastReceiver mDataBroadcastReceiver = null;
	private MediaMetadataRetriever mMediaRetriever = null;
	private SharedPreferences mPrefs = null;
	private List<Track> tracks = null;
	private double mCorrectedStart = 0, mCorrectedEnd = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		mPrefs = this.getSharedPreferences("com.krystal.memoir",
				Context.MODE_PRIVATE);
	}

	@SuppressLint("NewApi")
	public void setDisplayMatrix() {
		/** Note : For getting the height and width of the screen */
		Display display = getWindowManager().getDefaultDisplay();

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
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == VIDEO_IMPORT_FROM_GALLERY && resultCode == RESULT_OK) {
			boolean verified = false;
			Uri selectedVideoLocation = data.getData();
			mPath = MemoirApplication.getFilePathFromContentUri(
					selectedVideoLocation, getContentResolver());

			try {
				FileInputStream f = new FileInputStream(mPath);
				FileChannel fc = f.getChannel();
				IsoFile isoFile = new IsoFile(fc);
				MovieBox moov = null;
				if (isoFile != null
						&& isoFile.getBoxes(MovieBox.class).size() > 0)
					moov = isoFile.getBoxes(MovieBox.class).get(0);
				if (moov != null && moov.getBoxes(TrackBox.class).size() > 0) {
					for(TrackBox track : moov.getBoxes(TrackBox.class)) {
						TrackHeaderBox thb = track.getTrackHeaderBox();
						
						if(thb.getWidth() == 0 && thb.getHeight() == 0) {
							verified = false;
						} else if (thb.getWidth() != mPrefs.getInt(
								"com.krystal.memoir.standardwidth", 0)
								|| thb.getHeight() != mPrefs.getInt(
										"com.krystal.memoir.standardheight", 0)) {
							mPath = null;
							verified = false;
							break;
						} else {
							if(thb.getMatrix()[3] == -1) {
								//Log.d("asd", "This video seems to be rotated by 90");
								mPath = null;
								verified = false;
							} else {
								verified = true;
							}
							break;
						}
					}
					// Log.i("asd", "movie box details are " + thb.getHeight() +
					// "   getWidth" + thb.getWidth());
				} else {
					//mPath = null;
					verified = false;
				}
				isoFile.close();
				f.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (mPath == null) {
				Toast.makeText(this, "Error importing. See help for details.",
						Toast.LENGTH_LONG).show();
				return;
			}

			mMediaRetriever = new MediaMetadataRetriever();
			try {
				mMediaRetriever.setDataSource(this, selectedVideoLocation);
			} catch (IllegalArgumentException e) {
				try {
					mMediaRetriever.setDataSource(mPath);
				} catch (IllegalArgumentException e1) {
					if (mPath == null) {
						Toast.makeText(this,
								"Error importing. See help for details.",
								Toast.LENGTH_LONG).show();
						return;
					}
				}
			}
			mVideoWidth = Integer
					.parseInt(mMediaRetriever
							.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
			mVideoHeight = Integer
					.parseInt(mMediaRetriever
							.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
			if(!verified) {
				if (mVideoWidth != mPrefs.getInt(
						"com.krystal.memoir.standardwidth", 0)
						|| mVideoHeight != mPrefs.getInt(
								"com.krystal.memoir.standardheight", 0)) {
					
					verified = false;
				} else {
					verified = true;
				}
			}
			
			if(!verified) {
				Toast.makeText(this, "Error importing. See help for details.",
						Toast.LENGTH_LONG).show();
				mPath = null;
				return;
			}
			/*
			 * if (android.os.Build.VERSION.SDK_INT >= 14 &&
			 * android.os.Build.VERSION.SDK_INT <= 16) {
			 * 
			 * Bitmap bmp = mMediaRetriever.getFrameAtTime(0); if
			 * (bmp.getHeight() > bmp.getWidth()) { Toast.makeText( this,
			 * "This video is in portrait mode and can not be imported",
			 * Toast.LENGTH_LONG).show(); mPath = null; return; }
			 * 
			 * } else if (android.os.Build.VERSION.SDK_INT >= 17) { int
			 * rotationAngle = Integer .parseInt(mMediaRetriever
			 * .extractMetadata
			 * (MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
			 * 
			 * if (rotationAngle == 90 || rotationAngle == 270) {
			 * Toast.makeText( this,
			 * "This video is in portrait mode and can not be imported",
			 * Toast.LENGTH_LONG).show(); mPath = null; return; } }
			 */

			/**
			 * Note: Retrieving video date from Content URI is more acurate than
			 * from MetadataRetriever
			 */
			mVideoDate = MemoirApplication.getDateFromContentUri(
					selectedVideoLocation, getContentResolver());
			if (mVideoDate == null) {
				mVideoDate = mMediaRetriever.extractMetadata(
						MediaMetadataRetriever.METADATA_KEY_DATE).substring(0,
						8);
			}

			mDuration = Float
					.parseFloat(mMediaRetriever
							.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000;

			setContentView(R.layout.activity_import_video);
			mFrameLayoutVV = (FrameLayout) findViewById(R.id.ImportVideoFL1);
			mRelativeLayoutScroll = (RelativeLayout) findViewById(R.id.ImportVideoRL1);
			mVideoView = (VideoView) findViewById(R.id.ImportVideoVV1);

			mLinearLayoutContainer = (LinearLayout) findViewById(R.id.ImportVideoLLContainer);
			mImageViewPlay = (ImageView) findViewById(R.id.ImportVideoIVPlay);
			mSeekBar = (SeekBar) findViewById(R.id.ImportVideoSB);

			setDisplayMatrix();
			mFrameLayoutVV.setLayoutParams(new LinearLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, (int) (mHeight * 5 / 6)));
			mRelativeLayoutScroll
					.setLayoutParams(new LinearLayout.LayoutParams(
							LayoutParams.MATCH_PARENT, (int) (mHeight * 1 / 6)));
			mVideoView.setLayoutParams(new FrameLayout.LayoutParams(
					(int) (mWidth * 5 / 6), LayoutParams.MATCH_PARENT,
					Gravity.CENTER));
		} else {
			mPath = null;
			finish();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mPath == null) {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("video/*");
			startActivityForResult(intent, VIDEO_IMPORT_FROM_GALLERY);
		} else {
			mVideoView.setVideoPath(mPath);
			mVideoView.requestFocus();

			mVideoView.setOnPreparedListener(this);

			mVideoView.setOnErrorListener(new OnErrorListener() {
				@Override
				public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
					Log.e("asd", "Cant Play This Video :(");
					return true;
				}

			});

			mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				@Override
				public void onProgressChanged(SeekBar view, int position,
						boolean arg2) {
					mCorrectedStart = (float) position / (float) 10;
					mCorrectedEnd = mCorrectedStart
							+ (float) (mPrefs.getInt(
									"com.krystal.memoir.noofseconds", 1));

					// Log.i("asd", "Original correctedStart " + mCorrectedStart
					// + "  mCorrectedEnd" + mCorrectedEnd);
					boolean timeCorrected = false;

					if (tracks != null) {
						for (Track track : tracks) {
							if (track.getSyncSamples() != null
									&& track.getSyncSamples().length > 0) {
								if (timeCorrected) {
									throw new RuntimeException(
											"The startTime has already been corrected by another track with SyncSample. Not Supported.");
								}
								mCorrectedStart = correctTimeToSyncSample(
										track, mCorrectedStart, false);
								if (mCorrectedEnd < mDuration) {
									mCorrectedEnd = correctTimeToSyncSample(
											track, mCorrectedEnd, true);
								} else {
									mCorrectedEnd = mDuration;
								}
								timeCorrected = true;
							}
						}
					}

					// Log.i("asd", "After Alteration correctedStart "
					// + mCorrectedStart + "  mCorrectedEnd"
					// + mCorrectedEnd);
					mVideoView.seekTo((int) mCorrectedStart * 1000);
				}

				@Override
				public void onStartTrackingTouch(SeekBar arg0) {
				}

				@Override
				public void onStopTrackingTouch(SeekBar arg0) {
				}
			});

			mImageViewPlay.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					mImageViewPlay.setVisibility(View.INVISIBLE);
					long time = (long) ((mCorrectedEnd - mCorrectedStart) * 1000);
					mVideoView.start();
					mVideoView.postDelayed(new Runnable() {

						@Override
						public void run() {
							mVideoView.pause();
							mVideoView.seekTo((int) mCorrectedStart * 1000);
							mImageViewPlay.setVisibility(View.VISIBLE);
						}
					}, time);
				}
			});

			((ImageView) findViewById(R.id.ImportVideoIVLeft))
					.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View view) {
							mSeekBar.setProgress(mSeekBar.getProgress() - 1);
						}
					});

			((ImageView) findViewById(R.id.ImportVideoIVRight))
					.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View view) {
							mSeekBar.setProgress(mSeekBar.getProgress() + 1);
						}
					});
			((ImageView) findViewById(R.id.ImportVideoIVSelect))
					.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View view) {
							view.setBackgroundColor(getResources().getColor(
									R.color.selectTransparentBlue));

							Intent intent = new Intent(
									ImportVideoActivity.this,
									TranscodingService.class);
							intent.setAction(TranscodingService.ActionTrimVideo);
							intent.putExtra("filePath", mPath);
							intent.putExtra("startTime", mCorrectedStart);
							intent.putExtra("endTime", mCorrectedEnd);
							intent.putExtra(
									"outputFilePath",
									MemoirApplication
											.getOutputMediaFile(ImportVideoActivity.this));
							startService(intent);
						}
					});

			((ImageView) findViewById(R.id.ImportVideoIVCancel))
					.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View view) {
							view.setBackgroundColor(getResources().getColor(
									R.color.selectTransparentBlue));
							mPath = null;
							finish();
						}
					});
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mDataBroadcastReceiver == null)
			mDataBroadcastReceiver = new TranscodingServiceBroadcastReceiver();

		IntentFilter intentFilter = new IntentFilter(
				TranscodingService.ActionTrimVideo);
		LocalBroadcastManager.getInstance(this).registerReceiver(
				mDataBroadcastReceiver, intentFilter);
	}

	@Override
	public void onPrepared(MediaPlayer arg0) {
		int containerImageHeight = mLinearLayoutContainer.getHeight();
		int containerImageWidth = containerImageHeight * mVideoWidth
				/ mVideoHeight;
		int containerWidth = mLinearLayoutContainer.getWidth();
		int containerHeight = mLinearLayoutContainer.getHeight();
		double noOfFrames = Math.floor(containerWidth / containerImageWidth);
		float secondInterval = containerImageWidth * mDuration / containerWidth;

		mdistanceToTimeRatio = mLinearLayoutContainer.getWidth() / mDuration;

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				containerImageWidth, containerImageHeight);

		int i = 0;
		for (i = 0; i < noOfFrames; i++) {
			ImageView iv = new ImageView(this);
			iv.setScaleType(ScaleType.FIT_XY);
			mLinearLayoutContainer.addView(iv, params);
			new getFrameTask().executeOnExecutor(
					AsyncTask.THREAD_POOL_EXECUTOR,
					(new FrameIVStruct(
							Math.round(i * secondInterval * 1000000), iv)));
		}

		mLinearLayoutContainer.setLayoutParams(new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

		int imageWidth = Math.round(mdistanceToTimeRatio
				* mPrefs.getInt("com.krystal.memoir.noofseconds", 1));
		Bitmap bm = Bitmap.createBitmap(imageWidth, containerHeight,
				Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bm);
		c.drawColor(getResources().getColor(R.color.selectTransparentBlue));
		if (imageWidth > 3) {
			Paint p = new Paint();
			p.setColor(0xFF53D5FF);
			c.drawRect(0, 0, 3, containerHeight, p);
			c.drawRect(imageWidth - 3, 0, imageWidth, containerHeight, p);
		}

		Movie movie = null;
		try {
			movie = MovieCreator.build(new FileInputStream(mPath).getChannel());
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		tracks = movie.getTracks();

		Drawable drawable = new BitmapDrawable(getResources(), bm);
		mSeekBar.setThumb(drawable);
		mSeekBar.setThumbOffset((int) mdistanceToTimeRatio);
		mSeekBar.setPadding((int) mdistanceToTimeRatio, 0,
				-(int) mdistanceToTimeRatio, 0);
		mSeekBar.setLayoutParams(new FrameLayout.LayoutParams(
				(int) (noOfFrames * containerImageWidth),
				LayoutParams.MATCH_PARENT));
		mSeekBar.setMax((int) Math.floor(mDuration * 10));
	}

	private double correctTimeToSyncSample(Track track, double cutHere,
			boolean next) {
		double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
		long currentSample = 0;
		double currentTime = 0;
		for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
			TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
			for (int j = 0; j < entry.getCount(); j++) {
				if (Arrays.binarySearch(track.getSyncSamples(),
						currentSample + 1) >= 0) {
					// samples always start with 1 but we start with zero
					// therefore +1
					timeOfSyncSamples[Arrays.binarySearch(
							track.getSyncSamples(), currentSample + 1)] = currentTime;
				}
				currentTime += (double) entry.getDelta()
						/ (double) track.getTrackMetaData().getTimescale();
				currentSample++;
			}
		}
		double previous = 0;
		for (double timeOfSyncSample : timeOfSyncSamples) {
			if (timeOfSyncSample > cutHere) {
				if (next) {
					return timeOfSyncSample;
				} else {
					return previous;
				}
			}
			previous = timeOfSyncSample;
		}
		return timeOfSyncSamples[timeOfSyncSamples.length - 1];
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mDataBroadcastReceiver != null)
			LocalBroadcastManager.getInstance(this).unregisterReceiver(
					mDataBroadcastReceiver);
	}

	public class TranscodingServiceBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra("OutputFileName")) {
				String outputFile = intent.getStringExtra("OutputFileName");
				intent.putExtra("videoDate", mVideoDate);
				intent.putExtra("videoLength",
						(long) ((mCorrectedEnd - mCorrectedStart) * 1000));
				if (!outputFile.isEmpty()) {
					if (getParent() == null) {
						ImportVideoActivity.this.setResult(Activity.RESULT_OK,
								intent);
					} else {
						getParent().setResult(Activity.RESULT_OK, intent);
					}
				} else {
					ImportVideoActivity.this.setResult(
							Activity.RESULT_CANCELED, null);
				}
			} else {
				ImportVideoActivity.this.setResult(Activity.RESULT_CANCELED,
						null);
			}
			mPath = null;
			finish();
		}
	}

	public class FrameIVStruct {
		int frameAt = 0;
		ImageView iv = null;
		Bitmap b = null;

		FrameIVStruct(int fa, ImageView iv) {
			this.frameAt = fa;
			this.iv = iv;
		}
	}

	public class getFrameTask extends
			AsyncTask<FrameIVStruct, Void, FrameIVStruct> {

		@Override
		protected FrameIVStruct doInBackground(FrameIVStruct... arg0) {
			FrameIVStruct struct = arg0[0];
			Bitmap b = mMediaRetriever.getFrameAtTime(struct.frameAt);
			/** NOTE: reducing the size as original images are of 4MB each :( */
			struct.b = Bitmap.createScaledBitmap(b, 160, 120, false);
			return struct;
		}

		@Override
		protected void onPostExecute(FrameIVStruct result) {
			if (result != null) {
				result.iv.setImageBitmap(result.b);
			}
			super.onPostExecute(result);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mPath = null;
	}
}
