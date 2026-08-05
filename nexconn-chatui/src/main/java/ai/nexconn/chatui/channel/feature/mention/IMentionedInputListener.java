package ai.nexconn.chatui.channel.feature.mention;

import ai.nexconn.chat.channel.ChannelType;

public interface IMentionedInputListener {

    /**
     * When the @ mention feature is enabled (NC_enable_mentioned_message set to true in
     * NC_config.xml), this method sets a listener for @ input in group conversations. If {@link
     * IMentionedInputListener#onMentionedInput(ChannelType, String)} returns true, you handle
     * displaying the @ member selection UI; if false, the SDK's default @ member selection UI is
     * shown.
     *
     * @param conversationType conversation type
     * @param targetId conversation ID
     * @return true if you handle the @ member selection UI yourself; false to show the SDK's
     *     default UI.
     */
    boolean onMentionedInput(ChannelType conversationType, String targetId);
}
