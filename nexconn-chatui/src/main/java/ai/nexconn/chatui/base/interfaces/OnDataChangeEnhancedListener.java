package ai.nexconn.chatui.base.interfaces;

import ai.nexconn.chat.error.NCError;
import java.util.Collections;
import java.util.List;

/**
 * Enhanced data change listener with error key support.
 *
 * @since 5.12.0
 */
public interface OnDataChangeEnhancedListener<T> extends OnDataChangeListener<T> {

    /**
     * Called when an error occurs.
     *
     * @param error the error
     */
    @Override
    default void onDataError(NCError error) {
        onDataError(error, Collections.emptyList());
    }

    /**
     * Called when an error occurs with error key list.
     *
     * @param error the error
     * @param errorKeys the list of error keys
     */
    default void onDataError(NCError error, List<String> errorKeys) {}
}
