package com.krystal.memoir;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class MyLifeFragmentPortrait extends MyLifeFragment {

	private FrameLayout mMyLifeFL = null;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Activity activity = this.getActivity();
		mMyLifeFL = (FrameLayout) activity.findViewById(R.id.MyLifeFL);
		mTransparent = getResources().getColor(android.R.color.black);

		ViewTreeObserver vto = mMyLifeFL.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			@SuppressLint("NewApi")
			@Override
			public void onGlobalLayout() {
				setDisplayMatrix();

				mMyLifeFL.setLayoutParams(new RelativeLayout.LayoutParams(
						mWidth, (int) (mWidth * mWidth / mHeight)));

				ViewTreeObserver vto = mMyLifeFL.getViewTreeObserver();
				if (android.os.Build.VERSION.SDK_INT >= 14
						&& android.os.Build.VERSION.SDK_INT <= 16) {
					vto.removeGlobalOnLayoutListener(this);
				} else if (android.os.Build.VERSION.SDK_INT >= 17) {
					vto.removeOnGlobalLayoutListener(this);
				}
			}
		});

		mVideoView.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				MyLifeFragmentPortrait.this.onVideoViewPrepared();
			}
		});
	}
}
