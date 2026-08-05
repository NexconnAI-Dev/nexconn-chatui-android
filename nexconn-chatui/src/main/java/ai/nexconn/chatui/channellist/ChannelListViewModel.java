package ai.nexconn.chatui.channellist;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.model.ConnectionStatus;
import ai.nexconn.chat.params.PinParams;
import ai.nexconn.chat.user.model.UserOnlineStatus;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnPagedDataLoader;
import ai.nexconn.chatui.channellist.handler.ChannelListConnectionHandler;
import ai.nexconn.chatui.channellist.handler.ChannelListDataHandler;
import ai.nexconn.chatui.channellist.handler.ChannelListEventHandler;
import ai.nexconn.chatui.channellist.handler.ChannelListOnlineStatusHandler;
import ai.nexconn.chatui.channellist.handler.ChannelListReadReceiptHandler;
import ai.nexconn.chatui.channellist.handler.ChannelListReferenceMessageHandler;
import ai.nexconn.chatui.channellist.handler.ChannelListUserInfoHandler;
import ai.nexconn.chatui.channellist.model.BaseUiChannel;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.NoticeContent;
import ai.nexconn.chatui.notification.ChatUINotificationManager;
import ai.nexconn.chatui.utils.system.NetUtils;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Channel list ViewModel
 *
 * <p>Coordinates all Handlers, manages LiveData, and exposes data to Fragment.
 *
 * @since 5.10.4
 */
public class ChannelListViewModel extends BaseViewModel {

    private final MutableLiveData<List<BaseUiChannel>> conversationListLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<NoticeContent> noticeContentLiveData = new MutableLiveData<>();

    protected final ChannelListDataHandler dataHandler;
    private final ChannelListEventHandler eventHandler;
    private final ChannelListConnectionHandler connectionHandler;
    private final ChannelListUserInfoHandler userInfoHandler;
    private final ChannelListReadReceiptHandler readReceiptHandler;
    private final ChannelListOnlineStatusHandler onlineStatusHandler;
    private final ChannelListReferenceMessageHandler referenceMessageHandler;

    private volatile List<BaseUiChannel> cachedList = new CopyOnWriteArrayList<>();

    public ChannelListViewModel(@NonNull Bundle arguments) {
        this(
                arguments,
                new ChannelListDataHandler(NCChatUI.getContext()),
                new ChannelListEventHandler(),
                new ChannelListConnectionHandler(),
                new ChannelListUserInfoHandler(),
                new ChannelListReadReceiptHandler(),
                new ChannelListOnlineStatusHandler(),
                new ChannelListReferenceMessageHandler());
    }

    ChannelListViewModel(
            @NonNull Bundle arguments,
            ChannelListDataHandler dataHandler,
            ChannelListEventHandler eventHandler,
            ChannelListConnectionHandler connectionHandler,
            ChannelListUserInfoHandler userInfoHandler,
            ChannelListReadReceiptHandler readReceiptHandler,
            ChannelListOnlineStatusHandler onlineStatusHandler,
            ChannelListReferenceMessageHandler referenceMessageHandler) {
        super(arguments);

        this.dataHandler = dataHandler;
        this.eventHandler = eventHandler;
        this.connectionHandler = connectionHandler;
        this.userInfoHandler = userInfoHandler;
        this.readReceiptHandler = readReceiptHandler;
        this.onlineStatusHandler = onlineStatusHandler;
        this.referenceMessageHandler = referenceMessageHandler;

        setupDataListeners();
        setupEventListeners();
        setupConnectionListeners();
        setupUserInfoListeners();
        setupReadReceiptListeners();
        setupOnlineStatusListeners();
        setupReferenceMessageListeners();
    }

    // region Handler data listener initialization

    private void setupDataListeners() {
        dataHandler.addDataChangeListener(
                ChannelListDataHandler.KEY_CONVERSATION_LIST,
                new SafeDataHandler<List<BaseUiChannel>>() {
                    @Override
                    public void onDataChange(List<BaseUiChannel> data) {
                        handleConversationListChanged(data);
                    }
                });
    }

    private void setupEventListeners() {
        eventHandler.addDataChangeListener(
                ChannelListEventHandler.KEY_REFRESH_NEEDED,
                new SafeDataHandler<Long>() {
                    @Override
                    public void onDataChange(Long delayTime) {
                        dataHandler.refresh(delayTime == null ? 0L : delayTime);
                    }
                });

        eventHandler.addDataChangeListener(
                ChannelListEventHandler.KEY_DELETED_MSG_CONVERSATION,
                new SafeDataHandler<int[]>() {
                    @Override
                    public void onDataChange(int[] deletedMsgIds) {
                        handleDeletedMsgConversation(deletedMsgIds);
                    }
                });
    }

    private void setupConnectionListeners() {
        connectionHandler.addDataChangeListener(
                ChannelListConnectionHandler.KEY_CONNECTION_STATUS,
                new SafeDataHandler<ConnectionStatus>() {
                    @Override
                    public void onDataChange(ConnectionStatus status) {
                        if (ConnectionStatus.CONNECTED.equals(status)) {
                            dataHandler.refresh();
                        }
                        NoticeContent notice = buildNoticeContent(status);
                        if (notice != null) {
                            noticeContentLiveData.postValue(notice);
                        }
                    }
                });
    }

    private void setupUserInfoListeners() {
        userInfoHandler.addDataChangeListener(
                ChannelListUserInfoHandler.KEY_USER_UPDATE,
                new SafeDataHandler<ai.nexconn.chat.user.model.UserInfo>() {
                    @Override
                    public void onDataChange(ai.nexconn.chat.user.model.UserInfo info) {
                        if (eventHandler.isInBatchOfflineMode()) {
                            return;
                        }
                        for (BaseUiChannel c : cachedList) {
                            c.onUserInfoUpdate(info);
                        }
                        publishCachedList();
                    }
                });

        userInfoHandler.addDataChangeListener(
                ChannelListUserInfoHandler.KEY_GROUP_UPDATE,
                new SafeDataHandler<ai.nexconn.chat.channel.model.GroupInfo>() {
                    @Override
                    public void onDataChange(ai.nexconn.chat.channel.model.GroupInfo group) {
                        if (eventHandler.isInBatchOfflineMode()) {
                            return;
                        }
                        for (BaseUiChannel c : cachedList) {
                            c.onGroupInfoUpdate(group);
                        }
                        publishCachedList();
                    }
                });

        userInfoHandler.addDataChangeListener(
                ChannelListUserInfoHandler.KEY_GROUP_MEMBER_UPDATE,
                new SafeDataHandler<ai.nexconn.chatui.userinfo.model.GroupUserInfo>() {
                    @Override
                    public void onDataChange(
                            ai.nexconn.chatui.userinfo.model.GroupUserInfo groupUserInfo) {
                        if (eventHandler.isInBatchOfflineMode()) {
                            return;
                        }
                        for (BaseUiChannel c : cachedList) {
                            c.onGroupMemberUpdate(groupUserInfo);
                        }
                        publishCachedList();
                    }
                });
    }

    private void setupReadReceiptListeners() {
        readReceiptHandler.addDataChangeListener(
                ChannelListReadReceiptHandler.KEY_SYNC_READ_STATUS,
                new SafeDataHandler<ChannelIdentifier>() {
                    @Override
                    public void onDataChange(ChannelIdentifier id) {
                        handleSyncReadStatus(id);
                    }
                });

        readReceiptHandler.addDataChangeListener(
                ChannelListReadReceiptHandler.KEY_READ_RECEIPT_V5_UPDATE,
                new SafeDataHandler<
                        HashMap<String, ai.nexconn.chat.message.model.ReadReceiptInfo>>() {
                    @Override
                    public void onDataChange(
                            HashMap<String, ai.nexconn.chat.message.model.ReadReceiptInfo> data) {
                        handleReadReceiptV5Update(data);
                    }
                });

        readReceiptHandler.addDataChangeListener(
                ChannelListReadReceiptHandler.KEY_MESSAGE_MODIFIED,
                new SafeDataHandler<Boolean>() {
                    @Override
                    public void onDataChange(Boolean data) {
                        dataHandler.refresh();
                    }
                });
    }

    private void setupOnlineStatusListeners() {
        onlineStatusHandler.addDataChangeListener(
                ChannelListOnlineStatusHandler.KEY_ONLINE_STATUS_CHANGED,
                new SafeDataHandler<Map<String, UserOnlineStatus>>() {
                    @Override
                    public void onDataChange(Map<String, UserOnlineStatus> statuses) {
                        handleOnlineStatusChanged(statuses);
                    }
                });

        onlineStatusHandler.setDataSourceFromConversationList(cachedList);
    }

    private void setupReferenceMessageListeners() {
        referenceMessageHandler.addDataChangeListener(
                ChannelListReferenceMessageHandler.KEY_REFERENCE_MESSAGE_UPDATE,
                new SafeDataHandler<Map<ChannelIdentifier, ai.nexconn.chat.message.Message>>() {
                    @Override
                    public void onDataChange(
                            Map<ChannelIdentifier, ai.nexconn.chat.message.Message> data) {
                        handleReferenceMessageUpdate(data);
                    }
                });
    }

    // endregion

    // region Internal event handling

    void handleConversationListChanged(List<BaseUiChannel> data) {
        cachedList = data;
        onlineStatusHandler.attachCacheOnlineStatus(data);
        conversationListLiveData.postValue(data);
        if (!eventHandler.isInBatchMode()) {
            onlineStatusHandler.fetchOnlineStatus(data);
            readReceiptHandler.collectAndQueryReadReceiptInfo(data);
            referenceMessageHandler.collectAndRefreshReferenceMessages(data);
            userInfoHandler.preloadConversationInfos(data);
        }
    }

    void handleOnlineStatusChanged(Map<String, UserOnlineStatus> statuses) {
        for (BaseUiChannel c : cachedList) {
            if (c.mCore != null && ChannelType.DIRECT == c.mCore.getChannelType()) {
                UserOnlineStatus status = statuses.get(c.mCore.getChannelId());
                if (status != null) {
                    c.setOnlineStatus(status);
                }
            }
        }
        publishCachedList();
    }

    private void publishCachedList() {
        conversationListLiveData.postValue(new java.util.ArrayList<>(cachedList));
    }

    private void handleDeletedMsgConversation(int[] deletedMsgIds) {
        Set<Integer> deleteSet = new HashSet<>(deletedMsgIds.length);
        for (int id : deletedMsgIds) {
            deleteSet.add(id);
        }
        for (BaseUiChannel c : cachedList) {
            if (c.mCore != null && c.mCore.getLatestMessage() != null) {
                int clientId = c.mCore.getLatestMessage().getClientId();
                if (deleteSet.contains(clientId)) {
                    dataHandler.refresh();
                    return;
                }
            }
        }
    }

    private void handleSyncReadStatus(ChannelIdentifier id) {
        dataHandler.refresh();
        // Re-query read receipt info after sync event to update read status in conversation list
        readReceiptHandler.collectAndQueryReadReceiptInfo(cachedList);
    }

    private void handleReadReceiptV5Update(
            HashMap<String, ai.nexconn.chat.message.model.ReadReceiptInfo> data) {
        boolean needRefresh = false;

        for (BaseUiChannel c : cachedList) {
            if (c.mCore == null || c.mCore.getLatestMessage() == null) {
                continue;
            }

            String latestMsgServerId = c.mCore.getLatestMessage().getMessageId();

            if (TextUtils.isEmpty(latestMsgServerId)) {
                // Message ID not yet available (message still sending)
                // Skip for now, will be updated when conversation list refreshes naturally
                continue;
            }

            ai.nexconn.chat.message.model.ReadReceiptInfo receipt = data.get(latestMsgServerId);
            if (receipt != null) {
                c.setReadReceiptInfo(receipt);
                // Also update the message's SentStatus to ensure fallback logic works
                if (receipt.getReadCount() >= 1) {
                    c.mCore
                            .getLatestMessage()
                            .setSentStatus(ai.nexconn.chat.message.model.SentStatus.READ);
                }
                needRefresh = true;
            }
        }

        if (needRefresh) {
            // Create a new list to trigger RecyclerView adapter update
            // This ensures UI refreshes even when the list reference is the same
            publishCachedList();
        }
    }

    private void handleReferenceMessageUpdate(
            Map<ChannelIdentifier, ai.nexconn.chat.message.Message> data) {
        if (data == null || data.isEmpty()) {
            return;
        }
        boolean needRefresh = false;
        for (BaseUiChannel c : cachedList) {
            if (c.mCore == null) {
                continue;
            }
            ChannelIdentifier chId =
                    new ChannelIdentifier(c.mCore.getChannelType(), c.mCore.getChannelId());
            ai.nexconn.chat.message.Message updatedMsg = data.get(chId);
            if (updatedMsg != null) {
                // Reference message status was refreshed, trigger UI update
                needRefresh = true;
            }
        }
        if (needRefresh) {
            publishCachedList();
        }
    }

    // endregion

    // region Connection status to NoticeContent conversion

    protected NoticeContent buildNoticeContent(ConnectionStatus status) {
        if (!NCChatUIConfig.channelListConfig().isEnableConnectStateNotice()) {
            return null;
        }
        android.content.Context context = NCChatUI.getContext();
        Resources resources = context.getResources();
        NoticeContent notice = new NoticeContent();
        int iconResId = ChatUIThemeManager.getAttrResId(context, R.attr.nc_network_unreachable_img);
        boolean netAvailable = NetUtils.isNetWorkAvailable(context);

        if (ConnectionStatus.NETWORK_UNAVAILABLE.equals(status)) {
            notice.setContent(
                    resources.getString(R.string.nc_conversation_list_notice_network_unavailable));
        } else if (ConnectionStatus.KICKED_OFFLINE_BY_OTHER_CLIENT.equals(status)) {
            notice.setContent(resources.getString(R.string.nc_conversation_list_notice_kicked));
        } else if (ConnectionStatus.CONNECTED.equals(status)) {
            notice.setShowNotice(false);
            notice.setIconResId(iconResId);
            return notice;
        } else if (ConnectionStatus.UNCONNECTED.equals(status)) {
            notice.setContent(resources.getString(R.string.nc_conversation_list_notice_disconnect));
        } else if (ConnectionStatus.CONNECTING.equals(status)
                || ConnectionStatus.SUSPENDED.equals(status)) {
            if (netAvailable) {
                notice.setContent(
                        resources.getString(R.string.nc_conversation_list_notice_connecting));
                iconResId = R.drawable.nc_conversationlist_notice_connecting_animated;
            } else {
                // Device has no network: the SDK sits in a reconnect-pending state
                // (SUSPENDED/CONNECTING) but there is physically no network to connect over.
                // Fall back to the network-unavailable notice instead of a misleading "Connecting...".
                notice.setContent(
                        resources.getString(
                                R.string.nc_conversation_list_notice_network_unavailable));
            }
        } else if (ConnectionStatus.PROXY_UNAVAILABLE.equals(status)) {
            notice.setContent(
                    resources.getString(R.string.nc_conversation_list_notice_proxy_unavailable));
        } else {
            notice.setContent(
                    resources.getString(R.string.nc_conversation_list_notice_network_unavailable));
        }

        notice.setShowNotice(true);
        notice.setIconResId(iconResId);
        return notice;
    }

    // endregion

    // region Public API

    public void refresh() {
        dataHandler.refresh();
    }

    public void removeChannel(ChannelType channelType, String channelId) {
        BaseChannel.deleteChannels(
                Collections.singletonList(new ChannelIdentifier(channelType, channelId)),
                (result, error) -> {
                    if (error == null) {
                        dataHandler.refresh();
                    }
                });
    }

    public void setChannelTop(
            ChannelIdentifier identifier, boolean isTop, OperationHandler<Boolean> handler) {
        OperationHandler<Boolean> wrappedHandler =
                (result, error) -> {
                    if (handler != null) handler.onResult(result, error);
                    if (error == null) {
                        dataHandler.refresh();
                    }
                };
        if (isTop) {
            NCChatUI.createChannel(identifier).pin(new PinParams(), wrappedHandler);
        } else {
            NCChatUI.createChannel(identifier).unpin(wrappedHandler);
        }
    }

    public void onResume() {
        onlineStatusHandler.refreshDataSource();
        onlineStatusHandler.fetchOnlineStatus(cachedList);
        clearAllNotification();
        readReceiptHandler.collectAndQueryReadReceiptInfo(cachedList);
        ConnectionStatus currentStatus = NCEngine.getConnectionStatus();
        if (currentStatus != null) {
            NoticeContent notice = buildNoticeContent(currentStatus);
            if (notice != null) {
                noticeContentLiveData.postValue(notice);
            }
        }
        // Refresh the list so that pin/DND changes made on the settings page are
        // reflected immediately when the user navigates back to the conversation list.
        // dataHandler.refresh() is debounced internally, so calling it here is safe.
        dataHandler.refresh();
    }

    public LiveData<List<BaseUiChannel>> getConversationListLiveData() {
        return conversationListLiveData;
    }

    public LiveData<NoticeContent> getNoticeContentLiveData() {
        return noticeContentLiveData;
    }

    public OnPagedDataLoader getPagedDataLoader() {
        return dataHandler;
    }

    // endregion

    private void clearAllNotification() {
        if (NCChatUIConfig.featureConfig().NC_wipe_out_notification_message) {
            ChatUINotificationManager.getInstance().clearAllNotification();
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        dataHandler.stop();
        eventHandler.stop();
        connectionHandler.stop();
        userInfoHandler.stop();
        readReceiptHandler.stop();
        onlineStatusHandler.stop();
        referenceMessageHandler.stop();
    }
}
