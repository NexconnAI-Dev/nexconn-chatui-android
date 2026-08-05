package ai.nexconn.chatui.channel.messagelist.status;

import static ai.nexconn.chatui.channel.ChannelViewModel.DEFAULT_COUNT;

import ai.nexconn.chat.message.Message;
import ai.nexconn.chatui.channel.ChannelViewModel;
import ai.nexconn.chatui.channel.event.Event;
import ai.nexconn.chatui.channel.event.page.ScrollEvent;
import ai.nexconn.chatui.channel.event.page.ScrollToEndEvent;
import ai.nexconn.chatui.channel.event.page.SmoothScrollEvent;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.widget.pullrefresh.constant.RefreshState;
import android.os.Bundle;
import android.text.TextUtils;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

/** Channel page current state: normal mode. */
public class NormalState implements IMessageState {

    private boolean isLoading;
    private boolean isRefreshLoading;
    private final StateContext context;

    NormalState(StateContext context) {
        this.context = context;
    }

    /**
     * Normal mode initialization: fetches local history and processes unread count.
     *
     * @param messageViewModel ChannelViewModel
     * @param bundle Bundle
     */
    @Override
    public void init(final ChannelViewModel messageViewModel, Bundle bundle) {
        WeakReference<ChannelViewModel> weakVM = new WeakReference<>(messageViewModel);
        MessageProcessor.getMessagesDirection(
                messageViewModel,
                0,
                DEFAULT_COUNT,
                true,
                new MessageProcessor.GetMessageCallback() {
                    @Override
                    public void onSuccess(
                            List<Message> list, boolean loadOnlyOnce, boolean isHasMoreMsg) {
                        if (weakVM.get() != null) {
                            weakVM.get().onGetHistoryMessage(list, isHasMoreMsg);
                            // Process unread messages
                            MessageProcessor.processUnread(weakVM.get());
                        }
                    }

                    @Override
                    public void onErrorAsk(List<Message> list) {
                        if (weakVM.get() != null) {
                            weakVM.get().onGetHistoryMessage(Collections.<Message>emptyList());
                        }
                    }

                    @Override
                    public void onErrorAlways(List<Message> list) {
                        // Process unread messages
                        if (weakVM.get() != null) {
                            MessageProcessor.processUnread(weakVM.get());
                        }
                    }

                    @Override
                    public void onErrorOnlySuccess() {
                        // do nothing
                    }
                });
    }

    /**
     * Normal mode pull-up load-more.
     *
     * @param viewModel ChannelViewModel
     */
    @Override
    public void onLoadMore(final ChannelViewModel viewModel) {
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
                            weakVM.get().onLoadMoreMessage(list);
                            weakVM.get()
                                    .executePageEvent(
                                            new Event.RefreshEvent(RefreshState.LoadFinish));
                        }
                    }

                    @Override
                    public void onErrorAsk(List<Message> list) {
                        if (weakVM.get() != null) {
                            weakVM.get().onLoadMoreMessage(Collections.<Message>emptyList());
                            weakVM.get()
                                    .executePageEvent(
                                            new Event.RefreshEvent(RefreshState.LoadFinish));
                        }
                    }

                    @Override
                    public void onErrorAlways(List<Message> list) {
                        if (weakVM.get() != null) {
                            weakVM.get().onLoadMoreMessage(list);
                            weakVM.get()
                                    .executePageEvent(
                                            new Event.RefreshEvent(RefreshState.LoadFinish));
                        }
                    }

                    @Override
                    public void onErrorOnlySuccess() {
                        if (weakVM.get() != null) {
                            weakVM.get()
                                    .executePageEvent(
                                            new Event.RefreshEvent(RefreshState.LoadFinish));
                        }
                    }
                });
    }

    @Override
    public void onRefresh(final ChannelViewModel viewModel) {
        if (!isRefreshLoading) {
            isRefreshLoading = true;
            WeakReference<ChannelViewModel> weakVM = new WeakReference<>(viewModel);
            MessageProcessor.getMessagesDirection(
                    viewModel,
                    viewModel.getRefreshSentTime(),
                    DEFAULT_COUNT,
                    true,
                    new MessageProcessor.GetMessageCallback() {

                        @Override
                        public void onSuccess(
                                List<Message> list, boolean loadOnlyOnce, boolean isHasMoreMsg) {
                            isRefreshLoading = loadOnlyOnce;
                            if (weakVM.get() != null) {
                                weakVM.get().onGetHistoryMessage(list, isHasMoreMsg);
                                weakVM.get()
                                        .executePageEvent(
                                                new Event.RefreshEvent(RefreshState.RefreshFinish));
                            }
                        }

                        @Override
                        public void onErrorAsk(List<Message> list) {
                            isRefreshLoading = false;
                            if (weakVM.get() != null) {
                                weakVM.get().onGetHistoryMessage(Collections.<Message>emptyList());
                                weakVM.get()
                                        .executePageEvent(
                                                new Event.RefreshEvent(RefreshState.RefreshFinish));
                            }
                        }

                        @Override
                        public void onErrorAlways(List<Message> list) {
                            isRefreshLoading = false;
                            if (weakVM.get() != null) {
                                weakVM.get().onGetHistoryMessage(list);
                                weakVM.get()
                                        .executePageEvent(
                                                new Event.RefreshEvent(RefreshState.RefreshFinish));
                            }
                        }

                        @Override
                        public void onErrorOnlySuccess() {
                            isRefreshLoading = false;
                            if (weakVM.get() != null) {
                                weakVM.get()
                                        .executePageEvent(
                                                new Event.RefreshEvent(RefreshState.RefreshFinish));
                            }
                        }
                    });
        } else {
            viewModel.executePageEvent(new Event.RefreshEvent(RefreshState.RefreshFinish));
        }
    }

    /**
     * Normal mode message handling.
     *
     * @param viewModel ChannelViewModel
     * @param uiMessage the received message
     * @param left remaining message count after dispatching each data packet
     * @param hasPackage whether there are undispatched message packets on the server
     * @param offline whether this is an offline message
     */
    @Override
    public void onReceived(
            ChannelViewModel viewModel,
            UiMessage uiMessage,
            int left,
            boolean hasPackage,
            boolean offline) {
        // Deduplication
        for (UiMessage item : viewModel.getUiMessages()) {
            if (TextUtils.equals(item.getMessageId(), uiMessage.getMessageId())) {
                return;
            }
        }
        viewModel.getUiMessages().add(uiMessage);
        viewModel.refreshAllMessage(false);
        viewModel.updateMentionMessage(uiMessage.getMessage());
        // newMessageBar logic
        if (!NCChatUIConfig.channelConfig().isShowNewMessageBar(uiMessage.getChannelType())) {
            viewModel.executePostPageEvent(new ScrollToEndEvent());
        } else {
            // If not scrolled to the bottom, add to the unread list
            if (!viewModel.isScrollToBottom()
                    && !viewModel.filterMessageToHideNewMessageBar(uiMessage)) {
                viewModel.addUnreadNewMessage(uiMessage);
            }
            // Check whether the UI is scrolled to the bottom
            if (NCChatUIConfig.channelConfig().isShowNewMessageBar(viewModel.getCurChannelType())) {
                if (viewModel.isScrollToBottom()) {
                    viewModel.executePostPageEvent(new ScrollToEndEvent());
                } else {
                    viewModel.processNewMessageUnread(false);
                }
            }
        }
    }

    @Override
    public void onNewMessageBarClick(ChannelViewModel viewModel) {
        viewModel.cleanUnreadNewCount();
        viewModel.executePageEvent(new ScrollToEndEvent());
    }

    @Override
    public void onNewMentionMessageBarClick(ChannelViewModel viewModel) {
        List<Message> mMentionMessages = viewModel.getNewUnReadMentionMessages();
        if (mMentionMessages.isEmpty()) {
            viewModel.updateNewMentionMessageUnreadBar();
        } else {
            Message message = mMentionMessages.get(0);
            int position = viewModel.findPositionByMessageId(message.getClientId());
            if (position >= 0) {
                viewModel.executePageEvent(new ScrollEvent(position));
            } else {
                boolean isNewMentionMessage = false;
                if (viewModel.getUiMessages().size() > 0) {
                    UiMessage lastMessage =
                            viewModel.getUiMessages().get(viewModel.getUiMessages().size() - 1);
                    isNewMentionMessage = message.getSentTime() > lastMessage.getSentTime();
                }
                viewModel.getUiMessages().clear();
                getMentionMessage(viewModel, isNewMentionMessage, message);
            }
        }
    }

    private void getMentionMessage(
            final ChannelViewModel viewModel, boolean isNewMentionMessage, final Message message) {
        WeakReference<ChannelViewModel> weakVM = new WeakReference<>(viewModel);
        if (isNewMentionMessage) {
            if (!isLoading) {
                isLoading = true;
                MessageProcessor.getMessagesAll(
                        viewModel,
                        message.getSentTime(),
                        DEFAULT_COUNT,
                        DEFAULT_COUNT,
                        new MessageProcessor.GetMessageCallback() {
                            @Override
                            public void onSuccess(
                                    List<Message> list,
                                    boolean loadOnlyOnce,
                                    boolean isHasMoreMsg) {
                                isLoading = loadOnlyOnce;
                                if (list.size() < DEFAULT_COUNT * 2) {
                                    context.setCurrentState(context.normalState);
                                } else {
                                    context.setCurrentState(context.historyState);
                                }
                                if (weakVM.get() != null) {
                                    weakVM.get().onLoadMoreMessage(list);
                                    weakVM.get()
                                            .executePageEvent(
                                                    new Event.RefreshEvent(
                                                            RefreshState.LoadFinish));
                                    int position =
                                            weakVM.get()
                                                    .findPositionByMessageId(message.getClientId());
                                    if (position >= 0) {
                                        weakVM.get().executePageEvent(new ScrollEvent(position));
                                    }
                                }
                            }

                            @Override
                            public void onErrorAsk(List<Message> list) {
                                isLoading = false;
                            }

                            @Override
                            public void onErrorAlways(List<Message> list) {
                                if (list.size() < DEFAULT_COUNT * 2) {
                                    context.setCurrentState(context.normalState);
                                } else {
                                    context.setCurrentState(context.historyState);
                                }
                                if (weakVM.get() != null) {
                                    weakVM.get().onLoadMoreMessage(list);
                                    weakVM.get()
                                            .executePageEvent(
                                                    new Event.RefreshEvent(
                                                            RefreshState.LoadFinish));
                                    int position =
                                            weakVM.get()
                                                    .findPositionByMessageId(message.getClientId());
                                    if (position >= 0) {
                                        weakVM.get().executePageEvent(new ScrollEvent(position));
                                    }
                                }
                                isLoading = false;
                            }

                            @Override
                            public void onErrorOnlySuccess() {
                                isLoading = false;
                            }
                        });
            }
        } else {
            MessageProcessor.getMessagesDirection(
                    viewModel,
                    message.getSentTime() - 2,
                    DEFAULT_COUNT,
                    false,
                    new MessageProcessor.GetMessageCallback() {
                        @Override
                        public void onSuccess(
                                List<Message> list, boolean loadOnlyOnce, boolean isHasMoreMsg) {
                            if (weakVM.get() != null) {
                                executeMentionHistoryMsg(list, weakVM.get());
                            }
                        }

                        @Override
                        public void onErrorAsk(List<Message> list) {
                            if (weakVM.get() != null) {
                                weakVM.get().onGetHistoryMessage(Collections.<Message>emptyList());
                            }
                        }

                        @Override
                        public void onErrorAlways(List<Message> list) {
                            if (weakVM.get() != null) {
                                executeMentionHistoryMsg(list, weakVM.get());
                            }
                        }

                        @Override
                        public void onErrorOnlySuccess() {
                            // do nothing
                        }
                    });
        }
    }

    private void executeMentionHistoryMsg(List<Message> messageList, ChannelViewModel viewModel) {
        if (messageList.size() < DEFAULT_COUNT) {
            context.setCurrentState(context.normalState);
        } else {
            context.setCurrentState(context.historyState);
        }
        viewModel.onGetHistoryMessage(messageList);
        viewModel.executePageEvent(new ScrollEvent(0));
    }

    @Override
    public void onScrollToBottom(ChannelViewModel viewModel) {
        if (NCChatUIConfig.channelConfig().isShowNewMessageBar(viewModel.getCurChannelType())) {
            viewModel.cleanUnreadNewCount();
            viewModel.processNewMessageUnread(true);
        }
    }

    @Override
    public void onHistoryBarClick(final ChannelViewModel messageViewModel) {
        Message firstUnreadMessage = messageViewModel.getFirstUnreadMessage();
        if (firstUnreadMessage != null) {
            WeakReference<ChannelViewModel> weakVM = new WeakReference<>(messageViewModel);
            MessageProcessor.getMessagesDirection(
                    messageViewModel,
                    firstUnreadMessage.getSentTime() - 2,
                    DEFAULT_COUNT,
                    false,
                    new MessageProcessor.GetMessageCallback() {
                        @Override
                        public void onSuccess(
                                List<Message> list, boolean loadOnlyOnce, boolean isHasMoreMsg) {
                            if (weakVM.get() != null) {
                                executeHistoryBarClick(list, weakVM.get());
                            }
                        }

                        @Override
                        public void onErrorAsk(List<Message> list) {
                            if (weakVM.get() != null) {
                                weakVM.get().onGetHistoryMessage(Collections.<Message>emptyList());
                            }
                        }

                        @Override
                        public void onErrorAlways(List<Message> list) {
                            if (weakVM.get() != null) {
                                executeHistoryBarClick(list, weakVM.get());
                            }
                        }

                        @Override
                        public void onErrorOnlySuccess() {
                            // do nothing
                        }
                    });
        }
    }

    @Override
    public void onClearMessage(ChannelViewModel viewModel) {
        // do nothing
    }

    @Override
    public boolean isNormalState(ChannelViewModel viewModel) {
        return true;
    }

    private void executeHistoryBarClick(
            List<Message> messageList, ChannelViewModel messageViewModel) {
        messageViewModel.executePageEvent(new SmoothScrollEvent(0));
        messageViewModel.onReloadMessage(messageList);
        messageViewModel.hideHistoryBar();
        context.setCurrentState(context.historyState);
    }
}
