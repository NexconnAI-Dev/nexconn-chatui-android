package ai.nexconn.chatui.handler;

import ai.nexconn.chat.channel.model.ChannelIdentifier;

/**
 * Listener for channel-level events dispatched by {@link NCChatUI}.
 *
 * <p>All methods have default empty implementations; override only the callbacks you need.
 *
 * <p>Callback sources:
 *
 * <ul>
 *   <li>{@link NCChatUI#saveDraft} → {@link #onSaveDraft}
 *   <li>{@link NCChatUI#clearUnreadCount} → {@link #onClearedUnreadStatus}
 * </ul>
 */
public interface ChannelEventListener {

    /**
     * Called when a draft is saved for a channel.
     *
     * <p>Triggered by {@link NCChatUI#saveDraft}.
     *
     * @param channelIdentifier the channel for which the draft was saved
     * @param content the draft content
     */
    default void onSaveDraft(ChannelIdentifier channelIdentifier, String content) {}

    /**
     * Called when the unread status of a channel is cleared.
     *
     * <p>Triggered by {@link NCChatUI#clearUnreadCount}.
     *
     * @param channelIdentifier the channel whose unread status was cleared
     */
    default void onClearedUnreadStatus(ChannelIdentifier channelIdentifier) {}
}
