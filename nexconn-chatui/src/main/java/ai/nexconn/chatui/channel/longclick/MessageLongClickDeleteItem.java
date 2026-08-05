package ai.nexconn.chatui.channel.longclick;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.message.HDVoiceMessage;
import ai.nexconn.chat.message.MediaMessageContent;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.model.MessageDirection;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.feature.resend.ResendManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.manager.AudioPlayManager;
import ai.nexconn.chatui.manager.SendMediaManager;
import ai.nexconn.chatui.message.InformationNotificationMessage;
import ai.nexconn.chatui.model.State;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.common.ToastUtils;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.system.NetUtils;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.NonNull;
import java.util.Collections;
import java.util.List;

/**
 * Delete menu item with direction-aware behavior:
 *
 * <ul>
 *   <li>Received messages → delete for this user only (local side)
 *   <li>Own sent messages within recall interval → recall (delete for all)
 *   <li>Own sent messages past recall interval → delete for this user only (local side)
 * </ul>
 */
public class MessageLongClickDeleteItem implements MessageLongClickItem {

    private static final String TAG = MessageLongClickDeleteItem.class.getSimpleName();

    @Override
    public String getTitle(@NonNull android.content.Context context) {
        return context.getString(R.string.nc_delete_for_me);
    }

    @Override
    public String getTitle(@NonNull android.content.Context context, @NonNull UiMessage uiMessage) {
        Message message = uiMessage.getMessage();
        if (message == null) {
            return getTitle(context);
        }

        if (shouldDeleteForEveryone(uiMessage)) {
            return context.getString(R.string.nc_recall);
        }
        return context.getString(R.string.nc_delete_for_me);
    }

    @Override
    public int getIconAttrResId() {
        return R.attr.nc_conversation_menu_item_delete_img;
    }

    @Override
    public boolean isEnabled(@NonNull UiMessage uiMessage) {
        return uiMessage.getMessage() != null;
    }

    @Override
    public boolean onAction(
            @NonNull android.content.Context context, @NonNull UiMessage uiMessage) {
        Message message = uiMessage.getMessage();
        if (message == null) return false;

        if (message.getContent() instanceof HDVoiceMessage) {
            android.net.Uri playingUri = AudioPlayManager.getInstance().getPlayingUri();
            String localPath = ((HDVoiceMessage) message.getContent()).getLocalPath();
            if (playingUri != null && playingUri.toString().equals(localPath)) {
                AudioPlayManager.getInstance().stopPlay();
            }
        }

        cancelMediaTransferIfNeeded(uiMessage);

        if (message.getDirection() != MessageDirection.SEND) {
            // Received message: delete for the current user.
            deleteForMe(context, uiMessage);
        } else {
            if (isInFlight(uiMessage)) {
                // Still in-flight: local cleanup only.
                deleteLocalMessage(
                        uiMessage,
                        Collections.singletonList(uiMessage.getMessage()),
                        uiMessage.getMessage().getChannelIdentifier());
            } else if (shouldDeleteForEveryone(uiMessage)) {
                // Within recall window: delete for everyone.
                recallMessage(context, uiMessage);
            } else {
                // Recall expired or not supported by this message: delete for the current user.
                deleteForMe(context, uiMessage);
            }
        }
        return true;
    }

    /** Deletes the message for the current user only (other party still sees it). */
    private void deleteForMe(android.content.Context context, UiMessage uiMessage) {
        List<Message> messages = Collections.singletonList(uiMessage.getMessage());
        ChannelIdentifier identifier = uiMessage.getMessage().getChannelIdentifier();

        if (!NCChatUIConfig.channelConfig().isNeedDeleteRemoteMessage()) {
            deleteLocalMessage(uiMessage, messages, identifier);
            return;
        }

        String errorTxt = context.getString(R.string.nc_dialog_item_message_delete_failed_msg);
        if (!NetUtils.isNetWorkAvailable(context)) {
            ToastUtils.show(context, errorTxt, Toast.LENGTH_SHORT);
            return;
        }

        if (uiMessage.getChannelType() == ChannelType.OPEN) {
            deleteLocalMessage(uiMessage, messages, identifier);
            return;
        }

        NCChatUI.deleteMessages(
                identifier,
                messages,
                error -> {
                    if (error == null) {
                        ResendManager.getInstance()
                                .removeResendMessage(uiMessage.getMessage().getClientId());
                    } else {
                        RLog.e(TAG, "deleteForMe fail: " + error);
                        ToastUtils.show(context, errorTxt, Toast.LENGTH_SHORT);
                    }
                });
    }

    /** Recalls the message (delete for all endpoints). */
    private void recallMessage(android.content.Context context, UiMessage uiMessage) {
        String errorTxt = context.getString(R.string.nc_recall_failed_for_network_unavailable);
        if (!NetUtils.isNetWorkAvailable(context)) {
            ToastUtils.show(context, errorTxt, Toast.LENGTH_SHORT);
            return;
        }

        NCChatUI.deleteMessageForAll(
                uiMessage.getMessage().getChannelIdentifier(),
                uiMessage.getMessage(),
                (result, error) -> {
                    if (error != null) {
                        RLog.e(TAG, "recallMessage fail: " + error);
                        ToastUtils.show(context, errorTxt, Toast.LENGTH_SHORT);
                    }
                });
    }

    private void cancelMediaTransferIfNeeded(@NonNull UiMessage uiMessage) {
        Message message = uiMessage.getMessage();
        if (message == null || !(message.getContent() instanceof MediaMessageContent)) {
            return;
        }
        if (message.getDirection() != MessageDirection.SEND) {
            message.cancelDownloadingMedia(null);
            return;
        }
        if (!isInFlight(uiMessage)) {
            return;
        }
        SendMediaManager.getInstance()
                .cancelSendingMedia(
                        message.getChannelIdentifier().getChannelType(),
                        message.getChannelIdentifier().getChannelId(),
                        message.getClientId());
        NCChatUI.cancelSendMediaMessage(
                message,
                error -> {
                    if (error != null) {
                        RLog.w(TAG, "cancelSendMediaMessage before delete fail: " + error);
                    }
                });
    }

    private void deleteLocalMessage(
            UiMessage uiMessage, List<Message> messages, ChannelIdentifier identifier) {
        NCChatUI.deleteLocalMessages(
                identifier,
                messages,
                error -> {
                    if (error == null) {
                        ResendManager.getInstance()
                                .removeResendMessage(uiMessage.getMessage().getClientId());
                    }
                });
    }

    private boolean isInFlight(@NonNull UiMessage uiMessage) {
        return (uiMessage.getState() == State.ERROR
                        || uiMessage.getState() == State.PROGRESS
                        || uiMessage.getState() == State.CANCEL)
                && TextUtils.isEmpty(uiMessage.getMessageId());
    }

    private boolean shouldDeleteForEveryone(@NonNull UiMessage uiMessage) {
        Message message = uiMessage.getMessage();
        if (message == null) {
            return false;
        }
        if (message.getDirection() != MessageDirection.SEND) {
            return false;
        }
        if (message.getChannelIdentifier() != null
                && message.getChannelIdentifier().getChannelType() == ChannelType.SYSTEM) {
            return false;
        }
        if (message.getContent() instanceof InformationNotificationMessage) {
            return false;
        }
        if (!NCChatUIConfig.channelConfig().NC_enable_recall_message) {
            return false;
        }
        if (isInFlight(uiMessage)) {
            return false;
        }
        int recallInterval = NCChatUIConfig.channelConfig().NC_message_recall_interval;
        if (recallInterval <= 0) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - message.getSentTime();
        return elapsed <= recallInterval * 1000L;
    }
}
