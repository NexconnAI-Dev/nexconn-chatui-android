package ai.nexconn.chatui.channel.messagelist.provider;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.ReferenceMessage;
import ai.nexconn.chat.message.model.MessageDirection;
import ai.nexconn.chat.message.model.MessageType;
import ai.nexconn.chat.message.model.MessageUpdateStatus;
import ai.nexconn.chat.message.model.ReadReceiptInfo;
import ai.nexconn.chat.message.model.ReferenceMessageStatus;
import ai.nexconn.chat.message.model.SentStatus;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.feature.editmessage.EditMessageManager;
import ai.nexconn.chatui.channel.feature.resend.ResendManager;
import ai.nexconn.chatui.config.ChannelClickListener;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.handler.AppSettingsHandler;
import ai.nexconn.chatui.message.HistoryDividerMessage;
import ai.nexconn.chatui.model.State;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.text.ChatUIDateUtils;
import ai.nexconn.chatui.widget.adapter.IViewProviderListener;
import ai.nexconn.chatui.widget.adapter.ViewHolder;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.Objects;

public abstract class BaseMessageItemProvider<T extends MessageContent>
        implements IMessageProvider<T> {
    protected static final String TAG = "BaseMessageItemProvider";
    protected MessageItemProviderConfig mConfig = new MessageItemProviderConfig();

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rootView =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.nc_message_item, parent, false);
        FrameLayout contentView = rootView.findViewById(R.id.nc_content);
        ViewHolder contentViewHolder = onCreateMessageContentViewHolder(contentView, viewType);
        if (contentViewHolder != null) {
            if (contentView.getChildCount() == 0) {
                contentView.addView(contentViewHolder.itemView);
            }
        }
        return new MessageViewHolder(rootView.getContext(), rootView, contentViewHolder);
    }

    /**
     * Creates the ViewHolder for message content.
     *
     * @param parent the parent ViewGroup
     * @param viewType the view type
     * @return ViewHolder
     */
    protected abstract ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType);

    @Override
    public boolean isItemViewType(UiMessage item) {
        return isMessageViewType(item.getMessage().getContent());
    }

    @Override
    public void bindViewHolder(
            final ViewHolder holder,
            final UiMessage uiMessage,
            final int position,
            final List<UiMessage> list,
            final IViewProviderListener<UiMessage> listener) {
        if (uiMessage != null && uiMessage.getMessage() != null && listener != null) {
            Message message = uiMessage.getMessage();
            holder.setVisible(R.id.nc_selected, uiMessage.isEdit());
            holder.setVisible(R.id.nc_v_edit, uiMessage.isEdit());
            if (uiMessage.isEdit()) {
                holder.setSelected(R.id.nc_selected, uiMessage.isSelected());
                holder.setOnClickListener(
                        R.id.nc_v_edit,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                listener.onViewClick(MessageClickType.EDIT_CLICK, uiMessage);
                            }
                        });
            }
            boolean isSender = uiMessage.getMessage().getDirection() == MessageDirection.SEND;
            initTime(holder, position, list, message);
            initUserInfo(holder, uiMessage, position, listener, isSender);
            initContent(holder, isSender, uiMessage, position, listener, list);
            initStatus(holder, uiMessage, position, listener, message, isSender, list);

            if (holder instanceof MessageViewHolder) {
                T msgContent = null;
                try {
                    msgContent = (T) uiMessage.getMessage().getContent();
                } catch (ClassCastException e) {
                    RLog.e(TAG, "bindViewHolder MessageContent cast Exception, e:" + e);
                }
                if (msgContent != null) {
                    bindMessageContentViewHolder(
                            ((MessageViewHolder) holder).getMessageContentViewHolder(),
                            holder,
                            msgContent,
                            uiMessage,
                            position,
                            list,
                            listener);
                } else {
                    RLog.e(TAG, "bindViewHolder MessageContent cast Exception");
                }
            } else {
                RLog.e(TAG, "holder is not MessageViewHolder");
            }
            uiMessage.setChange(false);
        } else {
            RLog.e(TAG, "uiMessage is null");
        }
    }

    private void initTime(ViewHolder holder, int position, List<UiMessage> data, Message message) {
        String time =
                ChatUIDateUtils.getConversationFormatDate(
                        message.getSentTime(), holder.getContext());
        holder.setText(R.id.nc_time, time);
        if (position == 0) {
            holder.setVisible(
                    R.id.nc_time, !(message.getContent() instanceof HistoryDividerMessage));
        } else {
            UiMessage pre = data.get(position - 1);
            if (pre.getMessage() != null
                    && ChatUIDateUtils.isShowChatTime(
                            holder.getContext(),
                            message.getSentTime(),
                            pre.getMessage().getSentTime(),
                            180)) {
                holder.setVisible(R.id.nc_time, true);
            } else {
                holder.setVisible(R.id.nc_time, false);
            }
        }
    }

    private void initUserInfo(
            final ViewHolder holder,
            final UiMessage uiMessage,
            final int position,
            final IViewProviderListener<UiMessage> listener,
            boolean isSender) {
        if (mConfig.showPortrait) {
            holder.setVisible(R.id.nc_left_portrait, !isSender);
            holder.setVisible(R.id.nc_right_portrait, isSender);
            ImageView view =
                    holder.getView(isSender ? R.id.nc_right_portrait : R.id.nc_left_portrait);
            UserInfo userInfo = uiMessage.getUserInfo();
            String portraitUri =
                    userInfo != null && userInfo.getPortraitUri() != null
                            ? userInfo.getPortraitUri().toString()
                            : "";
            NCChatUIConfig.featureConfig()
                    .getChatUIImageEngine()
                    .loadConversationPortrait(
                            holder.getContext(), portraitUri, view, uiMessage.getMessage());
            holder.setOnClickListener(
                    R.id.nc_left_portrait,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (NCChatUIConfig.channelConfig().getChannelClickListener() != null) {
                                boolean result =
                                        NCChatUIConfig.channelConfig()
                                                .getChannelClickListener()
                                                .onUserPortraitClick(
                                                        holder.getContext(),
                                                        uiMessage
                                                                .getMessage()
                                                                .getChannelIdentifier()
                                                                .getChannelType(),
                                                        uiMessage.getUserInfo(),
                                                        uiMessage
                                                                .getMessage()
                                                                .getChannelIdentifier()
                                                                .getChannelId());
                                if (!result) {
                                    listener.onViewClick(
                                            MessageClickType.USER_PORTRAIT_CLICK, uiMessage);
                                }
                            }
                        }
                    });

            holder.setOnClickListener(
                    R.id.nc_right_portrait,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (NCChatUIConfig.channelConfig().getChannelClickListener() != null) {
                                boolean result =
                                        NCChatUIConfig.channelConfig()
                                                .getChannelClickListener()
                                                .onUserPortraitClick(
                                                        holder.getContext(),
                                                        uiMessage
                                                                .getMessage()
                                                                .getChannelIdentifier()
                                                                .getChannelType(),
                                                        uiMessage.getUserInfo(),
                                                        uiMessage
                                                                .getMessage()
                                                                .getChannelIdentifier()
                                                                .getChannelId());
                                if (!result) {
                                    listener.onViewClick(
                                            MessageClickType.USER_PORTRAIT_CLICK, uiMessage);
                                }
                            }
                        }
                    });

            holder.setOnLongClickListener(
                    R.id.nc_left_portrait,
                    new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            if (NCChatUIConfig.channelConfig().getChannelClickListener() != null) {
                                boolean result =
                                        NCChatUIConfig.channelConfig()
                                                .getChannelClickListener()
                                                .onUserPortraitLongClick(
                                                        holder.getContext(),
                                                        uiMessage
                                                                .getMessage()
                                                                .getChannelIdentifier()
                                                                .getChannelType(),
                                                        uiMessage.getUserInfo(),
                                                        uiMessage
                                                                .getMessage()
                                                                .getChannelIdentifier()
                                                                .getChannelId());
                                if (!result) {
                                    result =
                                            listener.onViewLongClick(
                                                    holder.getView(R.id.nc_content),
                                                    MessageClickType.USER_PORTRAIT_LONG_CLICK,
                                                    uiMessage);
                                }
                                return result;
                            }
                            return false;
                        }
                    });

            holder.setOnLongClickListener(
                    R.id.nc_right_portrait,
                    new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            if (NCChatUIConfig.channelConfig().getChannelClickListener() != null) {
                                boolean result =
                                        NCChatUIConfig.channelConfig()
                                                .getChannelClickListener()
                                                .onUserPortraitLongClick(
                                                        holder.getContext(),
                                                        uiMessage
                                                                .getMessage()
                                                                .getChannelIdentifier()
                                                                .getChannelType(),
                                                        uiMessage.getUserInfo(),
                                                        uiMessage
                                                                .getMessage()
                                                                .getChannelIdentifier()
                                                                .getChannelId());
                                if (!result) {
                                    listener.onViewLongClick(
                                            holder.getView(R.id.nc_content),
                                            MessageClickType.USER_PORTRAIT_LONG_CLICK,
                                            uiMessage);
                                }
                                return result;
                            }
                            return false;
                        }
                    });
            if (!NCChatUIConfig.channelConfig()
                    .isShowReceiverUserTitle(
                            uiMessage.getMessage().getChannelIdentifier().getChannelType())) {
                holder.setVisible(R.id.nc_title, false);
            } else {
                if (!isSender) {
                    holder.setVisible(R.id.nc_title, true);
                    holder.setText(R.id.nc_title, uiMessage.getDisplayName());
                } else {
                    holder.setVisible(R.id.nc_title, false);
                }
            }
        } else {
            holder.setVisible(R.id.nc_left_portrait, false);
            holder.setVisible(R.id.nc_right_portrait, false);
            holder.setVisible(R.id.nc_title, false);
        }
    }

    private void initContent(
            final ViewHolder holder,
            boolean isSender,
            final UiMessage uiMessage,
            final int position,
            final IViewProviderListener<UiMessage> listener,
            final List<UiMessage> list) {
        if (showBubble()) {
            holder.setBackgroundRes(
                    R.id.nc_content,
                    ChatUIThemeManager.getAttrResId(
                            holder.getContext(), getBackgroundAttrId(isSender, uiMessage)));
        } else {
            holder.getView(R.id.nc_content).setBackground(null);
        }
        holder.setPadding(R.id.nc_content, 0, 0, 0, 0);

        LinearLayout layout = holder.getView(R.id.nc_layout);
        if (mConfig.centerInHorizontal) {
            layout.setGravity(Gravity.CENTER_HORIZONTAL);
        } else {
            layout.setGravity(isSender ? Gravity.END : Gravity.START);
        }

        holder.setOnClickListener(
                R.id.nc_content,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean result = false;
                        /**
                         * Click event dispatch: app -> message template implementations ->
                         * Processor
                         */
                        if (NCChatUIConfig.channelConfig().getChannelClickListener() != null) {
                            result =
                                    NCChatUIConfig.channelConfig()
                                            .getChannelClickListener()
                                            .onMessageClick(
                                                    holder.getContext(), v, uiMessage.getMessage());
                        }
                        if (!result) {

                            T msgContent = null;
                            try {
                                msgContent = (T) uiMessage.getMessage().getContent();
                            } catch (ClassCastException e) {
                                RLog.e(
                                        TAG,
                                        "NC_content onClick MessageContent cast Exception, e:" + e);
                            }
                            if (msgContent != null) {
                                result =
                                        onItemClick(
                                                ((MessageViewHolder) holder)
                                                        .getMessageContentViewHolder(),
                                                msgContent,
                                                uiMessage,
                                                position,
                                                list,
                                                listener);
                            }
                            if (!result) {
                                listener.onViewClick(MessageClickType.CONTENT_CLICK, uiMessage);
                            }
                        }
                    }
                });

        holder.setOnLongClickListener(
                R.id.nc_content,
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        boolean result = false;
                        if (NCChatUIConfig.channelConfig().getChannelClickListener() != null) {
                            result =
                                    NCChatUIConfig.channelConfig()
                                            .getChannelClickListener()
                                            .onMessageLongClick(
                                                    holder.getContext(), v, uiMessage.getMessage());
                        }
                        if (!result) {
                            T msgContent = null;
                            try {
                                msgContent = (T) uiMessage.getMessage().getContent();
                            } catch (ClassCastException e) {
                                RLog.e(
                                        TAG,
                                        "NC_content onLongClick MessageContent cast Exception, e:"
                                                + e);
                            }
                            if (msgContent != null) {
                                result =
                                        onItemLongClick(
                                                ((MessageViewHolder) holder)
                                                        .getMessageContentViewHolder(),
                                                (T) uiMessage.getMessage().getContent(),
                                                uiMessage,
                                                position,
                                                list,
                                                listener);
                            }
                            if (!result) {
                                listener.onViewLongClick(
                                        holder.getView(R.id.nc_content),
                                        MessageClickType.CONTENT_LONG_CLICK,
                                        uiMessage);
                            }
                            return result;
                        }
                        return false;
                    }
                });
    }

    private int getBackgroundAttrId(boolean isSender, UiMessage uiMessage) {
        int backgroundAttrId =
                isSender
                        ? R.attr.nc_conversation_msg_send_background
                        : R.attr.nc_conversation_msg_receiver_background;
        if (Objects.equals(uiMessage.getMessage().getMessageType(), MessageType.FILE)
                || Objects.equals(uiMessage.getMessage().getMessageType(), MessageType.LOCATION)) {
            backgroundAttrId = R.attr.nc_conversation_msg_special_background;
        }
        return backgroundAttrId;
    }

    private void initStatus(
            ViewHolder holder,
            final UiMessage uiMessage,
            final int position,
            final IViewProviderListener<UiMessage> listener,
            Message message,
            boolean isSender,
            List<UiMessage> list) {
        if (mConfig.showWarning
                && !ResendManager.getInstance().needResend(uiMessage.getMessage().getClientId())) {
            if (isSender
                    && uiMessage.getState() == State.ERROR
                    && message.getSentStatus() == SentStatus.FAILED) {
                holder.setVisible(R.id.nc_warning, true);
                holder.setOnClickListener(
                        R.id.nc_warning,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                listener.onViewClick(MessageClickType.WARNING_CLICK, uiMessage);
                            }
                        });
            } else {
                holder.setVisible(R.id.nc_warning, false);
            }
        } else {
            holder.setVisible(R.id.nc_warning, false);
        }
        if (mConfig.showProgress) {
            if (isSender
                    && uiMessage.getState() == State.PROGRESS
                    && message.getSentStatus() == SentStatus.SENDING) {
                holder.setVisible(R.id.nc_progress, true);
            } else if (isSender
                    && uiMessage.getState() == State.ERROR
                    && ResendManager.getInstance()
                            .needResend(uiMessage.getMessage().getClientId())) {
                holder.setVisible(R.id.nc_progress, true);
            } else {
                holder.setVisible(R.id.nc_progress, false);
            }
        } else {
            holder.setVisible(R.id.nc_progress, false);
        }
        if (AppSettingsHandler.getInstance()
                .isReadReceiptV5Enabled(message.getChannelIdentifier().getChannelType())) {
            initReadV5Status(holder, uiMessage, listener, message, isSender);
        } else {
            holder.setVisible(R.id.nc_read_receipt, false);
            holder.setVisible(R.id.nc_read_receipt_status, false);
            holder.setVisible(R.id.nc_read_receipt_request, false);
        }
        initEditStatus(holder, message);
    }

    /**
     * Binds values to the views within the message view.
     *
     * @param holder ViewHolder
     * @param parentHolder the parent layout's ViewHolder
     * @param t the message content corresponding to this template
     * @param uiMessage {@link UiMessage}
     * @param position the message position
     * @param list the message list
     * @param listener click event listener for the ViewModel. If a sub-view's click event needs
     *     ViewModel handling, use this listener callback.
     */
    protected abstract void bindMessageContentViewHolder(
            ViewHolder holder,
            ViewHolder parentHolder,
            T t,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener);

    /**
     * @param holder ViewHolder
     * @param t the custom message content
     * @param uiMessage {@link UiMessage}
     * @param position the position
     * @param list the list data
     * @param listener click event listener for the ViewModel. If a sub-view's click event needs
     *     ViewModel handling, use this listener callback.
     * @return whether the click event was consumed
     */
    protected abstract boolean onItemClick(
            ViewHolder holder,
            T t,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener);

    protected boolean onItemLongClick(
            ViewHolder holder,
            T t,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        return false;
    }

    /**
     * Sets up the read-receipt V5 read-status image component. V1 read receipts used
     * NC_read_receipt (ImageView) for direct chats and
     * NC_read_receipt_status/NC_read_receipt_request (TextView) for groups. V5 uses ImageView for
     * both, so NC_read_receipt is reused and NC_read_receipt_status/NC_read_receipt_request are
     * hidden.
     */
    private void initReadV5Status(
            ViewHolder holder,
            final UiMessage uiMessage,
            final IViewProviderListener<UiMessage> listener,
            final Message message,
            boolean isSender) {
        holder.setVisible(R.id.nc_read_receipt_status, false);
        holder.setVisible(R.id.nc_read_receipt_request, false);

        if (!isSender || TextUtils.isEmpty(message.getMessageId())) {
            holder.setVisible(R.id.nc_read_receipt, false);
            return;
        }
        holder.setVisible(R.id.nc_read_receipt, true);
        ReadReceiptInfo readInfo = uiMessage.getReadReceiptInfo();
        if (ChannelType.DIRECT == message.getChannelIdentifier().getChannelType()) {
            ImageView readReceipt = holder.getView(R.id.nc_read_receipt);
            boolean isRead =
                    mConfig.showReadState && readInfo != null && readInfo.getReadCount() >= 1;
            if (isRead) {
                readReceipt.setImageResource(
                        ChatUIThemeManager.getAttrResId(
                                holder.getContext(),
                                R.attr.nc_conversation_list_cell_msg_read_img));
            } else {
                readReceipt.setImageResource(
                        ChatUIThemeManager.getAttrResId(
                                holder.getContext(),
                                R.attr.nc_conversation_list_cell_msg_unread_img));
            }
        } else if (ChannelType.GROUP == message.getChannelIdentifier().getChannelType()) {
            drawReadReceiptCircle(holder, readInfo);
            View.OnClickListener click =
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ChannelClickListener conversationClickListener =
                                    NCChatUIConfig.channelConfig().getChannelClickListener();
                            if (conversationClickListener != null
                                    && conversationClickListener.onReadReceiptStateClick(
                                            v.getContext(), uiMessage.getMessage())) {
                                return;
                            }
                            listener.onViewClick(
                                    MessageClickType.READ_RECEIPT_STATE_CLICK, uiMessage);
                        }
                    };
            holder.setOnClickListener(R.id.nc_read_receipt, click);
        }
    }

    private void initEditStatus(final ViewHolder holder, final Message message) {
        if (!NCChatUIConfig.featureConfig().isEditMessageEnable()
                || !mConfig.showEditState
                || message.getUpdateInfo() == null) {
            holder.setVisible(R.id.nc_edit_status_layout, false);
            return;
        }
        MessageUpdateStatus status = message.getUpdateInfo().getStatus();
        TextView content = holder.getView(R.id.nc_edit_status_content);
        if (MessageUpdateStatus.FAILED == status) {
            holder.setVisible(R.id.nc_edit_status_layout, true);
            holder.setVisible(R.id.nc_edit_status_failed, true);
            holder.setVisible(R.id.nc_edit_status_progress, false);
            content.setText(R.string.nc_edit_status_failed);
            content.setTextColor(content.getResources().getColor(R.color.nc_edit_failed));
            holder.setOnClickListener(
                    R.id.nc_edit_status_layout,
                    view -> EditMessageManager.getInstance().editMessage(message, ""));
        } else if (MessageUpdateStatus.UPDATING == status) {
            holder.setVisible(R.id.nc_edit_status_layout, true);
            holder.setVisible(R.id.nc_edit_status_failed, false);
            holder.setVisible(R.id.nc_edit_status_progress, true);
            content.setText(R.string.nc_edit_status_progress);
            content.setTextColor(content.getResources().getColor(R.color.nc_edit_progress));
            holder.setOnClickListener(R.id.nc_edit_status_layout, null);
        } else if (MessageUpdateStatus.SUCCESS == status) {
            holder.setVisible(R.id.nc_edit_status_layout, false);
            holder.setOnClickListener(R.id.nc_edit_status_layout, null);
        }
    }

    protected void setTextMessageContent(
            TextView textView, UiMessage uiMessage, SpannableStringBuilder span) {
        if (uiMessage.getMessage().getHasChanged()) {
            SpannableStringBuilder contentSpannable = new SpannableStringBuilder(span);
            String edited = textView.getContext().getString(R.string.nc_edit_status_success);
            SpannableStringBuilder spannable = new SpannableStringBuilder("（" + edited + "）");
            ForegroundColorSpan foregroundColorSpan =
                    new ForegroundColorSpan(getEditedSuffixColor(textView.getContext()));
            spannable.setSpan(
                    foregroundColorSpan, 0, spannable.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            contentSpannable.append(spannable);
            textView.setText(contentSpannable);
        } else {
            textView.setText(span);
        }
    }

    protected void setReferenceMessageContent(
            TextView textView, UiMessage uiMessage, SpannableStringBuilder span) {
        ReferenceMessage content = (ReferenceMessage) uiMessage.getMessage().getContent();
        textView.setText(buildReferenceMessageContent(textView.getContext(), content, span));
    }

    protected SpannableStringBuilder buildReferenceMessageContent(
            Context context, ReferenceMessage content, SpannableStringBuilder span) {
        ReferenceMessageStatus referMsgStatus = content.getReferMsgStatus();
        if (referMsgStatus == ReferenceMessageStatus.MODIFIED) {
            return buildModifiedReferenceMessageContent(
                    span,
                    context.getString(R.string.nc_edit_status_success),
                    getEditedSuffixColor(context));
        } else if (referMsgStatus == ReferenceMessageStatus.DELETED) {
            SpannableStringBuilder contentSpannable = new SpannableStringBuilder();
            String text = context.getString(R.string.nc_reference_status_delete);
            SpannableStringBuilder spannableString = new SpannableStringBuilder(text);
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(getEditStatusColor(context));
            spannableString.setSpan(
                    colorSpan, 0, spannableString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            contentSpannable.append(spannableString);
            return contentSpannable;
        } else if (referMsgStatus == ReferenceMessageStatus.RECALLED) {
            SpannableStringBuilder contentSpannable = new SpannableStringBuilder();
            String text = context.getString(R.string.nc_reference_status_recall);
            SpannableStringBuilder spannableString = new SpannableStringBuilder(text);
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(getEditStatusColor(context));
            spannableString.setSpan(
                    colorSpan, 0, spannableString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            contentSpannable.append(spannableString);
            return contentSpannable;
        } else {
            return span;
        }
    }

    /**
     * Determines whether this template should handle the given message content type.
     *
     * @param messageContent the message content
     * @return whether this template handles the given type
     */
    protected abstract boolean isMessageViewType(MessageContent messageContent);

    @Override
    public boolean isSummaryType(MessageContent messageContent) {
        return isMessageViewType(messageContent);
    }

    @Override
    public boolean showSummaryWithName() {
        return mConfig.showSummaryWithName;
    }

    public static class MessageViewHolder extends ViewHolder {
        private ViewHolder mMessageContentViewHolder;

        public MessageViewHolder(Context context, View itemView, ViewHolder messageViewHolder) {
            super(context, itemView);
            mMessageContentViewHolder = messageViewHolder;
        }

        public ViewHolder getMessageContentViewHolder() {
            return mMessageContentViewHolder;
        }
    }

    public boolean showBubble() {
        return mConfig.showContentBubble;
    }

    protected boolean checkViewsValid(View... views) {
        if (views == null || views.length == 0) {
            return false;
        }
        for (View view : views) {
            if (view == null) {
                return false;
            }
        }
        return true;
    }

    /** Draws a circle status indicator based on the read percentage. */
    private void drawReadReceiptCircle(ViewHolder holder, ReadReceiptInfo readInfo) {
        ImageView imageView = holder.getView(R.id.nc_read_receipt);
        int readCount = 0, unreadCount = 0;
        if (readInfo != null) {
            readCount = readInfo.getReadCount();
            unreadCount = readInfo.getUnreadCount();
        }
        int totalCount = readCount + unreadCount;
        float readPercentage = totalCount > 0 ? (float) readCount / totalCount : 0f;
        if (readPercentage >= 1f) {
            imageView.setImageResource(
                    ChatUIThemeManager.getAttrResId(
                            holder.getContext(), R.attr.nc_conversation_list_cell_msg_read_img));
            return;
        } else if (readPercentage <= 0f) {
            imageView.setImageResource(
                    ChatUIThemeManager.getAttrResId(
                            holder.getContext(), R.attr.nc_conversation_list_cell_msg_unread_img));
            return;
        }
        Context context = imageView.getContext();
        int size = 12; // dp
        float density = context.getResources().getDisplayMetrics().density;
        int sizeInPx = (int) (size * density);
        if (sizeInPx < 12) {
            sizeInPx = 12;
        }
        Bitmap bitmap = Bitmap.createBitmap(sizeInPx, sizeInPx, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);

        float center = sizeInPx / 2f;
        float borderWidth = Math.max(1.5f, sizeInPx / 8f);
        float outerRadius = center - (borderWidth * 0.5f);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(
                ChatUIThemeManager.getColorFromAttrId(
                        holder.getContext(), R.attr.nc_success_color));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(borderWidth);
        canvas.drawCircle(center, center, outerRadius, paint);
        if (readPercentage > 0f) {
            float innerPadding = Math.max(1.5f, sizeInPx / 8f);
            float fillRadius = outerRadius - (borderWidth * 0.5f) - innerPadding;
            if (fillRadius > 0) {
                paint.setStyle(Paint.Style.FILL);
                RectF rectF =
                        new RectF(
                                center - fillRadius,
                                center - fillRadius,
                                center + fillRadius,
                                center + fillRadius);
                float startAngle = -90f;
                float sweepAngle = 360f * readPercentage;
                canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);
            }
        }
        imageView.setImageDrawable(new BitmapDrawable(context.getResources(), bitmap));
    }

    private int getEditStatusColor(TextView textView) {
        return getEditStatusColor(textView.getContext());
    }

    private int getEditStatusColor(Context context) {
        return ChatUIThemeManager.getColorFromAttrId(
                context, R.attr.nc_text_primary_color);
    }

    /** The edited suffix uses the secondary color for body and reference text, matching iOS. */
    private int getEditedSuffixColor(Context context) {
        return ChatUIThemeManager.getColorFromAttrId(context, R.attr.nc_text_secondary_color);
    }

    protected SpannableStringBuilder buildModifiedReferenceMessageContent(
            SpannableStringBuilder span, String editedText, int color) {
        SpannableStringBuilder contentSpannable = new SpannableStringBuilder(span);
        contentSpannable.append("（").append(editedText).append("）");
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
        contentSpannable.setSpan(
                colorSpan, 0, contentSpannable.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return contentSpannable;
    }
}
