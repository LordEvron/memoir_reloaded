package com.krystal.memoir;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ViewFlipper;

public class WelcomeScreen extends Activity {

	private ViewFlipper page;
	private Animation animFlipInForeward;
	private Animation animFlipOutForeward;
	private Animation animFlipInBackward;
	private Animation animFlipOutBackward;
	private GestureDetector gestureDetector = null;
	private ImageView mSwipeLeft = null, mSwipeRight = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome_screen);

		page = (ViewFlipper) findViewById(R.id.welcomeVF);

		animFlipInForeward = AnimationUtils.loadAnimation(this, R.anim.flipin);
		animFlipOutForeward = AnimationUtils
				.loadAnimation(this, R.anim.flipout);
		animFlipInBackward = AnimationUtils.loadAnimation(this,
				R.anim.flipin_reverse);
		animFlipOutBackward = AnimationUtils.loadAnimation(this,
				R.anim.flipout_reverse);
		SimpleOnGestureListener simpleOnGestureListener = new SimpleOnGestureListener() {

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {

				float sensitvity = 50;
				if ((e1.getX() - e2.getX()) > sensitvity) {
					SwipeLeft();
				} else if ((e2.getX() - e1.getX()) > sensitvity) {
					SwipeRight();
				}
				return true;
			}
		};

		gestureDetector = new GestureDetector(this, simpleOnGestureListener);

		((ImageView) findViewById(R.id.WelcomeScreenB))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						onExit();
					}
				});

		mSwipeLeft = (ImageView) findViewById(R.id.WelcomeScreenSwipeLeft);
		mSwipeRight = (ImageView) findViewById(R.id.WelcomeScreenSwipeRight);

		mSwipeLeft.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				SwipeLeft();
				return;
			}
		});

		mSwipeRight.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				SwipeRight();
				return;
			}
		});
		updateArrows(0);
	}

	@Override
	public void onBackPressed() {
		/*
		 * //Currently disabling back press page.removeAllViews(); Intent i =
		 * new Intent(WelcomeScreen.this, MainActivity.class);
		 * i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); startActivity(i);
		 * finish();
		 */
	}

	public void onExit() {
		page.removeAllViews();
		Intent i = new Intent(WelcomeScreen.this, MainActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
		finish();
	}

	private void updateArrows(int displayedChild) {
		if (displayedChild == 0) {
			mSwipeLeft.setVisibility(View.VISIBLE);
			mSwipeRight.setVisibility(View.INVISIBLE);
		} else if (displayedChild == 6) {
			mSwipeLeft.setVisibility(View.INVISIBLE);
			mSwipeRight.setVisibility(View.VISIBLE);
		} else {
			mSwipeLeft.setVisibility(View.VISIBLE);
			mSwipeRight.setVisibility(View.VISIBLE);
		}
	}

	private void SwipeRight() {
		if (page.getDisplayedChild() != 0) {
			mSwipeLeft.setVisibility(View.INVISIBLE);
			mSwipeRight.setVisibility(View.INVISIBLE);

			page.setInAnimation(animFlipInBackward);
			page.setOutAnimation(animFlipOutBackward);
			page.showPrevious();
			updateArrows(page.getDisplayedChild());
		}
	}

	private void SwipeLeft() {
		if (page.getDisplayedChild() != 6) {
			mSwipeLeft.setVisibility(View.INVISIBLE);
			mSwipeRight.setVisibility(View.INVISIBLE);

			page.setInAnimation(animFlipInForeward);
			page.setOutAnimation(animFlipOutForeward);
			page.showNext();
			updateArrows(page.getDisplayedChild());
		} else {
			onExit();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return gestureDetector.onTouchEvent(event);
	}
}
