package ai.nexconn.chatui.widget.pullrefresh.api;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.annotation.RestrictTo.Scope.SUBCLASSES;

import androidx.annotation.RestrictTo;

/** Refresh footer. */
public interface RefreshFooter extends RefreshComponent {

    /**
     * [Framework internal only] Set that all data has been loaded, preventing further load
     * triggers.
     *
     * @param noMoreData Whether there is more data
     * @return true true if the all-data-loaded state display is supported, false otherwise
     */
    @RestrictTo({LIBRARY, LIBRARY_GROUP, SUBCLASSES})
    boolean setNoMoreData(boolean noMoreData);
}
