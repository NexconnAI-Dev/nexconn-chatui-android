package ai.nexconn.chatui.widget.pullrefresh.simple;

import ai.nexconn.chatui.widget.pullrefresh.listener.ScrollBoundaryDecider;
import ai.nexconn.chatui.widget.pullrefresh.util.SmartUtil;
import android.graphics.PointF;
import android.view.View;

/** Scroll boundary decider. */
public class SimpleBoundaryDecider implements ScrollBoundaryDecider {

    // <editor-fold desc="Internal">
    public PointF mActionEvent;
    public ScrollBoundaryDecider boundary;
    public boolean mEnableLoadMoreWhenContentNotFull = true;

    // </editor-fold>

    // <editor-fold desc="ScrollBoundaryDecider">
    @Override
    public boolean canRefresh(View content) {
        if (boundary != null) {
            return boundary.canRefresh(content);
        }
        // When mActionEvent is null, canRefresh will not perform dynamic recursive search
        return SmartUtil.canRefresh(content, mActionEvent);
    }

    @Override
    public boolean canLoadMore(View content) {
        if (boundary != null) {
            return boundary.canLoadMore(content);
        }
        // When mActionEvent is null, canLoadMore will not perform dynamic recursive search
        return SmartUtil.canLoadMore(content, mActionEvent, mEnableLoadMoreWhenContentNotFull);
    }
    // </editor-fold>
}
