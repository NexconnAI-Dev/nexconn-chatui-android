package ai.nexconn.chatui.usermanage.group.managerlist;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.usermanage.handler.GroupMembersFullHandler;
import ai.nexconn.chatui.usermanage.handler.GroupOperationsHandler;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ViewModel for group manager list.
 *
 * @since 5.12.2
 */
public class GroupManagerListViewModel extends BaseViewModel {

    private final MutableLiveData<List<ContactModel>> allGroupManagersLiveData =
            new MutableLiveData<>();

    protected final GroupMembersFullHandler groupMembersFullHandler;
    protected final GroupOperationsHandler groupOperationsHandler;

    public GroupManagerListViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ChannelIdentifier conversationIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        arguments, ChatUIConstants.KEY_CHANNEL_IDENTIFIER);
        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);
        groupMembersFullHandler = new GroupMembersFullHandler(conversationIdentifier);
        groupMembersFullHandler.addDataChangeListener(
                GroupMembersFullHandler.KEY_GET_ALL_GROUP_MEMBERS_BY_ROLES,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        allGroupManagersLiveData.postValue(
                                sortAndCategorizeContacts(groupMemberInfos));
                    }
                });
    }

    public MutableLiveData<List<ContactModel>> getAllGroupManagersLiveData() {
        return allGroupManagersLiveData;
    }

    public void removeGroupManager(List<String> userIds, OnDataChangeListener<Boolean> listener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_REMOVE_GROUP_MANAGERS, listener);
        groupOperationsHandler.removeGroupManagers(userIds);
    }

    void refreshGroupManagerList() {
        groupMembersFullHandler.getAllGroupMembersByRole(GroupMemberRole.ADMIN);
    }

    @NonNull
    private List<ContactModel> sortAndCategorizeContacts(List<GroupMemberInfo> groupMemberInfos) {
        if (groupMemberInfos == null || groupMemberInfos.isEmpty()) {
            return Collections.emptyList();
        }
        List<ContactModel> contactModels = new ArrayList<>();
        for (GroupMemberInfo groupMemberInfo : groupMemberInfos) {
            // Add contact ContactModel
            contactModels.add(
                    ContactModel.obtain(
                            groupMemberInfo,
                            ContactModel.ItemType.CONTENT,
                            ContactModel.CheckType.NONE));
        }

        return contactModels;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupMembersFullHandler.stop();
        groupOperationsHandler.stop();
    }
}
