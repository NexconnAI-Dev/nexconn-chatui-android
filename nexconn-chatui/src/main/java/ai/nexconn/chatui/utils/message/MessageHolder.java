package ai.nexconn.chatui.utils.message;

import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import androidx.annotation.Nullable;

/**
 * Temporary holder for passing nexconn {@link Message} and {@link MessageContent} objects between
 * Activities, since these types are not {@link android.os.Parcelable}.
 *
 * <p>Usage: call {@link #holdMessage} before starting the Activity, then call {@link #takeMessage}
 * in the target Activity's {@code onCreate}.
 */
public final class MessageHolder {
    private static Message sMessage;
    private static MessageContent sMessageContent;

    private MessageHolder() {}

    public static void holdMessage(@Nullable Message message) {
        sMessage = message;
    }

    @Nullable
    public static Message takeMessage() {
        Message m = sMessage;
        sMessage = null;
        return m;
    }

    public static void holdContent(@Nullable MessageContent content) {
        sMessageContent = content;
    }

    @Nullable
    public static MessageContent takeContent() {
        MessageContent c = sMessageContent;
        sMessageContent = null;
        return c;
    }
}
