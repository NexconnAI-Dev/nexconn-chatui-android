package ai.nexconn.chatui.config;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.feature.quickreply.IQuickReplyProvider;
import ai.nexconn.chatui.utils.log.RLog;
import android.content.Context;
import android.content.res.Resources;
import android.net.http.SslCertificate;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Feature flag configuration for the ChatUI SDK.
 *
 * <p>Obtain the singleton instance via {@link NCChatUIConfig#featureConfig()}.
 */
public class FeatureConfig {

    private static final String TAG = "FeatureConfig";

    /** Voice message quality setting. */
    public enum VoiceMessageType {
        /** High-quality (wideband) voice message. */
        HighQuality
    }

    // Whether message reference (quote) is enabled; on by default
    private boolean isReferenceEnable;
    private boolean isQuickReplyEnable;
    private boolean isEditMessageEnable;
    private boolean isUserOnlineStatusEnable;
    private IQuickReplyProvider quickReplyProvider;
    private VoiceMessageType voiceMessageType;
    private List<ChannelType> readReceiptSupportTypes;
    private MutableLiveData<Boolean> isQuickReply = new MediatorLiveData<>();
    // AMR_NB voice message bit rate (in bps)
    private int audioNBEncodingBitRate;
    // AMR_WB voice message bit rate (in bps)
    private int audioWBEncodingBitRate;

    private ChatUIImageEngine mChatUIImageEngine;
    private int userCacheMaxCount;
    private int groupCacheMaxCount;
    private int groupMemberCacheMaxCount;

    private boolean preLoadUserCache = true;
    public boolean NC_wipe_out_notification_message = true;
    public boolean NC_set_java_script_enabled = true;
    // Whether to play a sound for new messages while in the foreground (non-channel screen)
    public boolean soundInForeground = true;
    // Whether to vibrate for new messages while in the foreground (non-channel screen)
    private boolean vibrateInForeground = true;

    // Whether to show the default emoji panel
    private SSLInterceptor sSSLInterceptor;
    public String NC_translation_src_language;
    public String NC_translation_target_language;
    public boolean hideEmojiButton = false;
    private ChatUIMediaInterceptor kitMediaInterceptor;
    private boolean showUnknownMessage = true;
    private boolean showUnknownMessageNotification = false;

    public FeatureConfig() {
        isReferenceEnable = true;
        voiceMessageType = VoiceMessageType.HighQuality;
        readReceiptSupportTypes = new ArrayList<>();
        readReceiptSupportTypes.add(ChannelType.DIRECT);
        readReceiptSupportTypes.add(ChannelType.GROUP);
        isQuickReplyEnable = false;
        audioNBEncodingBitRate = 7950;
        audioWBEncodingBitRate = 12650;
        mChatUIImageEngine = new GlideChatUIImageEngine();
        userCacheMaxCount = 500;
        groupCacheMaxCount = 200;
        groupMemberCacheMaxCount = 500;
        NC_translation_src_language = "zh_CN";
        NC_translation_target_language = "en";
        isEditMessageEnable = false;
        isUserOnlineStatusEnable = false;
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
                NC_wipe_out_notification_message =
                        resources.getBoolean(R.bool.nc_wipe_out_notification_message);
            } catch (Exception e) {
                RLog.e(TAG, "NC_wipe_out_notification_message not get value", e);
            }
            try {
                NC_set_java_script_enabled =
                        resources.getBoolean(R.bool.nc_set_java_script_enabled);
            } catch (Exception e) {
                RLog.e(TAG, "NC_set_java_script_enabled not get value", e);
            }
            try {
                soundInForeground = resources.getBoolean(R.bool.nc_sound_in_foreground);
            } catch (Exception e) {
                RLog.e(TAG, "NC_sound_in_foreground not get value", e);
            }

            try {
                vibrateInForeground = resources.getBoolean(R.bool.nc_vibrate_in_foreground);
            } catch (Exception e) {
                RLog.e(TAG, "NC_vibrate_in_foreground not get value", e);
            }

            try {
                NC_translation_src_language =
                        resources.getString(R.string.nc_translation_src_language);
            } catch (Exception e) {
                RLog.e(TAG, "NC_translation_src_language not get value", e);
            }

            try {
                NC_translation_target_language =
                        resources.getString(R.string.nc_translation_target_language);
            } catch (Exception e) {
                RLog.e(TAG, "NC_translation_target_language not get value", e);
            }
        }
    }

    /** Returns whether the message reference (quote) feature is enabled. */
    public boolean isReferenceEnable() {
        return isReferenceEnable;
    }

    /** Returns whether the quick-reply feature is enabled. */
    public boolean isQuickReplyEnable() {
        return isQuickReplyEnable;
    }

    /** Returns an observable for the quick-reply enabled state. */
    public boolean isQuickReplyType() {
        return isQuickReplyEnable;
    }

    /**
     * Sets the AMR-NB voice encoding bit rate (bps).
     *
     * @param audioNBEncodingBitRate bit rate in bps
     */
    public void setAudioNBEncodingBitRate(int audioNBEncodingBitRate) {
        this.audioNBEncodingBitRate = audioNBEncodingBitRate;
    }

    /**
     * Sets the AMR-WB voice encoding bit rate (bps).
     *
     * @param audioWBEncodingBitRate bit rate in bps
     */
    public void setAudioWBEncodingBitRate(int audioWBEncodingBitRate) {
        this.audioWBEncodingBitRate = audioWBEncodingBitRate;
    }

    /**
     * Sets a custom image engine.
     *
     * @param engine the engine to use, replacing the default {@link GlideChatUIImageEngine}
     */
    public void setChatUIImageEngine(ChatUIImageEngine engine) {
        if (engine != null) this.mChatUIImageEngine = engine;
    }

    /**
     * Sets the voice message type.
     *
     * @param type voice quality type
     */
    // TODO Remove this method; only HighQuality is supported
    public void setVoiceMessageType(VoiceMessageType type) {
        this.voiceMessageType = type;
    }

    /** Returns the AMR-NB voice encoding bit rate (bps). */
    public int getAudioNBEncodingBitRate() {
        return audioNBEncodingBitRate;
    }

    /** Returns the active image engine. */
    public ChatUIImageEngine getChatUIImageEngine() {
        return mChatUIImageEngine;
    }

    /** Returns the AMR-WB voice encoding bit rate (bps). */
    public int getAudioWBEncodingBitRate() {
        return audioWBEncodingBitRate;
    }

    /** Returns the quick-reply content provider, or {@code null} if not set. */
    public IQuickReplyProvider getQuickReplyProvider() {
        return quickReplyProvider;
    }

    /** Returns a LiveData that tracks whether quick-reply is enabled. */
    public MutableLiveData<Boolean> getIsQuickReply() {
        return isQuickReply;
    }

    /**
     * Returns whether read receipts are enabled for the given channel type.
     *
     * @param type channel type
     * @return {@code true} if read receipts are enabled
     */
    public boolean isReadReceiptConversationType(ChannelType type) {
        if (readReceiptSupportTypes != null) {
            return readReceiptSupportTypes.contains(type);
        }
        return false;
    }

    /** Returns the current voice message type. */
    public VoiceMessageType getVoiceMessageType() {
        return voiceMessageType;
    }

    /**
     * Enables read receipts for the specified channel types.
     *
     * @param supportedTypes channel types to enable
     */
    public void enableReadReceipt(ChannelType... supportedTypes) {
        if (supportedTypes != null) {
            readReceiptSupportTypes.clear();
            readReceiptSupportTypes.addAll(Arrays.asList(supportedTypes));
        }
    }

    /**
     * Enables or disables the message reference (quote) feature.
     *
     * @param value {@code true} to enable
     */
    public void enableReference(Boolean value) {
        isReferenceEnable = value;
    }

    /**
     * Enables the quick-reply feature. Must be called before {@code NCChatUI.initialize}.
     *
     * @param provider content provider for quick-reply phrases
     */
    public void enableQuickReply(IQuickReplyProvider provider) {
        isQuickReplyEnable = true;
        quickReplyProvider = provider;
        isQuickReply.setValue(true);
    }

    /**
     * @return maximum number of cached user info entries
     */
    public int getUserCacheMaxCount() {
        return userCacheMaxCount;
    }

    /**
     * @param userCacheMaxCount maximum user info cache size; effective only before SDK
     *     initialization
     */
    public void setUserCacheMaxCount(int userCacheMaxCount) {
        this.userCacheMaxCount = userCacheMaxCount;
    }

    /**
     * @return maximum number of cached group info entries
     */
    public int getGroupCacheMaxCount() {
        return groupCacheMaxCount;
    }

    /**
     * @param groupCacheMaxCount maximum group info cache size; effective only before SDK
     *     initialization
     */
    public void setGroupCacheMaxCount(int groupCacheMaxCount) {
        this.groupCacheMaxCount = groupCacheMaxCount;
    }

    /**
     * @return maximum number of cached group member info entries
     */
    public int getGroupMemberCacheMaxCount() {
        return groupMemberCacheMaxCount;
    }

    /**
     * @param groupMemberCacheMaxCount maximum group member cache size; effective only before SDK
     *     initialization
     */
    public void setGroupMemberCacheMaxCount(int groupMemberCacheMaxCount) {
        this.groupMemberCacheMaxCount = groupMemberCacheMaxCount;
    }

    /**
     * @return whether user cache is pre-loaded
     */
    public boolean isPreLoadUserCache() {
        return preLoadUserCache;
    }

    /**
     * @param preLoadUserCache whether to pre-load the user cache
     */
    public void setPreLoadUserCache(boolean preLoadUserCache) {
        this.preLoadUserCache = preLoadUserCache;
    }

    /**
     * Returns whether to vibrate on new messages while in the foreground.
     *
     * @return vibrate-on-message setting
     */
    public boolean isVibrateInForeground() {
        return vibrateInForeground;
    }

    /**
     * Sets whether to vibrate on new messages while in the foreground.
     *
     * @param vibrateInForeground whether to vibrate
     */
    public void setVibrateInForeground(boolean vibrateInForeground) {
        this.vibrateInForeground = vibrateInForeground;
    }

    /**
     * Returns whether to play a sound on new messages while in the foreground.
     *
     * @return sound-on-message setting
     */
    public boolean isSoundInForeground() {
        return soundInForeground;
    }

    /**
     * Sets whether to play a sound on new messages while in the foreground.
     *
     * @param soundInForeground whether to play sound
     */
    public void setSoundInForeground(boolean soundInForeground) {
        this.soundInForeground = soundInForeground;
    }

    /** Returns the SSL interceptor, or {@code null} if not set. */
    public SSLInterceptor getSSLInterceptor() {
        return sSSLInterceptor;
    }

    /**
     * Returns whether the emoji button is hidden.
     *
     * @return {@code true} if the emoji button is hidden
     */
    public boolean isHideEmojiButton() {
        return hideEmojiButton;
    }

    /**
     * Shows or hides the emoji button in the input bar.
     *
     * @param hideEmojiButton {@code true} to hide
     */
    public void setHideEmojiButton(boolean hideEmojiButton) {
        this.hideEmojiButton = hideEmojiButton;
    }

    /**
     * Returns whether AAC encoding is forced. Since 5.4.1, AAC is always used instead of HE_AAC.
     *
     * @return always {@code true}
     */
    @Deprecated
    public boolean isForceUseAAC() {
        return true;
    }

    /**
     * No-op since 5.4.1. AAC encoding is always forced.
     *
     * @param forceUseAAC ignored
     */
    @Deprecated
    public void setForceUseAAC(boolean forceUseAAC) {
        // do nothing
    }

    /**
     * @param sSSLInterceptor SSL certificate interceptor for CombineWebViewActivity
     */
    public void setSSLInterceptor(SSLInterceptor sSSLInterceptor) {
        this.sSSLInterceptor = sSSLInterceptor;
    }

    /**
     * Interceptor for SSL certificate verification.
     *
     * <p>Register an implementation via {@link #setSSLInterceptor} to customize certificate pinning
     * or accept self-signed certificates.
     */
    public interface SSLInterceptor {
        /**
         * Called to check whether the given SSL certificate should be trusted.
         *
         * @param sslCertificate the SSL certificate to check
         * @return {@code true} to trust the certificate; {@code false} to reject it
         */
        boolean check(SslCertificate sslCertificate);
    }

    /** Returns the media interceptor, or {@code null} if not set. */
    public ChatUIMediaInterceptor getChatUIMediaInterceptor() {
        return kitMediaInterceptor;
    }

    /**
     * Sets a media interceptor for customizing Glide and WebView loading.
     *
     * @param kitMediaInterceptor the interceptor to use
     */
    public void setChatUIMediaInterceptor(ChatUIMediaInterceptor kitMediaInterceptor) {
        this.kitMediaInterceptor = kitMediaInterceptor;
    }

    /** Returns whether unknown message types should be displayed with a fallback view. */
    public boolean isShowUnknownMessage() {
        return showUnknownMessage;
    }

    /**
     * Sets whether unknown message types should be displayed with a fallback view.
     *
     * @param showUnknownMessage {@code true} to show; {@code false} to hide
     */
    public void setShowUnknownMessage(boolean showUnknownMessage) {
        this.showUnknownMessage = showUnknownMessage;
    }

    /** Returns whether an unknown message type should generate a notification. */
    public boolean isShowUnknownMessageNotification() {
        return showUnknownMessageNotification;
    }

    /**
     * Sets whether an unknown message type should generate a notification.
     *
     * @param showUnknownMessageNotification {@code true} to enable
     */
    public void setShowUnknownMessageNotification(boolean showUnknownMessageNotification) {
        this.showUnknownMessageNotification = showUnknownMessageNotification;
    }

    /** Enables or disables message editing on long-press. */
    public void enableEditMessage(boolean value) {
        isEditMessageEnable = value;
    }

    /** Returns whether message editing on long-press is enabled. */
    public boolean isEditMessageEnable() {
        return isEditMessageEnable;
    }

    /**
     * Enables or disables user online status display.
     *
     * @since 5.32.0
     */
    public void enableUserOnlineStatus(boolean value) {
        isUserOnlineStatusEnable = value;
    }

    /**
     * Returns whether user online status display is enabled.
     *
     * @since 5.32.0
     */
    public boolean isUserOnlineStatusEnable() {
        return isUserOnlineStatusEnable;
    }
}
