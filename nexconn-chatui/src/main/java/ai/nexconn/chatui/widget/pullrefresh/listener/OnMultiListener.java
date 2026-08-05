package ai.nexconn.chatui.widget.pullrefresh.listener;

import ai.nexconn.chatui.widget.pullrefresh.api.RefreshFooter;
import ai.nexconn.chatui.widget.pullrefresh.api.RefreshHeader;

/** Multi-purpose listener. */
public interface OnMultiListener extends OnRefreshLoadMoreListener, OnStateChangedListener {
    /**
     * Called during finger pull-down drag (called continuously; isDragging replaces the old
     * onPulling/onReleasing).
     *
     * @param header Header
     * @param isDragging true if finger is dragging, false if rebound animation
     * @param percent Pull percentage, value = offset/footerHeight (range: 0 to
     *     (footerHeight+maxDragHeight)/footerHeight)
     * @param offset Pixel offset of pull, range: 0 to (footerHeight+maxDragHeight)
     * @param headerHeight Height: HeaderHeight or FooterHeight
     * @param maxDragHeight Maximum drag height
     */
    void onHeaderMoving(
            RefreshHeader header,
            boolean isDragging,
            float percent,
            int offset,
            int headerHeight,
            int maxDragHeight);

    void onHeaderReleased(RefreshHeader header, int headerHeight, int maxDragHeight);

    void onHeaderStartAnimator(RefreshHeader header, int headerHeight, int maxDragHeight);

    void onHeaderFinish(RefreshHeader header, boolean success);

    /**
     * Called during finger pull-up drag (called continuously; isDragging replaces the old
     * onPulling/onReleasing).
     *
     * @param footer Footer
     * @param isDragging true if finger is dragging, false if rebound animation
     * @param percent Pull percentage, value = offset/footerHeight (range: 0 to
     *     (footerHeight+maxDragHeight)/footerHeight)
     * @param offset Pixel offset of pull, range: 0 to (footerHeight+maxDragHeight)
     * @param footerHeight Height: HeaderHeight or FooterHeight
     * @param maxDragHeight Maximum drag height
     */
    void onFooterMoving(
            RefreshFooter footer,
            boolean isDragging,
            float percent,
            int offset,
            int footerHeight,
            int maxDragHeight);

    void onFooterReleased(RefreshFooter footer, int footerHeight, int maxDragHeight);

    void onFooterStartAnimator(RefreshFooter footer, int footerHeight, int maxDragHeight);

    void onFooterFinish(RefreshFooter footer, boolean success);
}
