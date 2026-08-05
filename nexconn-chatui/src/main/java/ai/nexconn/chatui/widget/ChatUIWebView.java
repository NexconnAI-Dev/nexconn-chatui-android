package ai.nexconn.chatui.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebView;

public class ChatUIWebView extends WebView {

    public ChatUIWebView(Context context) {
        super(getFixedContext(context));
    }

    public ChatUIWebView(Context context, AttributeSet attrs) {
        super(getFixedContext(context), attrs);
    }

    public ChatUIWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(getFixedContext(context), attrs, defStyleAttr);
    }

    /**
     * Fix for Android 5.0 & 5.1 WebView crash on inflation:
     *
     * <p>https://stackoverflow.com/questions/41025200/android-view-inflateexception-error-inflating-class-android-webkit-webview
     */
    private static Context getFixedContext(Context context) {
        if (context == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return context.createConfigurationContext(new Configuration());
        }
        return context;
    }
}
