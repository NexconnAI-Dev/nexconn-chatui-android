package ai.nexconn.chatui.utils.bundle;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.channel.model.GroupInviteHandlePermission;
import ai.nexconn.chat.channel.model.GroupJoinPermission;
import ai.nexconn.chat.channel.model.GroupMemberInfoEditPermission;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chat.channel.model.GroupOperationPermission;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Bundle serialization utility for storing and retrieving non-Parcelable/Serializable nexconn model
 * objects in a Bundle.
 */
public final class ChatUIBundleUtils {

    private static final String SUFFIX_CHANNEL_TYPE = "_channel_type";
    private static final String SUFFIX_CHANNEL_ID = "_channel_id";

    private ChatUIBundleUtils() {}

    public static void putChannelIdentifier(
            @NonNull Bundle bundle, @NonNull String key, @Nullable ChannelIdentifier identifier) {
        if (identifier != null) {
            bundle.putInt(key + SUFFIX_CHANNEL_TYPE, identifier.getChannelType().getValue());
            bundle.putString(key + SUFFIX_CHANNEL_ID, identifier.getChannelId());
        }
    }

    @Nullable
    public static ChannelIdentifier getChannelIdentifier(
            @Nullable Bundle bundle, @NonNull String key) {
        if (bundle == null || !bundle.containsKey(key + SUFFIX_CHANNEL_ID)) return null;
        int typeValue = bundle.getInt(key + SUFFIX_CHANNEL_TYPE);
        String channelId = bundle.getString(key + SUFFIX_CHANNEL_ID);
        ChannelType type = ChannelType.fromValue(typeValue);
        if (type == null || channelId == null) return null;
        return new ChannelIdentifier(type, channelId);
    }

    public static void putGroupInfo(
            @NonNull Bundle bundle, @NonNull String key, @Nullable GroupInfo groupInfo) {
        if (groupInfo == null) return;
        Bundle sub = new Bundle();
        sub.putString("groupId", groupInfo.getGroupId());
        if (groupInfo.getGroupName() != null) sub.putString("groupName", groupInfo.getGroupName());
        if (groupInfo.getPortraitUri() != null)
            sub.putString("portraitUri", groupInfo.getPortraitUri());
        if (groupInfo.getIntroduction() != null)
            sub.putString("introduction", groupInfo.getIntroduction());
        if (groupInfo.getNotice() != null) sub.putString("notice", groupInfo.getNotice());
        if (groupInfo.getJoinPermission() != null)
            sub.putInt("joinPermission", groupInfo.getJoinPermission().getValue());
        if (groupInfo.getRemoveMemberPermission() != null)
            sub.putInt("removeMemberPermission", groupInfo.getRemoveMemberPermission().getValue());
        if (groupInfo.getInvitePermission() != null)
            sub.putInt("invitePermission", groupInfo.getInvitePermission().getValue());
        if (groupInfo.getInviteHandlePermission() != null)
            sub.putInt("inviteHandlePermission", groupInfo.getInviteHandlePermission().getValue());
        if (groupInfo.getGroupInfoEditPermission() != null)
            sub.putInt(
                    "groupInfoEditPermission", groupInfo.getGroupInfoEditPermission().getValue());
        if (groupInfo.getMemberInfoEditPermission() != null)
            sub.putInt(
                    "memberInfoEditPermission", groupInfo.getMemberInfoEditPermission().getValue());
        if (groupInfo.getCreatorId() != null) sub.putString("creatorId", groupInfo.getCreatorId());
        if (groupInfo.getOwnerId() != null) sub.putString("ownerId", groupInfo.getOwnerId());
        sub.putLong("createTime", groupInfo.getCreateTime());
        sub.putInt("membersCount", groupInfo.getMembersCount());
        sub.putLong("joinedTime", groupInfo.getJoinedTime());
        if (groupInfo.getRole() != null) sub.putInt("role", groupInfo.getRole().getValue());
        bundle.putBundle(key, sub);
    }

    @Nullable
    public static GroupInfo getGroupInfo(@Nullable Bundle bundle, @NonNull String key) {
        if (bundle == null) return null;
        Bundle sub = bundle.getBundle(key);
        if (sub == null) return null;
        String groupId = sub.getString("groupId");
        if (groupId == null) return null;

        GroupJoinPermission joinPermission =
                sub.containsKey("joinPermission")
                        ? GroupJoinPermission.fromValue(sub.getInt("joinPermission"))
                        : null;
        GroupOperationPermission removeMemberPermission =
                sub.containsKey("removeMemberPermission")
                        ? GroupOperationPermission.fromValue(sub.getInt("removeMemberPermission"))
                        : null;
        GroupOperationPermission invitePermission =
                sub.containsKey("invitePermission")
                        ? GroupOperationPermission.fromValue(sub.getInt("invitePermission"))
                        : null;
        GroupInviteHandlePermission inviteHandlePermission =
                sub.containsKey("inviteHandlePermission")
                        ? GroupInviteHandlePermission.fromValue(
                                sub.getInt("inviteHandlePermission"))
                        : null;
        GroupOperationPermission groupInfoEditPermission =
                sub.containsKey("groupInfoEditPermission")
                        ? GroupOperationPermission.fromValue(sub.getInt("groupInfoEditPermission"))
                        : null;
        GroupMemberInfoEditPermission memberInfoEditPermission =
                sub.containsKey("memberInfoEditPermission")
                        ? GroupMemberInfoEditPermission.fromValue(
                                sub.getInt("memberInfoEditPermission"))
                        : null;
        GroupMemberRole role =
                sub.containsKey("role") ? GroupMemberRole.fromValue(sub.getInt("role")) : null;

        return new GroupInfo(
                groupId,
                sub.getString("groupName"),
                sub.getString("portraitUri"),
                sub.getString("introduction"),
                sub.getString("notice"),
                null,
                joinPermission,
                removeMemberPermission,
                invitePermission,
                inviteHandlePermission,
                groupInfoEditPermission,
                memberInfoEditPermission,
                sub.getString("creatorId"),
                sub.getString("ownerId"),
                sub.getLong("createTime", 0),
                sub.getInt("membersCount", 0),
                sub.getLong("joinedTime", 0),
                role);
    }
}
