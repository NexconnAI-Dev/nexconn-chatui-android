package ai.nexconn.chatui.channel.event.action;

import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.message.Message;
import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class DownloadEvent {
    @IntDef({SUCCESS, PROGRESS, ERROR, CANCEL, PAUSE, START})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Event {
        // default implementation ignored
    }

    public static final int SUCCESS = 0;
    public static final int PROGRESS = 1;
    public static final int ERROR = 2;
    public static final int CANCEL = 3;
    public static final int PAUSE = 4;
    public static final int START = 5;

    private @Event int event;
    private Message message;
    private NCError code;
    private int progress;

    public DownloadEvent(@Event int event, Message message) {
        this(event, message, 0, null);
    }

    public DownloadEvent(@Event int event, Message message, int progress) {
        this(event, message, progress, null);
    }

    public DownloadEvent(@Event int event, Message message, NCError code) {
        this(event, message, 0, code);
    }

    public DownloadEvent(@Event int event, Message message, int progress, NCError code) {
        this.event = event;
        this.message = message;
        this.progress = progress;
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

    public int getProgress() {
        return progress;
    }
}
