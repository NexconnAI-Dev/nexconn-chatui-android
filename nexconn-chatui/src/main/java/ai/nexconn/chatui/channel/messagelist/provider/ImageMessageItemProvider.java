package ai.nexconn.chatui.channel.messagelist.provider;

import ai.nexconn.chat.message.ImageMessage;
import ai.nexconn.chat.message.Message;
import ai.nexconn.chat.message.MessageContent;
import ai.nexconn.chat.message.model.MessageDirection;
import ai.nexconn.chat.message.model.MessageType;
import ai.nexconn.chatui.NCChatUI;
import ai.nexconn.chatui.R;
import ai.nexconn.chatui.activity.PicturePagerActivity;
import ai.nexconn.chatui.channel.feature.resend.ResendManager;
import ai.nexconn.chatui.model.State;
import ai.nexconn.chatui.model.UiMessage;
import ai.nexconn.chatui.picture.tools.ScreenUtils;
import ai.nexconn.chatui.utils.file.FileUtils;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.widget.adapter.IViewProviderListener;
import ai.nexconn.chatui.widget.adapter.ViewHolder;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import java.util.List;

public class ImageMessageItemProvider extends BaseMessageItemProvider<ImageMessage> {
    private static final String TAG = "ImageMessageItemProvide";
    private static int THUMB_COMPRESSED_SIZE = 240;
    private static int THUMB_COMPRESSED_MIN_SIZE = 100;
    private final String MSG_TAG = MessageType.IMAGE;
    private Integer minSize = null;
    private Integer maxSize = null;

    public ImageMessageItemProvider() {
        mConfig.showContentBubble = false;
        mConfig.showProgress = false;
        mConfig.showReadState = true;
        mConfig.showWarning = true;
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
                e.printStackTrace();
            }
        }
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.nc_image_message_item, parent, false);
        return new ViewHolder(view.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(
            final ViewHolder holder,
            ViewHolder parentHolder,
            ImageMessage message,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        final ImageView imageView = holder.getView(R.id.nc_image);
        final View imageContainer = holder.getView(R.id.rl_content);
        if (!checkViewsValid(imageView) || !checkViewsValid(imageContainer)) {
            RLog.e(TAG, "checkViewsValid error," + uiMessage.getMessageType());
            return;
        }
        String thumbnailBase64 = message.getThumbnailBase64();
        Uri thumbnailUri = message.getThumbnailUri();
        String localPath = message.getLocalPath();
        Object thumbSource = null;
        // Keep thumbnail loading lightweight and avoid eager original download.
        // Priority: thumbnail(base64/uri/path) > localPath.
        byte[] thumbnailBytes = decodeThumbnailBase64(thumbnailBase64);
        if (thumbnailBytes != null && thumbnailBytes.length > 0) {
            thumbSource = thumbnailBytes;
        }
        if (thumbSource == null && isLikelyUri(thumbnailBase64)) {
            thumbSource = thumbnailBase64;
        }
        if (thumbSource == null && isUriSourceAvailable(thumbnailUri)) {
            thumbSource = thumbnailUri;
        }
        if (thumbSource == null && isLocalPathAvailable(localPath)) {
            thumbSource = localPath;
        }
        if (uiMessage.getState() == State.PROGRESS
                || (uiMessage.getState() == State.ERROR
                        && ResendManager.getInstance().needResend(uiMessage.getClientId()))) {
            holder.setVisible(R.id.rl_progress, true);
            holder.setVisible(R.id.main_bg, true);
            holder.setText(R.id.tv_progress, uiMessage.getProgress() + "%");
        } else {
            holder.setVisible(R.id.rl_progress, false);
            holder.setVisible(R.id.main_bg, false);
        }
        if (thumbSource != null) {
            boolean hasImmediateThumbnail = false;
            if (thumbnailBytes != null && thumbnailBytes.length > 0) {
                Bitmap thumbnailBitmap =
                        BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.length);
                if (thumbnailBitmap != null) {
                    imageView.setImageBitmap(thumbnailBitmap);
                    measureLayoutParams(
                            imageContainer,
                            new BitmapDrawable(
                                    imageContainer.getContext().getResources(), thumbnailBitmap));
                    hasImmediateThumbnail = true;
                }
            }
            RequestOptions options =
                    RequestOptions.bitmapTransform(
                                    new RoundedCorners(
                                            ScreenUtils.dip2px(NCChatUI.getContext(), 6)))
                            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
            if (hasImmediateThumbnail && imageView.getDrawable() != null) {
                options = options.placeholder(imageView.getDrawable());
            }
            Glide.with(imageView)
                    .load(thumbSource)
                    .error(
                            uiMessage.getMessage().getDirection() == MessageDirection.SEND
                                    ? R.drawable.nc_send_thumb_image_broken
                                    : R.drawable.nc_received_thumb_image_broken)
                    .apply(options)
                    .listener(
                            new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(
                                        @Nullable GlideException e,
                                        Object model,
                                        Target<Drawable> target,
                                        boolean isFirstResource) {
                                    mConfig.showWarning = true;
                                    ViewGroup.LayoutParams params =
                                            imageContainer.getLayoutParams();
                                    params.height =
                                            ScreenUtils.dip2px(imageContainer.getContext(), 35);
                                    params.width =
                                            ScreenUtils.dip2px(imageContainer.getContext(), 35);
                                    imageContainer.setLayoutParams(params);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(
                                        Drawable resource,
                                        Object model,
                                        Target<Drawable> target,
                                        DataSource dataSource,
                                        boolean isFirstResource) {
                                    mConfig.showWarning = true;
                                    measureLayoutParams(imageContainer, resource);
                                    return false;
                                }
                            })
                    .into(imageView);
        } else {
            mConfig.showWarning = true;
            ViewGroup.LayoutParams layoutParams = imageContainer.getLayoutParams();
            layoutParams.height = ScreenUtils.dip2px(imageContainer.getContext(), 35);
            layoutParams.width = ScreenUtils.dip2px(imageContainer.getContext(), 35);
            imageContainer.setLayoutParams(layoutParams);
            imageView.setImageResource(
                    uiMessage.getMessage().getDirection() == MessageDirection.SEND
                            ? R.drawable.nc_send_thumb_image_broken
                            : R.drawable.nc_received_thumb_image_broken);
        }
    }

    @Override
    protected boolean onItemClick(
            ViewHolder holder,
            ImageMessage imageMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        Message message = uiMessage.getMessage();
        if (message == null || message.getContent() == null) {
            RLog.e(TAG, "onItemClick error, message or message content is null");
            return false;
        }
        ai.nexconn.chatui.utils.message.MessageHolder.holdMessage(message);
        Intent intent = new Intent(holder.getContext(), PicturePagerActivity.class);
        holder.getContext().startActivity(intent);
        return true;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof ImageMessage && !messageContent.isDestruct();
    }

    private void measureLayoutParams(View view, Drawable drawable) {
        if (view == null) {
            return;
        }
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
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
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = ScreenUtils.dip2px(view.getContext(), finalHeight / 2);
        params.width = ScreenUtils.dip2px(view.getContext(), finalWidth / 2);
        view.setLayoutParams(params);
    }

    @Override
    public Spannable getSummarySpannable(Context context, ImageMessage imageMessage) {
        return new SpannableString(
                context.getString(R.string.nc_conversation_summary_content_image));
    }

    private boolean isLocalPathAvailable(String localPath) {
        if (TextUtils.isEmpty(localPath)) {
            return false;
        }
        try {
            Uri uri = Uri.parse(localPath);
            if (uri == null) {
                return false;
            }
            String scheme = uri.getScheme();
            if (TextUtils.isEmpty(scheme)) {
                return new java.io.File(localPath).exists();
            }
            if ("file".equalsIgnoreCase(scheme) || "content".equalsIgnoreCase(scheme)) {
                Context context = NCChatUI.getContext();
                return context != null && FileUtils.isFileExistsWithUri(context, uri);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isUriSourceAvailable(Uri uri) {
        if (uri == null) {
            return false;
        }
        try {
            String scheme = uri.getScheme();
            if (TextUtils.isEmpty(scheme)) {
                return new java.io.File(uri.toString()).exists();
            }
            if ("file".equalsIgnoreCase(scheme) || "content".equalsIgnoreCase(scheme)) {
                Context context = NCChatUI.getContext();
                return context != null && FileUtils.isFileExistsWithUri(context, uri);
            }
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isLikelyBase64(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        String normalized = normalizeBase64(value);
        if (TextUtils.isEmpty(normalized)) {
            return false;
        }
        if (normalized.length() < 16) {
            return false;
        }
        // URL-like strings must not be decoded as base64.
        if (normalized.startsWith("http://")
                || normalized.startsWith("https://")
                || normalized.startsWith("file://")
                || normalized.startsWith("content://")) {
            return false;
        }
        return normalized.matches("^[A-Za-z0-9+/=_-]+$");
    }

    private byte[] decodeThumbnailBase64(String value) {
        if (!isLikelyBase64(value)) {
            return null;
        }
        try {
            String normalized = normalizeBase64(value);
            if (TextUtils.isEmpty(normalized)) {
                return null;
            }
            byte[] decoded = Base64.decode(normalized, Base64.DEFAULT);
            if (decoded != null && decoded.length > 0) {
                return decoded;
            }
            decoded = Base64.decode(normalized, Base64.URL_SAFE);
            return (decoded != null && decoded.length > 0) ? decoded : null;
        } catch (IllegalArgumentException e) {
            try {
                byte[] decoded = Base64.decode(normalizeBase64(value), Base64.URL_SAFE);
                return (decoded != null && decoded.length > 0) ? decoded : null;
            } catch (IllegalArgumentException e2) {
                RLog.w(TAG, "Invalid thumbnailBase64, fallback to uri source");
                return null;
            }
        }
    }

    private String normalizeBase64(String value) {
        if (TextUtils.isEmpty(value)) {
            return value;
        }
        String normalized = value.trim();
        if (normalized.startsWith("data:")) {
            int idx = normalized.indexOf("base64,");
            if (idx >= 0 && idx + 7 < normalized.length()) {
                normalized = normalized.substring(idx + 7);
            }
        }
        return normalized.replace("\n", "").replace("\r", "").trim();
    }

    private boolean isLikelyUri(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        String normalized = value.trim();
        return normalized.startsWith("http://")
                || normalized.startsWith("https://")
                || normalized.startsWith("file://")
                || normalized.startsWith("content://");
    }
}
