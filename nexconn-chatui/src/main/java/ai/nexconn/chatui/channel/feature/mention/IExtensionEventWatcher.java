package ai.nexconn.chatui.channel.feature.mention;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.Message;
import android.content.Context;
import android.widget.EditText;

public interface IExtensionEventWatcher {
    /**
     * Callback when input field text changes.
     *
     * @param context context
     * @param type channel type
     * @param targetId target ID
     * @param cursorPos cursor position
     * @param count text change count
     * @param text text content
     */
    void onTextChanged(
            Context context,
            ChannelType type,
            String targetId,
            int cursorPos,
            int count,
            String text);

    /**
     * Pre-processing when the Extension module's send button is clicked. Other modules can set
     * additional information to Extension through this callback.
     *
     * @param message the initial message built when the Extension module's send button is clicked.
     *     Other modules can modify the message configuration.
     */
    void onSendToggleClick(Message message);

    void onDeleteClick(ChannelType type, String targetId, EditText editText, int cursorPos);

    void onDestroy(ChannelType type, String targetId);
}
