package ai.nexconn.chatui.config;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.user.model.UserInfo;
import android.content.Context;
import android.view.View;

/**
 * Listener for user interaction events within a conversation (channel) screen.
 *
 * <p>Register an implementation via {@link
 * ChannelConfig#setChannelClickListener(ChannelClickListener)}.
 *
 * <p>Each method returns {@code true} if the app handles the event, or {@code false} to fall back
 * to the default SDK behavior.
 */
public interface ChannelClickListener {

    /**
     * Called when a user's avatar is tapped.
     *
     * @param context context
     * @param conversationType channel type
     * @param user the tapped user's info
     * @param targetId channel ID
     * @return {@code true} if the app handles this click; {@code false} to use the default behavior
     */
    boolean onUserPortraitClick(
            Context context, ChannelType conversationType, UserInfo user, String targetId);

    /**
     * Called when a user's avatar is long-pressed.
     *
     * @param context context
     * @param conversationType channel type
     * @param user the long-pressed user's info
     * @param targetId channel ID
     * @return {@code true} if the app handles this event; {@code false} to use the default behavior
     */
    boolean onUserPortraitLongClick(
            Context context, ChannelType conversationType, UserInfo user, String targetId);

    /**
     * Called when a message bubble is tapped.
     *
     * @param context context
     * @param view the View that received the tap
     * @param message the tapped message
     * @return {@code true} if the app handles this click; {@code false} to use the default behavior
     */
    boolean onMessageClick(Context context, View view, Message message);

    /**
     * Called when a message bubble is long-pressed.
     *
     * @param context context
     * @param view the View that received the long-press
     * @param message the long-pressed message
     * @return {@code true} if the app handles this event; {@code false} to use the default behavior
     */
    boolean onMessageLongClick(Context context, View view, Message message);

    /**
     * Called when a hyperlink inside a message is tapped.
     *
     * @param context context
     * @param link the tapped link URL
     * @param message the message that contains the link
     * @return {@code true} if the app handles this click; {@code false} to use the default behavior
     */
    boolean onMessageLinkClick(Context context, String link, Message message);

    /**
     * Called when the read-receipt status indicator of a message is tapped.
     *
     * @param context context
     * @param message the message whose receipt status was tapped
     * @return {@code true} if the app handles this click; {@code false} to use the default behavior
     */
    boolean onReadReceiptStateClick(Context context, Message message);

    /**
     * Called when the quick-reply button is tapped.
     *
     * @param context context
     * @return {@code true} if the app handles this click; {@code false} to use the default behavior
     */
    default boolean onQuickReplyClick(Context context) {
        return false;
    }
}
