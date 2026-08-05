package ai.nexconn.chatui.usermanage.group.search;

import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnPagedDataLoader;
import ai.nexconn.chatui.usermanage.handler.GroupJoinedSearchPagedHandler;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for group search.
 *
 * @since 5.12.2
 */
public class GroupSearchViewModel extends BaseViewModel {

    private final MutableLiveData<List<GroupInfo>> filteredGroupInfoListLiveData =
            new MutableLiveData<>();
    protected final GroupJoinedSearchPagedHandler groupJoinedSearchPagedHandler;

    public GroupSearchViewModel(@NonNull Bundle arguments) {
        super(arguments);
        int maxCount = arguments.getInt(ChatUIConstants.KEY_MAX_COUNT_PAGED, 50);
        int validatedMaxMemberCountPaged = Math.max(1, Math.min(100, maxCount));

        groupJoinedSearchPagedHandler =
                new GroupJoinedSearchPagedHandler(validatedMaxMemberCountPaged);
        groupJoinedSearchPagedHandler.addDataChangeListener(
                GroupJoinedSearchPagedHandler.KEY_SEARCH_JOINED_GROUPS,
                new SafeDataHandler<List<GroupInfo>>() {
                    @Override
                    public void onDataChange(List<GroupInfo> groupInfos) {
                        filteredGroupInfoListLiveData.postValue(groupInfos);
                    }
                });
    }

    public LiveData<List<GroupInfo>> getFilteredGroupInfoListLiveData() {
        return filteredGroupInfoListLiveData;
    }

    /**
     * Search joined groups.
     *
     * @param query search keyword
     */
    public void searchJoinedGroups(@NonNull String query) {
        if (TextUtils.isEmpty(query)) {
            filteredGroupInfoListLiveData.postValue(new ArrayList<>());
            return;
        }
        groupJoinedSearchPagedHandler.searchJoinedGroups(query);
    }

    OnPagedDataLoader getOnPageDataLoader() {
        return groupJoinedSearchPagedHandler;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupJoinedSearchPagedHandler.stop();
    }
}
