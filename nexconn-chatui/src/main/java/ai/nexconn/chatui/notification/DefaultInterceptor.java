package ai.nexconn.chatui.notification;

import ai.nexconn.chat.message.Message;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Intent;

public abstract class DefaultInterceptor implements NotificationConfig.Interceptor {
    /**
     * Whether to intercept this local notification, typically used for custom notification display.
     *
     * @param message the message associated with the local notification
     * @return true to intercept (SDK will not show the notification; caller handles it); false to
     *     let SDK show it.
     */
    @Override
    public boolean isNotificationIntercepted(Message message) {
        return false;
    }

    /**
     * Callback when setting the local notification PendingIntent. The app layer can modify the
     * PendingIntent settings to customize notification click behavior. By default, clicking the
     * notification navigates to the corresponding conversation page.
     *
     * @param pendingIntent the SDK default PendingIntent
     * @param intent the intent carried by the PendingIntent. Available extras:
     *     intent.getStringExtra(RouteUtils.CHANNEL_TYPE);
     *     intent.getStringExtra(RouteUtils.TARGET_ID);
     *     intent.getParcelableExtra(RouteUtils.MESSAGE);
     * @return the PendingIntent to use for the local notification.
     */
    @Override
    public PendingIntent onPendingIntent(PendingIntent pendingIntent, Intent intent) {
        return pendingIntent;
    }

    /**
     * Whether this is a high-priority message. High-priority messages are exempt from global quiet
     * hours and per-channel do-not-disturb, e.g. @ mention messages.
     *
     * @param message the received message
     * @return whether this is a high-priority message
     */
    @Override
    public boolean isHighPriorityMessage(Message message) {
        return false;
    }

    /**
     * Callback before registering the default notification channel. Use this to intercept and
     * modify the default channel configuration, then return the modified channel.
     *
     * @param defaultChannel the default notification channel
     * @return the modified notification channel.
     */
    @Override
    public NotificationChannel onRegisterChannel(NotificationChannel defaultChannel) {
        return defaultChannel;
    }
}
