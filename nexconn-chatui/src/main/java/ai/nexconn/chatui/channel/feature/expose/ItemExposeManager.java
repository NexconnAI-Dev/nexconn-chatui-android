package ai.nexconn.chatui.channel.feature.expose;

import ai.nexconn.chatui.widget.adapter.BaseAdapter;
import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class ItemExposeManager<T> {

    private OnItemExposeListener<T> mItemOnExposeListener;

    private RecyclerView mRecyclerView;
    private BaseAdapter<T> mAdapter;

    /**
     * Exposure threshold ratio, range 0.0 ~ 1.0. 0.1 means 10% visibility triggers exposure
     * callback.
     */
    private float mExposeThresholdRatio = 0.1f;

    public ItemExposeManager() {}

    /**
     * Sets a listener for RecyclerView item visibility state.
     *
     * @param recyclerView recyclerView
     * @param onExposeListener callback for item visibility in the list
     */
    public void attach(
            RecyclerView recyclerView,
            BaseAdapter<T> adapter,
            OnItemExposeListener<T> onExposeListener) {
        if (recyclerView == null
                || recyclerView.getVisibility() != View.VISIBLE
                || adapter == null
                || onExposeListener == null) {
            return;
        }
        mItemOnExposeListener = onExposeListener;
        mRecyclerView = recyclerView;
        mAdapter = adapter;

        // Detect RecyclerView scroll events
        mRecyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(
                            @NonNull RecyclerView recyclerView, int newState) {
                        // SCROLL_STATE_IDLE: stopped scrolling; SCROLL_STATE_DRAGGING: user
                        // dragging
                        // SCROLL_STATE_SETTLING: fling scrolling
                        if (newState == RecyclerView.SCROLL_STATE_IDLE
                                || newState == RecyclerView.SCROLL_STATE_DRAGGING
                                || newState == RecyclerView.SCROLL_STATE_SETTLING) {
                            handleCurrentVisibleItems();
                        }
                    }

                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        // Includes counting currently visible views when first entering the list
                        handleCurrentVisibleItems();
                    }
                });
    }

    public void release() {
        mItemOnExposeListener = null;
        mRecyclerView = null;
        mAdapter = null;
    }

    /** Processes currently visible item views in mRecyclerView on screen. */
    public void handleCurrentVisibleItems() {
        // View.getGlobalVisibleRect(new Rect()) returns true if view is visually visible,
        // regardless of how much.
        if (mRecyclerView == null
                || mRecyclerView.getVisibility() != View.VISIBLE
                || !mRecyclerView.isShown()
                || !mRecyclerView.getGlobalVisibleRect(new Rect())) {
            return;
        }
        // Try-catch as a safeguard to prevent tracking from affecting normal business logic
        try {
            int[] range = new int[2];
            int orientation = -1;
            RecyclerView.LayoutManager manager = mRecyclerView.getLayoutManager();
            if (manager instanceof GridLayoutManager) {
                GridLayoutManager gridLayoutManager = (GridLayoutManager) manager;
                range = findRangeGrid(gridLayoutManager);
                orientation = gridLayoutManager.getOrientation();
            } else if (manager instanceof LinearLayoutManager) {
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) manager;
                range = findRangeLinear(linearLayoutManager);
                orientation = linearLayoutManager.getOrientation();
            } else if (manager instanceof StaggeredGridLayoutManager) {
                StaggeredGridLayoutManager staggeredGridLayoutManager =
                        (StaggeredGridLayoutManager) manager;
                range = findRangeStaggeredGrid(staggeredGridLayoutManager);
                orientation = staggeredGridLayoutManager.getOrientation();
            }
            if (range.length < 2) {
                return;
            }
            // Note: this processes all visible views during the current scroll
            for (int i = range[0]; i <= range[1]; i++) {
                if (manager != null) {
                    View view = manager.findViewByPosition(i);
                    setCallbackForLogicVisibleView(view, i, orientation);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets visibility callback for logically visible views. Logically visible means visible and the
     * visible height (width) exceeds the threshold ratio of the view's height (width).
     *
     * @param view the visible item's view
     * @param position the visible item's position
     * @param orientation RecyclerView's orientation
     */
    private void setCallbackForLogicVisibleView(View view, int position, int orientation) {
        if (view == null
                || view.getVisibility() != View.VISIBLE
                || !view.isShown()
                || !view.getGlobalVisibleRect(new Rect())) {
            return;
        }
        if (mAdapter == null || mItemOnExposeListener == null) {
            return;
        }
        Rect rect = new Rect();

        boolean cover = view.getGlobalVisibleRect(rect);

        // Exposure threshold ratio, range 0.0 ~ 1.0
        float exposeThresholdRatio =
                (mExposeThresholdRatio < 0.0f || mExposeThresholdRatio > 1.0f)
                        ? 0.2f
                        : mExposeThresholdRatio;
        // Item logically visible: visible and visible height (width) exceeds the threshold ratio of
        // view height (width)
        boolean visibleHeightEnough =
                orientation == OrientationHelper.VERTICAL
                        && rect.height() > view.getMeasuredHeight() * exposeThresholdRatio;
        boolean visibleWidthEnough =
                orientation == OrientationHelper.HORIZONTAL
                        && rect.width() > view.getMeasuredWidth() * exposeThresholdRatio;
        boolean isItemViewVisibleInLogic = visibleHeightEnough || visibleWidthEnough;

        T itemData = mAdapter.getItem(position);
        boolean visible = cover && isItemViewVisibleInLogic;
        mItemOnExposeListener.onItemViewVisible(visible, position, itemData);
    }

    private int[] findRangeLinear(LinearLayoutManager manager) {
        int[] range = new int[2];
        range[0] = manager.findFirstVisibleItemPosition();
        range[1] = manager.findLastVisibleItemPosition();
        return range;
    }

    private int[] findRangeGrid(GridLayoutManager manager) {
        int[] range = new int[2];
        range[0] = manager.findFirstVisibleItemPosition();
        range[1] = manager.findLastVisibleItemPosition();
        return range;
    }

    private int[] findRangeStaggeredGrid(StaggeredGridLayoutManager manager) {
        int[] startPos = new int[manager.getSpanCount()];
        int[] endPos = new int[manager.getSpanCount()];
        manager.findFirstVisibleItemPositions(startPos);
        manager.findLastVisibleItemPositions(endPos);
        return findRange(startPos, endPos);
    }

    private int[] findRange(int[] startPos, int[] endPos) {
        int start = startPos[0];
        int end = endPos[0];
        for (int i = 1; i < startPos.length; i++) {
            if (start > startPos[i]) {
                start = startPos[i];
            }
        }
        for (int i = 1; i < endPos.length; i++) {
            if (end < endPos[i]) {
                end = endPos[i];
            }
        }
        return new int[] {start, end};
    }
}
