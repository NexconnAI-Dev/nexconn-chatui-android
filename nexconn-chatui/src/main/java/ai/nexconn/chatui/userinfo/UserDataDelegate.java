package ai.nexconn.chatui.userinfo;

import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;

public class UserDataDelegate {
    private UserDataProvider.UserInfoProvider mUserInfoProvider;
    private UserDataProvider.GroupInfoProvider mGroupInfoProvider;
    private UserDataProvider.GroupUserInfoProvider mGroupUserInfoProvider;

    UserDataDelegate() {
        // default implementation ignored
    }

    public void setUserInfoProvider(UserDataProvider.UserInfoProvider provider) {
        mUserInfoProvider = provider;
    }

    public void setGroupInfoProvider(UserDataProvider.GroupInfoProvider provider) {
        mGroupInfoProvider = provider;
    }

    public void setGroupUserInfoProvider(UserDataProvider.GroupUserInfoProvider provider) {
        mGroupUserInfoProvider = provider;
    }

    public UserInfo getUserInfo(String userId) {
        return mUserInfoProvider != null ? mUserInfoProvider.getUserInfo(userId) : null;
    }

    public GroupUserInfo getGroupUserInfo(String groupId, String userId) {
        return mGroupUserInfoProvider != null
                ? mGroupUserInfoProvider.getGroupUserInfo(groupId, userId)
                : null;
    }

    public GroupInfo getGroupInfo(String groupId) {
        return mGroupInfoProvider != null ? mGroupInfoProvider.getGroupInfo(groupId) : null;
    }
}
