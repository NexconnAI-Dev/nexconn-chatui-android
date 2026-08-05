package ai.nexconn.chatui.model;

import ai.nexconn.chat.channel.ChannelType;
import java.util.List;

public class TypingInfo {
    public ChannelType conversationType;
    public String targetId;
    public List<TypingUserInfo> typingList;

    /** User info for a user who is currently typing. */
    public static class TypingUserInfo {
        public enum Type {
            voice,
            text
        }

        public Type type;
        public long sendTime;
        public String userId;
    }
}
