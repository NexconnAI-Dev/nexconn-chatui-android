package ai.nexconn.chatui.channel.event.action;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.Message;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeleteEvent {
    private ChannelType mConversationType;
    private String mTargetId;
    private int[] mMessageIds;
    private List<Message> mMessages;

    public DeleteEvent(ChannelType conversationType, String targetId, int[] messageIds) {
        this(conversationType, targetId, messageIds, null);
    }

    public DeleteEvent(
            ChannelType conversationType,
            String targetId,
            int[] messageIds,
            List<Message> messages) {
        this.mConversationType = conversationType;
        this.mTargetId = targetId;
        this.mMessageIds = messageIds;
        this.mMessages = messages != null ? new ArrayList<>(messages) : Collections.emptyList();
    }

    public ChannelType getConversationType() {
        return mConversationType;
    }

    public String getTargetId() {
        return mTargetId;
    }

    public int[] getMessageIds() {
        return mMessageIds;
    }

    public List<Message> getMessages() {
        return mMessages;
    }
}
