package ai.nexconn.chatui.userinfo;

import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.userinfo.db.model.Group;
import ai.nexconn.chatui.userinfo.db.model.GroupMember;
import ai.nexconn.chatui.userinfo.db.model.User;
import ai.nexconn.chatui.utils.text.StringUtils;
import ai.nexconn.chatui.widget.cache.NCCache;
import androidx.annotation.NonNull;

public class CacheDataSource {
    private final String TAG = CacheDataSource.class.getSimpleName();
    private NCCache<String, User> mUserCache;
    private NCCache<String, GroupMember> mGroupMemberCache;
    private NCCache<String, Group> mGroupCache;

    CacheDataSource() {
        mUserCache = new NCCache<>(NCChatUIConfig.featureConfig().getUserCacheMaxCount());
        mGroupMemberCache =
                new NCCache<>(NCChatUIConfig.featureConfig().getGroupMemberCacheMaxCount());
        mGroupCache = new NCCache<>(NCChatUIConfig.featureConfig().getGroupCacheMaxCount());
    }

    User getUserInfo(final String userId) {
        synchronized (mUserCache) {
            return mUserCache.get(userId);
        }
    }

    Group getGroupInfo(final String groupId) {
        synchronized (mGroupCache) {
            return mGroupCache.get(groupId);
        }
    }

    GroupMember getGroupUserInfo(final String groupId, final String userId) {
        synchronized (mGroupMemberCache) {
            final String key = StringUtils.getKey(groupId, userId);
            return mGroupMemberCache.get(key);
        }
    }

    void refreshUserInfo(@NonNull final User user) {
        synchronized (mUserCache) {
            mUserCache.put(user.id, user);
        }
    }

    void refreshGroupUserInfo(@NonNull final GroupMember groupMember) {
        synchronized (mGroupMemberCache) {
            String key = StringUtils.getKey(groupMember.groupId, groupMember.userId);
            mGroupMemberCache.put(key, groupMember);
        }
    }

    void refreshGroupInfo(@NonNull final Group group) {
        synchronized (mGroupCache) {
            mGroupCache.put(group.id, group);
        }
    }

    public void cleanCache() {
        mUserCache.clear();
        mGroupCache.clear();
        mGroupMemberCache.clear();
    }
}
