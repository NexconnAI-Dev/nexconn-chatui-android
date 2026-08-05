package ai.nexconn.chatui.userinfo.db.model;

import androidx.room.Embedded;

public class GroupUserInfo {
    @Embedded public GroupMember groupMember;

    public User user;
}
