package ai.nexconn.chatui.channel.event.action;

import ai.nexconn.chat.message.Message;

/**
 * Fired when a message in the UI should be replaced in place by a newly-created copy.
 *
 * <p>Used by the resend flow: after network recovery the SDK re-sends a queued message as a brand
 * new message with a new {@code clientId}. Without this event the new copy is appended to the end of
 * the list while the original is removed, so the resent message jumps to the bottom and can sink
 * under an open keyboard. Listeners should instead move the new copy into the original's slot,
 * keeping the list order and scroll position stable.
 */
public class ReplaceEvent {
    private final int mOriginalClientId;
    private final Message mNewMessage;

    public ReplaceEvent(int originalClientId, Message newMessage) {
        this.mOriginalClientId = originalClientId;
        this.mNewMessage = newMessage;
    }

    /** clientId of the original message that is being replaced. */
    public int getOriginalClientId() {
        return mOriginalClientId;
    }

    /** The newly-created message that should take the original's list position. */
    public Message getNewMessage() {
        return mNewMessage;
    }
}
