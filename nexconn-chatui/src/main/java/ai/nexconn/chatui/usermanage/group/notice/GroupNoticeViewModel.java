package ai.nexconn.chatui.usermanage.group.notice;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.usermanage.handler.GroupOperationsHandler;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.os.Bundle;
import androidx.annotation.NonNull;

/**
 * ViewModel for group notice/announcement.
 *
 * @since 5.12.0
 */
public class GroupNoticeViewModel extends BaseViewModel {

    protected final GroupOperationsHandler groupOperationsHandler;

    public GroupNoticeViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ChannelIdentifier conversationIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        arguments, ChatUIConstants.KEY_CHANNEL_IDENTIFIER);
        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);
    }

    /**
     * Update group notice.
     *
     * @param groupInfo group info with new notice
     * @param onDataChangeListener data change listener
     */
    @Deprecated
    public void updateGroupNotice(
            GroupInfo groupInfo, OnDataChangeListener<Boolean> onDataChangeListener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_UPDATE_GROUP_INFO, onDataChangeListener);
        groupOperationsHandler.updateGroupInfo(groupInfo);
    }

    /**
     * Update group notice with examination.
     *
     * @param groupInfo group info with new notice
     * @param onDataChangeListener data change listener
     */
    public void updateGroupNotice(
            GroupInfo groupInfo, OnDataChangeEnhancedListener<Boolean> onDataChangeListener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_UPDATE_GROUP_INFO_EXAMINE, onDataChangeListener);
        groupOperationsHandler.updateGroupInfoExamine(groupInfo);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupOperationsHandler.stop();
    }
}
