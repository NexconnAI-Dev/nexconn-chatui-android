package ai.nexconn.chatui.widget;

import ai.nexconn.chatui.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

/** Alphabetical index View on the right side */
public class SideBar extends View {

    // Touch event listener
    private OnTouchingLetterChangedListener onTouchingLetterChangedListener;

    // 26 letters
    public static final String[] DEFAULT_B = {
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
        "S", "T", "U", "V", "W", "X", "Y", "Z", "#"
    };
    private String[] b = DEFAULT_B;
    // Selected index
    private int choose = -1;

    private Paint paint = new Paint();

    private TextView mTextDialog;

    // Converted height in px
    private int singleHeightPx;
    // Height per character in dp
    private static final int SINGLE_HEIGHT_DP = 25;

    /**
     * Set the TextView used to display the selected letter
     *
     * @param mTextDialog the TextView to display the letter
     */
    public void setTextView(TextView mTextDialog) {
        this.mTextDialog = mTextDialog;
    }

    public SideBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public SideBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SideBar(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        // Convert dp to pixels
        singleHeightPx =
                (int)
                        TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                SINGLE_HEIGHT_DP,
                                context.getResources().getDisplayMetrics());
    }

    /** Override onMeasure to dynamically set the SideBar height based on letter count */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Dynamically calculate height: letter array length * height per character
        int desiredHeight = b.length * singleHeightPx;

        // Set measured width and height
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, desiredHeight);
    }

    /** Override onDraw to draw the letters */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        for (int i = 0; i < b.length; i++) {
            paint.setColor(Color.GRAY); // default color for all letters
            paint.setTypeface(Typeface.DEFAULT);
            paint.setAntiAlias(true);
            paint.setTextSize(30);
            // Selected state
            if (i == choose) {
                paint.setColor(Color.parseColor("#FFFFFF")); // selected letter color: white
                paint.setFakeBoldText(true);
            }
            // x = center - half of the string width
            float xPos = width / 2f - paint.measureText(b[i]) / 2;
            float yPos = singleHeightPx * i * 1.0f + singleHeightPx;
            canvas.drawText(b[i], xPos, yPos, paint);
            paint.reset(); // reset paint
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        final int action = event.getAction();
        final float y = event.getY(); // touch y coordinate
        final int oldChoose = choose;

        final OnTouchingLetterChangedListener listener = onTouchingLetterChangedListener;

        final int c = (int) (y / getHeight() * b.length); // map y proportion to letter array index

        if (action == MotionEvent.ACTION_UP) {
            setBackgroundDrawable(new ColorDrawable(0x00000000)); // set background color
            choose = -1;
            invalidate();
            if (mTextDialog != null) {
                mTextDialog.setVisibility(View.INVISIBLE);
            }
        } else {
            setBackgroundResource(R.drawable.nc_bg_sidebar); // sidebar pressed background
            if (oldChoose != c) {
                if (c >= 0 && c < b.length) {
                    if (listener != null) {
                        listener.onTouchingLetterChanged(b[c]);
                    }
                    if (mTextDialog != null) {
                        mTextDialog.setText(b[c]);
                        mTextDialog.setVisibility(View.VISIBLE);
                    }
                    choose = c;
                    invalidate();
                }
            }
        }

        return true;
    }

    /**
     * Set the listener for letter touch events
     *
     * @param onTouchingLetterChangedListener the listener
     */
    public void setOnTouchingLetterChangedListener(
            OnTouchingLetterChangedListener onTouchingLetterChangedListener) {
        this.onTouchingLetterChangedListener = onTouchingLetterChangedListener;
    }

    public interface OnTouchingLetterChangedListener {
        void onTouchingLetterChanged(String s);
    }

    /**
     * Set the letter array from external source
     *
     * @param letters the letter array
     */
    public void setLetters(String[] letters) {
        if (letters != null && letters.length > 0) {
            this.b = letters;
            requestLayout();
            invalidate(); // refresh the View to display the new letter array
        }
    }
}
