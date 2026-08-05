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

public class DefaultMessageItemProvider
        extends BaseNotificationMessageItemProvider<MessageContent> {
    private static final String TAG = "UnknownMessageItemProvider";

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
        holder.setText(R.id.nc_msg, holder.getContext().getString(R.string.nc_message_unknown));
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return true;
    }

    @Override
    public Spannable getSummarySpannable(Context context, MessageContent messageContent) {
        if (context == null) {
            return new SpannableString("");
        }
        return new SpannableString(context.getResources().getString(R.string.nc_message_unknown));
    }
}
