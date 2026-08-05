package ai.nexconn.chatui.channellist.model;

import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.DataManagementInfo;
import ai.nexconn.chat.channel.model.EditedMessageDraft;
import ai.nexconn.chat.message.model.ReadReceiptInfo;
import ai.nexconn.chat.message.model.SentStatus;
import ai.nexconn.chat.user.model.UserOnlineStatus;
import ai.nexconn.chatui.channel.feature.resend.ResendManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.userinfo.NCUserInfoManager;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;
import ai.nexconn.chatui.utils.log.RLog;
import android.content.Context;
import android.text.Spannable;
import android.text.TextUtils;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class BaseUiChannel {
    private final String TAG = this.getClass().getSimpleName();
    protected final String COLON_SPLIT = ": ";
    Context mContext;
    public BaseChannel mCore;
    public Spannable mConversationContent;

    protected String mConversationTitle;
    protected String mPortraitUrl;
    protected String mSenderUserName;

    private ReadReceiptInfo readReceiptInfo;

    private UserOnlineStatus onlineStatus;

    public BaseUiChannel(Context context, BaseChannel channel) {
        if (context == null || channel == null) {
            RLog.e(TAG, "Context or channel can't be null.");
            return;
        }
        mContext = context;
        mCore = channel;
    }

    public String getConversationTitle() {
        DataManagementInfo dataMgmt = mCore != null ? mCore.getDataManagementInfo() : null;
        if (dataMgmt != null && !TextUtils.isEmpty(dataMgmt.getName())) {
            return dataMgmt.getName();
        }
        return mConversationTitle;
    }

    public void setConversationTitle(String title) {
        mConversationTitle = title;
    }

    public String getPortraitUrl() {
        DataManagementInfo dataMgmt = mCore != null ? mCore.getDataManagementInfo() : null;
        if (dataMgmt != null && !TextUtils.isEmpty(dataMgmt.getPortraitUri())) {
            return dataMgmt.getPortraitUri();
        }
        return mPortraitUrl;
    }

    public void setPortraitUrl(String url) {
        mPortraitUrl = url;
    }

    public String getSenderUserName() {
        return mSenderUserName;
    }

    public void setSenderUserName(String name) {
        mSenderUserName = name;
    }

    public String getSenderUserId() {
        if (mCore != null && mCore.getLatestMessage() != null) {
            return mCore.getLatestMessage().getSenderUserId();
        }
        return null;
    }

    public void onDraftUpdate(String draft) {
        buildConversationContent();
    }

    public ChannelIdentifier getConversationIdentifier() {
        if (mCore == null) {
            return null;
        }
        if (TextUtils.isEmpty(mCore.getChannelId()) || mCore.getChannelType() == null) {
            return null;
        }
        return new ChannelIdentifier(mCore.getChannelType(), mCore.getChannelId());
    }

    public String getChannelKey() {
        if (mCore == null) {
            return "";
        }
        ChannelType type =
                mCore.getChannelType() != null ? mCore.getChannelType() : ChannelType.DIRECT;
        String channelId = mCore.getChannelId() != null ? mCore.getChannelId() : "";
        return "type=" + type + "&tid=" + channelId;
    }

    public void processResending(BaseChannel channel) {
        ai.nexconn.chat.message.Message latestMsg = channel.getLatestMessage();
        int clientId = latestMsg != null ? latestMsg.getClientId() : -1;
        if (ResendManager.getInstance().needResend(clientId)) {
            if (latestMsg != null) {
                latestMsg.setSentStatus(SentStatus.SENDING);
            }
        }
    }

    public BaseChannel currentConversation(String targetId) {
        return this.mCore;
    }

    public int getUnreadMessageCount() {
        if (this.mCore == null) {
            return -1;
        }
        return this.mCore.getUnreadCount();
    }

    abstract void buildConversationContent();

    public String getDraft() {
        if (mCore == null || TextUtils.isEmpty(mCore.getDraft())) {
            return "";
        }
        String draftContent = "";
        String draft = mCore.getDraft();
        try {
            JSONObject draftJson = new JSONObject(draft);
            draftContent = draftJson.optString("draftContent", "");
        } catch (JSONException e) {
            draftContent = draft;
        }
        return draftContent == null ? "" : draftContent;
    }

    private String getEditDraft() {
        if (!isEditedMessageDraftValid()) {
            return "";
        }
        EditedMessageDraft draft = mCore.getEditedMessageDraft();
        if (draft == null || TextUtils.isEmpty(draft.getContent())) {
            return "";
        }
        try {
            JSONObject editJson = new JSONObject(draft.getContent());
            String content = editJson.optString("content", "");
            return content != null ? content : "";
        } catch (JSONException e) {
            return draft.getContent() != null ? draft.getContent() : "";
        }
    }

    public String getDraftContent() {
        String editDraft = getEditDraft();
        if (!TextUtils.isEmpty(editDraft)) {
            return editDraft.replace("\n", "");
        }
        return getDraft().replace("\n", "");
    }

    private boolean isEditedMessageDraftValid() {
        return NCChatUIConfig.featureConfig().isEditMessageEnable()
                && mCore != null
                && mCore.getMentionedCount() <= 0
                && mCore.getEditedMessageDraft() != null
                && !TextUtils.isEmpty(mCore.getEditedMessageDraft().getMessageId())
                && !TextUtils.isEmpty(mCore.getEditedMessageDraft().getContent());
    }

    public boolean isShowDraftContent() {
        return isEditedMessageDraftValid() || !TextUtils.isEmpty(getDraft());
    }

    /**
     * User info update
     *
     * @param user nexconn UserInfo
     */
    public abstract void onUserInfoUpdate(ai.nexconn.chat.user.model.UserInfo user);

    /**
     * Group info update
     *
     * @param group nexconn GroupInfo
     */
    public abstract void onGroupInfoUpdate(ai.nexconn.chat.channel.model.GroupInfo group);

    /**
     * Group member update
     *
     * @param groupMember {@link GroupUserInfo}
     */
    public abstract void onGroupMemberUpdate(GroupUserInfo groupMember);

    /**
     * Channel update
     *
     * @param channel {@link BaseChannel}
     */
    public abstract void onConversationUpdate(BaseChannel channel);

    ai.nexconn.chat.user.model.UserInfo getUserInfo(String userId, BaseChannel channel) {
        boolean isInfoManagement =
                NCUserInfoManager.getInstance().getDataSourceType()
                        == NCUserInfoManager.DataSourceType.INFO_MANAGEMENT;
        if (isInfoManagement
                && channel != null
                && channel.getLatestMessage() != null
                && channel.getLatestMessage().getContent() != null
                && channel.getLatestMessage().getContent().getSenderUserInfo() != null) {
            ai.nexconn.chat.user.model.UserInfo ncUser =
                    channel.getLatestMessage().getContent().getSenderUserInfo();
            if (ncUser.getUserId() != null && ncUser.getUserId().equals(userId)) {
                return ncUser;
            }
        }
        return NCUserInfoManager.getInstance().getUserInfo(userId);
    }

    public ReadReceiptInfo getReadReceiptInfo() {
        if (!isReadReceiptMatchedLatestMessage(readReceiptInfo)) {
            // Clear stale receipt cache once latest message has changed.
            readReceiptInfo = null;
            return null;
        }
        return readReceiptInfo;
    }

    public void setReadReceiptInfo(ReadReceiptInfo readReceiptInfo) {
        if (!isReadReceiptMatchedLatestMessage(readReceiptInfo)) {
            this.readReceiptInfo = null;
            return;
        }
        this.readReceiptInfo = readReceiptInfo;
    }

    private boolean isReadReceiptMatchedLatestMessage(ReadReceiptInfo receiptInfo) {
        if (receiptInfo == null || mCore == null || mCore.getLatestMessage() == null) {
            return false;
        }
        String latestMessageId = mCore.getLatestMessage().getMessageId();
        String receiptMessageId = receiptInfo.getMessageId();
        return !TextUtils.isEmpty(latestMessageId) && latestMessageId.equals(receiptMessageId);
    }

    public void setOnlineStatus(UserOnlineStatus onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public UserOnlineStatus getOnlineStatus() {
        return onlineStatus;
    }
}
