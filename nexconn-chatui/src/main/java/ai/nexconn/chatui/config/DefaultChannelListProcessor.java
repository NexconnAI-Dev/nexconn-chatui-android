package ai.nexconn.chatui.config;

import ai.nexconn.chat.channel.BaseChannel;
import android.text.TextUtils;
import java.util.LinkedList;
import java.util.List;

/**
 * Default {@link BaseDataProcessor} implementation for the channel list.
 *
 * <p>Filters out channels with a null or empty channel ID, and channels with a null {@link
 * ai.nexconn.chat.channel.ChannelType}.
 */
public class DefaultChannelListProcessor extends BaseDataProcessor<BaseChannel> {
    @Override
    public List<BaseChannel> filtered(List<BaseChannel> data) {
        List<BaseChannel> invalidChannels = new LinkedList<>();
        for (BaseChannel item : data) {
            if (TextUtils.isEmpty(item.getChannelId()) || item.getChannelType() == null) {
                invalidChannels.add(item);
            }
        }
        data.removeAll(invalidChannels);
        return data;
    }
}
