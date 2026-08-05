package ai.nexconn.chatui.usermanage.friend.user.profile;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.channel.model.GroupMemberInfoEditPermission;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chat.handler.UserHandler;
import ai.nexconn.chat.user.model.FriendAddEvent;
import ai.nexconn.chat.user.model.FriendApplicationStatusChangedEvent;
import ai.nexconn.chat.user.model.FriendClearedEvent;
import ai.nexconn.chat.user.model.FriendDetail;
import ai.nexconn.chat.user.model.FriendInfoChangedSyncEvent;
import ai.nexconn.chat.user.model.FriendRelation;
import ai.nexconn.chat.user.model.FriendRelationType;
import ai.nexconn.chat.user.model.FriendRemoveEvent;
import ai.nexconn.chat.user.model.UserOnlineStatus;
import ai.nexconn.chat.user.model.UserProfile;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.manager.OnLineStatusManager;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.model.UiUserDetail;
import ai.nexconn.chatui.usermanage.handler.FriendInfoHandler;
import ai.nexconn.chatui.usermanage.handler.GroupInfoHandler;
import ai.nexconn.chatui.usermanage.handler.UserProfileHandler;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.lifecycle.MutableLiveData;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * User profile page ViewModel
 *
 * @since 5.12.0
 */
public class UserProfileViewModel extends BaseViewModel {

    private final MutableLiveData<UiUserDetail> mUserProfilesLiveData = new MutableLiveData<>();
    private final MutableLiveData<ContactModel> mContactModelLiveData = new MutableLiveData<>();
    private final MutableLiveData<GroupMemberInfo> myGroupMemberInfoLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<GroupInfo> groupInfoLiveData = new MutableLiveData<>();
    private final MutableLiveData<UserOnlineStatus> onlineStatusLiveData = new MutableLiveData<>();

    private final UserProfileHandler userProfileHandler;
    private final FriendInfoHandler friendInfoHandler;
    private GroupInfoHandler groupInfoHandler;

    private final String userId;
    private boolean checkFriend = true;

    private final UserHandler listener =
            new UserHandler() {
                @Override
                public void onFriendAdd(FriendAddEvent event) {
                    getUserProfile();
                }

                @Override
                public void onFriendRemove(FriendRemoveEvent event) {
                    getUserProfile();
                }

                @Override
                public void onFriendApplicationStatusChanged(
                        FriendApplicationStatusChangedEvent event) {
                    getUserProfile();
                }

                @Override
                public void onFriendCleared(FriendClearedEvent event) {
                    getUserProfile();
                }

                @Override
                public void onFriendInfoChangedSync(FriendInfoChangedSyncEvent event) {
                    getUserProfile();
                }
            };

    public UserProfileViewModel(Bundle arguments) {
        super(arguments);
        this.userId = arguments.getString(ChatUIConstants.KEY_USER_ID);

        ChannelIdentifier channelIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        arguments, ChatUIConstants.KEY_CHANNEL_IDENTIFIER);

        if (channelIdentifier != null) {
            groupInfoHandler = new GroupInfoHandler(channelIdentifier);
            groupInfoHandler.addDataChangeListener(
                    GroupInfoHandler.KEY_GET_GROUP_MEMBERS,
                    new SafeDataHandler<List<GroupMemberInfo>>() {
                        @Override
                        public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                            if (groupMemberInfos != null && !groupMemberInfos.isEmpty()) {
                                myGroupMemberInfoLiveData.postValue(groupMemberInfos.get(0));
                            }
                        }
                    });
            groupInfoHandler.addDataChangeListener(
                    GroupInfoHandler.KEY_GROUP_INFO,
                    new SafeDataHandler<GroupInfo>() {
                        @Override
                        public void onDataChange(GroupInfo groupInfo) {
                            groupInfoLiveData.postValue(groupInfo);
                            if (groupInfoHandler != null) {
                                groupInfoHandler.getGroupMembers(Arrays.asList(userId));
                            }
                        }
                    });
        }

        userProfileHandler = new UserProfileHandler();
        userProfileHandler.addDataChangeListener(
                UserProfileHandler.KEY_GET_USER_PROFILE,
                new OnDataChangeListener<UserProfile>() {
                    @Override
                    public void onDataChange(UserProfile profile) {
                        UiUserDetail uiUserDetail =
                                new UiUserDetail(
                                        profile.getUserId(),
                                        profile.getName(),
                                        null,
                                        profile.getPortraitUri(),
                                        !checkFriend);
                        mUserProfilesLiveData.postValue(uiUserDetail);
                        mContactModelLiveData.postValue(
                                ContactModel.obtain(profile, ContactModel.ItemType.CONTENT));
                        friendInfoHandler.getUserOnlineStatus(profile.getUserId());
                    }
                });
        friendInfoHandler = new FriendInfoHandler();
        friendInfoHandler.addDataChangeListener(
                FriendInfoHandler.KEY_CHECK_FRIEND,
                new OnDataChangeListener<FriendRelation>() {
                    @Override
                    public void onDataChange(FriendRelation info) {
                        if (info.getRelationType() == FriendRelationType.BOTH_WAY
                                || info.getRelationType() == FriendRelationType.IN_MY_FRIEND_LIST) {
                            friendInfoHandler.getFriendInfo(info.getUserId());
                        } else {
                            userProfileHandler.getUserProfile(info.getUserId());
                        }
                    }
                });
        friendInfoHandler.addDataChangeListener(
                FriendInfoHandler.KEY_GET_FRIEND,
                new OnDataChangeListener<FriendDetail>() {
                    @Override
                    public void onDataChange(FriendDetail info) {
                        UiUserDetail uiUserDetail =
                                new UiUserDetail(
                                        info.getUserId(),
                                        info.getName(),
                                        info.getRemark(),
                                        info.getPortraitUri(),
                                        true);
                        mUserProfilesLiveData.postValue(uiUserDetail);
                        mContactModelLiveData.postValue(
                                ContactModel.obtain(info, ContactModel.ItemType.CONTENT));
                        friendInfoHandler.getUserOnlineStatus(info.getUserId());
                    }
                });
        friendInfoHandler.addDataChangeListener(
                FriendInfoHandler.KEY_GET_FRIENDS_ONLINE_STATUS,
                new OnDataChangeEnhancedListener<Map<String, UserOnlineStatus>>() {
                    @Override
                    public void onDataChange(Map<String, UserOnlineStatus> value) {
                        for (Map.Entry<String, UserOnlineStatus> entry : value.entrySet()) {
                            if (TextUtils.equals(entry.getKey(), userId)) {
                                onlineStatusLiveData.postValue(entry.getValue());
                                return;
                            }
                        }
                    }
                });
        NCEngine.INSTANCE.addUserHandler("UserProfileViewModel", listener);
    }

    public MutableLiveData<UiUserDetail> getUserProfilesLiveData() {
        return mUserProfilesLiveData;
    }

    public MutableLiveData<ContactModel> getContactModelLiveData() {
        return mContactModelLiveData;
    }

    public MutableLiveData<GroupMemberInfo> getMyGroupMemberInfoLiveData() {
        return myGroupMemberInfoLiveData;
    }

    public MutableLiveData<UserOnlineStatus> getOnlineStatusLiveData() {
        return onlineStatusLiveData;
    }

    boolean hasEditPermission() {
        GroupInfo groupInfo = groupInfoLiveData.getValue();
        if (groupInfo != null) {
            GroupMemberInfoEditPermission editPermission = groupInfo.getMemberInfoEditPermission();
            GroupMemberRole role = groupInfo.getRole();
            if (editPermission == GroupMemberInfoEditPermission.OWNER_OR_SELF
                    && role == GroupMemberRole.OWNER) {
                return true;
            }
            if (editPermission == GroupMemberInfoEditPermission.OWNER_OR_ADMIN_OR_SELF
                    && (role == GroupMemberRole.OWNER || role == GroupMemberRole.ADMIN)) {
                return true;
            }
        }
        return false;
    }

    public UiUserDetail getUiUserDetail() {
        return mUserProfilesLiveData.getValue();
    }

    public void deleteFriend(OnDataChangeListener<Boolean> callback) {
        if (getUiUserDetail() != null) {
            friendInfoHandler.deleteFriend(getUiUserDetail().getUserId(), callback);
        }
    }

    public void getUserProfile() {
        Map<String, UserOnlineStatus> cache =
                OnLineStatusManager.getInstance().getNcOnlineStatusCache();
        UserOnlineStatus cachedStatus = cache.get(userId);
        if (cachedStatus != null) {
            onlineStatusLiveData.postValue(cachedStatus);
        }
        if (checkFriend) {
            friendInfoHandler.checkFriend(userId);
        } else {
            userProfileHandler.getUserProfile(userId);
        }

        if (groupInfoHandler != null) {
            groupInfoHandler.getGroupsInfo();
        }
    }

    public void applyFriend(String remark, OnDataChangeListener<Integer> callback) {
        if (getUiUserDetail() != null) {
            friendInfoHandler.applyFriend(getUiUserDetail().getUserId(), remark, callback);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        NCEngine.INSTANCE.removeUserHandler("UserProfileViewModel");
        userProfileHandler.stop();
        friendInfoHandler.stop();
        if (groupInfoHandler != null) {
            groupInfoHandler.stop();
        }
    }
}
