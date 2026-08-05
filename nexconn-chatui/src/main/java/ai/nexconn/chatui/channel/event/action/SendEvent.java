package ai.nexconn.chatui.channel.event.action;

import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.message.Message;
import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class SendEvent {
    @IntDef({SUCCESS, ATTACH, ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Event {
        // default implementation ignored
    }

    public static final int ATTACH = 0;
    public static final int SUCCESS = 1;
    public static final int ERROR = 2;

    private @Event int event;
    private Message message;
    private NCError code;

    public SendEvent(@Event int event, Message message) {
        this(event, message, null);
    }

    public SendEvent(int event, Message message, NCError code) {
        this.event = event;
        this.message = message;
        this.code = code;
    }

    public @Event int getEvent() {
        return event;
    }

    public Message getMessage() {
        return message;
    }

    public NCError getCode() {
        return code;
    }
}
