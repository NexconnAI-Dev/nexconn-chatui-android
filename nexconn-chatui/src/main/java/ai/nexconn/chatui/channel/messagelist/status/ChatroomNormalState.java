package ai.nexconn.chatui.channel.messagelist.status;

import static ai.nexconn.chatui.channel.ChannelViewModel.DEFAULT_COUNT;

import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.query.MessagesQuery;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.model.PageData;
import ai.nexconn.chat.params.MessagesQueryParams;
import ai.nexconn.chatui.channel.ChannelViewModel;
import ai.nexconn.chatui.model.UiMessage;
import android.os.Bundle;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ChatroomNormalState implements IMessageState {

    private StateContext context;

    ChatroomNormalState(StateContext context) {
        this.context = context;
    }

    @Override
    public void init(ChannelViewModel viewModel, Bundle bundle) {
        if (viewModel == null) {
            return;
        }
        WeakReference<ChannelViewModel> reference = new WeakReference<>(viewModel);
        MessagesQueryParams params = new MessagesQueryParams(viewModel.getChannelIdentifier());
        params.setStartTime(0L);
        params.setAscending(false);
        params.setPageSize(DEFAULT_COUNT + 1);
        MessagesQuery query = BaseChannel.createMessagesQuery(params);
        query.loadNextPage(
                new OperationHandler<PageData<Message>>() {
                    @Override
                    public void onResult(PageData<Message> pageData, NCError error) {
                        ChannelViewModel messageViewModel = reference.get();
                        if (messageViewModel == null) {
                            return;
                        }
                        List<Message> messages =
                                (pageData != null && pageData.getData() != null)
                                        ? pageData.getData()
                                        : new ArrayList<>();
                        if (messages.size() > 0) {
                            List<Message> result;
                            if (messages.size() < DEFAULT_COUNT + 1) {
                                result = messages;
                            } else {
                                result = messages.subList(0, DEFAULT_COUNT);
                            }
                            messageViewModel.onGetHistoryMessage(result);
                        }
                        MessageProcessor.processUnread(messageViewModel);
                    }
                });
    }

    @Override
    public void onLoadMore(ChannelViewModel viewModel) {
        // do nothing
    }

    @Override
    public void onRefresh(ChannelViewModel viewModel) {
        MessageProcessor.getLocalMessage(viewModel);
    }

    @Override
    public void onReceived(
            ChannelViewModel viewModel,
            UiMessage message,
            int left,
            boolean hasPackage,
            boolean offline) {
        // default implementation ignored
    }

    @Override
    public void onNewMessageBarClick(ChannelViewModel viewModel) {
        // do nothing
    }

    @Override
    public void onNewMentionMessageBarClick(ChannelViewModel viewModel) {
        // do nothing
    }

    @Override
    public void onScrollToBottom(ChannelViewModel viewModel) {
        // do nothing
    }

    @Override
    public void onHistoryBarClick(ChannelViewModel viewModel) {
        // do nothing
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
