package ai.nexconn.chatui.model;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chatui.utils.log.RLog;
import android.text.TextUtils;

/** Created by zhjchen on 4/29/15. */
public final class ChannelKey {

    private static final String SEPARATOR = "#@6NC_CLOUD9@#";

    private String key;
    private String targetId;
    private ChannelType type;

    private ChannelKey() {
        // default implementation ignored
    }

    public static ChannelKey obtain(String targetId, ChannelType type) {

        if (!TextUtils.isEmpty(targetId) && type != null) {
            ChannelKey conversationKey = new ChannelKey();
            conversationKey.setTargetId(targetId);
            conversationKey.setType(type);
            conversationKey.setKey(targetId + SEPARATOR + type.getValue());
            return conversationKey;
        }

        return null;
    }

    public static ChannelKey obtain(String key) {

        if (!TextUtils.isEmpty(key) && key.contains(SEPARATOR)) {
            ChannelKey conversationKey = new ChannelKey();

            if (key.contains(SEPARATOR)) {
                String[] array = key.split(SEPARATOR);
                conversationKey.setTargetId(array[0]);

                try {
                    conversationKey.setType(ChannelType.fromValue(Integer.parseInt(array[1])));
                } catch (NumberFormatException e) {
                    RLog.e("ChannelKey ", "NumberFormatException");
                    return null;
                }

                return conversationKey;
            }
        }

        return null;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public ChannelType getType() {
        return type;
    }

    public void setType(ChannelType type) {
        this.type = type;
    }
}
