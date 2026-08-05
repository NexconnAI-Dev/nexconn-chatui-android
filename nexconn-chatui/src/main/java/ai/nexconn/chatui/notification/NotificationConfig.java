package ai.nexconn.chatui.notification;

import ai.nexconn.chat.message.Message;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

public class NotificationConfig {
    private NotificationChannel mChannel;
    private PendingIntent mPendingIntent;
    private TitleType mTitleType;
    private ForegroundOtherPageAction mOtherPageAction;
    private Interceptor mInterceptor;
    private String categoryNotification = Notification.CATEGORY_MESSAGE; // Default category

    public String getCategoryNotification() {
        return categoryNotification;
    }

    /**
     * Sets the local notification category (must be set after initialization)
     *
     * @param categoryNotification the notification category
     */
    public void setCategoryNotification(String categoryNotification) {
        if (TextUtils.isEmpty(categoryNotification)) {
            categoryNotification = Notification.CATEGORY_MESSAGE;
        }
        this.categoryNotification = categoryNotification;
    }

    public NotificationConfig() {
        mTitleType = TitleType.TARGET_NAME;
        mOtherPageAction = ForegroundOtherPageAction.Sound;
    }

    public NotificationChannel getNotificationChannel() {
        return mChannel;
    }

    public void setNotificationChannel(NotificationChannel channel) {
        this.mChannel = channel;
    }

    public Interceptor getInterceptor() {
        return mInterceptor;
    }

    /**
     * Local notification interceptor
     *
     * @param interceptor the default abstract implementation; subclass DefaultInterceptor and
     *     override methods to customize interception
     */
    public void setInterceptor(DefaultInterceptor interceptor) {
        this.mInterceptor = interceptor;
    }

    public TitleType getTitleType() {
        return mTitleType;
    }

    public void setTitleType(TitleType type) {
        this.mTitleType = type;
    }

    public ForegroundOtherPageAction getForegroundOtherPageAction() {
        return mOtherPageAction;
    }

    public void setForegroundOtherPageAction(ForegroundOtherPageAction action) {
        this.mOtherPageAction = action;
    }

    /** Notification title type */
    public enum TitleType {
        APP_NAME, // Application name
        TARGET_NAME; // Name of the message target ID
    }

    /** Behavior when receiving a message while in foreground on a non-conversation page */
    public enum ForegroundOtherPageAction {
        Silent, // Silent
        Sound, // Vibrate or ring, determined by system settings
        Notification // Show notification, same behavior as when in background
    }

    public interface Interceptor {
        /**
         * Whether to intercept this local notification, typically used for custom notification
         * display.
         *
         * @param message the message associated with the local notification
         * @return true to intercept (SDK will not show the notification; caller handles it); false
         *     to let SDK show it.
         */
        boolean isNotificationIntercepted(Message message);

        /**
         * Callback when setting the local notification PendingIntent. The app layer can modify the
         * PendingIntent settings to customize notification click behavior. By default, clicking the
         * notification navigates to the corresponding conversation page.
         *
         * @param pendingIntent the SDK default PendingIntent
         * @param intent the intent carried by the PendingIntent. Available extras:
         *     intent.getStringExtra(RouteUtils.CHANNEL_TYPE);
         *     intent.getStringExtra(RouteUtils.TARGET_ID);
         *     intent.getIntExtra(RouteUtils.MESSAGE_ID, -1);
         * @return the PendingIntent to use for the local notification.
         */
        PendingIntent onPendingIntent(PendingIntent pendingIntent, Intent intent);

        /**
         * Whether this is a high-priority message. High-priority messages are exempt from global
         * quiet hours and per-channel do-not-disturb, e.g. @ mention messages.
         *
         * @param message the received message
         * @return whether this is a high-priority message
         */
        boolean isHighPriorityMessage(Message message);

        /**
         * Callback before registering the default notification channel. Use this to intercept and
         * modify the default channel configuration, then return the modified channel.
         *
         * @param defaultChannel the default notification channel
         * @return the modified notification channel.
         */
        @TargetApi(Build.VERSION_CODES.O)
        NotificationChannel onRegisterChannel(NotificationChannel defaultChannel);
    }
}
