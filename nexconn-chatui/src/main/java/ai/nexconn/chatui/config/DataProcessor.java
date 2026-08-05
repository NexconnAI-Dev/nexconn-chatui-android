package ai.nexconn.chatui.config;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import java.util.List;

/**
 * Processor for filtering and configuring channel data before display.
 *
 * <p>Implement this interface (or extend {@link BaseDataProcessor}) to customize which channel
 * types are shown and how they are filtered or aggregated in the channel list.
 *
 * <p>Register an implementation via {@link ChannelListConfig#setDataProcessor(DataProcessor)}.
 *
 * @param <T> the channel data type (typically {@link ai.nexconn.chat.channel.BaseChannel})
 */
public interface DataProcessor<T> {
    /**
     * Returns the channel types supported by this data processor.
     *
     * @return array of supported {@link ai.nexconn.chat.channel.ChannelType} values
     */
    ChannelType[] supportedTypes();

    /**
     * Filters the raw channel data before it is displayed in the channel list.
     *
     * <p>Called both when bulk-fetching channels from the database and when a new channel is
     * created from an incoming real-time message.
     *
     * @param data raw channel list to filter
     * @return filtered channel list
     */
    List<T> filtered(List<T> data);

    /**
     * Returns whether a channel type is displayed in aggregated (gathered) mode.
     *
     * @param type channel type to check
     * @return {@code true} if the type is gathered; {@code false} otherwise
     */
    boolean isGathered(ChannelType type);

    /**
     * Returns whether the channel identified by the given identifier is displayed in aggregated
     * mode.
     *
     * @param identifier channel identifier
     * @return {@code true} if the channel type is gathered; {@code false} otherwise
     */
    default boolean isGathered(ChannelIdentifier identifier) {
        if (identifier == null || identifier.getChannelType() == null) {
            return false;
        }
        return isGathered(identifier.getChannelType());
    }
}
