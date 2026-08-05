package ai.nexconn.chatui.model;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.model.MessageDirection;
import ai.nexconn.chat.message.model.MessageReadReceiptResponse;
import ai.nexconn.chat.message.model.MessageReceivedStatusInfo;
import ai.nexconn.chat.message.model.ReadReceiptInfo;
import ai.nexconn.chat.message.model.SentStatus;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chat.user.model.UserType;
import ai.nexconn.chatui.userinfo.NCUserInfoManager;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;
import ai.nexconn.chatui.utils.log.RLog;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import java.util.Map;

public class UiMessage extends UiBaseBean {
    private final String TAG = UiMessage.class.getSimpleName();
    private Message message;
    private UserInfo userInfo;
    private @State.Value int state;
    private int progress;
    private String destructTime;
    private boolean isPlaying;
    private boolean isEdit;
    private boolean isSelected;
    private String nickname;

    /** Spannable content for TextMessage and ReferenceMessage */
    private SpannableStringBuilder contentSpannable;

    /** Spannable content for ReferenceMessage when referMsg is TextMessage */
    private SpannableStringBuilder referenceContentSpannable;

    /** Translated text content */
    private String translatedContent;

    private @State.Value int translateStatus = State.NORMAL;

    /** Business state */
    private String businessState;

    /** Read receipt info */
    private ReadReceiptInfo readReceiptInfo;

    public UiMessage(Message message) {
        setMessage(message);
        initUserInfo();
        change();
    }

    public void initUserInfo() {
        if (TextUtils.isEmpty(message.getSenderUserId())) {
            if (message.getDirection() == MessageDirection.SEND) {
                message.setSenderUserId(NCEngine.getCurrentUserId());
            } else {
                RLog.e(TAG, "Invalid message with empty senderUserId!");
                return;
            }
        }

        UserInfo user;
        boolean isInfoManagement =
                NCUserInfoManager.getInstance().getDataSourceType()
                        == NCUserInfoManager.DataSourceType.INFO_MANAGEMENT;
        if (isInfoManagement
                && message.getContent() != null
                && message.getContent().getSenderUserInfo() != null
                && message.getContent().getSenderUserInfo().getUserId() != null
                && message.getContent()
                        .getSenderUserInfo()
                        .getUserId()
                        .equals(message.getSenderUserId())) {
            user = message.getContent().getSenderUserInfo();
        } else {
            user = NCUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
        }
        if (user != null) {
            userInfo = user;
            if (userInfo.getName() == null) {
                userInfo.setName(message.getSenderUserId());
            }
        } else {
            userInfo =
                    new UserInfo(
                            message.getSenderUserId(),
                            UserType.NORMAL,
                            message.getSenderUserId());
        }
        if (message.getChannelIdentifier().getChannelType() == ChannelType.GROUP) {
            GroupUserInfo groupUserInfo =
                    NCUserInfoManager.getInstance()
                            .getGroupUserInfo(
                                    message.getChannelIdentifier().getChannelId(),
                                    message.getSenderUserId());
            if (groupUserInfo != null && !TextUtils.isEmpty(groupUserInfo.getNickname())) {
                nickname = groupUserInfo.getNickname();
            }
        }
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
        switch (message.getSentStatus()) {
            case SENDING:
                state = State.PROGRESS;
                break;
            case FAILED:
                state = State.ERROR;
                break;
            case CANCELED:
                state = State.CANCEL;
                break;
        }
        change();
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
        change();
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
        change();
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        change();
    }

    public String getDestructTime() {
        return destructTime;
    }

    public void setDestructTime(String destructTime) {
        this.destructTime = destructTime;
        change();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        change();
    }

    public boolean isEdit() {
        return isEdit;
    }

    public void setEdit(boolean edit) {
        this.isEdit = edit;
        change();
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        change();
    }

    public void onUserInfoUpdate(UserInfo user) {
        if (user.getUserId().equals(message.getSenderUserId())) {
            if (user.getName() != null) {
                userInfo.setName(user.getName());
            } else {
                userInfo.setName("");
            }
            userInfo.setAlias(user.getAlias());
            userInfo.setPortraitUri(user.getPortraitUri());
            userInfo.setExtra(user.getExtra());
            change();
        }
    }

    public void onGroupMemberInfoUpdate(GroupUserInfo member) {
        if (member.getUserId().equals(message.getSenderUserId())) {
            nickname = member.getNickname();
            change();
        }
    }

    public int getClientId() {
        return message != null ? message.getClientId() : -1;
    }

    public String getMessageId() {
        return message != null ? message.getMessageId() : null;
    }

    public ChannelType getChannelType() {
        return message != null ? message.getChannelIdentifier().getChannelType() : null;
    }

    public String getChannelId() {
        return message != null ? message.getChannelIdentifier().getChannelId() : null;
    }

    public ChannelIdentifier getChannelIdentifier() {
        return message != null ? message.getChannelIdentifier() : null;
    }

    public MessageDirection getDirection() {
        return message != null ? message.getDirection() : null;
    }

    public void setDirection(MessageDirection direction) {
        if (message == null) {
            return;
        }
        message.setDirection(direction);
        change();
    }

    public MessageReceivedStatusInfo getReceivedStatusInfo() {
        return message != null ? message.getReceivedStatusInfo() : null;
    }

    public void setReceivedStatusInfo(MessageReceivedStatusInfo statusInfo) {
        if (message == null) {
            return;
        }
        message.setReceivedStatusInfo(statusInfo);
        change();
    }

    public SentStatus getSentStatus() {
        return message != null ? message.getSentStatus() : null;
    }

    public void setSentStatus(SentStatus sentStatus) {
        if (message == null) {
            return;
        }
        message.setSentStatus(sentStatus);
        change();
    }

    public long getSentTime() {
        return message != null ? message.getSentTime() : 0;
    }

    public void setSentTime(long sentTime) {
        if (message == null) {
            return;
        }
        message.setSentTime(sentTime);
        change();
    }

    public String getMessageType() {
        return message != null ? message.getMessageType() : null;
    }

    public MessageContent getContent() {
        return message != null ? message.getContent() : null;
    }

    public void setContent(MessageContent content) {
        if (message == null) {
            return;
        }
        message.setContent(content);
        change();
    }

    public String getExtra() {
        if (message != null && message.getContent() != null) {
            return message.getContent().getExtra();
        }
        return null;
    }

    public String getSenderUserId() {
        return message != null ? message.getSenderUserId() : null;
    }

    public void setSenderUserId(String senderUserId) {
        if (message == null) {
            return;
        }
        message.setSenderUserId(senderUserId);
        change();
    }

    public ReadReceiptInfo getReadReceiptInfo() {
        return readReceiptInfo;
    }

    public void setReadReceiptInfo(ReadReceiptInfo readReceiptInfo) {
        this.readReceiptInfo = readReceiptInfo;
        change();
    }

    public void setReadReceiptInfo(MessageReadReceiptResponse response) {
        if (response == null
                || response.getChannelIdentifier() == null
                || TextUtils.isEmpty(response.getMessageId())) {
            return;
        }
        ReadReceiptInfo info =
                new ReadReceiptInfo(
                        response.getChannelIdentifier().getChannelType(),
                        response.getChannelIdentifier().getChannelId(),
                        response.getMessageId(),
                        response.getUnreadCount(),
                        response.getReadCount(),
                        response.getTotalCount());
        this.readReceiptInfo = info;
        change();
    }

    public boolean isNeedReceipt() {
        return message != null && message.getNeedReceipt();
    }

    public Map<String, String> getMetadata() {
        return message != null ? message.getMetadata() : null;
    }

    public SpannableStringBuilder getContentSpannable() {
        return contentSpannable;
    }

    public void setContentSpannable(SpannableStringBuilder contentSpannable) {
        this.contentSpannable = contentSpannable;
    }

    public SpannableStringBuilder getReferenceContentSpannable() {
        return referenceContentSpannable;
    }

    public void setReferenceContentSpannable(SpannableStringBuilder referenceContentSpannable) {
        this.referenceContentSpannable = referenceContentSpannable;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getDisplayName() {
        return NCUserInfoManager.getInstance().getUserDisplayName(userInfo, nickname);
    }

    public String getTranslatedContent() {
        return translatedContent;
    }

    public void setTranslatedContent(String translatedContent) {
        this.translatedContent = translatedContent;
    }

    @State.Value
    public int getTranslateStatus() {
        return translateStatus;
    }

    public void setTranslateStatus(@State.Value int translateStatus) {
        this.translateStatus = translateStatus;
    }

    public String getBusinessState() {
        return businessState;
    }

    public void setBusinessState(String businessState) {
        this.businessState = businessState;
    }
}
