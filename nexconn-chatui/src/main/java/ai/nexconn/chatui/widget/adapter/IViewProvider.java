package ai.nexconn.chatui.widget.adapter;

import android.view.ViewGroup;
import java.util.List;

public interface IViewProvider<T> {
    ViewHolder onCreateViewHolder(ViewGroup parent, int viewType);

    /**
     * @param item the data item
     * @return whether this provider handles the item type
     */
    boolean isItemViewType(T item);

    /**
     * Bind view with data source
     *
     * @param holder the view holder
     * @param t the data item
     * @param position the position
     */
    void bindViewHolder(
            ViewHolder holder, T t, int position, List<T> list, IViewProviderListener<T> listener);
}
