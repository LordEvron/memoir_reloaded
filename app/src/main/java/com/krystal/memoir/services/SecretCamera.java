package com.krystal.memoir.services;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import androidx.core.app.NotificationCompat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

import com.krystal.memoir.MainActivity;
import com.krystal.memoir.MemoirApplication;
import com.krystal.memoir.R;
import com.krystal.memoir.database.MemoirDBA;
import com.krystal.memoir.database.Video;

// TODO : Add orientation support right now if the phone is held in right hand the videos would be 180 degree reversed.
// Remove the camera sound 

public class SecretCamera extends Service {

	private Camera mCamera;
	private CameraPreview mPreview;
	private MediaRecorder mMediaRecorder;
	private Video mVideo = null;
	private WindowManager mWindowManager = null;
	private SharedPreferences mPrefs = null;

	@Override
	public void onCreate() {
		super.onCreate();

		mPrefs = this.getSharedPreferences("com.krystal.memoir",
				Context.MODE_PRIVATE);

		MemoirDBA dba = ((MemoirApplication) getApplication()).getDBA();

		if (dba.checkVideoInLimit() && !dba.checkIfAnyUserVideo()
				&& mPrefs.getBoolean("com.krystal.memoir.shootoncall", true)) {

			mCamera = getCameraInstance();

			mPreview = new CameraPreview(this.getApplicationContext(), mCamera);

			mWindowManager = (WindowManager) this
					.getSystemService(Context.WINDOW_SERVICE);
			LayoutParams params = new WindowManager.LayoutParams(1, 1,
					WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
					WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
					PixelFormat.TRANSLUCENT);

			mPreview.setZOrderOnTop(true);
			mPreview.mHolder.setFormat(PixelFormat.TRANSPARENT);
			mWindowManager.addView(mPreview, params);
		} else {
			stopSelf();
		}
	}

	private boolean prepareVideoRecorder() {

		mMediaRecorder = new MediaRecorder();

		mCamera.lock();
		mCamera.unlock();
		mMediaRecorder.setCamera(mCamera);
		// mCamera.setDisplayOrientation(180);
		// mCamera.enableShutterSound(false);

		// AudioManager mgr = (AudioManager)
		// getSystemService(Context.AUDIO_SERVICE);
		// mgr.setStreamMute(AudioManager.STREAM_MUSIC, true);

		mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

		mMediaRecorder.setProfile(CamcorderProfile
				.get(CamcorderProfile.QUALITY_HIGH));

		// mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

		SimpleDateFormat ft = new SimpleDateFormat("yyyyMMdd");
		long d = Long.parseLong(ft.format(new Date()));

		mVideo = new Video(0, d, MemoirApplication.getOutputMediaFile(this),
				false, 2, false);
		mMediaRecorder.setOutputFile(mVideo.path);

		mMediaRecorder.setMaxDuration(mPrefs.getInt(
				"com.krystal.memoir.noofseconds", 1) * 1000);
		mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
			@Override
			public void onInfo(MediaRecorder mr, int what, int extra) {
				if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
					stopRecording();
				}
			}
		});

		try {
			mMediaRecorder.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			releaseMediaRecorder();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			releaseMediaRecorder();
			return false;
		}
		return true;
	}

	public void startRecording() {
		try {
			if (prepareVideoRecorder()) {
				mMediaRecorder.start();
			} else {
				releaseMediaRecorder();
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			releaseMediaRecorder();
		}
	}

	public void stopRecording() {
		try {
			mMediaRecorder.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		releaseMediaRecorder();
		releaseCamera();
		mWindowManager.removeView(mPreview);
		// AudioManager mgr = (AudioManager)
		// getSystemService(Context.AUDIO_SERVICE);
		// mgr.setStreamMute(AudioManager.STREAM_MUSIC, false);

		((MemoirApplication) getApplication()).getDBA().addVideo(mVideo);
		((MemoirApplication) getApplication()).getDBA().selectVideo(mVideo);

		SharedPreferences mPrefs = this.getSharedPreferences(
				"com.krystal.memoir", Context.MODE_PRIVATE);
		mPrefs.edit().putBoolean("com.krystal.memoir.datachanged", true)
				.commit();

		showNotification(mVideo);
		stopSelf();
	}

	public void showNotification(Video v) {

		/*
		 * MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		 * mmr.setDataSource(v.path); Bitmap b = mmr.getFrameAtTime(2000000);
		 */
		Bitmap b = ThumbnailUtils.createVideoThumbnail(v.path,
				MediaStore.Video.Thumbnails.MICRO_KIND);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this)
				.setSmallIcon(R.drawable.icon)
				.setAutoCancel(true)
				.setLargeIcon(b)
				.setContentTitle(
						"Memoir has taken a video while you were on call")
				.setContentText(
						"Memoir provides a feature that it would take a video while you are on call so incase you forget to take a video for a day , it does it for you");

		Intent resultIntent = new Intent(this, MainActivity.class);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
				resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		mBuilder.setContentIntent(resultPendingIntent);

		int mNotificationId = 1;
		NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotifyMgr.notify(mNotificationId, mBuilder.build());
	}

	private void releaseMediaRecorder() {
		if (mMediaRecorder != null) {
			mMediaRecorder.reset();
			mMediaRecorder.release();
			mMediaRecorder = null;
			// mCamera.lock();
		}
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}

	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return c;
	}

	public class CameraPreview extends SurfaceView implements
			SurfaceHolder.Callback {
		private SurfaceHolder mHolder;
		private Camera mCamera;
		boolean previewing = false;

		public CameraPreview(Context context, Camera camera) {
			super(context);
			mCamera = camera;
			mHolder = getHolder();
			mHolder.addCallback(this);
		}

		public void surfaceCreated(SurfaceHolder holder) {
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int w,
				int h) {

			try {
				if ((mCamera != null) && (previewing == false)) {
					mCamera.setPreviewDisplay(holder);
					mCamera.startPreview();
					Handler handler = new Handler();
					handler.postDelayed(new Runnable() {

						@Override
						public void run() {
							startRecording();
						}
					}, 500);
					previewing = true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_NOT_STICKY;
	}
}
