package ai.nexconn.chatui.channel.event.page;

import ai.nexconn.chat.message.Message;
import ai.nexconn.chatui.channel.messagelist.status.MessageProcessor;
import java.util.List;

public class ShowLoadMessageDialogEvent implements PageEvent {
    private final MessageProcessor.GetMessageCallback callback;
    private final List<Message> list;

    public ShowLoadMessageDialogEvent(
            MessageProcessor.GetMessageCallback callback, List<Message> list) {
        this.callback = callback;
        this.list = list;
    }

    public MessageProcessor.GetMessageCallback getCallback() {
        return callback;
    }

    public List<Message> getList() {
        return list;
    }
}
