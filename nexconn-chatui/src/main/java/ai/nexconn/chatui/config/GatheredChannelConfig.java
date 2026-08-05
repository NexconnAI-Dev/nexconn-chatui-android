package ai.nexconn.chatui.config;

import ai.nexconn.chat.channel.ChannelType;
import android.net.Uri;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for gathered (aggregated) channel display.
 *
 * <p>When a channel type is configured as gathered, all its conversations are collapsed into a
 * single row in the channel list. Use this class to set the display title and portrait for each
 * aggregated channel type.
 */
public class GatheredChannelConfig {
    private Map<ChannelType, Integer> mTitleMap;
    private Map<ChannelType, Uri> mPortraitMap;

    public GatheredChannelConfig() {
        mTitleMap = new HashMap<>();
        mPortraitMap = new HashMap<>();
    }

    /**
     * Sets the title string resource for an aggregated channel type.
     *
     * @param conversationType channel type
     * @param title string resource ID for the aggregated channel title
     */
    public void setConversationTitle(ChannelType conversationType, int title) {
        mTitleMap.put(conversationType, title);
    }

    /**
     * Returns the title string resource for an aggregated channel type.
     *
     * @param conversationType channel type
     * @return string resource ID, or {@code null} if not configured
     */
    public Integer getConversationTitle(ChannelType conversationType) {
        return mTitleMap.get(conversationType);
    }

    /**
     * Sets the portrait URI for an aggregated channel type.
     *
     * @param conversationType channel type
     * @param resUri portrait resource URI
     */
    public void setGatherConversationPortrait(ChannelType conversationType, Uri resUri) {
        mPortraitMap.put(conversationType, resUri);
    }

    /**
     * Returns the portrait URI for an aggregated channel type.
     *
     * @param conversationType channel type
     * @return portrait URI, or {@code null} if not configured
     */
    public Uri getGatherConversationPortrait(ChannelType conversationType) {
        return mPortraitMap.get(conversationType);
    }
}
