package ai.nexconn.chatui.channel.longclick;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.ChannelViewModel;
import ai.nexconn.chatui.channel.messagelist.provider.MessageClickType;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.UiMessage;
import android.content.Context;
import androidx.annotation.NonNull;

/** Multi-select messages (enters edit mode). */
public class MessageLongClickMultiSelectItem implements MessageLongClickItem {

    private final ChannelViewModel mViewModel;

    public MessageLongClickMultiSelectItem(@NonNull ChannelViewModel viewModel) {
        mViewModel = viewModel;
    }

    @Override
    public String getTitle(@NonNull Context context) {
        return context.getString(R.string.nc_multi_select);
    }

    @Override
    public int getIconAttrResId() {
        return R.attr.nc_conversation_menu_item_multiple_img;
    }

    @Override
    public boolean isEnabled(@NonNull UiMessage message) {
        return NCChatUIConfig.channelConfig().isShowMoreClickAction()
                && message.getChannelType() != ChannelType.SYSTEM;
    }

    @Override
    public boolean onAction(@NonNull Context context, @NonNull UiMessage message) {
        mViewModel.enterEditState();
        mViewModel.onViewClick(MessageClickType.EDIT_CLICK, message);
        return true;
    }
}
