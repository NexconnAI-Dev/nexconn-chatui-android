package ai.nexconn.chatui.channel.messagelist.processor;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chatui.utils.log.RLog;
import android.util.ArrayMap;

/**
 * Channel processor factory. Creates different processors by channel type, each handling
 * type-specific business logic. Custom processors can be set for custom business handling.
 */
public class ChannelProcessorFactory {
    private static final String TAG = ChannelProcessorFactory.class.getSimpleName();

    public static ChannelProcessorFactory getInstance() {
        return Holder.instance;
    }

    ArrayMap<ChannelType, IChannelProcessor> mProcessorMap;

    private ChannelProcessorFactory() {
        mProcessorMap = new ArrayMap<>();
        mProcessorMap.put(ChannelType.DIRECT, new DirectChannelProcessor());
        mProcessorMap.put(ChannelType.SYSTEM, new DirectChannelProcessor());
        mProcessorMap.put(ChannelType.GROUP, new GroupChannelProcessor());
        mProcessorMap.put(ChannelType.COMMUNITY, new GroupChannelProcessor());
        mProcessorMap.put(ChannelType.OPEN, new OpenChannelProcessor());
    }

    private static class Holder {
        private static ChannelProcessorFactory instance = new ChannelProcessorFactory();
    }

    public IChannelProcessor getProcessor(ChannelType type) {
        IChannelProcessor processor = mProcessorMap.get(type);
        if (processor == null) {
            RLog.e(
                    TAG,
                    "No processor defined for type :"
                            + type.name()
                            + "; Using private processor as default.");
            processor = mProcessorMap.get(ChannelType.DIRECT);
        }
        return processor;
    }

    public void setProcessor(ChannelType type, IChannelProcessor processor) {
        mProcessorMap.put(type, processor);
    }
}
