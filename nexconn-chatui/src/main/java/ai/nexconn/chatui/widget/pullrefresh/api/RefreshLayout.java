package ai.nexconn.chatui.widget.pullrefresh.api;

import ai.nexconn.chatui.widget.pullrefresh.constant.RefreshState;
import ai.nexconn.chatui.widget.pullrefresh.listener.OnLoadMoreListener;
import ai.nexconn.chatui.widget.pullrefresh.listener.OnMultiListener;
import ai.nexconn.chatui.widget.pullrefresh.listener.OnRefreshListener;
import ai.nexconn.chatui.widget.pullrefresh.listener.OnRefreshLoadMoreListener;
import ai.nexconn.chatui.widget.pullrefresh.listener.ScrollBoundaryDecider;
import ai.nexconn.chatui.widget.pullrefresh.simple.SimpleBoundaryDecider;
import ai.nexconn.chatui.widget.pullrefresh.simple.SimpleMultiListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Refresh layout interface. */
@SuppressWarnings({"UnusedReturnValue", "SameParameterValue", "unused"})
public interface RefreshLayout {

    /**
     * Set the Footer's height.
     *
     * @param dp Density-independent Pixels Density-independent pixels (use px2dp for pixel
     *     conversion)
     * @return RefreshLayout
     */
    RefreshLayout setFooterHeight(float dp);

    /**
     * Set the Footer height.
     *
     * @param px Pixels
     * @return RefreshLayout
     */
    RefreshLayout setFooterHeightPx(int px);

    /**
     * Set the Header's height.
     *
     * @param dp Density-independent Pixels Density-independent pixels (use px2dp for pixel
     *     conversion)
     * @return RefreshLayout
     */
    RefreshLayout setHeaderHeight(float dp);

    /**
     * Set the Header height.
     *
     * @param px Pixels
     * @return RefreshLayout
     */
    RefreshLayout setHeaderHeightPx(int px);

    /**
     * Set the Header's start offset (see srlHeaderInsetStart in the RepastPracticeActivity XML in
     * demo-app for the practical application).
     *
     * @param dp Density-independent Pixels Density-independent pixels (use px2dp for pixel
     *     conversion)
     * @return RefreshLayout
     */
    RefreshLayout setHeaderInsetStart(float dp);

    /**
     * Set the Header's start offset (see srlHeaderInsetStart in the RepastPracticeActivity XML in
     * demo-app for the practical application).
     *
     * @param px Pixels
     * @return RefreshLayout
     */
    RefreshLayout setHeaderInsetStartPx(int px);

    /**
     * Set the Footer's start offset.
     *
     * @see RefreshLayout#setHeaderInsetStart(float)
     * @param dp Density-independent Pixels Density-independent pixels (use px2dp for pixel
     *     conversion)
     * @return RefreshLayout
     */
    RefreshLayout setFooterInsetStart(float dp);

    /**
     * Set the Footer's start offset.
     *
     * @param px Pixels
     * @return RefreshLayout
     */
    RefreshLayout setFooterInsetStartPx(int px);

    /**
     * Set the damping effect. Ratio of displayed drag height to actual drag height (default 0.5).
     *
     * @param rate ratio = (The drag height of the view)/(The actual drag height of the finger)
     *     Ratio = view drag height / finger drag height
     * @return RefreshLayout
     */
    RefreshLayout setDragRate(@FloatRange(from = 0, to = 1) float rate);

    /**
     * Set the ratio of the maximum height to drag header.
     *
     * @param rate ratio = (the maximum height to drag header)/(the height of header) Ratio = max
     *     pull-down height / Header height
     * @return RefreshLayout
     */
    RefreshLayout setHeaderMaxDragRate(@FloatRange(from = 1, to = 10) float rate);

    /**
     * Set the ratio of the maximum height to drag footer.
     *
     * @param rate ratio = (the maximum height to drag footer)/(the height of footer) Ratio = max
     *     pull-up height / Footer height
     * @return RefreshLayout
     */
    RefreshLayout setFooterMaxDragRate(@FloatRange(from = 1, to = 10) float rate);

    /**
     * Set the ratio at which the refresh is triggered.
     *
     * @param rate Ratio of trigger refresh distance to HeaderHeight
     * @return RefreshLayout
     */
    RefreshLayout setHeaderTriggerRate(@FloatRange(from = 0, to = 1.0) float rate);

    /**
     * Set the ratio at which the load more is triggered.
     *
     * @param rate Ratio of trigger load distance to FooterHeight
     * @return RefreshLayout
     */
    RefreshLayout setFooterTriggerRate(@FloatRange(from = 0, to = 1.0) float rate);

    /**
     * Set the rebound interpolator.
     *
     * @param interpolator Animation interpolator
     * @return RefreshLayout
     */
    RefreshLayout setReboundInterpolator(@NonNull Interpolator interpolator);

    /**
     * Set the duration of the rebound animation.
     *
     * @param duration Duration
     * @return RefreshLayout
     */
    RefreshLayout setReboundDuration(int duration);

    /**
     * Set the footer of RefreshLayout.
     *
     * @param footer RefreshFooter refresh footer
     * @return RefreshLayout
     */
    RefreshLayout setRefreshFooter(@NonNull RefreshFooter footer);

    /**
     * Set the footer of RefreshLayout.
     *
     * @param footer RefreshFooter refresh footer
     * @param width the width in px, can use MATCH_PARENT and WRAP_CONTENT. Width in px, can use
     *     MATCH_PARENT, WRAP_CONTENT
     * @param height the height in px, can use MATCH_PARENT and WRAP_CONTENT. Height in px, can use
     *     MATCH_PARENT, WRAP_CONTENT
     * @return RefreshLayout
     */
    RefreshLayout setRefreshFooter(@NonNull RefreshFooter footer, int width, int height);

    /**
     * Set the header of RefreshLayout.
     *
     * @param header RefreshHeader refresh header
     * @return RefreshLayout
     */
    RefreshLayout setRefreshHeader(@NonNull RefreshHeader header);

    /**
     * Set the header of RefreshLayout.
     *
     * @param header RefreshHeader refresh header
     * @param width the width in px, can use MATCH_PARENT and WRAP_CONTENT. Width in px, can use
     *     MATCH_PARENT, WRAP_CONTENT
     * @param height the height in px, can use MATCH_PARENT and WRAP_CONTENT. Height in px, can use
     *     MATCH_PARENT, WRAP_CONTENT
     * @return RefreshLayout
     */
    RefreshLayout setRefreshHeader(@NonNull RefreshHeader header, int width, int height);

    /**
     * Set the content of RefreshLayout (Suitable for non-XML pages, not suitable for replacing empty
     * layouts).
     *
     * @param content View content view
     * @return RefreshLayout
     */
    RefreshLayout setRefreshContent(@NonNull View content);

    /**
     * Set the content of RefreshLayout (Suitable for non-XML pages, not suitable for replacing empty
     * layouts).
     *
     * @param content View content view
     * @param width the width in px, can use MATCH_PARENT and WRAP_CONTENT. Width in px, can use
     *     MATCH_PARENT, WRAP_CONTENT
     * @param height the height in px, can use MATCH_PARENT and WRAP_CONTENT. Height in px, can use
     *     MATCH_PARENT, WRAP_CONTENT
     * @return RefreshLayout
     */
    RefreshLayout setRefreshContent(@NonNull View content, int width, int height);

    /**
     * Whether to enable pull-down refresh (enabled by default).
     *
     * @param enabled Whether to enable
     * @return RefreshLayout
     */
    RefreshLayout setEnableRefresh(boolean enabled);

    /**
     * Set whether to enable pull-up loading more (enabled by default).
     *
     * @param enabled Whether to enable
     * @return RefreshLayout
     */
    RefreshLayout setEnableLoadMore(boolean enabled);

    /**
     * Sets whether to listen for the list to trigger a load event when scrolling to the bottom
     * (default true).
     *
     * @param enabled Whether to enable
     * @return RefreshLayout
     */
    RefreshLayout setEnableAutoLoadMore(boolean enabled);

    /**
     * Set whether to pull down the content while pulling down the header.
     *
     * @param enabled Whether to enable
     * @return RefreshLayout
     */
    RefreshLayout setEnableHeaderTranslationContent(boolean enabled);

    /**
     * Set whether to pull up the content while pulling up the footer.
     *
     * @param enabled Whether to enable
     * @return RefreshLayout
     */
    RefreshLayout setEnableFooterTranslationContent(boolean enabled);

    /**
     * Set whether to enable the over-scroll bounce function.
     *
     * @param enabled Whether to enable
     * @return RefreshLayout
     */
    RefreshLayout setEnableOverScrollBounce(boolean enabled);

    /**
     * Set whether to enable pure scroll mode.
     *
     * @param enabled Whether to enable
     * @return RefreshLayout
     */
    RefreshLayout setEnablePureScrollMode(boolean enabled);

    /**
     * Set whether to scroll the content to display new data after loading more complete. Whether to
     * scroll content to show new data after load more.
     *
     * @param enabled Whether to enable
     * @return RefreshLayout
     */
    RefreshLayout setEnableScrollContentWhenLoaded(boolean enabled);

    /**
     * Set whether to scroll the content to display new data after the refresh is complete. Whether
     * to scroll content to show new data after refresh
     *
     * @param enabled Whether to enable
     * @return RefreshLayout
     */
    RefreshLayout setEnableScrollContentWhenRefreshed(boolean enabled);

    /**
     * Set whether to pull up and load more when the content is not full of one page. Whether to
     * allow pull-up load more when content is less than one page.
     *
     * @param enabled Whether to enable
     * @return RefreshLayout
     */
    RefreshLayout setEnableLoadMoreWhenContentNotFull(boolean enabled);

    /**
     * Set whether to enable over-scroll drag (imitation iPhone effect).
     *
     * @param enabled Whether to enable
     * @return RefreshLayout
     */
    RefreshLayout setEnableOverScrollDrag(boolean enabled);

    /**
     * Set whether Footer follows the content after there is no more data.
     *
     * @param enabled Whether to enable
     * @return RefreshLayout
     */
    RefreshLayout setEnableFooterFollowWhenNoMoreData(boolean enabled);

    /**
     * Set whether to clip header when the Header is in the FixedBehind state.
     *
     * @param enabled Whether to enable
     * @return RefreshLayout
     */
    RefreshLayout setEnableClipHeaderWhenFixedBehind(boolean enabled);

    /**
     * Set whether to clip footer when the Footer is in the FixedBehind state.
     *
     * @param enabled Whether to enable
     * @return RefreshLayout
     */
    RefreshLayout setEnableClipFooterWhenFixedBehind(boolean enabled);

    /**
     * Setting whether nesting scrolling is enabled (default off + smart on). Set whether to enable
     * nested scrolling (default off, smart auto-enable).
     *
     * @param enabled Whether to enable
     * @return RefreshLayout
     */
    RefreshLayout setEnableNestedScroll(boolean enabled);

    /**
     * Set the View ID fixed below the Header, which stays stationary when Footer scrolls.
     *
     * @param id View ID fixed at header
     * @return RefreshLayout
     */
    RefreshLayout setFixedHeaderViewId(@IdRes int id);

    /**
     * Set the View ID fixed above the Footer, which stays stationary when Header scrolls.
     *
     * @param id View ID fixed at footer
     * @return RefreshLayout
     */
    RefreshLayout setFixedFooterViewId(@IdRes int id);

    /**
     * Set the View ID that should scroll along with the Header. Defaults to the entire Content
     * view.
     *
     * @param id View ID fixed at header
     * @return RefreshLayout
     */
    RefreshLayout setHeaderTranslationViewId(@IdRes int id);

    /**
     * Set the View ID that should scroll along with the Footer. Defaults to the entire Content
     * view.
     *
     * @param id View ID fixed at header
     * @return RefreshLayout
     */
    RefreshLayout setFooterTranslationViewId(@IdRes int id);

    /**
     * Set whether to disable content interaction during refresh.
     *
     * @param disable Whether to disable
     * @return RefreshLayout
     */
    RefreshLayout setDisableContentWhenRefresh(boolean disable);

    /**
     * Set whether to disable content interaction during loading.
     *
     * @param disable Whether to disable
     * @return RefreshLayout
     */
    RefreshLayout setDisableContentWhenLoading(boolean disable);

    /**
     * Set refresh listener separately.
     *
     * @param listener OnRefreshListener refresh listener
     * @return RefreshLayout
     */
    RefreshLayout setOnRefreshListener(OnRefreshListener listener);

    /**
     * Set load more listener separately.
     *
     * @param listener OnLoadMoreListener load more listener
     * @return RefreshLayout
     */
    RefreshLayout setOnLoadMoreListener(OnLoadMoreListener listener);

    /**
     * Set refresh and load listeners at the same time.
     *
     * @param listener OnRefreshLoadMoreListener refresh and load more listener
     * @return RefreshLayout
     */
    RefreshLayout setOnRefreshLoadMoreListener(OnRefreshLoadMoreListener listener);

    /**
     * Set up a multi-function listener. Recommended: {@link SimpleMultiListener}.
     *
     * @param listener OnMultiPurposeListener multi-purpose listener
     * @return RefreshLayout
     */
    RefreshLayout setOnMultiListener(OnMultiListener listener);

    /**
     * Set the scroll boundary Decider, Can customize when you can refresh. Recommended {@link
     * SimpleBoundaryDecider}.
     *
     * @param boundary ScrollBoundaryDecider scroll boundary decider
     * @return RefreshLayout
     */
    RefreshLayout setScrollBoundaryDecider(ScrollBoundaryDecider boundary);

    /**
     * Set theme color int (primaryColor and accentColor).
     *
     * @param primaryColors ColorInt theme colors
     * @return RefreshLayout
     */
    RefreshLayout setPrimaryColors(@ColorInt int... primaryColors);

    /**
     * Set theme color id (primaryColor and accentColor).
     *
     * @param primaryColorId ColorRes theme color resource IDs
     * @return RefreshLayout
     */
    RefreshLayout setPrimaryColorsId(@ColorRes int... primaryColorId);

    /**
     * Finish refresh.
     *
     * @return RefreshLayout
     */
    RefreshLayout finishRefresh();

    /**
     * Finish refresh.
     *
     * @param delayed Start delay
     * @return RefreshLayout
     */
    RefreshLayout finishRefresh(int delayed);

    /**
     * Finish refresh.
     *
     * @param success Whether data was refreshed successfully (affects last update time)
     * @return RefreshLayout
     */
    RefreshLayout finishRefresh(boolean success);

    /**
     * Finish refresh.
     *
     * @param delayed Start delay
     * @param success Whether data was refreshed successfully (affects last update time)
     * @param noMoreData Whether there is more data
     * @return RefreshLayout
     */
    RefreshLayout finishRefresh(int delayed, boolean success, Boolean noMoreData);

    /**
     * Finish refresh and mark no more data.
     *
     * @return RefreshLayout
     */
    RefreshLayout finishRefreshWithNoMoreData();

    /**
     * Finish load more.
     *
     * @return RefreshLayout
     */
    RefreshLayout finishLoadMore();

    /**
     * Finish load more.
     *
     * @param delayed Start delay
     * @return RefreshLayout
     */
    RefreshLayout finishLoadMore(int delayed);

    /**
     * Finish load more.
     *
     * @param success Whether data was successful
     * @return RefreshLayout
     */
    RefreshLayout finishLoadMore(boolean success);

    /**
     * Finish load more.
     *
     * @param delayed Start delay
     * @param success Whether data was successful
     * @param noMoreData Whether there is more data
     * @return RefreshLayout
     */
    RefreshLayout finishLoadMore(int delayed, boolean success, boolean noMoreData);

    /**
     * Finish load more and mark no more data.
     *
     * @return RefreshLayout
     */
    RefreshLayout finishLoadMoreWithNoMoreData();

    /**
     * Close the Header or Footer. Note: this cannot replace finishRefresh and finishLoadMore. 1.
     * closeHeaderOrFooter can close header/footer at any time in any state. 2. finishRefresh and
     * finishLoadMore can only close during refresh or load respectively.
     *
     * @return RefreshLayout
     */
    RefreshLayout closeHeaderOrFooter();

    /**
     * Set the no-more-data state.
     *
     * @param noMoreData Whether there is more data
     * @return RefreshLayout Prefer using the following three methods instead, as they synchronize
     *     state change with animation end. Use {@link RefreshLayout#resetNoMoreData()} use {@link
     *     RefreshLayout#finishRefreshWithNoMoreData()} use {@link
     *     RefreshLayout#finishLoadMoreWithNoMoreData()}
     */
    RefreshLayout setNoMoreData(boolean noMoreData);

    /**
     * Restore the original state after finishLoadMoreWithNoMoreData.
     *
     * @return RefreshLayout
     */
    RefreshLayout resetNoMoreData();

    /**
     * Get the current Header of RefreshLayout.
     *
     * @return RefreshLayout
     */
    @Nullable
    RefreshHeader getRefreshHeader();

    /**
     * Get the current Footer of RefreshLayout.
     *
     * @return RefreshLayout
     */
    @Nullable
    RefreshFooter getRefreshFooter();

    /**
     * Get the current state of RefreshLayout.
     *
     * @return RefreshLayout
     */
    @NonNull
    RefreshState getState();

    /**
     * Get the ViewGroup of RefreshLayout.
     *
     * @return ViewGroup
     */
    @NonNull
    ViewGroup getLayout();

    /**
     * Display refresh animation and trigger refresh event.
     *
     * @return true or false, Status non-compliance will fail. true if successful (fails if state is
     *     non-compliant)
     */
    boolean autoRefresh();

    /**
     * Display refresh animation and trigger refresh event with delayed start.
     *
     * @param delayed Start delay
     * @return true or false, Status non-compliance will fail. true if successful (fails if state is
     *     non-compliant)
     */
    boolean autoRefresh(int delayed);

    /**
     * Display refresh animation without triggering events.
     *
     * @return true or false, Status non-compliance will fail. true if successful (fails if state is
     *     non-compliant)
     */
    boolean autoRefreshAnimationOnly();

    /**
     * Display refresh animation with multifunction options.
     *
     * @param delayed Start delay
     * @param duration Drag animation duration
     * @param dragRate Drag height ratio
     * @param animationOnly Animation only
     * @return true or false, Status non-compliance will fail. true if successful (fails if state is
     *     non-compliant)
     */
    boolean autoRefresh(int delayed, int duration, float dragRate, boolean animationOnly);

    /**
     * Display load more animation and trigger load more event.
     *
     * @return true or false, Status non-compliance will fail. true if successful (fails if state is
     *     non-compliant)
     */
    boolean autoLoadMore();

    /**
     * Display load more animation and trigger load more event with delayed start.
     *
     * @param delayed Start delay
     * @return true or false, Status non-compliance will fail. true if successful (fails if state is
     *     non-compliant)
     */
    boolean autoLoadMore(int delayed);

    /**
     * Display load more animation without triggering events.
     *
     * @return true or false, Status non-compliance will fail. true if successful (fails if state is
     *     non-compliant)
     */
    boolean autoLoadMoreAnimationOnly();

    /**
     * Display load more animation with multifunction options.
     *
     * @param delayed Start delay
     * @param duration Drag animation duration
     * @param dragRate Drag height ratio
     * @param animationOnly Whether to only show animation without callbacks
     * @return true or false, Status non-compliance will fail. true if successful (fails if state is
     *     non-compliant)
     */
    boolean autoLoadMore(int delayed, int duration, float dragRate, boolean animationOnly);

    /**
     * Whether currently refreshing
     *
     * @return RefreshLayout
     */
    boolean isRefreshing();

    /**
     * Whether currently loading
     *
     * @return RefreshLayout
     */
    boolean isLoading();
}
