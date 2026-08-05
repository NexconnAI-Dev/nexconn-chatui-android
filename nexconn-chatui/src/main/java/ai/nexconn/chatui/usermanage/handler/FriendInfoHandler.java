package ai.nexconn.chatui.usermanage.handler;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.ErrorHandler;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.params.AddFriendParams;
import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chat.user.model.FriendRelation;
import ai.nexconn.chat.user.model.UserOnlineStatus;
import ai.nexconn.chat.user.model.UserProfile;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.manager.OnLineStatusListener;
import ai.nexconn.chatui.manager.OnLineStatusManager;
import ai.nexconn.chatui.utils.log.RLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Friend info handler.
 *
 * <p>Note: Call {@link #stop()} to release resources when done.
 *
 * @since 5.12.0
 */
public class FriendInfoHandler extends MultiDataHandler {
    private static final String TAG = "FriendInfoHandler";
    public static final MultiDataHandler.DataKey<FriendRelation> KEY_CHECK_FRIEND =
            MultiDataHandler.DataKey.obtain("KEY_CHECK_FRIEND", FriendRelation.class);
    public static final MultiDataHandler.DataKey<List<FriendDetail>> KEY_GET_FRIENDS =
            MultiDataHandler.DataKey.obtain(
                    "KEY_GET_FRIENDS", (Class<List<FriendDetail>>) (Class<?>) List.class);

    public static final MultiDataHandler.DataKey<UserProfile> KEY_SEARCH_USER =
            MultiDataHandler.DataKey.obtain("KEY_SEARCH_USER", UserProfile.class);
    public static final MultiDataHandler.DataKey<FriendDetail> KEY_GET_FRIEND =
            MultiDataHandler.DataKey.obtain("KEY_GET_FRIEND", FriendDetail.class);
    public static final MultiDataHandler.DataKey<Boolean> KEY_DELETE_FRIEND =
            MultiDataHandler.DataKey.obtain("KEY_DELETE_FRIEND", Boolean.class);
    public static final MultiDataHandler.DataKey<Integer> KEY_APPLY_FRIEND =
            MultiDataHandler.DataKey.obtain("KEY_APPLY_FRIEND", Integer.class);
    public static final MultiDataHandler.DataKey<List<FriendDetail>> KEY_SEARCH_FRIENDS =
            MultiDataHandler.DataKey.obtain(
                    "KEY_SEARCH_FRIENDS", (Class<List<FriendDetail>>) (Class<?>) List.class);
    // Get friends online status
    public static final DataKey<Map<String, UserOnlineStatus>> KEY_GET_FRIENDS_ONLINE_STATUS =
            DataKey.obtain(
                    "KEY_GET_FRIENDS_ONLINE_STATUS",
                    (Class<Map<String, UserOnlineStatus>>) (Class<?>) Map.class);
    private final OnLineStatusListener mOnLineStatusListener =
            statuses -> notifyDataChange(KEY_GET_FRIENDS_ONLINE_STATUS, statuses);
    private String mUserId = "";

    public FriendInfoHandler() {
        OnLineStatusManager.getInstance().addOnLineStatusListener(mOnLineStatusListener);
    }

    @Override
    public void stop() {
        super.stop();
        OnLineStatusManager.getInstance().removeOnLineStatusListener(mOnLineStatusListener);
    }

    public void getFriends() {
        NCEngine.getUserModule()
                .getFriends(
                        new OperationHandler<List<FriendDetail>>() {
                            @Override
                            public void onResult(List<FriendDetail> friendDetails, NCError error) {
                                if (error == null) {
                                    notifyDataChange(KEY_GET_FRIENDS, friendDetails);
                                } else {
                                    notifyDataError(KEY_GET_FRIENDS, error);
                                }
                            }
                        });
    }

    public void getUserOnlineStatus(String uid) {
        RLog.d(TAG, "fetchUsersOnlineStatus uid: " + uid);
        this.mUserId = uid;
        OnLineStatusManager.getInstance().fetchUsersOnlineStatus(uid, false);
    }

    public void getUserOnlineStatus(List<FriendDetail> friendDetails) {
        List<String> uidList = new ArrayList<>();
        if (!friendDetails.isEmpty()) {
            for (FriendDetail friendDetail : friendDetails) {
                uidList.add(friendDetail.getUserId());
            }
        }
        RLog.d(TAG, "fetchUsersOnlineStatus uidList: " + uidList);
        OnLineStatusManager.getInstance().fetchUsersOnlineStatus(uidList);
    }

    public void checkFriend(String userId) {
        List<String> userIds = new ArrayList<>(1);
        userIds.add(userId);
        NCEngine.getUserModule()
                .checkFriends(
                        userIds,
                        new OperationHandler<List<FriendRelation>>() {
                            @Override
                            public void onResult(List<FriendRelation> result, NCError error) {
                                if (error == null && result != null && !result.isEmpty()) {
                                    notifyDataChange(KEY_CHECK_FRIEND, result.get(0));
                                }
                            }
                        });
    }

    public void getFriendInfo(String userId) {
        List<String> userIds = new ArrayList<>(1);
        userIds.add(userId);
        NCEngine.getUserModule()
                .getFriendsInfo(
                        userIds,
                        new OperationHandler<List<FriendDetail>>() {
                            @Override
                            public void onResult(List<FriendDetail> info, NCError error) {
                                if (error == null && info != null && !info.isEmpty()) {
                                    notifyDataChange(KEY_GET_FRIEND, info.get(0));
                                }
                            }
                        });
    }

    public void deleteFriend(String userId, OnDataChangeListener<Boolean> listener) {
        replaceDataChangeListener(KEY_DELETE_FRIEND, listener);
        List<String> userIds = new ArrayList<>(1);
        userIds.add(userId);
        NCEngine.getUserModule()
                .deleteFriends(
                        userIds,
                        new ErrorHandler() {
                            @Override
                            public void onError(NCError error) {
                                if (error == null) {
                                    notifyDataChange(KEY_DELETE_FRIEND, true);
                                } else {
                                    notifyDataChange(KEY_DELETE_FRIEND, false);
                                }
                            }
                        });
    }

    public void applyFriend(String userId, String remark, OnDataChangeListener<Integer> listener) {
        replaceDataChangeListener(KEY_APPLY_FRIEND, listener);
        AddFriendParams params = new AddFriendParams(userId);
        params.setExtra(remark);
        NCEngine.getUserModule()
                .addFriend(
                        params,
                        new OperationHandler<Integer>() {
                            @Override
                            public void onResult(Integer resultCode, NCError error) {
                                if (error == null) {
                                    notifyDataChange(KEY_APPLY_FRIEND, resultCode);
                                } else {
                                    notifyDataChange(KEY_APPLY_FRIEND, error.getCode());
                                }
                            }
                        });
    }

    // TODO: searchUserProfileByUniqueId has no nexconn equivalent yet
    public void findUser(String uniqueId) {
        List<String> ids = new ArrayList<>(1);
        ids.add(uniqueId);
        NCEngine.getUserModule()
                .getUserProfiles(
                        ids,
                        new OperationHandler<List<UserProfile>>() {
                            @Override
                            public void onResult(List<UserProfile> userProfiles, NCError error) {
                                if (error == null
                                        && userProfiles != null
                                        && !userProfiles.isEmpty()) {
                                    notifyDataChange(KEY_SEARCH_USER, userProfiles.get(0));
                                } else {
                                    notifyDataChange(KEY_SEARCH_USER, null);
                                }
                            }
                        });
    }

    public void searchFriendsInfo(String query) {
        NCEngine.getUserModule()
                .searchFriendsInfo(
                        query,
                        new OperationHandler<List<FriendDetail>>() {
                            @Override
                            public void onResult(List<FriendDetail> friendDetails, NCError error) {
                                if (error == null) {
                                    notifyDataChange(KEY_SEARCH_FRIENDS, friendDetails);
                                } else {
                                    notifyDataChange(KEY_SEARCH_FRIENDS, new ArrayList<>());
                                }
                            }
                        });
    }
}
