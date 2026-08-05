package ai.nexconn.chatui.channel.messagelist.processor;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chatui.channel.ChannelFragment;
import ai.nexconn.chatui.channel.event.page.PageEvent;
import ai.nexconn.chatui.channel.extension.NCExtension;

/** Channel UI renderer. */
public interface IChannelUIRenderer {
    /**
     * Initializes each UI renderer after entering the channel page.
     *
     * @param fragment the channel fragment
     * @param extension the channel extension bar
     * @param channelType the channel type
     * @param targetId the channel id
     */
    void init(
            ChannelFragment fragment,
            NCExtension extension,
            ChannelType channelType,
            String targetId);

    /**
     * Callback for channel page rendering events.
     *
     * @param event the rendering event
     * @return whether the event was consumed. true means this renderer consumed the event and the
     *     channel page will not process it further; false means the channel page handles it by
     *     default.
     */
    boolean handlePageEvent(PageEvent event);

    /**
     * Callback when the back button is pressed.
     *
     * @return whether the event was consumed
     */
    boolean onBackPressed();

    /** Callback when leaving the channel page. */
    void onDestroy();
}
