package ai.nexconn.chatui.usermanage.handler;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.params.SetFriendInfoParams;
import ai.nexconn.chat.user.model.UserProfile;
import ai.nexconn.chatui.base.MultiDataHandler;
import java.util.Map;

/**
 * User profile operations handler.
 *
 * <p>Note: Call {@link #stop()} to release resources when done.
 *
 * @since 5.12.0
 */
public class UserProfileOperationsHandler extends MultiDataHandler {

    public static final DataKey<Boolean> KEY_UPDATE_MY_USER_PROFILE =
            DataKey.obtain("KEY_UPDATE_MY_USER_PROFILE", Boolean.class);

    public static final DataKey<Boolean> KEY_UPDATE_MY_USER_PROFILE_EXAMINE =
            DataKey.obtain("KEY_UPDATE_MY_USER_PROFILE_EXAMINE", Boolean.class);

    public static final DataKey<Boolean> KEY_SET_FRIEND_INFO =
            DataKey.obtain("KEY_SET_FRIEND_INFO", Boolean.class);

    public static final DataKey<Boolean> KEY_SET_FRIEND_INFO_EXMAINE =
            DataKey.obtain("KEY_SET_FRIEND_INFO_EXMAINE", Boolean.class);

    /**
     * Updates the current user's profile.
     *
     * @param userProfile the user profile
     */
    @Deprecated
    public void updateMyUserProfile(UserProfile userProfile) {
        NCEngine.getUserModule()
                .updateMyUserProfile(
                        userProfile,
                        (errorDetail, error) -> {
                            if (error == null) {
                                notifyDataChange(KEY_UPDATE_MY_USER_PROFILE, true);
                            } else {
                                notifyDataChange(KEY_UPDATE_MY_USER_PROFILE, false);
                                notifyDataError(KEY_UPDATE_MY_USER_PROFILE, error);
                            }
                        });
    }

    /**
     * Updates the current user's profile with review.
     *
     * @param userProfile the user profile
     */
    public void updateMyUserProfileExamine(UserProfile userProfile) {
        NCEngine.getUserModule()
                .updateMyUserProfile(
                        userProfile,
                        (errorDetail, error) -> {
                            if (error == null) {
                                notifyDataChange(KEY_UPDATE_MY_USER_PROFILE_EXAMINE, true);
                            } else {
                                if (errorDetail != null) {
                                    notifyDataError(
                                            KEY_UPDATE_MY_USER_PROFILE_EXAMINE, error, errorDetail);
                                } else {
                                    notifyDataError(KEY_UPDATE_MY_USER_PROFILE_EXAMINE, error);
                                }
                            }
                        });
    }

    /**
     * Sets friend info.
     *
     * @param userId the user ID
     * @param remark the remark
     * @param extProfile the extended profile
     */
    @Deprecated
    public void setFriendInfo(
            final String userId, final String remark, final Map<String, String> extProfile) {
        SetFriendInfoParams params = new SetFriendInfoParams(userId);
        params.setRemark(remark);
        params.setExtProfile(extProfile);
        NCEngine.getUserModule()
                .setFriendInfo(
                        params,
                        (errorDetail, error) -> {
                            if (error == null) {
                                notifyDataChange(KEY_SET_FRIEND_INFO, true);
                            } else {
                                notifyDataChange(KEY_SET_FRIEND_INFO, false);
                                notifyDataError(KEY_SET_FRIEND_INFO, error);
                            }
                        });
    }

    /**
     * Sets friend info with review.
     *
     * @param userId the user ID
     * @param remark the remark
     * @param extProfile the extended profile
     */
    public void setFriendInfoExamine(
            final String userId, final String remark, final Map<String, String> extProfile) {
        SetFriendInfoParams params = new SetFriendInfoParams(userId);
        params.setRemark(remark);
        params.setExtProfile(extProfile);
        NCEngine.getUserModule()
                .setFriendInfo(
                        params,
                        (errorDetail, error) -> {
                            if (error == null) {
                                notifyDataChange(KEY_SET_FRIEND_INFO_EXMAINE, true);
                            } else {
                                if (errorDetail != null) {
                                    notifyDataError(
                                            KEY_SET_FRIEND_INFO_EXMAINE, error, errorDetail);
                                } else {
                                    notifyDataError(KEY_SET_FRIEND_INFO_EXMAINE, error);
                                }
                            }
                        });
    }
}
