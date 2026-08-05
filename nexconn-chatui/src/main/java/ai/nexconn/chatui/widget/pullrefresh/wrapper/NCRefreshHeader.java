package ai.nexconn.chatui.widget.pullrefresh.wrapper;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.widget.pullrefresh.api.RefreshFooter;
import ai.nexconn.chatui.widget.pullrefresh.api.RefreshHeader;
import ai.nexconn.chatui.widget.pullrefresh.api.RefreshLayout;
import ai.nexconn.chatui.widget.pullrefresh.simple.SimpleComponent;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;

public class NCRefreshHeader extends SimpleComponent implements RefreshHeader, RefreshFooter {

    protected ImageView mProgressView;

    public NCRefreshHeader(Context context) {
        this(context, null);
    }

    public NCRefreshHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NCRefreshHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(context, R.layout.nc_refresh_header, this);
        mProgressView = findViewById(R.id.nc_refresh_progress);
        final Drawable drawable = mProgressView.getDrawable();
        if (drawable instanceof Animatable) {
            if (((Animatable) drawable).isRunning()) {
                ((Animatable) drawable).stop();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        final Drawable drawable = mProgressView.getDrawable();
        if (drawable instanceof Animatable) {
            if (((Animatable) drawable).isRunning()) {
                ((Animatable) drawable).stop();
            }
        }
    }

    @Override
    public int onFinish(@NonNull RefreshLayout refreshLayout, boolean success) {
        Drawable drawable = mProgressView.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).stop();
        }
        return super.onFinish(refreshLayout, success);
    }

    @Override
    public void onStartAnimator(
            @NonNull RefreshLayout refreshLayout, int height, int maxDragHeight) {
        Drawable drawable = mProgressView.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).start();
        }
    }
}
