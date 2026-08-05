package ai.nexconn.chatui.notification;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.ChannelNoDisturbLevel;
import ai.nexconn.chat.channel.model.ChannelNoDisturbLevelSyncEvent;
import ai.nexconn.chat.channel.model.ChannelStatusSyncCompletedEvent;
import ai.nexconn.chat.error.NCError;
import ai.nexconn.chat.handler.ChannelHandler;
import ai.nexconn.chat.handler.ErrorHandler;
import ai.nexconn.chat.handler.MessageHandler;
import ai.nexconn.chat.handler.OperationHandler;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.UnknownMessage;
import ai.nexconn.chat.message.model.MentionedInfo;
import ai.nexconn.chat.message.model.MentionedType;
import ai.nexconn.chat.message.model.MessageDeletedEvent;
import ai.nexconn.chat.message.model.MessageReceivedEvent;
import ai.nexconn.chat.model.NoDisturbTimeLevel;
import ai.nexconn.chat.params.NoDisturbTimeInfo;
import ai.nexconn.chat.params.NoDisturbTimeParams;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.ChannelActivity;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.ChannelKey;
import ai.nexconn.chatui.userinfo.NCUserInfoManager;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.route.RouteUtils;
import ai.nexconn.chatui.utils.system.ExecutorHelper;
import ai.nexconn.chatui.widget.cache.NCCache;
import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Vibrator;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

public class ChatUINotificationManager implements NCUserInfoManager.UserDataObserver {
    // When the app is in foreground but not on the conversation page, ring/vibrate at 3s intervals
    // on message receipt.
    private static final int SOUND_INTERVAL = 3000;
    private boolean mIsInForeground;
    private final String TAG = this.getClass().getSimpleName();
    private final int MAX_NOTIFICATION_STATUS_CACHE = 128;
    private Application mApplication;
    private NCCache<String, Integer> mNotificationCache;
    private boolean isQuietSettingSynced = false;
    private String mQuietStartTime; // Notification do-not-disturb start time
    private int mQuietSpanTime; // Notification do-not-disturb span in minutes
    private int requestCode = 1000;
    private long mLastSoundTime = 0;
    private Activity mTopForegroundActivity;
    private volatile ChannelIdentifier mResumedChannelIdentifier;
    private volatile boolean mIsChannelListPageResumed;
    private ConcurrentHashMap<String, Message> messageMap = new ConcurrentHashMap<>();
    private MediaPlayer mediaPlayer;

    private static final String MSG_HANDLER_ID = "ChatUINotificationManager";
    private static final String CHANNEL_HANDLER_ID = "ChatUINotificationManager_Channel";

    private final ChannelHandler mChannelStatusHandler =
            new ChannelHandler() {
                @Override
                public void onChannelNoDisturbLevelSync(
                        @NonNull ChannelNoDisturbLevelSyncEvent event) {
                    MessageNotificationHelper.updateLevelMap(
                            event.getChannelIdentifier(), event.getLevel().getValue());
                }

                @Override
                public void onChannelStatusSyncCompleted(
                        @NonNull ChannelStatusSyncCompletedEvent event) {
                    if (event.getError() == null) {
                        getNotificationQuietHours(null);
                    }
                }
            };

    private final MessageHandler onReceiveMessageHandler =
            new MessageHandler() {
                @Override
                public void onMessageReceived(@NonNull MessageReceivedEvent event) {
                    Message message = event.getMessage();
                    int left = event.getLeft() != null ? event.getLeft() : 0;
                    boolean hasPackage = Boolean.TRUE.equals(event.getHasPackage());
                    boolean offline = Boolean.TRUE.equals(event.getOffline());
                    boolean shouldNotify = shouldNotify(message, left, hasPackage, offline);
                    RLog.d(
                            TAG,
                            "onReceived. uid:"
                                    + message.getMessageId()
                                    + "; offline:"
                                    + offline
                                    + "; mIsInForeground:"
                                    + mIsInForeground
                                    + "; shouldNotify:"
                                    + shouldNotify);
                    if (shouldNotify) {
                        if (isHighPriorityMessage(message)) {
                            preToNotify(message);
                            return;
                        }
                        MessageNotificationHelper.getNotificationQuietHoursLevel(message);
                    }
                }
            };

    // TODO: Wire conversation status changes via nexconn ChannelHandler when API is available

    /** Recalled message notification handler: replaces the legacy OnRecallMessageListener. */
    private final MessageHandler onMessageDeletedHandler =
            new MessageHandler() {
                @Override
                public void onMessageDeleted(@NonNull MessageDeletedEvent event) {
                    List<Message> deletedMessages = event.getMessages();
                    if (deletedMessages == null) {
                        return;
                    }
                    for (Message msg : deletedMessages) {
                        if (msg != null && !isRecallFiltered(msg)) {
                            preToNotify(msg);
                        }
                    }
                }
            };

    private ChatUINotificationManager() {
        // default implementation ignored
    }

    public static ChatUINotificationManager getInstance() {
        return SingletonHolder.sInstance;
    }

    public void init(Application application) {
        messageMap.clear();
        mApplication = application;
        mNotificationCache = new NCCache<>(MAX_NOTIFICATION_STATUS_CACHE);
        MessageNotificationHelper.setNotifyListener(
                new MessageNotificationHelper.NotifyListener() {
                    @Override
                    public void onPreToNotify(Message message) {
                        if (message == null) {
                            return;
                        }
                        preToNotify(message);
                    }
                });
        NCEngine.addMessageHandler(MSG_HANDLER_ID, onReceiveMessageHandler);
        NCEngine.addMessageHandler(MSG_HANDLER_ID + "_deleted", onMessageDeletedHandler);
        NCEngine.addChannelHandler(CHANNEL_HANDLER_ID, mChannelStatusHandler);
        NCUserInfoManager.getInstance().addUserDataObserver(this);
        registerActivityLifecycleCallback();
    }

    void preToNotify(Message message) {
        if (!mIsInForeground) {
            prepareToSendNotification(message);
        } else if (shouldAlertInForeground(message)) {
            NotificationConfig.ForegroundOtherPageAction action =
                    NCChatUIConfig.notificationConfig().getForegroundOtherPageAction();
            if (action.equals(NotificationConfig.ForegroundOtherPageAction.Notification)) {
                prepareToSendNotification(message);
            } else if (action.equals(NotificationConfig.ForegroundOtherPageAction.Sound)
                    && System.currentTimeMillis() - mLastSoundTime > SOUND_INTERVAL) {
                AudioManager audio =
                        (AudioManager) mApplication.getSystemService(Context.AUDIO_SERVICE);
                if (audio != null && audio.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                    mLastSoundTime = System.currentTimeMillis();
                    if (ifVrate(message)) {
                        vibrate();
                    }
                    if (ifSound(audio, message)) {
                        sound();
                    }
                }
            }
        }
    }

    /** Whether to vibrate for this received message */
    private boolean ifVrate(Message message) {
        // Vibration disabled in settings
        if (!NCChatUIConfig.featureConfig().isVibrateInForeground()) {
            return false;
        }
        // Null message, don't vibrate
        if (message == null) {
            return false;
        }
        // Don't vibrate if the sender is the current user (multi-device scenario)
        if (TextUtils.equals(message.getSenderUserId(), NCEngine.getCurrentUserId())) {
            return false;
        }
        return true;
    }

    /** Whether to play a sound for this received message */
    private boolean ifSound(AudioManager audio, Message message) {
        // Vibrate mode, don't play sound
        if (audio.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
            return false;
        }
        // Sound disabled in settings
        if (!NCChatUIConfig.featureConfig().isSoundInForeground()) {
            return false;
        }
        // Null message, don't play sound
        if (message == null) {
            return false;
        }
        // Don't play sound if the sender is the current user (multi-device scenario)
        if (TextUtils.equals(message.getSenderUserId(), NCEngine.getCurrentUserId())) {
            return false;
        }
        return true;
    }

    private UserInfo getUserInfo(String userId, MessageContent messageContent) {
        boolean isInfoManagement =
                NCUserInfoManager.getInstance().getDataSourceType()
                        == NCUserInfoManager.DataSourceType.INFO_MANAGEMENT;
        if (isInfoManagement
                && messageContent != null
                && messageContent.getSenderUserInfo() != null
                && messageContent.getSenderUserInfo().getUserId() != null
                && messageContent.getSenderUserInfo().getUserId().equals(userId)) {
            return messageContent.getSenderUserInfo();
        }
        return NCUserInfoManager.getInstance().getUserInfo(userId);
    }

    private void prepareToSendNotification(Message message) {
        // If on main thread, dispatch to background for notification sending
        if (Looper.myLooper() == Looper.getMainLooper()) {
            ExecutorHelper.getInstance()
                    .notificationExecutor()
                    .execute(() -> prepareToSendNotification(message));
            return;
        }

        String title;
        String content;
        int mNotificationId = ChatUINotificationHelper.getNotificationId(message.getMessageId());
        ChannelType type = message.getChannelIdentifier().getChannelType();
        String targetId = message.getChannelIdentifier().getChannelId();

        ChannelKey targetKey =
                ChannelKey.obtain(
                        message.getChannelIdentifier().getChannelId(),
                        message.getChannelIdentifier().getChannelType());
        if (targetKey == null) {
            RLog.e(TAG, "onReceiveMessageFromApp targetKey is null");
        }
        if (type == ChannelType.GROUP) {
            ai.nexconn.chat.channel.model.GroupInfo group =
                    NCUserInfoManager.getInstance().getGroupInfo(targetId);
            title = group == null ? targetId : group.getGroupName();
            UserInfo senderInfo = getUserInfo(message.getSenderUserId(), message.getContent());
            if (senderInfo == null) {
                if (targetKey != null) {
                    messageMap.put(targetKey.getKey(), message);
                }
            }
            content =
                    senderInfo == null
                            ? message.getSenderUserId()
                            : NCUserInfoManager.getInstance().getUserDisplayName(senderInfo)
                                    + ":"
                                    + getMessageSummary(message);
        } else {
            UserInfo userInfo = getUserInfo(targetId, message.getContent());
            title =
                    userInfo == null
                            ? targetId
                            : NCUserInfoManager.getInstance().getUserDisplayName(userInfo);
            if (userInfo == null) {
                if (targetKey != null) {
                    messageMap.put(targetKey.getKey(), message);
                }
            }
            content = getMessageSummary(message);
        }
        if (NCChatUIConfig.notificationConfig()
                .getTitleType()
                .equals(NotificationConfig.TitleType.APP_NAME)) {
            title =
                    mApplication
                            .getPackageManager()
                            .getApplicationLabel(mApplication.getApplicationInfo())
                            .toString();
        }

        Class<? extends Activity> destination =
                RouteUtils.getActivity(RouteUtils.ChatUIActivityType.ChannelActivity);
        Intent intent =
                new Intent(mApplication, destination == null ? ChannelActivity.class : destination);
        intent.putExtra(RouteUtils.CHANNEL_TYPE, type.name().toLowerCase());
        intent.putExtra(RouteUtils.TARGET_ID, targetId);
        intent.putExtra(RouteUtils.MESSAGE_ID, message.getClientId());
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent =
                    PendingIntent.getActivity(
                            mApplication,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent =
                    PendingIntent.getActivity(
                            mApplication, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        if (NCChatUIConfig.notificationConfig().getInterceptor() != null) {
            pendingIntent =
                    NCChatUIConfig.notificationConfig()
                            .getInterceptor()
                            .onPendingIntent(pendingIntent, intent);
        }

        // TODO: Replace with nexconn equivalent - MessagePushConfig
        Object messagePushConfig = null;
        // Configure MessagePushConfig properties
        // TODO: Replace with nexconn equivalent - MessagePushConfig
        // if (messagePushConfig != null) {
        //     if (messagePushConfig.getAndroidConfig() != null) {
        //         ...
        //     }
        // }

        NotificationUtil.getInstance()
                .showNotification(
                        mApplication.getApplicationContext(),
                        title,
                        content,
                        pendingIntent,
                        mNotificationId);
        requestCode++;
    }

    @NonNull
    private String getMessageSummary(Message message) {
        if (isShowUnknownMessage(message)) {
            return mApplication.getString(R.string.nc_recall_success);
        }
        ai.nexconn.chat.message.MessageContent content = message.getContent();
        if (content == null) {
            return "";
        }
        return NCChatUIConfig.channelConfig()
                .getMessageSummary(mApplication.getApplicationContext(), content)
                .toString();
    }

    /**
     * Whether a local notification should be shown. The SDK does not show local notifications in
     * these cases: 1. Chatroom messages have no local notifications. 2. Foreground offline messages
     * and non-counted messages have no local notifications. 3. Messages received during
     * do-not-disturb mode have no notifications.
     *
     * @return true if local notification is needed; false otherwise
     */
    private boolean shouldNotify(Message message, int left, boolean hasPackage, boolean offline) {
        // TODO: Replace with nexconn equivalent - MessageConfig
        if (message.getDisableNotification()) {
            return false;
        }
        if (offline && mIsInForeground) {
            return false;
        }
        if (!message.isCounted() && !isShowUnknownMessage(message)) {
            return false;
        }

        // If notification is intercepted, SDK stops processing.
        if (NCChatUIConfig.notificationConfig().getInterceptor() != null
                && NCChatUIConfig.notificationConfig()
                        .getInterceptor()
                        .isNotificationIntercepted(message)) {
            return false;
        }
        // No local notification for chatroom or when on conversation page.
        // High-priority mention messages should still alert on the channel list page.
        if (message.getChannelIdentifier().getChannelType() == ChannelType.OPEN
                || isInCurrentUserConversationPage(message)
                || (!isHighPriorityMessage(message) && isInConversationPage(message))) {
            return false;
        }

        return true;
    }

    // TODO: Replace with nexconn equivalent - UnknownMessage
    private boolean isShowUnknownMessage(Message message) {
        return message != null && message.getContent() instanceof UnknownMessage;
    }

    private boolean isRecallFiltered(Message message) {
        // No local notification when in foreground
        if (mIsInForeground) {
            return true;
        }

        // TODO: Replace with nexconn equivalent - MessageConfig
        if (message.getDisableNotification()) {
            return true;
        }

        // If notification is intercepted, SDK stops processing.
        NotificationConfig.Interceptor interceptor =
                NCChatUIConfig.notificationConfig().getInterceptor();
        if (interceptor != null && interceptor.isNotificationIntercepted(message)) {
            return true;
        }

        // When global do-not-disturb settings have not been synced successfully, default to no
        // notification.
        if (!isQuietSettingSynced) {
            getNotificationQuietHours(null);
            return true;
        } else return isInQuietTime();
    }

    /**
     * Whether the user is on the channel list, channel, or short video recording page.
     *
     * @return whether the user is on one of those pages
     */
    private boolean isInConversationPage(Message message) {
        if (!mIsInForeground) {
            return false;
        }
        return isInCurrentUserConversationPage(message)
                || isInChannelListPage()
                || isRecordOrPlay();
    }

    private boolean shouldAlertInForeground(Message message) {
        if (!isInConversationPage(message)) {
            return true;
        }
        return isHighPriorityMessage(message) && isInChannelListPage();
    }

    private boolean isInChannelListPage() {
        return mIsInForeground && mIsChannelListPageResumed;
    }

    public boolean isInCurrentUserConversationPage(Message message) {
        if (!mIsInForeground) {
            return false;
        }
        ChannelIdentifier resumedIdentifier = mResumedChannelIdentifier;
        return message != null && isSameChannel(resumedIdentifier, message.getChannelIdentifier());
    }

    public void onChannelPageResumed(@Nullable ChannelIdentifier identifier) {
        mResumedChannelIdentifier = identifier;
    }

    public void onChannelPagePaused(@Nullable ChannelIdentifier identifier) {
        if (identifier == null || mResumedChannelIdentifier == null) {
            mResumedChannelIdentifier = null;
            return;
        }
        if (isSameChannel(identifier, mResumedChannelIdentifier)) {
            mResumedChannelIdentifier = null;
        }
    }

    public void onChannelListPageResumed() {
        mIsChannelListPageResumed = true;
    }

    public void onChannelListPagePaused() {
        mIsChannelListPageResumed = false;
    }

    private boolean isSameChannel(
            @Nullable ChannelIdentifier first, @Nullable ChannelIdentifier second) {
        return first != null
                && second != null
                && first.getChannelType() == second.getChannelType()
                && TextUtils.equals(first.getChannelId(), second.getChannelId());
    }

    private boolean isRecordOrPlay() {
        if (mTopForegroundActivity == null) {
            return false;
        }
        boolean isRecordOrPlay = false;
        if ("ai.nexconn.chatui.shortvideo.player.ShortVideoPlayerActivity"
                .equals(mTopForegroundActivity.getClass().getName())) {
            return true;
        }

        if (!"ai.nexconn.chatui.shortvideo.record.ShortVideoRecordActivity"
                .equals(mTopForegroundActivity.getClass().getName())) {
            return false;
        }
        try {
            Class c = Class.forName("ai.nexconn.chatui.shortvideo.record.CameraView");
            Field field = c.getDeclaredField("isRecorder");
            Field fieldPlay = c.getDeclaredField("isPlay");
            field.setAccessible(true);
            fieldPlay.setAccessible(true);
            isRecordOrPlay = (boolean) field.get(c) || (boolean) fieldPlay.get(c);
        } catch (Exception e) {
            RLog.i(TAG, "isRecordOrPlay " + e);
        }
        return isRecordOrPlay;
    }

    private boolean isHighPriorityMessage(Message message) {
        if (message == null || message.getContent() == null) {
            return false;
        }
        NotificationConfig.Interceptor interceptor =
                NCChatUIConfig.notificationConfig().getInterceptor();
        if (interceptor != null) {
            return interceptor.isHighPriorityMessage(message);
        } else if (message.getContent().getMentionedInfo() != null) {
            MentionedInfo mentionedInfo = message.getContent().getMentionedInfo();
            return mentionedInfo.getType().equals(MentionedType.ALL)
                    || (mentionedInfo.getType().equals(MentionedType.USERS)
                            && mentionedInfo.getUserIdList() != null
                            && mentionedInfo.getUserIdList().contains(NCEngine.getCurrentUserId()));
        }
        return false;
    }

    /**
     * Sets the notification do-not-disturb time.
     *
     * @param startTime start time in HH:MM:SS format.
     * @param spanMinutes interval in minutes, must be greater than 0 and less than 1440.
     * @param callback callback for the operation result.
     */
    public void setNotificationQuietHours(
            final String startTime, final int spanMinutes, final ErrorHandler callback) {
        String timezone = TimeZone.getDefault().getID();
        NoDisturbTimeParams params =
                new NoDisturbTimeParams(startTime, spanMinutes, NoDisturbTimeLevel.MUTED, timezone);
        NCEngine.setNoDisturbTime(
                params,
                error -> {
                    if (error == null) {
                        mQuietStartTime = startTime;
                        mQuietSpanTime = spanMinutes;
                        MessageNotificationHelper.updateQuietHour(
                                NoDisturbTimeLevel.MUTED.getValue(),
                                startTime,
                                spanMinutes,
                                timezone);
                    }
                    if (callback != null) {
                        callback.onError(error);
                    }
                });
    }

    /** Gets the notification do-not-disturb time; updates local cache after syncing from server. */
    public void getNotificationQuietHours(final Object callback) {
        NCEngine.getNoDisturbTime(
                new OperationHandler<NoDisturbTimeInfo>() {
                    @Override
                    public void onResult(NoDisturbTimeInfo info, NCError error) {
                        if (error == null && info != null) {
                            mQuietStartTime = info.getStartTime();
                            mQuietSpanTime = info.getSpanMinutes();
                            MessageNotificationHelper.updateQuietHour(
                                    info.getLevel().getValue(),
                                    info.getStartTime(),
                                    info.getSpanMinutes(),
                                    info.getTimezone());
                        }
                        isQuietSettingSynced = true;
                    }
                });
    }

    public void removeNotificationQuietHours(final ErrorHandler callback) {
        NCEngine.removeNoDisturbTime(
                error -> {
                    if (error == null) {
                        mQuietStartTime = null;
                        mQuietSpanTime = 0;
                        MessageNotificationHelper.updateQuietHour(0, null, 0, null);
                    }
                    if (callback != null) {
                        callback.onError(error);
                    }
                });
    }

    /**
     * @deprecated Use {@link #getChannelNoDisturbLevel(ChannelIdentifier, OperationHandler)}
     *     instead.
     */
    @Deprecated
    public void getConversationNotificationStatus(
            ChannelIdentifier channelIdentifier, final OperationHandler<Integer> callback) {
        getChannelNoDisturbLevel(channelIdentifier, callback);
    }

    public void getChannelNoDisturbLevel(
            ChannelIdentifier channelIdentifier, final OperationHandler<Integer> callback) {
        final String key =
                channelIdentifier.getChannelType().getValue()
                        + ";;;"
                        + channelIdentifier.getChannelId();
        Integer cached = mNotificationCache.get(key);
        if (cached != null && callback != null) {
            callback.onResult(cached, null);
            return;
        }
        // TODO: Replace with nexconn conversation-level disturbLevel API when available
        // Fall through to channel-type level
        BaseChannel.getChannelTypeNoDisturbLevel(
                channelIdentifier.getChannelType(),
                new OperationHandler<ChannelNoDisturbLevel>() {
                    @Override
                    public void onResult(ChannelNoDisturbLevel level, NCError error) {
                        int val =
                                level != null
                                        ? level.getValue()
                                        : ChannelNoDisturbLevel.DEFAULT.getValue();
                        mNotificationCache.put(key, val);
                        if (callback != null) {
                            callback.onResult(val, error);
                        }
                    }
                });
    }

    private void sound() {
        // If on main thread, dispatch to background for sound playback (MediaPlayer#native_setup
        // may block)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            ExecutorHelper.getInstance().compressExecutor().execute(() -> sound());
            return;
        }
        Uri res = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && NCChatUIConfig.notificationConfig().getInterceptor() != null) {
            NotificationChannel channel =
                    NCChatUIConfig.notificationConfig()
                            .getInterceptor()
                            .onRegisterChannel(
                                    NotificationUtil.getInstance().getDefaultChannel(mApplication));
            res = channel.getSound();
        }
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(
                    new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            if (mp != null) {
                                try {
                                    mp.stop();
                                    mp.reset();
                                    mp.release();
                                } catch (Exception e) {
                                    RLog.e(TAG, "sound", e);
                                }
                            }
                            if (mediaPlayer != null) {
                                mediaPlayer = null;
                            }
                        }
                    });
            // Set STREAM_RING mode: when the system is set to vibrate, use system settings to
            // determine whether to play notification sound.
            if (isWiredHeadsetOn(mApplication.getApplicationContext())) {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            } else if (isBluetoothA2dpOn(mApplication.getApplicationContext())) {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
            }
            mediaPlayer.setDataSource(mApplication, res);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(
                    new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            if (mediaPlayer != null) {
                                mediaPlayer.start();
                            }
                        }
                    });
        } catch (Exception e) {
            RLog.e(TAG, "sound", e);
            if (mediaPlayer != null) {
                mediaPlayer = null;
            }
        }
    }

    private boolean isWiredHeadsetOn(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                int deviceType = device.getType();
                if (deviceType == AudioDeviceInfo.TYPE_WIRED_HEADSET
                        || deviceType == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                    return true;
                }
            }
        } else {
            return audioManager.isWiredHeadsetOn();
        }
        return false;
    }

    private boolean isBluetoothA2dpOn(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.isBluetoothA2dpOn();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) mApplication.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(new long[] {0, 200, 250, 200}, -1);
        }
    }

    private boolean isInQuietTime() {
        int hour = -1;
        int minute = -1;
        int second = -1;

        if (!TextUtils.isEmpty(mQuietStartTime) && mQuietStartTime.contains(":")) {
            String[] time = mQuietStartTime.split(":");

            try {
                if (time.length >= 3) {
                    hour = Integer.parseInt(time[0]);
                    minute = Integer.parseInt(time[1]);
                    second = Integer.parseInt(time[2]);
                }
            } catch (NumberFormatException e) {
                RLog.e(TAG, "getConversationNotificationStatus NumberFormatException");
            }
        }

        if (hour == -1 || minute == -1 || second == -1) {
            return false;
        }

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(Calendar.HOUR_OF_DAY, hour);
        startCalendar.set(Calendar.MINUTE, minute);
        startCalendar.set(Calendar.SECOND, second);

        long startTime = startCalendar.getTimeInMillis();

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTimeInMillis(startTime + mQuietSpanTime * 60 * 1000);

        Calendar currentCalendar = Calendar.getInstance();
        // DND period can be within the same day (e.g. 12:00-14:00) or span midnight (e.g.
        // 22:00-07:00 next day)
        if (currentCalendar.get(Calendar.DAY_OF_MONTH) == endCalendar.get(Calendar.DAY_OF_MONTH)) {

            return currentCalendar.after(startCalendar) && currentCalendar.before(endCalendar);
        } else {

            // Spans midnight and currentCalendar is before startCalendar: check if between 00:00
            // and endCalendar
            if (currentCalendar.before(startCalendar)) {

                endCalendar.add(
                        Calendar.DAY_OF_MONTH,
                        -1); // Subtract 1 day from endCalendar for comparison

                return currentCalendar.before(endCalendar);
            } else {
                // Spans midnight and currentCalendar is after startCalendar: must be in DND period
                return true;
            }
        }
    }

    private void registerActivityLifecycleCallback() {
        mApplication.registerActivityLifecycleCallbacks(
                new Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(
                            @NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                        // do nothing
                    }

                    @Override
                    public void onActivityStarted(@NonNull Activity activity) {
                        // do nothing
                    }

                    @Override
                    public void onActivityResumed(@NonNull Activity activity) {
                        if (mTopForegroundActivity == null) {
                            mIsInForeground = true;
                        }
                        mTopForegroundActivity = activity;
                    }

                    @Override
                    public void onActivityPaused(@NonNull Activity activity) {
                        // do nothing
                    }

                    @Override
                    public void onActivityStopped(@NonNull Activity activity) {
                        if (mTopForegroundActivity == activity) {
                            mIsInForeground = false;
                            mTopForegroundActivity = null;
                        }
                    }

                    @Override
                    public void onActivitySaveInstanceState(
                            @NonNull Activity activity, @NonNull Bundle outState) {
                        // do nothing
                    }

                    @Override
                    public void onActivityDestroyed(@NonNull Activity activity) {
                        // do nothing
                    }
                });
    }

    /** Clears all notifications */
    public void clearAllNotification() {
        if (mApplication == null) {
            return;
        }
        NotificationManager notificationManager =
                (NotificationManager) mApplication.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    /**
     * @param targetId the target ID
     * @param channelType the channel type
     * @param stateType state type: 1 for do-not-disturb, 2 for pinned
     * @return the cache key
     */
    private String getKey(String targetId, ChannelType channelType, final String stateType) {
        return channelType.getValue() + stateType + targetId;
    }

    @Override
    public void onUserUpdate(UserInfo user) {
        if (user == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            ExecutorHelper.getInstance()
                    .notificationExecutor()
                    .execute(() -> resendNotificationOnInfoUpdate(user.getUserId()));
        }
    }

    @Override
    public void onGroupUpdate(ai.nexconn.chat.channel.model.GroupInfo group) {
        if (group == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            ExecutorHelper.getInstance()
                    .notificationExecutor()
                    .execute(() -> resendNotificationOnInfoUpdate(group.getGroupId()));
        }
    }

    /**
     * Resends the notification when the cached info for the targetId is updated
     *
     * @param targetId the target ID
     */
    private void resendNotificationOnInfoUpdate(String targetId) {
        ChannelType[] types =
                new ChannelType[] {
                    ChannelType.DIRECT, ChannelType.GROUP, ChannelType.OPEN, ChannelType.SYSTEM
                };
        Message message;
        for (ChannelType type : types) {
            ChannelKey conversationKey = ChannelKey.obtain(targetId, type);
            if (conversationKey == null) {
                continue;
            }
            String key = conversationKey.getKey();
            if (messageMap.containsKey(key)) {
                message = messageMap.get(key);
                messageMap.remove(key);
                if (message != null) {
                    prepareToSendNotification(message);
                }
            }
        }
    }

    @Override
    public void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo) {
        // do nothing
    }

    private static class SingletonHolder {
        static ChatUINotificationManager sInstance = new ChatUINotificationManager();
    }
}
