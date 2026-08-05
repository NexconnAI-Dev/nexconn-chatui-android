package ai.nexconn.chatui.channel.feature.recall;

import ai.nexconn.chat.message.Message;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RecallEditManager {
    private Map<String, ConcurrentHashMap<Integer, RecallEditCountDownTimer>> timerMap;

    private RecallEditManager() {
        timerMap = new HashMap<>();
    }

    private static class RecallEditManagerHolder {
        private static RecallEditManager instance = new RecallEditManager();
    }

    public static RecallEditManager getInstance() {
        return RecallEditManagerHolder.instance;
    }

    /**
     * Starts the countdown timer.
     *
     * @param message the message
     * @param millisInFuture countdown duration in milliseconds
     * @param callBack result callback
     */
    public void startCountDown(
            final Message message,
            long millisInFuture,
            final RecallEditCountDownCallBack callBack) {
        String key =
                message.getChannelIdentifier().getChannelType().name()
                        + message.getChannelIdentifier().getChannelId();
        ConcurrentHashMap<Integer, RecallEditCountDownTimer> recallEditTimerMap = timerMap.get(key);
        if (recallEditTimerMap != null) {
            RecallEditCountDownTimer timer = recallEditTimerMap.get(message.getClientId());
            if (timer != null) {
                timer.setListener(new RecallEditTimerListener(message, callBack));
                return;
            }
        }
        RecallEditCountDownTimer countDownTimer =
                new RecallEditCountDownTimer(
                        String.valueOf(message.getClientId()),
                        new RecallEditTimerListener(message, callBack),
                        millisInFuture);
        if (recallEditTimerMap == null) {
            ConcurrentHashMap<Integer, RecallEditCountDownTimer> timers = new ConcurrentHashMap<>();
            timers.put(message.getClientId(), countDownTimer);
            timerMap.put(key, timers);
        } else {
            recallEditTimerMap.put(message.getClientId(), countDownTimer);
        }
        countDownTimer.start();
    }

    /**
     * Cancels all countdown timers in the conversation.
     *
     * @param key composed of conversation type name + target ID
     */
    public void cancelCountDownInConversation(String key) {
        ConcurrentHashMap<Integer, RecallEditCountDownTimer> timers = timerMap.get(key);
        if (timers != null && timers.size() > 0) {
            Set<Map.Entry<Integer, RecallEditCountDownTimer>> entrySet = timers.entrySet();
            for (Map.Entry<Integer, RecallEditCountDownTimer> entry : entrySet) {
                RecallEditCountDownTimer timer = entry.getValue();
                if (timer != null) {
                    timer.cancel();
                }
            }
            timerMap.remove(key);
        }
    }

    /**
     * Cancels the countdown timer for the specified message ID.
     *
     * @param messageId the message ID
     */
    public void cancelCountDown(String messageId) {
        Set<Map.Entry<String, ConcurrentHashMap<Integer, RecallEditCountDownTimer>>> timerEntrySet =
                timerMap.entrySet();
        for (Map.Entry<String, ConcurrentHashMap<Integer, RecallEditCountDownTimer>> timerEntry :
                timerEntrySet) {
            ConcurrentHashMap<Integer, RecallEditCountDownTimer> timers = timerEntry.getValue();
            if (timers != null && timers.size() > 0) {
                RecallEditCountDownTimer timer = timers.get(Integer.valueOf(messageId));
                if (timer != null) {
                    timer.cancel();
                    timers.remove(Integer.valueOf(messageId));
                }
            }
        }
    }

    private class RecallEditTimerListener implements RecallEditCountDownTimerListener {
        private Message message;
        private RecallEditCountDownCallBack callBack;

        public RecallEditTimerListener(Message message, RecallEditCountDownCallBack callBack) {
            this.message = message;
            this.callBack = callBack;
        }

        @Override
        public void onTick(long untilFinished, String messageId) {
            // do nothing
        }

        @Override
        public void onFinish(String messageId) {
            Map<Integer, RecallEditCountDownTimer> value =
                    timerMap.get(
                            message.getChannelIdentifier().getChannelType().name() + messageId);
            if (value != null && value.get(message.getClientId()) != null) {
                value.remove(message.getClientId());
            }
            if (callBack != null) {
                callBack.onFinish(messageId);
            }
        }
    }
}
