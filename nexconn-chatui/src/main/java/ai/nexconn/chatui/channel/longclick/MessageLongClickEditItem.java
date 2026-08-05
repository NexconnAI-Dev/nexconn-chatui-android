package ai.nexconn.chatui.channel.longclick;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.feature.editmessage.EditMessageManager;
import ai.nexconn.chatui.model.UiMessage;
import android.content.Context;
import androidx.annotation.NonNull;

/** Edits a message. Only shown for editable messages. */
public class MessageLongClickEditItem implements MessageLongClickItem {

    @Override
    public String getTitle(@NonNull Context context) {
        return context.getString(R.string.nc_edit);
    }

    @Override
    public int getIconAttrResId() {
        return R.attr.nc_conversation_menu_item_edit_img;
    }

    @Override
    public boolean isEnabled(@NonNull UiMessage message) {
        return EditMessageManager.getInstance().canEditByLongClick(message);
    }

    @Override
    public boolean onAction(@NonNull Context context, @NonNull UiMessage message) {
        return EditMessageManager.getInstance().onMessageLongClickEdit(context, message);
    }
}
