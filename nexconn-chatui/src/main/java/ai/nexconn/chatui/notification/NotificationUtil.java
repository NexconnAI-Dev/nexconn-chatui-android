package ai.nexconn.chatui.notification;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.utils.log.RLog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.text.TextUtils;
import androidx.annotation.RequiresApi;

/** Created by jiangecho on 2016/11/29. */
public class NotificationUtil {
    private final String TAG = NotificationUtil.class.getSimpleName();
    private final int SOUND_INTERVAL = 3000;
    private long mLastSoundTime;

    private NotificationUtil() {
        // default implementation ignored
    }

    public static NotificationUtil getInstance() {
        return SingletonHolder.sInstance;
    }

    /**
     * @param context the context
     * @param title the notification title
     * @param content the notification content
     * @param pendingIntent PendingIntent
     * @param notificationId the notification ID
     * @param defaults notification defaults controlling sound/vibrate/lights, maps to
     *     Builder.setDefaults(int)
     */
    public void showNotification(
            Context context,
            String title,
            String content,
            PendingIntent pendingIntent,
            int notificationId,
            int defaults) {
        Notification notification;
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel =
                        NCChatUIConfig.notificationConfig().getNotificationChannel();
                if (channel == null) {
                    channel = getDefaultChannel(context);
                }
                if (NCChatUIConfig.notificationConfig().getInterceptor() != null) {
                    channel =
                            NCChatUIConfig.notificationConfig()
                                    .getInterceptor()
                                    .onRegisterChannel(getDefaultChannel(context));
                }
                nm.createNotificationChannel(channel);
                notification =
                        createNotification(
                                context, title, content, pendingIntent, defaults, channel.getId());
            } else {
                notification =
                        createNotification(context, title, content, pendingIntent, defaults, null);
            }

            if (notification != null) {
                RLog.d(TAG, "notify for local notification");
                try {
                    nm.notify(notificationId, notification);
                } catch (Exception e) {
                    RLog.d(TAG, "notify for local notification Exception e:" + e);
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public NotificationChannel getDefaultChannel(Context context) {
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        String channelName =
                context.getResources().getString(R.string.nc_notification_channel_name);
        NotificationChannel notificationChannel =
                new NotificationChannel(
                        ChatUINotificationHelper.getDefaultChannelId(), channelName, importance);
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.GREEN);
        notificationChannel.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null);
        notificationChannel.enableLights(true);
        notificationChannel.enableVibration(false);
        return notificationChannel;
    }

    public void showNotification(
            Context context,
            String title,
            String content,
            PendingIntent intent,
            int notificationId) {
        showNotification(context, title, content, intent, notificationId, Notification.DEFAULT_ALL);
    }

    public void clearNotification(Context context, int notificationId) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(notificationId);
    }

    private Notification createNotification(
            Context context,
            String title,
            String content,
            PendingIntent pendingIntent,
            int defaults,
            String channelId) {
        String tickerText = context.getString(R.string.nc_notification_ticker_text);
        boolean isLollipop = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
        String categoryNotification = NCChatUIConfig.notificationConfig().getCategoryNotification();
        int smallIcon =
                context.getResources()
                        .getIdentifier(
                                "notification_small_icon", "drawable", context.getPackageName());

        if (smallIcon <= 0 || !isLollipop) {
            smallIcon = context.getApplicationInfo().icon;
        }

        Drawable loadIcon = context.getApplicationInfo().loadIcon((context.getPackageManager()));
        Bitmap appIcon = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && loadIcon instanceof AdaptiveIconDrawable) {
                appIcon =
                        Bitmap.createBitmap(
                                loadIcon.getIntrinsicWidth(),
                                loadIcon.getIntrinsicHeight(),
                                Bitmap.Config.ARGB_8888);
                final Canvas canvas = new Canvas(appIcon);
                loadIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                loadIcon.draw(canvas);
            } else {
                appIcon = ((BitmapDrawable) loadIcon).getBitmap();
            }
        } catch (Exception e) {
            RLog.e(TAG, "createNotification", e);
        }

        Notification.Builder builder = new Notification.Builder(context);
        // Force non-grouped notifications on Android 7.0+ below 8.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setGroupSummary(false).setGroup("group");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(categoryNotification);
        }
        builder.setLargeIcon(appIcon);
        builder.setSmallIcon(smallIcon);
        builder.setTicker(tickerText);
        builder.setContentTitle(title);
        builder.setContentText(content);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        builder.setOngoing(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(
                    TextUtils.isEmpty(channelId)
                            ? ChatUINotificationHelper.getDefaultChannelId()
                            : channelId);
        } else {
            if (System.currentTimeMillis() - mLastSoundTime >= SOUND_INTERVAL) {
                builder.setDefaults(defaults);
            } else {
                builder.setDefaults(Notification.DEFAULT_LIGHTS);
            }
        }
        mLastSoundTime = System.currentTimeMillis();
        return builder.build();
    }

    public int getRingerMode(Context context) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audio.getRingerMode();
    }

    private static class SingletonHolder {
        static NotificationUtil sInstance = new NotificationUtil();
    }
}
