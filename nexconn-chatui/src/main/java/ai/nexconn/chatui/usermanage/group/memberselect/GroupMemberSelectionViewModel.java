package ai.nexconn.chatui.usermanage.group.memberselect;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.base.interfaces.OnPagedDataLoader;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.userinfo.NCUserInfoManager;
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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * ViewModel for group member selection.
 *
 * @since 5.12.2
 */
public class GroupMemberSelectionViewModel extends BaseViewModel implements OnPagedDataLoader {

    private final MutableLiveData<List<ContactModel>> filteredContactsLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ContactModel>> selectedContactsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    protected final GroupMembersPagedHandler groupMembersPagedHandler;
    protected final GroupMembersSearchPagedHandler groupMembersSearchPagedHandler;
    protected final GroupOperationsHandler groupOperationsHandler;

    private boolean isSearchMode = false;

    public GroupMemberSelectionViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ChannelIdentifier conversationIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        arguments, ChatUIConstants.KEY_CHANNEL_IDENTIFIER);
        final ArrayList<String> disableUserIdList =
                arguments.getStringArrayList(ChatUIConstants.KEY_DISABLE_USER_IDS);

        // Initialize GroupMembersPagedHandler to fetch group members
        groupMembersPagedHandler = new GroupMembersPagedHandler(conversationIdentifier);
        groupMembersPagedHandler.addDataChangeListener(
                GroupMembersPagedHandler.KEY_GET_GROUP_MEMBERS,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        List<ContactModel> contactModels =
                                getAllContactsContactModels(groupMemberInfos, disableUserIdList);
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
                                getAllContactsContactModels(groupMemberInfos, disableUserIdList));
                    }
                });

        groupMembersPagedHandler.getGroupMembersByRole(GroupMemberRole.UNDEF);
        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);
    }

    public MutableLiveData<List<ContactModel>> getFilteredContactsLiveData() {
        return filteredContactsLiveData;
    }

    public LiveData<List<ContactModel>> getSelectedContactsLiveData() {
        return selectedContactsLiveData;
    }

    /**
     * Update contact selection state.
     *
     * @param updatedContact the updated contact
     */
    public void updateContact(ContactModel updatedContact) {
        List<ContactModel> selectedList = new ArrayList<>(selectedContactsLiveData.getValue());

        // Use iterator to check and remove or add elements
        Iterator<ContactModel> iterator = selectedList.iterator();
        boolean found = false;

        while (iterator.hasNext()) {
            ContactModel contact = iterator.next();
            if (contact.getBean() instanceof GroupMemberInfo
                    && updatedContact.getBean() instanceof GroupMemberInfo) {
                GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contact.getBean();
                GroupMemberInfo updatedGroupMemberInfo = (GroupMemberInfo) updatedContact.getBean();
                if (groupMemberInfo.getUserId().equals(updatedGroupMemberInfo.getUserId())) {
                    found = true;
                    if (updatedContact.getCheckType() == ContactModel.CheckType.UNCHECKED) {
                        iterator.remove(); // Remove when unchecked
                    }
                    break;
                }
            }
        }

        if (!found && updatedContact.getCheckType() == ContactModel.CheckType.CHECKED) {
            // Not found and checked, add it
            selectedList.add(updatedContact);
        }

        // Update LiveData
        selectedContactsLiveData.postValue(selectedList);
    }

    /**
     * Query contacts.
     *
     * @param query search keyword
     */
    public void queryContacts(String query) {
        if (TextUtils.isEmpty(query)) {
            isSearchMode = false;
            groupMembersPagedHandler.getGroupMembersByRole(GroupMemberRole.UNDEF);
            return;
        }
        isSearchMode = true;
        groupMembersSearchPagedHandler.searchGroupMembers(query);
    }

    /**
     * Add group managers.
     *
     * @param onDataChangeListener data change listener
     */
    public void addGroupManagers(@NonNull OnDataChangeListener<Boolean> onDataChangeListener) {
        List<String> userIds = getSelectUserIds();
        if (userIds.isEmpty()) {
            return;
        }
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_ADD_GROUP_MANAGERS, onDataChangeListener);
        groupOperationsHandler.addGroupManagers(userIds);
    }

    /**
     * Add group follows.
     *
     * @param onDataChangeListener data change listener
     */
    public void addGroupFollows(@NonNull OnDataChangeListener<Boolean> onDataChangeListener) {
        List<String> userIds = getSelectUserIds();
        if (userIds.isEmpty()) {
            return;
        }
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_ADD_GROUP_FOLLOWS, onDataChangeListener);
        groupOperationsHandler.addGroupFollows(userIds);
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
    private List<ContactModel> getAllContactsContactModels(
            List<GroupMemberInfo> groupMemberInfos, ArrayList<String> disableUserIdList) {
        List<ContactModel> contactModels = new ArrayList<>();
        List<String> selectUserIds = getSelectUserIds();
        for (GroupMemberInfo memberInfo : groupMemberInfos) {
            if (NCUserInfoManager.getInstance().getCurrentUserInfo() != null
                    && Objects.equals(
                            NCUserInfoManager.getInstance().getCurrentUserInfo().getUserId(),
                            memberInfo.getUserId())) {
                continue;
            }
            ContactModel.CheckType checkType = ContactModel.CheckType.UNCHECKED;
            if (selectUserIds.contains(memberInfo.getUserId())) {
                checkType = ContactModel.CheckType.CHECKED;
            }
            if (disableUserIdList.contains(memberInfo.getUserId())) {
                checkType = ContactModel.CheckType.DISABLE;
            }
            contactModels.add(
                    ContactModel.obtain(memberInfo, ContactModel.ItemType.CONTENT, checkType));
        }
        return contactModels;
    }

    @NonNull
    private List<String> getSelectUserIds() {
        List<ContactModel> selectedList = selectedContactsLiveData.getValue();
        if (selectedList == null || selectedList.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> userIds = new ArrayList<>();
        for (ContactModel contact : selectedList) {
            if (contact.getBean() instanceof GroupMemberInfo) {
                GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contact.getBean();
                if (groupMemberInfo != null) {
                    userIds.add(groupMemberInfo.getUserId());
                }
            }
        }
        return userIds;
    }
}
