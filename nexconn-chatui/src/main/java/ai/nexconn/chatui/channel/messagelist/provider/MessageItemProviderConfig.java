package ai.nexconn.chatui.channel.messagelist.provider;

/**
 * Message display configuration. Parameters can be set in BaseMessageItemProvider subclass
 * constructors.
 */
public class MessageItemProviderConfig {
    /** Whether to show the user portrait. */
    public boolean showPortrait = true;

    /** Whether to center the content horizontally. */
    public boolean centerInHorizontal = false;

    /** Whether to show the send-failure warning icon. */
    public boolean showWarning = true;

    /** Whether to show the sending progress indicator. */
    public boolean showProgress = true;

    /** Whether to show the sender's nickname above the message in the conversation UI. */
    public boolean showSummaryWithName = true;

    /**
     * Whether to show the read-receipt status next to messages in direct chats. Default is false.
     */
    public boolean showReadState = false;

    /** Whether to show the message bubble background. */
    public boolean showContentBubble = true;

    /** Whether to show the edit status indicator. Default is false. */
    public boolean showEditState = false;
}
