package ai.nexconn.chatui.usermanage.handler;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.GroupChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupApplicationEvent;
import ai.nexconn.chat.channel.model.GroupFavoritesChangedSyncEvent;
import ai.nexconn.chat.channel.model.GroupInfoChangedEvent;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.channel.model.GroupMemberInfoChangedEvent;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chat.channel.model.GroupOperationEvent;
import ai.nexconn.chat.channel.query.GroupMembersByRoleQuery;
import ai.nexconn.chat.handler.GroupChannelHandler;
import ai.nexconn.chat.params.GroupMembersByRoleQueryParams;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.utils.log.RLog;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Handler for loading all group members.
 *
 * <p>Note: Call {@link #stop()} to release resources when done.
 *
 * @since 5.12.0
 */
public class GroupMembersFullHandler extends MultiDataHandler {

    private static final String TAG = GroupMembersFullHandler.class.getSimpleName();

    public static final DataKey<List<GroupMemberInfo>> KEY_GET_ALL_GROUP_MEMBERS_BY_ROLES =
            DataKey.obtain(
                    "KEY_GET_ALL_GROUP_MEMBERS_BY_ROLES",
                    (Class<List<GroupMemberInfo>>) (Class<?>) List.class);

    private final String groupId;
    private final List<GroupMemberInfo> groupMemberInfos = new ArrayList<>();

    private volatile boolean isLoading = false;
    private GroupMemberRole groupMemberRole;

    public GroupMembersFullHandler(@NonNull ChannelIdentifier channelIdentifier) {
        this.groupId = channelIdentifier.getChannelId();
        NCEngine.INSTANCE.addGroupChannelHandler(
                "GroupMembersFullHandler_" + hashCode(), groupChannelHandler);
    }

    /**
     * Gets all group members by role.
     *
     * @param groupMemberRole the group member role
     */
    public void getAllGroupMembersByRole(@NonNull GroupMemberRole groupMemberRole) {
        if (isLoading) {
            RLog.d(TAG, "getAllGroupMembersByRole is loaded");
            return;
        }
        this.groupMemberRole = groupMemberRole;
        groupMemberInfos.clear();
        isLoading = true;
        if (groupMemberRole == GroupMemberRole.UNDEF) {
            allFetchNextRole(GroupMemberRole.UNDEF);
        } else {
            fetchAllPagesByRole(groupMemberRole, true);
        }
    }

    private void allFetchNextRole(GroupMemberRole lastRole) {
        if (lastRole == GroupMemberRole.UNDEF) {
            fetchAllPagesByRole(GroupMemberRole.OWNER, false);
        } else if (lastRole == GroupMemberRole.OWNER) {
            fetchAllPagesByRole(GroupMemberRole.ADMIN, false);
        } else if (lastRole == GroupMemberRole.ADMIN) {
            fetchAllPagesByRole(GroupMemberRole.NORMAL, false);
        }
    }

    private void fetchAllPagesByRole(GroupMemberRole role, boolean isOnlyRole) {
        GroupMembersByRoleQueryParams params = new GroupMembersByRoleQueryParams(groupId);
        params.setRole(role);
        params.setPageSize(100);
        params.setAscending(true);
        GroupMembersByRoleQuery query = GroupChannel.createGroupMembersByRoleQuery(params);
        fetchAllPages(query, role, isOnlyRole);
    }

    private void fetchAllPages(
            GroupMembersByRoleQuery query, GroupMemberRole role, boolean isOnlyRole) {
        query.loadNextPage(
                (result, error) -> {
                    if (error == null) {
                        if (result != null
                                && result.getData() != null
                                && !result.getData().isEmpty()) {
                            groupMemberInfos.addAll(result.getData());
                        }
                        if (query.getHasMore()) {
                            fetchAllPages(query, role, isOnlyRole);
                        } else if (!isOnlyRole && role != GroupMemberRole.NORMAL) {
                            allFetchNextRole(role);
                        } else {
                            isLoading = false;
                            notifyDataChange(KEY_GET_ALL_GROUP_MEMBERS_BY_ROLES, groupMemberInfos);
                        }
                    } else {
                        isLoading = false;
                        notifyDataError(KEY_GET_ALL_GROUP_MEMBERS_BY_ROLES, error);
                    }
                });
    }

    @Override
    public void stop() {
        super.stop();
        NCEngine.INSTANCE.removeGroupChannelHandler("GroupMembersFullHandler_" + hashCode());
    }

    private final GroupChannelHandler groupChannelHandler =
            new GroupChannelHandler() {
                @Override
                public void onGroupOperation(@NonNull GroupOperationEvent event) {
                    if (Objects.equals(event.getGroupId(), GroupMembersFullHandler.this.groupId)) {
                        getAllGroupMembersByRole(groupMemberRole);
                    }
                }

                @Override
                public void onGroupInfoChanged(@NonNull GroupInfoChangedEvent event) {}

                @Override
                public void onGroupMemberInfoChanged(@NonNull GroupMemberInfoChangedEvent event) {
                    if (Objects.equals(event.getGroupId(), GroupMembersFullHandler.this.groupId)) {
                        getAllGroupMembersByRole(groupMemberRole);
                    }
                }

                @Override
                public void onGroupApplicationEvent(@NonNull GroupApplicationEvent event) {}

                @Override
                public void onGroupFavoritesChangedSync(
                        @NonNull GroupFavoritesChangedSyncEvent event) {}
            };
}
