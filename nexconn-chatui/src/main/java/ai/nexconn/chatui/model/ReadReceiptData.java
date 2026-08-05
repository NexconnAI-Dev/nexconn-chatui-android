package ai.nexconn.chatui.model;

import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.message.model.ReadReceiptUser;

public class ReadReceiptData {
    private ReadReceiptUser user;
    private GroupMemberInfo info;

    public ReadReceiptData(ReadReceiptUser user, GroupMemberInfo info) {
        this.user = user;
        this.info = info;
    }

    public GroupMemberInfo getInfo() {
        return info;
    }

    public void setInfo(GroupMemberInfo info) {
        this.info = info;
    }

    public ReadReceiptUser getUser() {
        return user;
    }

    public void setUser(ReadReceiptUser user) {
        this.user = user;
    }
}
