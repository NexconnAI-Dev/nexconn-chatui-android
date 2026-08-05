package ai.nexconn.chatui.widget.pullrefresh.listener;

import ai.nexconn.chatui.widget.pullrefresh.api.RefreshFooter;
import ai.nexconn.chatui.widget.pullrefresh.api.RefreshLayout;
import android.content.Context;
import androidx.annotation.NonNull;

/** Default footer creator. */
public interface DefaultRefreshFooterCreator {
    @NonNull
    RefreshFooter createRefreshFooter(@NonNull Context context, @NonNull RefreshLayout layout);
}
