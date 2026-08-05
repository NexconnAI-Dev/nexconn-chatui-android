package ai.nexconn.chatui.channel.longclick;

import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.ReferenceMessage;
import ai.nexconn.chat.message.StreamMessage;
import ai.nexconn.chat.message.TextMessage;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.message.StreamMsgUtil;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;
import androidx.annotation.NonNull;

/** Copies message text content. Supports TextMessage, ReferenceMessage, and StreamMessage. */
public class MessageLongClickCopyItem implements MessageLongClickItem {

    private static final String TAG = MessageLongClickCopyItem.class.getSimpleName();

    @Override
    public String getTitle(@NonNull Context context) {
        return context.getString(R.string.nc_copy);
    }

    @Override
    public int getIconAttrResId() {
        return R.attr.nc_conversation_menu_item_copy_img;
    }

    @Override
    public boolean isEnabled(@NonNull UiMessage uiMessage) {
        Message message = uiMessage.getMessage();
        if (message == null || message.getContent() == null) return false;
        return (message.getContent() instanceof TextMessage
                        || message.getContent() instanceof ReferenceMessage
                        || message.getContent() instanceof StreamMessage)
                && !message.getContent().isDestruct();
    }

    @Override
    public boolean onAction(@NonNull Context context, @NonNull UiMessage uiMessage) {
        Message message = uiMessage.getMessage();
        if (message == null || message.getContent() == null) return false;
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return false;

        try {
            if (message.getContent() instanceof TextMessage) {
                clipboard.setPrimaryClip(
                        ClipData.newPlainText(
                                null, ((TextMessage) message.getContent()).getText()));
            } else if (message.getContent() instanceof ReferenceMessage) {
                clipboard.setPrimaryClip(
                        ClipData.newPlainText(
                                null, ((ReferenceMessage) message.getContent()).getContent()));
            } else if (message.getContent() instanceof StreamMessage) {
                StreamMessage streamMsg = (StreamMessage) message.getContent();
                Pair<String, Boolean> summary = StreamMsgUtil.getStreamMessageSummary(message);
                boolean isShowSummary = !streamMsg.getSync() && !TextUtils.isEmpty(summary.first);
                String content =
                        StreamMsgUtil.limitContentLength(
                                isShowSummary ? summary.first : streamMsg.getContent());
                clipboard.setPrimaryClip(ClipData.newPlainText(null, content));
            }
        } catch (Exception e) {
            RLog.e(TAG, "onAction copy failed", e);
        }
        return true;
    }
}
