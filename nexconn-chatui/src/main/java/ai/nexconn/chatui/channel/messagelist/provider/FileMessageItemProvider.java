package ai.nexconn.chatui.channel.messagelist.provider;

import ai.nexconn.chat.message.FileMessage;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.model.MessageDirection;
import ai.nexconn.chat.message.model.SentStatus;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.feature.resend.ResendManager;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.file.FileTypeUtils;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.route.RouteUtils;
import ai.nexconn.chatui.widget.EllipsizeTextView;
import ai.nexconn.chatui.widget.FileRectangleProgress;
import ai.nexconn.chatui.widget.adapter.IViewProviderListener;
import ai.nexconn.chatui.widget.adapter.ViewHolder;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;

public class FileMessageItemProvider extends BaseMessageItemProvider<FileMessage> {

    private int progress = 0;

    public FileMessageItemProvider() {
        mConfig.showReadState = true;
        mConfig.showContentBubble = false;
        mConfig.showProgress = false;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.nc_item_file_message, parent, false);
        return new ViewHolder(view.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(
            final ViewHolder holder,
            ViewHolder parentHolder,
            FileMessage fileMessage,
            final UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        EllipsizeTextView tvFileName = holder.getView(R.id.nc_msg_tv_file_name);
        FileRectangleProgress fileProgress = holder.getView(R.id.nc_msg_pb_file_upload_progress);
        if (!checkViewsValid(tvFileName, fileProgress)) {
            RLog.e(TAG, "checkViewsValid error," + uiMessage.getMessageType());
            return;
        }
        tvFileName.setAdaptiveText(fileMessage.getName());
        long fileSizeBytes = fileMessage.getSize();
        holder.setText(R.id.nc_msg_tv_file_size, FileTypeUtils.formatFileSize(fileSizeBytes));
        holder.setImageResource(
                R.id.nc_msg_iv_file_type_image,
                FileTypeUtils.fileTypeImageId(holder.getContext(), fileMessage.getName()));
        boolean isSender = uiMessage.getMessage().getDirection() == MessageDirection.SEND;
        holder.setBackgroundRes(
                R.id.nc_message,
                ChatUIThemeManager.getAttrResId(
                        holder.getContext(), R.attr.nc_conversation_msg_special_background));
        if (uiMessage.getMessage().getSentStatus() == SentStatus.SENDING
                && uiMessage.getProgress() < 100) {
            holder.setVisible(R.id.nc_msg_pb_file_upload_progress, true);

            if (ResendManager.getInstance().needResend(uiMessage.getMessage().getClientId())) {
                if (uiMessage.getProgress() == progress) {
                    holder.setVisible(R.id.nc_progress, true);
                    holder.setVisible(R.id.nc_btn_cancel, false);
                } else {
                    holder.setVisible(R.id.nc_progress, false);
                    holder.setVisible(R.id.nc_btn_cancel, true);
                }
            } else {
                progress = uiMessage.getProgress();
                if (progress > 0) {
                    holder.setVisible(R.id.nc_btn_cancel, true);
                    holder.setVisible(R.id.nc_progress, false);
                } else {
                    holder.setVisible(R.id.nc_btn_cancel, false);
                    holder.setVisible(R.id.nc_progress, true);
                }
            }
            holder.setHoldVisible(R.id.nc_msg_canceled, false);
            fileProgress.setProgress(uiMessage.getProgress());
        } else if (uiMessage.getMessage().getSentStatus() == SentStatus.FAILED
                && ResendManager.getInstance().needResend(uiMessage.getMessage().getClientId())) {
            holder.setVisible(R.id.nc_msg_pb_file_upload_progress, true);
            holder.setVisible(R.id.nc_btn_cancel, false);
            holder.setHoldVisible(R.id.nc_msg_canceled, false);
            holder.setVisible(R.id.nc_progress, true);
            fileProgress.setProgress(uiMessage.getProgress());
        } else {
            if (uiMessage.getMessage().getSentStatus() == SentStatus.CANCELED) {
                holder.setHoldVisible(R.id.nc_msg_canceled, true);
            } else {
                holder.setHoldVisible(R.id.nc_msg_canceled, false);
            }
            holder.setHoldVisible(R.id.nc_msg_pb_file_upload_progress, false);
            holder.setVisible(R.id.nc_btn_cancel, false);
            holder.setVisible(R.id.nc_progress, false);
        }

        holder.setOnClickListener(
                R.id.nc_btn_cancel,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ResendManager.getInstance()
                                .removeResendMessage(uiMessage.getMessage().getClientId());
                        NCChatUI.cancelSendMediaMessage(
                                uiMessage.getMessage(),
                                error -> {
                                    RLog.d(TAG, "cancelSendMediaMessage error=" + error);
                                });
                        holder.setVisible(R.id.nc_msg_canceled, true);
                        holder.setHoldVisible(R.id.nc_msg_pb_file_upload_progress, false);
                        holder.setVisible(R.id.nc_btn_cancel, false);
                    }
                });
    }

    @Override
    protected boolean onItemClick(
            ViewHolder holder,
            FileMessage fileMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        RouteUtils.routeToFilePreviewActivity(
                holder.getContext(), uiMessage.getMessage(), fileMessage, uiMessage.getProgress());
        return true;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof FileMessage;
    }

    @Override
    public Spannable getSummarySpannable(Context context, FileMessage fileMessage) {
        if (fileMessage != null && !TextUtils.isEmpty(fileMessage.getName())) {
            return new SpannableString(
                    context.getString(R.string.nc_conversation_summary_content_file)
                            + fileMessage.getName());
        }
        return new SpannableString(
                context.getString(R.string.nc_conversation_summary_content_file));
    }
}
