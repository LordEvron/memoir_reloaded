package com.krystal.memoir;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class MyLifeFragmentLandscape extends MyLifeFragment {

	private FrameLayout mMyLifeContainerFL = null, mMyLifeFL = null;
	private RelativeLayout mMyLifeDrawerContainerRL = null;
	// private int gAdsHeight = 0;
	private int mDrawerContainerOrigWidth = 0, mDrawerContainerWidth = 0;
	private boolean isExpanded = false;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Activity activity = this.getActivity();

		mMyLifeFL = (FrameLayout) activity.findViewById(R.id.MyLifeFL);
		mMyLifeContainerFL = (FrameLayout) activity
				.findViewById(R.id.MyLifeContainerFL);
		mMyLifeDrawerContainerRL = (RelativeLayout) activity
				.findViewById(R.id.MyLifeDrawerContainer);

		ImageView myLifeDrawerIV = (ImageView) activity
				.findViewById(R.id.MyLifeDrawerIV);
		myLifeDrawerIV.setImageResource(R.drawable.drawer);
		myLifeDrawerIV.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (isExpanded) {
					isExpanded = false;
					mMyLifeDrawerContainerRL
							.setLayoutParams(new FrameLayout.LayoutParams(
									mDrawerContainerOrigWidth,
									LayoutParams.MATCH_PARENT, Gravity.RIGHT));
					((ImageView) view).setImageResource(R.drawable.drawer);
				} else {
					isExpanded = true;
					mMyLifeDrawerContainerRL
							.setLayoutParams(new FrameLayout.LayoutParams(
									mDrawerContainerWidth,
									LayoutParams.MATCH_PARENT, Gravity.RIGHT));
					((ImageView) view)
							.setImageResource(R.drawable.drawerreverse);
				}
			}
		});

		// gAdsHeight = (int) TypedValue.applyDimension(
		// TypedValue.COMPLEX_UNIT_DIP, 32,
		// getResources().getDisplayMetrics());

		mDrawerContainerWidth = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 480, getResources()
						.getDisplayMetrics());

		ViewTreeObserver vto = mMyLifeFL.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			@SuppressLint("NewApi")
			@Override
			public void onGlobalLayout() {
				draw();
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
				draw();
				MyLifeFragmentLandscape.this.onVideoViewPrepared();
			}
		});
	}

	public void draw() {
		setDisplayMatrix();

		int newHeight = mMyLifeContainerFL.getHeight();
		// int newHeight = mMyLifeContainerFL.getHeight() - gAdsHeight;
		mMyLifeContainerFL.setLayoutParams(new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, newHeight));

		mMyLifeFL.setLayoutParams(new FrameLayout.LayoutParams(newHeight
				* mWidth / mHeight, LayoutParams.MATCH_PARENT, Gravity.LEFT));

		mDrawerContainerOrigWidth = mCWidth - (newHeight * mWidth / mHeight);
		if (isExpanded) {
			mMyLifeDrawerContainerRL
					.setLayoutParams(new FrameLayout.LayoutParams(
							mDrawerContainerWidth, LayoutParams.MATCH_PARENT,
							Gravity.RIGHT));
			((ImageView) getActivity().findViewById(R.id.MyLifeDrawerIV))
					.setImageResource(R.drawable.drawerreverse);
		} else {
			mMyLifeDrawerContainerRL
					.setLayoutParams(new FrameLayout.LayoutParams(
							mDrawerContainerOrigWidth,
							LayoutParams.MATCH_PARENT, Gravity.RIGHT));
			((ImageView) getActivity().findViewById(R.id.MyLifeDrawerIV))
					.setImageResource(R.drawable.drawer);
		}
	}
}
