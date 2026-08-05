package ai.nexconn.chatui.config;

import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chatui.picture.engine.ImageEngine;
import android.content.Context;
import android.widget.ImageView;
import androidx.annotation.NonNull;

/**
 * Image loading engine used by the ChatUI SDK.
 *
 * <p>Implement this interface to provide a custom image-loading backend. The default implementation
 * is {@link GlideChatUIImageEngine}.
 *
 * <p>Register a custom implementation via {@link
 * ai.nexconn.chatui.config.FeatureConfig#setChatUIImageEngine}.
 */
public interface ChatUIImageEngine extends ImageEngine {
    /**
     * Loads the avatar for a single conversation item in the conversation list.
     *
     * @param context context
     * @param url avatar URL
     * @param imageView target ImageView
     * @param channel current channel; use the channel type to apply type-specific default images
     */
    void loadConversationListPortrait(
            @NonNull Context context,
            @NonNull String url,
            @NonNull ImageView imageView,
            BaseChannel channel);

    /**
     * Loads the user avatar shown next to a message in the conversation screen.
     *
     * @param context context
     * @param url avatar URL
     * @param imageView target ImageView
     * @param message current message; use to apply type-specific default images
     */
    void loadConversationPortrait(
            @NonNull Context context,
            @NonNull String url,
            @NonNull ImageView imageView,
            Message message);

    /**
     * Loads a user avatar.
     *
     * @param context context
     * @param url avatar URL
     * @param imageView target ImageView
     */
    void loadUserPortrait(
            @NonNull Context context, @NonNull String url, @NonNull ImageView imageView);

    /**
     * Loads a group avatar.
     *
     * @param context context
     * @param url group avatar URL
     * @param imageView target ImageView
     */
    void loadGroupPortrait(
            @NonNull Context context, @NonNull String url, @NonNull ImageView imageView);
}
