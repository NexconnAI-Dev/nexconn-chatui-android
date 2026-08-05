package ai.nexconn.chatui.channel.messagelist.processor;

import static ai.nexconn.chatui.channel.ChannelViewModel.DEFAULT_COUNT;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.UnknownMessage;
import ai.nexconn.chat.model.ConnectionStatus;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.channel.ChannelViewModel;
import ai.nexconn.chatui.channel.messagelist.status.StateContext;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.route.RouteUtils;
import android.content.Context;
import android.os.Bundle;
import java.util.List;

/** Base class for handling common logic across direct, group, and discussion channels. */
public abstract class BaseChannelProcessor implements IChannelProcessor {

    protected StateContext mState;

    @Override
    public void init(ChannelViewModel messageViewModel, Bundle bundle) {
        int state;
        if (bundle != null) {
            long indexTime = bundle.getLong(RouteUtils.INDEX_MESSAGE_TIME, 0);
            if (indexTime > 0) {
                state = StateContext.HISTORY_STATE;
            } else {
                state = StateContext.NORMAL_STATE;
            }
        } else {
            state = StateContext.NORMAL_STATE;
        }
        mState = new StateContext(state);
        mState.init(messageViewModel, bundle);
    }

    @Override
    public boolean onReceived(
            ChannelViewModel viewModel,
            UiMessage message,
            int left,
            boolean hasPackage,
            boolean offline) {
        if (mState != null) {
            mState.onReceived(viewModel, message, left, hasPackage, offline);
        }
        return false;
    }

    @Override
    public boolean onReceivedCmd(ChannelViewModel messageViewModel, Message message) {
        return !(message.getContent() instanceof UnknownMessage);
    }

    @Override
    public void onMessageItemClick(UiMessage uiMessage) {
        // default implementation ignored
    }

    @Override
    public boolean onMessageItemLongClick(UiMessage uiMessage) {
        return false;
    }

    @Override
    public void onUserPortraitClick(
            Context context, ChannelType conversationType, UserInfo userInfo, String targetId) {
        // default implementation ignored
    }

    @Override
    public boolean onUserPortraitLongClick(
            Context context, ChannelType conversationType, UserInfo userInfo, String targetId) {
        return false;
    }

    @Override
    public boolean onBackPressed(ChannelViewModel viewModel) {
        return false;
    }

    @Override
    public void onDestroy(ChannelViewModel viewModel) {
        // default implementation ignored
    }

    @Override
    public void onExistUnreadMessage(
            ChannelViewModel viewModel, long sentTime, int unreadMessageCount) {
        // default implementation ignored
    }

    @Override
    public void onMessageReceiptRequest(
            ChannelViewModel viewModel,
            ChannelType conversationType,
            String targetId,
            String messageUId) {
        // default implementation ignored
    }

    @Override
    public void onLoadMessage(ChannelViewModel viewModel, List<Message> messages) {
        // default implementation ignored
    }

    /** Checks the read status; if persisted state exists, sends a read receipt. */
    @Override
    public void onConnectStatusChange(ChannelViewModel viewModel, ConnectionStatus status) {
        // default implementation ignored
    }

    @Override
    public void onResume(ChannelViewModel viewModel) {
        // default implementation ignored
    }

    /**
     * @return the number of history messages to fetch on init; currently only chatrooms need to
     *     override this
     */
    public int getHistoryMessageCount() {
        return DEFAULT_COUNT + 1;
    }

    @Override
    public void onLoadMore(ChannelViewModel viewModel) {
        if (mState != null) {
            mState.onLoadMore(viewModel);
        }
    }

    @Override
    public void onClearMessage(ChannelViewModel viewModel) {
        if (mState != null) {
            mState.onClearMessage(viewModel);
        }
    }

    @Override
    public void onRefresh(ChannelViewModel viewModel) {
        if (mState != null) {
            mState.onRefresh(viewModel);
        }
    }

    @Override
    public void newMessageBarClick(ChannelViewModel viewModel) {
        if (mState != null) {
            mState.onNewMessageBarClick(viewModel);
        }
    }

    @Override
    public void unreadBarClick(ChannelViewModel viewModel) {
        if (mState != null) {
            mState.onHistoryBarClick(viewModel);
        }
    }

    @Override
    public void newMentionMessageBarClick(ChannelViewModel viewModel) {
        if (mState != null) {
            mState.newMentionMessageBarClick(viewModel);
        }
    }

    @Override
    public boolean isNormalState(ChannelViewModel viewModel) {
        if (mState == null) {
            return true;
        }
        return mState.isNormalState(viewModel);
    }

    @Override
    public boolean isHistoryState(ChannelViewModel viewModel) {
        return mState.isHistoryState(viewModel);
    }

    @Override
    public void onScrollToBottom(ChannelViewModel viewModel) {
        if (mState != null) {
            mState.onScrollToBottom(viewModel);
        }
    }
}
