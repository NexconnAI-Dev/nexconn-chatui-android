package ai.nexconn.chatui.usermanage.handler;

import ai.nexconn.chat.channel.GroupChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.channel.model.LeaveGroupConfig;
import ai.nexconn.chat.params.CreateGroupParams;
import ai.nexconn.chat.params.KickGroupMembersParams;
import ai.nexconn.chat.params.SetGroupMemberInfoParams;
import ai.nexconn.chat.params.TransferGroupOwnerParams;
import ai.nexconn.chat.params.UpdateGroupInfoParams;
import ai.nexconn.chatui.base.MultiDataHandler;
import androidx.annotation.NonNull;
import java.util.Collections;
import java.util.List;

/**
 * Group operations handler.
 *
 * <p>Note: Call {@link #stop()} to release resources when done.
 *
 * @since 5.12.0
 */
public class GroupOperationsHandler extends MultiDataHandler {

    /** Key for the create group operation. */
    public static final DataKey<Integer> KEY_CREATE_GROUP =
            DataKey.obtain("KEY_CREATE_GROUP", Integer.class);

    /** Key for the create group operation [with review]. */
    public static final DataKey<Integer> KEY_CREATE_GROUP_EXAMINE =
            DataKey.obtain("KEY_CREATE_GROUP_EXAMINE", Integer.class);

    /** Key for the invite users to group operation. */
    public static final DataKey<Integer> KEY_INVITE_USERS_TO_GROUP =
            DataKey.obtain("KEY_INVITE_USERS_TO_GROUP", Integer.class);

    /** Key for the kick group members operation. */
    public static final DataKey<Boolean> KEY_KICK_GROUP_MEMBERS =
            DataKey.obtain("KEY_KICK_GROUP_MEMBERS", Boolean.class);

    /** Key for the update group info operation. */
    public static final DataKey<Boolean> KEY_UPDATE_GROUP_INFO =
            DataKey.obtain("KEY_UPDATE_GROUP_INFO", Boolean.class);

    /** Key for the update group info operation [with review]. */
    public static final DataKey<Boolean> KEY_UPDATE_GROUP_INFO_EXAMINE =
            DataKey.obtain("KEY_UPDATE_GROUP_INFO_EXAMINE", Boolean.class);

    /** Key for the set group member info operation. */
    public static final DataKey<Boolean> KEY_SET_GROUP_MEMBER_INFO =
            DataKey.obtain("KEY_SET_GROUP_MEMBER_INFO", Boolean.class);

    /** Key for the set group member info operation [with review]. */
    public static final DataKey<Boolean> KEY_SET_GROUP_MEMBER_INFO_EXAMINE =
            DataKey.obtain("KEY_SET_GROUP_MEMBER_INFO_EXAMINE", Boolean.class);

    /** Key for the quit group operation. */
    public static final DataKey<Boolean> KEY_QUIT_GROUP =
            DataKey.obtain("KEY_QUIT_GROUP", Boolean.class);

    /** Key for the dismiss group operation. */
    public static final DataKey<Boolean> KEY_DISMISS_GROUP =
            DataKey.obtain("KEY_DISMISS_GROUP", Boolean.class);

    /** Key for adding favorited group members. */
    public static final DataKey<Boolean> KEY_ADD_GROUP_FOLLOWS =
            DataKey.obtain("KEY_ADD_GROUP_FOLLOWS", Boolean.class);

    /** Key for removing favorited group members. */
    public static final DataKey<Boolean> KEY_REMOVE_GROUP_FOLLOWS =
            DataKey.obtain("KEY_REMOVE_GROUP_FOLLOWS", Boolean.class);

    /** Key for the transfer group ownership operation. */
    public static final DataKey<Boolean> KEY_TRANSFER_GROUP_OWNER =
            DataKey.obtain("KEY_TRANSFER_GROUP_OWNER", Boolean.class);

    /** Key for the add group managers operation. */
    public static final DataKey<Boolean> KEY_ADD_GROUP_MANAGERS =
            DataKey.obtain("KEY_ADD_GROUP_MANAGERS", Boolean.class);

    /** Key for the remove group managers operation. */
    public static final DataKey<Boolean> KEY_REMOVE_GROUP_MANAGERS =
            DataKey.obtain("KEY_REMOVE_GROUP_MANAGERS", Boolean.class);

    /** Key for the join group operation. */
    public static final DataKey<Integer> KEY_JOIN_GROUP =
            DataKey.obtain("KEY_JOIN_GROUP", Integer.class);

    private final String groupId;

    /**
     * Constructor that initializes the group ID.
     *
     * @param channelIdentifier the channel identifier
     */
    public GroupOperationsHandler(@NonNull ChannelIdentifier channelIdentifier) {
        this.groupId = channelIdentifier.getChannelId();
    }

    @Deprecated
    public void createGroup(GroupInfo groupInfo, List<String> inviteeUserIds) {
        CreateGroupParams params =
                new CreateGroupParams(
                        groupInfo.getGroupId(), groupInfo.getGroupName(), inviteeUserIds);
        if (groupInfo.getPortraitUri() != null) {
            params.setPortraitUri(groupInfo.getPortraitUri());
        }
        if (groupInfo.getIntroduction() != null) {
            params.setIntroduction(groupInfo.getIntroduction());
        }
        if (groupInfo.getNotice() != null) {
            params.setNotice(groupInfo.getNotice());
        }
        GroupChannel.createGroup(
                params,
                (processCode, errorKeys, error) -> {
                    if (error == null) {
                        notifyDataChange(KEY_CREATE_GROUP, processCode != null ? processCode : 0);
                    } else {
                        notifyDataChange(KEY_CREATE_GROUP, error.getCode());
                        notifyDataError(KEY_CREATE_GROUP, error);
                    }
                });
    }

    public void createGroupExamine(GroupInfo groupInfo, List<String> inviteeUserIds) {
        CreateGroupParams params =
                new CreateGroupParams(
                        groupInfo.getGroupId(), groupInfo.getGroupName(), inviteeUserIds);
        if (groupInfo.getPortraitUri() != null) {
            params.setPortraitUri(groupInfo.getPortraitUri());
        }
        if (groupInfo.getIntroduction() != null) {
            params.setIntroduction(groupInfo.getIntroduction());
        }
        if (groupInfo.getNotice() != null) {
            params.setNotice(groupInfo.getNotice());
        }
        GroupChannel.createGroup(
                params,
                (processCode, errorKeys, error) -> {
                    if (error == null) {
                        notifyDataChange(
                                KEY_CREATE_GROUP_EXAMINE, processCode != null ? processCode : 0);
                    } else {
                        notifyDataChange(KEY_CREATE_GROUP_EXAMINE, error.getCode());
                        notifyDataError(
                                KEY_CREATE_GROUP_EXAMINE,
                                error,
                                errorKeys != null ? errorKeys : Collections.emptyList());
                    }
                });
    }

    /**
     * Invites users to join the group.
     *
     * @param userIds the list of user IDs
     */
    public void inviteUsersToGroup(@NonNull List<String> userIds) {
        new GroupChannel(groupId)
                .inviteUsers(
                        userIds,
                        (processCode, error) -> {
                            if (error == null) {
                                notifyDataChange(
                                        KEY_INVITE_USERS_TO_GROUP,
                                        processCode != null ? processCode : 0);
                            } else {
                                notifyDataError(KEY_INVITE_USERS_TO_GROUP, error);
                            }
                        });
    }

    /**
     * Kicks members from the group.
     *
     * @param userIds the list of user IDs
     * @param config the leave group configuration
     */
    public void kickGroupMembers(List<String> userIds, LeaveGroupConfig config) {
        KickGroupMembersParams params = new KickGroupMembersParams(userIds);
        if (config != null) {
            params.setConfig(config);
        }
        new GroupChannel(groupId)
                .kickMembers(
                        params,
                        error -> {
                            if (error == null) {
                                notifyDataChange(KEY_KICK_GROUP_MEMBERS, true);
                            } else {
                                notifyDataChange(KEY_KICK_GROUP_MEMBERS, false);
                                notifyDataError(KEY_KICK_GROUP_MEMBERS, error);
                            }
                        });
    }

    /**
     * Updates the group info.
     *
     * @param groupInfo the group info
     */
    @Deprecated
    public void updateGroupInfo(@NonNull GroupInfo groupInfo) {
        UpdateGroupInfoParams params = toUpdateGroupInfoParams(groupInfo);
        new GroupChannel(groupId)
                .updateInfo(
                        params,
                        (errorDetail, error) -> {
                            if (error == null) {
                                notifyDataChange(KEY_UPDATE_GROUP_INFO, true);
                            } else {
                                notifyDataChange(KEY_UPDATE_GROUP_INFO, false);
                                notifyDataError(KEY_UPDATE_GROUP_INFO, error);
                            }
                        });
    }

    /**
     * Updates the group info with review.
     *
     * @param groupInfo the group info
     */
    public void updateGroupInfoExamine(@NonNull GroupInfo groupInfo) {
        UpdateGroupInfoParams params = toUpdateGroupInfoParams(groupInfo);
        new GroupChannel(groupId)
                .updateInfo(
                        params,
                        (errorDetail, error) -> {
                            if (error == null) {
                                notifyDataChange(KEY_UPDATE_GROUP_INFO_EXAMINE, true);
                            } else {
                                notifyDataError(
                                        KEY_UPDATE_GROUP_INFO_EXAMINE,
                                        error,
                                        errorDetail != null
                                                ? errorDetail
                                                : Collections.emptyList());
                            }
                        });
    }

    /**
     * Sets group member info.
     *
     * @param userId the user ID
     * @param nickname the nickname
     * @param extra the extra information
     */
    @Deprecated
    public void setGroupMemberInfo(String userId, String nickname, String extra) {
        SetGroupMemberInfoParams params =
                new SetGroupMemberInfoParams(userId, nickname != null ? nickname : "", extra);
        new GroupChannel(groupId)
                .setMemberInfo(
                        params,
                        (errorDetail, error) -> {
                            if (error == null) {
                                notifyDataChange(KEY_SET_GROUP_MEMBER_INFO, true);
                            } else {
                                notifyDataChange(KEY_SET_GROUP_MEMBER_INFO, false);
                                notifyDataError(KEY_SET_GROUP_MEMBER_INFO, error);
                            }
                        });
    }

    /**
     * Sets group member info with review.
     *
     * @param userId the user ID
     * @param nickname the nickname
     * @param extra the extra information
     */
    public void setGroupMemberInfoExamine(String userId, String nickname, String extra) {
        SetGroupMemberInfoParams params =
                new SetGroupMemberInfoParams(userId, nickname != null ? nickname : "", extra);
        new GroupChannel(groupId)
                .setMemberInfo(
                        params,
                        (errorDetail, error) -> {
                            if (error == null) {
                                notifyDataChange(KEY_SET_GROUP_MEMBER_INFO_EXAMINE, true);
                            } else {
                                notifyDataError(
                                        KEY_SET_GROUP_MEMBER_INFO_EXAMINE,
                                        error,
                                        errorDetail != null
                                                ? errorDetail
                                                : Collections.emptyList());
                            }
                        });
    }

    /**
     * Quits the group.
     *
     * @param config the leave group configuration
     */
    public void quitGroup(LeaveGroupConfig config) {
        new GroupChannel(groupId)
                .leave(
                        config,
                        error -> {
                            if (error == null) {
                                notifyDataChange(KEY_QUIT_GROUP, true);
                            } else {
                                notifyDataChange(KEY_QUIT_GROUP, false);
                                notifyDataError(KEY_QUIT_GROUP, error);
                            }
                        });
    }

    /** Dismisses the group. */
    public void dismissGroup() {
        new GroupChannel(groupId)
                .dismiss(
                        error -> {
                            if (error == null) {
                                notifyDataChange(KEY_DISMISS_GROUP, true);
                            } else {
                                notifyDataChange(KEY_DISMISS_GROUP, false);
                                notifyDataError(KEY_DISMISS_GROUP, error);
                            }
                        });
    }

    /**
     * Adds favorited group members.
     *
     * @param userIds the list of user IDs
     * @since 5.12.2
     */
    public void addGroupFollows(List<String> userIds) {
        new GroupChannel(groupId)
                .addFavorites(
                        userIds,
                        error -> {
                            if (error == null) {
                                notifyDataChange(KEY_ADD_GROUP_FOLLOWS, true);
                            } else {
                                notifyDataChange(KEY_ADD_GROUP_FOLLOWS, false);
                                notifyDataError(KEY_ADD_GROUP_FOLLOWS, error);
                            }
                        });
    }

    /**
     * Removes favorited group members.
     *
     * @param userIds the list of user IDs
     * @since 5.12.2
     */
    public void removeGroupFollows(List<String> userIds) {
        new GroupChannel(groupId)
                .removeFavorites(
                        userIds,
                        error -> {
                            if (error == null) {
                                notifyDataChange(KEY_REMOVE_GROUP_FOLLOWS, true);
                            } else {
                                notifyDataChange(KEY_REMOVE_GROUP_FOLLOWS, false);
                                notifyDataError(KEY_REMOVE_GROUP_FOLLOWS, error);
                            }
                        });
    }

    /**
     * Transfers group ownership.
     *
     * @param newOwnerId the new owner's user ID
     * @param quitGroup whether to quit the group after transfer
     * @param config the leave group configuration
     * @since 5.12.2
     */
    public void transferGroupOwner(
            final String newOwnerId, final boolean quitGroup, final LeaveGroupConfig config) {
        TransferGroupOwnerParams params = new TransferGroupOwnerParams(newOwnerId);
        params.setLeaveAfterTransfer(quitGroup);
        if (config != null) {
            params.setConfig(config);
        }
        new GroupChannel(groupId)
                .transferOwner(
                        params,
                        error -> {
                            if (error == null) {
                                notifyDataChange(KEY_TRANSFER_GROUP_OWNER, true);
                            } else {
                                notifyDataChange(KEY_TRANSFER_GROUP_OWNER, false);
                                notifyDataError(KEY_TRANSFER_GROUP_OWNER, error);
                            }
                        });
    }

    /**
     * Adds group managers.
     *
     * @param userIds the list of user IDs
     * @since 5.12.2
     */
    public void addGroupManagers(List<String> userIds) {
        new GroupChannel(groupId)
                .addManagers(
                        userIds,
                        error -> {
                            if (error == null) {
                                notifyDataChange(KEY_ADD_GROUP_MANAGERS, true);
                            } else {
                                notifyDataChange(KEY_ADD_GROUP_MANAGERS, false);
                                notifyDataError(KEY_ADD_GROUP_MANAGERS, error);
                            }
                        });
    }

    /**
     * Removes group managers.
     *
     * @param userIds the list of user IDs
     * @since 5.12.2
     */
    public void removeGroupManagers(List<String> userIds) {
        new GroupChannel(groupId)
                .removeManagers(
                        userIds,
                        error -> {
                            if (error == null) {
                                notifyDataChange(KEY_REMOVE_GROUP_MANAGERS, true);
                            } else {
                                notifyDataChange(KEY_REMOVE_GROUP_MANAGERS, false);
                                notifyDataError(KEY_REMOVE_GROUP_MANAGERS, error);
                            }
                        });
    }

    /**
     * Joins the group.
     *
     * <p>The join permission determines whether the user can join directly. When the group requires
     * approval, the NC_GROUP_JOIN_GROUP_NEED_MANAGER_ACCEPT status code is returned, indicating the
     * user needs to wait for the owner or manager to approve.
     *
     * @since 5.34.0
     */
    public void joinGroup() {
        new GroupChannel(groupId)
                .join(
                        (processCode, error) -> {
                            if (error == null) {
                                notifyDataChange(
                                        KEY_JOIN_GROUP, processCode != null ? processCode : 0);
                            } else {
                                notifyDataChange(KEY_JOIN_GROUP, error.getCode());
                                notifyDataError(KEY_JOIN_GROUP, error);
                            }
                        });
    }

    private static UpdateGroupInfoParams toUpdateGroupInfoParams(@NonNull GroupInfo groupInfo) {
        UpdateGroupInfoParams params = new UpdateGroupInfoParams();
        params.setGroupName(groupInfo.getGroupName());
        params.setPortraitUri(groupInfo.getPortraitUri());
        params.setIntroduction(groupInfo.getIntroduction());
        params.setNotice(groupInfo.getNotice());
        params.setJoinPermission(groupInfo.getJoinPermission());
        params.setRemoveMemberPermission(groupInfo.getRemoveMemberPermission());
        params.setInvitePermission(groupInfo.getInvitePermission());
        params.setInviteHandlePermission(groupInfo.getInviteHandlePermission());
        params.setGroupInfoEditPermission(groupInfo.getGroupInfoEditPermission());
        params.setMemberInfoEditPermission(groupInfo.getMemberInfoEditPermission());
        return params;
    }
}
