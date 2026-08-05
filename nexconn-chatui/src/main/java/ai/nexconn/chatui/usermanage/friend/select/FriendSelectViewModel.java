package ai.nexconn.chatui.usermanage.friend.select;

import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.usermanage.handler.FriendInfoHandler;
import ai.nexconn.chatui.utils.text.StringUtils;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Friend selection page ViewModel
 *
 * @since 5.12.0
 */
public class FriendSelectViewModel extends BaseViewModel {

    private final MutableLiveData<List<ContactModel>> allContactsLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ContactModel>> filteredContactsLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ContactModel>> selectedContactsLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final FriendInfoHandler friendInfoHandler;

    public FriendSelectViewModel(@NonNull Bundle arguments) {
        super(arguments);
        friendInfoHandler = new FriendInfoHandler();
        friendInfoHandler.addDataChangeListener(
                FriendInfoHandler.KEY_GET_FRIENDS,
                new SafeDataHandler<List<FriendDetail>>() {
                    @Override
                    public void onDataChange(List<FriendDetail> friendInfos) {
                        List<ContactModel> contactModels = sortAndCategorizeContacts(friendInfos);
                        allContactsLiveData.postValue(contactModels);
                        filteredContactsLiveData.postValue(
                                contactModels); // Show all contacts initially
                    }
                });
        friendInfoHandler.addDataChangeListener(
                FriendInfoHandler.KEY_SEARCH_FRIENDS,
                friendInfos ->
                        filteredContactsLiveData.postValue(sortAndCategorizeContacts(friendInfos)));
        friendInfoHandler.getFriends();
    }

    public LiveData<List<ContactModel>> getFilteredContactsLiveData() {
        return filteredContactsLiveData;
    }

    public LiveData<List<ContactModel>> getSelectedContactsLiveData() {
        return selectedContactsLiveData;
    }

    @Deprecated
    public LiveData<List<ContactModel>> getAllContactsLiveData() {
        return allContactsLiveData;
    }

    /**
     * Update contact status
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
                FriendDetail updatedFriendInfo = (FriendDetail) updatedContact.getBean();
                if (friendInfo.getUserId().equals(updatedFriendInfo.getUserId())) {
                    found = true;
                    if (updatedContact.getCheckType() == ContactModel.CheckType.UNCHECKED) {
                        iterator.remove(); // Remove when unchecked
                    }
                    break;
                }
            }
        }

        if (!found && updatedContact.getCheckType() == ContactModel.CheckType.CHECKED) {
            // Not found and checked, add to list
            selectedList.add(updatedContact);
        }

        // Update LiveData
        selectedContactsLiveData.postValue(selectedList);
    }

    /**
     * Query contacts
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
    }

    private List<ContactModel> sortAndCategorizeContacts(List<FriendDetail> friendInfos) {
        // Sort contacts: Chinese by first pinyin letter, English by full name
        Collections.sort(
                friendInfos,
                (friend1, friend2) -> {
                    String name1 = getValidName(friend1);
                    String name2 = getValidName(friend2);
                    char firstChar1 = StringUtils.getFirstChar(name1.charAt(0));
                    char firstChar2 = StringUtils.getFirstChar(name2.charAt(0));
                    // If firstChar1 is '#', put it at the end
                    if (firstChar1 == '#') {
                        return 1;
                    }
                    // If firstChar2 is '#', put it at the end
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

            // Add a title ContactModel when the initial letter changes
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
            // Add contact ContactModel
            contactModels.add(
                    ContactModel.obtain(friendInfo, ContactModel.ItemType.CONTENT, checkType));
        }

        return contactModels;
    }

    private String getValidName(FriendDetail friendInfo) {
        // Return a valid name: prefer remark, then name, fallback to "#" as placeholder
        String name =
                !TextUtils.isEmpty(friendInfo.getRemark())
                        ? friendInfo.getRemark()
                        : friendInfo.getName();
        return !TextUtils.isEmpty(name) ? name : "#";
    }

    @NonNull
    private List<String> getSelectUserIds() {
        List<ContactModel> selectedList = selectedContactsLiveData.getValue();
        if (selectedList == null || selectedList.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> userIds = new ArrayList<>();
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
}
