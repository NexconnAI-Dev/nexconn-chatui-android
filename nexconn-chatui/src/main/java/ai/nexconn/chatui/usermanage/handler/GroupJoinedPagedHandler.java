package ai.nexconn.chatui.usermanage.handler;

import ai.nexconn.chat.channel.GroupChannel;
import ai.nexconn.chat.channel.model.GroupInfo;
import ai.nexconn.chat.channel.model.GroupMemberRole;
import ai.nexconn.chat.channel.query.JoinedGroupsByRoleQuery;
import ai.nexconn.chat.params.JoinedGroupsByRoleQueryParams;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.base.interfaces.OnPagedDataLoader;
import ai.nexconn.chatui.utils.log.RLog;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Paged handler for fetching joined groups.
 *
 * <p>Note: Call {@link #stop()} to release resources when done.
 *
 * @since 5.12.2
 */
public class GroupJoinedPagedHandler extends MultiDataHandler implements OnPagedDataLoader {

    private static final String TAG = GroupJoinedPagedHandler.class.getSimpleName();

    public static final DataKey<List<GroupInfo>> KEY_GET_JOINED_GROUPS_BY_ROLE =
            DataKey.obtain(
                    "KEY_GET_JOINED_GROUPS_BY_ROLE",
                    (Class<List<GroupInfo>>) (Class<?>) List.class);

    private static final DataKey<Boolean> KEY_LOAD_MORE =
            DataKey.obtain("KEY_LOAD_MORE", Boolean.class);

    private final int pageCount;

    private final List<GroupInfo> groupInfos = new ArrayList<>();

    private JoinedGroupsByRoleQuery query;

    private volatile boolean isLoading = false;

    public GroupJoinedPagedHandler() {
        this(50);
    }

    public GroupJoinedPagedHandler(int pageCount) {
        this.pageCount = pageCount;
    }

    /**
     * Gets joined groups by role.
     *
     * @param groupMemberRole the group member role
     */
    public void getJoinedGroupsByRole(@NonNull GroupMemberRole groupMemberRole) {
        if (isLoading) {
            RLog.d(TAG, "getJoinedGroupsByRole is loaded");
            return;
        }
        groupInfos.clear();
        isLoading = true;

        JoinedGroupsByRoleQueryParams params = new JoinedGroupsByRoleQueryParams();
        params.setRole(groupMemberRole);
        params.setPageSize(pageCount);
        query = GroupChannel.createJoinedGroupsByRoleQuery(params);

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
                        notifyDataChange(KEY_GET_JOINED_GROUPS_BY_ROLE, groupInfos);
                        notifyDataChange(KEY_LOAD_MORE, hasNext());
                    } else {
                        isLoading = false;
                        notifyDataError(KEY_GET_JOINED_GROUPS_BY_ROLE, error);
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
