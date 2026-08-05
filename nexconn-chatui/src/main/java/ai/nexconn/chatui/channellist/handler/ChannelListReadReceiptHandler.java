package ai.nexconn.chatui.channellist.handler;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.ChannelUnreadStatusSyncEvent;
import ai.nexconn.chat.handler.ChannelHandler;
import ai.nexconn.chat.message.model.MessageDirection;
import ai.nexconn.chat.message.model.MessageIdentifier;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.channellist.model.BaseUiChannel;
import ai.nexconn.chatui.handler.EditMessageHandler;
import ai.nexconn.chatui.handler.ReadReceiptV5Handler;
import ai.nexconn.chatui.utils.log.RLog;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Channel list read receipt handler
 *
 * <p>Handles V5 read receipts, multi-device read status synchronization, and message editing.
 *
 * @since 5.10.4
 */
public class ChannelListReadReceiptHandler extends MultiDataHandler {

    private static final String TAG = "ChannelListReadReceiptHandler";

    public static final DataKey<ChannelIdentifier> KEY_SYNC_READ_STATUS =
            DataKey.obtain("KEY_SYNC_READ_STATUS", ChannelIdentifier.class);

    @SuppressWarnings("unchecked")
    public static final DataKey<HashMap<String, ai.nexconn.chat.message.model.ReadReceiptInfo>>
            KEY_READ_RECEIPT_V5_UPDATE =
                    DataKey.obtain(
                            "KEY_READ_RECEIPT_V5_UPDATE",
                            (Class<HashMap<String, ai.nexconn.chat.message.model.ReadReceiptInfo>>)
                                    (Class<?>) HashMap.class);

    public static final DataKey<Boolean> KEY_MESSAGE_MODIFIED =
            DataKey.obtain("KEY_MESSAGE_MODIFIED", Boolean.class);

    private final String mChannelHandlerId = "ChannelListReadReceiptHandler_Channel_" + hashCode();

    private final EditMessageHandler editMessageHandler;
    private final ReadReceiptV5Handler readReceiptV5Handler;

    private final ChannelHandler syncReadStatusHandler =
            new ChannelHandler() {
                @Override
                public void onChannelUnreadStatusSync(ChannelUnreadStatusSyncEvent event) {
                    if (event == null || event.getChannelIdentifier() == null) return;
                    notifyDataChange(KEY_SYNC_READ_STATUS, event.getChannelIdentifier());
                }
            };

    public ChannelListReadReceiptHandler() {
        editMessageHandler = new EditMessageHandler();
        editMessageHandler.addDataChangeListener(
                EditMessageHandler.KEY_ON_MESSAGE_MODIFIED,
                messages -> {
                    if (messages != null && !messages.isEmpty()) {
                        notifyDataChange(KEY_MESSAGE_MODIFIED, Boolean.TRUE);
                    }
                });

        readReceiptV5Handler = new ReadReceiptV5Handler();
        readReceiptV5Handler.addDataChangeListener(
                ReadReceiptV5Handler.KEY_MESSAGE_READ_RECEIPT_V5_LISTENER,
                data ->
                        notifyDataChange(
                                KEY_READ_RECEIPT_V5_UPDATE,
                                ReadReceiptV5Handler.toNcReceiptMap(data)));
        readReceiptV5Handler.addDataChangeListener(
                ReadReceiptV5Handler.KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5_BY_IDENTIFIER,
                data ->
                        notifyDataChange(
                                KEY_READ_RECEIPT_V5_UPDATE,
                                ReadReceiptV5Handler.toNcReceiptMap(data)));

        NCEngine.addChannelHandler(mChannelHandlerId, syncReadStatusHandler);
    }

    /**
     * Collects messages in the channel list that require V5 read receipt queries and initiates the
     * query
     *
     * @param uiConversationList the current channel list
     */
    public void collectAndQueryReadReceiptInfo(List<BaseUiChannel> uiConversationList) {
        String currentUserId = NCEngine.getCurrentUserId();
        if (TextUtils.isEmpty(currentUserId) || uiConversationList.isEmpty()) {
            return;
        }
        List<MessageIdentifier> messageIdentifiers = new ArrayList<>();
        for (BaseUiChannel uiConversation : uiConversationList) {
            if (uiConversation.mCore == null
                    || ChannelType.DIRECT != uiConversation.mCore.getChannelType()) {
                continue;
            }
            ai.nexconn.chat.message.Message latestMessage = uiConversation.mCore.getLatestMessage();
            if (latestMessage == null) {
                continue;
            }
            if (currentUserId.equals(latestMessage.getSenderUserId())
                    && latestMessage.getNeedReceipt()
                    && MessageDirection.SEND.equals(latestMessage.getDirection())
                    && !TextUtils.isEmpty(latestMessage.getMessageId())) {
                ChannelIdentifier chId =
                        new ChannelIdentifier(
                                ChannelType.DIRECT, uiConversation.mCore.getChannelId());
                MessageIdentifier msgId = new MessageIdentifier(chId, latestMessage.getMessageId());
                messageIdentifiers.add(msgId);
            }
        }
        if (!messageIdentifiers.isEmpty()) {
            RLog.d(TAG, "Query read receipt info for conversation: " + messageIdentifiers);
            readReceiptV5Handler.getMessageReadReceiptInfoByNcIdentifiers(messageIdentifiers);
        }
    }

    /** Checks whether two ChannelIdentifiers match */
    public static boolean isIdentifierMatched(ChannelIdentifier id1, ChannelIdentifier id2) {
        if (id1 == null || id2 == null) {
            return false;
        }
        return id1.getChannelType() == id2.getChannelType()
                && Objects.equals(id1.getChannelId(), id2.getChannelId());
    }

    @Override
    public void stop() {
        super.stop();
        editMessageHandler.stop();
        readReceiptV5Handler.stop();
        NCEngine.removeChannelHandler(mChannelHandlerId);
    }
}
