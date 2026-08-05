package ai.nexconn.chatui.widget.pullrefresh.listener;

import ai.nexconn.chatui.widget.pullrefresh.api.RefreshLayout;
import android.content.Context;
import androidx.annotation.NonNull;

/** Default global initializer. */
public interface DefaultRefreshInitializer {
    void initialize(@NonNull Context context, @NonNull RefreshLayout layout);
}
