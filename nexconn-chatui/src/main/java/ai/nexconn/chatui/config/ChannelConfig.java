package ai.nexconn.chatui.config;

import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.HDVoiceMessage;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.model.MessageDirection;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.IChannelViewModelProcessor;
import ai.nexconn.chatui.channel.extension.component.moreaction.IClickActions;
import ai.nexconn.chatui.channel.feature.forward.ForwardClickActions;
import ai.nexconn.chatui.channel.feature.reference.ReferenceMessageItemProvider;
import ai.nexconn.chatui.channel.messagelist.processor.IChannelUIRenderer;
import ai.nexconn.chatui.channel.messagelist.provider.CombineMessageItemProvider;
import ai.nexconn.chatui.channel.messagelist.provider.DefaultMessageItemProvider;
import ai.nexconn.chatui.channel.messagelist.provider.FileMessageItemProvider;
import ai.nexconn.chatui.channel.messagelist.provider.GIFMessageItemProvider;
import ai.nexconn.chatui.channel.messagelist.provider.GroupNotificationMessageItemProvider;
import ai.nexconn.chatui.channel.messagelist.provider.HQVoiceMessageItemProvider;
import ai.nexconn.chatui.channel.messagelist.provider.HistoryDivMessageItemProvider;
import ai.nexconn.chatui.channel.messagelist.provider.IChannelSummaryProvider;
import ai.nexconn.chatui.channel.messagelist.provider.IMessageProvider;
import ai.nexconn.chatui.channel.messagelist.provider.ImageMessageItemProvider;
import ai.nexconn.chatui.channel.messagelist.provider.InformationNotificationMessageItemProvider;
import ai.nexconn.chatui.channel.messagelist.provider.ShortVideoMessageItemProvider;
import ai.nexconn.chatui.channel.messagelist.provider.TextMessageItemProvider;
import ai.nexconn.chatui.channel.messagelist.provider.UnknownMessageItemProvider;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.widget.adapter.ProviderManager;
import android.content.Context;
import android.content.res.Resources;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Configuration for the conversation (channel) screen.
 *
 * <p>Obtain the singleton instance via {@link NCChatUIConfig#channelConfig()}.
 */
public class ChannelConfig {

    private static final int conversationHistoryMessageMaxCount = 100;

    /** Enables multi-device unread count sync (direct and group channels only). */
    public static boolean enableMultiDeviceSync = true;

    private static int conversationRemoteMessageMaxCount = 100;
    private static int conversationShowUnreadMessageMaxCount = 100;
    private final String TAG = "ChannelConfig";
    // Message recall toggle
    public boolean NC_enable_recall_message = true;
    // Message resend toggle
    public boolean NC_enable_resend_message = true;
    public int NC_message_recall_interval = 120;
    public int NC_message_recall_edit_interval = 300;
    public int NC_chatroom_first_pull_message_count = 10;
    public boolean NC_is_show_warning_notification = true;
    // Whether unheard voice messages play continuously
    public boolean NC_play_audio_continuous = true;
    // Whether the @mention feature is enabled
    public boolean NC_enable_mentioned_message = true;
    // Read receipt validity duration (in seconds)
    public int NC_read_receipt_request_interval = 120;
    // Whether video files are included in the media selector
    public boolean NC_media_selector_contain_video = false;
    // Whether to auto-download HQ voice messages while online
    public boolean NC_enable_automatic_download_voice_msg = true;
    // Max GIF auto-download size; larger GIFs require manual download (in KB)
    public int NC_gifmsg_auto_download_size = 1024;
    // Maximum number of messages that can be multi-selected
    public int NC_max_message_selected_count = 100;
    // Whether combined-forward is enabled; off by default
    private boolean enableSendCombineMessage = false;
    private long NC_custom_service_evaluation_interval = 60 * 1000L;
    private boolean mStopCSWhenQuit = true;

    /** Read receipt; supported for direct and group channels only. */
    private boolean mEnableReadReceipt = true;

    private Set<ChannelType> mSupportReadReceiptConversationTypes = new HashSet<>(4);

    /** Whether to show the user title in direct chats. */
    private boolean showReceiverUserTitle = false;

    /** Whether to show the new-message unread bar (direct and group channels). */
    private boolean showNewMessageBar = true;

    /** Whether to show the history-message bar (direct and group channels). */
    private boolean showHistoryMessageBar = true;

    /** Whether to show the "more" option on long-press. */
    private boolean showMoreClickAction = true;

    /** Whether to show the history-divider message template. */
    private boolean showHistoryDividerMessage = true;

    /**
     * When {@code true}, long-press delete removes both local and remote messages.
     *
     * <p>When {@code false}, only the local message is deleted.
     *
     * <p>Default changed to {@code true} since version 5.6.3.
     */
    private boolean needDeleteRemoteMessage = true;

    /**
     * Whether to refresh the list when it becomes empty after deleting messages; defaults to {@code
     * true}.
     */
    private boolean needRefreshWhenListIsEmptyAfterDelete = true;

    private ChannelClickListener mChannelClickListener;
    private ProviderManager<UiMessage> mMessageListProvider = new ProviderManager<>();
    private List<IChannelUIRenderer> mConversationViewProcessors = new ArrayList<>();
    private CopyOnWriteArrayList<IChannelSummaryProvider> mConversationSummaryProviders =
            new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<IClickActions> mMoreClickActions = new CopyOnWriteArrayList<>();
    private IMessageProvider defaultMessageProvider = new DefaultMessageItemProvider();
    private IChannelViewModelProcessor mViewModelProcessor;
    // Whether to show the unread @mention message bar
    private boolean showNewMentionMessageBar = true;
    // Default number of history messages to fetch when entering a channel
    private int conversationHistoryMessageCount = 10;
    // Default number of remote history messages to fetch when entering a channel
    private int conversationRemoteMessageCount = 10;
    // Default number of unread messages to display when entering a channel
    private int conversationShowUnreadMessageCount = 10;
    private ChannelLoadMessageType conversationLoadMessageType = ChannelLoadMessageType.ALWAYS;

    ChannelConfig() {
        initMessageProvider();
        initViewProcessor();
        initMoreClickAction();
        mSupportReadReceiptConversationTypes.add(ChannelType.DIRECT);
        mSupportReadReceiptConversationTypes.add(ChannelType.GROUP);
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
                NC_enable_recall_message = resources.getBoolean(R.bool.nc_enable_message_recall);
            } catch (Exception e) {
                RLog.e(TAG, "NC_enable_recall_message not get value", e);
            }
            try {
                NC_message_recall_interval =
                        resources.getInteger(R.integer.nc_message_recall_interval);
            } catch (Exception e) {
                RLog.e(TAG, "NC_message_recall_interval not get value", e);
            }
            try {
                NC_message_recall_edit_interval =
                        resources.getInteger(R.integer.nc_message_recall_edit_interval);
            } catch (Exception e) {
                RLog.e(TAG, "NC_message_recall_edit_interval not get value", e);
            }
            try {
                NC_chatroom_first_pull_message_count =
                        resources.getInteger(R.integer.nc_chatroom_first_pull_message_count);
            } catch (Exception e) {
                RLog.e(TAG, "NC_chatroom_first_pull_message_count not get value", e);
            }
            try {
                mEnableReadReceipt = resources.getBoolean(R.bool.nc_read_receipt);
            } catch (Exception e) {
                RLog.e(TAG, "NC_read_receipt not get value", e);
            }
            try {
                enableMultiDeviceSync = resources.getBoolean(R.bool.nc_enable_sync_read_status);
            } catch (Exception e) {
                RLog.e(TAG, "NC_enable_sync_read_status not get value", e);
            }
            try {
                NC_play_audio_continuous = resources.getBoolean(R.bool.nc_play_audio_continuous);
            } catch (Exception e) {
                RLog.e(TAG, "NC_play_audio_continuous not get value", e);
            }
            try {
                NC_enable_mentioned_message =
                        resources.getBoolean(R.bool.nc_enable_mentioned_message);
            } catch (Exception e) {
                RLog.e(TAG, "NC_enable_mentioned_message not get value", e);
            }
            try {
                NC_read_receipt_request_interval =
                        resources.getInteger(R.integer.nc_read_receipt_request_interval);
            } catch (Exception e) {
                RLog.e(TAG, "NC_enable_mentioned_message not get value", e);
            }
            try {
                NC_media_selector_contain_video =
                        resources.getBoolean(R.bool.nc_media_selector_contain_video);
            } catch (Exception e) {
                RLog.e(TAG, "NC_media_selector_contain_video not get value", e);
            }
            try {
                NC_enable_automatic_download_voice_msg =
                        resources.getBoolean(R.bool.nc_enable_automatic_download_voice_msg);
            } catch (Exception e) {
                RLog.e(TAG, "NC_enable_automatic_download_voice_msg not get value", e);
            }
            try {
                NC_gifmsg_auto_download_size =
                        resources.getInteger(R.integer.nc_gifmsg_auto_download_size);
            } catch (Exception e) {
                RLog.e(TAG, "NC_gifmsg_auto_download_size not get value", e);
            }
            try {
                NC_max_message_selected_count =
                        resources.getInteger(R.integer.nc_max_message_selected_count);
            } catch (Exception e) {
                RLog.e(TAG, "NC_max_message_selected_count not get value", e);
            }
            try {
                showNewMentionMessageBar = resources.getBoolean(R.bool.nc_enable_unread_mention);
            } catch (Exception e) {
                RLog.e(TAG, "NC_enable_unread_mention not get value", e);
            }

            try {
                conversationHistoryMessageCount =
                        resources.getInteger(R.integer.nc_conversation_history_message_count);
                if (conversationHistoryMessageCount > conversationHistoryMessageMaxCount) {
                    conversationHistoryMessageCount = conversationHistoryMessageMaxCount;
                }
            } catch (Exception e) {
                RLog.e(TAG, "NC_conversation_history_message_count not get value", e);
            }

            try {
                conversationRemoteMessageCount =
                        resources.getInteger(R.integer.nc_conversation_remote_message_count);
                if (conversationRemoteMessageCount > conversationRemoteMessageMaxCount) {
                    conversationRemoteMessageCount = conversationRemoteMessageMaxCount;
                }
            } catch (Exception e) {
                RLog.e(TAG, "NC_conversation_remote_message_count not get value", e);
            }

            try {
                conversationShowUnreadMessageCount =
                        resources.getInteger(R.integer.nc_conversation_show_unread_message_count);
                if (conversationShowUnreadMessageCount > conversationShowUnreadMessageMaxCount) {
                    conversationShowUnreadMessageCount = conversationShowUnreadMessageMaxCount;
                }
            } catch (Exception e) {
                RLog.e(TAG, "NC_conversation_show_unread_message_count not get value", e);
            }
        }
    }

    private void initMessageProvider() {
        mMessageListProvider.setDefaultProvider(defaultMessageProvider);
        addMessageProvider(new TextMessageItemProvider());
        addMessageProvider(new ImageMessageItemProvider());
        addMessageProvider(new HQVoiceMessageItemProvider());
        addMessageProvider(new FileMessageItemProvider());
        addMessageProvider(new GIFMessageItemProvider());
        addMessageProvider(new InformationNotificationMessageItemProvider());
        addMessageProvider(new ReferenceMessageItemProvider());
        addMessageProvider(new ShortVideoMessageItemProvider());
        addMessageProvider(new HistoryDivMessageItemProvider());
        addMessageProvider(new GroupNotificationMessageItemProvider());
        addMessageProvider(new CombineMessageItemProvider());
        addMessageProvider(new UnknownMessageItemProvider());
    }

    private void initViewProcessor() {}

    private void initMoreClickAction() {
        mMoreClickActions.add(new ForwardClickActions());
    }

    /**
     * Inserts a click action at the specified position in the "more" actions list.
     *
     * @param index insertion position
     * @param action click action to add
     */
    public void addMoreClickAction(int index, IClickActions action) {
        if (action != null) mMoreClickActions.add(index, action);
    }

    /**
     * Removes a click action from the "more" actions list.
     *
     * @param action click action to remove
     */
    public void removeMoreClickAction(IClickActions action) {
        if (action != null) {
            mMoreClickActions.remove(action);
        }
    }

    /**
     * Sets whether combined message forwarding is enabled.
     *
     * <p>Default is {@code false}. Currently supported only in direct and group channels.
     *
     * @param enable {@code true} to show the combined forwarding option
     */
    public void setEnableSendCombineMessage(boolean enable) {
        enableSendCombineMessage = enable;
    }

    /**
     * Returns whether combined message forwarding is enabled.
     *
     * @return {@code true} if combined forwarding is enabled
     */
    public boolean isEnableSendCombineMessage() {
        return enableSendCombineMessage;
    }

    /**
     * Adds a UI renderer to the channel fragment.
     *
     * @param processor the UI renderer to add
     */
    public void addViewProcessor(IChannelUIRenderer processor) {
        mConversationViewProcessors.add(processor);
    }

    /**
     * @return list of registered channel fragment UI renderers
     */
    public List<IChannelUIRenderer> getViewProcessors() {
        return mConversationViewProcessors;
    }

    /**
     * @param provider message list item provider
     */
    public void addMessageProvider(IMessageProvider provider) {
        if (provider != null) {
            mMessageListProvider.addProvider(provider);
            mConversationSummaryProviders.add(provider);
        }
    }

    /**
     * Replaces an existing message item provider with a new one.
     *
     * @param oldProviderClass class of the provider to replace
     * @param provider new provider instance
     */
    public void replaceMessageProvider(Class oldProviderClass, IMessageProvider provider) {
        mMessageListProvider.replaceProvider(oldProviderClass, provider);
        int index = -1;
        for (int i = 0; i < mConversationSummaryProviders.size(); i++) {
            IChannelSummaryProvider item = mConversationSummaryProviders.get(i);
            if (item.getClass().equals(oldProviderClass)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            mConversationSummaryProviders.set(index, provider);
        }
    }

    /**
     * @return the message item provider manager
     */
    public ProviderManager<UiMessage> getMessageListProvider() {
        return mMessageListProvider;
    }

    /**
     * Returns the display summary spannable for a message content.
     *
     * @param context context
     * @param messageContent message content
     * @return summary spannable
     */
    public Spannable getMessageSummary(Context context, MessageContent messageContent) {
        if (messageContent == null) {
            return new SpannableString("");
        }
        Spannable spannable = null;
        Spannable defaultSpannable =
                defaultMessageProvider.getSummarySpannable(context, messageContent);
        for (IChannelSummaryProvider item : mConversationSummaryProviders) {
            if (item.isSummaryType(messageContent)) {
                try {
                    spannable = item.getSummarySpannable(context, messageContent);
                } catch (Exception e) {
                    RLog.e(TAG, "getMessageSummary error", e);
                    spannable = null;
                }
            }
        }
        return spannable == null ? defaultSpannable : spannable;
    }

    private boolean shouldShowUnknownMessage(MessageContent messageContent) {
        return false;
    }

    /**
     * Returns the display summary spannable for a channel's latest message.
     *
     * @param context context
     * @param channel the channel
     * @return message summary spannable
     */
    public Spannable getMessageSummary(Context context, BaseChannel channel) {
        if (channel == null || channel.getLatestMessage() == null) {
            return new SpannableString("");
        }
        ai.nexconn.chat.message.Message latestMsg = channel.getLatestMessage();
        if (latestMsg.getClientId() == -1) {
            return new SpannableString("");
        }
        MessageContent content = latestMsg.getContent();
        if (content == null || shouldShowUnknownMessage(content)) {
            return new SpannableString("");
        }
        Spannable summary = getMessageSummary(context, content);
        if (content instanceof HDVoiceMessage
                && latestMsg.getDirection() == MessageDirection.RECEIVE
                && latestMsg.getReceivedStatusInfo() != null
                && !latestMsg.getReceivedStatusInfo().isListened()
                && summary != null
                && summary.length() > 0) {
            SpannableString highlighted = new SpannableString(summary);
            highlighted.setSpan(
                    new ForegroundColorSpan(
                            context.getResources().getColor(R.color.nc_unread_message_color)),
                    0,
                    highlighted.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            return highlighted;
        }
        return summary;
    }

    /**
     * Returns whether to show the sender name alongside the message summary.
     *
     * @param messageContent message content
     * @return {@code true} to show the sender name; {@code false} otherwise
     */
    public boolean showSummaryWithName(MessageContent messageContent) {
        if (messageContent == null) {
            return false;
        }
        for (IChannelSummaryProvider item : mConversationSummaryProviders) {
            if (item.isSummaryType(messageContent)) {
                return item.showSummaryWithName();
            }
        }
        return false;
    }

    /**
     * @param showReceiverUserTitle whether to show the user title in direct chats
     */
    public void setShowReceiverUserTitle(boolean showReceiverUserTitle) {
        this.showReceiverUserTitle = showReceiverUserTitle;
    }

    /**
     * Returns whether to show the receiver user title for the given channel type. Only the direct
     * channel type is affected by this setting.
     *
     * @param type channel type
     * @return {@code true} to show the title; {@code false} otherwise
     */
    public boolean isShowReceiverUserTitle(ChannelType type) {
        if (!showReceiverUserTitle) {
            if (type == ChannelType.DIRECT) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return whether to show the "more" option on long-press
     */
    public boolean isShowMoreClickAction() {
        return showMoreClickAction;
    }

    /**
     * @param showMoreClickAction whether to show the "more" option on long-press
     */
    public void setShowMoreClickAction(boolean showMoreClickAction) {
        this.showMoreClickAction = showMoreClickAction;
    }

    /**
     * @return whether the history-divider message is shown
     */
    public boolean isShowHistoryDividerMessage() {
        return showHistoryDividerMessage;
    }

    /**
     * @param showHistoryDividerMessage whether to show the history-divider message
     */
    public void setShowHistoryDividerMessage(boolean showHistoryDividerMessage) {
        this.showHistoryDividerMessage = showHistoryDividerMessage;
    }

    /**
     * @param showNewMessageBar whether to show the new-message unread bubble (direct and group
     *     only)
     */
    public void setShowNewMessageBar(boolean showNewMessageBar) {
        this.showNewMessageBar = showNewMessageBar;
    }

    /**
     * Returns whether to show the new-message unread bubble (direct and group channels only).
     *
     * @param type channel type
     * @return {@code false} for unsupported types; otherwise the configured value
     */
    public boolean isShowNewMessageBar(ChannelType type) {
        if (showNewMessageBar) {
            if (type == ChannelType.DIRECT || type == ChannelType.GROUP) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether to show the unread @mention count badge (group channels only).
     *
     * @param type channel type
     * @return {@code false} for unsupported types; otherwise the configured value
     */
    public boolean isShowNewMentionMessageBar(ChannelType type) {
        if (showNewMentionMessageBar) {
            if (type == ChannelType.GROUP) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param showNewMentionMessageBar whether to show the unread @mention count badge (group only)
     */
    public void setShowNewMentionMessageBar(boolean showNewMentionMessageBar) {
        this.showNewMentionMessageBar = showNewMentionMessageBar;
    }

    /** Returns the maximum number of history messages to show in the channel screen. */
    public int getConversationHistoryMessageCount() {
        return conversationHistoryMessageCount;
    }

    /**
     * Sets the maximum number of history messages to show in the channel screen.
     *
     * @param conversationHistoryMessageCount maximum message count
     */
    public void setConversationHistoryMessageCount(int conversationHistoryMessageCount) {
        this.conversationHistoryMessageCount = conversationHistoryMessageCount;
    }

    /** Returns the maximum number of messages to fetch from the remote server. */
    public int getConversationRemoteMessageCount() {
        return conversationRemoteMessageCount;
    }

    /**
     * Sets the maximum number of messages to fetch from the remote server.
     *
     * @param conversationRemoteMessageCount maximum remote message count
     */
    public void setConversationRemoteMessageCount(int conversationRemoteMessageCount) {
        this.conversationRemoteMessageCount = conversationRemoteMessageCount;
    }

    /** Returns the maximum number of unread messages to display. */
    public int getConversationShowUnreadMessageCount() {
        return conversationShowUnreadMessageCount;
    }

    /**
     * Sets the maximum number of unread messages to display.
     *
     * @param conversationShowUnreadMessageCount maximum unread message count to show
     */
    public void setConversationShowUnreadMessageCount(int conversationShowUnreadMessageCount) {
        this.conversationShowUnreadMessageCount = conversationShowUnreadMessageCount;
    }

    /**
     * @param showHistoryMessageBar whether to show the history unread message bubble (direct and
     *     group only)
     */
    public void setShowHistoryMessageBar(boolean showHistoryMessageBar) {
        this.showHistoryMessageBar = showHistoryMessageBar;
    }

    /**
     * Returns whether to show the history unread message bubble (direct and group channels only).
     *
     * @param type channel type
     * @return {@code false} for unsupported types; otherwise the configured value
     */
    public boolean isShowHistoryMessageBar(ChannelType type) {
        if (showHistoryMessageBar) {
            if (type == ChannelType.DIRECT || type == ChannelType.GROUP) {
                return true;
            }
        }
        return false;
    }

    /** Returns the current channel click listener. */
    public ChannelClickListener getChannelClickListener() {
        return mChannelClickListener;
    }

    /**
     * Sets the click event listener for the channel screen.
     *
     * @param conversationClickListener the {@link ChannelClickListener} to register
     */
    public void setChannelClickListener(ChannelClickListener conversationClickListener) {
        mChannelClickListener = conversationClickListener;
    }

    /**
     * Returns the list of actions shown in the bottom panel when "more" is tapped after
     * long-pressing a message. Modify this list to customize the displayed actions.
     *
     * @return the current list of "more" click actions
     */
    public List<IClickActions> getMoreClickActions() {
        return mMoreClickActions;
    }

    /** Returns the custom ViewModel processor, or {@code null} if not set. */
    public IChannelViewModelProcessor getViewModelProcessor() {
        return mViewModelProcessor;
    }

    /**
     * Sets a custom ViewModel processor to extend channel screen business logic.
     *
     * @param viewModelProcessor the processor to use
     */
    public void setViewModelProcessor(IChannelViewModelProcessor viewModelProcessor) {
        mViewModelProcessor = viewModelProcessor;
    }

    /**
     * Enables or disables read receipts (direct and group channels only).
     *
     * @param enable read receipt toggle
     */
    public void setEnableReadReceipt(boolean enable) {
        mEnableReadReceipt = enable;
    }

    /**
     * Returns whether read receipts are enabled.
     *
     * @return read receipt toggle value
     */
    public boolean isEnableReadReceipt() {
        return mEnableReadReceipt;
    }

    /**
     * Sets the channel types that support read receipts.
     *
     * @param types channel types to enable read receipts for
     */
    public void setSupportReadReceiptConversationType(ChannelType... types) {
        mSupportReadReceiptConversationTypes.clear();
        mSupportReadReceiptConversationTypes.addAll(Arrays.asList(types));
    }

    /** Returns the set of channel types that support read receipts. */
    public Set<ChannelType> getSupportReadReceiptConversationType() {
        return mSupportReadReceiptConversationTypes;
    }

    /**
     * Returns whether to show the read receipt indicator (direct channels only).
     *
     * @param type channel type
     * @return {@code false} for unsupported types; otherwise the configured value
     */
    public boolean isShowReadReceipt(ChannelType type) {
        if (mEnableReadReceipt) {
            if (type == ChannelType.DIRECT) {
                return mSupportReadReceiptConversationTypes.contains(type);
            }
        }
        return false;
    }

    /**
     * Returns whether to show the read receipt request indicator (group channels only).
     *
     * @param type channel type
     * @return {@code false} for unsupported types; otherwise the configured value
     */
    public boolean isShowReadReceiptRequest(ChannelType type) {
        if (mEnableReadReceipt) {
            if (type == ChannelType.GROUP) {
                return mSupportReadReceiptConversationTypes.contains(type);
            }
        }
        return false;
    }

    /**
     * Returns whether multi-device read status sync is enabled. When enabled, messages read on
     * other devices will have their unread count cleared.
     *
     * @param type channel type (direct, group, community, and system channels only)
     * @return whether the feature is enabled for the given type
     */
    public boolean isEnableMultiDeviceSync(ChannelType type) {
        if (enableMultiDeviceSync) {
            if (type == ChannelType.DIRECT
                    || type == ChannelType.GROUP
                    || type == ChannelType.COMMUNITY
                    || type == ChannelType.SYSTEM) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enables or disables multi-device unread count sync.
     *
     * @param enableMultiDeviceSync {@code true} to enable sync; {@code false} to disable
     */
    public void setEnableMultiDeviceSync(boolean enableMultiDeviceSync) {
        this.enableMultiDeviceSync = enableMultiDeviceSync;
    }

    /**
     * Sets the strategy for loading remote messages when entering a channel.
     *
     * @param conversationLoadMessageType the {@link ChannelLoadMessageType} to use
     */
    public void setChannelLoadMessageType(ChannelLoadMessageType conversationLoadMessageType) {
        this.conversationLoadMessageType = conversationLoadMessageType;
    }

    /** Returns the current remote-message loading strategy. */
    public ChannelLoadMessageType getChannelLoadMessageType() {
        return conversationLoadMessageType;
    }

    /**
     * Returns whether remote messages are also deleted on long-press delete.
     *
     * @return {@code true} if remote messages are deleted
     */
    public boolean isNeedDeleteRemoteMessage() {
        return needDeleteRemoteMessage;
    }

    /**
     * Sets whether remote messages are also deleted on long-press delete.
     *
     * @param needDeleteRemoteMessage {@code true} to delete remote messages
     */
    public void setNeedDeleteRemoteMessage(boolean needDeleteRemoteMessage) {
        this.needDeleteRemoteMessage = needDeleteRemoteMessage;
    }

    /**
     * Returns whether to refresh the list when it becomes empty after deleting messages.
     *
     * @return {@code true} to refresh the page
     */
    public boolean isNeedRefreshWhenListIsEmptyAfterDelete() {
        return needRefreshWhenListIsEmptyAfterDelete;
    }

    /**
     * Sets whether to refresh the list when it becomes empty after deleting messages.
     *
     * @param needRefreshWhenListIsEmptyAfterDelete {@code true} to refresh the page
     */
    public void setNeedRefreshWhenListIsEmptyAfterDelete(
            boolean needRefreshWhenListIsEmptyAfterDelete) {
        this.needRefreshWhenListIsEmptyAfterDelete = needRefreshWhenListIsEmptyAfterDelete;
    }

    private HashMap<String, Integer> mFileSuffixTypeMap = new HashMap<>();

    /**
     * Registers icon resource mappings for file message suffixes.
     *
     * @param map keys are file suffixes (e.g. "png", "pdf"); use "default" to override the default
     *     icon. Values are Android drawable resource IDs.
     */
    public void registerFileSuffixTypes(HashMap<String, Integer> map) {
        if (map == null) {
            return;
        }
        this.mFileSuffixTypeMap = map;
    }

    /**
     * Internal use only.
     *
     * @return the registered file-suffix icon map
     */
    public HashMap<String, Integer> getFileSuffixTypes() {
        return mFileSuffixTypeMap;
    }
}
