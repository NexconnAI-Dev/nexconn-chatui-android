package ai.nexconn.chatui.channel.feature.location;

import ai.nexconn.chatui.channel.messagelist.processor.IChannelUIRenderer;

/**
 * @author gusd @Date 2022/05/15
 */
public abstract class LocationUiRender implements IChannelUIRenderer {
    private static final String TAG = "LocationUiRender";

    public abstract void joinLocation();
}
