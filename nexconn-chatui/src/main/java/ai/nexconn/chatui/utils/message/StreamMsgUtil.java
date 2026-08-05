package ai.nexconn.chatui.utils.message;

import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.StreamMessage;
import ai.nexconn.chatui.utils.log.RLog;
import android.text.TextUtils;
import android.util.Pair;
import androidx.annotation.NonNull;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Stream message utility class.
 *
 * @since 5.16.0
 */
public class StreamMsgUtil {

    private static final String TAG = "StreamMsgUtil";
    private static final String KEY_STREAM_MSG_SUMMARY = "RC_Ext_StreamMsgSummary";

    private StreamMsgUtil() {}

    @NonNull
    public static String getStreamMessageShowContent(
            StreamMessage streamMessage, Pair<String, Boolean> streamMessageSummary) {
        String content = streamMessage.getContent();
        if (content != null && streamMessageSummary.first != null) {
            content =
                    content.length() > streamMessageSummary.first.length()
                            ? content
                            : streamMessageSummary.first;
        }
        return limitContentLength(content);
    }

    public static String limitContentLength(String content) {
        // Truncate content to 10000 characters when it exceeds the limit
        if (content != null && content.length() > 10000) {
            content = content.substring(0, 10000) + "...";
        }
        return content == null ? "" : content;
    }

    @NonNull
    public static Pair<String, Boolean> getStreamMessageSummary(Message message) {
        if (message == null || message.getMetadata() == null) {
            return Pair.create("", false);
        }
        return getStreamMessageSummary(message.getMetadata());
    }

    @NonNull
    public static Pair<String, Boolean> getStreamMessageSummary(Map<String, String> expansion) {
        if (expansion == null || TextUtils.isEmpty(expansion.get(KEY_STREAM_MSG_SUMMARY))) {
            return Pair.create("", false);
        }
        try {
            String jsonString = expansion.get(KEY_STREAM_MSG_SUMMARY);
            JSONObject rootObject = new JSONObject(jsonString);
            String summary = rootObject.getString("summary");
            boolean complete = rootObject.getBoolean("complete");
            return Pair.create(summary, complete);
        } catch (JSONException e) {
            e.printStackTrace();
            RLog.e(TAG, "getStreamMessageSummary: " + e.getMessage());
        }
        return Pair.create("", false);
    }
}
