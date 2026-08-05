package ai.nexconn.chatui.channel.messagelist.provider;

import ai.nexconn.chat.message.MessageContent;
import android.content.Context;
import android.text.Spannable;

public interface IChannelSummaryProvider<T extends MessageContent> {
    /**
     * Whether this template handles the given message content.
     *
     * @param messageContent the message content to check
     * @return true if this template handles this message; the upper layer will then call {@link
     *     #getSummarySpannable(Context, MessageContent)} to obtain the resource. false if this
     *     template does not handle this message.
     */
    boolean isSummaryType(MessageContent messageContent);

    /**
     * The content to display in the conversation list when the last message is of this type. For
     * example, an image message should return the string "Image".
     *
     * @param context the context
     * @param t the message content
     * @return the spannable string to display in the conversation list
     */
    Spannable getSummarySpannable(Context context, T t);

    /**
     * Whether to prepend the sender's name in the conversation summary. This setting only applies
     * to group channels.
     *
     * @return whether to show the sender name
     */
    boolean showSummaryWithName();
}
