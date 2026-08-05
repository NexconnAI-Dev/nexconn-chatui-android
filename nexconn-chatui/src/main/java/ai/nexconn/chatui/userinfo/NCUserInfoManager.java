package ai.nexconn.chatui.userinfo;

import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.userinfo.db.model.User;
import ai.nexconn.chatui.userinfo.model.ExtendedGroupUserInfo;
import ai.nexconn.chatui.userinfo.model.ExtendedUserInfo;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;
import ai.nexconn.chatui.utils.log.RLog;
import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NCUserInfoManager {

    private static final String TAG = "NCUserInfoManager";
    private static final NCUserInfoManager sInstance = new NCUserInfoManager();

    private DataSourceType dataSourceType = DataSourceType.INFO_MANAGEMENT;
    private final UserInfoHelper userInfoHelper = new UserInfoHelper();
    private final UserManageHelper userManageHelper = new UserManageHelper();

    private boolean mIsUserInfoAttached;

    private NCUserInfoManager() {}

    public static NCUserInfoManager getInstance() {
        return sInstance;
    }

    public void setDataSourceType(@NonNull DataSourceType dataSourceType) {
        if (dataSourceType == null) return;
        RLog.i(TAG, "setDataSourceType: " + dataSourceType.name());
        this.dataSourceType = dataSourceType;
    }

    @NonNull
    public DataSourceType getDataSourceType() {
        return dataSourceType;
    }

    public void initAndUpdateUserDataBase(Context context) {
        userInfoHelper.initAndUpdateUserDataBase(context);
    }

    public void setUserInfoProvider(
            UserDataProvider.UserInfoProvider userInfoProvider, boolean isCacheUserInfo) {
        userInfoHelper.setUserInfoProvider(userInfoProvider, isCacheUserInfo);
    }

    public void setGroupInfoProvider(
            UserDataProvider.GroupInfoProvider groupInfoProvider, boolean isCacheGroupInfo) {
        userInfoHelper.setGroupInfoProvider(groupInfoProvider, isCacheGroupInfo);
    }

    public boolean isCacheUserOrGroupInfo() {
        return userInfoHelper.isCacheUserOrGroupInfo();
    }

    public void setGroupUserInfoProvider(
            UserDataProvider.GroupUserInfoProvider groupUserInfoProvider,
            boolean isCacheGroupUserInfo) {
        userInfoHelper.setGroupUserInfoProvider(groupUserInfoProvider, isCacheGroupUserInfo);
    }

    public UserInfo getUserInfo(final String userId) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            return userInfoHelper.getUserInfo(userId);
        } else {
            ExtendedUserInfo extended = userManageHelper.getUserInfo(userId);
            return extended != null ? extended.toUserInfo() : null;
        }
    }

    public GroupInfo getGroupInfo(final String groupId) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            return userInfoHelper.getGroupInfo(groupId);
        } else {
            return userManageHelper.getGroupInfo(groupId);
        }
    }

    public GroupUserInfo getGroupUserInfo(final String groupId, final String userId) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            return userInfoHelper.getGroupUserInfo(groupId, userId);
        } else {
            GroupMemberInfo memberInfo = userManageHelper.getGroupUserInfo(groupId, userId);
            if (memberInfo == null) return null;
            ExtendedGroupUserInfo extGUI = ExtendedGroupUserInfo.obtain(memberInfo);
            extGUI.setGroupId(groupId);
            return extGUI;
        }
    }

    public void preloadUserInfos(final List<String> ids) {
        if (ids == null || ids.isEmpty() || dataSourceType != DataSourceType.INFO_MANAGEMENT)
            return;
        userManageHelper.loadUserInfos(ids);
    }

    public void preloadGroupInfos(final List<String> ids) {
        if (ids == null || ids.isEmpty() || dataSourceType != DataSourceType.INFO_MANAGEMENT)
            return;
        userManageHelper.loadGroupInfos(ids);
    }

    public void preloadGroupUserInfos(final Map<String, String> groupUserInfos) {
        if (groupUserInfos == null
                || groupUserInfos.isEmpty()
                || dataSourceType != DataSourceType.INFO_MANAGEMENT) return;
        List<String> groupIds = new ArrayList<>();
        List<String> userIds = new ArrayList<>();
        for (Map.Entry<String, String> entry : groupUserInfos.entrySet()) {
            groupIds.add(entry.getKey());
            userIds.add(entry.getValue());
        }
        userManageHelper.loadGroupUserInfos(groupIds, userIds);
    }

    public UserInfo getCurrentUserInfo() {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            return userInfoHelper.getCurrentUserInfo();
        } else {
            ExtendedUserInfo extended = userManageHelper.getCurrentUserInfo();
            return extended != null ? extended.toUserInfo() : null;
        }
    }

    public void setCurrentUserInfo(UserInfo userInfo) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            userInfoHelper.setCurrentUserInfo(userInfo);
        } else {
            ExtendedUserInfo extended = ExtendedUserInfo.obtain(userInfo);
            userManageHelper.setCurrentUserInfo(extended);
        }
    }

    public void setMessageAttachedUserInfo(boolean state) {
        mIsUserInfoAttached = state;
    }

    public boolean getUserInfoAttachedState() {
        return mIsUserInfoAttached;
    }

    public void addUserDataObserver(UserDataObserver observer) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER)
            userInfoHelper.addUserDataObserver(observer);
        else userManageHelper.addUserDataObserver(observer);
    }

    public void removeUserDataObserver(UserDataObserver observer) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER)
            userInfoHelper.removeUserDataObserver(observer);
        else userManageHelper.removeUserDataObserver(observer);
    }

    public void refreshUserInfoCache(UserInfo userInfo) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            userInfoHelper.refreshUserInfoCache(userInfo);
        } else {
            ExtendedUserInfo extended = ExtendedUserInfo.obtain(userInfo);
            userManageHelper.refreshUserInfoCache(extended);
        }
    }

    public void refreshGroupInfoCache(GroupInfo groupInfo) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            userInfoHelper.refreshGroupInfoCache(groupInfo);
        } else {
            userManageHelper.refreshGroupInfoCache(groupInfo);
        }
    }

    public void refreshGroupUserInfoCache(GroupUserInfo groupUserInfo) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            userInfoHelper.refreshGroupUserInfoCache(groupUserInfo);
        } else {
            userManageHelper.refreshGroupUserInfoCache(
                    groupUserInfo.getGroupId(),
                    ((ExtendedGroupUserInfo) groupUserInfo).toGroupMemberInfo());
        }
    }

    public String getUserDisplayName(UserInfo userInfo) {
        if (userInfo == null) return null;
        return TextUtils.isEmpty(userInfo.getAlias()) ? userInfo.getName() : userInfo.getAlias();
    }

    public String getUserDisplayName(User user) {
        if (user == null) return null;
        return TextUtils.isEmpty(user.alias) ? user.name : user.alias;
    }

    public String getUserDisplayName(UserInfo userInfo, String groupMemberName) {
        if (userInfo == null) return groupMemberName == null ? "" : groupMemberName;
        if (!TextUtils.isEmpty(userInfo.getAlias())) return userInfo.getAlias();
        else if (!TextUtils.isEmpty(groupMemberName)) return groupMemberName;
        else return userInfo.getName();
    }

    public interface UserDataObserver {
        void onUserUpdate(UserInfo info);

        void onGroupUpdate(GroupInfo group);

        void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo);
    }

    public enum DataSourceType {
        INFO_PROVIDER,
        INFO_MANAGEMENT
    }
}
