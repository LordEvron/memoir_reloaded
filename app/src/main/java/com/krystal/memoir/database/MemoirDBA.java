package com.krystal.memoir.database;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.krystal.memoir.MemoirApplication;

public class MemoirDBA extends SQLiteOpenHelper {

	private static String DATABASE_NAME = "memoir.db";
	private static int DATABASE_VERSION = 1;
	private Context cxt = null;
	private List<List<Video>> localVideos = null;
	private long localSD = 0, localED = 0;
	private boolean localSelected = false, localShowOnlyMultiple = false;

	private SharedPreferences mPrefs = null;
	private String VIDEOS_TABLE_NAME = "videos";

	private String V_TABLE_KEY = "key";
	private String V_TABLE_DATE = "date";
	private String V_TABLE_PATH = "path";
	private String V_TABLE_THUMBNAIL_PATH = "thumbnail";
	private String V_TABLE_SELECTED = "selected";
	private String V_TABLE_LENGTH = "length";
	private String V_TABLE_USER_TAKEN = "usertaken";

	private int V_TABLE_KEY_INDEX = 0;
	private int V_TABLE_DATE_INDEX = 1;
	private int V_TABLE_PATH_INDEX = 2;
	private int V_TABLE_THUMBNAIL_PATH_INDEX = 3;
	private int V_TABLE_SELECTED_INDEX = 4;
	private int V_TABLE_LENGTH_INDEX = 5;
	private int V_TABLE_USER_TAKEN_INDEX = 6;

	private String VIDEOS_TABLE_CREATE = "CREATE TABLE " + VIDEOS_TABLE_NAME
			+ " (" + V_TABLE_KEY + " integer primary key autoincrement, "
			+ V_TABLE_DATE + " INTEGER, " + V_TABLE_PATH + " TEXT, "
			+ V_TABLE_THUMBNAIL_PATH + " TEXT, " + V_TABLE_SELECTED + " INTEGER, " 
			+ V_TABLE_LENGTH + " INTEGER, " + V_TABLE_USER_TAKEN + " INTEGER);";

	public MemoirDBA(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.cxt = context;
		mPrefs = context.getSharedPreferences("com.krystal.memoir",
				Context.MODE_PRIVATE);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(VIDEOS_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Needs to provide proper upgrade mechanism for this
		db.execSQL("DROP TABLE IF EXISTS " + VIDEOS_TABLE_NAME);
		onCreate(db);
	}

	public List<List<Video>> getVideos(long startDate, long endDate,
			boolean selected, boolean showOnlyMultiple) {
		if (localVideos == null || startDate != localSD || endDate != localED
				|| selected != localSelected
				|| showOnlyMultiple != localShowOnlyMultiple) {
			localVideos = getVideosInternal(this.cxt, startDate, endDate, selected,
					showOnlyMultiple);
			localSD = startDate;
			localED = endDate;
			localSelected = selected;
			localShowOnlyMultiple = showOnlyMultiple;
		}
		return localVideos;
	}

	public long addVideo(Video v) {
		localVideos = null;
		MemoirApplication.mTL.loadImage(v.path, null);
		return addVideoInternal(v);
	}

	private List<List<Video>> getVideosInternal(Context cxt, long startDate,
			long endDate, boolean selected, boolean showOnlyMultiple) {
		SQLiteDatabase db = this.getReadableDatabase();

		long t1, t2;
		t1 = startDate >= 0 ? startDate : 0;
		t2 = endDate > 0 ? endDate : 29990101;

		String selection = V_TABLE_DATE + " >= " + t1 + " AND " + V_TABLE_DATE
				+ " <= " + t2;
		if (selected) {
			selection = selection + " AND " + V_TABLE_SELECTED + " = 1 ";
		}
		String orderBy = V_TABLE_DATE + " DESC ";

		Cursor c = db.query(VIDEOS_TABLE_NAME, null, selection, null, null,
				null, orderBy);

		List<List<Video>> dateList = null;
		long currentDate = 0;
		List<Video> currentVideoList = null;

		if (c.getCount() > 0) {
			dateList = new ArrayList<List<Video>>();
		}

		if (c.moveToFirst()) {
			while (!c.isAfterLast()) {
				long date = c.getLong(V_TABLE_DATE_INDEX);
				if (date != currentDate) {
					currentVideoList = new ArrayList<Video>();
					dateList.add(currentVideoList);
					currentDate = date;
				}

				Video v = new Video(c.getInt(V_TABLE_KEY_INDEX),
						c.getLong(V_TABLE_DATE_INDEX),
						c.getString(V_TABLE_PATH_INDEX),
						c.getString(V_TABLE_THUMBNAIL_PATH_INDEX),
						c.getInt(V_TABLE_SELECTED_INDEX) > 0 ? true : false,
						c.getInt(V_TABLE_LENGTH_INDEX),
						c.getInt(V_TABLE_USER_TAKEN_INDEX) > 0 ? true : false);
				currentVideoList.add(v);
				c.moveToNext();
			}
		}
		c.close();

		List<List<Video>> dateList2 = null;
		if (showOnlyMultiple && dateList != null) {
			for (List<Video> lv : dateList) {
				if (lv.size() > 1) {
					if (dateList2 == null) {
						dateList2 = new ArrayList<List<Video>>();
					}
					dateList2.add(lv);
				}
			}
			return dateList2;
		}

		return dateList;
	}

	private long addVideoInternal(Video v) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(V_TABLE_DATE, v.date);
		values.put(V_TABLE_PATH, v.path);
		values.put(V_TABLE_THUMBNAIL_PATH, v.thumbnailPath);
		values.put(V_TABLE_SELECTED, v.selected == true ? 1 : 0);
		values.put(V_TABLE_LENGTH, v.length);
		values.put(V_TABLE_USER_TAKEN, v.userTaken == true ? 1 : 0);

		return db.insert(VIDEOS_TABLE_NAME, null, values);
	}

	public int deleteVideo(Video v) {

		if (!v.path.isEmpty()) {
			SQLiteDatabase db = this.getWritableDatabase();

			String whereClause = V_TABLE_PATH + " = '" + v.path + "'";
			int returnValue = db.delete(VIDEOS_TABLE_NAME, whereClause, null);

			File file = new File(v.path);
			file.delete();

			file = new File(v.path.substring(0, v.path.length() - 3) + "png");
			file.delete();

			localVideos = null;
			return returnValue;
		}
		return 0;
	}

	public boolean selectVideo(Video v) {
		localVideos = null;
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(V_TABLE_SELECTED, 0);
		String whereClause = V_TABLE_DATE + " = " + v.date;
		db.update(VIDEOS_TABLE_NAME, values, whereClause, null);

		values.remove(V_TABLE_SELECTED);
		values.put(V_TABLE_SELECTED, 1);
		whereClause = V_TABLE_PATH + " = '" + v.path + "'";

		if (db.update(VIDEOS_TABLE_NAME, values, whereClause, null) > 0)
			return true;

		return false;
	}

	public boolean checkVideoInLimit() {
		SQLiteDatabase db = this.getReadableDatabase();
		SimpleDateFormat ft = new SimpleDateFormat("yyyyMMdd");
		long d = Long.parseLong(ft.format(new Date()));

		String selection = V_TABLE_DATE + " = " + d;
		Cursor c = db.query(VIDEOS_TABLE_NAME, null, selection, null, null,
				null, null);
		if (c.getCount() < 5) {
			c.close();
			return true;
		}
		c.close();
		return false;
	}

	public boolean checkIfAnyUserVideo() {
		SQLiteDatabase db = this.getReadableDatabase();
		SimpleDateFormat ft = new SimpleDateFormat("yyyyMMdd");
		long d = Long.parseLong(ft.format(new Date()));
		String selection = V_TABLE_DATE + " = " + d + " AND "
				+ V_TABLE_USER_TAKEN + " > 0";
		Cursor c = db.query(VIDEOS_TABLE_NAME, null, selection, null, null,
				null, null);
		if (c.getCount() > 0) {
			c.close();
			return true;
		}
		c.close();
		return false;
	}

	public void updateDatabase() {
		localVideos = null;
		SQLiteDatabase db = this.getReadableDatabase();
		StringBuilder sb = new StringBuilder("1 = 0");
		String[] columns = { V_TABLE_KEY, V_TABLE_PATH, V_TABLE_THUMBNAIL_PATH };
		Cursor c = db.query(VIDEOS_TABLE_NAME, columns, null, null, null, null,
				null);
		if (c.getCount() > 0) {
			if (c.moveToFirst()) {
				while (!c.isAfterLast()) {
					if (!(new File(c.getString(1)).exists())) {
						sb.append(" or " + V_TABLE_KEY + " = " + c.getInt(0)
								+ " ");
						new File(c.getString(2)).delete();
						mPrefs.edit()
								.putBoolean("com.krystal.memoir.datachanged",
										true).commit();
					}
					c.moveToNext();
				}
			}
		}
		c.close();
		db = this.getWritableDatabase();
		db.delete(VIDEOS_TABLE_NAME, sb.toString(), null);
	}

	public void setStartEndDates() {
		SQLiteDatabase db = this.getReadableDatabase();
		String[] columns = { V_TABLE_DATE };
		String orderBy = V_TABLE_DATE + " DESC";
		Cursor c = db.query(VIDEOS_TABLE_NAME, columns, null, null, null, null,
				orderBy);
		if (c.getCount() > 0) {
			if (c.moveToFirst()) {
				mPrefs.edit().putLong("com.krystal.memoir.endall", c.getInt(0))
						.commit();
			}
		}
		c.close();

		orderBy = V_TABLE_DATE;
		c = db.query(VIDEOS_TABLE_NAME, columns, null, null, null, null,
				orderBy);
		if (c.getCount() > 0) {
			if (c.moveToFirst()) {
				mPrefs.edit()
						.putLong("com.krystal.memoir.startall", c.getInt(0))
						.commit();
			}
		}
		c.close();

		if (mPrefs.getLong("com.krystal.memoir.endselected", 0) > mPrefs
				.getLong("com.krystal.memoir.endall", 0)) {
			mPrefs.edit()
					.putLong("com.krystal.memoir.endselected",
							mPrefs.getLong("com.krystal.memoir.endall", 0))
					.commit();
		}

		if (mPrefs.getLong("com.krystal.memoir.startselected", 0) < mPrefs
				.getLong("com.krystal.memoir.startall", 0)) {
			mPrefs.edit()
					.putLong("com.krystal.memoir.startselected",
							mPrefs.getLong("com.krystal.memoir.startall", 0))
					.commit();
		}

	}

	public void updateDatabaseForThumbnail(String path, String thumbnailPath) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(V_TABLE_THUMBNAIL_PATH, thumbnailPath);
		String whereClause = V_TABLE_PATH + " = '" + path + "'";
		db.update(VIDEOS_TABLE_NAME, values, whereClause, null);
	}

	public void updateDatabaseForOlderEntries(int noofdays) {
		SQLiteDatabase db = this.getReadableDatabase();

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(cal.getTimeInMillis() - noofdays
				* MemoirApplication.DAY_IN_MILLIS - 1000);

		int month = cal.get(Calendar.MONTH) + 1;
		int date = cal.get(Calendar.DATE);
		long t1 = Long.parseLong("" + cal.get(Calendar.YEAR)
				+ (month < 10 ? ("0" + month) : month)
				+ (date < 10 ? ("0" + date) : date));

		String selection = V_TABLE_DATE + " <= " + t1 + " AND "
				+ V_TABLE_SELECTED + " = 0 ";
		String[] columns = { V_TABLE_PATH };
		Cursor c = db.query(VIDEOS_TABLE_NAME, columns, selection, null, null,
				null, null);

		ArrayList<String> videoList = null;

		if (c.getCount() > 0) {
			videoList = new ArrayList<String>();
			if (c.moveToFirst()) {
				while (!c.isAfterLast()) {
					videoList.add(c.getString(0));
					c.moveToNext();
				}
			}
		}

		c.close();

		if (videoList != null) {
			db = this.getWritableDatabase();
			db.beginTransaction();
			String whereClause = null;

			for (String path : videoList) {
				whereClause = V_TABLE_PATH + " = '" + path + "'";
				db.delete(VIDEOS_TABLE_NAME, whereClause, null);

				File file = new File(path);
				file.delete();

				file = new File(path + ".ts");
				file.delete();

				file = new File(path.substring(0, path.length() - 3) + "png");
				file.delete();
			}
			db.setTransactionSuccessful();
			db.endTransaction();
		}
	}

	public void resetAll() {
		SQLiteDatabase db = this.getReadableDatabase();

		String[] columns = { V_TABLE_PATH };
		Cursor c = db.query(VIDEOS_TABLE_NAME, columns, null, null, null, null,
				null);

		ArrayList<String> videoList = null;

		if (c.getCount() > 0) {
			videoList = new ArrayList<String>();
			if (c.moveToFirst()) {
				while (!c.isAfterLast()) {
					videoList.add(c.getString(0));
					c.moveToNext();
				}
			}
		}

		c.close();

		if (videoList != null) {
			for (String path : videoList) {
				File file = new File(path);
				file.delete();
				file = new File(path + ".ts");
				file.delete();
				file = new File(path.substring(0, path.length() - 3) + "png");
				file.delete();
			}
		}

		db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + VIDEOS_TABLE_NAME);
		db.execSQL(VIDEOS_TABLE_CREATE);
		localVideos = null;
	}
}
