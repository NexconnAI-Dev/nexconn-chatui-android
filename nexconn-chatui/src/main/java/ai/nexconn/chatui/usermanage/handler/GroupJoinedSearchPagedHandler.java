package ai.nexconn.chatui.usermanage.handler;

import ai.nexconn.chat.channel.GroupChannel;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.channel.query.SearchJoinedGroupsQuery;
import ai.nexconn.chat.params.SearchJoinedGroupsQueryParams;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.base.interfaces.OnPagedDataLoader;
import ai.nexconn.chatui.utils.log.RLog;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Paged handler for searching joined groups.
 *
 * <p>Note: Call {@link #stop()} to release resources when done.
 *
 * @since 5.12.2
 */
public class GroupJoinedSearchPagedHandler extends MultiDataHandler implements OnPagedDataLoader {

    private static final String TAG = GroupJoinedSearchPagedHandler.class.getSimpleName();

    public static final DataKey<List<GroupInfo>> KEY_SEARCH_JOINED_GROUPS =
            DataKey.obtain(
                    "KEY_SEARCH_JOINED_GROUPS", (Class<List<GroupInfo>>) (Class<?>) List.class);

    private static final DataKey<Boolean> KEY_LOAD_MORE =
            DataKey.obtain("KEY_LOAD_MORE", Boolean.class);

    private final int pageCount;

    private final List<GroupInfo> groupInfos = new ArrayList<>();

    private SearchJoinedGroupsQuery query;

    private volatile boolean isLoading = false;

    public GroupJoinedSearchPagedHandler() {
        this(50);
    }

    public GroupJoinedSearchPagedHandler(int pageCount) {
        this.pageCount = pageCount;
    }

    /**
     * Searches joined groups by name.
     *
     * @param groupName the group name to search for
     */
    public void searchJoinedGroups(@NonNull String groupName) {
        if (isLoading) {
            RLog.d(TAG, "getJoinedGroupsByRole is loaded");
            return;
        }
        groupInfos.clear();
        isLoading = true;

        SearchJoinedGroupsQueryParams params = new SearchJoinedGroupsQueryParams(groupName);
        params.setPageSize(pageCount);
        query = GroupChannel.createSearchJoinedGroupsQuery(params);

        loadPage();
    }

    private void loadPage() {
        query.loadNextPage(
                (result, error) -> {
                    if (error == null) {
                        if (result != null
                                && result.getData() != null
                                && !result.getData().isEmpty()) {
                            groupInfos.addAll(result.getData());
                        }
                        isLoading = false;
                        notifyDataChange(KEY_SEARCH_JOINED_GROUPS, groupInfos);
                        notifyDataChange(KEY_LOAD_MORE, hasNext());
                    } else {
                        isLoading = false;
                        notifyDataError(KEY_SEARCH_JOINED_GROUPS, error);
                        notifyDataChange(KEY_LOAD_MORE, false);
                    }
                });
    }

    @Override
    public void loadNext(OnDataChangeListener<Boolean> listener) {
        replaceDataChangeListener(KEY_LOAD_MORE, listener);
        loadPage();
    }

    @Override
    public boolean hasNext() {
        return query != null && query.getHasMore();
    }
}
