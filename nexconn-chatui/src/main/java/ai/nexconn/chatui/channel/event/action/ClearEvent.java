package ai.nexconn.chatui.channel.event.action;

import ai.nexconn.chat.channel.ChannelType;

public class ClearEvent {

    private ChannelType mConversationType;
    private String mTargetId;

    public ClearEvent(ChannelType conversationType, String targetId) {
        mConversationType = conversationType;
        mTargetId = targetId;
    }

    public ChannelType getConversationType() {
        return mConversationType;
    }

    public String getTargetId() {
        return mTargetId;
    }
}
