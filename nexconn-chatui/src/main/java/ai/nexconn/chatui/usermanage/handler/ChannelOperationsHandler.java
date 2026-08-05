package ai.nexconn.chatui.usermanage.handler;

import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.ChannelNoDisturbLevel;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.ErrorHandler;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.params.PinParams;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.notification.MessageNotificationHelper;
import androidx.annotation.NonNull;

/**
 * Channel operations handler.
 *
 * <p>Note: Call {@link #stop()} to release resources when done.
 *
 * @since 5.12.2
 */
public class ChannelOperationsHandler extends MultiDataHandler {

    public static final DataKey<ChannelNoDisturbLevel> KEY_SET_CONVERSATION_NOTIFICATION_STATUS =
            DataKey.obtain("KEY_SET_CONVERSATION_NOTIFICATION_STATUS", ChannelNoDisturbLevel.class);

    public static final DataKey<Boolean> KEY_SET_CONVERSATION_TO_TOP =
            DataKey.obtain("KEY_SET_CONVERSATION_TO_TOP", Boolean.class);

    @NonNull private final ChannelIdentifier channelIdentifier;

    public ChannelOperationsHandler(@NonNull ChannelIdentifier channelIdentifier) {
        this.channelIdentifier = channelIdentifier;
    }

    /**
     * Sets the conversation notification status.
     *
     * @param noDisturbLevel the do-not-disturb level
     */
    public void setConversationNotificationStatus(ChannelNoDisturbLevel noDisturbLevel) {
        BaseChannel channel =
                new BaseChannel(
                        channelIdentifier.getChannelType(), channelIdentifier.getChannelId());
        channel.setNoDisturbLevel(
                noDisturbLevel,
                new ErrorHandler() {
                    @Override
                    public void onError(NCError error) {
                        if (error == null) {
                            // Sync the local notification cache immediately so incoming messages
                            // on this device are muted/unmuted right away.
                            MessageNotificationHelper.updateLevelMap(
                                    channelIdentifier, noDisturbLevel.getValue());
                            notifyDataChange(
                                    KEY_SET_CONVERSATION_NOTIFICATION_STATUS, noDisturbLevel);
                        } else {
                            notifyDataError(KEY_SET_CONVERSATION_NOTIFICATION_STATUS, error);
                        }
                    }
                });
    }

    /**
     * Sets the conversation pinned (top) status.
     *
     * @param isTop whether to pin the conversation to the top
     */
    public void setConversationToTop(boolean isTop) {
        if (isTop) {
            NCChatUI.createChannel(channelIdentifier)
                    .pin(
                            new PinParams(),
                            new OperationHandler<Boolean>() {
                                @Override
                                public void onResult(Boolean result, NCError error) {
                                    if (error == null) {
                                        notifyDataChange(KEY_SET_CONVERSATION_TO_TOP, result);
                                    } else {
                                        notifyDataError(KEY_SET_CONVERSATION_TO_TOP, error);
                                    }
                                }
                            });
        } else {
            NCChatUI.createChannel(channelIdentifier)
                    .unpin(
                            new OperationHandler<Boolean>() {
                                @Override
                                public void onResult(Boolean result, NCError error) {
                                    if (error == null) {
                                        notifyDataChange(KEY_SET_CONVERSATION_TO_TOP, result);
                                    } else {
                                        notifyDataError(KEY_SET_CONVERSATION_TO_TOP, error);
                                    }
                                }
                            });
        }
    }
}
