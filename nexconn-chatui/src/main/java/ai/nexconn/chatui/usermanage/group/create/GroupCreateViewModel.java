package ai.nexconn.chatui.usermanage.group.create;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.channel.model.GroupJoinPermission;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.usermanage.handler.GroupOperationsHandler;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ViewModel for creating a group.
 *
 * @since 5.12.0
 */
public class GroupCreateViewModel extends BaseViewModel {

    private final String groupId;
    private final ArrayList<String> inviteeUserIds;
    private final Random rand = new Random();

    protected final GroupOperationsHandler groupOperationsHandler;

    public GroupCreateViewModel(@NonNull Bundle arguments) {
        super(arguments);
        groupId = generateOrRetrieveGroupId(arguments);
        inviteeUserIds = arguments.getStringArrayList(ChatUIConstants.KEY_INVITEE_USER_IDS);
        groupOperationsHandler =
                new GroupOperationsHandler(
                        new ChannelIdentifier(ai.nexconn.chat.channel.ChannelType.GROUP, ""));
    }

    /**
     * Get the group ID.
     *
     * @return group ID
     */
    public String getGroupId() {
        return groupId;
    }

    List<String> getInviteeUserIds() {
        return inviteeUserIds;
    }

    /**
     * Create a group.
     *
     * @param groupName group name
     * @param listener data change listener
     */
    @Deprecated
    public void createGroup(String groupName, OnDataChangeListener<Integer> listener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_CREATE_GROUP, listener);
        GroupInfo groupInfo =
                new GroupInfo(groupId, groupName, null, null, null, null, GroupJoinPermission.FREE);
        groupOperationsHandler.createGroup(groupInfo, inviteeUserIds);
    }

    /**
     * Create a group with examination.
     *
     * @param groupName group name
     * @param listener data change listener
     */
    public void createGroup(String groupName, OnDataChangeEnhancedListener<Integer> listener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_CREATE_GROUP_EXAMINE, listener);
        GroupInfo groupInfo =
                new GroupInfo(groupId, groupName, null, null, null, null, GroupJoinPermission.FREE);
        groupOperationsHandler.createGroupExamine(groupInfo, inviteeUserIds);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupOperationsHandler.stop();
    }

    private String generateOrRetrieveGroupId(Bundle arguments) {
        String id = arguments.getString(ChatUIConstants.KEY_GROUP_ID);
        if (TextUtils.isEmpty(id)) {
            return generateUniqueGroupId();
        } else {
            return id;
        }
    }

    private String generateUniqueGroupId() {
        String allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder uniqueId = new StringBuilder();

        // Add the current timestamp in milliseconds
        long timestamp = System.currentTimeMillis();
        uniqueId.append(Long.toString(timestamp));

        // Fill the remaining length with random characters
        for (int i = uniqueId.length(); i < 32; i++) {
            int index = rand.nextInt(allowedChars.length());
            uniqueId.append(allowedChars.charAt(index));
        }

        return uniqueId.toString();
    }
}
