package ai.nexconn.chatui.config;

import ai.nexconn.chatui.notification.NotificationConfig;
import android.content.Context;

/**
 * SDK configuration entry point.
 *
 * <p>Applications can modify per-screen configurations through this class.
 *
 * <p><b>Note:</b> Configuration must be applied before calling {@link
 * ai.nexconn.chatui.NCChatUI#initialize}.
 */
public class NCChatUIConfig {
    private static final String TAG = NCChatUIConfig.class.getSimpleName();
    private static ChannelListConfig sChannelListConfig = new ChannelListConfig();
    private static ChannelConfig sChannelConfig = new ChannelConfig();
    private static FeatureConfig sFeatureConfig = new FeatureConfig();
    private static NotificationConfig sNotificationConfig = new NotificationConfig();
    private static GatheredChannelConfig sGatheredChannelConfig = new GatheredChannelConfig();

    public static void syncFromXml(Context context) {
        sChannelConfig.initConfig(context);
        sFeatureConfig.initConfig(context);
        sChannelListConfig.initConfig(context);
        // Initialize themes
        ChatUIThemeManager.initThemes(context);
    }

    /**
     * Returns the channel-list screen configuration.
     *
     * @return ChannelListConfig
     */
    public static ChannelListConfig channelListConfig() {
        return sChannelListConfig;
    }

    /**
     * Returns the channel (conversation) screen configuration.
     *
     * @return ChannelConfig
     */
    public static ChannelConfig channelConfig() {
        return sChannelConfig;
    }

    /**
     * Returns the feature-flag configuration.
     *
     * @return FeatureConfig
     */
    public static FeatureConfig featureConfig() {
        return sFeatureConfig;
    }

    /**
     * Returns the notification configuration.
     *
     * @return NotificationConfig
     */
    public static NotificationConfig notificationConfig() {

        return sNotificationConfig;
    }

    /**
     * Returns the gathered (aggregated) channel configuration.
     *
     * @return GatheredChannelConfig
     */
    public static GatheredChannelConfig gatheredChannelConfig() {
        return sGatheredChannelConfig;
    }
}
