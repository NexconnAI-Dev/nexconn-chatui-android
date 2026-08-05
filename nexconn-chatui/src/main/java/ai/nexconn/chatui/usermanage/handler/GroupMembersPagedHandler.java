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
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.base.interfaces.OnPagedDataLoader;
import ai.nexconn.chatui.utils.log.RLog;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Paged handler for loading group members.
 *
 * <p>Note: Call {@link #stop()} to release resources when done.
 *
 * @since 5.12.0
 */
public class GroupMembersPagedHandler extends MultiDataHandler implements OnPagedDataLoader {

    private static final String TAG = GroupMembersPagedHandler.class.getSimpleName();

    public static final DataKey<List<GroupMemberInfo>> KEY_GET_GROUP_MEMBERS =
            MultiDataHandler.DataKey.obtain(
                    "KEY_GET_GROUP_MEMBERS", (Class<List<GroupMemberInfo>>) (Class<?>) List.class);

    private static final DataKey<Boolean> KEY_LOAD_MORE =
            DataKey.obtain("KEY_LOAD_MORE", Boolean.class);

    private final int pageCount;

    private final String groupId;
    private final List<GroupMemberInfo> groupMemberInfos = new ArrayList<>();

    private GroupMemberRole currentGroupMemberRole;
    private boolean isOnlyRole;

    private volatile boolean isLoading = false;
    private volatile boolean isLoadNext = false;
    private GroupMemberRole groupMemberRole;

    private GroupMembersByRoleQuery currentQuery;

    public GroupMembersPagedHandler(@NonNull ChannelIdentifier channelIdentifier) {
        this(channelIdentifier, 50);
    }

    public GroupMembersPagedHandler(@NonNull ChannelIdentifier channelIdentifier, int pageCount) {
        this.pageCount = pageCount;
        this.groupId = channelIdentifier.getChannelId();
        NCEngine.INSTANCE.addGroupChannelHandler(
                "GroupMembersPagedHandler_" + hashCode(), groupChannelHandler);
    }

    /**
     * Gets group members by role.
     *
     * @param groupMemberRole the group member role
     */
    public void getGroupMembersByRole(@NonNull GroupMemberRole groupMemberRole) {
        if (isLoading) {
            RLog.d(TAG, "getGroupMembersByRole is loaded");
            return;
        }
        this.groupMemberRole = groupMemberRole;
        groupMemberInfos.clear();
        isLoading = true;
        isLoadNext = false;
        currentQuery = null;
        if (groupMemberRole == GroupMemberRole.UNDEF) {
            fetchNextRole(GroupMemberRole.UNDEF);
        } else {
            startRoleQuery(groupMemberRole, true);
        }
    }

    private void startRoleQuery(GroupMemberRole role, boolean isOnlyRole) {
        this.currentGroupMemberRole = role;
        this.isOnlyRole = isOnlyRole;
        GroupMembersByRoleQueryParams params = new GroupMembersByRoleQueryParams(groupId);
        params.setRole(role);
        params.setPageSize(pageCount);
        params.setAscending(true);
        currentQuery = GroupChannel.createGroupMembersByRoleQuery(params);
        loadCurrentPage();
    }

    private void loadCurrentPage() {
        currentQuery.loadNextPage(
                (result, error) -> {
                    if (error == null) {
                        if (result != null
                                && result.getData() != null
                                && !result.getData().isEmpty()) {
                            groupMemberInfos.addAll(result.getData());
                        }
                        if (!isOnlyRole) {
                            if (currentQuery.getHasMore()) {
                                loadCurrentPage();
                            } else {
                                fetchNextRole(currentGroupMemberRole);
                            }
                        } else {
                            isLoading = false;
                            notifyDataChange(KEY_GET_GROUP_MEMBERS, groupMemberInfos);
                        }
                        notifyDataChange(KEY_LOAD_MORE, hasNext());
                    } else {
                        isLoading = false;
                        notifyDataError(KEY_GET_GROUP_MEMBERS, error);
                        notifyDataChange(KEY_LOAD_MORE, false);
                    }
                });
    }

    private void fetchNextRole(GroupMemberRole lastRole) {
        if (lastRole == GroupMemberRole.UNDEF) {
            startRoleQuery(GroupMemberRole.OWNER, false);
        } else if (lastRole == GroupMemberRole.OWNER) {
            startRoleQuery(GroupMemberRole.ADMIN, false);
        } else if (lastRole == GroupMemberRole.ADMIN) {
            startRoleQuery(GroupMemberRole.NORMAL, true);
        }
    }

    @Override
    public void loadNext(OnDataChangeListener<Boolean> listener) {
        isLoadNext = true;
        replaceDataChangeListener(KEY_LOAD_MORE, listener);
        loadCurrentPage();
    }

    @Override
    public boolean hasNext() {
        return currentQuery != null && currentQuery.getHasMore();
    }

    @Override
    public void stop() {
        super.stop();
        NCEngine.INSTANCE.removeGroupChannelHandler("GroupMembersPagedHandler_" + hashCode());
    }

    // todo
    private final GroupChannelHandler groupChannelHandler =
            new GroupChannelHandler() {
                @Override
                public void onGroupOperation(@NonNull GroupOperationEvent event) {
                    if (!isLoadNext
                            && Objects.equals(
                                    event.getGroupId(), GroupMembersPagedHandler.this.groupId)) {
                        getGroupMembersByRole(groupMemberRole);
                    }
                }

                @Override
                public void onGroupInfoChanged(@NonNull GroupInfoChangedEvent event) {}

                @Override
                public void onGroupMemberInfoChanged(@NonNull GroupMemberInfoChangedEvent event) {
                    if (!isLoadNext
                            && Objects.equals(
                                    event.getGroupId(), GroupMembersPagedHandler.this.groupId)) {
                        getGroupMembersByRole(groupMemberRole);
                    }
                }

                @Override
                public void onGroupApplicationEvent(@NonNull GroupApplicationEvent event) {}

                @Override
                public void onGroupFavoritesChangedSync(
                        @NonNull GroupFavoritesChangedSyncEvent event) {}
            };
}
