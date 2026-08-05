package ai.nexconn.chatui.channellist.model;

import ai.nexconn.chat.channel.BaseChannel;
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
import androidx.annotation.NonNull;

public class UiDirectChannel extends BaseUiChannel {
    private final String TAG = UiDirectChannel.class.getSimpleName();

    public UiDirectChannel(Context context, BaseChannel channel) {
        super(context, channel);
        onConversationUpdate(channel);
    }

    @Override
    void buildConversationContent() {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        if (isShowDraftContent()) {
            if (mContext != null) {
                String draft = mContext.getString(R.string.nc_conversation_summary_content_draft);
                SpannableString preStr = new SpannableString(draft);
                preStr.setSpan(
                        new ForegroundColorSpan(
                                mContext.getResources().getColor(R.color.nc_warning_color)),
                        0,
                        draft.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.append(preStr);
            }
            builder.append(getDraftContent());
        } else {
            Spannable summary = NCChatUIConfig.channelConfig().getMessageSummary(mContext, mCore);
            if (summary.length() > 0) {
                builder.append(summary);
            }
        }
        mConversationContent = builder;
    }

    @Override
    public String getConversationTitle() {
        if (NCUserInfoManager.getInstance().getDataSourceType()
                        == NCUserInfoManager.DataSourceType.INFO_MANAGEMENT
                && !TextUtils.isEmpty(mConversationTitle)) {
            return mConversationTitle;
        }
        return super.getConversationTitle();
    }

    @Override
    public String getPortraitUrl() {
        if (NCUserInfoManager.getInstance().getDataSourceType()
                        == NCUserInfoManager.DataSourceType.INFO_MANAGEMENT
                && !TextUtils.isEmpty(mPortraitUrl)) {
            return mPortraitUrl;
        }
        return super.getPortraitUrl();
    }

    @Override
    public void onUserInfoUpdate(@NonNull ai.nexconn.chat.user.model.UserInfo user) {
        if (user.getUserId() != null && user.getUserId().equals(mCore.getChannelId())) {
            setConversationTitle(NCUserInfoManager.getInstance().getUserDisplayName(user));
            setPortraitUrl(user.getPortraitUri());
            buildConversationContent();
            RLog.d(TAG, "onUserInfoUpdate. name:" + getConversationTitle());
        }
    }

    @Override
    public void onConversationUpdate(BaseChannel channel) {
        processResending(channel);
        mCore = channel;
        ai.nexconn.chat.user.model.UserInfo user =
                NCUserInfoManager.getInstance().getUserInfo(channel.getChannelId());
        setConversationTitle(
                user == null
                        ? channel.getChannelId()
                        : NCUserInfoManager.getInstance().getUserDisplayName(user));
        setPortraitUrl(user == null || user.getPortraitUri() == null ? "" : user.getPortraitUri());
        buildConversationContent();
    }

    @Override
    public void onGroupInfoUpdate(ai.nexconn.chat.channel.model.GroupInfo groups) {
        // do nothing
    }

    @Override
    public void onGroupMemberUpdate(GroupUserInfo groupMembers) {
        // do nothing
    }
}
