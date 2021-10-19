package com.krystal.memoir;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ViewFlipper;

public class AViewFlipper extends ViewFlipper {

	Paint paint = new Paint();
	int mWidth = 0, mHeight = 0;

	public AViewFlipper(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		int width = getWidth();

		float margin = 6;
		float radius = 7;
		float cx = width / 2 - ((radius + margin) * 2 * getChildCount() / 2);
		float cy = getHeight() - 25;

		canvas.save();
		for (int i = 0; i < getChildCount(); i++) {
			if (i == getDisplayedChild()) {
				paint.setColor(getResources().getColor(R.color.selectBlue));
				canvas.drawCircle(cx, cy, radius, paint);

			} else {
				paint.setColor(Color.GRAY);
				canvas.drawCircle(cx, cy, radius, paint);
			}
			cx += 2 * (radius + margin);
		}
		canvas.restore();
	}
}