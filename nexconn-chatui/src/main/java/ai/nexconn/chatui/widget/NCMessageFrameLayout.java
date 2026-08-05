package ai.nexconn.chatui.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class NCMessageFrameLayout extends FrameLayout {
    private Drawable mOldDrawable;

    public NCMessageFrameLayout(Context context) {
        super(context);
    }

    public NCMessageFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NCMessageFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setBackgroundResource(int resid) {
        super.setBackgroundResource(resid);
        mOldDrawable = getBackground();
        setBackgroundDrawable(null);
        setPadding(0, 0, 0, 0);
    }

    public Drawable getBackgroundDrawable() {
        return mOldDrawable;
    }
}
