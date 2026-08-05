package ai.nexconn.chatui.usermanage.group.mention;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.base.interfaces.OnPagedDataLoader;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.usermanage.handler.GroupInfoHandler;
import ai.nexconn.chatui.usermanage.handler.GroupMembersPagedHandler;
import ai.nexconn.chatui.usermanage.handler.GroupMembersSearchPagedHandler;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ViewModel for group mention member selection.
 *
 * @since 5.12.2
 */
public class GroupMentionViewModel extends BaseViewModel implements OnPagedDataLoader {

    private final MutableLiveData<List<ContactModel>> allContactsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ContactModel>> filteredContactsLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<GroupMemberRole> mentionAllRoleLiveData = new MutableLiveData<>();

    protected final GroupMembersPagedHandler groupMembersPagedHandler;
    protected final GroupMembersSearchPagedHandler groupMembersSearchPagedHandler;
    private final GroupInfoHandler groupInfoHandler;
    private final String currentUserId;
    private final ChannelIdentifier conversationIdentifier;

    private boolean isSearchMode = false;

    public GroupMentionViewModel(@NonNull Bundle arguments) {
        super(arguments);
        conversationIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        arguments, ChatUIConstants.KEY_CHANNEL_IDENTIFIER);
        currentUserId = NCEngine.getCurrentUserId();

        // Initialize GroupMembersPagedHandler to fetch group members
        groupMembersPagedHandler = new GroupMembersPagedHandler(conversationIdentifier);
        groupMembersPagedHandler.addDataChangeListener(
                GroupMembersPagedHandler.KEY_GET_GROUP_MEMBERS,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        List<ContactModel> contactModels =
                                sortAndCategorizeContacts(groupMemberInfos);
                        allContactsLiveData.postValue(contactModels);
                        if (!isSearchMode) {
                            filteredContactsLiveData.postValue(contactModels);
                        }
                    }
                });

        groupMembersSearchPagedHandler = new GroupMembersSearchPagedHandler(conversationIdentifier);
        groupMembersSearchPagedHandler.addDataChangeListener(
                GroupMembersSearchPagedHandler.KEY_SEARCH_GROUP_MEMBERS,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        filteredContactsLiveData.postValue(
                                sortAndCategorizeContacts(groupMemberInfos));
                    }
                });

        groupInfoHandler = new GroupInfoHandler(conversationIdentifier);
        groupInfoHandler.addDataChangeListener(
                GroupInfoHandler.KEY_GET_GROUP_MEMBERS,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        GroupMemberRole role = null;
                        if (groupMemberInfos != null && !groupMemberInfos.isEmpty()) {
                            role = groupMemberInfos.get(0).getRole();
                        }
                        if (role != null) {
                            mentionAllRoleLiveData.postValue(role);
                        }
                    }
                });
        if (!TextUtils.isEmpty(currentUserId)) {
            groupInfoHandler.getGroupMembers(Collections.singletonList(currentUserId));
        }
    }

    public LiveData<List<ContactModel>> getFilteredContactsLiveData() {
        return filteredContactsLiveData;
    }

    public LiveData<GroupMemberRole> getMentionAllRoleLiveData() {
        return mentionAllRoleLiveData;
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

    void refreshGroupManagerList() {
        groupMembersPagedHandler.getGroupMembersByRole(GroupMemberRole.UNDEF);
    }

    @NonNull
    private List<ContactModel> sortAndCategorizeContacts(List<GroupMemberInfo> groupMemberInfos) {
        if (groupMemberInfos == null || groupMemberInfos.isEmpty()) {
            return Collections.emptyList();
        }

        // Filter out current user
        List<ContactModel> contactModels = new ArrayList<>();
        for (GroupMemberInfo groupMemberInfo : groupMemberInfos) {
            if (TextUtils.isEmpty(currentUserId)
                    || !currentUserId.equals(groupMemberInfo.getUserId())) {
                // Add contact ContactModel
                ContactModel<GroupMemberInfo> contactModel =
                        ContactModel.obtain(
                                groupMemberInfo,
                                ContactModel.ItemType.CONTENT,
                                ContactModel.CheckType.NONE);
                contactModel.putExtra(GroupMentionFragment.class.getSimpleName());
                contactModels.add(contactModel);
            }
        }

        return contactModels;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupMembersPagedHandler.stop();
        groupMembersSearchPagedHandler.stop();
        groupInfoHandler.stop();
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
}
