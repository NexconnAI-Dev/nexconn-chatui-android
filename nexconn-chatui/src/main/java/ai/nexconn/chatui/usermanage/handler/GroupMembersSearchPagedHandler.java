package ai.nexconn.chatui.usermanage.handler;

import ai.nexconn.chat.channel.GroupChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.channel.query.SearchGroupMembersQuery;
import ai.nexconn.chat.params.SearchGroupMembersQueryParams;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.base.interfaces.OnPagedDataLoader;
import ai.nexconn.chatui.utils.log.RLog;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Paged handler for searching group members.
 *
 * <p>Note: Call {@link #stop()} to release resources when done.
 *
 * @since 5.12.2
 */
public class GroupMembersSearchPagedHandler extends MultiDataHandler implements OnPagedDataLoader {

    private static final String TAG = GroupMembersSearchPagedHandler.class.getSimpleName();

    public static final DataKey<List<GroupMemberInfo>> KEY_SEARCH_GROUP_MEMBERS =
            DataKey.obtain(
                    "KEY_SEARCH_GROUP_MEMBERS",
                    (Class<List<GroupMemberInfo>>) (Class<?>) List.class);

    private static final DataKey<Boolean> KEY_LOAD_MORE =
            DataKey.obtain("KEY_LOAD_MORE", Boolean.class);

    private final int pageCount;

    private final List<GroupMemberInfo> groupMemberInfos = new ArrayList<>();

    private final String groupId;
    private SearchGroupMembersQuery query;

    private volatile boolean isLoading = false;

    public GroupMembersSearchPagedHandler(@NonNull ChannelIdentifier channelIdentifier) {
        this(channelIdentifier, 50);
    }

    public GroupMembersSearchPagedHandler(
            @NonNull ChannelIdentifier channelIdentifier, int pageCount) {
        this.pageCount = pageCount;
        this.groupId = channelIdentifier.getChannelId();
    }

    public void searchGroupMembers(@NonNull String name) {
        if (isLoading) {
            RLog.d(TAG, "searchGroupMembers is loaded");
            return;
        }
        groupMemberInfos.clear();
        isLoading = true;

        SearchGroupMembersQueryParams params = new SearchGroupMembersQueryParams(groupId, name);
        params.setPageSize(pageCount);
        params.setAscending(true);
        query = GroupChannel.createSearchGroupMembersQuery(params);

        loadPage();
    }

    private void loadPage() {
        query.loadNextPage(
                (result, error) -> {
                    if (error == null) {
                        if (result != null
                                && result.getData() != null
                                && !result.getData().isEmpty()) {
                            groupMemberInfos.addAll(result.getData());
                        }
                        isLoading = false;
                        notifyDataChange(KEY_SEARCH_GROUP_MEMBERS, groupMemberInfos);
                        notifyDataChange(KEY_LOAD_MORE, hasNext());
                    } else {
                        isLoading = false;
                        notifyDataError(KEY_SEARCH_GROUP_MEMBERS, error);
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
