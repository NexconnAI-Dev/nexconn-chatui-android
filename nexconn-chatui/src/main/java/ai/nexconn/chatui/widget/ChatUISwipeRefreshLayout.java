package ai.nexconn.chatui.widget;

import ai.nexconn.chatui.R;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.ListView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class ChatUISwipeRefreshLayout extends SwipeRefreshLayout {

    private int mScaledTouchSlop;
    private View mFooterView;
    private ListView mListView;
    private OnLoadListener mOnLoadListener;
    private OnFlushListener mFlushListener;

    public boolean isRefreshFinish;

    public boolean isLoadMoreFinish;
    private boolean condition4 = false, condition5 = false;
    private boolean loadMoreEnabled = true;

    private boolean refreshEnabled = true;
    private boolean autoLoading;

    public ChatUISwipeRefreshLayout(Context context) {
        this(context, null);
    }

    public ChatUISwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    /**
     * Initialize views
     *
     * @param context context
     * @param attrs AttributeSet
     */
    private void initView(Context context, AttributeSet attrs) {
        // Inflate the footer loading layout
        mFooterView = View.inflate(context, R.layout.view_footer, null);
        // Minimum distance for a touch move to be recognized as a drag
        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // Get ListView and set its layout position
        if (mListView == null) {
            // Check how many children the container has
            if (getChildCount() > 0) {
                // Check if the first child is a ListView
                if (getChildAt(0) instanceof ListView) {
                    // Create the ListView object
                    mListView = (ListView) getChildAt(0);
                    // Set the scroll listener for the ListView
                    setListViewOnScroll();
                }
            }
            setOnRefresh();
        }
    }

    private float mDownY, mUpY;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Starting point of the touch
                mDownY = ev.getY();
                break;
                // Check if load-more can be triggered during move
            case MotionEvent.ACTION_MOVE:
                // End point of the touch
                mUpY = ev.getY();
                if (canLoadMore() && !autoLoading) {
                    // Manually scroll out FooterView and load data
                    loadData();
                }
                break;
            case MotionEvent.ACTION_UP:
            default:
                break;
        }
        // If pull-to-refresh is disabled, skip the rest
        if (refreshEnabled) {
            // Disable the control during loading to prevent refresh while loading
            if (isLoadMoreFinish) {
                setEnabled(false);
            } else {
                setEnabled(true);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * Check whether load-more conditions are met
     *
     * @return whether load-more conditions are met
     */
    private boolean canLoadMore() {
        // 1. Pull-up gesture detected
        boolean condition1 = (mDownY - mUpY) >= mScaledTouchSlop;

        // 2. Cannot pull-up to load while already loading
        boolean condition2 = !isLoadMoreFinish;

        // 3. Cannot load while refreshing
        boolean condition3 = !isRefreshFinish;

        return condition1
                && condition2
                && condition3
                && condition4
                && condition5
                && loadMoreEnabled;
    }

    /** Handle the data loading logic */
    private void loadData() {
        // Set loading state to make the layout visible
        setLoadMoreFinish(true);
        if (mOnLoadListener != null) {
            new Handler()
                    .postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    mOnLoadListener.onLoad();
                                }
                            },
                            3000);
        } else { // If no load listener is set, stop after 2 seconds
            new Handler()
                    .postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    setLoadMoreFinish(false);
                                }
                            },
                            2000);
        }
    }

    /**
     * Set the pull-up loading mode: auto-show FooterView and load when scrolled to bottom, or
     * require an additional manual pull-up after reaching the bottom.
     *
     * @param autoLoading true to auto-load when scrolled to bottom, false to require manual pull-up
     */
    public void setAutoLoading(boolean autoLoading) {
        this.autoLoading = autoLoading;
    }

    /**
     * Set the refresh state, controlled by the flushing parameter
     *
     * @param flushing whether to refresh
     */
    public void setRefreshing(boolean flushing) {
        isRefreshFinish = flushing;
        super.setRefreshing(flushing);
        // setRefreshing(flushing,);
    }

    /**
     * Set the loading state
     *
     * @param loading loading state
     */
    public void setLoadMoreFinish(boolean loading) {
        // Update the current state
        isLoadMoreFinish = loading;
        if (isLoadMoreFinish) {
            if (mListView != null) {
                // Add and display the footer layout
                mListView.addFooterView(mFooterView);
                if (mListView.getAdapter() != null) {
                    // Scroll the FooterView into view after adding it
                    mListView.smoothScrollToPosition(mListView.getAdapter().getCount() - 1);
                }
            }
        } else {
            // Hide the layout
            if (mListView != null && mListView.getFooterViewsCount() > 0) {
                mListView.removeFooterView(mFooterView);
            }

            // Reset scroll coordinates
            mDownY = 0;
            mUpY = 0;
        }
    }

    /** Set the scroll listener for the ListView */
    private void setListViewOnScroll() {

        mListView.setOnScrollListener(
                new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {
                        if (canLoadMore() && autoLoading) {
                            // Auto-show FooterView and load data
                            loadData();
                        }
                    }

                    @Override
                    public void onScroll(
                            AbsListView view,
                            int firstVisibleItem,
                            int visibleItemCount,
                            int totalItemCount) {
                        // Reset conditions to false at the start of each scroll
                        condition4 = false;
                        condition5 = false;

                        if ((firstVisibleItem + visibleItemCount) == totalItemCount) {
                            View lastVisibleItemView =
                                    mListView.getChildAt(mListView.getChildCount() - 1);
                            if (lastVisibleItemView != null
                                    && lastVisibleItemView.getBottom() == mListView.getHeight()) {
                                condition4 = true;
                            }
                        }

                        if (totalItemCount > visibleItemCount) {
                            // Condition 5: ListView data exceeds one screen
                            condition5 = true;
                        }
                    }
                });
    }

    /** Set the refresh event */
    private void setOnRefresh() {
        if (!isLoadMoreFinish) {
            setOnRefreshListener(
                    new OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            setRefreshing(true);
                            if (mFlushListener != null) {
                                mFlushListener.onFlush();
                            } else { // If no refresh listener is set, stop after 2 seconds
                                new Handler()
                                        .postDelayed(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        ChatUISwipeRefreshLayout.this.setRefreshing(
                                                                false);
                                                    }
                                                },
                                                2000);
                            }
                        }
                    });
        } else {
            // Prevent refresh during loading
            setRefreshing(false);
        }
    }

    /**
     * Set whether pull-to-refresh is enabled
     *
     * @param enabled whether pull-to-refresh is enabled
     */
    public void setCanRefresh(boolean enabled) {
        refreshEnabled = enabled;
        setEnabled(enabled);
    }

    public void setCanLoading(boolean enabled) {
        loadMoreEnabled = enabled;
    }

    public interface OnLoadListener {
        void onLoad();
    }

    public interface OnFlushListener {
        void onFlush();
    }

    public void setOnLoadListener(OnLoadListener listener) {
        this.mOnLoadListener = listener;
    }

    public void setOnFlushListener(OnFlushListener listener) {
        mFlushListener = listener;
    }
}
