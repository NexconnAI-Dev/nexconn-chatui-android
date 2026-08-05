package ai.nexconn.chatui.usermanage.friend.mine.gender;

import ai.nexconn.chat.user.model.UserProfile;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.usermanage.handler.UserProfileOperationsHandler;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.os.Bundle;

/**
 * Update gender page ViewModel
 *
 * @since 5.12.0
 */
public class UpdateGenderViewModel extends BaseViewModel {
    private final UserProfileOperationsHandler userProfileOperationsHandler;
    private UserProfile userProfile;

    public UpdateGenderViewModel(Bundle bundle) {
        super(bundle);
        this.userProfile =
                new UserProfile(
                        bundle.getString(ChatUIConstants.KEY_USER_PROFILE_USER_ID),
                        bundle.getString(ChatUIConstants.KEY_USER_PROFILE_NAME),
                        bundle.getString(ChatUIConstants.KEY_USER_PROFILE_PORTRAIT_URI),
                        null,
                        null,
                        null,
                        bundle.getInt(ChatUIConstants.KEY_USER_PROFILE_GENDER, 0),
                        null,
                        null,
                        null,
                        null);
        userProfileOperationsHandler = new UserProfileOperationsHandler();
    }

    @Deprecated
    public void updateUserProfile(UserProfile userProfile, OnDataChangeListener<Boolean> listener) {
        userProfileOperationsHandler.replaceDataChangeListener(
                UserProfileOperationsHandler.KEY_UPDATE_MY_USER_PROFILE, listener);
        userProfileOperationsHandler.updateMyUserProfile(userProfile);
    }

    public void updateUserProfile(
            UserProfile userProfile, OnDataChangeEnhancedListener<Boolean> listener) {
        userProfileOperationsHandler.replaceDataChangeListener(
                UserProfileOperationsHandler.KEY_UPDATE_MY_USER_PROFILE_EXAMINE, listener);
        userProfileOperationsHandler.updateMyUserProfileExamine(userProfile);
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userProfileOperationsHandler.stop();
    }
}
