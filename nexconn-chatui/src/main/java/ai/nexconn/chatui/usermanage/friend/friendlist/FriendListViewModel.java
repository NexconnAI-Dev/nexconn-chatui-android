package ai.nexconn.chatui.usermanage.friend.friendlist;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.handler.UserHandler;
import ai.nexconn.chat.user.model.FriendAddEvent;
import ai.nexconn.chat.user.model.FriendApplicationStatusChangedEvent;
import ai.nexconn.chat.user.model.FriendClearedEvent;
import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chat.user.model.FriendInfoChangedSyncEvent;
import ai.nexconn.chat.user.model.FriendRemoveEvent;
import ai.nexconn.chat.user.model.UserOnlineStatus;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.model.OnlineStatusFriendInfo;
import ai.nexconn.chatui.usermanage.handler.FriendInfoHandler;
import ai.nexconn.chatui.utils.text.StringUtils;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Friend list page ViewModel
 *
 * @since 5.12.0
 */
public class FriendListViewModel extends BaseViewModel {

    private final String mHandlerId = "FriendListViewModel_" + hashCode();
    private final MutableLiveData<List<ContactModel>> allContactsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Map<String, UserOnlineStatus>> onlineStatusLiveData =
            new MutableLiveData<>();
    private final FriendInfoHandler friendInfoHandler;

    private final UserHandler listener =
            new UserHandler() {
                @Override
                public void onFriendAdd(FriendAddEvent event) {
                    friendInfoHandler.getFriends();
                }

                @Override
                public void onFriendRemove(FriendRemoveEvent event) {
                    friendInfoHandler.getFriends();
                }

                @Override
                public void onFriendApplicationStatusChanged(
                        FriendApplicationStatusChangedEvent event) {}

                @Override
                public void onFriendCleared(FriendClearedEvent event) {
                    friendInfoHandler.getFriends();
                }

                @Override
                public void onFriendInfoChangedSync(FriendInfoChangedSyncEvent event) {
                    friendInfoHandler.getFriends();
                }
            };

    public FriendListViewModel(@NonNull Bundle arguments) {
        super(arguments);
        friendInfoHandler = new FriendInfoHandler();
        friendInfoHandler.addDataChangeListener(
                FriendInfoHandler.KEY_GET_FRIENDS,
                new SafeDataHandler<List<FriendDetail>>() {
                    @Override
                    public void onDataChange(List<FriendDetail> friendInfos) {
                        List<ContactModel> contactModels = sortAndCategorizeContacts(friendInfos);
                        allContactsLiveData.postValue(contactModels); // Show all contacts initially
                        friendInfoHandler.getUserOnlineStatus(friendInfos);
                    }
                });
        friendInfoHandler.addDataChangeListener(
                FriendInfoHandler.KEY_GET_FRIENDS_ONLINE_STATUS,
                (OnDataChangeEnhancedListener<Map<String, UserOnlineStatus>>)
                        onlineStatusLiveData::postValue);

        NCEngine.addUserHandler(mHandlerId, listener);
        getAllFriends();
    }

    public LiveData<List<ContactModel>> getAllContactsLiveData() {
        return allContactsLiveData;
    }

    public LiveData<Map<String, UserOnlineStatus>> getOnlineStatusLiveData() {
        return onlineStatusLiveData;
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

            contactModels.add(
                    ContactModel.obtain(
                            new OnlineStatusFriendInfo(friendInfo, false),
                            ContactModel.ItemType.CONTENT,
                            ContactModel.CheckType.UNCHECKED));
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

    public void getAllFriends() {
        friendInfoHandler.getFriends();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        NCEngine.removeUserHandler(mHandlerId);
        friendInfoHandler.stop();
    }
}
