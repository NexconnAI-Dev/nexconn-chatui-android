package ai.nexconn.chatui.handler;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.model.AppSettings;
import ai.nexconn.chat.model.ConnectionStatus;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.utils.system.ExecutorHelper;
import java.util.Set;

/**
 * Application settings handler — process-wide singleton.
 *
 * <p>Manages fetching, caching, and refreshing {@link ai.nexconn.chat.model.AppSettings},
 * including:
 *
 * <ul>
 *   <li>Listening for connection-status changes and auto-refreshing settings on reconnect
 *   <li>Caching the latest {@code AppSettings} in memory
 *   <li>Providing accessors for individual feature flags
 * </ul>
 *
 * @since 5.28.0
 */
public class AppSettingsHandler {

    private static final String TAG = "AppSettingsHandler";
    private static final String CONNECTION_HANDLER_ID = "AppSettingsHandler";

    /** Singleton holder: thread-safe lazy initialization via JVM class-loading. */
    private static class Holder {
        private static final AppSettingsHandler INSTANCE = new AppSettingsHandler();
    }

    private AppSettings appSettings = new AppSettings();

    private final ai.nexconn.chat.handler.ConnectionStatusHandler connectionStatusHandler =
            event -> {
                if (ConnectionStatus.CONNECTED == event.getStatus()) {
                    getInnerAppSettings();
                }
            };

    /** Whether {@code AppSettings} has been successfully fetched at least once. */
    private volatile boolean hasInit = false;

    // Private constructor — use getInstance()
    private AppSettingsHandler() {
        NCEngine.addConnectionStatusHandler(CONNECTION_HANDLER_ID, connectionStatusHandler);
        getInnerAppSettings();
    }

    /**
     * Returns the process-wide singleton instance.
     *
     * @return the {@link AppSettingsHandler} singleton
     */
    public static AppSettingsHandler getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Returns the cached application settings.
     *
     * @return the current {@link ai.nexconn.chat.model.AppSettings}
     */
    public AppSettings getAppSettings() {
        return appSettings;
    }

    /**
     * Returns whether {@code AppSettings} has been successfully fetched at least once.
     *
     * @return {@code true} after the first successful fetch; {@code false} otherwise
     */
    public boolean hasInit() {
        return hasInit;
    }

    /**
     * Returns whether read receipts (V5) are enabled for the given channel type.
     *
     * <p>Read receipts are only supported for {@link ai.nexconn.chat.channel.ChannelType#DIRECT}
     * and {@link ai.nexconn.chat.channel.ChannelType#GROUP}, and only when the Kit-level
     * read-receipt toggle is on and the channel type is in the supported set.
     *
     * @param type channel type to check
     * @return {@code true} if V5 read receipts are enabled; {@code false} otherwise
     */
    public boolean isReadReceiptV5Enabled(ChannelType type) {
        // Not a direct or group channel — not supported
        if (ChannelType.GROUP != type && ChannelType.DIRECT != type) {
            return false;
        }
        // Kit-level read-receipt toggle is off
        boolean enableReadReceipt = NCChatUIConfig.channelConfig().isEnableReadReceipt();
        if (!enableReadReceipt) {
            return false;
        }
        // Channel type not in the supported set
        Set<ChannelType> types =
                NCChatUIConfig.channelConfig().getSupportReadReceiptConversationType();
        if (types.isEmpty() || !types.contains(type)) {
            return false;
        }
        // nexconn SDK supports V5 read receipts by default
        return true;
    }

    /**
     * Returns whether the online-status feature is enabled (affects UI visibility).
     *
     * <p>Returns {@code true} when the Kit feature flag is on AND at least one of
     * friend-online-status or general online-status subscription is enabled.
     *
     * @return {@code true} if online status should be shown in the UI
     */
    public boolean isOnlineStatusEnable() {
        if (!NCChatUIConfig.featureConfig().isUserOnlineStatusEnable()) {
            return false;
        }
        return appSettings.isFriendOnlineStatusSubscribeEnabled()
                || appSettings.isOnlineStatusSubscribeEnabled();
    }

    /** Returns whether friend online-status subscription is enabled. */
    public boolean isFriendOnlineStatusSubscribeEnable() {
        return NCChatUIConfig.featureConfig().isUserOnlineStatusEnable()
                && appSettings.isFriendOnlineStatusSubscribeEnabled();
    }

    /** Returns whether general online-status subscription is enabled. */
    public boolean isOnlineStatusSubscribeEnable() {
        return NCChatUIConfig.featureConfig().isUserOnlineStatusEnable()
                && appSettings.isOnlineStatusSubscribeEnabled();
    }

    /**
     * Returns whether user-profile hosting is enabled.
     *
     * @deprecated No corresponding field in nexconn AppSettings; always returns {@code false}.
     * @return {@code false}
     */
    public boolean isUserProfileEnabled() {
        return false;
    }

    /** Asynchronously fetches and updates the cached app settings. */
    private void getInnerAppSettings() {
        ExecutorHelper.getInstance()
                .compressExecutor()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                appSettings = NCEngine.getAppSettings();
                                hasInit = true;
                            }
                        });
    }

    /** Stops the handler and releases the connection-status listener. */
    private void stop() {
        NCEngine.removeConnectionStatusHandler(CONNECTION_HANDLER_ID);
    }
}
