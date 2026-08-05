package ai.nexconn.chatui.usermanage.group.add;

import static ai.nexconn.chatui.usermanage.handler.FriendInfoHandler.KEY_GET_FRIENDS;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.usermanage.handler.FriendInfoHandler;
import ai.nexconn.chatui.usermanage.handler.GroupMembersFullHandler;
import ai.nexconn.chatui.usermanage.handler.GroupOperationsHandler;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import ai.nexconn.chatui.utils.text.StringUtils;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * ViewModel for adding group members.
 *
 * @since 5.12.0
 */
public class AddGroupMembersViewModel extends BaseViewModel {

    private final MutableLiveData<List<ContactModel>> filteredContactsLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ContactModel>> selectedContactsLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final Set<String> existingGroupMemberIds = new HashSet<>();

    protected final FriendInfoHandler friendInfoHandler;
    protected final GroupMembersFullHandler groupMembersFullHandler;
    protected final GroupOperationsHandler groupOperationsHandler;

    private boolean isJoining = false;
    private final String groupId;

    public AddGroupMembersViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ChannelIdentifier conversationIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        arguments, ChatUIConstants.KEY_CHANNEL_IDENTIFIER);
        groupId = conversationIdentifier.getChannelId();

        friendInfoHandler = new FriendInfoHandler();
        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);

        friendInfoHandler.addDataChangeListener(
                FriendInfoHandler.KEY_SEARCH_FRIENDS,
                friendInfos ->
                        filteredContactsLiveData.postValue(sortAndCategorizeContacts(friendInfos)));

        // Initialize GroupDetailHandler to fetch group members
        groupMembersFullHandler = new GroupMembersFullHandler(conversationIdentifier);
        groupMembersFullHandler.addDataChangeListener(
                GroupMembersFullHandler.KEY_GET_ALL_GROUP_MEMBERS_BY_ROLES,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        if (groupMemberInfos != null) {
                            for (GroupMemberInfo memberInfo : groupMemberInfos) {
                                existingGroupMemberIds.add(memberInfo.getUserId());
                            }
                        }
                        // After fetching group members, fetch friends and update their status
                        fetchAndFilterFriendDetail();
                    }
                });
        groupMembersFullHandler.getAllGroupMembersByRole(GroupMemberRole.UNDEF);
    }

    private void fetchAndFilterFriendDetail() {
        friendInfoHandler.addDataChangeListener(
                KEY_GET_FRIENDS,
                new SafeDataHandler<List<FriendDetail>>() {
                    @Override
                    public void onDataChange(List<FriendDetail> friendInfos) {
                        List<ContactModel> contactModels = sortAndCategorizeContacts(friendInfos);
                        filteredContactsLiveData.postValue(
                                contactModels); // Show all contacts in initial state
                    }
                });
        friendInfoHandler.getFriends();
    }

    /**
     * Invite users to group.
     *
     * @param onDataChangeListener data change listener
     * @since 5.12.2
     */
    public void inviteUsersToGroup(@NonNull OnDataChangeListener<Integer> onDataChangeListener) {
        if (isJoining) {
            return;
        }

        List<String> userIds = getSelectUserIds();
        if (userIds.isEmpty()) {
            return;
        }
        isJoining = true;
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_INVITE_USERS_TO_GROUP,
                new OnDataChangeListener<Integer>() {
                    @Override
                    public void onDataChange(Integer code) {
                        isJoining = false;
                        onDataChangeListener.onDataChange(code);
                    }

                    @Override
                    public void onDataError(NCError error) {
                        isJoining = false;
                        onDataChangeListener.onDataChange(error.getCode());
                        onDataChangeListener.onDataError(error);
                    }
                });

        groupOperationsHandler.inviteUsersToGroup(userIds);
    }

    /**
     * @since 5.12.2
     */
    public LiveData<List<ContactModel>> getFilteredContactsLiveData() {
        return filteredContactsLiveData;
    }

    /**
     * @since 5.12.2
     */
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
            if (contact.getBean() instanceof FriendDetail
                    && updatedContact.getBean() instanceof FriendDetail) {
                FriendDetail friendInfo = (FriendDetail) contact.getBean();
                FriendDetail updatedFriendDetail = (FriendDetail) updatedContact.getBean();
                if (friendInfo.getUserId().equals(updatedFriendDetail.getUserId())) {
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
            friendInfoHandler.getFriends();
            return;
        }
        friendInfoHandler.searchFriendsInfo(query);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        friendInfoHandler.stop();
        groupMembersFullHandler.stop();
        groupOperationsHandler.stop();
    }

    List<String> getSelectUserIds() {
        List<String> userIds = new ArrayList<>();
        List<ContactModel> selectedList = selectedContactsLiveData.getValue();
        if (selectedList == null || selectedList.isEmpty()) {
            return userIds;
        }
        for (ContactModel contact : selectedList) {
            if (contact.getBean() instanceof FriendDetail) {
                FriendDetail friendInfo = (FriendDetail) contact.getBean();
                if (friendInfo != null) {
                    userIds.add(friendInfo.getUserId());
                }
            }
        }
        return userIds;
    }

    String getGroupId() {
        return groupId;
    }

    private List<ContactModel> sortAndCategorizeContacts(List<FriendDetail> friendInfos) {
        // Sort: Chinese by initial letter, English by full name
        Collections.sort(
                friendInfos,
                (friend1, friend2) -> {
                    String name1 = getValidName(friend1);
                    String name2 = getValidName(friend2);
                    char firstChar1 = StringUtils.getFirstChar(name1.charAt(0));
                    char firstChar2 = StringUtils.getFirstChar(name2.charAt(0));
                    // If firstChar1 is #, place it at the end
                    if (firstChar1 == '#') {
                        return 1;
                    }
                    // If firstChar2 is #, place it at the end
                    if (firstChar2 == '#') {
                        return -1;
                    }
                    if (Character.isLetter(firstChar1) && Character.isLetter(firstChar2)) {
                        return firstChar1 - firstChar2;
                    } else {
                        return name1.compareToIgnoreCase(name2);
                    }
                });

        List<ContactModel> contactModels = new ArrayList<>();
        char lastCategory = '\0';
        List<String> selectUserIds = getSelectUserIds();
        for (FriendDetail friendInfo : friendInfos) {
            String name = getValidName(friendInfo);
            char firstChar = StringUtils.getFirstChar(name.charAt(0));

            // If initial letter differs, add a title ContactModel
            if (firstChar != lastCategory) {
                contactModels.add(
                        ContactModel.obtain(
                                String.valueOf(firstChar),
                                ContactModel.ItemType.TITLE,
                                ContactModel.CheckType.NONE));
                lastCategory = firstChar;
            }

            ContactModel.CheckType checkType = ContactModel.CheckType.UNCHECKED;
            if (selectUserIds.contains(friendInfo.getUserId())) {
                checkType = ContactModel.CheckType.CHECKED;
            }
            if (existingGroupMemberIds.contains(friendInfo.getUserId())) {
                checkType = ContactModel.CheckType.DISABLE;
            }

            // Add contact ContactModel
            contactModels.add(
                    ContactModel.obtain(friendInfo, ContactModel.ItemType.CONTENT, checkType));
        }

        return contactModels;
    }

    private String getValidName(FriendDetail friendInfo) {
        // Return a valid name: prefer remark, then name, fall back to "#" as placeholder
        String name =
                !TextUtils.isEmpty(friendInfo.getRemark())
                        ? friendInfo.getRemark()
                        : friendInfo.getName();
        return !TextUtils.isEmpty(name) ? name : "#";
    }
}
