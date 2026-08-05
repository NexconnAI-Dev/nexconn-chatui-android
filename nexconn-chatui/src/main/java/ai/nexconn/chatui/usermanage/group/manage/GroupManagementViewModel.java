package ai.nexconn.chatui.usermanage.group.manage;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.usermanage.handler.GroupInfoHandler;
import ai.nexconn.chatui.usermanage.handler.GroupOperationsHandler;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * ViewModel for group management.
 *
 * @since 5.12.2
 */
public class GroupManagementViewModel extends BaseViewModel {

    private final MutableLiveData<GroupInfo> groupInfoLiveData = new MutableLiveData<>();

    protected final GroupInfoHandler groupInfoHandler;
    protected final GroupOperationsHandler groupOperationsHandler;

    public GroupManagementViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ChannelIdentifier conversationIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        arguments, ChatUIConstants.KEY_CHANNEL_IDENTIFIER);
        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);

        groupInfoHandler = new GroupInfoHandler(conversationIdentifier);
        groupInfoHandler.addDataChangeListener(
                GroupInfoHandler.KEY_GROUP_INFO, groupInfoLiveData::postValue);

        refreshGroupInfo();
    }

    public LiveData<GroupInfo> getGroupInfoLiveData() {
        return groupInfoLiveData;
    }

    /**
     * Update group info.
     *
     * @param groupInfo group info
     * @param listener data change listener
     */
    @Deprecated
    public void updateGroupInfo(GroupInfo groupInfo, OnDataChangeListener<Boolean> listener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_UPDATE_GROUP_INFO, listener);
        groupOperationsHandler.updateGroupInfo(groupInfo);
    }

    /**
     * Update group info with examination.
     *
     * @param groupInfo group info
     * @param listener data change listener
     */
    public void updateGroupInfo(
            GroupInfo groupInfo, OnDataChangeEnhancedListener<Boolean> listener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_UPDATE_GROUP_INFO_EXAMINE, listener);
        groupOperationsHandler.updateGroupInfoExamine(groupInfo);
    }

    void refreshGroupInfo() {
        groupInfoHandler.getGroupsInfo();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupInfoHandler.stop();
        groupOperationsHandler.stop();
    }
}
