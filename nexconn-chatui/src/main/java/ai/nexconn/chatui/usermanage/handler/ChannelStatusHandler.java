package ai.nexconn.chatui.usermanage.handler;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.ChannelNoDisturbLevel;
import ai.nexconn.chat.channel.model.ChannelNoDisturbLevelSyncEvent;
import ai.nexconn.chat.channel.model.ChannelPinnedSyncEvent;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.ChannelHandler;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chatui.base.MultiDataHandler;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Channel status handler.
 *
 * <p>Note: Call {@link #stop()} to release resources when done.
 *
 * @since 5.12.2
 */
public class ChannelStatusHandler extends MultiDataHandler {

    public static final DataKey<ChannelNoDisturbLevel> KEY_GET_CONVERSATION_NOTIFICATION_STATUS =
            DataKey.obtain("KEY_GET_CONVERSATION_NOTIFICATION_STATUS", ChannelNoDisturbLevel.class);

    public static final DataKey<Boolean> KEY_GET_CONVERSATION_TOP_STATUS =
            DataKey.obtain("KEY_GET_CONVERSATION_TOP_STATUS", Boolean.class);
    @NonNull private final ChannelIdentifier channelIdentifier;

    private final String channelHandlerId = "ChannelStatusHandler_" + hashCode();

    public ChannelStatusHandler(@NonNull ChannelIdentifier channelIdentifier) {
        this.channelIdentifier = channelIdentifier;
        NCEngine.addChannelHandler(
                channelHandlerId,
                new ChannelHandler() {
                    @Override
                    public void onChannelPinnedSync(@NonNull ChannelPinnedSyncEvent event) {
                        if (channelIdentifier.equals(event.getChannelIdentifier())) {
                            notifyDataChange(KEY_GET_CONVERSATION_TOP_STATUS, event.isPinned());
                        }
                    }

                    @Override
                    public void onChannelNoDisturbLevelSync(
                            @NonNull ChannelNoDisturbLevelSyncEvent event) {
                        if (channelIdentifier.equals(event.getChannelIdentifier())) {
                            notifyDataChange(
                                    KEY_GET_CONVERSATION_NOTIFICATION_STATUS, event.getLevel());
                        }
                    }
                });
    }

    /** Gets the conversation notification status. */
    public void getConversationNotificationStatus() {
        List<ChannelIdentifier> identifiers = new ArrayList<>(1);
        identifiers.add(channelIdentifier);
        BaseChannel.getChannels(
                identifiers,
                new OperationHandler<List<BaseChannel>>() {
                    @Override
                    public void onResult(List<BaseChannel> channels, NCError error) {
                        if (error == null && channels != null && !channels.isEmpty()) {
                            notifyDataChange(
                                    KEY_GET_CONVERSATION_NOTIFICATION_STATUS,
                                    channels.get(0).getNoDisturbLevel());
                        } else if (error != null) {
                            notifyDataError(KEY_GET_CONVERSATION_NOTIFICATION_STATUS, error);
                        }
                    }
                });
    }

    /** Gets the conversation pinned (top) status. */
    public void getConversationTopStatus() {
        List<ChannelIdentifier> identifiers = new ArrayList<>(1);
        identifiers.add(channelIdentifier);
        BaseChannel.getChannels(
                identifiers,
                new OperationHandler<List<BaseChannel>>() {
                    @Override
                    public void onResult(List<BaseChannel> channels, NCError error) {
                        if (error == null && channels != null && !channels.isEmpty()) {
                            notifyDataChange(
                                    KEY_GET_CONVERSATION_TOP_STATUS, channels.get(0).isPinned());
                        } else if (error != null) {
                            notifyDataError(KEY_GET_CONVERSATION_TOP_STATUS, error);
                        }
                    }
                });
    }

    @Override
    public void stop() {
        super.stop();
        NCEngine.removeChannelHandler(channelHandlerId);
    }
}
