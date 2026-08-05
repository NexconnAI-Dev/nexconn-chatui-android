package ai.nexconn.chatui.userinfo;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.GroupChannel;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.channel.model.GroupInfoChangedEvent;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.channel.model.GroupMemberInfoChangedEvent;
import ai.nexconn.chat.channel.model.GroupOperation;
import ai.nexconn.chat.channel.model.GroupOperationEvent;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.GroupChannelHandler;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.handler.UserHandler;
import ai.nexconn.chat.model.ConnectionStatus;
import ai.nexconn.chat.params.SetFriendInfoParams;
import ai.nexconn.chat.params.SetGroupMemberInfoParams;
import ai.nexconn.chat.params.UpdateGroupInfoParams;
import ai.nexconn.chat.user.model.FriendAddEvent;
import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chat.user.model.FriendInfoChangedSyncEvent;
import ai.nexconn.chat.user.model.FriendRemoveEvent;
import ai.nexconn.chat.user.model.SubscribeType;
import ai.nexconn.chat.user.model.SubscriptionChangedEvent;
import ai.nexconn.chat.user.model.SubscriptionStatusInfo;
import ai.nexconn.chat.user.model.UserProfile;
import ai.nexconn.chatui.userinfo.model.ExtendedGroupUserInfo;
import ai.nexconn.chatui.userinfo.model.ExtendedUserInfo;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.system.ExecutorHelper;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * User info management class for retrieving user information.
 *
 * @since 5.10.5
 */
class UserManageHelper {
    public static final String TAG = "UserManageHelper";
    private static final int MAX_CACHE_SIZE = 1000; // Max local cache capacity
    private static final int MAX_RETRY_COUNT = 6; // Max request retry count
    private static final int MAX_BATCH_REQUEST_SIZE = 100; // Max batch request size per call
    private static final int ERROR_NET_DATA_IS_SYNCHRONIZING = 34329;
    private static final int ERROR_REQUEST_OVERFREQUENCY = 20607;

    private final LruCache<String, ExtendedUserInfo> extendedUserInfoCache =
            new LruCache<>(MAX_CACHE_SIZE); // Extended user info cache
    private final LruCache<String, GroupInfo> groupInfoCache; // Group info cache
    private final LruCache<String, GroupMemberInfo> groupMemberInfoCache; // Group member info cache
    private final GroupMemberQueryGuard groupMemberQueryGuard;
    private final GroupMemberRequester groupMemberRequester;
    private final List<NCUserInfoManager.UserDataObserver> mUserDataObservers =
            new CopyOnWriteArrayList<>(); // Observer list for dispatching user/group data updates

    private ExtendedUserInfo
            currentExtendedUserInfo; // Extended info for the current logged-in user
    private String currentUserId; // Current logged-in user ID

    private final Set<String> pendingUserIds =
            Collections.synchronizedSet(new HashSet<>()); // User IDs pending batch fetch
    private final Set<String> pendingGroupIds =
            Collections.synchronizedSet(new HashSet<>()); // Group IDs pending batch fetch
    private final Map<String, Set<String>> pendingGroupMemberRequests =
            Collections.synchronizedMap(new HashMap<>()); // Group member IDs pending batch fetch
    private final Set<String> executingGroupIds =
            Collections.synchronizedSet(new HashSet<>()); // Group IDs currently being fetched
    private final Handler mainHandler; // Main thread handler
    private static final long BATCH_DELAY_MS = 0; // Batch request delay
    private static final long RETRY_REQUEST_DELAY_MS = 500; // Retry request delay
    private volatile boolean isUserBatchRunning = false; // Whether a user batch request is running
    private volatile boolean isGroupBatchRunning =
            false; // Whether a group batch request is running
    private volatile boolean isMyUserProfileRunning =
            false; // Whether current user profile request is running
    private volatile boolean hasPendingMyUserProfileRequest =
            false; // Whether there is a pending current user profile request
    private final Object myUserProfileLock = new Object(); // Lock for current user profile requests

    UserManageHelper() {
        this(
                new Handler(Looper.getMainLooper()),
                new GroupMemberQueryGuard(),
                (groupId, userIds, callback) ->
                        new GroupChannel(groupId).getMembers(userIds, callback),
                new LruCache<>(MAX_CACHE_SIZE),
                new LruCache<>(MAX_CACHE_SIZE));
        registerSdkHandlers();
    }

    UserManageHelper(
            Handler mainHandler,
            GroupMemberQueryGuard groupMemberQueryGuard,
            GroupMemberRequester groupMemberRequester,
            LruCache<String, GroupInfo> groupInfoCache,
            LruCache<String, GroupMemberInfo> groupMemberInfoCache) {
        this.mainHandler = mainHandler;
        this.groupMemberQueryGuard = groupMemberQueryGuard;
        this.groupMemberRequester = groupMemberRequester;
        this.groupInfoCache = groupInfoCache;
        this.groupMemberInfoCache = groupMemberInfoCache;
    }

    private void registerSdkHandlers() {
        NCEngine.addConnectionStatusHandler(
                "userManageHelper",
                event -> {
                    if (event.getStatus() == ConnectionStatus.CONNECTED) {
                        String userId = NCEngine.getCurrentUserId();
                        if (!Objects.equals(currentUserId, userId)) {
                            RLog.i(
                                    TAG,
                                    "onConnected: clear cache current:"
                                            + userId
                                            + " last:"
                                            + currentUserId);
                            extendedUserInfoCache.evictAll();
                            groupInfoCache.evictAll();
                            resetGroupMemberQuerySession();
                            currentUserId = userId;
                        }
                    }
                });

        NCEngine.addGroupChannelHandler(
                "userManageHelper",
                new GroupChannelHandler() {
                    @Override
                    public void onGroupOperation(@NonNull GroupOperationEvent event) {
                        handleGroupOperation(event);
                    }

                    @Override
                    public void onGroupInfoChanged(@NonNull GroupInfoChangedEvent event) {
                        GroupInfo groupInfo = event.getGroupInfo();
                        if (groupInfo != null) {
                            refreshGroupInfoInner(groupInfo.getGroupId());
                        }
                    }

                    @Override
                    public void onGroupMemberInfoChanged(
                            @NonNull GroupMemberInfoChangedEvent event) {
                        GroupMemberInfo memberInfo = event.getMemberInfo();
                        if (memberInfo != null) {
                            refreshGroupMemberInfoInner(event.getGroupId(), memberInfo);
                        }
                    }
                });

        NCEngine.addUserHandler(
                "userManageHelper",
                new UserHandler() {
                    @Override
                    public void onSubscriptionChanged(@NonNull SubscriptionChangedEvent event) {
                        List<SubscriptionStatusInfo> events = event.getEvents();
                        Log.e(TAG, "onSubscriptionChanged: " + events);
                        if (events == null || events.isEmpty()) {
                            return;
                        }
                        for (SubscriptionStatusInfo statusInfo : events) {
                            if (statusInfo.getSubscribeType() == SubscribeType.FRIEND_USER_PROFILE
                                    && statusInfo.getUserProfile() != null) {
                                UserProfile profile = statusInfo.getUserProfile();
                                ExtendedUserInfo info =
                                        extendedUserInfoCache.get(profile.getUserId());
                                if (info != null) {
                                    info.setName(profile.getName());
                                    info.setPortraitUri(profile.getPortraitUri());
                                    info.setFriendDetail(
                                            new FriendDetail(
                                                    profile.getUserId(),
                                                    profile.getName(),
                                                    profile.getPortraitUri(),
                                                    info.getFriendDetail() != null
                                                            ? info.getFriendDetail().getRemark()
                                                            : null,
                                                    info.getFriendDetail() != null
                                                            ? info.getFriendDetail().getExtProfile()
                                                            : null,
                                                    info.getFriendDetail() != null
                                                            ? info.getFriendDetail().getAddTime()
                                                            : 0));
                                } else {
                                    FriendDetail friendDetail =
                                            new FriendDetail(
                                                    profile.getUserId(),
                                                    profile.getName(),
                                                    profile.getPortraitUri(),
                                                    null,
                                                    null,
                                                    0);
                                    info = ExtendedUserInfo.obtain(friendDetail);
                                    extendedUserInfoCache.put(profile.getUserId(), info);
                                }
                                notifyUserChange(info);
                            }
                        }
                    }

                    @Override
                    public void onFriendAdd(@NonNull FriendAddEvent event) {
                        refreshUserInfoInner(event.getUserId());
                    }

                    @Override
                    public void onFriendRemove(@NonNull FriendRemoveEvent event) {
                        List<String> userIds = event.getUserIds();
                        if (userIds != null) {
                            for (String userId : userIds) {
                                refreshUserInfoInner(userId);
                            }
                        }
                    }

                    @Override
                    public void onFriendInfoChangedSync(@NonNull FriendInfoChangedSyncEvent event) {
                        refreshUserInfoInner(event.getUserId());
                    }
                });
    }

    void handleGroupOperation(@NonNull GroupOperationEvent event) {
        if (event.getOperation() == GroupOperation.CREATE) {
            groupMemberQueryGuard.onGroupInfoLoaded(event.getGroupInfo());
        }
        refreshGroupInfoInner(event.getGroupId());
    }

    void resetGroupMemberQuerySession() {
        synchronized (pendingGroupMemberRequests) {
            groupMemberQueryGuard.resetSession();
            groupMemberInfoCache.evictAll();
            pendingGroupMemberRequests.clear();
            executingGroupIds.clear();
            mainHandler.removeCallbacks(batchGroupMemberInfoRunnable);
        }
    }

    /* Refresh single group member cache and re-fetch data */
    private void refreshGroupMemberInfoInner(String groupId, GroupMemberInfo memberInfo) {
        groupMemberInfoCache.remove(generateGroupMemberKey(groupId, memberInfo.getUserId()));
        getGroupUserInfo(groupId, memberInfo.getUserId());
    }

    /* Refresh single group info cache and re-fetch data */
    private void refreshGroupInfoInner(String groupId) {
        if (groupId == null) {
            RLog.e(TAG, "refreshGroupInfoInner: groupId is null");
            return;
        }
        groupInfoCache.remove(groupId);
        getGroupInfo(groupId);
    }

    /* Refresh single user info cache and re-fetch data */
    private void refreshUserInfoInner(String userId) {
        if (userId == null) {
            RLog.e(TAG, "refreshUserInfoInner: userId is null");
            return;
        }
        extendedUserInfoCache.remove(userId);
        getUserInfo(userId);
    }

    /**
     * Gets user info
     *
     * @param userId the user ID
     * @return the user info, or null if not cached
     */
    ExtendedUserInfo getUserInfo(final String userId) {
        if (userId == null) {
            return null;
        }
        ExtendedUserInfo cachedExtendedUserInfo = extendedUserInfoCache.get(userId);
        if (cachedExtendedUserInfo != null) {
            return cachedExtendedUserInfo;
        }

        String currentUserId = NCEngine.getCurrentUserId();
        if (Objects.equals(currentUserId, userId)) {
            synchronized (myUserProfileLock) {
                if (isMyUserProfileRunning) {
                    hasPendingMyUserProfileRequest = true;
                } else {
                    isMyUserProfileRunning = true;
                    mainHandler.post(myUserProfileRequestRunnable);
                }
            }
        } else {
            synchronized (pendingUserIds) {
                pendingUserIds.add(userId);
                if (!isUserBatchRunning) {
                    isUserBatchRunning = true;
                    mainHandler.postDelayed(batchUserProfileRequestRunnable, BATCH_DELAY_MS);
                }
            }
        }
        return null;
    }

    /**
     * Gets group info
     *
     * @param groupId the group ID
     * @return the group info, or null if not cached
     */
    GroupInfo getGroupInfo(@NonNull final String groupId) {
        if (groupId == null) {
            RLog.e(TAG, "getGroupInfo: groupId is null");
            return null;
        }
        GroupInfo cachedInfo = groupInfoCache.get(groupId);
        if (cachedInfo != null) {
            return cachedInfo;
        }

        synchronized (pendingGroupIds) {
            pendingGroupIds.add(groupId);
            if (!isGroupBatchRunning) {
                isGroupBatchRunning = true;
                mainHandler.postDelayed(batchGroupInfoRequestRunnable, BATCH_DELAY_MS);
            }
        }
        return null;
    }

    /**
     * Gets group member info
     *
     * @param groupId the group ID
     * @param userId the user ID
     * @return the group member info, or null if not cached
     */
    GroupMemberInfo getGroupUserInfo(final String groupId, final String userId) {
        long requestGeneration = groupMemberQueryGuard.currentGeneration();
        String key = generateGroupMemberKey(groupId, userId);
        GroupMemberInfo cachedMemberInfo = groupMemberInfoCache.get(key);
        if (cachedMemberInfo != null) {
            return cachedMemberInfo;
        }
        if (!canQueryGroupMemberInfo(groupId)) {
            return null;
        }

        synchronized (pendingGroupMemberRequests) {
            if (!groupMemberQueryGuard.isCurrentGeneration(requestGeneration)
                    || !canQueryGroupMemberInfo(groupId)) {
                return null;
            }
            Set<String> userIds = pendingGroupMemberRequests.get(groupId);
            if (userIds == null) {
                userIds = new HashSet<>();
                pendingGroupMemberRequests.put(groupId, userIds);
            }
            userIds.add(userId);
            mainHandler.postDelayed(batchGroupMemberInfoRunnable, BATCH_DELAY_MS);
        }

        return null;
    }

    boolean canQueryGroupMemberInfo(String groupId) {
        return groupMemberQueryGuard.shouldQuery(groupId, groupInfoCache.get(groupId));
    }

    void loadUserInfos(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<String> requestIds = new ArrayList<>();
        List<ExtendedUserInfo> cacheList = new ArrayList<>();
        for (String id : userIds) {
            ExtendedUserInfo cachedExtendedUserInfo = extendedUserInfoCache.get(id);
            if (cachedExtendedUserInfo != null) {
                cacheList.add(cachedExtendedUserInfo);
            } else {
                requestIds.add(id);
            }
        }
        RLog.d(TAG, "loadUserInfos: " + cacheList.size());
        if (requestIds.isEmpty()) {
            return;
        }
        synchronized (pendingUserIds) {
            pendingUserIds.addAll(requestIds);
            if (!isUserBatchRunning) {
                isUserBatchRunning = true;
                mainHandler.postDelayed(batchUserProfileRequestRunnable, BATCH_DELAY_MS);
            }
        }
    }

    void loadGroupInfos(List<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return;
        }
        List<String> requestIds = new ArrayList<>();
        List<GroupInfo> cacheList = new ArrayList<>();
        for (String id : groupIds) {
            GroupInfo cachedInfo = groupInfoCache.get(id);
            if (cachedInfo != null) {
                cacheList.add(cachedInfo);
            } else {
                requestIds.add(id);
            }
        }
        RLog.d(TAG, "loadGroupInfos: " + cacheList.size());
        if (requestIds.isEmpty()) {
            return;
        }
        synchronized (pendingGroupIds) {
            pendingGroupIds.addAll(requestIds);
            if (!isGroupBatchRunning) {
                isGroupBatchRunning = true;
                mainHandler.postDelayed(batchGroupInfoRequestRunnable, BATCH_DELAY_MS);
            }
        }
    }

    void loadGroupUserInfos(List<String> groupIds, List<String> userIds) {
        if (groupIds == null || groupIds.isEmpty() || userIds == null || userIds.isEmpty()) {
            return;
        }
        long requestGeneration = groupMemberQueryGuard.currentGeneration();
        List<String> requestGroupIds = new ArrayList<>();
        List<String> requestUserIds = new ArrayList<>();
        List<GroupMemberInfo> cacheList = new ArrayList<>();
        for (int i = 0; i < groupIds.size(); i++) {
            String groupId = groupIds.get(i);
            String userId = userIds.get(i);
            String key = generateGroupMemberKey(groupId, userId);
            GroupMemberInfo cachedMemberInfo = groupMemberInfoCache.get(key);
            if (cachedMemberInfo != null) {
                cacheList.add(cachedMemberInfo);
            } else {
                if (!canQueryGroupMemberInfo(groupId)) {
                    continue;
                }
                requestGroupIds.add(groupId);
                requestUserIds.add(userId);
            }
        }
        RLog.d(TAG, "loadGroupUserInfos: " + cacheList.size());
        if (requestGroupIds.isEmpty() || requestUserIds.isEmpty()) {
            return;
        }
        synchronized (pendingGroupMemberRequests) {
            if (!groupMemberQueryGuard.isCurrentGeneration(requestGeneration)) {
                return;
            }
            boolean requestAdded = false;
            for (int i = 0; i < requestGroupIds.size(); i++) {
                String groupId = requestGroupIds.get(i);
                if (!canQueryGroupMemberInfo(groupId)) {
                    continue;
                }
                Set<String> userIdSet = pendingGroupMemberRequests.get(groupId);
                if (userIdSet == null) {
                    userIdSet = new HashSet<>();
                    pendingGroupMemberRequests.put(groupId, userIdSet);
                }
                userIdSet.add(requestUserIds.get(i));
                requestAdded = true;
            }
            if (requestAdded) {
                mainHandler.postDelayed(batchGroupMemberInfoRunnable, BATCH_DELAY_MS);
            }
        }
    }

    /**
     * Gets the current user info
     *
     * @return the current user info
     */
    ExtendedUserInfo getCurrentUserInfo() {
        String currentUserId = NCEngine.getCurrentUserId();
        return getUserInfo(currentUserId);
    }

    /**
     * Sets the current user info
     *
     * @param userProfile the user info
     */
    void setCurrentUserInfo(@NonNull ExtendedUserInfo userProfile) {
        this.currentExtendedUserInfo = userProfile;
        NCEngine.getUserModule()
                .updateMyUserProfile(
                        userProfile.getUserProfile(),
                        (errorDetail, error) -> {
                            if (error == null) {
                                notifyUserChange(currentExtendedUserInfo);
                            }
                        });
    }

    /**
     * Adds a user data observer
     *
     * @param observer the user data observer
     */
    void addUserDataObserver(@NonNull NCUserInfoManager.UserDataObserver observer) {
        mUserDataObservers.add(observer);
    }

    /**
     * Removes a user data observer
     *
     * @param observer the user data observer
     */
    void removeUserDataObserver(@NonNull NCUserInfoManager.UserDataObserver observer) {
        mUserDataObservers.remove(observer);
    }

    /**
     * Refreshes the user info cache
     *
     * @param extendedUserInfo the user info to refresh
     */
    void refreshUserInfoCache(@NonNull ExtendedUserInfo extendedUserInfo) {
        if (TextUtils.isEmpty(extendedUserInfo.getUserId())) {
            RLog.e(TAG, "refreshUserInfoCache: userId is empty");
            return;
        }
        extendedUserInfoCache.put(extendedUserInfo.getUserId(), extendedUserInfo);
        notifyUserChange(extendedUserInfo);
        String currentUserId = NCEngine.getCurrentUserId();
        if (Objects.equals(currentUserId, extendedUserInfo.getUserId())) {
            currentExtendedUserInfo = extendedUserInfo;
            NCEngine.getUserModule()
                    .updateMyUserProfile(
                            extendedUserInfo.getUserProfile(),
                            (errorDetail, error) -> {
                                if (error == null) {
                                    refreshUserInfoInner(extendedUserInfo.getUserId());
                                }
                            });
        } else {
            String remark = "";
            HashMap<String, String> extendedUserInfoMap = new HashMap<>();
            if (!TextUtils.isEmpty(extendedUserInfo.getUserProfile().getUserId())) {
                extendedUserInfoMap =
                        (HashMap<String, String>) extendedUserInfo.getUserProfile().getExtProfile();
                remark = extendedUserInfo.getUserProfile().getName();
            } else if (!TextUtils.isEmpty(extendedUserInfo.getFriendDetail().getUserId())) {
                extendedUserInfoMap =
                        (HashMap<String, String>)
                                extendedUserInfo.getFriendDetail().getExtProfile();
                remark = extendedUserInfo.getFriendDetail().getRemark();
            }
            SetFriendInfoParams params = new SetFriendInfoParams(extendedUserInfo.getUserId());
            params.setRemark(remark);
            params.setExtProfile(extendedUserInfoMap);
            NCEngine.getUserModule()
                    .setFriendInfo(
                            params,
                            (errorDetail, error) -> {
                                if (error == null) {
                                    refreshUserInfoInner(extendedUserInfo.getUserId());
                                }
                            });
        }
    }

    /**
     * Refreshes the group info cache
     *
     * @param groupInfo the group info to refresh
     */
    void refreshGroupInfoCache(GroupInfo groupInfo) {
        UpdateGroupInfoParams params = new UpdateGroupInfoParams();
        params.setGroupName(groupInfo.getGroupName());
        params.setPortraitUri(groupInfo.getPortraitUri());
        params.setIntroduction(groupInfo.getIntroduction());
        params.setNotice(groupInfo.getNotice());
        params.setExtProfile(groupInfo.getExtProfile());
        new GroupChannel(groupInfo.getGroupId())
                .updateInfo(
                        params,
                        (errorDetail, error) -> {
                            if (groupInfo != null) {
                                refreshGroupInfoInner(groupInfo.getGroupId());
                            }
                        });
    }

    /**
     * Refreshes the group member info cache
     *
     * @param groupId the group ID
     * @param groupMemberInfo the group member info to refresh
     */
    void refreshGroupUserInfoCache(String groupId, GroupMemberInfo groupMemberInfo) {
        SetGroupMemberInfoParams params =
                new SetGroupMemberInfoParams(
                        groupMemberInfo.getUserId(),
                        groupMemberInfo.getNickname() != null ? groupMemberInfo.getNickname() : "",
                        groupMemberInfo.getExtra());
        new GroupChannel(groupId)
                .setMemberInfo(
                        params,
                        (errorDetail, error) -> {
                            if (error == null) {
                                refreshGroupMemberInfoInner(groupId, groupMemberInfo);
                            }
                        });
    }

    /* Notify all observers that user info has changed */
    private void notifyUserChange(ExtendedUserInfo extendedUserInfo) {
        runOnMainThread(
                () -> {
                    ai.nexconn.chat.user.model.UserInfo ncUser = extendedUserInfo.toUserInfo();
                    for (NCUserInfoManager.UserDataObserver observer : mUserDataObservers) {
                        observer.onUserUpdate(ncUser);
                    }
                });
    }

    /* Notify all observers that group info has changed */
    private void notifyGroupChange(GroupInfo groupInfo) {
        runOnMainThread(
                () -> {
                    for (NCUserInfoManager.UserDataObserver observer : mUserDataObservers) {
                        observer.onGroupUpdate(groupInfo);
                    }
                });
    }

    /* Notify all observers that a group member's info has changed */
    private void notifyGroupMemberChange(
            String groupId, GroupMemberInfo groupMemberInfo, long requestGeneration) {
        runOnMainThread(
                () -> {
                    synchronized (pendingGroupMemberRequests) {
                        if (!groupMemberQueryGuard.isCurrentGeneration(requestGeneration)) {
                            return;
                        }
                        for (NCUserInfoManager.UserDataObserver observer : mUserDataObservers) {
                            ExtendedGroupUserInfo extendedGroupUserInfo =
                                    ExtendedGroupUserInfo.obtain(groupMemberInfo);
                            extendedGroupUserInfo.setGroupId(groupId);
                            observer.onGroupUserInfoUpdate(extendedGroupUserInfo);
                        }
                    }
                });
    }

    // User profile fetch task (batched via queue-coalesced requests)
    private final Runnable batchUserProfileRequestRunnable =
            new Runnable() {
                @Override
                public void run() {
                    final List<String> userIds;
                    synchronized (pendingUserIds) {
                        if (pendingUserIds.size() > MAX_BATCH_REQUEST_SIZE) {
                            userIds = new ArrayList<>(MAX_BATCH_REQUEST_SIZE);
                            Iterator<String> iterator = pendingUserIds.iterator();
                            for (int i = 0; i < MAX_BATCH_REQUEST_SIZE; i++) {
                                String id = iterator.next();
                                userIds.add(id);
                                iterator.remove();
                            }
                        } else {
                            userIds = new ArrayList<>(pendingUserIds);
                            pendingUserIds.clear();
                        }
                    }
                    requestFriendsInfoWithRetry(
                            userIds,
                            0,
                            new OperationHandler<List<FriendDetail>>() {
                                @Override
                                public void onResult(
                                        List<FriendDetail> friendDetails, NCError error) {
                                    if (error != null) {
                                        requestUserProfiles(userIds);
                                        return;
                                    }
                                    final Map<String, ExtendedUserInfo> userMap = new HashMap<>();
                                    List<String> successIds = new ArrayList<>();
                                    List<String> needRequestIds = new ArrayList<>(userIds);

                                    if (friendDetails != null && !friendDetails.isEmpty()) {
                                        for (FriendDetail friend : friendDetails) {
                                            needRequestIds.remove(friend.getUserId());
                                            String uid = friend.getUserId();
                                            userMap.put(uid, ExtendedUserInfo.obtain(friend));
                                            successIds.add(uid);
                                        }
                                    }
                                    updateCacheAndNotify(userMap.values());
                                    finishUserBatch(successIds);
                                    if (!needRequestIds.isEmpty()) {
                                        requestUserProfiles(needRequestIds);
                                    }
                                }
                            });
                }
            };

    /* Request user profiles with retry and notification logic */
    private void requestUserProfiles(List<String> userIds) {
        Map<String, ExtendedUserInfo> userMap = new HashMap<>();
        List<String> successIds = new ArrayList<>();
        requestUserProfilesWithRetry(
                userIds,
                0,
                new OperationHandler<List<UserProfile>>() {
                    @Override
                    public void onResult(List<UserProfile> userProfiles, NCError error) {
                        if (error != null) {
                            updateCacheAndNotify(userMap.values());
                            finishUserBatch(successIds);
                            return;
                        }
                        if (userProfiles != null && !userProfiles.isEmpty()) {
                            for (UserProfile profile : userProfiles) {
                                String uid = profile.getUserId();
                                userMap.put(uid, ExtendedUserInfo.obtain(profile));
                                successIds.add(uid);
                            }
                        }
                        updateCacheAndNotify(userMap.values());
                        finishUserBatch(successIds);
                    }
                });
    }

    /* Request user profiles with retry logic */
    private void requestUserProfilesWithRetry(
            final List<String> userIds,
            final int retryCount,
            final OperationHandler<List<UserProfile>> callback) {
        NCEngine.getUserModule()
                .getUserProfiles(
                        userIds,
                        (userProfiles, error) -> {
                            if (error != null && shouldRetry(error, retryCount)) {
                                postRetryDelayed(
                                        () ->
                                                requestUserProfilesWithRetry(
                                                        userIds, retryCount + 1, callback));
                            } else {
                                callback.onResult(userProfiles, error);
                            }
                        });
    }

    /* Request friend info with retry logic */
    private void requestFriendsInfoWithRetry(
            final List<String> userIds,
            final int retryCount,
            final OperationHandler<List<FriendDetail>> callback) {
        NCEngine.getUserModule()
                .getFriendsInfo(
                        userIds,
                        (friendDetails, error) -> {
                            if (error != null && shouldRetry(error, retryCount)) {
                                postRetryDelayed(
                                        () ->
                                                requestFriendsInfoWithRetry(
                                                        userIds, retryCount + 1, callback));
                            } else {
                                callback.onResult(friendDetails, error);
                            }
                        });
    }

    // Group info fetch task (batched via queue-coalesced requests)
    private final Runnable batchGroupInfoRequestRunnable =
            new Runnable() {
                @Override
                public void run() {
                    final List<String> groupIds;
                    synchronized (pendingGroupIds) {
                        if (pendingGroupIds.size() > MAX_BATCH_REQUEST_SIZE) {
                            groupIds = new ArrayList<>(MAX_BATCH_REQUEST_SIZE);
                            Iterator<String> iterator = pendingGroupIds.iterator();
                            for (int i = 0; i < MAX_BATCH_REQUEST_SIZE; i++) {
                                String id = iterator.next();
                                groupIds.add(id);
                                iterator.remove();
                            }
                        } else {
                            groupIds = new ArrayList<>(pendingGroupIds);
                            pendingGroupIds.clear();
                        }
                    }

                    requestGroupsInfoWithRetry(
                            groupIds,
                            0,
                            new OperationHandler<List<GroupInfo>>() {
                                @Override
                                public void onResult(List<GroupInfo> groupInfos, NCError error) {
                                    if (error != null) {
                                        finishGroupBatch(new ArrayList<>());
                                        return;
                                    }
                                    final List<String> successIds = new ArrayList<>();
                                    if (groupInfos != null) {
                                        for (GroupInfo info : groupInfos) {
                                            if (info != null) {
                                                groupInfoCache.put(info.getGroupId(), info);
                                                groupMemberQueryGuard.onGroupInfoLoaded(info);
                                                notifyGroupChange(info);
                                                successIds.add(info.getGroupId());
                                            }
                                        }
                                    }
                                    finishGroupBatch(successIds);
                                }
                            });
                }
            };

    /* Request group info with retry logic */
    private void requestGroupsInfoWithRetry(
            final List<String> groupIds,
            final int retryCount,
            final OperationHandler<List<GroupInfo>> callback) {
        GroupChannel.getGroupsInfo(
                groupIds,
                (groupInfos, error) -> {
                    if (error != null && shouldRetry(error, retryCount)) {
                        postRetryDelayed(
                                () ->
                                        requestGroupsInfoWithRetry(
                                                groupIds, retryCount + 1, callback));
                    } else {
                        callback.onResult(groupInfos, error);
                    }
                });
    }

    /* Finish a user info batch fetch, clean up state and trigger next batch */
    private void finishUserBatch(List<String> successIds) {
        synchronized (pendingUserIds) {
            if (successIds != null && !successIds.isEmpty()) {
                for (String successId : successIds) {
                    pendingUserIds.remove(successId);
                }
            }
            if (!pendingUserIds.isEmpty()) {
                mainHandler.postDelayed(batchUserProfileRequestRunnable, 0);
            } else {
                isUserBatchRunning = false;
            }
        }
    }

    /* Finish a group info batch fetch, clean up state and trigger next batch */
    private void finishGroupBatch(List<String> successIds) {
        synchronized (pendingGroupIds) {
            if (successIds != null && !successIds.isEmpty()) {
                for (String successId : successIds) {
                    pendingGroupIds.remove(successId);
                }
            }
            if (!pendingGroupIds.isEmpty()) {
                mainHandler.post(batchGroupInfoRequestRunnable);
            } else {
                isGroupBatchRunning = false;
            }
        }
    }

    // Group member info fetch task (batched via queue-coalesced requests)
    private final Runnable batchGroupMemberInfoRunnable =
            this::dispatchPendingGroupMemberRequests;

    void dispatchPendingGroupMemberRequests() {
        synchronized (pendingGroupMemberRequests) {
            Iterator<Map.Entry<String, Set<String>>> iterator =
                    pendingGroupMemberRequests.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Set<String>> entry = iterator.next();
                String groupId = entry.getKey();
                if (!canQueryGroupMemberInfo(groupId)) {
                    iterator.remove();
                    continue;
                }
                if (executingGroupIds.contains(groupId)) {
                    continue;
                }

                Set<String> pendingIds = entry.getValue();
                if (pendingIds == null || pendingIds.isEmpty()) {
                    iterator.remove();
                    continue;
                }

                long requestGeneration = groupMemberQueryGuard.currentGeneration();
                executingGroupIds.add(groupId);

                List<String> batchIds = new ArrayList<>();
                Iterator<String> idIterator = pendingIds.iterator();
                int count = 0;
                while (idIterator.hasNext() && count < MAX_BATCH_REQUEST_SIZE) {
                    batchIds.add(idIterator.next());
                    idIterator.remove();
                    count++;
                }

                if (pendingIds.isEmpty()) {
                    iterator.remove();
                }

                requestGroupMembersWithRetry(
                        groupId,
                        batchIds,
                        0,
                        requestGeneration,
                        new OperationHandler<List<GroupMemberInfo>>() {
                            @Override
                            public void onResult(
                                    List<GroupMemberInfo> groupMemberInfos, NCError error) {
                                synchronized (pendingGroupMemberRequests) {
                                    if (!groupMemberQueryGuard.isCurrentGeneration(
                                            requestGeneration)) {
                                        return;
                                    }
                                    if (error == null && groupMemberInfos != null) {
                                        for (GroupMemberInfo info : groupMemberInfos) {
                                            String key =
                                                    generateGroupMemberKey(
                                                            groupId, info.getUserId());
                                            groupMemberInfoCache.put(key, info);
                                            notifyGroupMemberChange(
                                                    groupId, info, requestGeneration);
                                        }
                                    }
                                    finishGroupMemberBatch(
                                            groupId, requestGeneration, error);
                                }
                            }
                        });
            }
        }
    }

    /* Finish a group member batch fetch, continue with next batch if needed */
    private void finishGroupMemberBatch(
            String groupId, long requestGeneration, NCError error) {
        synchronized (pendingGroupMemberRequests) {
            if (!groupMemberQueryGuard.isCurrentGeneration(requestGeneration)) {
                return;
            }
            if (error != null
                    && error.getCode() == GroupMemberQueryGuard.ERROR_GROUP_USER_NOT_IN_GROUP) {
                pendingGroupMemberRequests.remove(groupId);
            }
            executingGroupIds.remove(groupId);

            if (!pendingGroupMemberRequests.isEmpty()) {
                mainHandler.post(batchGroupMemberInfoRunnable);
            }
        }
    }

    /* Update user cache and notify observers */
    private void updateCacheAndNotify(Collection<ExtendedUserInfo> userInfos) {
        if (userInfos.isEmpty()) {
            return;
        }
        for (ExtendedUserInfo info : userInfos) {
            extendedUserInfoCache.put(info.getUserId(), info);
            notifyUserChange(info);
        }
    }

    /* Ensure the given action runs on the main thread */
    private void runOnMainThread(Runnable action) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            action.run();
        } else {
            ExecutorHelper.getInstance().mainThread().execute(action);
        }
    }

    /* Finalization logic after current user profile request completes */
    private void finishMyProfileRequest() {
        synchronized (myUserProfileLock) {
            if (hasPendingMyUserProfileRequest) {
                hasPendingMyUserProfileRequest = false;
                mainHandler.post(myUserProfileRequestRunnable);
            } else {
                isMyUserProfileRunning = false;
            }
        }
    }

    /* Performs the actual fetch for the current user's profile */
    private final Runnable myUserProfileRequestRunnable =
            new Runnable() {
                @Override
                public void run() {
                    requestMyUserProfileWithRetry(
                            0,
                            (userProfile, error) -> {
                                if (error == null && userProfile != null) {
                                    ExtendedUserInfo extendedUserInfo =
                                            ExtendedUserInfo.obtain(userProfile);
                                    extendedUserInfoCache.put(
                                            userProfile.getUserId(), extendedUserInfo);
                                    notifyUserChange(extendedUserInfo);
                                }
                                finishMyProfileRequest();
                            });
                }
            };

    /* Fetch current user profile with retry logic */
    private void requestMyUserProfileWithRetry(
            final int retryCount, final OperationHandler<UserProfile> callback) {
        NCEngine.getUserModule()
                .getMyUserProfile(
                        (userProfile, error) -> {
                            if (error != null && shouldRetry(error, retryCount)) {
                                postRetryDelayed(
                                        () ->
                                                requestMyUserProfileWithRetry(
                                                        retryCount + 1, callback));
                            } else {
                                callback.onResult(userProfile, error);
                            }
                        });
    }

    /* Request multiple members' info for a specific group with retry logic */
    private void requestGroupMembersWithRetry(
            final String groupId,
            final List<String> userIds,
            final int retryCount,
            final long requestGeneration,
            final OperationHandler<List<GroupMemberInfo>> callback) {
        synchronized (pendingGroupMemberRequests) {
            if (!groupMemberQueryGuard.isCurrentGeneration(requestGeneration)) {
                return;
            }
            groupMemberRequester.request(
                    groupId,
                    userIds,
                    (groupMemberInfos, error) -> {
                        synchronized (pendingGroupMemberRequests) {
                            if (!groupMemberQueryGuard.isCurrentGeneration(requestGeneration)) {
                                return;
                            }
                            if (error != null && shouldRetry(error, retryCount)) {
                                postRetryDelayed(
                                        () ->
                                                requestGroupMembersWithRetry(
                                                        groupId,
                                                        userIds,
                                                        retryCount + 1,
                                                        requestGeneration,
                                                        callback));
                            } else {
                                groupMemberQueryGuard.onQueryResult(
                                        requestGeneration, groupId, error);
                                callback.onResult(groupMemberInfos, error);
                            }
                        }
                    });
        }
    }

    boolean hasPendingGroupMemberRequest(String groupId) {
        synchronized (pendingGroupMemberRequests) {
            Set<String> userIds = pendingGroupMemberRequests.get(groupId);
            return userIds != null && !userIds.isEmpty();
        }
    }

    boolean isExecutingGroupMemberRequest(String groupId) {
        synchronized (pendingGroupMemberRequests) {
            return executingGroupIds.contains(groupId);
        }
    }

    /* Determine whether the current error warrants a retry */
    private boolean shouldRetry(NCError error, int retryCount) {
        if (error == null) return false;
        return (error.getCode() == ERROR_NET_DATA_IS_SYNCHRONIZING
                        || error.getCode() == ERROR_REQUEST_OVERFREQUENCY)
                && retryCount < MAX_RETRY_COUNT;
    }

    /* Unified retry delay scheduling */
    private void postRetryDelayed(Runnable runnable) {
        mainHandler.postDelayed(runnable, RETRY_REQUEST_DELAY_MS);
    }

    /**
     * Generates the cache key for a group member
     *
     * @param groupId the group ID
     * @param userId the user ID
     * @return the cache key
     */
    private String generateGroupMemberKey(String groupId, String userId) {
        return String.format("%s_%s", groupId, userId);
    }

    interface GroupMemberRequester {
        void request(
                String groupId,
                List<String> userIds,
                OperationHandler<List<GroupMemberInfo>> callback);
    }
}
