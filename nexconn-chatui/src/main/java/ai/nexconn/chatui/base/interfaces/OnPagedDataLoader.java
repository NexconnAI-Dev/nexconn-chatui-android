package ai.nexconn.chatui.base.interfaces;

/**
 * Paged data loading interface.
 *
 * @since 5.12.0
 */
public interface OnPagedDataLoader {

    /**
     * Loads the previous page of data.
     *
     * @param listener the load completion listener
     */
    default void loadPrevious(OnDataChangeListener<Boolean> listener) {}

    /**
     * Loads the next page of data.
     *
     * @param listener the load completion listener
     */
    void loadNext(OnDataChangeListener<Boolean> listener);

    /**
     * Returns whether there is a next page.
     *
     * @return true if more data is available
     */
    boolean hasNext();

    /**
     * Returns whether there is a previous page.
     *
     * @return true if a previous page is available
     */
    default boolean hasPrevious() {
        return false;
    }
}
