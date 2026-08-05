package ai.nexconn.chatui.channel.feature.mention;

import android.text.TextUtils;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;

/**
 * When saving a draft with @ mention info, serializes MentionBlocks for persistence. They can later
 * be deserialized and added back to NCMentionManager.
 */
public class DraftHelper {

    private final EditText editText;

    public DraftHelper() {
        this.editText = null;
    }

    public DraftHelper(@NonNull EditText editText) {
        this.editText = editText;
    }

    public String getMentionBlocks() {
        return getMentionBlocks(editText);
    }

    public String getMentionBlocks(EditText editText) {
        MentionInstance mentionInstance =
                NCMentionManager.getInstance().obtainMentionInstance(editText);
        if (mentionInstance == null
                || mentionInstance.mentionBlocks == null
                || mentionInstance.mentionBlocks.isEmpty()) {
            return "";
        }
        JSONArray jsonArray = new JSONArray();
        for (MentionBlock mentionBlock : mentionInstance.mentionBlocks) {
            jsonArray.put(mentionBlock.toJson());
        }
        return jsonArray.toString();
    }

    public void addMentionBlocks(String mentionInfo) {
        addMentionBlocks(editText, mentionInfo);
    }

    public void addMentionBlocks(EditText editText, String mentionInfo) {
        MentionInstance mentionInstance =
                NCMentionManager.getInstance().obtainMentionInstance(editText);
        if (mentionInstance == null || mentionInstance.mentionBlocks == null) {
            return;
        }

        List<MentionBlock> mentionBlocks = parseMentionBlocks(mentionInfo);
        addMentionBlocks(editText, mentionBlocks);
    }

    public void addMentionBlocks(EditText editText, List<MentionBlock> mentionBlocks) {
        MentionInstance mentionInstance =
                NCMentionManager.getInstance().obtainMentionInstance(editText);
        if (mentionInstance == null || mentionInstance.mentionBlocks == null) {
            return;
        }

        if (mentionBlocks == null || mentionBlocks.isEmpty()) {
            return;
        }
        mentionInstance.mentionBlocks.clear();
        for (MentionBlock mentionBlock : mentionBlocks) {
            if (mentionBlock != null && !TextUtils.isEmpty(mentionBlock.userId)) {
                mentionInstance.mentionBlocks.add(mentionBlock);
            }
        }
    }

    @Nullable
    private List<MentionBlock> parseMentionBlocks(String mentionInfo) {
        if (TextUtils.isEmpty(mentionInfo)) {
            return Collections.emptyList();
        }

        try {
            JSONArray jsonArray = new JSONArray(mentionInfo);
            List<MentionBlock> list = new ArrayList<>(jsonArray.length());
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(new MentionBlock(jsonArray.getString(i)));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
