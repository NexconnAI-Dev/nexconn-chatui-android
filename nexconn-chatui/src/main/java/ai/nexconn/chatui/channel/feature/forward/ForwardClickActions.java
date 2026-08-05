package ai.nexconn.chatui.channel.feature.forward;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.ChannelFragment;
import ai.nexconn.chatui.channel.ChannelViewModel;
import ai.nexconn.chatui.channel.extension.component.moreaction.IClickActions;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.route.RouteUtils;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;
import java.util.List;

public class ForwardClickActions implements IClickActions {
    private static final String TAG = ForwardClickActions.class.getSimpleName();

    @Override
    public Drawable obtainDrawable(Context context) {
        return context.getResources()
                .getDrawable(
                        ChatUIThemeManager.getAttrResId(
                                context, R.attr.nc_conversation_menu_item_forward_img));
    }

    @Override
    public void onClick(final Fragment fragment) {
        if (fragment == null
                || fragment.getActivity() == null
                || fragment.getActivity().isFinishing()) {
            RLog.e(TAG, "onClick activity is null or finishing.");
            return;
        }
        if (!NCChatUIConfig.channelConfig().isEnableSendCombineMessage()) {
            startSelectConversationActivity(fragment, 0);
            return;
        }

        // Show dialog for user to choose between one-by-one forward or combined forward
        String[] items =
                new String[] {
                    fragment.getString(R.string.nc_forward_by_step),
                    fragment.getString(R.string.nc_forward_combine)
                };
        new AlertDialog.Builder(fragment.getActivity())
                .setItems(
                        items,
                        (dialog, which) -> {
                            // which == 0: one-by-one forward, which == 1: combined forward
                            startSelectConversationActivity(fragment, which);
                        })
                .show();
    }

    @Override
    public boolean filter(UiMessage message) {
        return !(ChannelType.DIRECT == message.getChannelType()
                || ChannelType.GROUP == message.getChannelType());
    }

    private void startSelectConversationActivity(Fragment pFragment, int index) {
        if (pFragment == null) {
            return;
        }
        final ChannelFragment fragment = (ChannelFragment) pFragment;
        ChannelViewModel messageViewModel =
                new ViewModelProvider(pFragment).get(ChannelViewModel.class);
        List<Message> messageList = new ArrayList<>();
        for (UiMessage uiMessage : messageViewModel.getSelectedUiMessages()) {
            messageList.add(uiMessage.getMessage());
        }
        List<Message> messages =
                ForwardManager.filterMessagesList(fragment.getContext(), messageList, index);
        if (messages.isEmpty()) {
            RLog.e(TAG, "startSelectConversationActivity the size of messages is 0!");
            return;
        }
        ArrayList<Integer> messageIds = new ArrayList<>();
        for (Message msg : messages) {
            messageIds.add(msg.getClientId());
        }
        ForwardType forwardType = index == 0 ? ForwardType.SINGLE : ForwardType.MULTI;
        RouteUtils.routeToForwardSelectChannelActivity(pFragment, forwardType, messageIds);
    }

    public enum ForwardType {
        SINGLE(0),
        MULTI(1);

        int value;

        ForwardType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public ForwardType valueOf(int value) {
            if (value == SINGLE.value) {
                return SINGLE;
            } else {
                return MULTI;
            }
        }
    }
}
