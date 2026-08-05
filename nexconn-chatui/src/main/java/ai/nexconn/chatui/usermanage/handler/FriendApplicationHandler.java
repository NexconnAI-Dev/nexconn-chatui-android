package ai.nexconn.chatui.usermanage.handler;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.ErrorHandler;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.model.PageResult;
import ai.nexconn.chat.params.FriendApplicationsQueryParams;
import ai.nexconn.chat.user.model.FriendApplicationDetail;
import ai.nexconn.chat.user.model.FriendApplicationStatus;
import ai.nexconn.chat.user.model.FriendApplicationType;
import ai.nexconn.chat.user.query.FriendApplicationsQuery;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.base.interfaces.OnDataChangeListener;
import java.util.Arrays;

/**
 * Friend application handler.
 *
 * <p>Note: Call {@link #stop()} to release resources when done.
 *
 * @since 5.12.0
 */
public class FriendApplicationHandler extends MultiDataHandler {

    public static final DataKey<PageResult> KEY_GET_FRIEND_APPLICATIONS =
            MultiDataHandler.DataKey.obtain("KEY_GET_FRIEND_APPLICATIONS", PageResult.class);
    public static final DataKey<Boolean> KEY_ACCEPT_FRIEND_APPLICATIONS =
            MultiDataHandler.DataKey.obtain("KEY_ACCEPT_FRIEND_APPLICATIONS", Boolean.class);

    public static final DataKey<Boolean> KEY_REJECT_FRIEND_APPLICATIONS =
            MultiDataHandler.DataKey.obtain("KEY_REJECT_FRIEND_APPLICATIONS", Boolean.class);

    public void getFriendApplications(
            FriendApplicationType[] type, FriendApplicationStatus[] status) {
        getFriendApplications(new FriendApplicationsQueryParams(), type, status);
    }

    public void getFriendApplications(
            FriendApplicationsQueryParams params,
            FriendApplicationType[] type,
            FriendApplicationStatus[] status) {

        if (type != null) {
            params.setApplicationTypes(Arrays.asList(type));
        }
        if (status != null) {
            params.setApplicationStatuses(Arrays.asList(status));
        }
        FriendApplicationsQuery query =
                NCEngine.getUserModule().createFriendApplicationsQuery(params);
        query.loadNextPage(
                new OperationHandler<PageResult<FriendApplicationDetail>>() {
                    @Override
                    public void onResult(
                            PageResult<FriendApplicationDetail> result, NCError error) {
                        if (error == null && result != null) {
                            notifyDataChange(KEY_GET_FRIEND_APPLICATIONS, result);
                        }
                    }
                });
    }

    public void acceptFriendApplication(String userId, OnDataChangeListener<Boolean> listener) {
        replaceDataChangeListener(KEY_ACCEPT_FRIEND_APPLICATIONS, listener);
        NCEngine.getUserModule()
                .acceptFriendApplication(
                        userId,
                        new ErrorHandler() {
                            @Override
                            public void onError(NCError error) {
                                if (error == null) {
                                    notifyDataChange(KEY_ACCEPT_FRIEND_APPLICATIONS, true);
                                } else {
                                    notifyDataChange(KEY_ACCEPT_FRIEND_APPLICATIONS, false);
                                }
                            }
                        });
    }

    public void refuseFriendApplication(String userId, OnDataChangeListener<Boolean> listener) {
        replaceDataChangeListener(KEY_REJECT_FRIEND_APPLICATIONS, listener);
        NCEngine.getUserModule()
                .refuseFriendApplication(
                        userId,
                        new ErrorHandler() {
                            @Override
                            public void onError(NCError error) {
                                if (error == null) {
                                    notifyDataChange(KEY_REJECT_FRIEND_APPLICATIONS, true);
                                } else {
                                    notifyDataChange(KEY_REJECT_FRIEND_APPLICATIONS, false);
                                }
                            }
                        });
    }
}
