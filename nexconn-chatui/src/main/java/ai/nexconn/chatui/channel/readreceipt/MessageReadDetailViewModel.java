package ai.nexconn.chatui.channel.readreceipt;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.channel.model.GroupMemberInfo;
import ai.nexconn.chat.message.model.ReadReceiptInfo;
import ai.nexconn.chat.message.model.ReadReceiptUser;
import ai.nexconn.chatui.base.BaseViewModel;
import ai.nexconn.chatui.base.interfaces.OnDataChangeEnhancedListener;
import ai.nexconn.chatui.handler.ReadReceiptDetailHandler;
import ai.nexconn.chatui.model.ReadReceiptData;
import ai.nexconn.chatui.userinfo.NCUserInfoManager;
import ai.nexconn.chatui.userinfo.model.ExtendedGroupUserInfo;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;
import ai.nexconn.chatui.utils.constant.ChatUIConstants;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for the group message read-status detail page.
 *
 * @since 5.30.0
 */
public class MessageReadDetailViewModel extends BaseViewModel
        implements NCUserInfoManager.UserDataObserver {
    private static final String TAG = "MessageReadDetailVM";

    private final String messageId;
    private final String channelId;
    private final ChannelIdentifier channelIdentifier;
    private final String senderUserId;
    private final long sentTime;
    private ReadReceiptInfo readReceiptInfo;
    private final ReadReceiptDetailHandler readReceiptDetailHandler;
    private boolean isInfoProvider = false;
    // LiveData
    private final MutableLiveData<ReadReceiptInfo> readReceiptInfoLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<List<ReadReceiptData>> readUsersLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<List<ReadReceiptData>> unreadUsersLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<GroupMemberInfo> memberInfoUpdateLiveData =
            new MutableLiveData<>();

    public MessageReadDetailViewModel(@NonNull Bundle arguments) {
        super(arguments);
        messageId = arguments.getString(ChatUIConstants.KEY_RECEIPT_MESSAGE_ID);
        String argChannelId = arguments.getString(ChatUIConstants.KEY_RECEIPT_CHANNEL_ID);
        channelId = argChannelId != null ? argChannelId : "";
        int channelTypeValue = arguments.getInt(ChatUIConstants.KEY_RECEIPT_CHANNEL_TYPE, 1);
        ChannelType channelType = ChannelType.fromValue(channelTypeValue);
        if (channelType == null) {
            channelType = ChannelType.DIRECT;
        }
        channelIdentifier = new ChannelIdentifier(channelType, channelId);
        senderUserId = arguments.getString(ChatUIConstants.KEY_RECEIPT_SENDER_USER_ID);
        sentTime = arguments.getLong(ChatUIConstants.KEY_RECEIPT_SENT_TIME, 0L);

        int readCount = arguments.getInt(ChatUIConstants.KEY_RECEIPT_READ_COUNT, -1);
        if (readCount >= 0) {
            readReceiptInfo =
                    new ReadReceiptInfo(
                            channelType,
                            channelId,
                            messageId != null ? messageId : "",
                            arguments.getInt(ChatUIConstants.KEY_RECEIPT_UNREAD_COUNT, 0),
                            readCount,
                            arguments.getInt(ChatUIConstants.KEY_RECEIPT_TOTAL_COUNT, 0));
        }

        NCUserInfoManager.getInstance().addUserDataObserver(this);
        isInfoProvider =
                NCUserInfoManager.getInstance().getDataSourceType()
                        == NCUserInfoManager.DataSourceType.INFO_PROVIDER;
        readReceiptDetailHandler = new ReadReceiptDetailHandler();
        readReceiptDetailHandler.addDataChangeListener(
                ReadReceiptDetailHandler.KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5,
                new OnDataChangeEnhancedListener<ReadReceiptInfo>() {
                    @Override
                    public void onDataChange(ReadReceiptInfo value) {
                        readReceiptInfo = value;
                        readReceiptInfoLiveData.postValue(value);
                    }
                });
        readReceiptDetailHandler.addDataChangeListener(
                ReadReceiptDetailHandler.KEY_MESSAGE_READ_V5_USER_LIST,
                (OnDataChangeEnhancedListener<List<ReadReceiptUser>>)
                        result -> readUsersLiveData.postValue(convertGroupMemberInfos(result)));
        readReceiptDetailHandler.addDataChangeListener(
                ReadReceiptDetailHandler.KEY_MESSAGE_UNREAD_V5_USER_LIST,
                (OnDataChangeEnhancedListener<List<ReadReceiptUser>>)
                        result -> unreadUsersLiveData.postValue(convertGroupMemberInfos(result)));
        if (readReceiptInfo != null) {
            readReceiptInfoLiveData.setValue(readReceiptInfo);
        } else if (!TextUtils.isEmpty(messageId) && !TextUtils.isEmpty(channelId)) {
            readReceiptDetailHandler.getMessageReadReceiptInfoV5(channelIdentifier, messageId);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        NCUserInfoManager.getInstance().removeUserDataObserver(this);
        readReceiptDetailHandler.stop();
    }

    public MutableLiveData<ReadReceiptInfo> getReadReceiptInfoV5LiveData() {
        return readReceiptInfoLiveData;
    }

    public MutableLiveData<List<ReadReceiptData>> getReadUsersLiveData() {
        return readUsersLiveData;
    }

    public MutableLiveData<List<ReadReceiptData>> getUnreadUsersLiveData() {
        return unreadUsersLiveData;
    }

    public MutableLiveData<GroupMemberInfo> getMemberInfoUpdateLiveData() {
        return memberInfoUpdateLiveData;
    }

    public ReadReceiptInfo getReadReceiptInfoV5() {
        return readReceiptInfo;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getSenderUserId() {
        return senderUserId;
    }

    public long getSentTime() {
        return sentTime;
    }

    public void getMessagesReadReceiptUsersByPage(boolean isRead) {
        if (isRead)
            readReceiptDetailHandler.getMessagesReadUsersByPage(channelIdentifier, messageId);
        else readReceiptDetailHandler.getMessagesUnReadUsersByPage(channelIdentifier, messageId);
    }

    @Override
    public void onUserUpdate(ai.nexconn.chat.user.model.UserInfo info) {}

    @Override
    public void onGroupUpdate(ai.nexconn.chat.channel.model.GroupInfo group) {}

    @Override
    public void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo) {
        memberInfoUpdateLiveData.setValue(convertGroupMemberInfo(groupUserInfo.getUserId()));
    }

    private List<ReadReceiptData> convertGroupMemberInfos(List<ReadReceiptUser> users) {
        List<ReadReceiptData> readReceiptDataList = new ArrayList<>();
        if (users.isEmpty()) {
            return readReceiptDataList;
        }
        for (int i = 0; i < users.size(); i++) {
            ReadReceiptUser user = users.get(i);
            GroupMemberInfo info = convertGroupMemberInfo(user.getUserId());
            readReceiptDataList.add(new ReadReceiptData(user, info));
        }
        return readReceiptDataList;
    }

    private GroupMemberInfo convertGroupMemberInfo(String userId) {
        GroupUserInfo info = NCUserInfoManager.getInstance().getGroupUserInfo(channelId, userId);
        if (info != null && !isInfoProvider) {
            ExtendedGroupUserInfo extendedGroupUserInfo = (ExtendedGroupUserInfo) info;
            GroupMemberInfo groupMemberInfo = extendedGroupUserInfo.getGroupMemberInfo();
            if (groupMemberInfo != null && !TextUtils.isEmpty(groupMemberInfo.getUserId())) {
                return groupMemberInfo;
            }
        }
        return createGroupMemberInfoWithUserInfo(userId);
    }

    private GroupMemberInfo createGroupMemberInfoWithUserInfo(String userId) {
        ai.nexconn.chat.user.model.UserInfo userInfo =
                NCUserInfoManager.getInstance().getUserInfo(userId);
        String name = null;
        String portraitUri = null;
        if (userInfo != null) {
            name = userInfo.getName();
            portraitUri = userInfo.getPortraitUri();
        }
        return new GroupMemberInfo(userId, name, portraitUri, null, null, null, 0, false);
    }
}
