package ai.nexconn.chatui.manager;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.handler.UserHandler;
import ai.nexconn.chat.params.SubscribeEventParams;
import ai.nexconn.chat.params.UnsubscribeEventParams;
import ai.nexconn.chat.user.model.FriendAddEvent;
import ai.nexconn.chat.user.model.FriendClearedEvent;
import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chat.user.model.FriendRemoveEvent;
import ai.nexconn.chat.user.model.SubscribeType;
import ai.nexconn.chat.user.model.SubscriptionChangedEvent;
import ai.nexconn.chat.user.model.SubscriptionStatusInfo;
import ai.nexconn.chat.user.model.SubscriptionSyncCompletedEvent;
import ai.nexconn.chat.user.model.UserOnlineStatus;
import ai.nexconn.chatui.handler.AppSettingsHandler;
import ai.nexconn.chatui.utils.log.RLog;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Online status manager
 *
 * @since 5.32.0
 */
public class OnLineStatusManager {
    private static final String TAG = "OnLineStatusManager";
    private static final int DEFAULT_SUBSCRIBE_ONLINE_TIME =
            7 * 24 * 60 * 60; // Default subscribe duration: 7 days
    private static final int EXPIRED_SUBSCRIBE_ONLINE_TIME =
            6 * 24 * 60 * 60; // Subscribe validity: 6 days
    private static final int DEFAULT_MAX_BATCH_SIZE = 100;
    private static final int MAX_QUERY_SUBSCRIBE_BATCH_SIZE = 100;
    private static final int MAX_SUBSCRIBE_EVENT_BATCH_SIZE = 20;
    private static final int MIN_SUBSCRIBE_EVENT_BATCH_SIZE =
            1; // Fallback when MAX exceeds total subscription count
    private static final int DEFAULT_DELAY_TIME = 500; // Delayed task execution time
    private static final int SUBSCRIPTION_SYNC_COMPLETED_DELAYED_TIME =
            500; // Delay waiting for friend status sync
    private static final int DEFAULT_SUBSCRIBE_NUMBER = 1000; // Max subscription count
    private static final int MAX_RETRY_COUNT = 10; // Max API retry count
    private final Map<String, Long> mSubscribeInfoCache =
            new ConcurrentHashMap<>(); // Subscription info cache
    private final Map<String, String> mFriendUserIdCache =
            new ConcurrentHashMap<>(); // Friend info cache
    private final Map<String, UserOnlineStatus> mOnlineStatusCache = new ConcurrentHashMap<>();
    private final Map<String, String> mGetFriendRequestCache =
            new ConcurrentHashMap<>(); // Pending friend info request cache
    private final List<OnLineStatusListener> mOnLineStatusListeners =
            new CopyOnWriteArrayList<>(); // Online status listeners
    private OnlineStatusDataSource
            mOnlineStatusDataSource; // Data source used to determine priority subscription list
    // when limit is exceeded
    private final List<String> mPriorityUserList =
            new CopyOnWriteArrayList<>(); // Data from OnlineStatusDataSource#onPriorityUserList
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean mHasFriendOnlineSyncCompleted =
            false; // Whether friend status sync is completed

    private final ai.nexconn.chat.handler.ConnectionStatusHandler mConnectionStatusListener =
            event -> {
                if (ai.nexconn.chat.model.ConnectionStatus.SIGNED_OUT == event.getStatus()) {
                    onSignOut();
                } else if (ai.nexconn.chat.model.ConnectionStatus.CONNECTED == event.getStatus()) {
                    // Clear online status cache when reconnected to avoid showing stale status
                    onReconnected();
                }
            };

    private OnLineStatusManager() {}

    private static class Holder {
        private static final OnLineStatusManager INSTANCE = new OnLineStatusManager();
    }

    public static OnLineStatusManager getInstance() {
        return OnLineStatusManager.Holder.INSTANCE;
    }

    /** Initializes the manager */
    public void init() {
        RLog.d(TAG, "init");
        ai.nexconn.chat.NCEngine.addConnectionStatusHandler(
                "OnLineStatusManager", mConnectionStatusListener);
        ai.nexconn.chat.NCEngine.addUserHandler("OnLineStatusManager", mFriendUserHandler);
    }

    private final UserHandler mFriendUserHandler =
            new UserHandler() {
                @Override
                public void onFriendAdd(FriendAddEvent event) {
                    String userId = event.getUserId();
                    RLog.d(TAG, "onFriendAdd: " + userId);
                    if (TextUtils.isEmpty(userId)) {
                        return;
                    }
                    List<String> userList = new ArrayList<>();
                    userList.add(userId);
                    // Cancel existing subscription to avoid duplicates
                    batchUnsubscribeOnlineStatusEvent(userList, false, null);
                    // Update friend cache
                    mFriendUserIdCache.put(userId, userId);
                    // If friend online status subscription is enabled, re-fetch
                    if (AppSettingsHandler.getInstance().isFriendOnlineStatusSubscribeEnable()) {
                        getOnlineStatus(userList, null);
                    }
                }

                @Override
                public void onFriendRemove(FriendRemoveEvent event) {
                    List<String> userIds = event.getUserIds();
                    RLog.d(TAG, "onFriendRemove: " + userIds);
                    if (userIds == null || userIds.isEmpty()) {
                        return;
                    }
                    for (String userId : userIds) {
                        mFriendUserIdCache.remove(userId);
                    }
                    batchSubscribeEvent(userIds, MAX_SUBSCRIBE_EVENT_BATCH_SIZE);
                }

                @Override
                public void onFriendCleared(FriendClearedEvent event) {
                    RLog.d(TAG, "onFriendCleared: " + event.getOperationTime());
                    List<String> userIds = new ArrayList<>(mFriendUserIdCache.keySet());
                    mFriendUserIdCache.clear();
                    batchSubscribeEvent(userIds, MAX_SUBSCRIBE_EVENT_BATCH_SIZE);
                }

                @Override
                public void onSubscriptionChanged(SubscriptionChangedEvent event) {
                    if (event.getEvents() == null || event.getEvents().isEmpty()) {
                        return;
                    }
                    Map<String, UserOnlineStatus> updated = new HashMap<>();
                    for (SubscriptionStatusInfo info : event.getEvents()) {
                        boolean isOnline = false;
                        List<ai.nexconn.chat.user.model.PlatformStatus> platformStatuses =
                                new ArrayList<>();
                        if (info.getDetails() != null) {
                            for (ai.nexconn.chat.user.model.SubscriptionStatusDetail detail :
                                    info.getDetails()) {
                                boolean online = detail.getEventValue() == 1;
                                platformStatuses.add(
                                        new ai.nexconn.chat.user.model.PlatformStatus(
                                                detail.getPlatform(),
                                                online,
                                                detail.getChangeTime()));
                                if (online) isOnline = true;
                            }
                        }
                        UserOnlineStatus status =
                                new UserOnlineStatus(info.getUserId(), isOnline, platformStatuses);
                        mOnlineStatusCache.put(info.getUserId(), status);
                        updated.put(info.getUserId(), status);
                    }
                    notifyStatusChange(updated);
                }

                @Override
                public void onSubscriptionSyncCompleted(SubscriptionSyncCompletedEvent event) {
                    RLog.d(TAG, "onSubscriptionSyncCompleted: " + event.getType());
                    if (SubscribeType.FRIEND_ONLINE_STATUS == event.getType()) {
                        mHasFriendOnlineSyncCompleted = true;
                    }
                }
            };

    /** Sign out and clear caches */
    public void onSignOut() {
        RLog.d(TAG, "clearCache");
        mMainHandler.removeCallbacksAndMessages(null);
        mHasFriendOnlineSyncCompleted = false;
        mSubscribeInfoCache.clear();
        mFriendUserIdCache.clear();
        mOnlineStatusCache.clear();
        mGetFriendRequestCache.clear();
        mPriorityUserList.clear();
    }

    /** Clear online status cache when reconnected to avoid showing stale status */
    private void onReconnected() {
        RLog.d(TAG, "onReconnected - clearing online status cache");
        mOnlineStatusCache.clear();
        mHasFriendOnlineSyncCompleted = false;
        List<String> priorityUsers =
                mOnlineStatusDataSource == null
                        ? Collections.emptyList()
                        : mOnlineStatusDataSource.onPriorityUserList();
        if (priorityUsers != null && !priorityUsers.isEmpty()) {
            fetchUsersOnlineStatus(new ArrayList<>(priorityUsers));
        }
    }

    /** Adds an online status change listener */
    public void addOnLineStatusListener(OnLineStatusListener listener) {
        if (listener != null && !mOnLineStatusListeners.contains(listener)) {
            RLog.d(TAG, "addOnLineStatusListener");
            mOnLineStatusListeners.add(listener);
        }
    }

    /** Removes an online status change listener */
    public void removeOnLineStatusListener(OnLineStatusListener listener) {
        if (listener != null) {
            RLog.d(TAG, "removeOnLineStatusListener");
            mOnLineStatusListeners.remove(listener);
        }
    }

    /** Clears the priority user list cache */
    public void clearPriorityUserList() {
        mPriorityUserList.clear();
    }

    /** Sets the online status subscription data source */
    public void setOnlineStatusDataSource(OnlineStatusDataSource dataSource) {
        RLog.d(TAG, "setOnlineStatusDataSource dataSource " + dataSource);
        mOnlineStatusDataSource = dataSource;
    }

    /** Removes the online status subscription data source */
    public void removeOnlineStatusDataSource(OnlineStatusDataSource dataSource) {
        RLog.d(TAG, "removeOnlineStatusDataSource dataSource " + dataSource);
        if (dataSource == mOnlineStatusDataSource) {
            mOnlineStatusDataSource = null;
        }
    }

    public Map<String, UserOnlineStatus> getUsersOnlineStatusCache() {
        return mOnlineStatusCache;
    }

    public Map<String, UserOnlineStatus> getNcOnlineStatusCache() {
        return mOnlineStatusCache;
    }

    /**
     * Fetches online status by user ID (friends + non-friends).
     *
     * <p>Note:
     *
     * <p>1. Results are only returned via OnLineStatusListener set by addOnLineStatusListener; call
     * removeOnLineStatusListener when the page is destroyed.
     *
     * <p>2. If cached status exists it is returned immediately; otherwise, online status is fetched
     * and returned via OnLineStatusListener.
     */
    public void fetchUsersOnlineStatus(String userId, boolean processSubscribedLimit) {
        if (TextUtils.isEmpty(userId)) {
            return;
        }
        batchFetchUsersOnlineStatus(Collections.singletonList(userId), processSubscribedLimit, 0);
    }

    /**
     * Fetches online status by user ID list (friends + non-friends).
     *
     * <p>Note:
     *
     * <p>1. Results are only returned via OnLineStatusListener set by addOnLineStatusListener; call
     * removeOnLineStatusListener when the page is destroyed.
     *
     * <p>2. If cached status exists it is returned immediately; otherwise, online status is fetched
     * and returned via OnLineStatusListener.
     */
    public void fetchUsersOnlineStatus(List<String> userIdList) {
        batchFetchUsersOnlineStatus(userIdList, true, 0);
    }

    private void batchFetchUsersOnlineStatus(
            List<String> userIdList, boolean processSubscribedLimit, int retryCount) {
        if (userIdList == null || userIdList.isEmpty()) {
            return;
        }
        // Max retry limit of 20 times (~10 seconds)
        int MAX_SYNC_WAIT_RETRY = 20;
        if (mHasFriendOnlineSyncCompleted && AppSettingsHandler.getInstance().hasInit()) {
            fetchUsersOnlineStatusImpl(userIdList, processSubscribedLimit);
        } else {
            if (retryCount < MAX_SYNC_WAIT_RETRY) {
                mMainHandler.postDelayed(
                        () ->
                                batchFetchUsersOnlineStatus(
                                        userIdList, processSubscribedLimit, retryCount + 1),
                        SUBSCRIPTION_SYNC_COMPLETED_DELAYED_TIME);
            } else {
                // Timeout: sync not completed after 10s. Fallback to query anyway to avoid
                // blank online status display. This ensures users can see online status even
                // if sync event is delayed or missing.
                RLog.w(
                        TAG,
                        "fetchUsersOnlineStatus timeout after "
                                + (MAX_SYNC_WAIT_RETRY
                                        * SUBSCRIPTION_SYNC_COMPLETED_DELAYED_TIME
                                        / 1000)
                                + "s, fallback to query");
                fetchUsersOnlineStatusImpl(userIdList, processSubscribedLimit);
            }
        }
    }

    /**
     * Fetches online status by user ID list (friends + non-friends)
     *
     * @param userList user ID list
     * @param processSubscribedLimit whether to handle subscription limit exceeded
     */
    private void fetchUsersOnlineStatusImpl(List<String> userList, boolean processSubscribedLimit) {
        RLog.d(
                TAG,
                "fetchOnlineStatus userList: "
                        + userList
                        + ",processSubscribedLimit:"
                        + processSubscribedLimit);
        HashMap<String, UserOnlineStatus> cacheResult = new HashMap<>();
        List<String> waitQueryList = new ArrayList<>();
        for (String uid : userList) {
            UserOnlineStatus status = mOnlineStatusCache.get(uid);
            if (status != null) {
                cacheResult.put(uid, status);
            } else {
                waitQueryList.add(uid);
            }
        }
        if (!cacheResult.isEmpty()) {
            notifyStatusChange(cacheResult);
        }
        if (waitQueryList.isEmpty()) {
            RLog.d(TAG, "fetchOnlineStatus waitQueryList isEmpty, userList:" + userList);
            return;
        }
        RLog.d(TAG, "fetchOnlineStatus waitQueryList: " + waitQueryList);
        batchGetFriendsInfo(waitQueryList, processSubscribedLimit);
    }

    /**
     * Fetches friend info in batches
     *
     * @param uidList user ID list
     */
    private void batchGetFriendsInfo(List<String> uidList, boolean processSubscribedLimit) {
        executeBatchTask(
                uidList,
                0,
                DEFAULT_MAX_BATCH_SIZE,
                new Task<String>() {
                    @Override
                    public void run(List<String> batchData, Runnable continueTask) {
                        getFriendsInfo(batchData, processSubscribedLimit, continueTask, 0);
                    }
                },
                null);
    }

    /** Fetches friend info */
    private void getFriendsInfo(
            List<String> uidList,
            boolean processSubscribedLimit,
            Runnable continueTask,
            int retryCount) {
        String uidListTag = uidList.toString();
        RLog.d(
                TAG,
                "getFriendsInfo uidList: "
                        + uidListTag
                        + ",processSubscribedLimit:"
                        + processSubscribedLimit
                        + ", retryCount:"
                        + retryCount);
        // If info management is off (friend online status is necessarily off), use subscribe logic.
        // If info management is on but friend online status is off, friends won't get status;
        // non-friend online status depends on whether online status subscription is enabled.
        if (!AppSettingsHandler.getInstance().isUserProfileEnabled()) {
            RLog.d(TAG, "fetchOnlineStatus userProfile or friendOnlineStatus closed");
            mGetFriendRequestCache.remove(uidListTag);
            batchSubscribeEvent(
                    uidList,
                    MAX_SUBSCRIBE_EVENT_BATCH_SIZE,
                    processSubscribedLimit,
                    new Runnable() {
                        @Override
                        public void run() {
                            if (continueTask != null) {
                                continueTask.run();
                            }
                        }
                    });
            return;
        }
        if (mGetFriendRequestCache.containsKey(uidListTag)) {
            RLog.d(TAG, "fetchOnlineStatus mRequestCache contains cache， tag:" + uidListTag);
            if (continueTask != null) {
                continueTask.run();
            }
            return;
        }
        mGetFriendRequestCache.put(uidListTag, "");
        NCEngine.getUserModule()
                .getFriendsInfo(
                        uidList,
                        new OperationHandler<List<FriendDetail>>() {
                            @Override
                            public void onResult(List<FriendDetail> friendDetails, NCError error) {
                                mGetFriendRequestCache.remove(uidListTag);
                                List<String> friendIds = new ArrayList<>();
                                List<String> nonFriendIds = new ArrayList<>(uidList);
                                if (error == null && friendDetails != null) {
                                    for (FriendDetail detail : friendDetails) {
                                        friendIds.add(detail.getUserId());
                                        nonFriendIds.remove(detail.getUserId());
                                    }
                                }
                                if (!friendIds.isEmpty()
                                        && AppSettingsHandler.getInstance()
                                                .isFriendOnlineStatusSubscribeEnable()) {
                                    getOnlineStatus(friendIds, null);
                                }
                                if (!nonFriendIds.isEmpty()) {
                                    batchSubscribeEvent(
                                            nonFriendIds,
                                            MAX_SUBSCRIBE_EVENT_BATCH_SIZE,
                                            processSubscribedLimit,
                                            continueTask);
                                } else if (continueTask != null) {
                                    continueTask.run();
                                }
                            }
                        });
    }

    /** Batch subscribe to online status (non-friends) */
    private void batchSubscribeEvent(
            List<String> uidList,
            int batchSize,
            boolean processSubscribedLimit,
            Runnable onAllCompleted) {
        RLog.d(
                TAG,
                "batchSubscribeEvent uidSize:"
                        + uidList.size()
                        + ",batchSize:"
                        + batchSize
                        + ",processSubscribedLimit:"
                        + processSubscribedLimit
                        + ",list:"
                        + uidList);
        executeBatchTask(
                uidList,
                0,
                batchSize,
                new Task<String>() {
                    @Override
                    public void run(List<String> batchData, Runnable continueTask) {
                        subscribeEvent(
                                batchData, batchSize, processSubscribedLimit, continueTask, 0);
                    }
                },
                onAllCompleted);
    }

    private void batchSubscribeEvent(List<String> uidList, int batchSize) {
        batchSubscribeEvent(uidList, batchSize, true, null);
    }

    /**
     * Subscribes to online status (non-friends).
     *
     * <p>Note:
     *
     * <p>1. Callers must ensure all users in uidList are non-friends.
     *
     * <p>2. Subscription order must be preserved to correctly detect when the subscription limit is
     * exceeded. Do not use a for-loop; call sequentially instead.
     *
     * @param uidList user ID list
     */
    private void subscribeEvent(
            List<String> uidList,
            int batchSize,
            boolean processSubscribedLimit,
            Runnable continueTask,
            int retryCount) {
        if (!AppSettingsHandler.getInstance().isOnlineStatusSubscribeEnable()) {
            if (continueTask != null) {
                continueTask.run();
            }
            return;
        }
        RLog.d(
                TAG,
                "subscribeEvent uidList:"
                        + uidList
                        + ",batchSize:"
                        + batchSize
                        + ",processSubscribedLimit:"
                        + processSubscribedLimit
                        + ", retryCount:"
                        + retryCount);
        SubscribeEventParams params =
                new SubscribeEventParams(SubscribeType.ONLINE_STATUS, uidList);
        params.setExpiry(DEFAULT_SUBSCRIBE_ONLINE_TIME);
        NCEngine.getUserModule()
                .subscribeEvent(
                        params,
                        (failedIds, error) -> {
                            if (error != null && isNeedRetry(error.getCode(), retryCount)) {
                                mMainHandler.postDelayed(
                                        () ->
                                                subscribeEvent(
                                                        uidList,
                                                        batchSize,
                                                        processSubscribedLimit,
                                                        continueTask,
                                                        retryCount + 1),
                                        DEFAULT_DELAY_TIME);
                                return;
                            }
                            if (error == null && !uidList.isEmpty()) {
                                getOnlineStatus(uidList, null);
                            }
                            if (continueTask != null) {
                                continueTask.run();
                            }
                        });
    }

    private void processSubscribedUsersExceedLimit(int batchSize) {
        RLog.d(TAG, "processSubscribedUsersExceedLimit - not implemented for nexconn");
    }

    private void queryAllSubscribeEvent(Runnable onCompleted) {
        if (onCompleted != null) {
            onCompleted.run();
        }
    }

    private void querySubscribeEventByIds(List<String> uidList, int retryCount) {
        RLog.d(TAG, "querySubscribeEventByIds - not implemented for nexconn");
    }

    /**
     * Batch unsubscribe from user online status
     *
     * @param uidList user ID list
     * @param onCompleted completion callback
     */
    private void batchUnsubscribeOnlineStatusEvent(
            List<String> uidList, boolean removeOnlineStatusCache, Runnable onCompleted) {
        RLog.d(
                TAG,
                "batchUnsubscribeOnlineStatusEvent: "
                        + uidList
                        + ",removeOnlineStatusCache:"
                        + removeOnlineStatusCache);
        executeBatchTask(
                uidList,
                0,
                DEFAULT_MAX_BATCH_SIZE,
                (uidList1, continueTask) ->
                        unsubscribeOnlineStatusEvent(
                                uidList1, removeOnlineStatusCache, continueTask, 0),
                onCompleted);
    }

    private void unsubscribeOnlineStatusEvent(
            List<String> uidList,
            boolean removeOnlineStatusCache,
            Runnable continueTask,
            int retryCount) {
        if (removeOnlineStatusCache) {
            for (String uid : uidList) {
                mOnlineStatusCache.remove(uid);
                mSubscribeInfoCache.remove(uid);
            }
        }
        NCEngine.getUserModule()
                .unsubscribeEvent(
                        new UnsubscribeEventParams(SubscribeType.ONLINE_STATUS, uidList),
                        (failedIds, error) -> {
                            if (error != null && isNeedRetry(error.getCode(), retryCount)) {
                                mMainHandler.postDelayed(
                                        () ->
                                                unsubscribeOnlineStatusEvent(
                                                        uidList,
                                                        false,
                                                        continueTask,
                                                        retryCount + 1),
                                        DEFAULT_DELAY_TIME);
                                return;
                            }
                            if (continueTask != null) {
                                continueTask.run();
                            }
                        });
    }

    /** Gets online status */
    private void getOnlineStatus(List<String> uidList, Runnable onCompleted) {
        getOnlineStatus(uidList, onCompleted, 0);
    }

    private void getOnlineStatus(List<String> uidList, Runnable onCompleted, int retryCount) {
        NCEngine.getUserModule()
                .getSubscribeUsersOnlineStatus(
                        uidList,
                        new OperationHandler<List<UserOnlineStatus>>() {
                            @Override
                            public void onResult(List<UserOnlineStatus> result, NCError error) {
                                if (error != null) {
                                    if (isNeedRetry(error.getCode(), retryCount)) {
                                        mMainHandler.postDelayed(
                                                () ->
                                                        getOnlineStatus(
                                                                uidList,
                                                                onCompleted,
                                                                retryCount + 1),
                                                DEFAULT_DELAY_TIME);
                                        return;
                                    }
                                }
                                if (result != null && !result.isEmpty()) {
                                    Map<String, UserOnlineStatus> updated = new HashMap<>();
                                    for (UserOnlineStatus status : result) {
                                        mOnlineStatusCache.put(status.getUserId(), status);
                                        updated.put(status.getUserId(), status);
                                    }
                                    notifyStatusChange(updated);
                                }
                                if (onCompleted != null) {
                                    onCompleted.run();
                                }
                            }
                        });
    }

    private <T> String formatLogString(List<T> data) {
        if (data == null || data.isEmpty()) {
            return "[]";
        }
        return " " + data.size() + " items";
    }

    private String formatLogString(Map<String, ?> data) {
        if (data == null || data.isEmpty()) {
            return "[]";
        }
        return data.keySet().toString();
    }

    private void notifyStatusChange(Map<String, UserOnlineStatus> onlineResult) {
        if (onlineResult == null || onlineResult.isEmpty()) {
            return;
        }
        RLog.d(TAG, "notifyStatusChange: " + onlineResult.keySet());
        post(
                () -> {
                    for (OnLineStatusListener listener : mOnLineStatusListeners) {
                        listener.onOnlineStatusUpdate(onlineResult);
                    }
                });
    }

    /**
     * Posts to the main thread queue
     *
     * @param runnable the task to execute
     */
    private void post(Runnable runnable) {
        mMainHandler.post(runnable);
    }

    /**
     * Posts a delayed task
     *
     * @param runnable the delayed task
     */
    private void postDelay(Runnable runnable) {
        mMainHandler.postDelayed(runnable, DEFAULT_DELAY_TIME);
    }

    /**
     * Batch processor interface
     *
     * @param <T> data type
     */
    private interface Task<T> {
        /**
         * Processes a batch of data
         *
         * @param batchData the current batch data list
         * @param continueTask call run() to continue with the next batch
         */
        void run(List<T> batchData, Runnable continueTask);
    }

    /** Determines whether a retry is needed */
    private boolean isNeedRetry(int code, int retryCount) {
        return (code == 20607 || code == 30003) && retryCount < MAX_RETRY_COUNT;
    }

    /**
     * General batch processing method
     *
     * @param list the complete list to process
     * @param startIndex start index, pass 0 to start from beginning
     * @param task the batch processor
     * @param onAllCompleted callback after all batches complete, may be null
     * @param <T> data type
     */
    private <T> void executeBatchTask(
            List<T> list, int startIndex, int batchSize, Task<T> task, Runnable onAllCompleted) {
        if (list == null || list.isEmpty() || startIndex >= list.size()) {
            if (onAllCompleted != null) {
                onAllCompleted.run();
            }
            return;
        }

        int endIndex = Math.min(startIndex + batchSize, list.size());
        List<T> batch = list.subList(startIndex, endIndex);

        task.run(
                batch,
                () -> {
                    // Recursively process the next batch
                    executeBatchTask(list, endIndex, batchSize, task, onAllCompleted);
                });
    }
}
