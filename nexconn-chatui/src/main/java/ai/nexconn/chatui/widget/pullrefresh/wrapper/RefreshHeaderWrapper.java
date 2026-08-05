package ai.nexconn.chatui.widget.pullrefresh.wrapper;

import ai.nexconn.chatui.widget.pullrefresh.api.RefreshHeader;
import ai.nexconn.chatui.widget.pullrefresh.simple.SimpleComponent;
import android.annotation.SuppressLint;
import android.view.View;

/** Refresh header wrapper. */
@SuppressLint("ViewConstructor")
public class RefreshHeaderWrapper extends SimpleComponent implements RefreshHeader {

    public RefreshHeaderWrapper(View wrapper) {
        super(wrapper);
    }
}
