package ai.nexconn.chatui.channel.feature.recall;

public interface RecallEditCountDownTimerListener {
    /**
     * @param untilFinished remaining time in seconds
     * @param messageId message UID
     */
    void onTick(long untilFinished, String messageId);

    /**
     * @param messageId message UID
     */
    void onFinish(String messageId);
}
