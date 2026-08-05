package ai.nexconn.chatui.channellist.handler;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.handler.ChannelHandler;
import ai.nexconn.chat.handler.MessageHandler;
import ai.nexconn.chat.message.model.MessageDeletedEvent;
import ai.nexconn.chat.message.model.MessageReceivedEvent;
import ai.nexconn.chat.message.model.OfflineMessageSyncCompletedEvent;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.channel.event.action.DeleteEvent;
import ai.nexconn.chatui.channel.event.action.DownloadEvent;
import ai.nexconn.chatui.channel.event.action.InsertEvent;
import ai.nexconn.chatui.channel.event.action.MessageEventListener;
import ai.nexconn.chatui.channel.event.action.RefreshEvent;
import ai.nexconn.chatui.channel.event.action.SendEvent;
import ai.nexconn.chatui.channel.event.action.SendMediaEvent;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.handler.ChannelEventListener;

/**
 * Channel list event listener handler
 *
 * <p>Registers all event listeners that trigger channel list refresh, and notifies the ViewModel
 * via DataKey.
 *
 * <p>Monitored event types:
 *
 * <ul>
 *   <li>Channel events (draft, clear, unread, delete, status changes)
 *   <li>Message events (send, download, delete, recall, refresh, insert, clear)
 *   <li>Message reception (distinguishes online/offline batch mode)
 *   <li>Message recall
 *   <li>Cancel send
 *   <li>Channel status changes
 * </ul>
 *
 * @since 5.10.4
 */
public class ChannelListEventHandler extends MultiDataHandler {

    private final String mHandlerId = "ChannelListEventHandler_" + hashCode();
    private static final int DEFAULT_DELAY = 500;

    public static final DataKey<Long> KEY_REFRESH_NEEDED =
            DataKey.obtain("KEY_REFRESH_NEEDED", Long.class);

    @SuppressWarnings("unchecked")
    public static final DataKey<int[]> KEY_DELETED_MSG_CONVERSATION =
            DataKey.obtain("KEY_DELETED_MSG_CONVERSATION", int[].class);

    /** Delayed refresh interval during offline message batch reception */
    private final int batchOfflineDelay;

    /**
     * Current refresh delay; uses default for online messages, switches to batchOfflineDelay for
     * offline batch
     */
    private int currentRefreshDelay = DEFAULT_DELAY;

    // region Kit internal event listeners (still via IMCenter)

    private final ChannelEventListener conversationEventListener =
            new ChannelEventListener() {
                @Override
                public void onSaveDraft(ChannelIdentifier channelIdentifier, String content) {
                    requestRefresh();
                }

                @Override
                public void onClearedUnreadStatus(ChannelIdentifier channelIdentifier) {
                    requestRefresh();
                }
            };

    private final MessageEventListener messageEventListener =
            new MessageEventListener() {
                @Override
                public void onSendMessage(SendEvent event) {
                    if (event != null && event.getMessage() != null) {
                        requestRefresh();
                    }
                }

                @Override
                public void onSendMediaMessage(SendMediaEvent event) {
                    if (event != null
                            && event.getEvent() != SendMediaEvent.PROGRESS
                            && event.getMessage() != null) {
                        requestRefresh();
                    }
                }

                @Override
                public void onDownloadMessage(DownloadEvent event) {
                    if (event != null
                            && event.getMessage() != null
                            && event.getEvent() != DownloadEvent.PROGRESS) {
                        requestRefresh();
                    }
                }

                @Override
                public void onDeleteMessage(DeleteEvent event) {
                    if (event != null) {
                        notifyDataChange(KEY_DELETED_MSG_CONVERSATION, event.getMessageIds());
                    }
                }

                @Override
                public void onRefreshEvent(RefreshEvent event) {
                    if (event != null && event.getMessage() != null) {
                        requestRefresh();
                    }
                }

                @Override
                public void onInsertMessage(InsertEvent event) {
                    if (event != null) {
                        requestRefresh();
                    }
                }
            };

    private final Runnable cancelSendMediaListener = this::requestRefresh;

    private final String mChannelHandlerId = "ChannelListEventHandler_Channel_" + hashCode();

    private final ChannelHandler channelStatusHandler =
            new ChannelHandler() {
                @Override
                public void onChannelPinnedSync(
                        ai.nexconn.chat.channel.model.ChannelPinnedSyncEvent event) {
                    requestRefresh();
                }

                @Override
                public void onChannelNoDisturbLevelSync(
                        ai.nexconn.chat.channel.model.ChannelNoDisturbLevelSyncEvent event) {
                    requestRefresh();
                }

                @Override
                public void onChannelStatusSyncCompleted(
                        ai.nexconn.chat.channel.model.ChannelStatusSyncCompletedEvent event) {
                    requestRefresh();
                }
            };

    // endregion

    // region SDK-level listeners (via NCEngine)

    private final MessageHandler ncMessageHandler =
            new MessageHandler() {
                @Override
                public void onMessageReceived(MessageReceivedEvent event) {
                    boolean offline = Boolean.TRUE.equals(event.getOffline());
                    boolean hasPackage = Boolean.TRUE.equals(event.getHasPackage());
                    int left = event.getLeft() != null ? event.getLeft() : 0;
                    boolean wasBatchMode = isInBatchMode();
                    boolean isBatchMode = offline && (hasPackage || left > 0);
                    currentRefreshDelay = isBatchMode ? batchOfflineDelay : DEFAULT_DELAY;

                    if (wasBatchMode && !isBatchMode) {
                        requestRefresh();
                    } else {
                        notifyDataChange(KEY_REFRESH_NEEDED, (long) currentRefreshDelay);
                    }
                }

                @Override
                public void onOfflineMessageSyncCompleted(OfflineMessageSyncCompletedEvent event) {
                    currentRefreshDelay = DEFAULT_DELAY;
                    requestRefresh();
                }

                @Override
                public void onMessageDeleted(MessageDeletedEvent event) {
                    requestRefresh();
                }
            };

    // endregion

    public ChannelListEventHandler() {
        batchOfflineDelay = NCChatUIConfig.channelListConfig().getDelayRefreshTime();
        registerListeners();
    }

    /** Whether currently in batch message reception mode (online or offline) */
    public boolean isInBatchMode() {
        return currentRefreshDelay == batchOfflineDelay;
    }

    /** Whether currently in offline message batch reception mode */
    public boolean isInBatchOfflineMode() {
        return isInBatchMode();
    }

    private void requestRefresh() {
        notifyDataChange(KEY_REFRESH_NEEDED, 0L);
    }

    private void registerListeners() {
        NCChatUI.addChannelEventListener(conversationEventListener);
        NCChatUI.addCancelSendMediaMessageListener(cancelSendMediaListener);
        NCChatUI.addMessageEventListener(messageEventListener);

        NCEngine.addMessageHandler(mHandlerId, ncMessageHandler);
        NCEngine.addChannelHandler(mChannelHandlerId, channelStatusHandler);
    }

    private void unregisterListeners() {
        NCChatUI.removeChannelEventListener(conversationEventListener);
        NCChatUI.removeCancelSendMediaMessageListener(cancelSendMediaListener);
        NCChatUI.removeMessageEventListener(messageEventListener);

        NCEngine.removeMessageHandler(mHandlerId);
        NCEngine.removeChannelHandler(mChannelHandlerId);
    }

    @Override
    public void stop() {
        super.stop();
        unregisterListeners();
    }
}
