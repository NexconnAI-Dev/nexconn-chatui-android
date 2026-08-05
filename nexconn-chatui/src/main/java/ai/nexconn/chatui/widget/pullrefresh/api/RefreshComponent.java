package ai.nexconn.chatui.widget.pullrefresh.api;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.annotation.RestrictTo.Scope.SUBCLASSES;

import ai.nexconn.chatui.widget.pullrefresh.constant.SpinnerStyle;
import ai.nexconn.chatui.widget.pullrefresh.listener.OnStateChangedListener;
import android.view.View;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/** Internal refresh component. */
public interface RefreshComponent extends OnStateChangedListener {
    /**
     * Get the actual view.
     *
     * @return The actual view
     */
    @NonNull
    View getView();

    /**
     * Get the spinner style {@link SpinnerStyle}. Must return non-null.
     *
     * @return The spinner style
     */
    @NonNull
    SpinnerStyle getSpinnerStyle();

    /**
     * [Framework internal only] Set the primary colors.
     *
     * @param colors Corresponding to srlPrimaryColor and srlAccentColor in XML
     */
    @RestrictTo({LIBRARY, LIBRARY_GROUP, SUBCLASSES})
    void setPrimaryColors(@ColorInt int... colors);

    /**
     * [Framework internal only] Called when dimension measurement is complete (called once if
     * height does not change; invoked in RefreshLayout#onMeasure).
     *
     * @param kernel RefreshKernel
     * @param height HeaderHeight or FooterHeight
     * @param maxDragHeight Maximum drag height
     */
    @RestrictTo({LIBRARY, LIBRARY_GROUP, SUBCLASSES})
    void onInitialized(@NonNull RefreshKernel kernel, int height, int maxDragHeight);

    /**
     * [Framework internal only] Called during finger drag (called continuously; isDragging replaces
     * the old onPulling/onReleasing).
     *
     * @param isDragging true if finger is dragging, false if rebound animation
     * @param percent Pull percentage, value = offset/footerHeight (range: 0 to
     *     (footerHeight+maxDragHeight)/footerHeight)
     * @param offset Pixel offset of pull, range: 0 to (footerHeight+maxDragHeight)
     * @param height Height: HeaderHeight or FooterHeight (offset can exceed height, making percent
     *     > 1)
     * @param maxDragHeight Maximum drag height. offset can exceed the height parameter but will not
     *     exceed maxDragHeight
     */
    @RestrictTo({LIBRARY, LIBRARY_GROUP, SUBCLASSES})
    void onMoving(boolean isDragging, float percent, int offset, int height, int maxDragHeight);

    /**
     * [Framework internal only] Called at the moment of release (called once, triggers loading).
     *
     * @param refreshLayout RefreshLayout
     * @param height Height: HeaderHeight or FooterHeight
     * @param maxDragHeight Maximum drag height
     */
    @RestrictTo({LIBRARY, LIBRARY_GROUP, SUBCLASSES})
    void onReleased(@NonNull RefreshLayout refreshLayout, int height, int maxDragHeight);

    /**
     * [Framework internal only] Called when animation starts.
     *
     * @param refreshLayout RefreshLayout
     * @param height HeaderHeight or FooterHeight
     * @param maxDragHeight Maximum drag height
     */
    @RestrictTo({LIBRARY, LIBRARY_GROUP, SUBCLASSES})
    void onStartAnimator(@NonNull RefreshLayout refreshLayout, int height, int maxDragHeight);

    /**
     * [Framework internal only] Called when animation ends.
     *
     * @param refreshLayout RefreshLayout
     * @param success Whether data was refreshed or loaded successfully
     * @return Time needed for finish animation. Returning Integer.MAX_VALUE cancels the finish
     *     event and keeps the current state
     */
    @RestrictTo({LIBRARY, LIBRARY_GROUP, SUBCLASSES})
    int onFinish(@NonNull RefreshLayout refreshLayout, boolean success);

    /**
     * [Framework internal only] Called during horizontal drag.
     *
     * @param percentX Horizontal position percentage of finger relative to screen (0 to 1)
     * @param offsetX Horizontal pixel offset of finger relative to screen (0 to LayoutWidth)
     * @param offsetMax Maximum offset
     */
    @RestrictTo({LIBRARY, LIBRARY_GROUP, SUBCLASSES})
    void onHorizontalDrag(float percentX, int offsetX, int offsetMax);

    /**
     * Whether horizontal drag is supported (affects onHorizontalDrag invocation).
     *
     * @return Horizontal drag consumes more time and resources; return false if not supported
     */
    boolean isSupportHorizontalDrag();
}
