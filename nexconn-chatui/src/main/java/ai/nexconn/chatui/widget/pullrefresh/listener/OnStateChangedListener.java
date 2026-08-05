package ai.nexconn.chatui.widget.pullrefresh.listener;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.annotation.RestrictTo.Scope.SUBCLASSES;

import ai.nexconn.chatui.widget.pullrefresh.api.RefreshLayout;
import ai.nexconn.chatui.widget.pullrefresh.constant.RefreshState;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/** Refresh state change listener. */
public interface OnStateChangedListener {
    /**
     * [Framework internal only] State change event {@link RefreshState}.
     *
     * @param refreshLayout RefreshLayout
     * @param oldState State before change
     * @param newState State after change
     */
    @RestrictTo({LIBRARY, LIBRARY_GROUP, SUBCLASSES})
    void onStateChanged(
            @NonNull RefreshLayout refreshLayout,
            @NonNull RefreshState oldState,
            @NonNull RefreshState newState);
}
