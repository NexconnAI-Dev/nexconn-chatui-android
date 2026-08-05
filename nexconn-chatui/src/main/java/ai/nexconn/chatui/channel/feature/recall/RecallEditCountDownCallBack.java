package ai.nexconn.chatui.channel.feature.recall;

public interface RecallEditCountDownCallBack {
    /**
     * @param messageId message UID
     */
    void onFinish(String messageId);
}
