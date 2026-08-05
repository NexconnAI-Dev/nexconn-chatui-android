package ai.nexconn.chatui.handler;

import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.DirectChannel;
import ai.nexconn.chat.channel.GroupChannel;
import ai.nexconn.chat.channel.OpenChannel;
import ai.nexconn.chat.channel.SystemChannel;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.query.MessagesReadReceiptUsersQuery;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.model.MessageReadReceiptStatus;
import ai.nexconn.chat.message.model.ReadReceiptInfo;
import ai.nexconn.chat.message.model.ReadReceiptUser;
import ai.nexconn.chat.model.PageResult;
import ai.nexconn.chat.params.MessagesReadReceiptUsersQueryParams;
import ai.nexconn.chatui.base.MultiDataHandler;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;

/**
 * Data handler for the read-receipt detail screen.
 *
 * <p>Fetches and provides the list of users who have read a specific message.
 *
 * @since 5.30.0
 */
public class ReadReceiptDetailHandler extends MultiDataHandler {

    private static final String TAG = "ReadReceiptDetailHandler";

    /** Data key for querying the V5 read-receipt info of a specific message. */
    public static final DataKey<ReadReceiptInfo> KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5 =
            DataKey.obtain("KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5", ReadReceiptInfo.class);

    @SuppressWarnings("unchecked")
    public static final DataKey<List<ReadReceiptUser>> KEY_MESSAGE_READ_V5_USER_LIST =
            DataKey.obtain(
                    "KEY_MESSAGE_READ_V5_USER_LIST",
                    (Class<List<ReadReceiptUser>>) (Class<?>) List.class);

    @SuppressWarnings("unchecked")
    public static final DataKey<List<ReadReceiptUser>> KEY_MESSAGE_UNREAD_V5_USER_LIST =
            DataKey.obtain(
                    "KEY_MESSAGE_UNREAD_V5_USER_LIST",
                    (Class<List<ReadReceiptUser>>) (Class<?>) List.class);

    private final int pageCount = 100;
    private MessagesReadReceiptUsersQuery readUsersQuery = null;
    private MessagesReadReceiptUsersQuery unreadUsersQuery = null;
    private final List<ReadReceiptUser> readUsers = new ArrayList<>();
    private final List<ReadReceiptUser> unreadUsers = new ArrayList<>();
    private boolean isRequestReadList = false;
    private boolean isRequestUnreadList = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final int maxRetryCount = 5;
    private int retryCountForReadInfoV5 = 0;

    public ReadReceiptDetailHandler() {
        super();
    }

    @Override
    public void stop() {
        super.stop();
    }

    /**
     * Fetches V5 read-receipt info for a message.
     *
     * @param channelIdentifier channel identifier
     * @param messageId message ID
     */
    public void getMessageReadReceiptInfoV5(ChannelIdentifier channelIdentifier, String messageId) {
        if (!isAlive()) {
            return;
        }
        if (channelIdentifier == null || messageId == null) {
            return;
        }
        if (!AppSettingsHandler.getInstance()
                .isReadReceiptV5Enabled(channelIdentifier.getChannelType())) {
            return;
        }
        List<String> uIds = new ArrayList<>();
        uIds.add(messageId);
        createChannel(channelIdentifier)
                .getMessageReadReceiptInfo(
                        uIds,
                        new OperationHandler<List<ReadReceiptInfo>>() {
                            @Override
                            public void onResult(List<ReadReceiptInfo> result, NCError error) {
                                if (error != null) {
                                    if (error.getCode() == 20607) {
                                        getMessageReadReceiptInfoV5(channelIdentifier, messageId);
                                    } else {
                                        notifyDataChange(
                                                KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5, null);
                                    }
                                    return;
                                }
                                if (result != null && !result.isEmpty()) {
                                    notifyDataChange(
                                            KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5, result.get(0));
                                    return;
                                }
                                if (retryCountForReadInfoV5++ < maxRetryCount) {
                                    runDelay(
                                            () ->
                                                    getMessageReadReceiptInfoV5(
                                                            channelIdentifier, messageId));
                                } else {
                                    notifyDataChange(KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5, null);
                                }
                            }
                        });
    }

    /**
     * Fetches V5 read-receipt info for a message — convenience overload.
     *
     * @param message the message to query
     */
    public void getMessageReadReceiptInfoV5(Message message) {
        if (message == null) return;
        getMessageReadReceiptInfoV5(message.getChannelIdentifier(), message.getMessageId());
    }

    /**
     * Fetches the list of users who have read a message, paginated.
     *
     * @param channelIdentifier channel identifier
     * @param messageId message ID
     */
    public void getMessagesReadUsersByPage(ChannelIdentifier channelIdentifier, String messageId) {
        if (!isAlive()) {
            return;
        }
        if (isRequestReadList) {
            return;
        }
        isRequestReadList = true;
        if (readUsersQuery == null) {
            MessagesReadReceiptUsersQueryParams params =
                    new MessagesReadReceiptUsersQueryParams(channelIdentifier, messageId);
            params.setPageSize(pageCount);
            params.setStatus(MessageReadReceiptStatus.RESPONDED);
            readUsersQuery = BaseChannel.createMessagesReadReceiptUsersQuery(params);
        }
        if (!readUsersQuery.getHasMore()) {
            isRequestReadList = false;
            return;
        }
        readUsersQuery.loadNextPage(
                new OperationHandler<PageResult<ReadReceiptUser>>() {
                    @Override
                    public void onResult(PageResult<ReadReceiptUser> result, NCError error) {
                        isRequestReadList = false;
                        if (error != null) {
                            notifyDataError(KEY_MESSAGE_READ_V5_USER_LIST, error);
                            return;
                        }
                        if (result != null && result.getData() != null) {
                            readUsers.addAll(result.getData());
                        }
                        notifyDataChange(KEY_MESSAGE_READ_V5_USER_LIST, new ArrayList<>(readUsers));
                    }
                });
    }

    /**
     * Fetches the list of users who have read a message — convenience overload.
     *
     * @param message the message to query
     */
    public void getMessagesReadUsersByPage(Message message) {
        if (message == null) return;
        getMessagesReadUsersByPage(message.getChannelIdentifier(), message.getMessageId());
    }

    /**
     * Fetches the list of users who have NOT read a message, paginated.
     *
     * @param channelIdentifier channel identifier
     * @param messageId message ID
     */
    public void getMessagesUnReadUsersByPage(
            ChannelIdentifier channelIdentifier, String messageId) {
        if (!isAlive()) {
            return;
        }
        if (isRequestUnreadList) {
            return;
        }
        isRequestUnreadList = true;
        if (unreadUsersQuery == null) {
            MessagesReadReceiptUsersQueryParams params =
                    new MessagesReadReceiptUsersQueryParams(channelIdentifier, messageId);
            params.setPageSize(pageCount);
            params.setStatus(MessageReadReceiptStatus.UNRESPONDED);
            unreadUsersQuery = BaseChannel.createMessagesReadReceiptUsersQuery(params);
        }
        if (!unreadUsersQuery.getHasMore()) {
            isRequestUnreadList = false;
            return;
        }
        unreadUsersQuery.loadNextPage(
                new OperationHandler<PageResult<ReadReceiptUser>>() {
                    @Override
                    public void onResult(PageResult<ReadReceiptUser> result, NCError error) {
                        isRequestUnreadList = false;
                        if (error != null) {
                            notifyDataError(KEY_MESSAGE_UNREAD_V5_USER_LIST, error);
                            return;
                        }
                        if (result != null && result.getData() != null) {
                            unreadUsers.addAll(result.getData());
                        }
                        notifyDataChange(
                                KEY_MESSAGE_UNREAD_V5_USER_LIST, new ArrayList<>(unreadUsers));
                    }
                });
    }

    /**
     * Fetches the list of users who have NOT read a message — convenience overload.
     *
     * @param message the message to query
     */
    public void getMessagesUnReadUsersByPage(Message message) {
        if (message == null) return;
        getMessagesUnReadUsersByPage(message.getChannelIdentifier(), message.getMessageId());
    }

    private void runDelay(Runnable runnable) {
        mainHandler.postDelayed(runnable, 1000);
    }

    private static BaseChannel createChannel(ChannelIdentifier id) {
        if (id.getChannelType() == ChannelType.GROUP) return new GroupChannel(id.getChannelId());
        if (id.getChannelType() == ChannelType.OPEN) return new OpenChannel(id.getChannelId());
        if (id.getChannelType() == ChannelType.SYSTEM) return new SystemChannel(id.getChannelId());
        return new DirectChannel(id.getChannelId());
    }
}
