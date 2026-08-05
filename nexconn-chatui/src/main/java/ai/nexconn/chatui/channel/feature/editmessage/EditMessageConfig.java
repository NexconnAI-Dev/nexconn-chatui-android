package ai.nexconn.chatui.channel.feature.editmessage;

import ai.nexconn.chat.message.model.ReferenceMessageStatus;
import ai.nexconn.chatui.channel.feature.mention.MentionBlock;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import java.util.List;

public class EditMessageConfig {
    // Message UID
    public String uid;
    // Message sentTime
    public long sentTime;
    // Edited message content
    public String content;
    // Referenced content of the edited message
    public String referContent;
    // Referenced message UID of the edited message
    public String referUid;
    // Referenced message status of the edited message
    public ReferenceMessageStatus referStatus = ReferenceMessageStatus.DEFAULT;
    // @ mention content
    public List<MentionBlock> mentionBlocks;

    public EditMessageConfig() {}

    public static boolean isInvalid(EditMessageConfig config) {
        return config == null || TextUtils.isEmpty(config.uid) || TextUtils.isEmpty(config.content);
    }

    @NonNull
    @Override
    public String toString() {
        return "EditMessageConfig{"
                + "uid='"
                + uid
                + '\''
                + ", sentTime="
                + sentTime
                + ", content='"
                + content
                + '\''
                + ", referContent='"
                + referContent
                + '\''
                + ", referUid='"
                + referUid
                + '\''
                + ", referStatus="
                + referStatus
                + ", mentionBlocks="
                + mentionBlocks
                + '}';
    }
}
