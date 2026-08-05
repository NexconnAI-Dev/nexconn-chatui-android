package ai.nexconn.chatui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/** Loading dots animation View that displays 3 dots moving forward sequentially */
public class LoadingDotsView extends View {

    private static final int DOT_COUNT = 3;
    private static final int DOT_RADIUS = 2; // dp
    private static final int DOT_SPACING = 8; // dp
    private static final long ANIMATION_DURATION = 600; // ms
    private static final long ANIMATION_DELAY = 200; // ms between dots

    private Paint mDotPaint;
    private float mDotRadius;
    private float mDotSpacing;
    private float[] mDotAlphas;
    private AnimatorSet mAnimatorSet;
    private boolean mIsAnimating;

    public LoadingDotsView(Context context) {
        this(context, null);
    }

    public LoadingDotsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadingDotsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        mDotRadius = DOT_RADIUS * density;
        mDotSpacing = DOT_SPACING * density;

        mDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDotPaint.setColor(0xFF666666); // dark gray
        mDotPaint.setStyle(Paint.Style.FILL);

        mDotAlphas = new float[DOT_COUNT];

        // Initialize state
        resetDots();
    }

    private void resetDots() {
        for (int i = 0; i < DOT_COUNT; i++) {
            mDotAlphas[i] = 0.3f; // default semi-transparent
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = (int) (mDotRadius * 2 * DOT_COUNT + mDotSpacing * (DOT_COUNT - 1));
        int height = (int) (mDotRadius * 2);

        setMeasuredDimension(
                resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!mIsAnimating) {
            return;
        }

        float centerY = getHeight() / 2f;
        float startX = mDotRadius;

        for (int i = 0; i < DOT_COUNT; i++) {
            float centerX = startX + i * (mDotRadius * 2 + mDotSpacing);

            // Set alpha
            int alpha = (int) (255 * mDotAlphas[i]);
            mDotPaint.setAlpha(alpha);

            canvas.drawCircle(centerX, centerY, mDotRadius, mDotPaint);
        }
    }

    /** Start the animation */
    public void startAnimation() {
        if (mIsAnimating) {
            return;
        }

        mIsAnimating = true;
        startAnimationInternal();
    }

    /** Internal animation start method, used for looping playback */
    private void startAnimationInternal() {
        if (!mIsAnimating) {
            return;
        }

        resetDots();

        mAnimatorSet = new AnimatorSet();
        AnimatorSet.Builder builder = null;

        for (int i = 0; i < DOT_COUNT; i++) {
            AnimatorSet dotAnimator = createDotAnimator(i);

            if (builder == null) {
                builder = mAnimatorSet.play(dotAnimator);
            } else {
                builder.with(dotAnimator);
            }
        }

        mAnimatorSet.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (mIsAnimating) {
                            // Loop playback: reset and recreate the animation
                            mAnimatorSet = null;
                            startAnimationInternal();
                        }
                    }
                });

        mAnimatorSet.start();
        invalidate();
    }

    /** Stop the animation */
    private void stopAnimation() {
        mIsAnimating = false;
        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
            mAnimatorSet = null;
        }
        resetDots();
        invalidate();
    }

    /** Create the animation for a single dot */
    private AnimatorSet createDotAnimator(int index) {
        // Alpha animation: 0.3 -> 1.0 -> 0.3
        ObjectAnimator alphaAnimator =
                ObjectAnimator.ofFloat(this, "dotAlpha" + index, 0.3f, 1.0f, 0.3f);
        alphaAnimator.setDuration(ANIMATION_DURATION);

        // Set delay and interpolator
        long delay = index * ANIMATION_DELAY;
        alphaAnimator.setStartDelay(delay);
        alphaAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet dotSet = new AnimatorSet();
        dotSet.play(alphaAnimator);

        return dotSet;
    }

    // Dynamic property setters (used by ObjectAnimator)
    public void setDotAlpha0(float alpha) {
        mDotAlphas[0] = alpha;
        invalidate();
    }

    public void setDotAlpha1(float alpha) {
        mDotAlphas[1] = alpha;
        invalidate();
    }

    public void setDotAlpha2(float alpha) {
        mDotAlphas[2] = alpha;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != VISIBLE) {
            stopAnimation();
        }
    }
}
