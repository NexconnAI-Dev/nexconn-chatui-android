package ai.nexconn.chatui.channel.messagelist.status;

import ai.nexconn.chatui.channel.ChannelViewModel;
import ai.nexconn.chatui.model.UiMessage;
import android.os.Bundle;

/** Handles logic for the message list, history-message bar, and new-message bar. */
public interface IMessageState {
    /**
     * Handles history messages initialization.
     *
     * @param viewModel {@link ChannelViewModel}
     * @param bundle Bundle
     */
    void init(ChannelViewModel viewModel, Bundle bundle);

    /**
     * Handles pull-up load-more.
     *
     * @param viewModel {@link ChannelViewModel}
     */
    void onLoadMore(ChannelViewModel viewModel);

    /**
     * Handles pull-down refresh.
     *
     * @param viewModel {@link ChannelViewModel}
     */
    void onRefresh(ChannelViewModel viewModel);

    /**
     * Callback when a message is received.
     *
     * @param viewModel {@link ChannelViewModel}
     * @param message the message
     * @param left number of remaining unfetched messages
     * @param hasPackage whether there are remaining message packages
     * @param offline whether the message is offline
     */
    void onReceived(
            ChannelViewModel viewModel,
            UiMessage message,
            int left,
            boolean hasPackage,
            boolean offline);

    /**
     * New-message bar click event.
     *
     * @param viewModel {@link ChannelViewModel}
     */
    void onNewMessageBarClick(ChannelViewModel viewModel);

    /**
     * New {@code @}-mention message bar click event.
     *
     * @param viewModel {@link ChannelViewModel}
     */
    void onNewMentionMessageBarClick(ChannelViewModel viewModel);

    /**
     * Scrolled to the bottom.
     *
     * @param viewModel {@link ChannelViewModel}
     */
    void onScrollToBottom(ChannelViewModel viewModel);

    /**
     * History-message bar click event.
     *
     * @param viewModel {@link ChannelViewModel}
     */
    void onHistoryBarClick(ChannelViewModel viewModel);

    /**
     * Clears messages.
     *
     * @param viewModel {@link ChannelViewModel}
     */
    void onClearMessage(ChannelViewModel viewModel);

    /**
     * Whether the current state is the normal channel state.
     *
     * @param viewModel {@link ChannelViewModel}
     * @return true if in normal channel state
     */
    boolean isNormalState(ChannelViewModel viewModel);
}
