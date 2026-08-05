package ai.nexconn.chatui.channel.messagelist.status;

import static ai.nexconn.chatui.channel.ChannelViewModel.DEFAULT_COUNT;

import ai.nexconn.chat.message.Message;
import ai.nexconn.chatui.channel.ChannelViewModel;
import ai.nexconn.chatui.channel.event.Event;
import ai.nexconn.chatui.channel.event.page.ScrollEvent;
import ai.nexconn.chatui.channel.event.page.ScrollToEndEvent;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.route.RouteUtils;
import ai.nexconn.chatui.widget.pullrefresh.constant.RefreshState;
import android.os.Bundle;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

public class HistoryState implements IMessageState {
    private static final String TAG = "HistoryState";
    private boolean isLoading;
    private final StateContext context;

    HistoryState(StateContext context) {
        this.context = context;
    }

    /** Navigates to a specific history message. */
    @Override
    public void init(final ChannelViewModel messageViewModel, Bundle bundle) {
        long indexTime = 0;
        if (bundle != null) {
            indexTime = bundle.getLong(RouteUtils.INDEX_MESSAGE_TIME, 0);
        }

        if (indexTime > 0) {
            isLoading = true;
            WeakReference<ChannelViewModel> weakVM = new WeakReference<>(messageViewModel);
            MessageProcessor.getMessagesAll(
                    messageViewModel,
                    indexTime,
                    5,
                    5,
                    new MessageProcessor.GetMessageCallback() {
                        @Override
                        public void onSuccess(
                                List<Message> list, boolean loadOnlyOnce, boolean isHasMoreMsg) {
                            if (weakVM.get() != null) {
                                weakVM.get().onGetHistoryMessage(list, isHasMoreMsg);
                                // '5' is the index of the target message
                                weakVM.get().executePageEvent(new ScrollEvent(5));
                            }
                            isLoading = false;
                        }

                        @Override
                        public void onErrorAsk(List<Message> list) {
                            isLoading = false;
                        }

                        @Override
                        public void onErrorAlways(List<Message> list) {
                            if (weakVM.get() != null) {
                                weakVM.get().onGetHistoryMessage(list);
                                // '5' is the index of the target message
                                weakVM.get().executePageEvent(new ScrollEvent(5));
                            }

                            isLoading = false;
                        }

                        @Override
                        public void onErrorOnlySuccess() {
                            isLoading = false;
                        }
                    });
        }
    }

    @Override
    public void onLoadMore(final ChannelViewModel viewModel) {
        if (!isLoading) {
            isLoading = true;
            WeakReference<ChannelViewModel> weakVM = new WeakReference<>(viewModel);
            MessageProcessor.getMessagesDirection(
                    viewModel,
                    viewModel.getLoadMoreSentTime(),
                    DEFAULT_COUNT,
                    false,
                    new MessageProcessor.GetMessageCallback() {
                        @Override
                        public void onSuccess(
                                List<Message> list, boolean loadOnlyOnce, boolean isHasMoreMsg) {
                            if (weakVM.get() != null) {
                                executeHistoryLoadMore(list, weakVM.get());
                            }
                        }

                        @Override
                        public void onErrorAsk(List<Message> list) {
                            if (weakVM.get() != null) {
                                weakVM.get().onGetHistoryMessage(Collections.<Message>emptyList());
                                weakVM.get()
                                        .executePageEvent(
                                                new Event.RefreshEvent(RefreshState.LoadFinish));
                            }
                            isLoading = false;
                        }

                        @Override
                        public void onErrorAlways(List<Message> list) {
                            if (weakVM.get() != null) {
                                executeHistoryLoadMore(list, weakVM.get());
                            }
                            isLoading = false;
                        }

                        @Override
                        public void onErrorOnlySuccess() {
                            context.setCurrentState(context.normalState);
                            if (weakVM.get() != null) {
                                weakVM.get()
                                        .executePageEvent(
                                                new Event.RefreshEvent(RefreshState.LoadFinish));
                            }
                            isLoading = false;
                        }
                    });
        }
    }

    private void executeHistoryLoadMore(List<Message> messageList, ChannelViewModel viewModel) {
        if (messageList.size() < DEFAULT_COUNT) {
            viewModel.onLoadMoreMessage(messageList);
            context.setCurrentState(context.normalState);
        } else {
            viewModel.onLoadMoreMessage(messageList);
        }
        viewModel.executePageEvent(new Event.RefreshEvent(RefreshState.LoadFinish));
        isLoading = false;
    }

    @Override
    public void onRefresh(ChannelViewModel viewModel) {
        context.normalState.onRefresh(viewModel);
    }

    @Override
    public void onReceived(
            ChannelViewModel viewModel,
            UiMessage uiMessage,
            int left,
            boolean hasPackage,
            boolean offline) {
        // Not at the bottom, add to unread list
        if (!viewModel.isScrollToBottom()) {
            if (!viewModel.filterMessageToHideNewMessageBar(uiMessage)) {
                viewModel.addUnreadNewMessage(uiMessage);
            }
            if (NCChatUIConfig.channelConfig()
                    .isShowNewMentionMessageBar(uiMessage.getChannelType())) {
                viewModel.updateMentionMessage(uiMessage.getMessage());
            }
        }
        // Directly show the message bar
        viewModel.processNewMessageUnread(false);
    }

    @Override
    public void onNewMessageBarClick(final ChannelViewModel viewModel) {
        // Fetch DEFAULT_COUNT+1 (11) records; if fewer than 11, local message fetching is complete
        viewModel.cleanUnreadNewCount();
        WeakReference<ChannelViewModel> weakVM = new WeakReference<>(viewModel);
        MessageProcessor.getMessagesDirection(
                viewModel,
                0,
                DEFAULT_COUNT + 1,
                true,
                new MessageProcessor.GetMessageCallback() {

                    @Override
                    public void onSuccess(
                            List<Message> list, boolean loadOnlyOnce, boolean isHasMoreMsg) {
                        if (weakVM.get() != null) {
                            executeNewMessageBarClick(list, weakVM.get());
                        }
                    }

                    @Override
                    public void onErrorAsk(List<Message> list) {
                        context.setCurrentState(context.normalState);
                        if (weakVM.get() != null) {
                            weakVM.get().refreshAllMessage();
                        }
                    }

                    @Override
                    public void onErrorAlways(List<Message> list) {
                        if (weakVM.get() != null) {
                            executeNewMessageBarClick(list, weakVM.get());
                        }
                    }

                    @Override
                    public void onErrorOnlySuccess() {
                        context.setCurrentState(context.normalState);
                        if (weakVM.get() != null) {
                            weakVM.get().refreshAllMessage();
                        }
                    }
                });
    }

    private void executeNewMessageBarClick(List<Message> messageList, ChannelViewModel viewModel) {
        if (messageList.size() < DEFAULT_COUNT + 1) {
            viewModel.onReloadMessage(messageList);
        } else {
            viewModel.onReloadMessage(messageList.subList(0, DEFAULT_COUNT));
        }
        viewModel.executePageEvent(new ScrollToEndEvent());
        context.setCurrentState(context.normalState);
        viewModel.refreshAllMessage();
    }

    @Override
    public void onNewMentionMessageBarClick(ChannelViewModel viewModel) {
        context.normalState.onNewMentionMessageBarClick(viewModel);
    }

    /**
     * @param viewModel In history state, scrolling to bottom does nothing except trigger load-more.
     */
    @Override
    public void onScrollToBottom(ChannelViewModel viewModel) {
        onLoadMore(viewModel);
    }

    /**
     * @param viewModel In history state, no special handling is needed.
     */
    @Override
    public void onHistoryBarClick(ChannelViewModel viewModel) {
        context.normalState.onHistoryBarClick(viewModel);
    }

    @Override
    public void onClearMessage(ChannelViewModel viewModel) {
        // do nothing
    }

    @Override
    public boolean isNormalState(ChannelViewModel viewModel) {
        return false;
    }
}
