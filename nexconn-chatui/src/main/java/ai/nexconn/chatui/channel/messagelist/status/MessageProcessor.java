package ai.nexconn.chatui.channel.messagelist.status;

import static ai.nexconn.chatui.channel.ChannelViewModel.DEFAULT_COUNT;
import static ai.nexconn.chatui.channel.ChannelViewModel.DEFAULT_REMOTE_COUNT;

import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.query.LocalMessagesByTimeQuery;
import ai.nexconn.chat.channel.query.MessagesQuery;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.handler.RefreshReferenceMessageHandler;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.ReferenceMessage;
import ai.nexconn.chat.message.model.ReferenceMessageResult;
import ai.nexconn.chat.message.model.ReferenceMessageStatus;
import ai.nexconn.chat.model.PageData;
import ai.nexconn.chat.params.LocalMessagesByTimeQueryParams;
import ai.nexconn.chat.params.MessagesQueryParams;
import ai.nexconn.chat.params.RefreshReferenceMessageParams;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.channel.ChannelViewModel;
import ai.nexconn.chatui.channel.event.Event;
import ai.nexconn.chatui.channel.event.action.RefreshEvent;
import ai.nexconn.chatui.channel.event.page.ScrollMentionEvent;
import ai.nexconn.chatui.channel.event.page.ShowLoadMessageDialogEvent;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.widget.pullrefresh.constant.RefreshState;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MessageProcessor {

    public static void getMessagesDirection(
            final ChannelViewModel messageViewModel,
            long sentTime,
            int count,
            final boolean isForward,
            final GetMessageCallback callback) {
        if (messageViewModel == null) {
            if (callback != null) {
                callback.onSuccess(new ArrayList<>(), false);
            }
            return;
        }
        // First load (sentTime==0): always use LocalMessagesByTimeQuery first for an instant
        // local-cache preview (works offline too), then trigger MessagesQuery for remote sync.
        // This avoids a blank screen while waiting for the network on any channel type.
        if (sentTime == 0) {
            WeakReference<ChannelViewModel> weakVM = new WeakReference<>(messageViewModel);
            ChannelIdentifier id = messageViewModel.getChannelIdentifier();
            LocalMessagesByTimeQueryParams localParams = new LocalMessagesByTimeQueryParams(id);
            localParams.setSentTime(0L);
            localParams.setAscending(false);
            localParams.setPageSize(DEFAULT_COUNT);
            LocalMessagesByTimeQuery localQuery =
                    BaseChannel.createLocalMessagesByTimeQuery(localParams);
            localQuery.loadNextPage(
                    new OperationHandler<PageData<Message>>() {
                        @Override
                        public void onResult(PageData<Message> pageData, NCError error) {
                            List<Message> messages =
                                    (pageData != null && pageData.getData() != null)
                                            ? pageData.getData()
                                            : new ArrayList<>();
                            refreshMessageExtendInfo(
                                    weakVM.get(),
                                    messages,
                                    new RefreshCallback<List<Message>>() {
                                        @Override
                                        public void onSuccess(List<Message> refreshList) {
                                            // Check if ViewModel is still valid before proceeding
                                            ChannelViewModel vm = weakVM.get();
                                            if (vm == null) {
                                                // ViewModel was GC'd, abort to prevent blank screen
                                                if (callback != null) {
                                                    callback.onErrorAlways(new ArrayList<>());
                                                }
                                                return;
                                            }
                                            // Show local messages immediately without touching
                                            // unread/scroll state — that happens after remote sync.
                                            if (!refreshList.isEmpty()) {
                                                List<Message> ordered =
                                                        new ArrayList<>(refreshList);
                                                if (!isForward) {
                                                    Collections.reverse(ordered);
                                                }
                                                vm.onGetHistoryMessage(ordered);
                                            }
                                            // Full remote sync — owns the callback (processUnread
                                            // etc.)
                                            // Use the validated vm reference instead of
                                            // weakVM.get()
                                            getMessages(vm, sentTime, count, isForward, callback);
                                        }
                                    });
                        }
                    });
        } else {
            getMessages(messageViewModel, sentTime, count, isForward, callback);
        }
    }

    public static void getMessages(
            final ChannelViewModel messageViewModel,
            long sentTime,
            int count,
            final boolean isForward,
            final GetMessageCallback callback) {
        if (messageViewModel == null) {
            if (callback != null) {
                callback.onSuccess(new ArrayList<>(), false);
            }
            return;
        }
        ChannelIdentifier id = messageViewModel.getChannelIdentifier();
        MessagesQueryParams params = new MessagesQueryParams(id);
        params.setStartTime(sentTime);
        params.setAscending(!isForward); // isForward=true → descend (older), false → ascend (newer)
        params.setPageSize(count);
        WeakReference<ChannelViewModel> weakVM = new WeakReference<>(messageViewModel);
        MessagesQuery query = BaseChannel.createMessagesQuery(params);
        query.loadNextPage(
                new OperationHandler<PageData<Message>>() {
                    @Override
                    public void onResult(PageData<Message> pageData, NCError error) {
                        boolean hasMoreMsg = query.getHasMore();
                        List<Message> messageList =
                                (pageData != null && pageData.getData() != null)
                                        ? pageData.getData()
                                        : new ArrayList<>();
                        if (error != null && messageList.isEmpty()) {
                            getLocalMessages(
                                    messageViewModel,
                                    sentTime,
                                    count,
                                    isForward,
                                    new OperationHandler<PageData<Message>>() {
                                        @Override
                                        public void onResult(
                                                PageData<Message> localPageData,
                                                NCError localError) {
                                            List<Message> localMessages =
                                                    (localPageData != null
                                                                    && localPageData.getData()
                                                                            != null)
                                                            ? localPageData.getData()
                                                            : new ArrayList<>();
                                            handleGetMessagesResult(
                                                    weakVM,
                                                    localMessages,
                                                    localError,
                                                    false,
                                                    isForward,
                                                    callback);
                                        }
                                    });
                            return;
                        }
                        handleGetMessagesResult(
                                weakVM, messageList, error, hasMoreMsg, isForward, callback);
                    }
                });
    }

    private static void getLocalMessages(
            ChannelViewModel messageViewModel,
            long sentTime,
            int count,
            boolean isForward,
            OperationHandler<PageData<Message>> handler) {
        ChannelIdentifier id = messageViewModel.getChannelIdentifier();
        LocalMessagesByTimeQueryParams localParams = new LocalMessagesByTimeQueryParams(id);
        localParams.setSentTime(sentTime);
        localParams.setAscending(!isForward);
        localParams.setPageSize(count);
        LocalMessagesByTimeQuery localQuery =
                BaseChannel.createLocalMessagesByTimeQuery(localParams);
        localQuery.loadNextPage(handler);
    }

    private static void handleGetMessagesResult(
            final WeakReference<ChannelViewModel> weakVM,
            List<Message> messageList,
            final NCError error,
            final boolean hasMoreMsg,
            final boolean isForward,
            final GetMessageCallback callback) {
        RefreshCallback<List<Message>> refreshCallback =
                new RefreshCallback<List<Message>>() {
                    @Override
                    public void onSuccess(List<Message> refreshList) {
                        ai.nexconn.chatui.config.ChannelLoadMessageType type =
                                NCChatUIConfig.channelConfig().getChannelLoadMessageType();
                        if (!isForward) {
                            Collections.reverse(refreshList);
                        }
                        if (error == null) {
                            if (callback != null) {
                                callback.onSuccess(refreshList, false, hasMoreMsg);
                            }
                            return;
                        }
                        if (ai.nexconn.chatui.config.ChannelLoadMessageType.ASK.equals(type)) {
                            if (callback != null) {
                                if (weakVM.get() != null) {
                                    weakVM.get()
                                            .executePageEvent(
                                                    new ShowLoadMessageDialogEvent(
                                                            callback, refreshList));
                                }
                                callback.onErrorAsk(refreshList);
                            }
                        } else if (ai.nexconn.chatui.config.ChannelLoadMessageType.ONLY_SUCCESS
                                .equals(type)) {
                            if (weakVM.get() != null) {
                                weakVM.get().onGetHistoryMessage(Collections.<Message>emptyList());
                            }
                            if (callback != null) {
                                callback.onErrorOnlySuccess();
                            }
                        } else {
                            if (weakVM.get() != null) {
                                weakVM.get().onGetHistoryMessage(refreshList);
                            }
                            if (callback != null) {
                                callback.onErrorAlways(refreshList);
                            }
                        }
                    }
                };
        refreshMessageExtendInfo(weakVM.get(), messageList, refreshCallback);
    }

    public static void getMessagesAll(
            final ChannelViewModel messageViewModel,
            final long sentTime,
            int before,
            final int after,
            final GetMessageCallback callback) {
        if (messageViewModel == null) {
            if (callback != null) {
                callback.onSuccess(new ArrayList<>(), false);
            }
            return;
        }
        final List<Message> allData = new ArrayList<>();
        ChannelIdentifier id = messageViewModel.getChannelIdentifier();
        // Ascending = older messages before sentTime
        MessagesQueryParams ascParams = new MessagesQueryParams(id);
        ascParams.setStartTime(sentTime);
        ascParams.setAscending(true); // older before sentTime
        ascParams.setPageSize(before);
        final long descendStartTime = sentTime + 1;
        WeakReference<ChannelViewModel> weakVM = new WeakReference<>(messageViewModel);
        MessagesQuery ascQuery = BaseChannel.createMessagesQuery(ascParams);
        ascQuery.loadNextPage(
                new OperationHandler<PageData<Message>>() {
                    @Override
                    public void onResult(PageData<Message> pageData, NCError error) {
                        List<Message> messageList =
                                (pageData != null && pageData.getData() != null)
                                        ? pageData.getData()
                                        : new ArrayList<>();
                        RefreshCallback<List<Message>> refreshCallback =
                                new RefreshCallback<List<Message>>() {
                                    @Override
                                    public void onSuccess(List<Message> refreshList) {
                                        ai.nexconn.chatui.config.ChannelLoadMessageType type =
                                                NCChatUIConfig.channelConfig()
                                                        .getChannelLoadMessageType();
                                        Collections.reverse(refreshList);
                                        allData.addAll(refreshList);
                                        if (error == null) {
                                            getMessagesDescend(
                                                    descendStartTime,
                                                    after,
                                                    weakVM.get(),
                                                    allData,
                                                    true,
                                                    callback);
                                            return;
                                        }
                                        if (ai.nexconn.chatui.config.ChannelLoadMessageType.ASK
                                                .equals(type)) {
                                            if (weakVM.get() != null) {
                                                weakVM.get()
                                                        .executePageEvent(
                                                                new ShowLoadMessageDialogEvent(
                                                                        callback, allData));
                                            }
                                            if (callback != null) {
                                                callback.onErrorAsk(allData);
                                            }
                                        } else if (ai.nexconn.chatui.config.ChannelLoadMessageType
                                                .ONLY_SUCCESS
                                                .equals(type)) {
                                            if (weakVM.get() != null) {
                                                weakVM.get()
                                                        .onGetHistoryMessage(
                                                                Collections.<Message>emptyList());
                                            }
                                            if (callback != null) {
                                                callback.onErrorOnlySuccess();
                                            }
                                        } else {
                                            getMessagesDescend(
                                                    descendStartTime,
                                                    after,
                                                    weakVM.get(),
                                                    allData,
                                                    false,
                                                    callback);
                                        }
                                    }
                                };
                        refreshMessageExtendInfo(weakVM.get(), messageList, refreshCallback);
                    }
                });
    }

    private static void getMessagesDescend(
            final long sentTime,
            int after,
            final ChannelViewModel viewModel,
            final List<Message> allData,
            final boolean isSuccess,
            final GetMessageCallback callback) {
        if (viewModel == null) {
            if (callback != null) {
                callback.onErrorAlways(allData);
            }
            return;
        }
        ChannelIdentifier id = viewModel.getChannelIdentifier();
        MessagesQueryParams descParams = new MessagesQueryParams(id);
        descParams.setStartTime(sentTime);
        descParams.setAscending(false); // descend = newer messages
        descParams.setPageSize(after);
        WeakReference<ChannelViewModel> weakVM = new WeakReference<>(viewModel);
        MessagesQuery descQuery = BaseChannel.createMessagesQuery(descParams);
        descQuery.loadNextPage(
                new OperationHandler<PageData<Message>>() {
                    @Override
                    public void onResult(PageData<Message> pageData, NCError error) {
                        boolean hasMoreMsg = descQuery.getHasMore();
                        List<Message> messageList =
                                (pageData != null && pageData.getData() != null)
                                        ? pageData.getData()
                                        : new ArrayList<>();
                        RefreshCallback<List<Message>> refreshCallback =
                                new RefreshCallback<List<Message>>() {
                                    @Override
                                    public void onSuccess(List<Message> refreshList) {
                                        if (!(refreshList == null || refreshList.isEmpty())) {
                                            allData.addAll(refreshList);
                                        }
                                        if (callback != null) {
                                            if (isSuccess) {
                                                callback.onSuccess(allData, false, hasMoreMsg);
                                            } else {
                                                callback.onErrorAlways(allData);
                                            }
                                        }
                                    }
                                };
                        refreshMessageExtendInfo(weakVM.get(), messageList, refreshCallback);
                    }
                });
    }

    public static void processUnread(final ChannelViewModel messageViewModel) {
        if (messageViewModel == null) {
            return;
        }
        WeakReference<ChannelViewModel> weakVM = new WeakReference<>(messageViewModel);
        ChannelIdentifier identifier = messageViewModel.getChannelIdentifier();
        BaseChannel.getChannels(
                Collections.singletonList(identifier),
                new OperationHandler<List<BaseChannel>>() {
                    @Override
                    public void onResult(List<BaseChannel> channels, NCError error) {
                        if (channels == null || channels.isEmpty()) {
                            return;
                        }
                        BaseChannel channel = channels.get(0);
                        int unreadMessageCount = channel.getUnreadCount();
                        int mentionedCount = channel.getMentionedCount();
                        if (unreadMessageCount > 0 && weakVM.get() != null) {
                            weakVM.get()
                                    .onExistUnreadMessage(
                                            channel.getLatestMessage() != null
                                                    ? channel.getLatestMessage().getSentTime()
                                                    : 0L,
                                            unreadMessageCount);
                        } else if (weakVM.get() != null) {
                            // No unread messages: make sure the history bar is cleared in case
                            // a previous ViewModel run left it visible (e.g. conversation list
                            // cleared the local count before the user entered this page).
                            weakVM.get().hideHistoryBar();
                        }
                        initUnreadMessage(weakVM.get(), unreadMessageCount);
                        initMentionedMessage(mentionedCount, weakVM.get());
                    }
                });
    }

    private static void initUnreadMessage(
            final ChannelViewModel messageViewModel, final int unreadMessageCount) {
        if (messageViewModel == null) {
            return;
        }
        if (messageViewModel.isInitUnreadMessageFinish()) {
            // Already initialized. If the unread count has since dropped below the display
            // threshold (e.g. cleared from the conversation list before entering this page),
            // ensure the history bar is hidden so it does not show stale information.
            if (unreadMessageCount < ChannelViewModel.SHOW_UNREAD_MESSAGE_COUNT) {
                messageViewModel.hideHistoryBar();
            }
            return;
        }
        WeakReference<ChannelViewModel> weakVM = new WeakReference<>(messageViewModel);
        messageViewModel
                .createChannel()
                .getFirstUnreadMessage(
                        new OperationHandler<Message>() {
                            @Override
                            public void onResult(Message message, NCError error) {
                                if (weakVM.get() == null) {
                                    return;
                                }
                                if (unreadMessageCount >= ChannelViewModel.SHOW_UNREAD_MESSAGE_COUNT
                                        && message != null) {
                                    weakVM.get().setFirstUnreadMessage(message);
                                    if (NCChatUIConfig.channelConfig()
                                            .isShowHistoryMessageBar(
                                                    weakVM.get().getCurChannelType())) {
                                        weakVM.get().showHistoryBar(unreadMessageCount);
                                    }
                                }
                                weakVM.get().setInitUnreadMessageFinish(true);
                                weakVM.get().cleanUnreadStatus();
                            }
                        });
    }

    private static void initMentionedMessage(
            int mentionedCount, final ChannelViewModel messageViewModel) {
        if (messageViewModel == null) {
            return;
        }
        if (messageViewModel.isInitMentionedMessageFinish()) {
            return;
        }
        WeakReference<ChannelViewModel> weakVM = new WeakReference<>(messageViewModel);
        if (mentionedCount > 0) {
            messageViewModel
                    .createChannel()
                    .getUnreadMentionedMessages(
                            new OperationHandler<List<Message>>() {
                                @Override
                                public void onResult(List<Message> messages, NCError error) {
                                    if (weakVM.get() == null) {
                                        return;
                                    }
                                    if (messages != null && messages.size() > 0) {
                                        weakVM.get().setNewUnReadMentionMessages(messages);
                                        weakVM.get().executePageEvent(new ScrollMentionEvent());
                                    }
                                    weakVM.get().setInitMentionedMessageFinish(true);
                                    weakVM.get().cleanUnreadStatus();
                                }
                            });
        } else {
            messageViewModel.setInitMentionedMessageFinish(true);
            messageViewModel.cleanUnreadStatus();
        }
    }

    public static void getLocalMessage(final ChannelViewModel messageViewModel) {
        if (messageViewModel == null) {
            return;
        }
        WeakReference<ChannelViewModel> weakVM = new WeakReference<>(messageViewModel);
        ChannelIdentifier id = messageViewModel.getChannelIdentifier();
        // Use sentTime cursor for local query
        long startTime =
                messageViewModel.getRefreshSentTime() > 0
                        ? messageViewModel.getRefreshSentTime()
                        : 0L;
        MessagesQueryParams params = new MessagesQueryParams(id);
        params.setStartTime(startTime);
        params.setAscending(true); // ascending from startTime
        params.setPageSize(DEFAULT_COUNT + 1);
        MessagesQuery query = BaseChannel.createMessagesQuery(params);
        query.loadNextPage(
                new OperationHandler<PageData<Message>>() {
                    @Override
                    public void onResult(PageData<Message> pageData, NCError error) {
                        if (weakVM.get() == null) {
                            return;
                        }
                        List<Message> messages =
                                (pageData != null && pageData.getData() != null)
                                        ? pageData.getData()
                                        : new ArrayList<>();
                        RefreshCallback<List<Message>> refreshCallback =
                                new RefreshCallback<List<Message>>() {
                                    @Override
                                    public void onSuccess(List<Message> refreshList) {
                                        if (refreshList != null && refreshList.size() > 0) {
                                            List<Message> result;
                                            if (refreshList.size() < DEFAULT_COUNT + 1) {
                                                result = refreshList;
                                            } else {
                                                result = refreshList.subList(0, DEFAULT_COUNT);
                                            }
                                            weakVM.get().onGetHistoryMessage(result);
                                            weakVM.get()
                                                    .executePageEvent(
                                                            new Event.RefreshEvent(
                                                                    RefreshState.RefreshFinish));
                                        } else {
                                            if (!weakVM.get().isRemoteMessageLoadFinish()) {
                                                getRemoteMessage(weakVM.get());
                                            } else {
                                                weakVM.get()
                                                        .executePageEvent(
                                                                new Event.RefreshEvent(
                                                                        RefreshState
                                                                                .RefreshFinish));
                                            }
                                        }
                                    }
                                };
                        refreshMessageExtendInfo(weakVM.get(), messages, refreshCallback);
                    }
                });
    }

    private static void getRemoteMessage(final ChannelViewModel messageViewModel) {
        if (messageViewModel == null) {
            return;
        }
        WeakReference<ChannelViewModel> weakVM = new WeakReference<>(messageViewModel);
        ChannelIdentifier id = messageViewModel.getChannelIdentifier();
        MessagesQueryParams params = new MessagesQueryParams(id);
        params.setStartTime(messageViewModel.getRefreshSentTime());
        params.setAscending(true);
        params.setPageSize(DEFAULT_REMOTE_COUNT);
        MessagesQuery query = BaseChannel.createMessagesQuery(params);
        query.loadNextPage(
                new OperationHandler<PageData<Message>>() {
                    @Override
                    public void onResult(PageData<Message> pageData, NCError error) {
                        if (weakVM.get() == null) {
                            return;
                        }
                        if (error != null) {
                            weakVM.get()
                                    .executePageEvent(
                                            new Event.RefreshEvent(RefreshState.RefreshFinish));
                            return;
                        }
                        List<Message> messages =
                                (pageData != null && pageData.getData() != null)
                                        ? pageData.getData()
                                        : new ArrayList<>();
                        RefreshCallback<List<Message>> refreshCallback =
                                new RefreshCallback<List<Message>>() {
                                    @Override
                                    public void onSuccess(List<Message> refreshList) {
                                        if (refreshList != null && refreshList.size() > 0) {
                                            weakVM.get().onGetHistoryMessage(refreshList);
                                        }
                                        weakVM.get()
                                                .executePageEvent(
                                                        new Event.RefreshEvent(
                                                                RefreshState.RefreshFinish));
                                    }
                                };
                        refreshMessageExtendInfo(weakVM.get(), messages, refreshCallback);
                    }
                });
    }

    /** Refreshes extended message properties: read-receipt V5 info and reference-message status. */
    private static void refreshMessageExtendInfo(
            ChannelViewModel messageViewModel,
            List<Message> messagesList,
            RefreshCallback<List<Message>> callback) {
        if (messageViewModel == null) {
            callback.onSuccess(messagesList);
            return;
        }
        ChannelIdentifier id = messageViewModel.getChannelIdentifier();
        messageViewModel.getMessageReadReceiptInfoV5(messagesList);
        if (messagesList == null || messagesList.isEmpty()) {
            callback.onSuccess(messagesList);
            return;
        }
        List<String> uIds = new ArrayList<>();
        for (Message message : messagesList) {
            if (message.getContent() instanceof ReferenceMessage) {
                ReferenceMessageStatus status =
                        ((ReferenceMessage) message.getContent()).getReferMsgStatus();
                if (status == ReferenceMessageStatus.MODIFIED
                        || status == ReferenceMessageStatus.DEFAULT) {
                    uIds.add(message.getMessageId());
                }
            }
        }
        if (uIds.isEmpty()) {
            callback.onSuccess(messagesList);
            return;
        }
        List<Message> resultList = new ArrayList<>(messagesList);
        RefreshReferenceMessageParams params = new RefreshReferenceMessageParams(id, uIds);
        BaseChannel.refreshReferenceMessage(
                params,
                new RefreshReferenceMessageHandler() {
                    @Override
                    public void onLocalMessages(List<ReferenceMessageResult> results) {
                        if (results == null || results.isEmpty()) {
                            callback.onSuccess(resultList);
                            return;
                        }
                        HashMap<String, Message> map = new HashMap<>();
                        for (ReferenceMessageResult result : results) {
                            if (result.getMessage() != null) {
                                map.put(result.getMessageId(), result.getMessage());
                            }
                        }
                        for (int i = 0; i < resultList.size(); i++) {
                            Message msg = resultList.get(i);
                            if (msg.getContent() instanceof ReferenceMessage) {
                                Message found = map.get(msg.getMessageId());
                                if (found != null) {
                                    resultList.set(i, found);
                                }
                            }
                        }
                        callback.onSuccess(resultList);
                    }

                    @Override
                    public void onRemoteMessages(List<ReferenceMessageResult> results) {
                        if (results == null || results.isEmpty()) {
                            return;
                        }
                        List<Message> messages = new ArrayList<>();
                        for (ReferenceMessageResult result : results) {
                            if (result.getMessage() != null) {
                                messages.add(result.getMessage());
                            }
                        }
                        RefreshEvent event = new RefreshEvent(messages, true);
                        NCChatUI.refreshMessage(event);
                    }

                    @Override
                    public void onError(NCError error) {
                        callback.onSuccess(resultList);
                    }
                });
    }

    public interface RefreshCallback<T> {
        void onSuccess(T result);
    }

    public interface GetMessageCallback {

        default void onSuccess(List<Message> list, boolean loadOnlyOnce, boolean isHasMoreMsg) {}

        default void onSuccess(List<Message> list, boolean loadOnlyOnce) {
            onSuccess(list, loadOnlyOnce, true);
        }

        void onErrorAsk(List<Message> list);

        void onErrorAlways(List<Message> list);

        void onErrorOnlySuccess();
    }
}
