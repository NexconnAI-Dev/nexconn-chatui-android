package ai.nexconn.chatui.config;

import ai.nexconn.chat.channel.ChannelType;
import java.util.List;

/**
 * Abstract base implementation of {@link DataProcessor}.
 *
 * <p>Supports all major channel types by default. Override individual methods to customize
 * filtering or gathered-display behavior.
 */
public abstract class BaseDataProcessor<T> implements DataProcessor<T> {
    @Override
    public ChannelType[] supportedTypes() {
        return new ChannelType[] {
            ChannelType.DIRECT,
            ChannelType.GROUP,
            ChannelType.COMMUNITY,
            ChannelType.SYSTEM,
            ChannelType.OPEN
        };
    }

    @Override
    public List<T> filtered(List<T> data) {
        return data;
    }

    @Override
    public boolean isGathered(ChannelType type) {
        return false;
    }
}
