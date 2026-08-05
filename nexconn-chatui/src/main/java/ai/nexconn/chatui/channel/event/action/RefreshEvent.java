package ai.nexconn.chatui.channel.event.action;

import ai.nexconn.chat.message.Message;
import java.util.List;

public class RefreshEvent {
    private Message message;

    /** Messages for batch update */
    private List<Message> messages;

    /** Whether the message content has been modified */
    private boolean isModifyMessageContent = false;

    public RefreshEvent(Message message) {
        this.message = message;
    }

    public RefreshEvent(List<Message> messages, boolean isModifyMessageContent) {
        if (messages != null && !messages.isEmpty()) {
            this.message = messages.get(0);
        }
        this.messages = messages;
        this.isModifyMessageContent = isModifyMessageContent;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public boolean isModifyMessageContent() {
        return isModifyMessageContent;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
