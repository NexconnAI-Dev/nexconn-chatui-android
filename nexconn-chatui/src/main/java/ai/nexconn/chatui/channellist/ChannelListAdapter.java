package ai.nexconn.chatui.channellist;

import ai.nexconn.chatui.channellist.model.BaseUiChannel;
import ai.nexconn.chatui.channellist.provider.ConversationListProvider;
import ai.nexconn.chatui.widget.adapter.ViewHolder;
import ai.nexconn.chatui.widget.adapter.WrapperUtils;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.collection.SparseArrayCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * Channel list Adapter
 *
 * <p>Standalone implementation that directly holds a {@link ConversationListProvider} without
 * depending on a ProviderManager singleton. Created via Fragment's {@code onCreateAdapter()},
 * supports subclass customization.
 *
 * @since 5.10.4
 */
public class ChannelListAdapter extends RecyclerView.Adapter<ViewHolder> {

    private static final int ITEM_TYPE_EMPTY = -200;
    private static final int BASE_ITEM_TYPE_HEADER = -300;
    private static final int BASE_ITEM_TYPE_FOOTER = -400;
    private static final int ITEM_TYPE_CONVERSATION = 0;

    protected List<BaseUiChannel> mDataList = new ArrayList<>();
    private ConversationListProvider provider;
    private OnItemClickListener clickListener;

    private View mEmptyView;
    private @LayoutRes int mEmptyViewId;
    private final SparseArrayCompat<View> mHeaderViews = new SparseArrayCompat<>();
    private final SparseArrayCompat<View> mFootViews = new SparseArrayCompat<>();

    public ChannelListAdapter() {
        this(new ConversationListProvider());
    }

    public ChannelListAdapter(@NonNull ConversationListProvider provider) {
        this.provider = provider;
    }

    public void setProvider(@NonNull ConversationListProvider provider) {
        this.provider = provider;
    }

    // region Click events

    public interface OnItemClickListener {
        void onItemClick(View view, ViewHolder holder, int position);

        boolean onItemLongClick(View view, ViewHolder holder, int position);
    }

    public void setItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    // endregion

    // region Data operations

    public void setDataCollection(List<BaseUiChannel> data) {
        if (data == null) {
            data = new ArrayList<>();
        }
        this.mDataList = new ArrayList<>(data);
        notifyDataSetChanged();
    }

    public List<BaseUiChannel> getData() {
        return mDataList;
    }

    public BaseUiChannel getItem(int position) {
        return mDataList.get(position);
    }

    // endregion

    // region RecyclerView.Adapter implementation

    @Override
    public int getItemViewType(int position) {
        if (isHeaderViewPos(position)) {
            return mHeaderViews.keyAt(position);
        }
        if (isFooterViewPos(position)) {
            return mFootViews.keyAt(
                    position - (getHeadersCount() + (isEmpty() ? 1 : getRealItemCount())));
        }
        if (isEmpty()) {
            return ITEM_TYPE_EMPTY;
        }
        return ITEM_TYPE_CONVERSATION;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mHeaderViews.get(viewType) != null) {
            return ViewHolder.createViewHolder(parent.getContext(), mHeaderViews.get(viewType));
        }
        if (mFootViews.get(viewType) != null) {
            return ViewHolder.createViewHolder(parent.getContext(), mFootViews.get(viewType));
        }
        if (viewType == ITEM_TYPE_EMPTY) {
            if (mEmptyView != null) {
                return ViewHolder.createViewHolder(parent.getContext(), mEmptyView);
            } else {
                return ViewHolder.createViewHolder(parent.getContext(), parent, mEmptyViewId);
            }
        }
        return provider.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (isHeaderViewPos(position) || isFooterViewPos(position) || isEmpty()) {
            return;
        }
        int listPosition = position - getHeadersCount();
        BaseUiChannel item = mDataList.get(listPosition);
        provider.bindViewHolder(holder, item, listPosition, mDataList, null);

        holder.itemView.setOnClickListener(
                v -> {
                    if (clickListener != null) {
                        int dataPosition = resolveDataPosition(holder, listPosition);
                        if (dataPosition == RecyclerView.NO_POSITION) {
                            return;
                        }
                        clickListener.onItemClick(v, holder, dataPosition);
                    }
                });
        holder.itemView.setOnLongClickListener(
                v -> {
                    if (clickListener == null) {
                        return false;
                    }
                    int dataPosition = resolveDataPosition(holder, listPosition);
                    if (dataPosition == RecyclerView.NO_POSITION) {
                        return false;
                    }
                    return clickListener.onItemLongClick(v, holder, dataPosition);
                });
    }

    @Override
    public int getItemCount() {
        if (isEmpty()) {
            return getHeadersCount() + getFootersCount() + 1;
        }
        return getHeadersCount() + getFootersCount() + getRealItemCount();
    }

    // endregion

    // region Header / Footer / Empty

    public void addHeaderView(View view) {
        if (view == null) return;
        detachFromParent(view);
        mHeaderViews.put(mHeaderViews.size() + BASE_ITEM_TYPE_HEADER, view);
    }

    public void addFootView(View view) {
        if (view == null) return;
        detachFromParent(view);
        mFootViews.put(mFootViews.size() + BASE_ITEM_TYPE_FOOTER, view);
    }

    public void setEmptyView(View view) {
        if (view == null) return;
        detachFromParent(view);
        mEmptyView = view;
    }

    public void setEmptyView(@LayoutRes int emptyId) {
        mEmptyViewId = emptyId;
    }

    public int getHeadersCount() {
        return mHeaderViews.size();
    }

    public int getFootersCount() {
        return mFootViews.size();
    }

    // endregion

    // region Internal methods

    private int getRealItemCount() {
        return mDataList.size();
    }

    private boolean isEmpty() {
        return (mEmptyView != null || mEmptyViewId != 0) && getRealItemCount() == 0;
    }

    private boolean isHeaderViewPos(int position) {
        return position < getHeadersCount();
    }

    private boolean isFooterViewPos(int position) {
        return position >= getHeadersCount() + (isEmpty() ? 1 : getRealItemCount());
    }

    private void detachFromParent(View view) {
        if (view.getParent() != null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }

    /**
     * RecyclerView data can refresh while user is touching, causing holder position to become
     * NO_POSITION temporarily. Fall back to the bind-time listPosition to keep click/long-click
     * stable.
     */
    private int resolveDataPosition(@NonNull ViewHolder holder, int bindPosition) {
        int adapterPosition = holder.getAdapterPosition();
        if (adapterPosition != RecyclerView.NO_POSITION) {
            int dataPosition = adapterPosition - getHeadersCount();
            if (dataPosition >= 0 && dataPosition < mDataList.size()) {
                return dataPosition;
            }
        }
        if (bindPosition >= 0 && bindPosition < mDataList.size()) {
            return bindPosition;
        }
        return RecyclerView.NO_POSITION;
    }

    // endregion

    // region GridLayout support

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        WrapperUtils.onAttachedToRecyclerView(
                this,
                recyclerView,
                (layoutManager, oldLookup, position) -> {
                    int viewType = getItemViewType(position);
                    if (mHeaderViews.get(viewType) != null
                            || mFootViews.get(viewType) != null
                            || isEmpty()) {
                        return layoutManager.getSpanCount();
                    }
                    if (oldLookup != null) {
                        return oldLookup.getSpanSize(position);
                    }
                    return 1;
                });
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        int position = holder.getLayoutPosition();
        if (isHeaderViewPos(position) || isFooterViewPos(position) || isEmpty()) {
            WrapperUtils.setFullSpan(holder);
        }
    }

    // endregion
}
