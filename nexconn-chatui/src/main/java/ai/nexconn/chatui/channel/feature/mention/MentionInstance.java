package ai.nexconn.chatui.channel.feature.mention;

import ai.nexconn.chat.channel.ChannelType;
import android.widget.EditText;
import java.util.List;

public class MentionInstance {
    public ChannelType conversationType;
    public String targetId;
    public EditText inputEditText;
    public List<MentionBlock> mentionBlocks;
}
