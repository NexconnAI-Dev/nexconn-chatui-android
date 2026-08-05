package ai.nexconn.chatui.config;

public enum ChannelLoadMessageType {
    /** Always load messages (default). */
    ALWAYS,
    /** Never load messages. */
    NEVER,
    /** Load from local cache only. */
    LOCAL_CACHE,
    /** Show dialog asking user whether to load remote messages. */
    ASK,
    /** Only show successfully loaded messages, skip failed remote loads. */
    ONLY_SUCCESS
}
