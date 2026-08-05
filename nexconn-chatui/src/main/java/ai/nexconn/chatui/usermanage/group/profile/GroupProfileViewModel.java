package ai.nexconn.chatui.usermanage.group.profile;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.ChannelNoDisturbLevel;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chat.channel.model.LeaveGroupConfig;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.usermanage.handler.ChannelOperationsHandler;
import ai.nexconn.chatui.usermanage.handler.ChannelStatusHandler;
import ai.nexconn.chatui.usermanage.handler.GroupInfoHandler;
import ai.nexconn.chatui.usermanage.handler.GroupMembersPagedHandler;
import ai.nexconn.chatui.usermanage.handler.GroupOperationsHandler;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.Arrays;
import java.util.List;

/**
 * ViewModel for group profile.
 *
 * @since 5.12.0
 */
public class GroupProfileViewModel extends BaseViewModel {

    private final MutableLiveData<List<GroupMemberInfo>> GroupMemberInfosLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<GroupMemberInfo> myMemberInfoLiveData = new MutableLiveData<>();
    private final MutableLiveData<GroupInfo> groupInfoLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isConversationTopLiveData = new MutableLiveData<>();
    private final MutableLiveData<ChannelNoDisturbLevel> conversationNotificationStatusLiveData =
            new MutableLiveData<>();

    protected final GroupInfoHandler groupInfoHandler;
    protected final GroupOperationsHandler groupOperationsHandler;
    protected final GroupMembersPagedHandler groupMembersPagedHandler;

    /**
     * @since 5.12.2
     */
    protected final ChannelStatusHandler conversationStatusHandler;

    /**
     * @since 5.12.2
     */
    protected final ChannelOperationsHandler conversationOperationsHandler;

    public GroupProfileViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ChannelIdentifier conversationIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        arguments, ChatUIConstants.KEY_CHANNEL_IDENTIFIER);
        final int maxCount =
                Math.max(
                        5,
                        Math.min(
                                50,
                                arguments.getInt(
                                        ChatUIConstants.KEY_MAX_MEMBER_COUNT_DISPLAY, 30)));

        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);

        groupInfoHandler = new GroupInfoHandler(conversationIdentifier);
        groupInfoHandler.addDataChangeListener(
                GroupInfoHandler.KEY_GROUP_INFO, groupInfoLiveData::postValue);
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

        groupMembersPagedHandler = new GroupMembersPagedHandler(conversationIdentifier, maxCount);
        groupMembersPagedHandler.addDataChangeListener(
                GroupMembersPagedHandler.KEY_GET_GROUP_MEMBERS,
                GroupMemberInfosLiveData::postValue);

        conversationStatusHandler = new ChannelStatusHandler(conversationIdentifier);
        conversationStatusHandler.addDataChangeListener(
                ChannelStatusHandler.KEY_GET_CONVERSATION_TOP_STATUS,
                new SafeDataHandler<Boolean>() {
                    @Override
                    public void onDataChange(Boolean isTop) {
                        isConversationTopLiveData.postValue(isTop);
                    }
                });
        conversationStatusHandler.addDataChangeListener(
                ChannelStatusHandler.KEY_GET_CONVERSATION_NOTIFICATION_STATUS,
                new SafeDataHandler<ChannelNoDisturbLevel>() {
                    @Override
                    public void onDataChange(ChannelNoDisturbLevel conversationNotificationStatus) {
                        conversationNotificationStatusLiveData.postValue(
                                conversationNotificationStatus);
                    }
                });

        conversationOperationsHandler = new ChannelOperationsHandler(conversationIdentifier);

        refreshGroupInfo();
    }

    public LiveData<GroupInfo> getGroupInfoLiveData() {
        return groupInfoLiveData;
    }

    public LiveData<List<GroupMemberInfo>> getGroupMemberInfosLiveData() {
        return GroupMemberInfosLiveData;
    }

    public MutableLiveData<GroupMemberInfo> getMyMemberInfoLiveData() {
        return myMemberInfoLiveData;
    }

    /**
     * @since 5.12.2
     */
    public MutableLiveData<Boolean> getIsConversationTopLiveData() {
        return isConversationTopLiveData;
    }

    /**
     * @since 5.12.2
     */
    public MutableLiveData<ChannelNoDisturbLevel> getConversationNotificationStatusLiveData() {
        return conversationNotificationStatusLiveData;
    }

    /**
     * Dismiss the group.
     *
     * @param listener data change listener
     */
    public void dismissGroup(OnDataChangeListener<Boolean> listener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_DISMISS_GROUP, listener);
        groupOperationsHandler.dismissGroup();
    }

    /**
     * Quit the group.
     *
     * @param listener data change listener
     */
    public void quitGroup(OnDataChangeListener<Boolean> listener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_QUIT_GROUP, listener);
        groupOperationsHandler.quitGroup(new LeaveGroupConfig(true, true, true));
    }

    /**
     * Set conversation do-not-disturb status.
     *
     * @param conversationNotificationStatus conversation notification status
     * @param listener data change listener
     * @since 5.12.2
     */
    public void setConversationNotificationStatus(
            ChannelNoDisturbLevel conversationNotificationStatus,
            OnDataChangeListener<ChannelNoDisturbLevel> listener) {
        conversationOperationsHandler.replaceDataChangeListener(
                ChannelOperationsHandler.KEY_SET_CONVERSATION_NOTIFICATION_STATUS, listener);
        conversationOperationsHandler.setConversationNotificationStatus(
                conversationNotificationStatus);
    }

    /**
     * Set conversation pin-to-top status.
     *
     * @param isTop whether to pin the conversation
     * @param listener data change listener
     * @since 5.12.2
     */
    public void setConversationTopStatus(boolean isTop, OnDataChangeListener<Boolean> listener) {
        conversationOperationsHandler.replaceDataChangeListener(
                ChannelOperationsHandler.KEY_SET_CONVERSATION_TO_TOP, listener);
        conversationOperationsHandler.setConversationToTop(isTop);
    }

    void refreshGroupInfo() {
        conversationStatusHandler.getConversationTopStatus();
        conversationStatusHandler.getConversationNotificationStatus();
        groupMembersPagedHandler.getGroupMembersByRole(GroupMemberRole.UNDEF);

        groupInfoHandler.getGroupsInfo();
        String userId = NCEngine.getCurrentUserId();
        if (userId != null) {
            groupInfoHandler.getGroupMembers(Arrays.asList(userId));
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupInfoHandler.stop();
        groupOperationsHandler.stop();
        groupMembersPagedHandler.stop();
        conversationStatusHandler.stop();
        conversationOperationsHandler.stop();
    }
}
