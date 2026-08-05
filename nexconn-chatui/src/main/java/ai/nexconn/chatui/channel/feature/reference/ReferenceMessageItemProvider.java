package ai.nexconn.chatui.channel.feature.reference;

import ai.nexconn.chat.channel.ChannelType;
import ai.nexconn.chat.message.FileMessage;
import ai.nexconn.chat.message.ImageMessage;
import ai.nexconn.chat.message.MediaMessageContent;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.ReferenceMessage;
import ai.nexconn.chat.message.StreamMessage;
import ai.nexconn.chat.message.TextMessage;
import ai.nexconn.chat.message.model.ReferenceMessageStatus;
import ai.nexconn.chat.user.model.UserInfo;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.activity.FilePreviewActivity;
import ai.nexconn.chatui.activity.PicturePagerActivity;
import ai.nexconn.chatui.channel.messagelist.provider.BaseMessageItemProvider;
import ai.nexconn.chatui.config.ChatUIThemeManager;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.picture.tools.ScreenUtils;
import ai.nexconn.chatui.userinfo.NCUserInfoManager;
import ai.nexconn.chatui.userinfo.model.GroupUserInfo;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.utils.route.RouteUtils;
import ai.nexconn.chatui.utils.text.StringUtils;
import ai.nexconn.chatui.utils.text.TextViewUtils;
import ai.nexconn.chatui.widget.ILinkClickListener;
import ai.nexconn.chatui.widget.LinkTextViewMovementMethod;
import ai.nexconn.chatui.widget.ReferenceDialog;
import ai.nexconn.chatui.widget.adapter.IViewProviderListener;
import ai.nexconn.chatui.widget.adapter.ViewHolder;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.LayoutDirection;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.text.TextUtilsCompat;
import androidx.fragment.app.FragmentActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import java.util.List;
import java.util.Locale;

public class ReferenceMessageItemProvider extends BaseMessageItemProvider<ReferenceMessage> {
    private static final int MAX_DENSITY_DPI = 500;
    private static final int STANDARD_DEFAULT_DENSITY_DPI = 440;
    private static final int DATUM_DENSITY_DPI = 160;
    private static int THUMB_COMPRESSED_SIZE = 240;
    private static int THUMB_COMPRESSED_MIN_SIZE = 100;
    private Integer minSize = null;
    private Integer maxSize = null;

    public ReferenceMessageItemProvider() {
        mConfig.showReadState = true;
        mConfig.showEditState = true;
        Context context = NCChatUI.getContext();
        if (context != null) {
            Resources resources = context.getResources();
            try {
                THUMB_COMPRESSED_SIZE =
                        resources.getInteger(
                                resources.getIdentifier(
                                        "nc_thumb_compress_size",
                                        "integer",
                                        context.getPackageName()));
                THUMB_COMPRESSED_MIN_SIZE =
                        resources.getInteger(
                                resources.getIdentifier(
                                        "nc_thumb_compress_min_size",
                                        "integer",
                                        context.getPackageName()));
            } catch (Resources.NotFoundException e) {
                RLog.e(TAG, "Failed to load reference image size config", e);
            }
        }
    }

    private static final String TAG = "ReferenceMessageItemProvider";

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.nc_item_reference_message, parent, false);
        return new ViewHolder(parent.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(
            ViewHolder holder,
            ViewHolder parentHolder,
            ReferenceMessage referenceMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        TextView referenceSendContent = holder.getView(R.id.nc_msg_tv_reference_send_content);
        if (referenceMessage.getReferMsgSenderId() != null) {
            holder.setText(
                    R.id.nc_msg_tv_reference_name,
                    getDisplayName(uiMessage, referenceMessage) + " : ");
        }
        if (referenceSendContent != null) {
            if (referenceMessage.getContent() != null) {
                setTextContent(
                        referenceSendContent, uiMessage, referenceMessage.getContent(), true);
            } else {
                referenceSendContent.setText("");
            }
            setMovementMethod(uiMessage, referenceSendContent);
        }
        int primaryAttrResId =
                ChatUIThemeManager.getAttrResId(holder.getContext(), R.attr.nc_text_primary_color);
        int secondaryAttrResId =
                ChatUIThemeManager.getAttrResId(
                        holder.getContext(), R.attr.nc_text_secondary_color);
        // Referenced name, content text and the vertical mark use the secondary color (aligns with
        // iOS).
        holder.setBackgroundRes(R.id.nc_reference_vertical_mark, secondaryAttrResId);
        holder.setTextColorRes(R.id.nc_msg_tv_reference_name, secondaryAttrResId);
        holder.setTextColorRes(R.id.nc_msg_tv_reference_file_name, primaryAttrResId);
        holder.setTextColorRes(R.id.nc_msg_tv_reference_content, secondaryAttrResId);
        if (processReferenceMessageStatus(referenceMessage, holder, uiMessage)) {
            return;
        }
        if (referenceMessage.getReferMsg() == null) {
            return;
        }
        if (referenceMessage.getReferMsg() instanceof TextMessage
                || referenceMessage.getReferMsg() instanceof StreamMessage) {
            String content;
            if (referenceMessage.getReferMsg() instanceof TextMessage) {
                content = ((TextMessage) referenceMessage.getReferMsg()).getText();
            } else {
                content = ((StreamMessage) referenceMessage.getReferMsg()).getContent();
            }
            // Fallback for audited/filtered reference content:
            // backend may return DEFAULT status with empty body.
            if (TextUtils.isEmpty(StringUtils.getStringNoBlank(content))) {
                setDeletedReferenceFallback(holder);
            } else {
                setTextType(
                        holder.getConvertView(),
                        holder,
                        parentHolder,
                        position,
                        referenceMessage,
                        content,
                        uiMessage);
                holder.setVisible(R.id.nc_msg_tv_reference_content, true);
                holder.setVisible(R.id.nc_msg_iv_reference, false);
                holder.setVisible(R.id.nc_msg_tv_reference_file_name, false);
            }
        } else if (referenceMessage.getReferMsg() instanceof ImageMessage) {
            setImageType(
                    holder.getConvertView(),
                    holder,
                    parentHolder,
                    position,
                    referenceMessage,
                    uiMessage);
            holder.setVisible(R.id.nc_msg_tv_reference_content, false);
            holder.setVisible(R.id.nc_msg_iv_reference, true);
            holder.setVisible(R.id.nc_msg_tv_reference_file_name, false);
        } else if (referenceMessage.getReferMsg() instanceof FileMessage) {
            setFileType(
                    holder.getConvertView(),
                    holder,
                    parentHolder,
                    position,
                    referenceMessage,
                    uiMessage);
            holder.setVisible(R.id.nc_msg_tv_reference_content, false);
            holder.setVisible(R.id.nc_msg_iv_reference, false);
            holder.setVisible(R.id.nc_msg_tv_reference_file_name, true);
        } else if (referenceMessage.getReferMsg() instanceof ReferenceMessage) {
            setReferenceType(
                    holder.getConvertView(),
                    holder,
                    parentHolder,
                    position,
                    referenceMessage,
                    uiMessage);
            holder.setVisible(R.id.nc_msg_tv_reference_content, true);
            holder.setVisible(R.id.nc_msg_iv_reference, false);
            holder.setVisible(R.id.nc_msg_tv_reference_file_name, false);
        } else {
            holder.setVisible(R.id.nc_msg_tv_reference_content, true);
            holder.setText(
                    R.id.nc_msg_tv_reference_content,
                    holder.getContext().getString(R.string.nc_message_unknown));
            holder.setVisible(R.id.nc_msg_iv_reference, false);
            holder.setVisible(R.id.nc_msg_tv_reference_file_name, false);
        }

        setMaximumDisplaySize(holder);
    }

    @Override
    protected boolean onItemClick(
            ViewHolder holder,
            ReferenceMessage referenceMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        return false;
    }

    @Override
    protected boolean onItemLongClick(
            ViewHolder holder,
            ReferenceMessage referenceMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        return false;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof ReferenceMessage;
    }

    @Override
    public Spannable getSummarySpannable(Context context, ReferenceMessage referenceMessage) {
        if (referenceMessage != null && !TextUtils.isEmpty(referenceMessage.getContent())) {
            return new SpannableString(referenceMessage.getContent());
        } else {
            return null;
        }
    }

    private String getDisplayName(UiMessage uiMessage, ReferenceMessage referenceMessage) {
        if (uiMessage.getMessage().getSenderUserId() != null) {
            UserInfo userInfo =
                    getUserInfo(
                            referenceMessage.getReferMsgSenderId(), referenceMessage.getReferMsg());
            String groupMemberName = "";
            if (uiMessage.getMessage().getChannelIdentifier().getChannelType()
                    == ChannelType.GROUP) {
                GroupUserInfo groupUserInfo =
                        NCUserInfoManager.getInstance()
                                .getGroupUserInfo(
                                        uiMessage
                                                .getMessage()
                                                .getChannelIdentifier()
                                                .getChannelId(),
                                        referenceMessage.getReferMsgSenderId());
                groupMemberName = groupUserInfo != null ? groupUserInfo.getNickname() : "";
            }
            return NCUserInfoManager.getInstance().getUserDisplayName(userInfo, groupMemberName);
        }
        return "";
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

    private SpannableStringBuilder createSpan(String content) {
        SpannableStringBuilder spannable = new SpannableStringBuilder(content);
        return spannable;
    }

    private void setTextContent(
            final TextView textView, final UiMessage data, String content, boolean isSendContent) {
        textView.setTag(data.getMessageId());
        content = isSendContent ? content : StringUtils.getStringNoBlank(content);
        if (isSendContent) {
            if (data.getContentSpannable() == null) {
                Runnable textViewRunnable =
                        () -> setTextMessageContent(textView, data, data.getContentSpannable());
                SpannableStringBuilder spannable =
                        TextViewUtils.getSpannable(
                                content,
                                false,
                                new TextViewUtils.RegularCallBack() {
                                    @Override
                                    public void finish(SpannableStringBuilder spannable) {
                                        data.setContentSpannable(spannable);
                                        if (textView.getTag().equals(data.getMessageId())) {
                                            textView.post(textViewRunnable);
                                        }
                                    }
                                });
                data.setContentSpannable(spannable);
            }
            setTextMessageContent(textView, data, data.getContentSpannable());
        } else {
            if (data.getReferenceContentSpannable() == null) {
                Runnable textViewRunnable =
                        () ->
                                setReferenceMessageContent(
                                        textView, data, data.getReferenceContentSpannable());
                SpannableStringBuilder spannable =
                        TextViewUtils.getSpannable(
                                content,
                                false,
                                new TextViewUtils.RegularCallBack() {
                                    @Override
                                    public void finish(SpannableStringBuilder spannable) {
                                        data.setReferenceContentSpannable(spannable);
                                        if (textView.getTag().equals(data.getMessageId())) {
                                            textView.post(textViewRunnable);
                                        }
                                    }
                                });
                data.setReferenceContentSpannable(spannable);
            }
            setReferenceMessageContent(textView, data, data.getReferenceContentSpannable());
        }
    }

    private void setMovementMethod(final UiMessage uiMessage, final TextView textView) {
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
                                                            textView.getContext(),
                                                            link,
                                                            uiMessage.getMessage());
                                }
                                if (result) return true;
                                String str = link.toLowerCase();
                                if (str.startsWith("http") || str.startsWith("https")) {
                                    RouteUtils.routeToWebActivity(textView.getContext(), link);
                                    result = true;
                                }

                                return result;
                            }
                        }));
    }

    private void setTextType(
            final View view,
            ViewHolder holder,
            ViewHolder parentHolder,
            final int position,
            final ReferenceMessage referenceMessage,
            final String content,
            final UiMessage uiMessage) {
        if (referenceMessage == null || referenceMessage.getReferMsg() == null) {
            return;
        }
        TextView textView = holder.getView(R.id.nc_msg_tv_reference_content);
        if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
                == LayoutDirection.RTL) {
            textView.getViewTreeObserver()
                    .addOnGlobalLayoutListener(new OnGlobalLayoutListenerByEllipsize(textView, 1));
        }

        setTextContent(textView, uiMessage, content, false);
        setReferenceContentAction(
                view, holder, parentHolder, position, referenceMessage, uiMessage);
        textClickAction(view, textView, uiMessage, referenceMessage);
    }

    private void textClickAction(
            final View view,
            TextView textView,
            final UiMessage uiMessage,
            ReferenceMessage referenceMessage) {
        textView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isInvalidReferenceStatus(referenceMessage)) {
                            return;
                        }
                        showPopWindow(view.getContext(), uiMessage);
                        hideInputKeyboard(view);
                    }
                });
    }

    private void hideInputKeyboard(View view) {
        InputMethodManager imm =
                (InputMethodManager)
                        view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void setReferenceContentAction(
            final View view,
            ViewHolder holder,
            final ViewHolder parentHolder,
            final int position,
            final ReferenceMessage referenceMessage,
            final UiMessage uiMessage) {
        holder.setOnLongClickListener(
                R.id.nc_msg_tv_reference_content,
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        return parentHolder.getView(R.id.nc_content).performLongClick();
                    }
                });

        holder.setOnLongClickListener(
                R.id.nc_msg_tv_reference_send_content,
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        return parentHolder.getView(R.id.nc_content).performLongClick();
                    }
                });
        TextView referenceContent = holder.getView(R.id.nc_msg_tv_reference_content);
        setMovementMethod(uiMessage, referenceContent);
    }

    private void setImageType(
            final View view,
            ViewHolder holder,
            final ViewHolder parentHolder,
            final int position,
            final ReferenceMessage referenceMessage,
            final UiMessage uiMessage) {
        if (referenceMessage == null || referenceMessage.getReferMsg() == null) {
            return;
        }
        final ImageView imageView = holder.getView(R.id.nc_msg_iv_reference);
        setReferenceImageDefaultLayoutSize(imageView);
        ImageMessage content = (ImageMessage) referenceMessage.getReferMsg();
        String imageUrl = null;
        if (content.getLocalPath() != null) {
            imageUrl = content.getLocalPath();
        } else if (content.getRemoteUrl() != null) {
            imageUrl = content.getRemoteUrl();
        }
        if (imageUrl != null) {
            RequestOptions options =
                    RequestOptions.bitmapTransform(
                                    new RoundedCorners(
                                            ScreenUtils.dip2px(NCChatUI.getContext(), 3)))
                            .override(
                                    com.bumptech.glide.request.target.Target.SIZE_ORIGINAL,
                                    com.bumptech.glide.request.target.Target.SIZE_ORIGINAL);
            Glide.with(view)
                    .load(imageUrl)
                    .apply(options)
                    .listener(
                            new com.bumptech.glide.request.RequestListener<
                                    android.graphics.drawable.Drawable>() {
                                @Override
                                public boolean onLoadFailed(
                                        com.bumptech.glide.load.engine.GlideException e,
                                        Object model,
                                        com.bumptech.glide.request.target.Target<
                                                        android.graphics.drawable.Drawable>
                                                target,
                                        boolean isFirstResource) {
                                    setReferenceImageLayoutSize(imageView, 35, 35);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(
                                        android.graphics.drawable.Drawable resource,
                                        Object model,
                                        com.bumptech.glide.request.target.Target<
                                                        android.graphics.drawable.Drawable>
                                                target,
                                        com.bumptech.glide.load.DataSource dataSource,
                                        boolean isFirstResource) {
                                    measureReferenceImageLayout(imageView, resource);
                                    return false;
                                }
                            })
                    .into(imageView);
        } else {
            setReferenceImageLayoutSize(imageView, 35, 35);
        }
        imageView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (isInvalidReferenceStatus(referenceMessage)) {
                            return;
                        }
                        try {
                            ai.nexconn.chatui.utils.message.MessageHolder.holdMessage(
                                    uiMessage.getMessage());
                            Intent intent =
                                    new Intent(view.getContext(), PicturePagerActivity.class);
                            intent.setPackage(view.getContext().getPackageName());
                            view.getContext().startActivity(intent);
                        } catch (Exception e) {
                            RLog.e(TAG, "setImageType", e);
                        }
                    }
                });

        imageView.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        return parentHolder.getView(R.id.nc_content).performLongClick();
                    }
                });

        holder.setOnLongClickListener(
                R.id.nc_msg_tv_reference_send_content,
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        return parentHolder.getView(R.id.nc_content).performLongClick();
                    }
                });
    }

    private void measureReferenceImageLayout(View imageView, Drawable drawable) {
        if (imageView == null || drawable == null) {
            return;
        }
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        if (width <= 0 || height <= 0) {
            setReferenceImageDefaultLayoutSize(imageView);
            return;
        }
        if (minSize == null) {
            minSize = THUMB_COMPRESSED_MIN_SIZE;
        }
        if (maxSize == null) {
            maxSize = THUMB_COMPRESSED_SIZE;
        }

        int finalWidth;
        int finalHeight;
        if (width < minSize || height < minSize) {
            if (width < height) {
                finalWidth = minSize;
                finalHeight = Math.min((int) (minSize * 1f / width * height), maxSize);
            } else {
                finalHeight = minSize;
                finalWidth = Math.min((int) (minSize * 1f / height * width), maxSize);
            }
        } else if (width < maxSize && height < maxSize) {
            finalWidth = width;
            finalHeight = height;
        } else {
            if (width > height) {
                if (width * 1f / height <= maxSize * 1.0f / minSize) {
                    finalWidth = maxSize;
                    finalHeight = (int) (maxSize * 1f / width * height);
                } else {
                    finalWidth = maxSize;
                    finalHeight = minSize;
                }
            } else {
                if (height * 1f / width <= maxSize * 1.0f / minSize) {
                    finalHeight = maxSize;
                    finalWidth = (int) (maxSize * 1f / height * width);
                } else {
                    finalHeight = maxSize;
                    finalWidth = minSize;
                }
            }
        }

        setReferenceImageLayoutSize(imageView, finalWidth / 2, finalHeight / 2);
    }

    private void setReferenceImageDefaultLayoutSize(View imageView) {
        if (imageView == null) {
            return;
        }
        int sizePx =
                imageView.getResources().getDimensionPixelSize(R.dimen.nc_reference_image_size);
        ViewGroup.LayoutParams imageParams = imageView.getLayoutParams();
        if (imageParams != null) {
            imageParams.width = sizePx;
            imageParams.height = sizePx;
            imageView.setLayoutParams(imageParams);
        }
    }

    private void setReferenceImageLayoutSize(View imageView, int widthDp, int heightDp) {
        if (imageView == null) {
            return;
        }
        Context context = imageView.getContext();
        int widthPx = ScreenUtils.dip2px(context, widthDp);
        int heightPx = ScreenUtils.dip2px(context, heightDp);

        ViewGroup.LayoutParams imageParams = imageView.getLayoutParams();
        if (imageParams != null) {
            imageParams.width = widthPx;
            imageParams.height = heightPx;
            imageView.setLayoutParams(imageParams);
        }
    }

    private void setFileType(
            final View view,
            final ViewHolder holder,
            final ViewHolder parentHolder,
            final int position,
            final ReferenceMessage referenceMessage,
            final UiMessage uiMessage) {
        if (referenceMessage == null || referenceMessage.getReferMsg() == null) {
            return;
        }
        final FileMessage content = (FileMessage) referenceMessage.getReferMsg();
        String string =
                view.getContext().getString(R.string.nc_search_file_prefix)
                        + ' '
                        + content.getName();

        final SpannableStringBuilder ssb = new SpannableStringBuilder(string);
        ssb.setSpan(
                new ForegroundColorSpan(
                        ChatUIThemeManager.getColorFromAttrId(
                                view.getContext(), R.attr.nc_primary_color)),
                0,
                string.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.setText(R.id.nc_msg_tv_reference_file_name, ssb);
        holder.setOnClickListener(
                R.id.nc_msg_tv_reference_file_name,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (isInvalidReferenceStatus(referenceMessage)) {
                            return;
                        }
                        try {
                            Message msg = uiMessage.getMessage();
                            MediaMessageContent fileContent = null;
                            if (msg != null && msg.getContent() instanceof ReferenceMessage) {
                                MessageContent referMsg =
                                        ((ReferenceMessage) msg.getContent()).getReferMsg();
                                if (referMsg instanceof MediaMessageContent) {
                                    fileContent = (MediaMessageContent) referMsg;
                                }
                            }
                            ai.nexconn.chatui.utils.message.MessageHolder.holdMessage(msg);
                            ai.nexconn.chatui.utils.message.MessageHolder.holdContent(fileContent);
                            Intent intent = new Intent();
                            intent.setClass(view.getContext(), FilePreviewActivity.class);
                            intent.setPackage(view.getContext().getPackageName());
                            intent.putExtra("Progress", uiMessage.getProgress());
                            view.getContext().startActivity(intent);
                        } catch (Exception e) {
                            RLog.e(TAG, "exception: " + e);
                        }
                    }
                });
        holder.setOnLongClickListener(
                R.id.nc_msg_tv_reference_file_name,
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        return parentHolder.getView(R.id.nc_content).performLongClick();
                    }
                });
        holder.setOnLongClickListener(
                R.id.nc_msg_tv_reference_send_content,
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        return parentHolder.getView(R.id.nc_content).performLongClick();
                    }
                });
    }

    private void setReferenceType(
            final View view,
            ViewHolder holder,
            ViewHolder parentHolder,
            final int position,
            final ReferenceMessage referenceMessage,
            final UiMessage uiMessage) {
        if (referenceMessage == null || referenceMessage.getReferMsg() == null) {
            return;
        }
        ReferenceMessage content = (ReferenceMessage) referenceMessage.getReferMsg();
        setTextContent(
                holder.getView(R.id.nc_msg_tv_reference_content),
                uiMessage,
                content.getContent(),
                false);
        setReferenceContentAction(
                view, holder, parentHolder, position, referenceMessage, uiMessage);
        textClickAction(
                view,
                holder.getView(R.id.nc_msg_tv_reference_content),
                uiMessage,
                referenceMessage);
    }

    // Processes reference message status; returns true for delete/recall to show unified display.
    private boolean processReferenceMessageStatus(
            ReferenceMessage referenceMessage, ViewHolder holder, UiMessage uiMessage) {
        ReferenceMessageStatus status = referenceMessage.getReferMsgStatus();
        if (status == ReferenceMessageStatus.DELETED || status == ReferenceMessageStatus.RECALLED) {
            setDeletedReferenceFallback(holder);
            return true;
        }
        return false;
    }

    private void setDeletedReferenceFallback(ViewHolder holder) {
        TextView textView = holder.getView(R.id.nc_msg_tv_reference_content);
        textView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
                == LayoutDirection.RTL) {
            textView.getViewTreeObserver()
                    .addOnGlobalLayoutListener(new OnGlobalLayoutListenerByEllipsize(textView, 1));
        }
        holder.setText(
                R.id.nc_msg_tv_reference_content,
                holder.getContext().getString(R.string.nc_reference_status_delete));
        holder.setVisible(R.id.nc_msg_tv_reference_content, true);
        holder.setVisible(R.id.nc_msg_iv_reference, false);
        holder.setVisible(R.id.nc_msg_tv_reference_file_name, false);
    }

    private boolean isInvalidReferenceStatus(ReferenceMessage referenceMessage) {
        ReferenceMessageStatus status = referenceMessage.getReferMsgStatus();
        return status == ReferenceMessageStatus.DELETED
                || status == ReferenceMessageStatus.RECALLED;
    }

    private void showPopWindow(Context context, UiMessage uiMessage) {
        // View inflate = View.inflate(context, R.layout.nc_reference_popupwindow, null);
        if (context instanceof FragmentActivity) {
            new ReferenceDialog(uiMessage)
                    .show(((FragmentActivity) context).getSupportFragmentManager());
        }

        // final PopupWindow popupWindow = new PopupWindow(inflate,
        // WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        // lp.layoutInDisplayCutoutMode =
        // WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        //
        //        //sdk > 21 fix title bar not being masked
        //        popupWindow.setClippingEnabled(false);
        //        popupWindow.setFocusable(true);
        //        popupWindow.setOutsideTouchable(true);
        //        popupWindow.showAtLocation(inflate, Gravity.NO_GRAVITY, 0, 0);
        //        fullScreenImmersive(inflate);
    }

    private void fullScreenImmersive(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int uiOptions =
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN;
            view.setSystemUiVisibility(uiOptions);
        }
    }

    public class OnGlobalLayoutListenerByEllipsize
            implements ViewTreeObserver.OnGlobalLayoutListener {

        private TextView textView;
        private int maxLines;

        public OnGlobalLayoutListenerByEllipsize(TextView textView, int maxLines) {
            if (maxLines <= 0) {
                throw new IllegalArgumentException("MaxLines cannot be less than or equal to 0");
            }
            this.textView = textView;
            this.maxLines = maxLines;
            this.textView.setMaxLines(this.maxLines + 1);
            this.textView.setSingleLine(false);
        }

        @Override
        public void onGlobalLayout() {
            if (textView.getLineCount() > maxLines) {
                int line = textView.getLayout().getLineEnd(maxLines - 1);
                // Use CharSequence for emoji compatibility; String would prevent emoji from
                // displaying
                CharSequence truncate = "...";
                CharSequence text = textView.getText();
                try {
                    text = text.subSequence(0, line - 2);
                } catch (Exception e) {
                    truncate = "";
                    text = textView.getText();
                }
                if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
                        == LayoutDirection.RTL) {
                    textView.setText(truncate.toString() + text);
                } else {
                    textView.setText(text + truncate.toString());
                }
            }
        }
    }

    /**
     * If DisplayMetrics.densityDpi exceeds 500 in portrait mode, adjusts the parent View width.
     *
     * <p>pixel = DP_VALUE * (deviceDPI / baseDPI_160)
     *
     * @param holder ViewHolder
     */
    private void setMaximumDisplaySize(ViewHolder holder) {
        Resources resources = holder.getContext().getResources();
        if (resources == null) {
            return;
        }

        DisplayMetrics metrics = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();
        if (metrics.densityDpi > MAX_DENSITY_DPI
                && config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            float dimensionValue =
                    holder.getContext().getResources().getDimension(R.dimen.nc_reference_width);
            float dbValue = dimensionValue / metrics.density;
            float viewWidthValue = dbValue * STANDARD_DEFAULT_DENSITY_DPI / DATUM_DENSITY_DPI;
            LinearLayout rootView = holder.getView(R.id.nc_reference_root_view);
            ViewGroup.LayoutParams params = rootView.getLayoutParams();
            // Ensures layout doesn't become oversized on high-density DPI screens
            params.width = (int) viewWidthValue;
            params.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            rootView.setLayoutParams(params);
        }
    }
}
