package ai.nexconn.chatui.channellist.handler;

import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.handler.RefreshReferenceMessageHandler;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.ReferenceMessage;
import ai.nexconn.chat.message.model.ReferenceMessageResult;
import ai.nexconn.chat.message.model.ReferenceMessageStatus;
import ai.nexconn.chat.params.RefreshReferenceMessageParams;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.channellist.model.BaseUiChannel;
import ai.nexconn.chatui.utils.log.RLog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Channel list reference message handler
 *
 * <p>Handles refreshing reference message status for conversation list items.
 *
 * @since 5.10.4
 */
public class ChannelListReferenceMessageHandler extends MultiDataHandler {

    private static final String TAG = "ChannelListReferenceMessageHandler";

    public static final DataKey<Map<ChannelIdentifier, ai.nexconn.chat.message.Message>>
            KEY_REFERENCE_MESSAGE_UPDATE =
                    DataKey.obtain(
                            "KEY_REFERENCE_MESSAGE_UPDATE",
                            (Class<Map<ChannelIdentifier, ai.nexconn.chat.message.Message>>)
                                    (Class<?>) Map.class);

    /**
     * Collects reference messages in the channel list that need status refresh and initiates the
     * refresh
     *
     * @param uiConversationList the current channel list
     */
    public void collectAndRefreshReferenceMessages(List<BaseUiChannel> uiConversationList) {
        if (uiConversationList == null || uiConversationList.isEmpty()) {
            return;
        }

        // Collect conversations with reference messages that need refresh
        List<RefreshTask> tasks = new ArrayList<>();
        for (BaseUiChannel uiConversation : uiConversationList) {
            if (uiConversation.mCore == null) {
                continue;
            }
            ai.nexconn.chat.message.Message latestMessage = uiConversation.mCore.getLatestMessage();
            if (latestMessage == null) {
                continue;
            }
            MessageContent content = latestMessage.getContent();
            if (!(content instanceof ReferenceMessage)) {
                continue;
            }
            ReferenceMessage refMsg = (ReferenceMessage) content;
            ReferenceMessageStatus status = refMsg.getReferMsgStatus();
            // Only refresh if status is MODIFIED or DEFAULT
            if (status == ReferenceMessageStatus.MODIFIED
                    || status == ReferenceMessageStatus.DEFAULT) {
                ChannelIdentifier chId =
                        new ChannelIdentifier(
                                uiConversation.mCore.getChannelType(),
                                uiConversation.mCore.getChannelId());
                tasks.add(new RefreshTask(chId, latestMessage.getMessageId()));
            }
        }

        if (tasks.isEmpty()) {
            return;
        }

        RLog.d(TAG, "Refresh reference messages for " + tasks.size() + " conversations");

        // Refresh reference messages for each conversation
        for (RefreshTask task : tasks) {
            refreshReferenceMessage(task);
        }
    }

    private void refreshReferenceMessage(RefreshTask task) {
        List<String> messageIds = new ArrayList<>();
        messageIds.add(task.messageId);
        RefreshReferenceMessageParams params =
                new RefreshReferenceMessageParams(task.channelId, messageIds);

        BaseChannel.refreshReferenceMessage(
                params,
                new RefreshReferenceMessageHandler() {
                    @Override
                    public void onLocalMessages(List<ReferenceMessageResult> results) {
                        if (results == null || results.isEmpty()) {
                            return;
                        }
                        // Notify UI update with refreshed messages
                        Map<ChannelIdentifier, ai.nexconn.chat.message.Message> updateMap =
                                new HashMap<>();
                        for (ReferenceMessageResult result : results) {
                            if (result.getMessage() != null) {
                                updateMap.put(task.channelId, result.getMessage());
                            }
                        }
                        if (!updateMap.isEmpty()) {
                            notifyDataChange(KEY_REFERENCE_MESSAGE_UPDATE, updateMap);
                        }
                    }

                    @Override
                    public void onRemoteMessages(List<ReferenceMessageResult> results) {
                        if (results == null || results.isEmpty()) {
                            return;
                        }
                        // Notify UI update with refreshed messages
                        Map<ChannelIdentifier, ai.nexconn.chat.message.Message> updateMap =
                                new HashMap<>();
                        for (ReferenceMessageResult result : results) {
                            if (result.getMessage() != null) {
                                updateMap.put(task.channelId, result.getMessage());
                            }
                        }
                        if (!updateMap.isEmpty()) {
                            notifyDataChange(KEY_REFERENCE_MESSAGE_UPDATE, updateMap);
                        }
                    }

                    @Override
                    public void onError(ai.nexconn.chat.error.NCError error) {
                        RLog.e(TAG, "refreshReferenceMessage error: " + error);
                    }
                });
    }

    private static class RefreshTask {
        final ChannelIdentifier channelId;
        final String messageId;

        RefreshTask(ChannelIdentifier channelId, String messageId) {
            this.channelId = channelId;
            this.messageId = messageId;
        }
    }
}
