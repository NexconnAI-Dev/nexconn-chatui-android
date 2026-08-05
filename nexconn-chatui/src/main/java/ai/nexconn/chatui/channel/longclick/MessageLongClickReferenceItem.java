package ai.nexconn.chatui.channel.longclick;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.FileMessage;
import ai.nexconn.chat.message.ImageMessage;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.ReferenceMessage;
import ai.nexconn.chat.message.StreamMessage;
import ai.nexconn.chat.message.TextMessage;
import ai.nexconn.chat.message.model.SentStatus;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.feature.reference.ReferenceManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.message.StreamMsgUtil;
import android.content.Context;
import android.util.Pair;
import androidx.annotation.NonNull;

/**
 * Quotes and replies to a message. Valid for successfully sent messages in non-SYSTEM channels that
 * are not burn-after-reading.
 */
public class MessageLongClickReferenceItem implements MessageLongClickItem {

    @Override
    public String getTitle(@NonNull Context context) {
        return context.getString(R.string.nc_reference);
    }

    @Override
    public int getIconAttrResId() {
        return R.attr.nc_conversation_menu_item_reference_img;
    }

    @Override
    public boolean isEnabled(@NonNull UiMessage uiMessage) {
        Message message = uiMessage.getMessage();
        if (message == null || message.getContent() == null) return false;

        SentStatus sentStatus = message.getSentStatus();
        boolean isSuccess =
                sentStatus != SentStatus.CANCELED
                        && sentStatus != SentStatus.FAILED
                        && sentStatus != SentStatus.SENDING;

        ChannelType channelType = message.getChannelIdentifier().getChannelType();
        boolean forbidConversationType = channelType == ChannelType.SYSTEM;

        boolean isFireMsg = message.getContent().isDestruct();
        boolean isEnableReferenceMsg = NCChatUIConfig.featureConfig().isReferenceEnable();
        boolean isApplicableType =
                (message.getContent() instanceof TextMessage)
                        || (message.getContent() instanceof ImageMessage)
                        || (message.getContent() instanceof FileMessage)
                        || (message.getContent() instanceof ReferenceMessage)
                        || isEnableStreamMsg(message);

        return isSuccess
                && isEnableReferenceMsg
                && isApplicableType
                && !forbidConversationType
                && !isFireMsg;
    }

    @Override
    public boolean onAction(@NonNull Context context, @NonNull UiMessage message) {
        return ReferenceManager.getInstance().showReferenceViewInEditMode(context, message);
    }

    private boolean isEnableStreamMsg(Message message) {
        if (!(message.getContent() instanceof StreamMessage)) return false;
        StreamMessage streamMessage = (StreamMessage) message.getContent();
        Pair<String, Boolean> summary = StreamMsgUtil.getStreamMessageSummary(message);
        boolean isSuccess = streamMessage.getSync() || summary.second;
        if (!streamMessage.getSync()) {
            streamMessage.setContent(
                    StreamMsgUtil.getStreamMessageShowContent(streamMessage, summary));
        }
        return isSuccess;
    }
}
