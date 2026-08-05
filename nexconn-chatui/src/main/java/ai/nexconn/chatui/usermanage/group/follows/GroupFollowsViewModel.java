package ai.nexconn.chatui.usermanage.group.follows;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupFollowDetail;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.model.ContactModel;
import ai.nexconn.chatui.usermanage.handler.GroupInfoHandler;
import ai.nexconn.chatui.usermanage.handler.GroupMembersByUserIdsHandler;
import ai.nexconn.chatui.usermanage.handler.GroupOperationsHandler;
import ai.nexconn.chatui.utils.bundle.ChatUIBundleUtils;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ViewModel for group follows list.
 *
 * @since 5.12.2
 */
public class GroupFollowsViewModel extends BaseViewModel {

    private final MutableLiveData<List<ContactModel>> allGroupFollowsLiveData =
            new MutableLiveData<>();

    protected final GroupInfoHandler groupInfoHandler;
    protected final GroupMembersByUserIdsHandler groupMembersByUserIdsHandler;
    protected final GroupOperationsHandler groupOperationsHandler;

    public GroupFollowsViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ChannelIdentifier conversationIdentifier =
                ChatUIBundleUtils.getChannelIdentifier(
                        arguments, ChatUIConstants.KEY_CHANNEL_IDENTIFIER);

        groupInfoHandler = new GroupInfoHandler(conversationIdentifier);
        groupMembersByUserIdsHandler = new GroupMembersByUserIdsHandler(conversationIdentifier);
        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);

        groupMembersByUserIdsHandler.addDataChangeListener(
                GroupMembersByUserIdsHandler.KEY_GET_GROUP_MEMBERS,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        allGroupFollowsLiveData.postValue(
                                sortAndCategorizeContacts(groupMemberInfos));
                    }
                });
        groupInfoHandler.addDataChangeListener(
                GroupInfoHandler.KEY_GROUP_FOLLOWS,
                groupFollowsInfos -> {
                    if (groupFollowsInfos == null || groupFollowsInfos.isEmpty()) {
                        allGroupFollowsLiveData.postValue(Collections.emptyList());
                        return;
                    }
                    List<String> groupFollowsIds = new ArrayList<>();
                    for (GroupFollowDetail followDetail : groupFollowsInfos) {
                        groupFollowsIds.add(followDetail.getUserId());
                    }
                    groupMembersByUserIdsHandler.getGroupMembers(groupFollowsIds);
                });
    }

    public LiveData<List<ContactModel>> getAllGroupFollowsLiveData() {
        return allGroupFollowsLiveData;
    }

    /**
     * Remove group follows.
     *
     * @param userIds user ID list
     * @param listener data change listener
     */
    public void removeGroupFollows(List<String> userIds, OnDataChangeListener<Boolean> listener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_REMOVE_GROUP_FOLLOWS, listener);
        groupOperationsHandler.removeGroupFollows(userIds);
    }

    @NonNull
    private List<ContactModel> sortAndCategorizeContacts(List<GroupMemberInfo> groupMemberInfos) {
        if (groupMemberInfos == null || groupMemberInfos.isEmpty()) {
            return Collections.emptyList();
        }
        List<ContactModel> contactModels = new ArrayList<>();
        for (GroupMemberInfo groupMemberInfo : groupMemberInfos) {
            // Add contact ContactModel
            contactModels.add(
                    ContactModel.obtain(
                            groupMemberInfo,
                            ContactModel.ItemType.CONTENT,
                            ContactModel.CheckType.NONE));
        }

        return contactModels;
    }

    void refreshGroupFollows() {
        groupInfoHandler.getGroupFollows();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupInfoHandler.stop();
        groupMembersByUserIdsHandler.stop();
        groupOperationsHandler.stop();
    }
}
