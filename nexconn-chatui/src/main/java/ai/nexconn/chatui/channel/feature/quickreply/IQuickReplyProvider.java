package ai.nexconn.chatui.channel.feature.quickreply;

import ai.nexconn.chat.channel.ChannelType;
import java.util.List;

public interface IQuickReplyProvider {
    public List<String> getPhraseList(ChannelType type);
}
