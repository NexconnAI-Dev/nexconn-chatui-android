package ai.nexconn.chatui.widget.pullrefresh.api;

import ai.nexconn.chatui.widget.pullrefresh.constant.RefreshState;
import android.animation.ValueAnimator;
import androidx.annotation.NonNull;

/** Core refresh layout interface. Opens APIs for complex Headers or Footers. */
@SuppressWarnings({"unused", "UnusedReturnValue", "SameParameterValue"})
public interface RefreshKernel {

    @NonNull
    RefreshLayout getRefreshLayout();

    @NonNull
    RefreshContent getRefreshContent();

    RefreshKernel setState(@NonNull RefreshState state);

    // <editor-fold desc="View displacement: Spinner">

    /**
     * Start two-level refresh.
     *
     * @param open Whether to expand
     * @return RefreshKernel
     */
    RefreshKernel startTwoLevel(boolean open);

    /**
     * Finish and close two-level refresh.
     *
     * @return RefreshKernel
     */
    RefreshKernel finishTwoLevel();

    /**
     * Translate the view to a specified position. moveSpinner is named after Google's official
     * {@link androidx.swiperefreshlayout.widget.SwipeRefreshLayout}
     *
     * @param spinner Position (px)
     * @param isDragging true if finger is dragging, false if rebound animation is playing
     * @return RefreshKernel
     */
    RefreshKernel moveSpinner(int spinner, boolean isDragging);

    /**
     * Execute animation to move the view to a specified position. moveSpinner is named after
     * Google's official {@link androidx.swiperefreshlayout.widget.SwipeRefreshLayout}
     *
     * @param endSpinner Target end position (px)
     * @return ValueAnimator null if no animation was executed
     */
    ValueAnimator animSpinner(int endSpinner);

    // </editor-fold>

    // <editor-fold desc="Touch event request">

    /**
     * Specify a background to draw for the Header or Footer during pull.
     *
     * @param internal Pass this when called from Header or Footer
     * @param backgroundColor Background color
     * @return RefreshKernel
     */
    RefreshKernel requestDrawBackgroundFor(@NonNull RefreshComponent internal, int backgroundColor);

    /**
     * Request touch events.
     *
     * @param internal Pass this when called from Header or Footer
     * @param request Whether to request touch events
     * @return RefreshKernel
     */
    RefreshKernel requestNeedTouchEventFor(@NonNull RefreshComponent internal, boolean request);

    /**
     * Set default content translation behavior.
     *
     * @param internal Pass this when called from Header or Footer
     * @param translation Whether to translate
     * @return RefreshKernel
     */
    RefreshKernel requestDefaultTranslationContentFor(
            @NonNull RefreshComponent internal, boolean translation);

    /**
     * Re-measure headerHeight or footerHeight. Requires height to be WRAP_CONTENT.
     *
     * @param internal Pass this when called from Header or Footer
     * @return RefreshKernel
     */
    RefreshKernel requestRemeasureHeightFor(@NonNull RefreshComponent internal);

    /**
     * Set the two-level rebound duration.
     *
     * @param duration Two-level rebound duration
     * @return RefreshKernel
     */
    RefreshKernel requestFloorDuration(int duration);

    /**
     * Set the height ratio for closing the two-level floor by swiping up from the bottom.
     *
     * @return RefreshKernel
     */
    RefreshKernel requestFloorBottomPullUpToCloseRate(float rate);
    // </editor-fold>
}
