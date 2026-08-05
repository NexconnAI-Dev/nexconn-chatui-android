package ai.nexconn.chatui.userinfo;

import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.error.NCError;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class GroupMemberQueryGuard {
    static final int ERROR_GROUP_USER_NOT_IN_GROUP = 25418;

    private final Set<String> blockedGroupIds = Collections.synchronizedSet(new HashSet<>());
    private long generation;

    synchronized boolean shouldQuery(String groupId, GroupInfo groupInfo) {
        if (groupId == null || groupId.isEmpty() || blockedGroupIds.contains(groupId)) {
            return false;
        }
        return groupInfo == null || groupInfo.getJoinedTime() > 0;
    }

    synchronized long currentGeneration() {
        return generation;
    }

    synchronized boolean isCurrentGeneration(long requestGeneration) {
        return generation == requestGeneration;
    }

    synchronized void resetSession() {
        generation++;
        blockedGroupIds.clear();
    }

    synchronized void onQueryResult(long requestGeneration, String groupId, NCError error) {
        if (!isCurrentGeneration(requestGeneration) || groupId == null || groupId.isEmpty()) {
            return;
        }
        if (error == null) {
            blockedGroupIds.remove(groupId);
        } else if (error.getCode() == ERROR_GROUP_USER_NOT_IN_GROUP) {
            blockedGroupIds.add(groupId);
        }
    }

    synchronized void onGroupInfoLoaded(GroupInfo groupInfo) {
        if (groupInfo != null && groupInfo.getJoinedTime() > 0) {
            blockedGroupIds.remove(groupInfo.getGroupId());
        }
    }
}
