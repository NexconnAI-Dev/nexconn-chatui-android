package ai.nexconn.chatui.base.interfaces;

/**
 * Action click event listener.
 *
 * @since 5.12.2
 */
public interface OnActionClickListener<T> {

    /**
     * Called when an action is clicked.
     *
     * @param t the action info
     */
    void onActionClick(T t);

    /**
     * Called when an action is clicked with confirmation support.
     *
     * @param t the action info
     * @param listener the confirmation click listener
     */
    default <E> void onActionClickWithConfirm(T t, OnConfirmClickListener<E> listener) {
        onActionClick(t);
    }

    interface OnConfirmClickListener<T> {
        void onActionClick(T t);
    }
}
