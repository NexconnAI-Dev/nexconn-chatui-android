package ai.nexconn.chatui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * My GitHub : https://github.com/af913337456/
 *
 * <p>My Blog : http://www.cnblogs.com/linguanh/
 *
 * <p>second time edited by LinGuanHong on 2017/4/26.
 */
public class CircleProgressView extends View {

    private Paint paintBgCircle;

    private Paint paintCircle;

    private Paint paintProgressCircle;

    private Paint paintIndeterminateCircle;

    private static final float startAngle = -90f; // start angle

    private float sweepAngle = 0; // sweep angle

    private float rotationAngle = 0; // rotation offset for indeterminate mode

    private static final float indeterminateSweepAngle = 330f;

    private static final int progressCirclePadding =
            3; // gap between progress circle and background circle

    private boolean fillIn = false; // whether the progress circle is filled

    private boolean indeterminate = false; // indeterminate spinner mode

    private static final int animDuration = 1000;

    private CircleProgressViewAnim mCircleProgressViewAnim; // animation effect

    public CircleProgressView(Context context) {
        super(context);
        init();
    }

    public CircleProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private void init() {

        mCircleProgressViewAnim = new CircleProgressViewAnim();
        mCircleProgressViewAnim.setDuration(animDuration);
        // progressCirclePadding = dip2px(getContext(), 3);

        paintBgCircle = new Paint();
        paintBgCircle.setAntiAlias(true);
        paintBgCircle.setStyle(Paint.Style.STROKE);
        paintBgCircle.setColor(0xCCFFFFFF);

        paintCircle = new Paint();
        paintCircle.setAntiAlias(true);
        paintCircle.setStyle(Paint.Style.FILL);
        paintCircle.setColor(Color.GRAY);

        paintProgressCircle = new Paint();
        paintProgressCircle.setAntiAlias(true);
        paintProgressCircle.setStyle(Paint.Style.FILL);
        paintProgressCircle.setColor(0xCCFFFFFF);

        paintIndeterminateCircle = new Paint();
        paintIndeterminateCircle.setAntiAlias(true);
        paintIndeterminateCircle.setStyle(Paint.Style.STROKE);
        paintIndeterminateCircle.setStrokeWidth(dip2px(getContext(), 1));
        paintIndeterminateCircle.setStrokeCap(Paint.Cap.BUTT);
        paintIndeterminateCircle.setColor(0xCCFFFFFF);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (indeterminate) {
            drawIndeterminateProgress(canvas);
            return;
        }
        canvas.drawCircle(
                getMeasuredWidth() / 2,
                getMeasuredWidth() / 2,
                getMeasuredWidth() / 2,
                paintBgCircle);
        RectF f =
                new RectF(
                        progressCirclePadding,
                        progressCirclePadding,
                        getMeasuredWidth() - progressCirclePadding,
                        getMeasuredWidth() - progressCirclePadding);
        canvas.drawArc(f, startAngle, sweepAngle, true, paintProgressCircle);
        if (!fillIn)
            canvas.drawCircle(
                    getMeasuredWidth() / 2,
                    getMeasuredWidth() / 2,
                    getMeasuredWidth() / 2 - progressCirclePadding * 2,
                    paintCircle);
    }

    private void drawIndeterminateProgress(Canvas canvas) {
        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        float strokeInset = paintIndeterminateCircle.getStrokeWidth() / 2f;
        float centerX = getMeasuredWidth() / 2f;
        float centerY = getMeasuredHeight() / 2f;
        float radius = size / 2f;
        RectF rectF =
                new RectF(
                        centerX - radius + strokeInset,
                        centerY - radius + strokeInset,
                        centerX + radius - strokeInset,
                        centerY + radius - strokeInset);
        canvas.drawArc(
                rectF,
                startAngle + rotationAngle,
                indeterminateSweepAngle,
                false,
                paintIndeterminateCircle);
    }

    public void startAnimAutomatic(boolean fillIn) {
        this.fillIn = fillIn;
        this.indeterminate = true;
        this.sweepAngle = 0;
        this.rotationAngle = 0;
        if (mCircleProgressViewAnim != null) clearAnimation();
        mCircleProgressViewAnim.setRepeatCount(Animation.INFINITE);
        mCircleProgressViewAnim.setRepeatMode(Animation.RESTART);
        startAnimation(mCircleProgressViewAnim);
    }

    public void stopAnimAutomatic() {
        if (mCircleProgressViewAnim != null) clearAnimation();
        this.indeterminate = false;
        this.rotationAngle = 0;
        invalidate();
    }

    public void setProgress(int progress, boolean fillIn) {
        this.fillIn = fillIn;
        this.indeterminate = false;
        sweepAngle = (float) (360 / 100.0 * progress);
        invalidate();
    }

    private class CircleProgressViewAnim extends Animation {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            rotationAngle = 360 * interpolatedTime;
            invalidate();
        }
    }
}
