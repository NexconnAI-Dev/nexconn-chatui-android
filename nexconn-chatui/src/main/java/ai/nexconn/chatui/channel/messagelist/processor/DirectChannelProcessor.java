package ai.nexconn.chatui.channel.messagelist.processor;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.channel.ChannelViewModel;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.UiMessage;
import android.os.Bundle;

public class DirectChannelProcessor extends BaseChannelProcessor {

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
            ChannelType type = viewModel.getCurChannelType();
            if (NCChatUIConfig.channelConfig().isEnableMultiDeviceSync(type)) {
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
            ChannelViewModel viewModel, long sentTime, int unreadMessageCount) {
        // Do NOT call clearUnreadCount here.
        //
        // The old behaviour was to clear immediately with latestMessage.sentTime, which tells the
        // server "I've read everything up to the latest message". The server then fires
        // onClearedUnreadStatus → hideHistoryBar(), which races with initUnreadMessage() →
        // showHistoryBar(), causing the "X unread messages" banner to disappear before the
        // user ever sees it (Bug #6971860475).
        //
        // The correct sync points are:
        //   • onScrollToBottom – user has actually scrolled to the bottom and read everything.
        //   • onReceived       – user is in-channel and a new message arrives (already syncs).
        //   • onResume         – user re-enters an already-initialised channel.
    }

    @Override
    public void onScrollToBottom(ChannelViewModel viewModel) {
        super.onScrollToBottom(viewModel);
        // User has scrolled to the bottom, meaning all unread messages have been seen.
        // Now it is safe to tell other devices that this channel is fully read.
        ChannelType type = viewModel.getCurChannelType();
        if (NCChatUIConfig.channelConfig().isEnableMultiDeviceSync(type)) {
            ChannelIdentifier id = viewModel.getChannelIdentifier();
            if (id != null) {
                NCChatUI.clearUnreadCount(id, null);
            }
        }
    }

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
}
