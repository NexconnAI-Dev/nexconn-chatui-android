package ai.nexconn.chatui.widget;

import ai.nexconn.chatui.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class FileRectangleProgress extends View {

    public static final int PI_RADIUS = 180; // degrees corresponding to pi radians

    private int mProgress; // progress value, range: 0-100
    private int mCorner; // corner radius; using half the side length creates a circle
    private int mStartAngle; // starting angle for progress, 0-n, where 0 aligns with the x-axis
    private int mBackgroundColor; // color of the overlay area (the non-progress portion)

    private int width;
    private int height;
    private PointF mCenter; // center of the View
    private PointF mStart; // starting point on the circle at the start angle
    private float mRadius; // radius of the View's circumscribed circle
    private RectF mBackground; // the clipped base rounded rectangle
    private Path mClipArcPath = new Path(); // the sector portion to clip (B)
    private Path mClipBgPath = new Path(); // the entire View background (A); drawn area is A-B
    private RectF
            mEnclosingRectF; // bounding rect of the circumscribed circle, larger than View when
    // padding is ignored
    private Paint mPaint = new Paint();

    public FileRectangleProgress(Context context) {
        super(context);
    }

    public FileRectangleProgress(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FileRectangleProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void setProgress(int progress) {
        mProgress = progress;
        invalidate();
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleProgress);
        mProgress = typedArray.getInt(R.styleable.CircleProgress_circleProgress, 0);
        mCorner = typedArray.getDimensionPixelOffset(R.styleable.CircleProgress_circleCorner, 0);
        mStartAngle = typedArray.getInt(R.styleable.CircleProgress_startAngle, 315);
        mBackgroundColor =
                typedArray.getColor(
                        R.styleable.CircleProgress_backgroundColor, Color.argb(90, 90, 90, 90));
        typedArray.recycle();

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
        mPaint.setColor(mBackgroundColor);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        float rw = (width - getPaddingStart() - getPaddingEnd()) / 2f;
        float rh = (height - getPaddingTop() - getPaddingBottom()) / 2f;
        mRadius = (float) Math.sqrt(rw * rw + rh * rh);
        mCenter = new PointF(getPaddingStart() + rw, getPaddingTop() + rh);
        mStart =
                new PointF(
                        (float) (mCenter.x + mRadius * Math.cos(mStartAngle * Math.PI / PI_RADIUS)),
                        (float)
                                (mCenter.y
                                        + mRadius * Math.sin(mStartAngle * Math.PI / PI_RADIUS)));
        mBackground =
                new RectF(
                        getPaddingStart(),
                        getPaddingTop(),
                        width - getPaddingEnd(),
                        height - getPaddingBottom());
        mEnclosingRectF =
                new RectF(
                        mCenter.x - mRadius,
                        mCenter.y - mRadius,
                        mCenter.x + mRadius,
                        mCenter.y + mRadius);
        mClipBgPath.reset();
        mClipBgPath.addRoundRect(mBackground, mCorner, mCorner, Path.Direction.CW);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.clipPath(mClipBgPath);
        canvas.clipPath(getSectorClip(360 * mProgress / 100f + mStartAngle), Region.Op.DIFFERENCE);
        canvas.drawRoundRect(mBackground, mCorner, mCorner, mPaint);
        canvas.restore();
    }

    private Path getSectorClip(float sweepAngle) {
        mClipArcPath.reset();
        mClipArcPath.moveTo(mCenter.x, mCenter.y);
        mClipArcPath.lineTo(mStart.x, mStart.y);
        mClipArcPath.lineTo(
                (float) (mCenter.x + mRadius * Math.cos(sweepAngle * Math.PI / PI_RADIUS)),
                (float) (mCenter.y + mRadius * Math.sin(sweepAngle * Math.PI / PI_RADIUS)));
        mClipArcPath.close();
        mClipArcPath.addArc(mEnclosingRectF, mStartAngle, sweepAngle - mStartAngle);
        return mClipArcPath;
    }
}
