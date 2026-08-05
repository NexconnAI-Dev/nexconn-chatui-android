package ai.nexconn.chatui.channel.messagelist.provider;

import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.message.HistoryDividerMessage;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.widget.adapter.IViewProviderListener;
import ai.nexconn.chatui.widget.adapter.ViewHolder;
import android.content.Context;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;

public class HistoryDivMessageItemProvider
        extends BaseNotificationMessageItemProvider<HistoryDividerMessage> {

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View rootView =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.nc_item_new_message_divider, parent, false);
        return new ViewHolder(parent.getContext(), rootView);
    }

    @Override
    protected void bindMessageContentViewHolder(
            ViewHolder holder,
            ViewHolder parentHolder,
            HistoryDividerMessage msg,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        holder.setText(R.id.tv_divider_message, msg.getContent());
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof HistoryDividerMessage;
    }

    @Override
    public Spannable getSummarySpannable(
            Context context, HistoryDividerMessage historyDividerMessage) {
        return null;
    }
}
