package ai.nexconn.chatui.widget.pullrefresh.listener;

import ai.nexconn.chatui.widget.pullrefresh.api.RefreshHeader;
import ai.nexconn.chatui.widget.pullrefresh.api.RefreshLayout;
import android.content.Context;
import androidx.annotation.NonNull;

/** Default header creator. */
public interface DefaultRefreshHeaderCreator {
    @NonNull
    RefreshHeader createRefreshHeader(@NonNull Context context, @NonNull RefreshLayout layout);
}
