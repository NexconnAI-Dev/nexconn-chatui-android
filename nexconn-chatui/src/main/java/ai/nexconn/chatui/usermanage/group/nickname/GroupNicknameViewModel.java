package ai.nexconn.chatui.usermanage.group.nickname;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.usermanage.handler.GroupInfoHandler;
import ai.nexconn.chatui.usermanage.handler.GroupOperationsHandler;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import java.util.Arrays;
import java.util.List;

/**
 * ViewModel for editing group nickname.
 *
 * @since 5.12.0
 */
public class GroupNicknameViewModel extends BaseViewModel {

    private final MutableLiveData<GroupMemberInfo> myMemberInfoLiveData = new MutableLiveData<>();

    protected final GroupInfoHandler groupInfoHandler;
    protected final GroupOperationsHandler groupOperationsHandler;
    private final String userId;

    public GroupNicknameViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ChannelIdentifier conversationIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        arguments, ChatUIConstants.KEY_CHANNEL_IDENTIFIER);

        groupInfoHandler = new GroupInfoHandler(conversationIdentifier);
        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);

        userId = arguments.getString(ChatUIConstants.KEY_USER_ID);
        groupInfoHandler.addDataChangeListener(
                GroupInfoHandler.KEY_GET_GROUP_MEMBERS,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        if (groupMemberInfos != null && !groupMemberInfos.isEmpty()) {
                            myMemberInfoLiveData.postValue(groupMemberInfos.get(0));
                        }
                    }
                });
        groupInfoHandler.getGroupMembers(Arrays.asList(userId));
    }

    public MutableLiveData<GroupMemberInfo> getMyMemberInfoLiveData() {
        return myMemberInfoLiveData;
    }

    /**
     * Update group nickname.
     *
     * @param newNickName new group nickname
     * @param onDataChangeListener data change listener
     */
    @Deprecated
    public void updateGroupNickName(
            String newNickName, OnDataChangeListener<Boolean> onDataChangeListener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_SET_GROUP_MEMBER_INFO, onDataChangeListener);
        groupOperationsHandler.setGroupMemberInfo(userId, newNickName, null);
    }

    /**
     * Update group member nickname with examination.
     *
     * @param newNickName new group nickname
     * @param onDataChangeListener data change listener
     */
    public void updateGroupNickName(
            String newNickName, OnDataChangeEnhancedListener<Boolean> onDataChangeListener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_SET_GROUP_MEMBER_INFO_EXAMINE, onDataChangeListener);
        groupOperationsHandler.setGroupMemberInfoExamine(userId, newNickName, null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupInfoHandler.stop();
        groupOperationsHandler.stop();
    }
}
