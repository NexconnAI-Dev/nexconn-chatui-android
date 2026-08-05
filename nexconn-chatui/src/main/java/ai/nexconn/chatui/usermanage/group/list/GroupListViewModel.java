package ai.nexconn.chatui.usermanage.group.list;

import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnPagedDataLoader;
import ai.nexconn.chatui.usermanage.handler.GroupJoinedPagedHandler;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;

/**
 * ViewModel for group list.
 *
 * @since 5.12.2
 */
public class GroupListViewModel extends BaseViewModel {

    private final MutableLiveData<List<GroupInfo>> allGroupInfoListLiveData =
            new MutableLiveData<>();

    protected final GroupJoinedPagedHandler groupJoinedPagedHandler;

    public GroupListViewModel(@NonNull Bundle arguments) {
        super(arguments);
        int maxCount = arguments.getInt(ChatUIConstants.KEY_MAX_COUNT_PAGED, 50);
        int validatedMaxMemberCountPaged = Math.max(1, Math.min(100, maxCount));
        groupJoinedPagedHandler = new GroupJoinedPagedHandler(validatedMaxMemberCountPaged);
        groupJoinedPagedHandler.addDataChangeListener(
                GroupJoinedPagedHandler.KEY_GET_JOINED_GROUPS_BY_ROLE,
                new SafeDataHandler<List<GroupInfo>>() {
                    @Override
                    public void onDataChange(List<GroupInfo> groupInfos) {
                        allGroupInfoListLiveData.postValue(groupInfos);
                    }
                });

        refreshJoinedGroupList();
    }

    public LiveData<List<GroupInfo>> getAllGroupInfoListLiveData() {
        return allGroupInfoListLiveData;
    }

    void refreshJoinedGroupList() {
        groupJoinedPagedHandler.getJoinedGroupsByRole(GroupMemberRole.UNDEF);
    }

    OnPagedDataLoader getOnPageDataLoader() {
        return groupJoinedPagedHandler;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupJoinedPagedHandler.stop();
    }
}
