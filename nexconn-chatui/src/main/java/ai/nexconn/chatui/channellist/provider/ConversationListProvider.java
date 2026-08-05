package ai.nexconn.chatui.channellist.provider;

import static android.content.Context.MODE_PRIVATE;

import ai.nexconn.chat.NCEngine;
import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.channel.model.ChannelNoDisturbLevel;
import ai.nexconn.chat.message.model.ReadReceiptInfo;
import ai.nexconn.chat.message.model.SentStatus;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channellist.model.BaseUiChannel;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.handler.AppSettingsHandler;
import ai.nexconn.chatui.message.InformationNotificationMessage;
import ai.nexconn.chatui.utils.common.ChatUIUtils;
import ai.nexconn.chatui.utils.text.ChatUIDateUtils;
import ai.nexconn.chatui.widget.adapter.IViewProvider;
import ai.nexconn.chatui.widget.adapter.IViewProviderListener;
import ai.nexconn.chatui.widget.adapter.ViewHolder;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.widget.TextViewCompat;
import java.util.List;

/**
 * Unified provider for the channel list
 *
 * @since 5.10.4
 */
public class ConversationListProvider implements IViewProvider<BaseUiChannel> {

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.nc_conversationlist_item, parent, false);
        return ViewHolder.createViewHolder(parent.getContext(), view);
    }

    @Override
    public boolean isItemViewType(BaseUiChannel item) {
        return true;
    }

    @Override
    public void bindViewHolder(
            final ViewHolder holder,
            final BaseUiChannel uiConversation,
            int position,
            List<BaseUiChannel> list,
            IViewProviderListener<BaseUiChannel> listener) {

        holder.setText(R.id.nc_conversation_title, uiConversation.getConversationTitle());
        bindChildClickToItem(holder, R.id.nc_conversation_title);

        String portraitUrl = uiConversation.getPortraitUrl();
        if (!TextUtils.isEmpty(portraitUrl)) {
            if (holder.getView(R.id.nc_conversation_portrait) instanceof ImageView) {
                NCChatUIConfig.featureConfig()
                        .getChatUIImageEngine()
                        .loadConversationListPortrait(
                                holder.getContext(),
                                portraitUrl,
                                holder.<ImageView>getView(R.id.nc_conversation_portrait),
                                uiConversation.mCore);
            }
        } else {
            int drawableId =
                    ChatUIThemeManager.getAttrResId(
                            holder.getContext(), R.attr.nc_conversation_list_cell_portrait_msg_img);
            if (ChannelType.GROUP.equals(uiConversation.mCore.getChannelType())) {
                drawableId =
                        ChatUIThemeManager.getAttrResId(
                                holder.getContext(),
                                R.attr.nc_conversation_list_cell_group_portrait_img);
            } else if (ChannelType.OPEN.equals(uiConversation.mCore.getChannelType())) {
                drawableId =
                        ChatUIThemeManager.getAttrResId(
                                holder.getContext(),
                                R.attr.nc_conversation_list_cell_discussion_portrait_img);
            }
            if (holder.getView(R.id.nc_conversation_portrait) instanceof ImageView) {
                Uri uri = ChatUIUtils.getUriFromDrawableRes(holder.getContext(), drawableId);
                NCChatUIConfig.featureConfig()
                        .getChatUIImageEngine()
                        .loadConversationListPortrait(
                                holder.getContext(),
                                uri.toString(),
                                holder.<ImageView>getView(R.id.nc_conversation_portrait),
                                uiConversation.mCore);
            }
        }
        holder.getView(R.id.nc_conversation_portrait)
                .setOnClickListener(
                        v -> {
                            boolean consumed = false;
                            if (NCChatUIConfig.channelListConfig().getListener() != null) {
                                consumed =
                                        NCChatUIConfig.channelListConfig()
                                                .getListener()
                                                .onConversationPortraitClick(
                                                        holder.getContext(),
                                                        uiConversation.mCore.getChannelType(),
                                                        uiConversation.mCore.getChannelId());
                            }
                            // Portrait click should fallback to item click when not consumed.
                            if (!consumed && holder.itemView != null) {
                                holder.itemView.performClick();
                            }
                        });
        holder.getView(R.id.nc_conversation_portrait)
                .setOnLongClickListener(
                        v -> {
                            boolean consumed = false;
                            if (NCChatUIConfig.channelListConfig().getListener() != null) {
                                consumed =
                                        NCChatUIConfig.channelListConfig()
                                                .getListener()
                                                .onConversationPortraitLongClick(
                                                        holder.getContext(),
                                                        uiConversation.mCore.getChannelType(),
                                                        uiConversation.mCore.getChannelId());
                            }
                            // Portrait long-click should fallback to item long-click menu.
                            return consumed
                                    || (holder.itemView != null
                                            && holder.itemView.performLongClick());
                        });

        TextView contentView = (TextView) holder.getView(R.id.nc_conversation_content);
        TextViewCompat.setCompoundDrawablesRelative(contentView, null, null, null, null);
        Drawable drawable = getContentStatusDrawable(holder, uiConversation);
        if (drawable != null) {
            Bitmap bitmap =
                    BitmapFactory.decodeResource(
                            holder.getContext().getResources(),
                            ChatUIThemeManager.getAttrResId(
                                    holder.getContext(),
                                    R.attr.nc_conversation_list_cell_msg_fail_msg));
            int width = bitmap.getWidth();
            int bottom = width;
            drawable.setBounds(0, 0, width, bottom);
            contentView.setCompoundDrawablePadding(10);
            TextViewCompat.setCompoundDrawablesRelative(contentView, drawable, null, null, null);
        }
        holder.setText(
                R.id.nc_conversation_content,
                uiConversation.mConversationContent,
                TextView.BufferType.SPANNABLE);
        bindChildClickToItem(holder, R.id.nc_conversation_content);

        int unreadCount = uiConversation.getUnreadMessageCount();
        if (unreadCount > 0) {
            holder.setVisible(R.id.nc_conversation_unread, true);
            if (unreadCount > 99) {
                holder.setImageResource(
                        R.id.nc_conversation_unread_bg, R.drawable.nc_unread_count_bg_large);
                holder.setText(
                        R.id.nc_conversation_unread_count,
                        holder.getContext().getString(R.string.nc_conversation_unread_dot));
            } else {
                holder.setImageResource(
                        R.id.nc_conversation_unread_bg, R.drawable.nc_unread_count_bg_normal);
                String count = Integer.toString(unreadCount);
                holder.setText(R.id.nc_conversation_unread_count, count);
            }
            holder.setVisible(R.id.nc_conversation_unread, true);
        } else {
            holder.setVisible(R.id.nc_conversation_unread, false);
        }

        long sentTime =
                uiConversation.mCore.getLatestMessage() != null
                        ? uiConversation.mCore.getLatestMessage().getSentTime()
                        : uiConversation.mCore.getOperationTime();
        String time = ChatUIDateUtils.getConversationListFormatDate(sentTime, holder.getContext());
        holder.setText(R.id.nc_conversation_date, time);
        bindChildClickToItem(holder, R.id.nc_conversation_date);

        boolean isTop = uiConversation.mCore.isPinned();
        if (isTop) {
            int topColorResId =
                    ChatUIThemeManager.getAttrResId(
                            holder.getContext(), R.attr.nc_conversation_stick_color);
            holder.getConvertView()
                    .setBackgroundColor(holder.getContext().getResources().getColor(topColorResId));
        } else {
            int whiteColorResId =
                    ChatUIThemeManager.getAttrResId(
                            holder.getContext(), R.attr.nc_conversation_list_background_color);
            holder.getConvertView()
                    .setBackgroundColor(
                            holder.getContext().getResources().getColor(whiteColorResId));
        }

        holder.setVisible(R.id.nc_conversation_pin_top, isTop);

        boolean noDisturb = uiConversation.mCore.getNoDisturbLevel() == ChannelNoDisturbLevel.MUTED;
        holder.setVisible(R.id.nc_conversation_no_disturb, noDisturb);
        if (ChannelType.COMMUNITY.equals(uiConversation.mCore.getChannelType())) {
            holder.setVisible(R.id.divider, false);
        } else {
            holder.setVisible(R.id.divider, true);
        }
        if (isDebugMode(holder.getContext())) {
            String debugTitle = uiConversation.getConversationTitle();
            holder.setText(
                    R.id.nc_conversation_title,
                    debugTitle + "(" + uiConversation.mCore.getNoDisturbLevel() + ")");
        }

        if (AppSettingsHandler.getInstance().isOnlineStatusEnable()
                && uiConversation.mCore.getChannelType() == ChannelType.DIRECT) {
            holder.setVisible(R.id.nc_conversation_online_status, true);
            boolean isOnline =
                    uiConversation.getOnlineStatus() != null
                            && uiConversation.getOnlineStatus().isOnline();
            int resId =
                    ChatUIThemeManager.getAttrResId(
                            holder.getContext(),
                            isOnline
                                    ? R.attr.nc_user_online_status_img
                                    : R.attr.nc_user_offline_status_img);
            holder.setImageResource(R.id.nc_conversation_online_status, resId);
        } else {
            holder.setVisible(R.id.nc_conversation_online_status, false);
        }

        initPrivateReadStatus(holder, uiConversation);
    }

    private void initPrivateReadStatus(ViewHolder holder, BaseUiChannel uiConversation) {
        holder.setVisible(R.id.nc_conversation_read_receipt, false);
    }

    protected Drawable getContentStatusDrawable(ViewHolder holder, BaseUiChannel uiConversation) {
        Resources resources = holder.getContext().getResources();
        if (uiConversation.isShowDraftContent()
                || TextUtils.isEmpty(uiConversation.mConversationContent)) {
            return null;
        }
        ai.nexconn.chat.message.Message latestMsg = uiConversation.mCore.getLatestMessage();
        SentStatus sentStatus = latestMsg != null ? latestMsg.getSentStatus() : SentStatus.NONE;
        if (SentStatus.FAILED == sentStatus) {
            return resources.getDrawable(
                    ChatUIThemeManager.getAttrResId(
                            holder.getContext(), R.attr.nc_conversation_list_cell_msg_fail_msg));
        } else if (SentStatus.SENDING == sentStatus) {
            return resources.getDrawable(
                    ChatUIThemeManager.getAttrResId(
                            holder.getContext(), R.attr.nc_conversation_list_cell_msg_sending_img));
        } else if (latestMsg != null
                && uiConversation.mCore != null
                && uiConversation.mCore.getChannelType() == ChannelType.DIRECT
                && latestMsg.getSenderUserId() != null
                && latestMsg.getSenderUserId().equals(NCEngine.getCurrentUserId())
                && latestMsg.getContent() != null
                && !(latestMsg.getContent() instanceof InformationNotificationMessage)
                && !TextUtils.isEmpty(latestMsg.getMessageId())) {
            return resources.getDrawable(getReadReceiptStatusResId(holder, uiConversation));
        }
        return null;
    }

    protected int getReadReceiptStatusResId(ViewHolder holder, BaseUiChannel uiConversation) {
        ReadReceiptInfo receiptInfo = uiConversation.getReadReceiptInfo();
        boolean read = receiptInfo != null && receiptInfo.getReadCount() >= 1;

        // Fallback: if ReadReceiptInfo query is delayed/missing, check message SentStatus
        if (!read
                && uiConversation.mCore != null
                && uiConversation.mCore.getLatestMessage() != null) {
            read = SentStatus.READ == uiConversation.mCore.getLatestMessage().getSentStatus();
        }

        if (read) {
            return ChatUIThemeManager.getAttrResId(
                    holder.getContext(), R.attr.nc_conversation_list_cell_msg_read_img);
        }
        return ChatUIThemeManager.getAttrResId(
                holder.getContext(), R.attr.nc_conversation_list_cell_msg_unread_img);
    }

    private boolean isDebugMode(Context context) {
        return context.getSharedPreferences("config", MODE_PRIVATE).getBoolean("isDebug", false);
    }

    /**
     * For child views that are not intended to handle interaction independently, proxy touch events
     * back to itemView so list click/long-click behavior remains consistent.
     */
    private void bindChildClickToItem(ViewHolder holder, int childViewId) {
        View childView = holder.getView(childViewId);
        if (childView == null || holder.itemView == null) {
            return;
        }
        childView.setOnClickListener(v -> holder.itemView.performClick());
        childView.setOnLongClickListener(v -> holder.itemView.performLongClick());
    }
}
