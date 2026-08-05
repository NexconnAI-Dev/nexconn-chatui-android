package ai.nexconn.chatui.channel;

import ai.nexconn.chatui.model.UiMessage;

/** Channel list processing interface for custom user implementations. */
public interface IChannelViewModelProcessor {
    boolean onViewClick(ChannelViewModel viewModel, int clickType, UiMessage data);

    boolean onViewLongClick(ChannelViewModel viewModel, int clickType, UiMessage data);
}
