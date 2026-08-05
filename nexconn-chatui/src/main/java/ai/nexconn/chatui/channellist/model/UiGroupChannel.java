package ai.nexconn.chatui.channellist.model;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.BaseChannel;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.CommunityChannel;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.userinfo.NCUserInfoManager;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;
import ai.nexconn.chatui.utils.log.RLog;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import java.util.HashSet;
import java.util.Set;

public class UiGroupChannel extends BaseUiChannel {
    private String TAG = UiGroupChannel.class.getSimpleName();
    private Set<String> mNicknameIds;
    private SpannableString mPreString;

    public UiGroupChannel(Context context, BaseChannel channel) {
        super(context, channel);
        RLog.d(TAG, "new group conversation.");
        mNicknameIds = new HashSet<>();
        onConversationUpdate(channel);
    }

    @Override
    void buildConversationContent() {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        String preStr;
        String senderName = TextUtils.isEmpty(mSenderUserName) ? "" : mSenderUserName;
        ai.nexconn.chat.message.MessageContent ncContent =
                mCore.getLatestMessage() != null ? mCore.getLatestMessage().getContent() : null;
        boolean isShowName =
                ncContent != null && NCChatUIConfig.channelConfig().showSummaryWithName(ncContent);

        if (mCore.getMentionedCount() > 0) {
            if (mContext != null) {
                preStr = mContext.getString(R.string.nc_conversation_summary_content_mentioned);
                mPreString = new SpannableString(preStr);
                mPreString.setSpan(
                        new ForegroundColorSpan(
                                mContext.getResources().getColor(R.color.nc_warning_color)),
                        0,
                        preStr.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.append(mPreString);
            }
            if (!TextUtils.isEmpty(senderName) && isShowName) {
                builder.append(senderName).append(COLON_SPLIT);
            }
            Spannable messageSummary =
                    NCChatUIConfig.channelConfig().getMessageSummary(mContext, mCore);
            if (!TextUtils.isEmpty(messageSummary)) {
                builder.append(messageSummary);
            }
        } else if (isShowDraftContent()) {
            if (mContext != null) {
                preStr = mContext.getString(R.string.nc_conversation_summary_content_draft);
                mPreString = new SpannableString(preStr);
                mPreString.setSpan(
                        new ForegroundColorSpan(
                                mContext.getResources().getColor(R.color.nc_warning_color)),
                        0,
                        preStr.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.append(mPreString);
            }
            builder.append(getDraftContent());
        } else {
            mPreString = new SpannableString("");
            Spannable messageSummary =
                    NCChatUIConfig.channelConfig().getMessageSummary(mContext, mCore);
            String senderId = getSenderUserId();
            if (senderId != null && senderId.equals(NCEngine.getCurrentUserId())) {
                if (!TextUtils.isEmpty(messageSummary)) {
                    builder.append(messageSummary);
                }
            } else {
                if (!TextUtils.isEmpty(senderName)
                        && !TextUtils.isEmpty(messageSummary)
                        && isShowName) {
                    builder.append(senderName).append(COLON_SPLIT).append(messageSummary);
                } else if (!TextUtils.isEmpty(senderName) && isShowName) {
                    builder.append(senderName);
                } else if (!TextUtils.isEmpty(messageSummary)) {
                    builder.append(messageSummary);
                }
            }
        }
        mConversationContent = builder;
    }

    @Override
    public void onUserInfoUpdate(ai.nexconn.chat.user.model.UserInfo user) {
        if (!TextUtils.isEmpty(getDraftContent()) || user == null) {
            return;
        }
        String senderId = getSenderUserId();
        if (senderId != null && senderId.equals(user.getUserId())) {
            if (!mNicknameIds.contains(user.getUserId())) {
                setSenderUserName(NCUserInfoManager.getInstance().getUserDisplayName(user));
            }
            buildConversationContent();
        }
    }

    @Override
    public void onGroupInfoUpdate(ai.nexconn.chat.channel.model.GroupInfo group) {
        if (group == null) {
            return;
        }
        if (group.getGroupId().equals(mCore.getChannelId())) {
            RLog.d(TAG, "onGroupInfoUpdate. name:" + group.getGroupName());
            setConversationTitle(group.getGroupName());
            setPortraitUrl(group.getPortraitUri());
        }
    }

    @Override
    public void onGroupMemberUpdate(GroupUserInfo groupMember) {
        if (groupMember == null) {
            return;
        }
        if (groupMember.getGroupId().equals(mCore.getChannelId())
                && groupMember.getUserId().equals(getSenderUserId())) {
            mNicknameIds.add(groupMember.getUserId());
            setSenderUserName(groupMember.getNickname());
            buildConversationContent();
        }
    }

    @Override
    public void onConversationUpdate(BaseChannel channel) {
        processResending(channel);
        mCore = channel;
        String groupId = channel.getChannelId();
        if (channel.getChannelType() == ChannelType.COMMUNITY) {
            String subChannelId = "";
            if (channel instanceof CommunityChannel) {
                subChannelId = ((CommunityChannel) channel).getSubChannelId();
                if (subChannelId == null) subChannelId = "";
            }
            groupId = channel.getChannelId() + subChannelId;
        }
        ai.nexconn.chat.channel.model.GroupInfo group =
                NCUserInfoManager.getInstance().getGroupInfo(groupId);
        if (group != null) {
            RLog.d(TAG, "onConversationUpdate. name:" + group.getGroupName());
        } else {
            RLog.d(TAG, "onConversationUpdate. group info is null");
        }
        setConversationTitle(group == null ? "" : group.getGroupName());
        setPortraitUrl(
                group == null || group.getPortraitUri() == null ? "" : group.getPortraitUri());
        String senderUserId = getSenderUserId();
        GroupUserInfo groupUserInfo =
                NCUserInfoManager.getInstance().getGroupUserInfo(groupId, senderUserId);
        ai.nexconn.chat.user.model.UserInfo userInfo = getUserInfo(senderUserId, mCore);
        if (groupUserInfo != null) {
            mNicknameIds.add(groupUserInfo.getUserId());
            setSenderUserName(
                    NCUserInfoManager.getInstance()
                            .getUserDisplayName(userInfo, groupUserInfo.getNickname()));
        } else {
            if (userInfo != null) {
                setSenderUserName(NCUserInfoManager.getInstance().getUserDisplayName(userInfo));
            }
        }
        buildConversationContent();
    }
}
