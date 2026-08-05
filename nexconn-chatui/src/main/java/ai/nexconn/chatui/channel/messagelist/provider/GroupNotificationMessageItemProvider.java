package ai.nexconn.chatui.channel.messagelist.provider;

import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.widget.adapter.IViewProviderListener;
import ai.nexconn.chatui.widget.adapter.ViewHolder;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;

public class GroupNotificationMessageItemProvider
        extends BaseNotificationMessageItemProvider<MessageContent> {

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(
                                R.layout.nc_item_group_information_notification_message,
                                parent,
                                false);
        return new ViewHolder(parent.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(
            ViewHolder holder,
            ViewHolder parentHolder,
            MessageContent messageContent,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        // Group events are delivered through GroupChannelHandler. Applications can register a
        // custom provider for their own group notification message type.
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return false;
    }

    @Override
    public Spannable getSummarySpannable(Context context, MessageContent content) {
        return new SpannableString(context.getString(R.string.nc_item_group_notification_summary));
    }
}
