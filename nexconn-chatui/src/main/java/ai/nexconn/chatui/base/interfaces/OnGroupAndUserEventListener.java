package ai.nexconn.chatui.base.interfaces;

import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.user.model.UserProfile;
import java.util.Map;

/**
 * Group and user event listener.
 *
 * @since 5.12.2
 */
public interface OnGroupAndUserEventListener {

    /**
     * Updates group info.
     *
     * @param groupInfo the group info
     * @param error the error, null on success
     */
    void updateGroupInfo(GroupInfo groupInfo, NCError error);

    /**
     * Updates group member info.
     *
     * @param groupId the group ID
     * @param userId the user ID
     * @param nickname the nickname
     * @param extra the extra data
     * @param error the error, null on success
     */
    void setGroupMemberInfo(
            String groupId, String userId, String nickname, String extra, NCError error);

    /**
     * Updates the current user's profile.
     *
     * @param userProfile the user profile
     * @param error the error, null on success
     */
    void updateMyUserProfile(UserProfile userProfile, NCError error);

    /**
     * Sets friend info.
     *
     * @param userId the user ID
     * @param remark the remark
     * @param extProfile the extended profile
     * @param error the error, null on success
     */
    void setFriendInfo(String userId, String remark, Map<String, String> extProfile, NCError error);
}
