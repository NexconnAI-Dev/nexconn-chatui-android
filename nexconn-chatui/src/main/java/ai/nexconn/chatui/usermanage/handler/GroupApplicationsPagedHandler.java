package ai.nexconn.chatui.usermanage.handler;

import ai.nexconn.chat.channel.GroupChannel;
import ai.nexconn.chat.channel.model.GroupApplicationDirection;
import ai.nexconn.chat.channel.model.GroupApplicationInfo;
import ai.nexconn.chat.channel.model.GroupApplicationStatus;
import ai.nexconn.chat.channel.query.GroupApplicationsQuery;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.model.PageData;
import ai.nexconn.chat.params.GroupApplicationsQueryParams;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import ai.nexconn.chatui.base.interfaces.OnPagedDataLoader;
import ai.nexconn.chatui.utils.log.RLog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Paged handler for fetching group applications.
 *
 * <p>Note: Call {@link #stop()} to release resources when done.
 *
 * @since 5.12.2
 */
public class GroupApplicationsPagedHandler extends MultiDataHandler implements OnPagedDataLoader {

    private static final String TAG = GroupApplicationsPagedHandler.class.getSimpleName();

    public static final DataKey<List<GroupApplicationInfo>> KEY_GET_GROUP_APPLICATIONS =
            DataKey.obtain(
                    "KEY_GET_GROUP_APPLICATIONS",
                    (Class<List<GroupApplicationInfo>>) (Class<?>) List.class);

    private static final DataKey<Boolean> KEY_LOAD_MORE =
            DataKey.obtain("KEY_LOAD_MORE", Boolean.class);

    private final int pageCount;

    private final List<GroupApplicationInfo> groupApplicationInfos = new ArrayList<>();

    private GroupApplicationsQuery query;

    private volatile boolean isLoading = false;

    public GroupApplicationsPagedHandler() {
        this(50);
    }

    public GroupApplicationsPagedHandler(int pageCount) {
        this.pageCount = pageCount;
    }

    /**
     * Gets group applications.
     *
     * @param directions the application directions
     * @param status the application statuses
     */
    public void getGroupApplications(
            final GroupApplicationDirection[] directions, final GroupApplicationStatus[] status) {
        if (isLoading) {
            RLog.d(TAG, "getGroupApplications is loaded");
            return;
        }
        groupApplicationInfos.clear();
        isLoading = true;

        GroupApplicationsQueryParams params = new GroupApplicationsQueryParams();
        params.setPageSize(pageCount);
        if (directions != null) {
            params.setDirections(Arrays.asList(directions));
        }
        if (status != null) {
            params.setStatus(Arrays.asList(status));
        }
        query = GroupChannel.createGroupApplicationsQuery(params);
        loadNextPageInternal();
    }

    private void loadNextPageInternal() {
        isLoading = true;
        query.loadNextPage(
                new OperationHandler<PageData<GroupApplicationInfo>>() {
                    @Override
                    public void onResult(PageData<GroupApplicationInfo> result, NCError error) {
                        if (error == null && result != null) {
                            if (result.getData() != null && !result.getData().isEmpty()) {
                                groupApplicationInfos.addAll(result.getData());
                            }
                            isLoading = false;
                            notifyDataChange(KEY_GET_GROUP_APPLICATIONS, groupApplicationInfos);
                            notifyDataChange(KEY_LOAD_MORE, hasNext());
                        } else {
                            isLoading = false;
                            if (error != null) {
                                notifyDataError(KEY_GET_GROUP_APPLICATIONS, error);
                            }
                            notifyDataChange(KEY_LOAD_MORE, false);
                        }
                    }
                });
    }

    @Override
    public void loadNext(OnDataChangeListener<Boolean> listener) {
        replaceDataChangeListener(KEY_LOAD_MORE, listener);
        loadNextPageInternal();
    }

    @Override
    public boolean hasNext() {
        return query != null && query.getHasMore();
    }
}
