package ai.nexconn.chatui.widget.pullrefresh.listener;

import android.view.View;

/** Scroll boundary decider. */
public interface ScrollBoundaryDecider {
    /**
     * Determine whether pull-down refresh can start based on content view state.
     *
     * @param content Content view
     * @return true will trigger pull-down refresh
     */
    boolean canRefresh(View content);

    /**
     * Determine whether pull-up load more can start based on content view state.
     *
     * @param content Content view
     * @return true will trigger load more
     */
    boolean canLoadMore(View content);
}
