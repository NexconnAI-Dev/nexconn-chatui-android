package ai.nexconn.chatui.channel.feature.reference;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.FileMessage;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.userinfo.NCUserInfoManager;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;
import ai.nexconn.chatui.utils.text.StringUtils;
import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/** Reference UI component. */
@SuppressLint("ViewConstructor")
public class ReferenceView extends FrameLayout {
    private Context mContext;
    private View mReferenceView;
    private TextView mReferenceSenderName;
    private TextView mReferenceContent;
    private ReferenceCancelListener mCancelListener;

    public ReferenceView(Context context, ViewGroup parent, UiMessage message) {
        super(context);
        mContext = context;
        initView(context, parent);
        initData(message);
    }

    private void initView(Context context, ViewGroup parent) {
        mReferenceView =
                LayoutInflater.from(context)
                        .inflate(R.layout.nc_reference_ext_attach_view, parent, false);

        ImageView cancelButton = mReferenceView.findViewById(R.id.nc_reference_cancel);
        mReferenceSenderName = mReferenceView.findViewById(R.id.nc_reference_sender_name);
        mReferenceContent = mReferenceView.findViewById(R.id.nc_reference_content);
        cancelButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mCancelListener != null) {
                            mCancelListener.onCanceled();
                        }
                    }
                });
    }

    private void initData(UiMessage uiMessage) {
        Message message = uiMessage.getMessage();
        if (message != null) {
            Spannable content =
                    NCChatUIConfig.channelConfig()
                            .getMessageSummary(mContext, message.getContent());
            MessageContent messageContent = message.getContent();
            SpannableStringBuilder ssb;
            if (messageContent instanceof FileMessage) {
                ssb = new SpannableStringBuilder(content);
            } else {
                ssb = new SpannableStringBuilder(StringUtils.getStringNoBlank(content.toString()));
            }
            mReferenceSenderName.setText(getDisplayName(uiMessage));
            mReferenceContent.setText(ssb);
        }
    }

    public View getReferenceView() {
        return mReferenceView;
    }

    public void setReferenceCancelListener(ReferenceCancelListener referenceCancelListener) {
        this.mCancelListener = referenceCancelListener;
    }

    private String getDisplayName(UiMessage uiMessage) {
        String groupMemberName = "";
        if (uiMessage.getMessage().getChannelIdentifier().getChannelType() == ChannelType.GROUP) {
            GroupUserInfo groupUserInfo =
                    NCUserInfoManager.getInstance()
                            .getGroupUserInfo(
                                    uiMessage.getMessage().getChannelIdentifier().getChannelId(),
                                    uiMessage.getMessage().getSenderUserId());
            groupMemberName = groupUserInfo != null ? groupUserInfo.getNickname() : "";
        }

        UserInfo userInfo =
                getUserInfo(uiMessage.getMessage().getSenderUserId(), uiMessage.getContent());
        return NCUserInfoManager.getInstance().getUserDisplayName(userInfo, groupMemberName) + "：";
    }

    private UserInfo getUserInfo(String userId, MessageContent messageContent) {
        boolean isInfoManagement =
                NCUserInfoManager.getInstance().getDataSourceType()
                        == NCUserInfoManager.DataSourceType.INFO_MANAGEMENT;
        if (isInfoManagement
                && messageContent != null
                && messageContent.getSenderUserInfo() != null
                && messageContent.getSenderUserInfo().getUserId() != null
                && messageContent.getSenderUserInfo().getUserId().equals(userId)) {
            return messageContent.getSenderUserInfo();
        }
        return NCUserInfoManager.getInstance().getUserInfo(userId);
    }

    public interface ReferenceCancelListener {
        void onCanceled();
    }
}
