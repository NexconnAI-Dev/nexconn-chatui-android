package ai.nexconn.chatui.channel.messagelist.provider;

import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.message.InformationNotificationMessage;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.widget.adapter.IViewProviderListener;
import ai.nexconn.chatui.widget.adapter.ViewHolder;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;

public class InformationNotificationMessageItemProvider
        extends BaseNotificationMessageItemProvider<InformationNotificationMessage> {

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View rootView =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.nc_item_information_notification_message, parent, false);
        return new ViewHolder(parent.getContext(), rootView);
    }

    @Override
    public boolean isItemViewType(UiMessage item) {
        return item.getMessage().getContent() instanceof InformationNotificationMessage;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof InformationNotificationMessage;
    }

    @Override
    protected void bindMessageContentViewHolder(
            ViewHolder holder,
            ViewHolder parentHolder,
            InformationNotificationMessage content,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        holder.setText(R.id.nc_msg, content.getMessage());
    }

    @Override
    public Spannable getSummarySpannable(Context context, InformationNotificationMessage data) {
        if (data != null && !TextUtils.isEmpty(data.getMessage())) {
            return new SpannableString(data.getMessage());
        }
        return null;
    }
}
