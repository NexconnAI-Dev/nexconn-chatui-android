package ai.nexconn.chatui.usermanage.handler;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.GroupChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupApplicationEvent;
import ai.nexconn.chat.channel.model.GroupFavoritesChangedSyncEvent;
import ai.nexconn.chat.channel.model.GroupInfoChangedEvent;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.channel.model.GroupMemberInfoChangedEvent;
import ai.nexconn.chat.channel.model.GroupOperationEvent;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.GroupChannelHandler;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.utils.log.RLog;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Handler for fetching group members by user IDs.
 *
 * <p>Note: Call {@link #stop()} to release resources when done.
 *
 * @since 5.12.2
 */
public class GroupMembersByUserIdsHandler extends MultiDataHandler {

    private static final String TAG = GroupMembersByUserIdsHandler.class.getSimpleName();

    public static final DataKey<List<GroupMemberInfo>> KEY_GET_GROUP_MEMBERS =
            MultiDataHandler.DataKey.obtain(
                    "KEY_GET_GROUP_MEMBERS", (Class<List<GroupMemberInfo>>) (Class<?>) List.class);

    private static final int MAX_BATCH_SIZE = 100;

    private final String groupId;
    private final List<GroupMemberInfo> groupMemberInfoList = new ArrayList<>();

    private volatile boolean isLoading = false;
    private List<String> userIds;

    public GroupMembersByUserIdsHandler(@NonNull ChannelIdentifier channelIdentifier) {
        this.groupId = channelIdentifier.getChannelId();
        NCEngine.INSTANCE.addGroupChannelHandler(
                "GroupMembersByUserIdsHandler_" + hashCode(), groupChannelHandler);
    }

    public void getGroupMembers(List<String> userIds) {
        if (userIds == null) {
            notifyDataError(
                    KEY_GET_GROUP_MEMBERS,
                    new NCError(-1, "Invalid parameter: userIdList is null"));
            return;
        }
        if (isLoading) {
            RLog.d(TAG, "getGroupMembers is loaded");
            return;
        }
        isLoading = true;
        groupMemberInfoList.clear();
        this.userIds = userIds;
        fetchGroupMembersInBatches(userIds, 0);
    }

    private void fetchGroupMembersInBatches(List<String> userIds, int startIndex) {
        if (startIndex >= userIds.size()) {
            isLoading = false;
            notifyDataChange(KEY_GET_GROUP_MEMBERS, groupMemberInfoList);
            return;
        }

        int endIndex = Math.min(startIndex + MAX_BATCH_SIZE, userIds.size());
        List<String> batch = userIds.subList(startIndex, endIndex);

        new GroupChannel(groupId)
                .getMembers(
                        batch,
                        (groupMemberInfos, error) -> {
                            if (error == null) {
                                if (groupMemberInfos != null) {
                                    groupMemberInfoList.addAll(groupMemberInfos);
                                }
                                if (endIndex < userIds.size()) {
                                    fetchGroupMembersInBatches(userIds, endIndex);
                                } else {
                                    isLoading = false;
                                    notifyDataChange(KEY_GET_GROUP_MEMBERS, groupMemberInfoList);
                                }
                            } else {
                                isLoading = false;
                                notifyDataError(KEY_GET_GROUP_MEMBERS, error);
                            }
                        });
    }

    @Override
    public void stop() {
        super.stop();
        NCEngine.INSTANCE.removeGroupChannelHandler("GroupMembersByUserIdsHandler_" + hashCode());
    }

    private final GroupChannelHandler groupChannelHandler =
            new GroupChannelHandler() {
                @Override
                public void onGroupOperation(@NonNull GroupOperationEvent event) {}

                @Override
                public void onGroupInfoChanged(@NonNull GroupInfoChangedEvent event) {}

                @Override
                public void onGroupMemberInfoChanged(@NonNull GroupMemberInfoChangedEvent event) {
                    if (Objects.equals(
                                    event.getGroupId(), GroupMembersByUserIdsHandler.this.groupId)
                            && GroupMembersByUserIdsHandler.this.userIds != null
                            && event.getMemberInfo() != null
                            && GroupMembersByUserIdsHandler.this.userIds.contains(
                                    event.getMemberInfo().getUserId())) {
                        getGroupMembers(userIds);
                    }
                }

                @Override
                public void onGroupApplicationEvent(@NonNull GroupApplicationEvent event) {}

                @Override
                public void onGroupFavoritesChangedSync(
                        @NonNull GroupFavoritesChangedSyncEvent event) {}
            };
}
