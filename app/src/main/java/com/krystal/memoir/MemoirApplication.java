package com.krystal.memoir;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.widget.Toast;

import com.krystal.memoir.database.MemoirDBA;
import com.krystal.memoir.database.Video;
import com.krystal.memoir.services.ThumbnailLoader;

public class MemoirApplication extends Application {

	private MemoirDBA mDBA;
	public static boolean useExternal = true;
	private SharedPreferences mPrefs = null;
	public static ThumbnailLoader mTL = null;
	private static String[] getMonth = { "Jan", "Feb", "Mar", "Apr", "May",
			"Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
	private static String mExtFileDirectory = null;
	public static long DAY_IN_MILLIS = 86400000;

	@Override
	public void onCreate() {
		mDBA = new MemoirDBA(getApplicationContext());
		mTL = ThumbnailLoader.initialize(this, mDBA);
		mPrefs = this.getSharedPreferences("com.krystal.memoir",
				Context.MODE_PRIVATE);

		if(getApplicationContext().getExternalFilesDir(
				Environment.DIRECTORY_MOVIES) == null) {
			Toast.makeText(this,
					"Please restart the phone as your external files directory is not accessible",
					Toast.LENGTH_LONG).show();
			return;
		}
		mExtFileDirectory = getApplicationContext().getExternalFilesDir(
				Environment.DIRECTORY_MOVIES).getAbsolutePath();

		if (!mPrefs.contains("com.krystal.memoir.startall")) {
			SharedPreferences.Editor editor = mPrefs.edit();
			SimpleDateFormat ft = new SimpleDateFormat("yyyyMMdd");
			long date = Long.parseLong(ft.format(new Date()));
			editor.putLong("com.krystal.memoir.startall", date);
			editor.putLong("com.krystal.memoir.endall", date);
			editor.putLong("com.krystal.memoir.startselected", date);
			editor.putLong("com.krystal.memoir.endselected", date);
			editor.putBoolean("com.krystal.memoir.datachanged", false);
			editor.putBoolean("com.krystal.memoir.showonlymultiple", false);
			editor.putBoolean("com.krystal.memoir.shootoncall", false);
			editor.putInt("com.krystal.memoir.noofseconds", 2);
			editor.commit();

			Video v = getMyLifeFile(this);
			if (v != null) {
				File f = new File(v.path);
				f.delete();
			}

			setDefaultCameraResolution();
		}

		this.sendBroadcast(new Intent(
				"com.krystal.memoir.BootupBroadcastReceiver"));
	}

	public static String convertDate(long date, String defaultS) {
		if (date == 0) {
			return defaultS;
		}
		String dateS = String.valueOf(date);
		dateS = dateS.substring(6, 8) + " "
				+ getMonth[(int) ((date % 10000) / 100) - 1] + " "
				+ dateS.substring(0, 4);
		return dateS;
	}

	public static String getDateStringWRTToday(long date) {
		String daysAgo = null;

		long now = System.currentTimeMillis();

		Calendar cal1 = Calendar.getInstance();
		int day = (int) (date % 100);
		int month = (int) ((date % 10000) / 100) - 1;
		int year = (int) (date / 10000);
		cal1.set(year, month, day);

		long then = cal1.getTimeInMillis();
		long difference = now - then;
		int ago = 0;

		if (difference >= DAY_IN_MILLIS - 10000/* Tolerance */) {
			ago = (int) Math.round(difference / DAY_IN_MILLIS);
			if (ago == 1) {
				daysAgo = String.format(Locale.ENGLISH, "%d day ago", ago);
			} else if (ago <= 10) {
				daysAgo = String.format(Locale.ENGLISH, "%d days ago", ago);
			} else {
				daysAgo = String.format(Locale.ENGLISH, "%d %s %d", day,
						getMonth[month], year);
			}
		} else if (difference < -10000 /* Tolerance */) {
			daysAgo = String.format(Locale.ENGLISH, "%d %s %d", day,
					getMonth[month], year);
		} else {
			daysAgo = String.format("Today");
		}
		return daysAgo;
	}

	public static String convertPath(String path) {
		return path.substring(0, path.length() - 3) + "png";
	}

	public MemoirDBA getDBA() {
		return mDBA;
	}

	public Video getMyLifeFile(Context c) {
		String outputFilename = null;

		if (useExternal) {
			outputFilename = c
					.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
					.getAbsolutePath();
		} else
			outputFilename = c.getFilesDir().getAbsolutePath();

		outputFilename = outputFilename.concat("/MyLife.mp4");
		File f = new File(outputFilename);

		if (f.exists()) {
			Video v = new Video(outputFilename);
			if (!(new File(convertPath(outputFilename)).exists())) {
				MemoirApplication.mTL.convertThumbnail(outputFilename,
						MediaStore.Video.Thumbnails.MINI_KIND);
			}
			v.thumbnailPath = convertPath(outputFilename);
			return v;
		}
		return null;
	}

	public void deleteMyLifeFile(Context c) {
		String outputFilename = null;

		if (useExternal) {
			outputFilename = c
					.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
					.getAbsolutePath();
		} else
			outputFilename = c.getFilesDir().getAbsolutePath();

		outputFilename = outputFilename.concat("/MyLife.mp4");
		File f = new File(outputFilename);
		if (f.exists()) {
			f.delete();
		}

		f = new File(outputFilename.substring(0, outputFilename.length() - 3)
				+ "png");
		if (f.exists()) {
			f.delete();
		}
	}

	public static Video getMyLifeFileStatic() {
		String outputFilename = mExtFileDirectory.concat("/MyLife.mp4");
		File f = new File(outputFilename);

		if (f.exists()) {
			Video v = new Video(outputFilename);
			if (!(new File(convertPath(outputFilename)).exists())) {
				MemoirApplication.mTL.convertThumbnail(outputFilename,
						MediaStore.Video.Thumbnails.MINI_KIND);
			}
			v.thumbnailPath = convertPath(outputFilename);
			return v;
		}
		return null;
	}

	public static String getOutputMediaFile(Context c) {
		if (useExternal) {
			String state = Environment.getExternalStorageState();
			if (Environment.MEDIA_MOUNTED.equals(state)) {
				File mediaStorageDir = new File(c.getExternalFilesDir(
						Environment.DIRECTORY_MOVIES).getAbsolutePath(),
						"Memoir");
				if (!mediaStorageDir.exists()) {
					if (!mediaStorageDir.mkdirs()) {
						return null;
					}
				}

				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
						.format(new Date());
				//String fileName = mediaStorageDir.getPath() + File.separator
				//		+ "VID_" + timeStamp + ".mp4";

				String fileName = c.getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES) + File.separator
						+ "VID_" + timeStamp + ".mp4";
				return fileName;
			}
		} else {
			File mediaStorageDir = new File(c.getFilesDir().getAbsolutePath(),
					"Memoir");
			if (!mediaStorageDir.exists()) {
				if (!mediaStorageDir.mkdirs()) {
					return null;
				}
			}
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
					.format(new Date());
			String fileName = mediaStorageDir.getPath() + File.separator
					+ "VID_" + timeStamp + ".mp4";
			return fileName;
		}

		return null;
	}

	@SuppressLint("InlinedApi")
	public static String getFilePathFromContentUri(Uri selectedVideoUri,
			ContentResolver contentResolver) {
		String filePath;
		Cursor cursor = null;
		if (android.os.Build.VERSION.SDK_INT >= 16) {
			String[] filePathColumn = { MediaColumns.DATA, MediaColumns.HEIGHT,
					MediaColumns.WIDTH };
			cursor = contentResolver.query(selectedVideoUri, filePathColumn,
					null, null, null);
		} else {
			String[] filePathColumn = { MediaColumns.DATA };
			cursor = contentResolver.query(selectedVideoUri, filePathColumn,
					null, null, null);
		}

		cursor.moveToFirst();

		filePath = cursor.getString(0);

		if (android.os.Build.VERSION.SDK_INT == 16) {
			int height = cursor.getInt(1);
			int width = cursor.getInt(2);
			cursor.close();
			if (width < height)
				return null;
		}
		return filePath;
	}

	public static String getDateFromContentUri(Uri selectedVideoUri,
			ContentResolver contentResolver) {
		long date;
		String[] dateColumn = { MediaColumns.DATE_ADDED };

		Cursor cursor = contentResolver.query(selectedVideoUri, dateColumn,
				null, null, null);
		cursor.moveToFirst();

		date = cursor.getLong(cursor.getColumnIndex(dateColumn[0]));
		cursor.close();
		String timeStamp = new SimpleDateFormat("yyyyMMdd").format(new Date(
				date * 1000L));
		return timeStamp;
	}

	public void setDefaultCameraResolution() {

		Camera c = null;
		try {
			c = Camera.open();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Parameters param = c.getParameters();
		List<Camera.Size> list = null;
		int maxHeight = 0, maxWidth = 0;

		list = param.getSupportedVideoSizes();
		if (list != null && list.size() > 0) {
			maxWidth = 0;
			for (Size s : list) {
				if (s.width > maxWidth) {
					maxWidth = s.width;
				}
			}
		}
		list = param.getSupportedPreviewSizes();
		if (list != null && list.size() > 0) {
			for (Size s : list) {
				if (s.width > maxWidth) {
					maxWidth = s.width;
				}
			}
		}

		if (maxWidth == 1920) {
			maxHeight = 1080;
		} else if (maxWidth == 1280) {
			maxHeight = 720;
		} else if (maxWidth == 960) {
			maxHeight = 720;
		} else if (maxWidth == 800) {
			maxHeight = 480;
		} else if (maxWidth == 768) {
			maxHeight = 576;
		} else if (maxWidth == 720) {
			maxHeight = 480;
		} else if (maxWidth == 640) {
			maxHeight = 480;
		} else if (maxWidth == 352) {
			maxHeight = 288;
		} else if (maxWidth == 320) {
			maxHeight = 240;
		} else if (maxWidth == 240) {
			maxHeight = 160;
		} else if (maxWidth == 176) {
			maxHeight = 144;
		} else if (maxWidth == 128) {
			maxHeight = 96;
		}

		mPrefs.edit().putInt("com.krystal.memoir.standardheight", maxHeight)
				.putInt("com.krystal.memoir.standardwidth", maxWidth).commit();
	}
}
