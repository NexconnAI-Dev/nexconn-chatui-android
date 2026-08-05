package ai.nexconn.chatui.notification;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.model.MentionedInfo;
import ai.nexconn.chat.message.model.MentionedType;
import ai.nexconn.chatui.utils.log.RLog;
import android.text.TextUtils;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

public class MessageNotificationHelper {
    private static final String TAG = MessageNotificationHelper.class.getSimpleName();
    private static final ConcurrentHashMap<String, Integer> levelMap = new ConcurrentHashMap<>();
    private static Integer quietLevel = null;
    private static String mStartTime;
    private static int mSpanTime;
    private static String mTimeZone;
    private static NotifyListener mNotifyListener;

    public static void updateLevelMap(
            ai.nexconn.chat.channel.model.ChannelIdentifier identifier, int level) {
        String targetId =
                TextUtils.isEmpty(identifier.getChannelId()) ? "" : identifier.getChannelId();
        levelMap.put(identifier.getChannelType().getValue() + ";;;" + targetId + ";", level);
    }

    public static void updateLevelMap(String key, int level) {
        levelMap.put(key, level);
    }

    public static void updateQuietHour(int level, String startTime, int spanTime, String timezone) {
        quietLevel = level;
        mStartTime = startTime;
        mSpanTime = spanTime;
        mTimeZone = timezone;
    }

    public static void clearCache() {
        levelMap.clear();
    }

    private static boolean containsLevelMap(Message message) {
        String targetId =
                TextUtils.isEmpty(message.getChannelIdentifier().getChannelId())
                        ? ""
                        : message.getChannelIdentifier().getChannelId();
        String channelId = "";
        String key =
                message.getChannelIdentifier().getChannelType().getValue()
                        + ";;;"
                        + targetId
                        + ";"
                        + channelId;
        Integer level = levelMap.get(key);
        if (levelMap.containsKey(key) && level != null && level != 0) {
            notifyMessage(level, message);
            return true;
        }
        return false;
    }

    public static void getNotificationQuietHoursLevelLegacy(
            String startTime, int spanTime, Runnable onSuccess) {
        if (quietLevel != null && onSuccess != null) {
            onSuccess.run();
        }
    }

    public static void getNotificationQuietHoursLevel(Message message) {
        if (message == null || message.getContent() == null) {
            return;
        }

        if (isInQuietTime(mStartTime, mSpanTime, mTimeZone)) {
            if (quietLevel == 5) {
                return;
            }

            if (quietLevel == 0) {
                getConversationChannelNotificationLevel(message);
                return;
            }

            boolean notify = checkQuietHourAbility(message);
            if (notify) {
                if (mNotifyListener != null) {
                    mNotifyListener.onPreToNotify(message);
                }
            }
        } else {
            getConversationChannelNotificationLevel(message);
        }
    }

    public static void getConversationChannelNotificationLevel(Message message) {
        if (message == null || message.getContent() == null) {
            return;
        }
        if (containsLevelMap(message)) {
            return;
        }
        // Channel-level notification API not yet available in nexconn; fall through
        getConversationNotificationLevel(message);
    }

    private static void notifyMessage(int level, Message message) {
        boolean notify = checkNotifyAbility(message, level);
        if (notify) {
            if (mNotifyListener != null) {
                mNotifyListener.onPreToNotify(message);
            }
        }
    }

    public static void getConversationNotificationLevel(Message message) {
        if (message == null || message.getContent() == null) {
            return;
        }
        if (containsLevelMap(message)) {
            return;
        }
        // Conversation-level notification API not yet available in nexconn; fall through to type
        // level
        getConversationTypeNotificationLevel(message);
    }

    public static void getConversationTypeNotificationLevel(Message message) {
        if (message == null || message.getContent() == null) {
            return;
        }
        if (containsLevelMap(message)) {
            return;
        }
        ChannelType channelType = message.getChannelIdentifier().getChannelType();
        BaseChannel.getChannelTypeNoDisturbLevel(
                channelType,
                new ai.nexconn.chat.handler.OperationHandler<
                        ai.nexconn.chat.channel.model.ChannelNoDisturbLevel>() {
                    @Override
                    public void onResult(
                            ai.nexconn.chat.channel.model.ChannelNoDisturbLevel level,
                            ai.nexconn.chat.error.NCError error) {
                        int levelValue =
                                level != null
                                        ? level.getValue()
                                        : ai.nexconn.chat.channel.model.ChannelNoDisturbLevel
                                                .DEFAULT
                                                .getValue();
                        updateLevelMap(channelType.getValue() + ";;;" + ";", levelValue);
                        if (levelValue
                                == ai.nexconn.chat.channel.model.ChannelNoDisturbLevel.DEFAULT
                                        .getValue()) {
                            if (mNotifyListener != null) {
                                mNotifyListener.onPreToNotify(message);
                            }
                            return;
                        }
                        notifyMessage(levelValue, message);
                    }
                });
    }

    private static boolean checkNotifyAbility(Message message, int level) {
        MentionedInfo mentionedInfo = message.getContent().getMentionedInfo();
        ChannelType channelType = message.getChannelIdentifier().getChannelType();
        if (level == -1) {
            return true;
        }

        if (level == 5) {
            return false;
        }

        if (level == 1) {
            if (channelType == ChannelType.COMMUNITY || channelType == ChannelType.GROUP) {
                return mentionedInfo != null
                        && (MentionedType.ALL.equals(mentionedInfo.getType())
                                || (MentionedType.USERS.equals(mentionedInfo.getType()))
                                        && mentionedInfo.getUserIdList() != null
                                        && mentionedInfo
                                                .getUserIdList()
                                                .contains(NCEngine.getCurrentUserId()));
            }

            if (channelType == ChannelType.DIRECT || channelType == ChannelType.SYSTEM) {
                return false;
            }
        }

        if (level == 2) {
            return mentionedInfo != null
                    && MentionedType.USERS.equals(mentionedInfo.getType())
                    && mentionedInfo.getUserIdList() != null
                    && mentionedInfo.getUserIdList().contains(NCEngine.getCurrentUserId());
        }

        if (level == 4) {
            return mentionedInfo != null && MentionedType.ALL.equals(mentionedInfo.getType());
        }
        return false;
    }

    private static boolean checkQuietHourAbility(Message message) {
        MentionedInfo mentionedInfo = message.getContent().getMentionedInfo();
        ChannelType channelType = message.getChannelIdentifier().getChannelType();

        if (channelType == ChannelType.DIRECT || channelType == ChannelType.SYSTEM) {
            return false;
        }

        if (channelType == ChannelType.COMMUNITY || channelType == ChannelType.GROUP) {
            return mentionedInfo != null
                    && (MentionedType.ALL.equals(mentionedInfo.getType())
                            || (MentionedType.USERS).equals(mentionedInfo.getType()));
        }
        return false;
    }

    private static boolean isInQuietTime(String startTime, int spanMinutes, String timeZoneId) {
        int hour = -1;
        int minute = -1;
        int second = -1;

        if (!TextUtils.isEmpty(startTime) && startTime.contains(":")) {
            String[] time = startTime.split(":");

            try {
                if (time.length >= 3) {
                    hour = Integer.parseInt(time[0]);
                    minute = Integer.parseInt(time[1]);
                    second = Integer.parseInt(time[2]);
                }
            } catch (NumberFormatException e) {
                RLog.e(TAG, "e : " + e.getMessage());
            }
        }

        if (hour == -1 || minute == -1 || second == -1) {
            return false;
        }

        TimeZone timeZone = TimeZone.getTimeZone(timeZoneId != null ? timeZoneId : "");
        Calendar startCalendar = Calendar.getInstance(timeZone);
        startCalendar.set(Calendar.HOUR_OF_DAY, hour);
        startCalendar.set(Calendar.MINUTE, minute);
        startCalendar.set(Calendar.SECOND, second);

        long start = startCalendar.getTimeInMillis();

        Calendar endCalendar = Calendar.getInstance(timeZone);
        endCalendar.setTimeInMillis(start + (long) spanMinutes * 60 * 1000);

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

    static void setNotifyListener(NotifyListener notifyListener) {
        mNotifyListener = notifyListener;
    }

    interface NotifyListener {
        void onPreToNotify(Message message);
    }

    /**
     * Per-channel DND level updates are now handled by ChannelHandler.onChannelNoDisturbLevelSync
     * registered in ChatUINotificationManager.init(). Global quiet hours are refreshed via
     * NCEngine.getNoDisturbTime() on ChannelHandler.onChannelStatusSyncCompleted.
     */
    public static void setPushNotifyLevelListener() {
        // No-op: handled by ChannelHandler in ChatUINotificationManager
    }
}
