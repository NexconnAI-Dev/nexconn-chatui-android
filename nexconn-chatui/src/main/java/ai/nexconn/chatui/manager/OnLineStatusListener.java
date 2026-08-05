package ai.nexconn.chatui.manager;

import ai.nexconn.chat.user.model.UserOnlineStatus;
import java.util.Map;

/**
 * Online status change listener interface
 *
 * @since 5.32.0
 */
public interface OnLineStatusListener {
    /**
     * Called when online statuses change
     *
     * @param list the updated status map
     */
    void onOnlineStatusUpdate(Map<String, UserOnlineStatus> list);
}
