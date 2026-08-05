package ai.nexconn.chatui.usermanage.handler;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.user.model.UserProfile;
import ai.nexconn.chatui.base.MultiDataHandler;
import java.util.ArrayList;
import java.util.List;

/**
 * User profile handler.
 *
 * <p>Note: Call {@link #stop()} to release resources when done.
 *
 * @since 5.12.0
 */
public class UserProfileHandler extends MultiDataHandler {

    public static final DataKey<UserProfile> KEY_GET_USER_PROFILE =
            DataKey.obtain("KEY_GET_USER_PROFILE", UserProfile.class);

    public static final DataKey<UserProfile> KEY_GET_MY_USER_PROFILE =
            DataKey.obtain("KEY_GET_MY_USER_PROFILE", UserProfile.class);

    /** Gets the current user's profile. */
    public void getMyUserProfile() {
        NCEngine.getUserModule()
                .getMyUserProfile(
                        new OperationHandler<UserProfile>() {
                            @Override
                            public void onResult(UserProfile userProfile, NCError error) {
                                if (error == null) {
                                    notifyDataChange(KEY_GET_MY_USER_PROFILE, userProfile);
                                } else {
                                    notifyDataError(KEY_GET_MY_USER_PROFILE, error);
                                }
                            }
                        });
    }

    /**
     * Gets a user's profile.
     *
     * @param id the user ID
     */
    public void getUserProfile(String id) {
        ArrayList<String> idList = new ArrayList<>(1);
        idList.add(id);
        NCEngine.getUserModule()
                .getUserProfiles(
                        idList,
                        new OperationHandler<List<UserProfile>>() {
                            @Override
                            public void onResult(List<UserProfile> userProfiles, NCError error) {
                                if (error == null) {
                                    if (userProfiles != null && !userProfiles.isEmpty()) {
                                        notifyDataChange(KEY_GET_USER_PROFILE, userProfiles.get(0));
                                    }
                                } else {
                                    notifyDataError(KEY_GET_USER_PROFILE, error);
                                }
                            }
                        });
    }
}
