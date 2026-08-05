package ai.nexconn.chatui.widget.pullrefresh.listener;

import ai.nexconn.chatui.widget.pullrefresh.api.RefreshLayout;
import androidx.annotation.NonNull;

/** Refresh listener. */
public interface OnRefreshListener {
    void onRefresh(@NonNull RefreshLayout refreshLayout);
}
