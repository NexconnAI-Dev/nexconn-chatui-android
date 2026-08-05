package ai.nexconn.chatui.channel.messagelist.processor;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.model.ConnectionStatus;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.channel.ChannelViewModel;
import ai.nexconn.chatui.model.UiMessage;
import android.content.Context;
import android.os.Bundle;
import java.util.List;

/** Channel business processor. */
public interface IChannelProcessor {

    /**
     * First-time initialization: bind events, etc.
     *
     * @param messageViewModel the message ViewModel
     * @param bundle the bundle passed from the previous page
     */
    void init(ChannelViewModel messageViewModel, Bundle bundle);

    /**
     * Callback invoked when a message is received.
     *
     * @param messageViewModel
     * @param message
     * @param left
     * @param hasPackage
     * @param offline
     * @return whether the event is intercepted
     */
    boolean onReceived(
            ChannelViewModel messageViewModel,
            UiMessage message,
            int left,
            boolean hasPackage,
            boolean offline);

    /**
     * Callback for command messages (MessageTag is None or Status).
     *
     * @param messageViewModel
     * @param message the intercepted command message
     * @return true to suppress UI display; false to show in UI
     */
    boolean onReceivedCmd(ChannelViewModel messageViewModel, Message message);

    /**
     * Message click event.
     *
     * @param uiMessage
     */
    void onMessageItemClick(UiMessage uiMessage);

    /**
     * Message long-press event.
     *
     * @param uiMessage
     * @return whether the event is intercepted
     */
    boolean onMessageItemLongClick(UiMessage uiMessage);

    /**
     * User portrait click event.
     *
     * @param context
     * @param conversationType
     * @param userInfo
     * @param targetId
     */
    void onUserPortraitClick(
            Context context, ChannelType conversationType, UserInfo userInfo, String targetId);

    /**
     * User portrait long-press event.
     *
     * @param context
     * @param conversationType
     * @param userInfo
     * @param targetId
     * @return whether the event is consumed
     */
    boolean onUserPortraitLongClick(
            Context context, ChannelType conversationType, UserInfo userInfo, String targetId);

    boolean onBackPressed(ChannelViewModel viewModel);

    void onDestroy(ChannelViewModel viewModel);

    void onExistUnreadMessage(ChannelViewModel viewModel, long sentTime, int unreadMessageCount);

    void onMessageReceiptRequest(
            ChannelViewModel viewModel,
            ChannelType conversationType,
            String targetId,
            String messageUId);

    void onLoadMessage(ChannelViewModel viewModel, List<Message> messages);

    void onConnectStatusChange(ChannelViewModel viewModel, ConnectionStatus status);

    void onResume(ChannelViewModel viewModel);

    void onLoadMore(ChannelViewModel viewModel);

    void onClearMessage(ChannelViewModel viewModel);

    void onRefresh(ChannelViewModel viewModel);

    void newMessageBarClick(ChannelViewModel viewModel);

    void unreadBarClick(ChannelViewModel viewModel);

    void newMentionMessageBarClick(ChannelViewModel viewModel);

    boolean isNormalState(ChannelViewModel viewModel);

    boolean isHistoryState(ChannelViewModel viewModel);

    void onScrollToBottom(ChannelViewModel viewModel);
}
