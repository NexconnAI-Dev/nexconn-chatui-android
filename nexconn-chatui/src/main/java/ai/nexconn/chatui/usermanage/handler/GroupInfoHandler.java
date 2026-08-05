package ai.nexconn.chatui.usermanage.handler;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.GroupChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupApplicationEvent;
import ai.nexconn.chat.channel.model.GroupFavoritesChangedSyncEvent;
import ai.nexconn.chat.channel.model.GroupFollowDetail;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.channel.model.GroupInfoChangedEvent;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.channel.model.GroupMemberInfoChangedEvent;
import ai.nexconn.chat.channel.model.GroupOperationEvent;
import ai.nexconn.chat.channel.query.SearchGroupMembersQuery;
import ai.nexconn.chat.handler.GroupChannelHandler;
import ai.nexconn.chat.params.SearchGroupMembersQueryParams;
import ai.nexconn.chatui.base.MultiDataHandler;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Handler for fetching group info, group member info, etc.
 *
 * <p>Note: Call {@link #stop()} to release resources when done.
 *
 * @since 5.12.0
 */
public class GroupInfoHandler extends MultiDataHandler {

    public static final DataKey<GroupInfo> KEY_GROUP_INFO =
            MultiDataHandler.DataKey.obtain("KEY_GROUP_INFO", GroupInfo.class);

    public static final DataKey<List<GroupMemberInfo>> KEY_GET_GROUP_MEMBERS =
            MultiDataHandler.DataKey.obtain(
                    "KEY_GET_GROUP_MEMBERS", (Class<List<GroupMemberInfo>>) (Class<?>) List.class);

    public static final DataKey<List<GroupMemberInfo>> KEY_SEARCH_GROUP_MEMBERS =
            MultiDataHandler.DataKey.obtain(
                    "KEY_SEARCH_GROUP_MEMBERS",
                    (Class<List<GroupMemberInfo>>) (Class<?>) List.class);

    public static final DataKey<List<GroupFollowDetail>> KEY_GROUP_FOLLOWS =
            MultiDataHandler.DataKey.obtain(
                    "KEY_GROUP_FOLLOWS", (Class<List<GroupFollowDetail>>) (Class<?>) List.class);

    private final String groupId;
    private List<String> userIds;

    public GroupInfoHandler(@NonNull ChannelIdentifier channelIdentifier) {
        this.groupId = channelIdentifier.getChannelId();
        NCEngine.INSTANCE.addGroupChannelHandler(
                "GroupInfoHandler_" + hashCode(), groupChannelHandler);
    }

    /** Gets group info. */
    public void getGroupsInfo() {
        ArrayList<String> groupIds = new ArrayList<>();
        groupIds.add(groupId);
        GroupChannel.getGroupsInfo(
                groupIds,
                (groupInfos, error) -> {
                    if (error == null) {
                        if (groupInfos != null && !groupInfos.isEmpty()) {
                            notifyDataChange(KEY_GROUP_INFO, groupInfos.get(0));
                        }
                    } else {
                        notifyDataError(KEY_GROUP_INFO, error);
                    }
                });
    }

    /**
     * Gets group member info.
     *
     * @param userIds list of user IDs. If userIds.size > 100, use {@link
     *     GroupMembersByUserIdsHandler#getGroupMembers(List)} instead.
     */
    public void getGroupMembers(List<String> userIds) {
        this.userIds = userIds;
        new GroupChannel(groupId)
                .getMembers(
                        userIds,
                        (groupMemberInfos, error) -> {
                            if (error == null) {
                                notifyDataChange(KEY_GET_GROUP_MEMBERS, groupMemberInfos);
                            } else {
                                notifyDataError(KEY_GET_GROUP_MEMBERS, error);
                            }
                        });
    }

    /**
     * Searches for group members.
     *
     * <p>Please use {@link GroupMembersSearchPagedHandler} for searching instead.
     *
     * @param name the user name to search for
     */
    @Deprecated
    public void searchGroupMembers(String name) {
        SearchGroupMembersQueryParams params = new SearchGroupMembersQueryParams(groupId, name);
        params.setPageSize(100);
        SearchGroupMembersQuery query = GroupChannel.createSearchGroupMembersQuery(params);
        query.loadNextPage(
                (result, error) -> {
                    if (error == null) {
                        if (result != null) {
                            notifyDataChange(KEY_SEARCH_GROUP_MEMBERS, result.getData());
                        }
                    } else {
                        notifyDataError(KEY_SEARCH_GROUP_MEMBERS, error);
                    }
                });
    }

    /** Gets the list of favorited group members. */
    public void getGroupFollows() {
        new GroupChannel(groupId)
                .getFavorites(
                        (followInfos, error) -> {
                            if (error == null) {
                                notifyDataChange(
                                        KEY_GROUP_FOLLOWS,
                                        followInfos == null ? new ArrayList<>() : followInfos);
                            } else {
                                notifyDataError(KEY_GROUP_FOLLOWS, error);
                            }
                        });
    }

    @Override
    public void stop() {
        super.stop();
        NCEngine.INSTANCE.removeGroupChannelHandler("GroupInfoHandler_" + hashCode());
    }

    private final GroupChannelHandler groupChannelHandler =
            new GroupChannelHandler() {
                @Override
                public void onGroupOperation(@NonNull GroupOperationEvent event) {
                    if (Objects.equals(event.getGroupId(), GroupInfoHandler.this.groupId)) {
                        getGroupsInfo();
                    }
                }

                @Override
                public void onGroupInfoChanged(@NonNull GroupInfoChangedEvent event) {
                    if (event.getGroupInfo() != null
                            && Objects.equals(
                                    event.getGroupInfo().getGroupId(),
                                    GroupInfoHandler.this.groupId)) {
                        getGroupsInfo();
                    }
                }

                @Override
                public void onGroupMemberInfoChanged(@NonNull GroupMemberInfoChangedEvent event) {
                    if (Objects.equals(event.getGroupId(), GroupInfoHandler.this.groupId)
                            && GroupInfoHandler.this.userIds != null
                            && event.getMemberInfo() != null
                            && GroupInfoHandler.this.userIds.contains(
                                    event.getMemberInfo().getUserId())) {
                        getGroupMembers(GroupInfoHandler.this.userIds);
                    }
                }

                @Override
                public void onGroupApplicationEvent(@NonNull GroupApplicationEvent event) {}

                @Override
                public void onGroupFavoritesChangedSync(
                        @NonNull GroupFavoritesChangedSyncEvent event) {
                    if (Objects.equals(event.getGroupId(), GroupInfoHandler.this.groupId)) {
                        getGroupFollows();
                    }
                }
            };
}
