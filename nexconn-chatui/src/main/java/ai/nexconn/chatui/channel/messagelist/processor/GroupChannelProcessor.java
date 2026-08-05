package ai.nexconn.chatui.channel.messagelist.processor;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.channel.ChannelViewModel;
import ai.nexconn.chatui.channel.feature.mention.NCMentionManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.UiMessage;
import android.content.Context;
import android.os.Bundle;

public class GroupChannelProcessor extends BaseChannelProcessor {
    private static final String TAG = "GroupChannelProcessor";

    @Override
    public void init(final ChannelViewModel messageViewModel, Bundle bundle) {
        super.init(messageViewModel, bundle);
    }

    @Override
    public boolean onReceived(
            ChannelViewModel viewModel,
            UiMessage message,
            int left,
            boolean hasPackage,
            boolean offline) {
        if (left == 0 && !hasPackage) {
            ChannelType channelType = viewModel.getCurChannelType();
            if (NCChatUIConfig.channelConfig().isEnableMultiDeviceSync(channelType)) {
                ChannelIdentifier id = viewModel.getChannelIdentifier();
                if (id != null) {
                    NCChatUI.clearUnreadCount(id, null);
                }
            }
        }
        return super.onReceived(viewModel, message, left, hasPackage, offline);
    }

    @Override
    public void onExistUnreadMessage(
            ChannelViewModel viewModel, long sentTime, int unreadMessageCount) {}

    @Override
    public void onResume(ChannelViewModel viewModel) {
        // Only sync if message-list initialization is already complete (i.e. the user is
        // returning to a channel they have already loaded, not opening it for the first time).
        // On first entry, initUnreadMessage() will show the history bar shortly after onResume
        // returns; syncing here would fire onClearedUnreadStatus and hide the bar immediately.
        // Multi-device sync for the first-entry path is now deferred to onScrollToBottom().
        if (!viewModel.isInitUnreadMessageFinish()) {
            return;
        }
        ChannelType type = viewModel.getCurChannelType();
        if (NCChatUIConfig.channelConfig().isEnableMultiDeviceSync(type)) {
            ChannelIdentifier id = viewModel.getChannelIdentifier();
            if (id != null) {
                NCChatUI.clearUnreadCount(id, null);
            }
        }
    }

    @Override
    public boolean onUserPortraitLongClick(
            Context context, ChannelType conversationType, UserInfo userInfo, String targetId) {
        if (userInfo != null && !userInfo.getUserId().equals(NCEngine.getCurrentUserId())) {
            NCMentionManager.getInstance()
                    .mentionMember(conversationType, targetId, userInfo.getUserId());
            return true;
        }
        return false;
    }
}
