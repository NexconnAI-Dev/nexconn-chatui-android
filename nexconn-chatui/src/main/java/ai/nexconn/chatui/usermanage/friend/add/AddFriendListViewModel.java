package ai.nexconn.chatui.usermanage.friend.add;

import static ai.nexconn.chatui.usermanage.handler.FriendInfoHandler.KEY_SEARCH_USER;

import ai.nexconn.chat.user.model.UserProfile;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.usermanage.handler.FriendInfoHandler;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

/**
 * Add friend list page ViewModel
 *
 * @since 5.12.0
 */
public class AddFriendListViewModel extends BaseViewModel {
    private final MutableLiveData<UserProfile> userProfileLiveData = new MutableLiveData<>();
    private final FriendInfoHandler friendInfoHandler;

    public AddFriendListViewModel(@NonNull Bundle arguments) {
        super(arguments);
        friendInfoHandler = new FriendInfoHandler();
        friendInfoHandler.addDataChangeListener(
                KEY_SEARCH_USER,
                new SafeDataHandler<UserProfile>() {
                    @Override
                    public void onDataChange(UserProfile data) {
                        userProfileLiveData.postValue(data);
                    }
                });
    }

    public MutableLiveData<UserProfile> getUserProfileLiveData() {
        return userProfileLiveData;
    }

    public void findUser(String uniqueId) {
        friendInfoHandler.findUser(uniqueId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        friendInfoHandler.stop();
    }
}
