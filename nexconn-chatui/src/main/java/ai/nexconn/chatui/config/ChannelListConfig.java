package ai.nexconn.chatui.config;

import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channellist.model.BaseUiChannel;
import ai.nexconn.chatui.channellist.provider.ConversationListProvider;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.widget.adapter.IViewProvider;
import ai.nexconn.chatui.widget.adapter.ProviderManager;
import android.content.Context;
import android.content.res.Resources;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the channel-list screen.
 *
 * <p>Obtain the singleton instance via {@link NCChatUIConfig#channelListConfig()}.
 */
public class ChannelListConfig {

    private final String TAG = "ChannelListConfig";
    private final ChannelType[] mSupportedTypes = {
        ChannelType.DIRECT,
        ChannelType.GROUP,
        ChannelType.SYSTEM,
        ChannelType.OPEN,
        ChannelType.COMMUNITY
    };
    private ChannelListBehaviorListener mListener;
    private boolean mIsEnableConnectStateNotice = true;
    private boolean mEnableAutomaticDownloadHQVoice = true;
    private int mConversationCountPerPage = 100;
    private int delayRefreshTime = 5000;
    private boolean topPriority = true;
    private ProviderManager<BaseUiChannel> mProviderManager;
    private DataProcessor<BaseChannel> mDataProcessor;

    private BaseDataProcessor<BaseChannel> mConversationListDataProcessor =
            new DefaultChannelListProcessor();

    public ChannelListConfig() {
        List<IViewProvider<BaseUiChannel>> providerList = new ArrayList<>();
        mProviderManager = new ProviderManager<>(providerList);
        mProviderManager.setDefaultProvider(new ConversationListProvider());
    }

    /**
     * Initializes this configuration from Android resource values.
     *
     * @param context application or activity context
     */
    public void initConfig(Context context) {
        if (context != null) {
            Resources resources = context.getResources();
            try {
                mIsEnableConnectStateNotice =
                        resources.getBoolean(R.bool.nc_is_show_warning_notification);
            } catch (Exception e) {
                RLog.e(TAG, "NC_is_show_warning_notification not get value", e);
            }
        }
    }

    /**
     * Sets the behavior listener for the channel-list screen.
     *
     * @param listener the listener to register
     */
    public void setBehaviorListener(ChannelListBehaviorListener listener) {
        this.mListener = listener;
    }

    /**
     * Sets the number of conversations to load per page.
     *
     * @param count page size
     */
    public void setCountPerPage(int count) {
        this.mConversationCountPerPage = count;
    }

    /**
     * Replaces the provider manager for the channel list.
     *
     * @param providerManager the provider manager to use
     */
    public void setConversationListProvider(ProviderManager<BaseUiChannel> providerManager) {
        this.mProviderManager = providerManager;
    }

    /**
     * Adds a conversation list view provider.
     *
     * @param provider the provider to add
     */
    public void setConversationProvider(ConversationListProvider provider) {
        this.mProviderManager.addProvider(provider);
    }

    /** Returns the provider manager for the channel list. */
    public ProviderManager<BaseUiChannel> getProviderManager() {
        return mProviderManager;
    }

    /** Returns whether high-quality voice messages are downloaded automatically. */
    public boolean isEnableAutomaticDownloadHQVoice() {
        return mEnableAutomaticDownloadHQVoice;
    }

    /**
     * Enables or disables automatic download of high-quality voice messages.
     *
     * @param enable {@code true} to enable
     */
    public void setEnableAutomaticDownloadHQVoice(boolean enable) {
        this.mEnableAutomaticDownloadHQVoice = enable;
    }

    /** Returns the current data processor for the channel list. */
    public DataProcessor<BaseChannel> getDataProcessor() {
        if (mDataProcessor != null) {
            return mDataProcessor;
        } else {
            return mConversationListDataProcessor;
        }
    }

    /**
     * Sets the data processor.
     *
     * @param dataFilter the data processor
     * @deprecated Use {@link #setDataProcessor(BaseDataProcessor)} instead.
     */
    @Deprecated
    public void setDataProcessor(DataProcessor<BaseChannel> dataFilter) {
        this.mDataProcessor = dataFilter;
    }

    /**
     * Sets a custom data processor for filtering and gathering channels.
     *
     * @param dataFilter the processor to use
     */
    public void setDataProcessor(BaseDataProcessor<BaseChannel> dataFilter) {
        this.mConversationListDataProcessor = dataFilter;
    }

    /** Returns the current behavior listener. */
    public ChannelListBehaviorListener getListener() {
        return mListener;
    }

    /** Returns whether the connection-state notification banner is enabled. */
    public boolean isEnableConnectStateNotice() {
        return mIsEnableConnectStateNotice;
    }

    /** Returns the number of conversations loaded per page. */
    public int getConversationCountPerPage() {
        return mConversationCountPerPage;
    }

    /** Returns the refresh delay (ms) after receiving a message. */
    public int getDelayRefreshTime() {
        return delayRefreshTime;
    }

    /**
     * Sets the refresh delay (ms) after receiving a message.
     *
     * @param delayRefreshTime delay in milliseconds
     */
    public void setDelayRefreshTime(int delayRefreshTime) {
        this.delayRefreshTime = delayRefreshTime;
    }

    /** Returns whether top-priority channels always appear first in the list. */
    public boolean isTopPriority() {
        return topPriority;
    }

    /**
     * Sets whether top-priority channels always appear first.
     *
     * @param topPriority {@code true} to enable top-priority ordering
     */
    public void setTopPriority(boolean topPriority) {
        this.topPriority = topPriority;
    }
}
