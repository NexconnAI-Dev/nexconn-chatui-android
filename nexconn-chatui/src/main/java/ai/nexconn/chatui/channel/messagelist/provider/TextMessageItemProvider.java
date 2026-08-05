package ai.nexconn.chatui.channel.messagelist.provider;

import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.TextMessage;
import ai.nexconn.chat.message.model.MessageDirection;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.State;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.route.RouteUtils;
import ai.nexconn.chatui.utils.text.TextViewUtils;
import ai.nexconn.chatui.widget.ILinkClickListener;
import ai.nexconn.chatui.widget.LinkTextViewMovementMethod;
import ai.nexconn.chatui.widget.adapter.IViewProviderListener;
import ai.nexconn.chatui.widget.adapter.ViewHolder;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.LayoutDirection;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.text.TextUtilsCompat;
import java.util.List;
import java.util.Locale;

public class TextMessageItemProvider extends BaseMessageItemProvider<TextMessage> {

    public TextMessageItemProvider() {
        mConfig.showReadState = true;
        mConfig.showEditState = true;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.nc_translate_text_message_item, parent, false);
        return new ViewHolder(parent.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(
            final ViewHolder holder,
            ViewHolder parentHolder,
            TextMessage message,
            final UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        final TextView textView = holder.getView(R.id.nc_text);
        final TextView translatedView = holder.getView(R.id.nc_translated_text);
        final ProgressBar progressBar = holder.getView(R.id.nc_pb_translating);

        if (!checkViewsValid(textView, translatedView, progressBar)) {
            RLog.e(TAG, "checkViewsValid error," + uiMessage.getMessageType());
            return;
        }

        if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
                == LayoutDirection.RTL) {
            textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        }
        textView.setTag(uiMessage.getMessageId());
        if (uiMessage.getContentSpannable() == null) {
            Runnable textViewRunnable =
                    new Runnable() {
                        @Override
                        public void run() {
                            String tag =
                                    textView.getTag() == null ? "" : textView.getTag().toString();
                            if (TextUtils.equals(tag, String.valueOf(uiMessage.getMessageId()))) {
                                setTextMessageContent(
                                        textView, uiMessage, uiMessage.getContentSpannable());
                            }
                        }
                    };
            SpannableStringBuilder spannable =
                    TextViewUtils.getSpannable(
                            message.getText(),
                            new TextViewUtils.RegularCallBack() {
                                @Override
                                public void finish(SpannableStringBuilder spannable) {
                                    uiMessage.setContentSpannable(spannable);
                                    textView.post(textViewRunnable);
                                }
                            });
            uiMessage.setContentSpannable(spannable);
        }
        setTextMessageContent(textView, uiMessage, uiMessage.getContentSpannable());
        textView.setMovementMethod(
                new LinkTextViewMovementMethod(
                        new ILinkClickListener() {
                            @Override
                            public boolean onLinkClick(String link) {
                                boolean result = false;
                                if (NCChatUIConfig.channelConfig().getChannelClickListener()
                                        != null) {
                                    result =
                                            NCChatUIConfig.channelConfig()
                                                    .getChannelClickListener()
                                                    .onMessageLinkClick(
                                                            holder.getContext(),
                                                            link,
                                                            uiMessage.getMessage());
                                }
                                if (result) {
                                    return true;
                                }
                                String str = link.toLowerCase();
                                if (str.startsWith("http") || str.startsWith("https")) {
                                    RouteUtils.routeToWebActivity(textView.getContext(), link);
                                    result = true;
                                }

                                return result;
                            }
                        }));
        textView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ViewParent parent = view.getParent();
                        if (parent instanceof View) {
                            ((View) parent).performClick();
                        }
                    }
                });

        textView.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        ViewParent parent = view.getParent();
                        if (parent instanceof View) {
                            return ((View) parent).performLongClick();
                        }
                        return false;
                    }
                });

        boolean isSender = uiMessage.getMessage().getDirection() == MessageDirection.SEND;

        if (mConfig.showContentBubble) {
            holder.setBackgroundRes(
                    R.id.nc_text,
                    ChatUIThemeManager.getAttrResId(
                            holder.getContext(),
                            isSender
                                    ? R.attr.nc_conversation_msg_send_background
                                    : R.attr.nc_conversation_msg_receiver_background));
        }
        if (uiMessage.getTranslateStatus() == State.SUCCESS
                && !TextUtils.isEmpty(uiMessage.getTranslatedContent())) {
            translatedView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            translatedView.setText(uiMessage.getTranslatedContent());
            holder.setBackgroundRes(
                    R.id.nc_translated_text,
                    isSender
                            ? R.drawable.nc_ic_translation_bubble_right
                            : R.drawable.nc_ic_translation_bubble_left);
        } else if (uiMessage.getTranslateStatus() == State.PROGRESS) {
            translatedView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            translatedView.setVisibility(View.GONE);
            holder.setBackgroundRes(
                    R.id.nc_pb_translating,
                    isSender
                            ? R.drawable.nc_ic_translation_bubble_right
                            : R.drawable.nc_ic_translation_bubble_left);
        } else {
            translatedView.setText(null);
            translatedView.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            translatedView.setBackground(null);
        }

        setDirection(textView, isSender);
        setDirection(translatedView, isSender);
        setDirection(progressBar, isSender);

        holder.itemView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ViewParent parent = v.getParent();
                        if (parent instanceof View) {
                            ((View) parent).performClick();
                        }
                    }
                });
        holder.itemView.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        ViewParent parent = v.getParent();
                        if (parent instanceof View) {
                            return ((View) parent).performLongClick();
                        }
                        return false;
                    }
                });
    }

    private void setDirection(View view, boolean isSender) {
        ConstraintLayout.LayoutParams lp = ((ConstraintLayout.LayoutParams) view.getLayoutParams());
        if (isSender) {
            lp.startToStart = ConstraintLayout.LayoutParams.UNSET;
            lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        } else {
            lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            lp.endToEnd = ConstraintLayout.LayoutParams.UNSET;
        }
        view.setLayoutParams(lp);
    }

    @Override
    protected boolean onItemClick(
            ViewHolder holder,
            TextMessage message,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        return false;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof TextMessage && !messageContent.isDestruct();
    }

    @Override
    public Spannable getSummarySpannable(Context context, TextMessage message) {
        if (message != null && !TextUtils.isEmpty(message.getText())) {
            String content = message.getText();
            content = content.replace("\n", " ");
            if (content.length() > 100) {
                content = content.substring(0, 100);
            }
            return new SpannableString(content);
        } else {
            return new SpannableString("");
        }
    }

    @Override
    public boolean showBubble() {
        return false;
    }
}
