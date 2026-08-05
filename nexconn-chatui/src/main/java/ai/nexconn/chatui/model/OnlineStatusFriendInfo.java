package ai.nexconn.chatui.model;

import ai.nexconn.chat.user.model.FriendDetail;

/**
 * Combines friend detail with online status information.
 *
 * @since 5.32.0
 */
public class OnlineStatusFriendInfo {
    private FriendDetail friendDetail;
    private boolean isOnline;

    public OnlineStatusFriendInfo(FriendDetail friendDetail, boolean isOnline) {
        this.friendDetail = friendDetail;
        this.isOnline = isOnline;
    }

    public FriendDetail getFriendDetail() {
        return friendDetail;
    }

    public void setFriendDetail(FriendDetail friendDetail) {
        this.friendDetail = friendDetail;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }
}
