package ai.nexconn.chatui.config;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chatui.channellist.model.BaseUiChannel;
import android.content.Context;
import android.view.View;

/**
 * Listener for user interaction events on the channel-list screen.
 *
 * <p>Each method returns {@code true} if the app handles the event, or {@code false} to fall back
 * to the default SDK behavior.
 */
public interface ChannelListBehaviorListener {

    /**
     * Called when a conversation's avatar is tapped in the list.
     *
     * @param context context
     * @param channelType channel type
     * @param targetId the user ID of the tapped avatar
     * @return {@code true} if the app handles this click; {@code false} for default behavior
     */
    boolean onConversationPortraitClick(Context context, ChannelType channelType, String targetId);

    /**
     * Called when a conversation's avatar is long-pressed in the list.
     *
     * @param context context
     * @param channelType channel type
     * @param targetId the user ID of the long-pressed avatar
     * @return {@code true} if the app handles this event; {@code false} for default behavior
     */
    boolean onConversationPortraitLongClick(
            Context context, ChannelType channelType, String targetId);

    /**
     * Called when a conversation list item is long-pressed.
     *
     * @param context context
     * @param view the View that received the long-press
     * @param conversation the long-pressed conversation item
     * @return {@code true} if the app handles this event; {@code false} for default behavior
     */
    boolean onConversationLongClick(Context context, View view, BaseUiChannel conversation);

    /**
     * Called when a conversation list item is tapped.
     *
     * @param context context
     * @param view the View that received the tap
     * @param conversation the tapped conversation item
     * @return {@code true} if the app handles this click; {@code false} for default behavior
     */
    boolean onConversationClick(Context context, View view, BaseUiChannel conversation);
}
