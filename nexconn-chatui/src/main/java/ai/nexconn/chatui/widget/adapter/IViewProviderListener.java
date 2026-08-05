package ai.nexconn.chatui.widget.adapter;

import android.view.View;

public interface IViewProviderListener<T> {

    /**
     * @param clickType identifier to distinguish click events
     * @param data the data source
     */
    void onViewClick(int clickType, T data);

    /**
     * @param view the view that triggered the long press
     * @param clickType identifier to distinguish click events
     * @param data the data source
     */
    default boolean onViewLongClick(View view, int clickType, T data) {
        return onViewLongClick(clickType, data);
    }

    /**
     * @param clickType identifier to distinguish click events
     * @param data the data source
     * @deprecated Use {@link #onViewLongClick(View, int, Object)} instead
     */
    @Deprecated
    boolean onViewLongClick(int clickType, T data);
}
