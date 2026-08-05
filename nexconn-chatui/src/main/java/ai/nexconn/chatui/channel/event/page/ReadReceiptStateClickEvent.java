package ai.nexconn.chatui.channel.event.page;

import ai.nexconn.chatui.model.UiMessage;

/**
 * Click event for the read receipt state component in a message bubble.
 *
 * @since 5.30.0
 */
public class ReadReceiptStateClickEvent implements PageEvent {

    private UiMessage message;

    public ReadReceiptStateClickEvent(UiMessage message) {
        this.message = message;
    }

    public UiMessage getMessage() {
        return message;
    }
}
