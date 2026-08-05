package ai.nexconn.chatui.widget.pullrefresh.util;

import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.widget.pullrefresh.api.RefreshKernel;
import ai.nexconn.chatui.widget.pullrefresh.listener.CoordinatorLayoutListener;
import android.view.View;

/** Design compatibility fallback utility. */
public class DesignUtil {
    private static final String TAG = "DesignUtil";

    public static void checkCoordinatorLayout(
            View content, RefreshKernel kernel, final CoordinatorLayoutListener listener) {
        try { // try block must not be removed, otherwise compatibility issues will occur
            // SDK does not handle this; without referencing CoordinatorLayout, it is not supported
            //            if (content instanceof CoordinatorLayout) {
            //                kernel.getRefreshLayout().setEnableNestedScroll(false);
            //                ViewGroup layout = (ViewGroup) content;
            //                for (int i = layout.getChildCount() - 1; i >= 0; i--) {
            //                    View view = layout.getChildAt(i);
            //                    if (view instanceof AppBarLayout) {
            //                        ((AppBarLayout) view).addOnOffsetChangedListener(new
            // AppBarLayout.OnOffsetChangedListener() {
            //                            @Override
            //                            public void onOffsetChanged(AppBarLayout appBarLayout, int
            // verticalOffset) {
            //                                listener.onCoordinatorUpdate(
            //                                        verticalOffset >= 0,
            //                                        (appBarLayout.getTotalScrollRange() +
            // verticalOffset) <= 0);
            //                            }
            //                        });
            //                    }
            //                }
            //            }
        } catch (Throwable e) {
            RLog.e(TAG, "checkCoordinatorLayout", e);
        }
    }
}
