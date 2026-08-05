package ai.nexconn.chatui.notification;

import android.text.TextUtils;
import android.util.LruCache;

/** Helper class for managing notification IDs and channel configuration. */
public class ChatUINotificationHelper {
    private static final String TAG = "ChatUINotificationHelper";
    private static final LruCache<String, Integer> NOTIFICATION_ID_CACHE = new LruCache<>(100);
    private static int BASE_NOTIFICATION_ID = 1000;
    private static final String DEFAULT_CHANNEL_ID = "nc_notification_id";
    private static final String DEFAULT_CHANNEL_NAME = "nc_notification_channel_name";
    private static final String DEFAULT_VOIP_CHANNEL_ID = "nc_notification_voip_id";
    private static final String DEFAULT_VOIP_CHANNEL_NAME = "nc_notification_voip_channel_name";

    private static int PUSH_SERVICE_NOTIFICATION_ID =
            2000; // Notification ID for non-persistent messages (PUSH_SERVICE/SYSTEM type).
    private static final int VOIP_NOTIFICATION_ID = 3000; // VoIP notification ID.

    public static synchronized int getNotificationId(String messageUId) {
        if (TextUtils.isEmpty(messageUId)) {
            return ++BASE_NOTIFICATION_ID;
        }
        Integer id = NOTIFICATION_ID_CACHE.get(messageUId);
        if (id != null) {
            return id;
        }
        id = ++BASE_NOTIFICATION_ID;
        NOTIFICATION_ID_CACHE.put(messageUId, id);
        return id;
    }

    public static synchronized int getNotificationId() {
        return BASE_NOTIFICATION_ID;
    }

    public static int getPushServiceNotificationId() {
        return ++PUSH_SERVICE_NOTIFICATION_ID;
    }

    public static int getVoipNotificationId() {
        return VOIP_NOTIFICATION_ID;
    }

    public static String getDefaultChannelId() {
        return DEFAULT_CHANNEL_ID;
    }

    public static String getDefaultChannelName() {
        return DEFAULT_CHANNEL_NAME;
    }

    public static String getDefaultVoipChannelId() {
        return DEFAULT_VOIP_CHANNEL_ID;
    }

    public static String getDefaultVoipChannelName() {
        return DEFAULT_VOIP_CHANNEL_NAME;
    }

    public static synchronized void resetNotificationId() {
        BASE_NOTIFICATION_ID = 1000;
    }

    public static synchronized void resetPushServiceNotificationId() {
        PUSH_SERVICE_NOTIFICATION_ID = 2000;
    }
}
