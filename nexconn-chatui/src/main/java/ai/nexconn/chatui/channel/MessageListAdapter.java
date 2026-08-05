package ai.nexconn.chatui.channel;

import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.widget.adapter.BaseAdapter;
import ai.nexconn.chatui.widget.adapter.IViewProviderListener;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;
import java.util.ArrayList;
import java.util.List;

public class MessageListAdapter extends BaseAdapter<UiMessage> {

    public MessageListAdapter(IViewProviderListener<UiMessage> listener) {
        super(listener, NCChatUIConfig.channelConfig().getMessageListProvider());
    }

    @Override
    public void setDataCollection(List<UiMessage> data) {
        if (data == null) {
            data = new ArrayList<>();
        }
        // Full refresh needed when transitioning between empty and non-empty states
        if ((mDataList.size() == 0 && data.size() > 0)
                || (mDataList.size() > 0 && data.size() == 0)) {
            super.setDataCollection(data);
            notifyDataSetChanged();
        } else {
            mDiffCallback.setNewList(data);
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(mDiffCallback, false);
            super.setDataCollection(data);
            diffResult.dispatchUpdatesTo(
                    new ListUpdateCallback() {
                        @Override
                        public void onInserted(int position, int count) {
                            notifyItemRangeInserted(getHeadersCount() + position, count);
                        }

                        @Override
                        public void onRemoved(int position, int count) {
                            notifyItemRangeRemoved(getHeadersCount() + position, count);
                        }

                        @Override
                        public void onMoved(int fromPosition, int toPosition) {
                            notifyItemMoved(
                                    getHeadersCount() + fromPosition,
                                    getHeadersCount() + toPosition);
                        }

                        @Override
                        public void onChanged(int position, int count, @Nullable Object payload) {
                            // Pass payload (non-null from getChangePayload) to suppress animation.
                            notifyItemRangeChanged(getHeadersCount() + position, count, payload);
                        }
                    });
        }
    }

    MessageDiffCallBack mDiffCallback = new MessageDiffCallBack();

    private class MessageDiffCallBack extends DiffUtil.Callback {
        private List<UiMessage> newList;

        @Override
        public int getOldListSize() {
            if (mDataList != null) {
                return mDataList.size();
            } else {
                return 0;
            }
        }

        @Override
        public int getNewListSize() {
            if (newList != null) {
                return newList.size();
            } else {
                return 0;
            }
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            int oldClientId = mDataList.get(oldItemPosition).getClientId();
            int newClientId = newList.get(newItemPosition).getClientId();
            // Use clientId (local DB row ID) for reliable identity comparison.
            // For messages not yet in DB (clientId = -1), fall back to messageId equality.
            if (oldClientId > 0 && newClientId > 0) {
                return oldClientId == newClientId;
            }
            String oldMsgId = mDataList.get(oldItemPosition).getMessageId();
            String newMsgId = newList.get(newItemPosition).getMessageId();
            return oldMsgId != null && oldMsgId.equals(newMsgId);
        }

        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            // Return non-null to suppress RecyclerView's default item-change animation,
            // which is the main cause of flickering on video progress / state updates.
            return Boolean.TRUE;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            UiMessage newItem = newList.get(newItemPosition);
            if (newItem.isChange()) {
                return false;
            }
            return true;
        }

        public void setNewList(List<UiMessage> newList) {
            this.newList = newList;
        }
    }
}
