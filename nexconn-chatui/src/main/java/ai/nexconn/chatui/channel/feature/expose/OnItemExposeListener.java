package ai.nexconn.chatui.channel.feature.expose;

public interface OnItemExposeListener<T> {
    /**
     * Item visibility callback. When this method is called, the item is always visually visible
     * (regardless of how much is visible).
     *
     * @param visible true if logically visible, i.e., width/height exceeds threshold
     * @param position the item's position in the list
     * @param data the data
     */
    void onItemViewVisible(boolean visible, int position, T data);
}
