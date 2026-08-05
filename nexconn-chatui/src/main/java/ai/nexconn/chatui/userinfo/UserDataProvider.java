package ai.nexconn.chatui.userinfo;

import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;

public class UserDataProvider {
    /**
     * Provider for user information.
     *
     * <p>If a chat participant has never authenticated through the server, the SDK has no user info
     * for that user and will call this provider to retrieve it.
     */
    public interface UserInfoProvider {
        /**
         * Returns the user information for the given user ID.
         *
         * @param userId the user ID.
         * @return the corresponding {@link UserInfo}, or {@code null} if not available.
         */
        UserInfo getUserInfo(String userId);
    }

    /** Provider for {@link GroupUserInfo} (group member info). */
    public interface GroupUserInfoProvider {
        /**
         * Returns the group member info for the given group and user.
         *
         * @param groupId the group ID.
         * @param userId the user ID.
         * @return the corresponding {@link GroupUserInfo}, or {@code null} if not available.
         */
        GroupUserInfo getGroupUserInfo(String groupId, String userId);
    }

    /**
     * Provider for group information.
     *
     * <p>The SDK does not store group info internally. When group info is needed during a chat, the
     * SDK will call this provider to retrieve it.
     */
    public interface GroupInfoProvider {
        /**
         * Returns the group information for the given group ID.
         *
         * @param groupId the group ID.
         * @return the corresponding {@link GroupInfo}, or {@code null} if not available.
         */
        GroupInfo getGroupInfo(String groupId);
    }
}
