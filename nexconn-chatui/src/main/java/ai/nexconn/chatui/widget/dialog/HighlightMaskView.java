package ai.nexconn.chatui.widget.dialog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

/**
 * Mask View for displaying a highlight area (AnchorView) with a shadow mask effect. The highlight
 * area has no mask while other areas show a semi-transparent overlay.
 */
class HighlightMaskView extends View {

    private Paint mMaskPaint; // mask paint
    private Paint mClearPaint; // clear paint for cutting out the highlight area
    private Rect mHighlightRect; // rectangle of the highlight area
    private int mMaskColor = 0x99000000; // mask color, default semi-transparent black
    private float mCornerRadius = 0; // corner radius of the highlight area

    public HighlightMaskView(Context context) {
        super(context);
        init();
    }

    private void init() {
        // Disable hardware acceleration to use PorterDuffXfermode
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        // Initialize mask paint
        mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMaskPaint.setStyle(Paint.Style.FILL);
        mMaskPaint.setColor(mMaskColor);

        // Initialize clear paint for cutting out the highlight area
        mClearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mClearPaint.setStyle(Paint.Style.FILL);
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    /**
     * Set the highlight area
     *
     * @param rect the rectangle of the highlight area
     */
    public void setHighlightRect(Rect rect) {
        this.mHighlightRect = rect;
        invalidate(); // redraw
    }

    /**
     * Set the corner radius of the highlight area
     *
     * @param cornerRadius corner radius in px
     */
    public void setCornerRadius(float cornerRadius) {
        this.mCornerRadius = cornerRadius;
        invalidate();
    }

    /**
     * Set the mask color
     *
     * @param color mask color
     */
    public void setMaskColor(int color) {
        this.mMaskColor = color;
        mMaskPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw full-screen mask
        canvas.drawRect(0, 0, getWidth(), getHeight(), mMaskPaint);

        // If there is a highlight area, cut it out
        if (mHighlightRect != null) {
            if (mCornerRadius > 0) {
                // Draw rounded rectangle highlight area
                RectF rectF = new RectF(mHighlightRect);
                canvas.drawRoundRect(rectF, mCornerRadius, mCornerRadius, mClearPaint);
            } else {
                // Draw rectangular highlight area
                canvas.drawRect(mHighlightRect, mClearPaint);
            }
        }
    }
}
