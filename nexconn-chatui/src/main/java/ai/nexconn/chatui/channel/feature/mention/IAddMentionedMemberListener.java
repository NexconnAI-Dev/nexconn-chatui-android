package ai.nexconn.chatui.channel.feature.mention;

import ai.nexconn.chat.user.model.UserInfo;

public interface IAddMentionedMemberListener {
    boolean onAddMentionedMember(UserInfo userInfo, int from);
}
