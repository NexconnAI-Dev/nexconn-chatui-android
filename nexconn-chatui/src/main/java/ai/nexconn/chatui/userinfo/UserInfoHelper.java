package ai.nexconn.chatui.userinfo;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.handler.MessageHandler;
import ai.nexconn.chat.message.model.MessageReceivedEvent;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.userinfo.db.model.Group;
import ai.nexconn.chatui.userinfo.db.model.GroupMember;
import ai.nexconn.chatui.userinfo.db.model.User;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;
import ai.nexconn.chatui.utils.common.ChatUIUtils;
import ai.nexconn.chatui.utils.function.Action0;
import ai.nexconn.chatui.utils.function.Action1;
import ai.nexconn.chatui.utils.function.Func0;
import ai.nexconn.chatui.utils.function.Func1;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.optional.Option;
import ai.nexconn.chatui.utils.system.ExecutorHelper;
import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * User info management class for retrieving user information.
 *
 * @since 5.10.5
 */
class UserInfoHelper {
    private final String TAG = UserInfoHelper.class.getSimpleName();
    private UserDataDelegate mUserDataDelegate;
    private CacheDataSource cacheDataSource;
    private DbDataSource dbDataSource;
    private UserInfo mCurrentUserInfo;
    private boolean isCacheUserInfo = true;
    private boolean isCacheGroupInfo = true;
    private boolean isCacheGroupMemberInfo = true;
    private List<NCUserInfoManager.UserDataObserver> mUserDataObservers;
    private Context context;
    private String lastUserId = "";

    UserInfoHelper() {
        mUserDataDelegate = new UserDataDelegate();
        mUserDataObservers = new CopyOnWriteArrayList<>();
        cacheDataSource = new CacheDataSource();
        NCEngine.addMessageHandler(
                "userInfoHelper",
                new MessageHandler() {
                    @Override
                    public void onMessageReceived(@NonNull MessageReceivedEvent event) {
                        ai.nexconn.chat.message.Message message = event.getMessage();
                        if (message != null
                                && message.getContent() != null
                                && message.getContent().getSenderUserInfo() != null
                                && getUserDatabase() != null) {
                            final UserInfo userInfo = message.getContent().getSenderUserInfo();
                            UserInfo oldUserInfo = getUserInfo(userInfo.getUserId());
                            if (oldUserInfo != null
                                    && Objects.equals(oldUserInfo.getName(), userInfo.getName())
                                    && Objects.equals(
                                            oldUserInfo.getPortraitUri(), userInfo.getPortraitUri())
                                    && Objects.equals(oldUserInfo.getAlias(), userInfo.getAlias())
                                    && Objects.equals(
                                            oldUserInfo.getExtra(), userInfo.getExtra())) {
                                return;
                            }
                            refreshUserInfoCache(userInfo);
                        }
                    }
                });
    }

    public @Nullable UserDatabase getUserDatabase() {
        return dbDataSource == null ? null : dbDataSource.getDatabase();
    }

    /**
     * Initializes and opens the user info database
     *
     * @param context the application context
     */
    void initAndUpdateUserDataBase(Context context) {
        this.context = context;
        ExecutorHelper.getInstance()
                .diskIO()
                .execute(() -> initDbDataSource(NCEngine.getCurrentUserId()));
    }

    private void initDbDataSource(String userId) {
        if (TextUtils.isEmpty(userId)) {
            RLog.e(TAG, "initDbDataSource but userId is empty.");
            return;
        }

        if (TextUtils.equals(lastUserId, userId) && dbDataSource != null) {
            RLog.e(TAG, "initDbDataSource but userId is same.");
            return;
        }
        cacheDataSource.cleanCache();
        lastUserId = userId;
        dbDataSource =
                new DbDataSource(
                        context,
                        lastUserId,
                        new RoomDatabase.Callback() {
                            @Override
                            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                super.onCreate(db);
                            }

                            @Override
                            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                super.onOpen(db);
                                if (NCChatUIConfig.featureConfig().isPreLoadUserCache()) {
                                    preLoadUserCache();
                                }
                            }

                            @Override
                            public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
                                super.onDestructiveMigration(db);
                            }
                        });
    }

    /**
     * Sets the user info provider for UI to retrieve user names and avatars. ViewModels observe
     * user info changes and refresh accordingly. If user info needs to be fetched asynchronously
     * from the server, the provider can initiate an async request and return null. After the result
     * returns, call {@link #refreshUserInfoCache(UserInfo)} to refresh the info.
     *
     * @param userInfoProvider the user info provider {@link UserDataProvider.UserInfoProvider}.
     * @param isCacheUserInfo whether ChatUI should cache user info. If the app's UserInfoProvider
     *     fetches data from the network each time without local caching, it will slow down info
     *     loading; in that case set this to true to let ChatUI cache user info.
     */
    void setUserInfoProvider(
            UserDataProvider.UserInfoProvider userInfoProvider, boolean isCacheUserInfo) {
        mUserDataDelegate.setUserInfoProvider(userInfoProvider);
        this.isCacheUserInfo = isCacheUserInfo;
    }

    void setGroupInfoProvider(
            UserDataProvider.GroupInfoProvider groupInfoProvider, boolean isCacheGroupInfo) {
        mUserDataDelegate.setGroupInfoProvider(groupInfoProvider);
        this.isCacheGroupInfo = isCacheGroupInfo;
    }

    boolean isCacheUserOrGroupInfo() {
        return isCacheUserInfo || isCacheGroupInfo;
    }

    /**
     * Sets the group member info provider.
     *
     * <p>Can be used to customize nicknames within a group.
     *
     * <p>Once set, when the SDK UI displays user info it will call {@link
     * UserDataProvider.GroupUserInfoProvider#getGroupUserInfo(String, String)}. The provider only
     * needs to return the corresponding {@link GroupUserInfo} for the given groupId and userId. If
     * user info needs to be fetched asynchronously from the server, the provider can initiate an
     * async request and return null. After the result returns, call {@link
     * #refreshGroupUserInfoCache(GroupUserInfo)} to refresh the info.
     *
     * @param groupUserInfoProvider the group user info provider.
     * @param isCacheGroupUserInfo whether ChatUI should cache GroupUserInfo. If the app's
     *     GroupUserInfoProvider fetches data from the network each time without local caching, it
     *     will slow down info loading; in that case set this to true to let ChatUI cache the info.
     */
    void setGroupUserInfoProvider(
            UserDataProvider.GroupUserInfoProvider groupUserInfoProvider,
            boolean isCacheGroupUserInfo) {
        mUserDataDelegate.setGroupUserInfoProvider(groupUserInfoProvider);
        isCacheGroupMemberInfo = isCacheGroupUserInfo;
    }

    UserInfo getUserInfo(final String userId) {
        if (TextUtils.isEmpty(userId)) {
            return null;
        }
        // Check L1 cache first
        return Option.ofObj(cacheDataSource.getUserInfo(userId))
                .map(
                        new Func1<User, UserInfo>() {
                            @Override
                            public UserInfo call(User user) {
                                // L1 cache hit, return userInfo directly
                                return transformUser(user);
                            }
                        })
                .orDefault(
                        new Func0<UserInfo>() {
                            @Override
                            public UserInfo call() {
                                // L1 cache miss
                                if (isCacheUserInfo) {
                                    // L2 cache
                                    getDbUserInfo(userId);
                                    return null;
                                } else {
                                    UserInfo userInfo = mUserDataDelegate.getUserInfo(userId);
                                    if (userInfo != null) {
                                        saveUserInfoCache(userInfo);
                                    }
                                    return userInfo;
                                }
                            }
                        });
    }

    private void getDbUserInfo(final String userId) {
        if (dbDataSource == null) {
            UserInfo userInfo = mUserDataDelegate.getUserInfo(userId);
            if (userInfo != null) {
                refreshUserInfoCache(userInfo);
            }
            return;
        }
        dbDataSource.getUserInfo(
                userId,
                new Consumer<User>() {
                    @Override
                    public void accept(User user) {
                        Option.ofObj(user)
                                .ifSome(
                                        new Action1<User>() {
                                            @Override
                                            public void call(User user) {
                                                // Cache in memory
                                                cacheDataSource.refreshUserInfo(user);
                                                notifyUserChange(transformUser(user));
                                            }
                                        })
                                .ifNone(
                                        new Action0() {
                                            @Override
                                            public void call() {
                                                UserInfo userInfo =
                                                        mUserDataDelegate.getUserInfo(userId);
                                                if (userInfo != null) {
                                                    refreshUserInfoCache(userInfo);
                                                }
                                            }
                                        });
                    }
                });
    }

    private void notifyUserChange(final UserInfo userInfo) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            for (NCUserInfoManager.UserDataObserver item : mUserDataObservers) {
                item.onUserUpdate(userInfo);
            }
        } else {
            ExecutorHelper.getInstance()
                    .mainThread()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    notifyUserChange(userInfo);
                                }
                            });
        }
    }

    private UserInfo transformUser(User user) {
        String url = user.portraitUrl;
        String portraitUriStr;
        if (!TextUtils.isEmpty(url)) {
            portraitUriStr = url;
        } else if (context != null) {
            int drawableId =
                    ChatUIThemeManager.getAttrResId(
                            context, R.attr.nc_conversation_list_cell_portrait_msg_img);
            portraitUriStr = ChatUIUtils.getUriFromDrawableRes(context, drawableId).toString();
        } else if (NCChatUI.getContext() != null) {
            int drawableId =
                    ChatUIThemeManager.getAttrResId(
                            NCChatUI.getContext(),
                            R.attr.nc_conversation_list_cell_portrait_msg_img);
            portraitUriStr =
                    ChatUIUtils.getUriFromDrawableRes(NCChatUI.getContext(), drawableId).toString();
        } else {
            portraitUriStr = "";
        }
        return new UserInfo(
                user.id,
                0,
                user.name == null ? "" : user.name,
                portraitUriStr,
                user.alias,
                user.extra);
    }

    GroupInfo getGroupInfo(final String groupId) {
        if (TextUtils.isEmpty(groupId)) {
            return null;
        }
        // Check L1 cache first
        return Option.ofObj(cacheDataSource.getGroupInfo(groupId))
                .map(
                        new Func1<Group, GroupInfo>() {
                            @Override
                            public GroupInfo call(Group group) {
                                return transformGroup(group);
                            }
                        })
                .orDefault(
                        new Func0<GroupInfo>() {
                            @Override
                            public GroupInfo call() {
                                if (isCacheGroupInfo) {
                                    getDbGroupInfo(groupId);
                                    return null;
                                } else {
                                    GroupInfo groupInfo = mUserDataDelegate.getGroupInfo(groupId);
                                    if (groupInfo != null) {
                                        saveGroupInfoCache(groupInfo);
                                    }
                                    return groupInfo;
                                }
                            }
                        });
    }

    private GroupInfo transformGroup(@NonNull Group group) {
        return new GroupInfo(group.id, group.name, group.portraitUrl);
    }

    private void getDbGroupInfo(final String groupId) {
        if (dbDataSource == null) {
            GroupInfo groupInfo = mUserDataDelegate.getGroupInfo(groupId);
            if (groupInfo != null) {
                refreshGroupInfoCache(groupInfo);
            }
            return;
        }
        dbDataSource.getGroupInfo(
                groupId,
                new Consumer<Group>() {
                    @Override
                    public void accept(Group group) {
                        Option.ofObj(group)
                                .ifSome(
                                        new Action1<Group>() {
                                            @Override
                                            public void call(Group group) {
                                                cacheDataSource.refreshGroupInfo(group);
                                                notifyGroupChange(transformGroup(group));
                                            }
                                        })
                                .ifNone(
                                        new Action0() {
                                            @Override
                                            public void call() {
                                                GroupInfo groupInfo =
                                                        mUserDataDelegate.getGroupInfo(groupId);
                                                if (groupInfo != null) {
                                                    refreshGroupInfoCache(groupInfo);
                                                }
                                            }
                                        });
                    }
                });
    }

    private void notifyGroupChange(final GroupInfo groupInfo) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            for (NCUserInfoManager.UserDataObserver item : mUserDataObservers) {
                item.onGroupUpdate(groupInfo);
            }
        } else {
            ExecutorHelper.getInstance()
                    .mainThread()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    notifyGroupChange(groupInfo);
                                }
                            });
        }
    }

    GroupUserInfo getGroupUserInfo(final String groupId, final String userId) {
        if (TextUtils.isEmpty(groupId) || TextUtils.isEmpty(userId)) {
            return null;
        }
        // Check L1 cache first
        return Option.ofObj(cacheDataSource.getGroupUserInfo(groupId, userId))
                .map(
                        new Func1<GroupMember, GroupUserInfo>() {
                            @Override
                            public GroupUserInfo call(GroupMember groupMember) {
                                // L1 cache hit, return directly
                                return transformGroupMember(groupMember);
                            }
                        })
                .orDefault(
                        new Func0<GroupUserInfo>() {
                            @Override
                            public GroupUserInfo call() {
                                // L1 cache miss
                                if (isCacheGroupMemberInfo) {
                                    // L2 cache
                                    getDbGroupUserInfo(groupId, userId);
                                    return null;
                                } else {
                                    GroupUserInfo groupUserInfo =
                                            mUserDataDelegate.getGroupUserInfo(groupId, userId);
                                    if (groupUserInfo != null) {
                                        saveGroupUserInfoCache(groupUserInfo);
                                    }
                                    return groupUserInfo;
                                }
                            }
                        });
    }

    private void getDbGroupUserInfo(final String groupId, final String userId) {
        if (dbDataSource == null) {
            GroupUserInfo groupUserInfo = mUserDataDelegate.getGroupUserInfo(groupId, userId);
            if (groupUserInfo != null) {
                refreshGroupUserInfoCache(groupUserInfo);
            }
            return;
        }
        dbDataSource.getGroupUserInfo(
                groupId,
                userId,
                new Consumer<GroupMember>() {
                    @Override
                    public void accept(GroupMember groupMember) {
                        Option.ofObj(groupMember)
                                .ifSome(
                                        new Action1<GroupMember>() {
                                            @Override
                                            public void call(GroupMember member) {
                                                // Cache in memory
                                                cacheDataSource.refreshGroupUserInfo(member);
                                                notifyGroupMemberChange(
                                                        transformGroupMember(member));
                                            }
                                        })
                                .ifNone(
                                        new Action0() {
                                            @Override
                                            public void call() {
                                                GroupUserInfo groupUserInfo =
                                                        mUserDataDelegate.getGroupUserInfo(
                                                                groupId, userId);
                                                if (groupUserInfo != null) {
                                                    refreshGroupUserInfoCache(groupUserInfo);
                                                }
                                            }
                                        });
                    }
                });
    }

    private void notifyGroupMemberChange(final GroupUserInfo groupUserInfo) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            for (NCUserInfoManager.UserDataObserver item : mUserDataObservers) {
                item.onGroupUserInfoUpdate(groupUserInfo);
            }
        } else {
            ExecutorHelper.getInstance()
                    .mainThread()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    notifyGroupMemberChange(groupUserInfo);
                                }
                            });
        }
    }

    private GroupUserInfo transformGroupMember(@NonNull GroupMember groupMember) {
        return new GroupUserInfo(
                groupMember.groupId, groupMember.userId, groupMember.memberName, groupMember.extra);
    }

    UserInfo getCurrentUserInfo() {
        if (mCurrentUserInfo != null) {
            return mCurrentUserInfo;
        } else {
            return getUserInfo(NCEngine.getCurrentUserId());
        }
    }

    /**
     * Sets the current user info. If the developer has not implemented a user info provider and
     * instead relies on message-attached user info, use this method to set the current user's info,
     * then call {@link NCUserInfoManager#setMessageAttachedUserInfo(boolean)} after initialization.
     * This attaches the current user's info to each message, and ChatUI will extract and display it
     * upon message receipt.
     *
     * @param userInfo the current user info.
     */
    void setCurrentUserInfo(UserInfo userInfo) {
        mCurrentUserInfo = userInfo;
    }

    private void saveUserInfoCache(UserInfo userInfo) {
        if (userInfo == null) {
            RLog.e(TAG, "Invalid to refresh a null user object.");
            return;
        }
        User user = new User(userInfo);
        cacheDataSource.refreshUserInfo(user);
        if (isCacheUserInfo && dbDataSource != null) {
            dbDataSource.refreshUserInfo(user);
        }
    }

    void refreshUserInfoCache(UserInfo userInfo) {
        saveUserInfoCache(userInfo);
        notifyUserChange(userInfo);
    }

    void refreshGroupInfoCache(GroupInfo groupInfo) {
        saveGroupInfoCache(groupInfo);
        notifyGroupChange(groupInfo);
    }

    private void saveGroupInfoCache(GroupInfo groupInfo) {
        if (groupInfo == null) {
            RLog.e(TAG, "Invalid to refresh a null group object.");
            return;
        }
        RLog.d(TAG, "refresh Group info.");
        Group group =
                new Group(
                        groupInfo.getGroupId(),
                        groupInfo.getGroupName(),
                        groupInfo.getPortraitUri() == null ? "" : groupInfo.getPortraitUri(),
                        null);
        cacheDataSource.refreshGroupInfo(group);
        if (isCacheGroupInfo && dbDataSource != null) {
            dbDataSource.refreshGroupInfo(group);
        }
    }

    void refreshGroupUserInfoCache(GroupUserInfo groupUserInfo) {
        saveGroupUserInfoCache(groupUserInfo);
        notifyGroupMemberChange(groupUserInfo);
    }

    private void saveGroupUserInfoCache(GroupUserInfo groupUserInfo) {
        if (groupUserInfo == null) {
            RLog.e(TAG, "Invalid to refresh a null groupUserInfo object.");
            return;
        }
        GroupMember groupMember =
                new GroupMember(
                        groupUserInfo.getGroupId(),
                        groupUserInfo.getUserId(),
                        groupUserInfo.getNickname(),
                        groupUserInfo.getExtra());
        cacheDataSource.refreshGroupUserInfo(groupMember);
        if (isCacheGroupMemberInfo && dbDataSource != null) {
            dbDataSource.refreshGroupUserInfo(groupMember);
        }
    }

    /**
     * Adds a data change listener. Called when user info, group info, or group nickname info
     * changes.
     *
     * @param observer the data change listener.
     */
    void addUserDataObserver(final NCUserInfoManager.UserDataObserver observer) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            mUserDataObservers.add(observer);
        } else {
            ExecutorHelper.getInstance()
                    .mainThread()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    addUserDataObserver(observer);
                                }
                            });
        }
    }

    /**
     * Removes a data change listener.
     *
     * @param observer the previously registered data change listener.
     */
    void removeUserDataObserver(final NCUserInfoManager.UserDataObserver observer) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            mUserDataObservers.remove(observer);
        } else {
            ExecutorHelper.getInstance()
                    .mainThread()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    removeUserDataObserver(observer);
                                }
                            });
        }
    }

    private void preLoadUserCache() {
        if (dbDataSource == null) {
            return;
        }
        dbDataSource.getLimitUser(
                NCChatUIConfig.featureConfig().getUserCacheMaxCount(),
                new Consumer<List<User>>() {
                    @Override
                    public void accept(List<User> users) {
                        for (User item : users) {
                            cacheDataSource.refreshUserInfo(item);
                            notifyUserChange(transformUser(item));
                        }
                    }
                });
        dbDataSource.getLimitGroup(
                NCChatUIConfig.featureConfig().getGroupCacheMaxCount(),
                new Consumer<List<Group>>() {
                    @Override
                    public void accept(List<Group> groups) {
                        for (Group item : groups) {
                            cacheDataSource.refreshGroupInfo(item);
                            notifyGroupChange(transformGroup(item));
                        }
                    }
                });
        dbDataSource.getLimitGroupMember(
                NCChatUIConfig.featureConfig().getGroupMemberCacheMaxCount(),
                new Consumer<List<GroupMember>>() {
                    @Override
                    public void accept(List<GroupMember> groupMembers) {
                        for (GroupMember item : groupMembers) {
                            cacheDataSource.refreshGroupUserInfo(item);
                            notifyGroupMemberChange(transformGroupMember(item));
                        }
                    }
                });
    }
}
