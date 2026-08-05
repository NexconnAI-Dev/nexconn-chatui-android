package ai.nexconn.chatui.widget.pullrefresh.wrapper;

import ai.nexconn.chatui.widget.pullrefresh.api.RefreshFooter;
import ai.nexconn.chatui.widget.pullrefresh.simple.SimpleComponent;
import android.annotation.SuppressLint;
import android.view.View;

/** Refresh footer wrapper. */
@SuppressLint("ViewConstructor")
public class RefreshFooterWrapper extends SimpleComponent implements RefreshFooter {

    public RefreshFooterWrapper(View wrapper) {
        super(wrapper);
    }
}
