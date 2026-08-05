package ai.nexconn.chatui.model;

import ai.nexconn.chat.user.model.FriendApplicationDetail;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public class UiFriendApplicationInfo {
    private FriendApplicationDetail info;
    private @StringRes int showTime;

    public UiFriendApplicationInfo(@NonNull FriendApplicationDetail info, @StringRes int showTime) {
        this.info = info;
        this.showTime = showTime;
    }

    public FriendApplicationDetail getInfo() {
        return info;
    }

    public @StringRes int getShowTime() {
        return showTime;
    }
}
