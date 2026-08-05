package ai.nexconn.chatui.usermanage.group.application;

import ai.nexconn.chat.channel.model.GroupApplicationDirection;
import ai.nexconn.chat.channel.model.GroupApplicationInfo;
import ai.nexconn.chat.channel.model.GroupApplicationStatus;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.base.interfaces.OnPagedDataLoader;
import ai.nexconn.chatui.usermanage.handler.GroupApplicationOperationsHandler;
import ai.nexconn.chatui.usermanage.handler.GroupApplicationsPagedHandler;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for group applications.
 *
 * @since 5.12.2
 */
public class GroupApplicationsViewModel extends BaseViewModel {

    private final MutableLiveData<List<GroupApplicationInfo>> groupApplicationInfoListLiveData =
            new MutableLiveData<>(new ArrayList<>());

    protected final GroupApplicationsPagedHandler groupApplicationsPagedHandler;
    protected final GroupApplicationOperationsHandler groupApplicationOperationsHandler;

    public GroupApplicationsViewModel(@NonNull Bundle arguments) {
        super(arguments);
        int maxCount = arguments.getInt(ChatUIConstants.KEY_MAX_COUNT_PAGED, 50);
        int validatedMaxCountPaged = Math.max(1, Math.min(100, maxCount));
        groupApplicationsPagedHandler = new GroupApplicationsPagedHandler(validatedMaxCountPaged);
        groupApplicationsPagedHandler.addDataChangeListener(
                GroupApplicationsPagedHandler.KEY_GET_GROUP_APPLICATIONS,
                new SafeDataHandler<List<GroupApplicationInfo>>() {
                    @Override
                    public void onDataChange(List<GroupApplicationInfo> groupApplicationInfos) {
                        groupApplicationInfoListLiveData.postValue(groupApplicationInfos);
                    }
                });

        groupApplicationOperationsHandler = new GroupApplicationOperationsHandler();

        GroupApplicationDirection[] directions =
                new GroupApplicationDirection[] {
                    GroupApplicationDirection.APPLICATION_SENT,
                    GroupApplicationDirection.INVITATION_SENT,
                    GroupApplicationDirection.APPLICATION_RECEIVED,
                    GroupApplicationDirection.INVITATION_RECEIVED
                };
        GroupApplicationStatus[] status =
                new GroupApplicationStatus[] {
                    GroupApplicationStatus.ADMIN_UNHANDLED,
                    GroupApplicationStatus.ADMIN_REFUSED,
                    GroupApplicationStatus.JOINED,
                    GroupApplicationStatus.EXPIRED,
                    GroupApplicationStatus.INVITEE_REFUSED,
                    GroupApplicationStatus.INVITEE_UNHANDLED
                };
        getGroupApplications(directions, status);
    }

    public MutableLiveData<List<GroupApplicationInfo>> getGroupApplicationInfoListLiveData() {
        return groupApplicationInfoListLiveData;
    }

    /**
     * Get group applications.
     *
     * @param directions application directions
     * @param status application statuses
     */
    public void getGroupApplications(
            GroupApplicationDirection[] directions, GroupApplicationStatus[] status) {
        groupApplicationsPagedHandler.getGroupApplications(directions, status);
    }

    /**
     * Accept a group invite.
     *
     * @param groupId group ID
     * @param inviterId inviter ID
     * @param onDataChangeListener data change listener
     */
    public void acceptGroupInvite(
            String groupId, String inviterId, OnDataChangeListener<Boolean> onDataChangeListener) {
        groupApplicationOperationsHandler.replaceDataChangeListener(
                GroupApplicationOperationsHandler.KEY_ACCEPT_GROUP_INVITE, onDataChangeListener);
        groupApplicationOperationsHandler.acceptGroupInvite(groupId, inviterId);
    }

    /**
     * Refuse a group invite.
     *
     * @param groupId group ID
     * @param inviterId inviter ID
     * @param reason rejection reason
     * @param onDataChangeListener data change listener
     */
    public void refuseGroupInvite(
            String groupId,
            String inviterId,
            String reason,
            OnDataChangeListener<Boolean> onDataChangeListener) {
        groupApplicationOperationsHandler.replaceDataChangeListener(
                GroupApplicationOperationsHandler.KEY_REFUSE_GROUP_INVITE, onDataChangeListener);
        groupApplicationOperationsHandler.refuseGroupInvite(groupId, inviterId, reason);
    }

    /**
     * Accept a group application.
     *
     * @param groupId group ID
     * @param inviterId inviter ID
     * @param applicantId applicant ID
     * @param onDataChangeListener data change listener
     */
    public void acceptGroupApplication(
            String groupId,
            String inviterId,
            String applicantId,
            OnDataChangeListener<Integer> onDataChangeListener) {
        groupApplicationOperationsHandler.replaceDataChangeListener(
                GroupApplicationOperationsHandler.KEY_ACCEPT_GROUP_APPLICATION,
                onDataChangeListener);
        groupApplicationOperationsHandler.acceptGroupApplication(groupId, inviterId, applicantId);
    }

    /**
     * Refuse a group application.
     *
     * @param groupId group ID
     * @param inviterId inviter ID
     * @param applicantId applicant ID
     * @param reason rejection reason
     * @param onDataChangeListener data change listener
     */
    public void refuseGroupApplication(
            String groupId,
            String inviterId,
            String applicantId,
            String reason,
            OnDataChangeListener<Boolean> onDataChangeListener) {
        groupApplicationOperationsHandler.replaceDataChangeListener(
                GroupApplicationOperationsHandler.KEY_REFUSE_GROUP_APPLICATION,
                onDataChangeListener);
        groupApplicationOperationsHandler.refuseGroupApplication(
                groupId, inviterId, applicantId, reason);
    }

    OnPagedDataLoader getOnPageDataLoader() {
        return groupApplicationsPagedHandler;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupApplicationsPagedHandler.stop();
        groupApplicationOperationsHandler.stop();
    }
}
