package ai.nexconn.chatui.widget.pullrefresh.simple;

import ai.nexconn.chatui.widget.pullrefresh.api.RefreshFooter;
import ai.nexconn.chatui.widget.pullrefresh.api.RefreshHeader;
import ai.nexconn.chatui.widget.pullrefresh.api.RefreshLayout;
import ai.nexconn.chatui.widget.pullrefresh.constant.RefreshState;
import ai.nexconn.chatui.widget.pullrefresh.listener.OnMultiListener;
import androidx.annotation.NonNull;

/** Multi-purpose listener. */
public class SimpleMultiListener implements OnMultiListener {

    @Override
    public void onHeaderMoving(
            RefreshHeader header,
            boolean isDragging,
            float percent,
            int offset,
            int headerHeight,
            int maxDragHeight) {
        // default implementation ignored
    }

    @Override
    public void onHeaderReleased(RefreshHeader header, int headerHeight, int maxDragHeight) {
        // default implementation ignored
    }

    @Override
    public void onHeaderStartAnimator(RefreshHeader header, int footerHeight, int maxDragHeight) {
        // default implementation ignored
    }

    @Override
    public void onHeaderFinish(RefreshHeader header, boolean success) {
        // default implementation ignored
    }

    @Override
    public void onFooterMoving(
            RefreshFooter footer,
            boolean isDragging,
            float percent,
            int offset,
            int footerHeight,
            int maxDragHeight) {
        // default implementation ignored
    }

    @Override
    public void onFooterReleased(RefreshFooter footer, int footerHeight, int maxDragHeight) {
        // do nothing
    }

    @Override
    public void onFooterStartAnimator(RefreshFooter footer, int headerHeight, int maxDragHeight) {
        // do nothing
    }

    @Override
    public void onFooterFinish(RefreshFooter footer, boolean success) {
        // do nothing
    }

    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {
        // do nothing
    }

    @Override
    public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
        // do nothing
    }

    @Override
    public void onStateChanged(
            @NonNull RefreshLayout refreshLayout,
            @NonNull RefreshState oldState,
            @NonNull RefreshState newState) {
        // do nothing
    }
}
