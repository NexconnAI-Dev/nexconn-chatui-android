package ai.nexconn.chatui.channel.messagelist.provider;

import ai.nexconn.chat.channel.model.ChannelIdentifier;
import ai.nexconn.chat.message.CombineMessage;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.channel.feature.combineforward.CombineForwardTitleHelper;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.route.RouteUtils;
import ai.nexconn.chatui.widget.adapter.IViewProviderListener;
import ai.nexconn.chatui.widget.adapter.ViewHolder;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.List;

/**
 * Combined-forward message bubble Provider. Displays title, summary list, divider, and "chat
 * records" label.
 */
public class CombineMessageItemProvider extends BaseMessageItemProvider<CombineMessage> {

    private static final int MAX_SUMMARY_LINES = 4;

    public CombineMessageItemProvider() {
        mConfig.showReadState = true;
        mConfig.showContentBubble = false;
        mConfig.showProgress = true;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.nc_item_combine_message, parent, false);
        return new ViewHolder(view.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(
            ViewHolder holder,
            ViewHolder parentHolder,
            CombineMessage combineMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        // Set bubble background
        holder.setBackgroundRes(
                R.id.nc_message,
                ChatUIThemeManager.getAttrResId(
                        holder.getContext(), R.attr.nc_conversation_msg_special_background));

        // Title: concatenated from nameList
        TextView titleView = holder.getView(R.id.nc_combine_title);
        titleView.setText(
                CombineForwardTitleHelper.buildTitle(holder.getContext(), combineMessage));

        bindSummaryPreview(holder, combineMessage.getSummaryList());

        // Bottom label
        TextView labelView = holder.getView(R.id.nc_combine_label);
        labelView.setText(holder.getContext().getString(R.string.nc_combine_chat_record));
    }

    private void bindSummaryPreview(ViewHolder holder, List<String> summaryList) {
        int[] summaryIds = {
            R.id.nc_combine_summary_1,
            R.id.nc_combine_summary_2,
            R.id.nc_combine_summary_3,
            R.id.nc_combine_summary_4
        };
        TextView summaryView = holder.getView(summaryIds[0]);
        StringBuilder previewBuilder = new StringBuilder();
        if (summaryList != null) {
            for (String summary : summaryList) {
                if (TextUtils.isEmpty(summary)) {
                    continue;
                }
                if (previewBuilder.length() > 0) {
                    previewBuilder.append('\n');
                }
                previewBuilder.append(summary);
            }
        }
        String previewText = previewBuilder.toString();
        if (TextUtils.isEmpty(previewText)) {
            summaryView.setText("");
            summaryView.setVisibility(View.GONE);
        } else {
            summaryView.setSingleLine(false);
            summaryView.setMaxLines(MAX_SUMMARY_LINES);
            summaryView.setEllipsize(TextUtils.TruncateAt.END);
            summaryView.setText(previewText);
            summaryView.setVisibility(View.VISIBLE);
        }
        for (int i = 1; i < summaryIds.length; i++) {
            TextView extraSummaryView = holder.getView(summaryIds[i]);
            extraSummaryView.setText("");
            extraSummaryView.setVisibility(View.GONE);
        }
    }

    @Override
    protected boolean onItemClick(
            ViewHolder holder,
            CombineMessage combineMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        // Clear unread and mention count before entering merge forward detail page
        // This fixes issue 6976781159: @ indicator not clearing when viewing merge forward
        Message message = uiMessage.getMessage();
        ChannelIdentifier identifier = message != null ? message.getChannelIdentifier() : null;
        if (identifier != null && identifier.getChannelType() != null) {
            NCChatUI.clearUnreadCount(identifier, null);
        }

        RouteUtils.routeToCombineMessageDetailActivity(holder.getContext(), message);
        return true;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof CombineMessage;
    }

    @Override
    public Spannable getSummarySpannable(Context context, CombineMessage combineMessage) {
        return new SpannableString(
                context.getString(R.string.nc_conversation_summary_content_combine));
    }

}
