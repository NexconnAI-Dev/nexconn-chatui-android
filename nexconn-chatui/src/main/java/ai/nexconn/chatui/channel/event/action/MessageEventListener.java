package ai.nexconn.chatui.channel.event.action;

import ai.nexconn.chat.channel.model.ChannelIdentifier;

public interface MessageEventListener {

    default void onSendMessage(SendEvent event) {}

    default void onSendMediaMessage(SendMediaEvent event) {}

    default void onDeleteMessage(DeleteEvent event) {}

    /**
     * Notifies that a message should be replaced in place by a newly-created copy (e.g. a resent
     * message re-created with a new clientId after network recovery). Listeners should move the new
     * message into the original's list position instead of appending it, keeping order stable.
     */
    default void onReplaceMessage(ReplaceEvent event) {}

    default void onRefreshEvent(RefreshEvent event) {}

    default void onInsertMessage(InsertEvent event) {}

    default void onDownloadMessage(DownloadEvent event) {}

    /**
     * Notifies that all messages in a given channel have been cleared (e.g. via {@code
     * deleteMessagesForMeByTimestamp} from a settings page). Listeners bound to that channel should
     * drop their cached messages and reload the list.
     */
    default void onChannelMessagesCleared(ChannelIdentifier identifier) {}
}
