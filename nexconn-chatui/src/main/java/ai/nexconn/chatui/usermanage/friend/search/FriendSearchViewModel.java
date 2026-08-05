package ai.nexconn.chatui.usermanage.friend.search;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.handler.UserHandler;
import ai.nexconn.chat.user.model.FriendAddEvent;
import ai.nexconn.chat.user.model.FriendApplicationStatusChangedEvent;
import ai.nexconn.chat.user.model.FriendClearedEvent;
import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chat.user.model.FriendInfoChangedSyncEvent;
import ai.nexconn.chat.user.model.FriendRemoveEvent;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.usermanage.handler.FriendInfoHandler;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;

/**
 * Friend search ViewModel
 *
 * @since 5.12.0
 */
public class FriendSearchViewModel extends BaseViewModel {
    private final MutableLiveData<List<FriendDetail>> friendInfoLiveData = new MutableLiveData<>();
    private final FriendInfoHandler friendInfoHandler;
    private String query;
    private final UserHandler listener =
            new UserHandler() {
                @Override
                public void onFriendAdd(FriendAddEvent event) {
                    if (!TextUtils.isEmpty(query)) {
                        queryContacts(query);
                    }
                }

                @Override
                public void onFriendRemove(FriendRemoveEvent event) {
                    if (!TextUtils.isEmpty(query)) {
                        queryContacts(query);
                    }
                }

                @Override
                public void onFriendApplicationStatusChanged(
                        FriendApplicationStatusChangedEvent event) {
                    if (!TextUtils.isEmpty(query)) {
                        queryContacts(query);
                    }
                }

                @Override
                public void onFriendCleared(FriendClearedEvent event) {
                    if (!TextUtils.isEmpty(query)) {
                        queryContacts(query);
                    }
                }

                @Override
                public void onFriendInfoChangedSync(FriendInfoChangedSyncEvent event) {
                    if (!TextUtils.isEmpty(query)) {
                        queryContacts(query);
                    }
                }
            };

    public FriendSearchViewModel(@NonNull Bundle arguments) {
        super(arguments);
        friendInfoHandler = new FriendInfoHandler();
        friendInfoHandler.addDataChangeListener(
                FriendInfoHandler.KEY_SEARCH_FRIENDS,
                new SafeDataHandler<List<FriendDetail>>() {
                    @Override
                    public void onDataChange(List<FriendDetail> friendInfos) {
                        friendInfoLiveData.postValue(friendInfos);
                    }
                });
        NCEngine.INSTANCE.addUserHandler("FriendSearchViewModel", listener);
    }

    public MutableLiveData<List<FriendDetail>> getFriendInfoLiveData() {
        return friendInfoLiveData;
    }

    /**
     * Query contacts
     *
     * @param query search keyword
     */
    public void queryContacts(String query) {
        this.query = query;
        if (TextUtils.isEmpty(query)) {
            friendInfoLiveData.postValue(new ArrayList<>());
            return;
        }
        friendInfoHandler.searchFriendsInfo(query);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        NCEngine.INSTANCE.removeUserHandler("FriendSearchViewModel");
        friendInfoHandler.stop();
    }
}
