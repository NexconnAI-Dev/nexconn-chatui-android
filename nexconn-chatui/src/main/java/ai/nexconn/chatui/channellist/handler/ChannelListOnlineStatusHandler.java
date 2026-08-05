package ai.nexconn.chatui.channellist.handler;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.user.model.UserOnlineStatus;
import ai.nexconn.chatui.base.MultiDataHandler;
import ai.nexconn.chatui.channellist.model.BaseUiChannel;
import ai.nexconn.chatui.manager.OnLineStatusListener;
import ai.nexconn.chatui.manager.OnLineStatusManager;
import ai.nexconn.chatui.manager.OnlineStatusDataSource;
import ai.nexconn.chatui.utils.log.RLog;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Channel list online status handler
 *
 * <p>Listens for user online status changes and manages the online status data source.
 *
 * @since 5.10.4
 */
public class ChannelListOnlineStatusHandler extends MultiDataHandler {

    private static final String TAG = "ChannelListOnlineStatusHandler";

    @SuppressWarnings("unchecked")
    public static final DataKey<Map<String, UserOnlineStatus>> KEY_ONLINE_STATUS_CHANGED =
            DataKey.obtain(
                    "KEY_ONLINE_STATUS_CHANGED",
                    (Class<Map<String, UserOnlineStatus>>) (Class<?>) Map.class);

    private OnlineStatusDataSource onlineStatusDataSource;

    private final OnLineStatusListener onLineStatusListener =
            statuses -> notifyDataChange(KEY_ONLINE_STATUS_CHANGED, statuses);

    public ChannelListOnlineStatusHandler() {
        OnLineStatusManager.getInstance().addOnLineStatusListener(onLineStatusListener);
    }

    public void setDataSourceFromConversationList(List<BaseUiChannel> uiConversationList) {
        onlineStatusDataSource = () -> getOnlineStatusUserIds(uiConversationList);
        OnLineStatusManager.getInstance().setOnlineStatusDataSource(onlineStatusDataSource);
    }

    public void refreshDataSource() {
        if (onlineStatusDataSource != null) {
            OnLineStatusManager.getInstance().setOnlineStatusDataSource(onlineStatusDataSource);
        }
    }

    public void fetchOnlineStatus(List<BaseUiChannel> uiConversationList) {
        List<String> uidList = getOnlineStatusUserIds(uiConversationList);
        if (!uidList.isEmpty()) {
            RLog.d(TAG, "fetchUsersOnlineStatus for conversation list");
            OnLineStatusManager.getInstance().fetchUsersOnlineStatus(uidList);
        }
    }

    public void attachCacheOnlineStatus(List<BaseUiChannel> uiConversationList) {
        Map<String, UserOnlineStatus> statuses =
                OnLineStatusManager.getInstance().getNcOnlineStatusCache();
        if (statuses.isEmpty()) {
            return;
        }
        for (BaseUiChannel uiConversation : uiConversationList) {
            if (uiConversation.mCore != null
                    && ChannelType.DIRECT == uiConversation.mCore.getChannelType()
                    && !TextUtils.isEmpty(uiConversation.mCore.getChannelId())) {
                UserOnlineStatus status = statuses.get(uiConversation.mCore.getChannelId());
                if (status != null) {
                    uiConversation.setOnlineStatus(status);
                }
            }
        }
    }

    private List<String> getOnlineStatusUserIds(List<BaseUiChannel> uiConversationList) {
        List<String> uidList = new ArrayList<>();
        if (uiConversationList == null || uiConversationList.isEmpty()) {
            return uidList;
        }
        for (BaseUiChannel uiConversation : uiConversationList) {
            if (uiConversation.mCore != null
                    && ChannelType.DIRECT == uiConversation.mCore.getChannelType()
                    && !TextUtils.isEmpty(uiConversation.mCore.getChannelId())) {
                uidList.add(uiConversation.mCore.getChannelId());
            }
        }
        return uidList;
    }

    @Override
    public void stop() {
        super.stop();
        OnLineStatusManager.getInstance().removeOnLineStatusListener(onLineStatusListener);
        if (onlineStatusDataSource != null) {
            OnLineStatusManager.getInstance().removeOnlineStatusDataSource(onlineStatusDataSource);
        }
    }
}
