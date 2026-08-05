package ai.nexconn.chatui.channel.event.action;

import ai.nexconn.chat.message.Message;

public class InsertEvent {

    private Message message;

    public InsertEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
