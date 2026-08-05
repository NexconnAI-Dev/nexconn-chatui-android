package ai.nexconn.chatui.channel.messagelist.processor;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.OpenChannel;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.ErrorHandler;
import ai.nexconn.chat.params.EnterOpenChannelParams;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.ChannelViewModel;
import ai.nexconn.chatui.channel.event.page.ShowWarningDialogEvent;
import ai.nexconn.chatui.channel.feature.mention.NCMentionManager;
import ai.nexconn.chatui.channel.messagelist.status.StateContext;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.route.RouteUtils;
import android.content.Context;
import android.os.Bundle;

public class OpenChannelProcessor extends BaseChannelProcessor {
    private static final String TAG = "ChatRoomBusinessProcess";
    private int NC_chatRoom_first_pull_message_count;

    @Override
    public void init(final ChannelViewModel messageViewModel, Bundle bundle) {
        mState = new StateContext(StateContext.CHATROOM_NORMAL_STATE);
        mState.init(messageViewModel, bundle);
        NC_chatRoom_first_pull_message_count =
                NCChatUIConfig.channelConfig().NC_chatroom_first_pull_message_count;
        boolean createIfNotExist = bundle.getBoolean(RouteUtils.CREATE_CHATROOM, true);
        String extra = bundle.getString("extra", null);

        OpenChannel openChannel = new OpenChannel(messageViewModel.getCurTargetId());
        EnterOpenChannelParams params = new EnterOpenChannelParams();
        params.setMessageCount(getHistoryMessageCount());
        params.setExtra(extra);

        if (createIfNotExist) {
            // TODO: [nexconn-migration] joinChatRoom (auto-create) vs enterChannel
            // (joinExistChatRoom) may behave differently
            openChannel.enterChannel(
                    params,
                    new ErrorHandler() {
                        @Override
                        public void onError(NCError error) {
                            if (error == null) {
                                RLog.i(
                                        TAG,
                                        "enterChannel onSuccess : "
                                                + messageViewModel.getCurTargetId());
                            } else {
                                RLog.e(TAG, "enterChannel onError : " + error);
                                int code = error.getCode();
                                if (code == 30002 || code == 30001) {
                                    messageViewModel.executePageEvent(
                                            new ShowWarningDialogEvent(
                                                    messageViewModel
                                                            .getApplication()
                                                            .getString(
                                                                    R.string
                                                                            .nc_notice_network_unavailable)));
                                } else {
                                    messageViewModel.executePageEvent(
                                            new ShowWarningDialogEvent(
                                                    messageViewModel
                                                            .getApplication()
                                                            .getString(
                                                                    R.string
                                                                            .nc_join_chatroom_failure)));
                                }
                            }
                        }
                    });
        } else {
            openChannel.enterChannel(
                    params,
                    new ErrorHandler() {
                        @Override
                        public void onError(NCError error) {
                            if (error == null) {
                                RLog.i(
                                        TAG,
                                        "enterChannel onSuccess : "
                                                + messageViewModel.getCurTargetId());
                            } else {
                                RLog.e(TAG, "enterChannel onError : " + error);
                                int code = error.getCode();
                                if (code == 30002 || code == 30001) {
                                    messageViewModel.executePageEvent(
                                            new ShowWarningDialogEvent(
                                                    messageViewModel
                                                            .getApplication()
                                                            .getString(
                                                                    R.string
                                                                            .nc_notice_network_unavailable)));
                                } else {
                                    messageViewModel.executePageEvent(
                                            new ShowWarningDialogEvent(
                                                    messageViewModel
                                                            .getApplication()
                                                            .getString(
                                                                    R.string
                                                                            .nc_join_chatroom_failure)));
                                }
                            }
                        }
                    });
        }
        super.init(messageViewModel, bundle);
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

    @Override
    public int getHistoryMessageCount() {
        int count = NC_chatRoom_first_pull_message_count;
        if (count == 0) {
            return 10;
        } else if (count == -1) {
            return 0;
        } else {
            return count;
        }
    }

    @Override
    public void onDestroy(ChannelViewModel viewModel) {
        String mTargetId = viewModel.getCurTargetId();
        RLog.i(TAG, "exitChannel : " + mTargetId);
        OpenChannel openChannel = new OpenChannel(mTargetId);
        openChannel.exitChannel(
                "",
                new ErrorHandler() {
                    @Override
                    public void onError(NCError error) {
                        if (error == null) {
                            RLog.i(TAG, "exitChannel onSuccess : " + mTargetId);
                        } else {
                            RLog.e(TAG, "exitChannel onError : " + error);
                        }
                    }
                });
    }
}
