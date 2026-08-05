package ai.nexconn.chatui.base.interfaces;

import ai.nexconn.chat.error.NCError;

/**
 * Data change listener.
 *
 * @since 5.12.0
 */
public interface OnDataChangeListener<T> {

    /**
     * Called when data changes successfully.
     *
     * @param t the callback result data
     */
    void onDataChange(T t);

    /**
     * Called when an error occurs.
     *
     * @param error the error
     */
    default void onDataError(NCError error) {}
}
