package ai.nexconn.chatui.usermanage.group.transfer;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chat.channel.model.LeaveGroupConfig;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.base.interfaces.OnPagedDataLoader;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.usermanage.handler.GroupMembersPagedHandler;
import ai.nexconn.chatui.usermanage.handler.GroupMembersSearchPagedHandler;
import ai.nexconn.chatui.usermanage.handler.GroupOperationsHandler;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for group ownership transfer.
 *
 * @since 5.12.2
 */
public class GroupTransferViewModel extends BaseViewModel implements OnPagedDataLoader {

    private final MutableLiveData<List<ContactModel>> allContactsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ContactModel>> filteredContactsLiveData =
            new MutableLiveData<>();

    protected final GroupMembersPagedHandler groupMembersPagedHandler;
    protected final GroupMembersSearchPagedHandler groupMembersSearchPagedHandler;
    protected final GroupOperationsHandler groupOperationsHandler;

    private boolean isSearchMode = false;
    private final String groupId;

    public GroupTransferViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ChannelIdentifier conversationIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        arguments, ChatUIConstants.KEY_CHANNEL_IDENTIFIER);
        groupId = conversationIdentifier.getChannelId();
        // Initialize GroupMembersPagedHandler to fetch group members
        groupMembersPagedHandler = new GroupMembersPagedHandler(conversationIdentifier);
        groupMembersPagedHandler.addDataChangeListener(
                GroupMembersPagedHandler.KEY_GET_GROUP_MEMBERS,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        List<ContactModel> contactModels =
                                getAllContactsContactModels(groupMemberInfos);
                        allContactsLiveData.postValue(contactModels);
                        filteredContactsLiveData.postValue(contactModels);
                    }
                });

        groupMembersSearchPagedHandler = new GroupMembersSearchPagedHandler(conversationIdentifier);
        groupMembersSearchPagedHandler.addDataChangeListener(
                GroupMembersSearchPagedHandler.KEY_SEARCH_GROUP_MEMBERS,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        filteredContactsLiveData.postValue(
                                getAllContactsContactModels(groupMemberInfos));
                    }
                });

        groupMembersPagedHandler.getGroupMembersByRole(GroupMemberRole.UNDEF);
        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);
    }

    public LiveData<List<ContactModel>> getFilteredContactsLiveData() {
        return filteredContactsLiveData;
    }

    /**
     * Query group members.
     *
     * @param query search keyword
     */
    public void queryGroupMembers(String query) {
        if (TextUtils.isEmpty(query)) {
            isSearchMode = false;
            groupMembersPagedHandler.getGroupMembersByRole(GroupMemberRole.UNDEF);
            return;
        }
        isSearchMode = true;
        groupMembersSearchPagedHandler.searchGroupMembers(query);
    }

    /**
     * Transfer group ownership.
     *
     * @param groupMemberInfo target group member info
     */
    public void transferGroupOwner(
            GroupMemberInfo groupMemberInfo, OnDataChangeListener<Boolean> listener) {
        groupOperationsHandler.addDataChangeListener(
                GroupOperationsHandler.KEY_TRANSFER_GROUP_OWNER, listener);
        groupOperationsHandler.transferGroupOwner(
                groupMemberInfo.getUserId(), false, new LeaveGroupConfig());
    }

    String getGroupId() {
        return groupId;
    }

    @Override
    public void loadNext(OnDataChangeListener<Boolean> listener) {
        if (isSearchMode) {
            groupMembersSearchPagedHandler.loadNext(listener);
        } else {
            groupMembersPagedHandler.loadNext(listener);
        }
    }

    @Override
    public boolean hasNext() {
        if (isSearchMode) {
            return groupMembersSearchPagedHandler.hasNext();
        } else {
            return groupMembersPagedHandler.hasNext();
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupMembersPagedHandler.stop();
        groupOperationsHandler.stop();
        groupMembersSearchPagedHandler.stop();
    }

    @NonNull
    private List<ContactModel> getAllContactsContactModels(List<GroupMemberInfo> groupMemberInfos) {
        List<ContactModel> contactModels = new ArrayList<>();
        for (GroupMemberInfo memberInfo : groupMemberInfos) {
            if (memberInfo.getRole() == GroupMemberRole.OWNER) {
                continue;
            }
            contactModels.add(
                    ContactModel.obtain(
                            memberInfo,
                            ContactModel.ItemType.CONTENT,
                            ContactModel.CheckType.NONE));
        }
        return contactModels;
    }
}
